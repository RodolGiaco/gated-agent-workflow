#!/usr/bin/env bash
# Runs one issue end to end: branch, implementation, push, pull request and
# queued merge. Takes the issue number as an explicit argument; it never infers
# which issue to work on.
#
# The run ends in exactly one declared state: merged, queued, or open with a
# reason. It never reports success it did not observe.
set -uo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" \
  || { echo "runner: not inside a git repository" >&2; exit 1; }
cd "$REPO_ROOT" || exit 1

ISSUE_NUMBER="${1:-}"
[ -n "$ISSUE_NUMBER" ] || { echo "usage: bash kit/run-issue.sh <issue-number>" >&2; exit 1; }
case "$ISSUE_NUMBER" in *[!0-9]*) echo "runner: the issue number must be digits" >&2; exit 1 ;; esac

step() { printf '\n=== %s\n' "$1"; }
die()  { printf 'runner: %s\n' "$1" >&2; exit 1; }

set -a
# shellcheck disable=SC1091
. .claude/kit.vars || die "cannot read .claude/kit.vars"
set +a
: "${KIT_MAIN_BRANCH:?not set in kit.vars}"
: "${KIT_ISSUE_BRANCH_PREFIX:?not set in kit.vars}"

# ---------------------------------------------------------------- preflight --
# Every command the run depends on is checked before anything is mutated,
# because a run that fails halfway leaves a branch and a state file behind.
step "Preflight"
bash kit/doctor.sh || die "doctor reported failures; fix them before running an issue"

git diff --quiet && git diff --cached --quiet \
  || die "the working tree has uncommitted changes; the run would mix them into the issue"

# ------------------------------------------------------------------- branch --
step "Branch"
git fetch origin --quiet || die "git fetch failed"

TITLE=$(gh issue view "$ISSUE_NUMBER" --json title --jq .title) \
  || die "cannot read issue $ISSUE_NUMBER from the forge"
[ -n "$TITLE" ] || die "issue $ISSUE_NUMBER has an empty title"

# The slug only feeds the branch name, so anything outside the allowed set is
# dropped rather than escaped. The guards enforce the resulting shape.
SLUG=$(printf '%s' "$TITLE" \
  | tr '[:upper:]' '[:lower:]' \
  | sed -E 's/[^a-z0-9]+/-/g; s/^-+//; s/-+$//' \
  | cut -c1-40 | sed -E 's/-+$//')
[ -n "$SLUG" ] || die "the issue title produced an empty slug"

BRANCH="${KIT_ISSUE_BRANCH_PREFIX}${ISSUE_NUMBER}-${SLUG}"
git switch -c "$BRANCH" "origin/${KIT_MAIN_BRANCH}" --quiet \
  || die "cannot create $BRANCH from origin/${KIT_MAIN_BRANCH}"

# A mutation is not applied until it is read back.
ACTUAL=$(git symbolic-ref --quiet --short HEAD)
[ "$ACTUAL" = "$BRANCH" ] || die "expected to be on $BRANCH, found $ACTUAL"
echo "on $BRANCH"

# -------------------------------------------------------------------- state --
# Read by the SessionStart hook. Written with jq because the title comes from
# the forge and is not trusted input.
mkdir -p .claude/kit-state
jq -nc --arg n "$ISSUE_NUMBER" --arg t "$TITLE" --arg b "$BRANCH" \
  '{number: ($n|tonumber), title: $t, branch: $b}' \
  > .claude/kit-state/current-issue.json \
  || die "cannot write the session state"

# --------------------------------------------------------------- the session --
step "Session"
RESULT=$(mktemp)
trap 'rm -f "$RESULT"' EXIT

# No --bare: the run needs the project hooks, agents and permission rules.
# dontAsk because nobody is here to answer a prompt.
claude -p "Work issue #${ISSUE_NUMBER} to completion on the current branch.
Read the issue with gh issue view ${ISSUE_NUMBER}. Implement every acceptance
criterion it states. Commit your work on this branch with a message that names
the issue. Delegate to the code-reviewer and acceptance-auditor subagents before
your final commit and address any blocking finding they return. Do not push and
do not open a pull request: the runner does that." \
  --permission-mode dontAsk \
  --output-format json > "$RESULT"

# is_error is false when a permission denial stops a command, so the exit code
# and that field both lie. The denial list is the evidence.
DENIALS=$(jq -r '.permission_denials | length' < "$RESULT" 2>/dev/null || echo "unknown")
COST=$(jq -r '.total_cost_usd // 0' < "$RESULT" 2>/dev/null || echo 0)
printf 'cost: %s USD   denials: %s\n' "$COST" "$DENIALS"

if [ "$DENIALS" != "0" ]; then
  jq -r '.permission_denials[] | "  denied: " + (.tool_input.command // .tool_name)' < "$RESULT"
  die "the session was blocked; the branch is left in place for inspection"
fi

COMMITS=$(git rev-list --count "origin/${KIT_MAIN_BRANCH}..HEAD")
[ "$COMMITS" -gt 0 ] || die "the session produced no commit; nothing to push"
echo "commits on the branch: $COMMITS"

# --------------------------------------------------------------------- push --
# The runner pushes, not the model: this step has to be auditable in the log.
step "Push and pull request"
git push -u origin "$BRANCH" --quiet || die "push rejected"

PR=$(gh pr create --fill --base "$KIT_MAIN_BRANCH" --head "$BRANCH" --json number --jq .number 2>/dev/null) \
  || PR=$(gh pr list --head "$BRANCH" --json number --jq '.[0].number')
[ -n "$PR" ] || die "cannot determine the pull request number"
echo "pull request #$PR"

# ------------------------------------------------------------------- merge --
# Measured on this repository: the auto-merge request does not survive the
# cycle of a failing check followed by a fix. It is requested and then read
# back, and requested again when it did not stick.
step "Queue the merge"
QUEUED=no
for attempt in 1 2 3; do
  gh pr merge "$PR" --auto --squash >/dev/null 2>&1 || true
  sleep 5
  if [ "$(gh pr view "$PR" --json autoMergeRequest --jq '.autoMergeRequest != null')" = "true" ]; then
    QUEUED=yes
    break
  fi
  if [ "$(gh pr view "$PR" --json state --jq .state)" = "MERGED" ]; then
    QUEUED=merged
    break
  fi
  echo "auto-merge did not stick on attempt $attempt"
done

# -------------------------------------------------------------------- state --
step "Result"
STATE=$(gh pr view "$PR" --json state --jq .state)
case "$STATE:$QUEUED" in
  MERGED:*)
    echo "MERGED   issue #$ISSUE_NUMBER, pull request #$PR"
    rm -f .claude/kit-state/current-issue.json
    exit 0 ;;
  OPEN:yes)
    echo "QUEUED   issue #$ISSUE_NUMBER, pull request #$PR waits for the required checks"
    exit 0 ;;
  *)
    echo "OPEN     issue #$ISSUE_NUMBER, pull request #$PR is not queued"
    gh pr view "$PR" --json mergeStateStatus,statusCheckRollup \
      --jq '{mergeStateStatus, checks: [.statusCheckRollup[] | {name, conclusion}]}'
    exit 1 ;;
esac