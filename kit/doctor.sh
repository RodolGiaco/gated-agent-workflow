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

# Every hook aborts when one of these is missing.
MISSING=""
for key in KIT_MAIN_BRANCH KIT_ISSUE_BRANCH_PREFIX KIT_MAINTENANCE_BRANCH_PREFIX; do
  grep -qE "^${key}=." .claude/kit.vars 2>/dev/null || MISSING="$MISSING $key"
done
[ -z "$MISSING" ] \
  && pass "kit.vars defines every required key" \
  || fail "kit.vars defines every required key" "missing:$MISSING"

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
