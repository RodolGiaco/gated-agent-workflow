#!/usr/bin/env bash
# Verifies that the kit is installed, consistent and backed by the server rule.
# Reports every check; exits non-zero when any of them fails.
set -uo pipefail

KIT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" \
  || { echo "doctor: not inside a git repository" >&2; exit 1; }
cd "$REPO_ROOT" || exit 1

FAILURES=0
pass() { printf 'PASS  %s\n' "$1"; }
fail() { printf 'FAIL  %s\n      %s\n' "$1" "$2"; FAILURES=$((FAILURES + 1)); }

# Tools the hooks and the cycle depend on.
command -v jq >/dev/null 2>&1 \
  && pass "jq is installed" || fail "jq is installed" "required by every hook"
command -v gh >/dev/null 2>&1 \
  && pass "gh is installed" || fail "gh is installed" "required to reach the forge"
gh auth status >/dev/null 2>&1 \
  && pass "gh is authenticated" || fail "gh is authenticated" "run: gh auth login"

# The installed copy must match its source, or the kit ships one thing and
# the repository runs another.
if diff -rq "$KIT_DIR/hooks" .claude/hooks >/dev/null 2>&1; then
  pass "installed hooks match kit/hooks"
else
  fail "installed hooks match kit/hooks" "run: bash kit/install.sh"
fi
if diff -q "$KIT_DIR/settings.json" .claude/settings.json >/dev/null 2>&1; then
  pass "installed settings.json matches kit/settings.json"
else
  fail "installed settings.json matches kit/settings.json" "run: bash kit/install.sh"
fi

# The review gate delegates to these by name. Missing files fail the gate
# with a confusing message, so it is checked here instead.
MISSING_AGENTS=""
for a in code-reviewer acceptance-auditor; do
  [ -r ".claude/agents/$a.md" ] || MISSING_AGENTS="$MISSING_AGENTS $a"
done
[ -z "$MISSING_AGENTS" ] \
  && pass "review subagents are present" \
  || fail "review subagents are present" "missing:$MISSING_AGENTS"

# Subagents and workflows are copied out of the kit and committed, so the two
# copies can drift. The installed one is what runs.
for pair in "agents:.claude/agents" "workflows:.github/workflows"; do
  src="kit/${pair%%:*}"; dst="${pair##*:}"
  if diff -rq "$src" "$dst" >/dev/null 2>&1; then
    pass "installed ${pair%%:*} match $src"
  else
    fail "installed ${pair%%:*} match $src" "run: bash kit/install.sh"
  fi
done

# The review workflow fails on every run without this secret. Listing secrets
# needs admin rights, which the CI token does not have, so being unable to ask
# is reported as unknown instead of as missing: a check that cannot tell those
# two apart fails on every run and stops meaning anything.
SECRETS=$(gh secret list --json name --jq '.[].name' 2>/dev/null)
if [ -z "$SECRETS" ]; then
  printf 'SKIP  the review token secret is set\n      cannot list secrets here; this needs admin rights\n'
elif printf '%s' "$SECRETS" | grep -qx CLAUDE_CODE_OAUTH_TOKEN; then
  pass "the review token secret is set"
else
  fail "the review token secret is set" "run: claude setup-token, then gh secret set CLAUDE_CODE_OAUTH_TOKEN"
fi

# Every hook aborts when one of these is missing.
MISSING=""
for key in KIT_MAIN_BRANCH KIT_ISSUE_BRANCH_PREFIX KIT_MAINTENANCE_BRANCH_PREFIX; do
  grep -qE "^${key}=." .claude/kit.vars 2>/dev/null || MISSING="$MISSING $key"
done
[ -z "$MISSING" ] \
  && pass "kit.vars defines every required key" \
  || fail "kit.vars defines every required key" "missing:$MISSING"

# kit.vars is sourced by every hook and by CI. A value with spaces and no
# quotes runs its remainder as a command, which breaks every guard at once
# and only surfaces far from the edit that caused it.
BAD_QUOTING=$(grep -nE '^[A-Z_][A-Z0-9_]*=[^"'"'"'#]* ' .claude/kit.vars 2>/dev/null | cut -d: -f1 | tr '\n' ' ')
[ -z "$BAD_QUOTING" ] \
  && pass "kit.vars values with spaces are quoted" \
  || fail "kit.vars values with spaces are quoted" "unquoted value on line(s): $BAD_QUOTING"

# A permission rule anchored at a path that no longer exists grants nothing
# and fails silently. It has already happened twice after moving directories.
STALE=""
# Only allow rules are checked. A rule that grants a capability over a path
# that does not exist grants nothing and fails silently; a deny or ask rule
# over an absent path simply never fires, which is harmless and is how a rule
# anticipates a file the project has not created yet.
for path in $(jq -r '.permissions.allow[]? | select(test("^(Edit|Write|Read)\\(/"))' \
              .claude/settings.json 2>/dev/null \
              | sed -E 's/^[A-Za-z]+\(\///; s/\)$//; s/\*.*//; s#/$##' | sort -u); do
  [ -e "$path" ] || STALE="$STALE $path"
done
[ -z "$STALE" ] \
  && pass "permission rules point at paths that exist" \
  || fail "permission rules point at paths that exist" "missing:$STALE"

# Hook scripts are executed, not sourced.
NOT_EXEC=""
for f in .claude/hooks/*.sh; do
  [ -x "$f" ] || NOT_EXEC="$NOT_EXEC $(basename "$f")"
done
[ -z "$NOT_EXEC" ] \
  && pass "hook scripts are executable" \
  || fail "hook scripts are executable" "not executable:$NOT_EXEC"

# Session state must never reach the repository: it carries forge data.
git check-ignore -q .claude/kit-state 2>/dev/null \
  && pass ".claude/kit-state is ignored by git" \
  || fail ".claude/kit-state is ignored by git" "add it to .gitignore"

# The only barrier the model cannot switch off.
if gh ruleset list 2>/dev/null | grep -q active; then
  pass "an active ruleset protects the default branch"
else
  fail "an active ruleset protects the default branch" \
       "the local layers are evadable on their own; see kit/github/ruleset-main.json"
fi

printf -- '---\nfailures: %s\n' "$FAILURES"
[ "$FAILURES" -eq 0 ]
