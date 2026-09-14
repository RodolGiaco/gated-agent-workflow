#!/usr/bin/env bash
# Applies the server-side barrier to the repository this is run from: the branch
# ruleset, the required status checks bound to the app that publishes them, and
# the auto-merge setting. Idempotent: an existing ruleset is updated, not
# duplicated.
#
# This is the only layer the model cannot switch off. The permission rules and
# the hooks are evadable by design; without this script the kit installs two
# layers and no barrier.
set -uo pipefail

KIT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" \
  || { echo "protection: not inside a git repository" >&2; exit 1; }
cd "$REPO_ROOT" || exit 1

RULESET_NAME="protect-main"
TEMPLATE="$KIT_DIR/github/ruleset-main.json"

fail() { printf 'protection: %s\n' "$1" >&2; exit 1; }
note() { printf 'protection: %s\n' "$1"; }

command -v gh >/dev/null 2>&1 || fail "gh is required"
command -v jq >/dev/null 2>&1 || fail "jq is required"
gh auth status >/dev/null 2>&1 || fail "gh is not authenticated; run: gh auth login"
[ -r "$TEMPLATE" ] || fail "cannot read $TEMPLATE"

REPO=$(gh repo view --json nameWithOwner --jq .nameWithOwner) \
  || fail "cannot resolve the repository; is there a remote?"
note "repository: $REPO"

# Branch protection is unavailable on private repositories under a free plan,
# so the kit says so instead of failing later with an opaque API error.
VISIBILITY=$(gh repo view --json visibility --jq .visibility)
note "visibility: $VISIBILITY"
if [ "$VISIBILITY" != "PUBLIC" ]; then
  note "warning: rulesets need a paid plan on a private repository."
  note "         if the next step fails with 403, that is why."
fi

# ------------------------------------------------------- required checks ----
# Each required check is bound to the app that publishes it. Without that
# binding, anyone with write access to the repository can set the state of any
# status check, which includes the agent publishing its own green verdict.
#
# The app id is measured from the check runs this repository actually produced,
# never taken from memory. Check runs attach to the head commit of a pull
# request, so the protected branch is not where they are found.
APP_ID=""
HEAD_SHA=$(gh pr list --repo "$REPO" --state merged --limit 1 \
           --json headRefOid \
           --jq '.[0].headRefOid // empty') \
  || fail "cannot query merged pull requests"

if [ -n "$HEAD_SHA" ]; then
  APP_ID=$(gh api "/repos/$REPO/commits/$HEAD_SHA/check-runs" \
           --jq '[.check_runs[].app.id] | unique | if length == 1 then .[0] else empty end') \
    || fail "cannot query check runs for commit $HEAD_SHA"
fi

CONTEXTS=$(jq -r '
  .rules[] | select(.type=="required_status_checks")
  | .parameters.required_status_checks[].context' "$TEMPLATE" 2>/dev/null | tr '\n' ' ')

PAYLOAD=$(mktemp)
trap 'rm -f "$PAYLOAD"' EXIT

if [ -n "$APP_ID" ]; then
  note "status checks will be bound to app id $APP_ID"
  jq --argjson app "$APP_ID" '
    (.rules[] | select(.type=="required_status_checks")
     | .parameters.required_status_checks[]) |= (.integration_id = $app)
  ' "$TEMPLATE" > "$PAYLOAD"
else
  # A repository with no merged pull request has published no check run yet, so
  # there is no app id to measure. Requiring a check that never reported would
  # block every pull request with no way out, so the checks are dropped from
  # this first application and added once the workflows have run.
  note "no check run found yet, so required status checks are left out for now"
  note "run this again after the first pull request merges to add them"
  jq 'del(.rules[] | select(.type=="required_status_checks"))' "$TEMPLATE" > "$PAYLOAD"
  CONTEXTS=""
fi

# --------------------------------------------------------------- apply ----
EXISTING=$(gh api "/repos/$REPO/rulesets" \
           --jq "[.[] | select(.name==\"$RULESET_NAME\") | .id][0] // empty") \
  || fail "cannot query repository rulesets"

if [ -n "$EXISTING" ]; then
  note "updating ruleset $EXISTING"
  gh api --method PUT "/repos/$REPO/rulesets/$EXISTING" --input "$PAYLOAD" >/dev/null \
    || fail "the ruleset could not be updated"
  RULESET_ID="$EXISTING"
else
  note "creating the ruleset"
  RULESET_ID=$(gh api --method POST "/repos/$REPO/rulesets" --input "$PAYLOAD" --jq .id) \
    || fail "the ruleset could not be created; on a private repository this needs a paid plan"
fi

# Auto-merge is a separate repository switch. Without it, queueing the merge
# fails and the cycle stops at an open pull request nobody merges.
gh api --method PATCH "/repos/$REPO" -F allow_auto_merge=true >/dev/null 2>&1 \
  || note "warning: auto-merge could not be enabled; enable it in the repository settings"

# --------------------------------------------------------------- verify ----
# A mutation is not applied until it is read back.
echo
note "verifying"
FAILURES=0
check() {
  if [ "$2" = "true" ]; then printf 'PASS  %s\n' "$1"
  else printf 'FAIL  %s\n      %s\n' "$1" "$3"; FAILURES=$((FAILURES + 1)); fi
}

STATE=$(gh api "/repos/$REPO/rulesets/$RULESET_ID" 2>/dev/null)

check "the ruleset is active" \
  "$([ "$(jq -r .enforcement <<<"$STATE")" = "active" ] && echo true)" \
  "enforcement is $(jq -r .enforcement <<<"$STATE")"

check "nobody can bypass it" \
  "$([ "$(jq -r '.bypass_actors | length' <<<"$STATE")" = "0" ] && echo true)" \
  "bypass actors are configured; the barrier is not absolute"

for rule in pull_request deletion non_fast_forward; do
  check "rule $rule is present" \
    "$(jq -e --arg r "$rule" 'any(.rules[]; .type == $r)' <<<"$STATE" >/dev/null && echo true)" \
    "the rule is missing from the applied ruleset"
done

if [ -n "$CONTEXTS" ]; then
  for ctx in $CONTEXTS; do
    check "check $ctx is required and bound to an app" \
      "$(jq -e --arg c "$ctx" --argjson a "${APP_ID:-0}" '
         any(.rules[]; .type=="required_status_checks"
             and any(.parameters.required_status_checks[];
                     .context == $c and .integration_id == $a))' <<<"$STATE" >/dev/null && echo true)" \
      "the context is absent or not bound to app ${APP_ID:-none}"
  done
fi

check "auto-merge is enabled on the repository" \
  "$(gh api "/repos/$REPO" --jq .allow_auto_merge 2>/dev/null)" \
  "queueing a merge will fail; enable it in the repository settings"

printf -- '---\nfailures: %s\n' "$FAILURES"
if [ "$FAILURES" -eq 0 ] && [ -z "$CONTEXTS" ]; then
  echo
  note "the barrier is in place but nothing is required to pass yet."
  note "merge the first pull request, then run this again to require the checks."
fi
[ "$FAILURES" -eq 0 ]
