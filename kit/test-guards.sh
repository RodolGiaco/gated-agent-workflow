#!/usr/bin/env bash
# Offline acceptance battery for every PreToolUse guard.
# Builds throwaway repositories so branch-dependent rules can be exercised
# in both contexts. Never starts a Claude session.
set -u
SOURCE_HOOKS="$PWD/.claude/hooks"
SOURCE_VARS="$PWD/.claude/kit.vars"
FAILURES=0

# Run one case against one guard and compare with the expected outcome.
check() {
  local guard="$1" expected="$2" cmd="$3" out rc got
  out=$(jq -nc --arg c "$cmd" '{tool_name:"Bash",tool_input:{command:$c}}' \
        | "$CLAUDE_PROJECT_DIR/.claude/hooks/$guard" 2>&1)
  rc=$?
  if [ "$rc" = 2 ]; then got="exit2"
  elif [ "$rc" = 0 ] && printf '%s' "$out" | grep -q '"permissionDecision":"deny"'; then got="deny"
  elif [ "$rc" = 0 ]; then got="allow"
  else got="rc$rc"; fi
  if [ "$got" = "$expected" ]; then
    printf 'PASS  %-5s %-12s %s\n' "$got" "${guard%.sh}" "$cmd"
  else
    printf 'FAIL  expected=%-5s got=%-5s %-12s %s\n      %s\n' \
      "$expected" "$got" "${guard%.sh}" "$cmd" "$out"
    FAILURES=$((FAILURES + 1))
  fi
}

# Create a throwaway repository checked out on the given branch.
make_repo() {
  local branch="$1" dir
  dir=$(mktemp -d)
  git init -q -b "$branch" "$dir"
  mkdir -p "$dir/.claude/hooks"
  cp "$SOURCE_HOOKS"/*.sh "$dir/.claude/hooks/"
  cp "$SOURCE_VARS" "$dir/.claude/kit.vars"
  export CLAUDE_PROJECT_DIR="$dir"
  cd "$dir" || exit 1
}

ORIGIN="$PWD"

echo "=== on the issue branch ==="
make_repo issue/00001-example
# Only the issue branch is a valid push target.
check guard-push.sh   allow "git push origin issue/00001-example"
# Every spelling that reaches the protected branch.
check guard-push.sh   deny  "git push origin main"
check guard-push.sh   deny  "git push -u origin main"
check guard-push.sh   deny  "git push origin HEAD:main"
check guard-push.sh   deny  "git push origin refs/heads/main"
check guard-push.sh   deny  "git push origin main; ls"
# History rewriting and bulk pushes.
check guard-push.sh   deny  "git push --force origin issue/00001-example"
check guard-push.sh   deny  "git push origin +main"
check guard-push.sh   deny  "git push --all origin"
check guard-push.sh   deny  "git push --mirror origin"
check guard-push.sh   deny  "git push --delete origin issue/00001-example"
# Forms that hide the subcommand from a naive parser.
check guard-push.sh   deny  "git -C . push origin main"
check guard-push.sh   deny  "git --no-pager push origin main"
check guard-push.sh   deny  "FOO=bar git push origin main"
check guard-push.sh   deny  "timeout 30 git push origin main"
check guard-push.sh   deny  "nice -n 10 git push origin main"
# Values the guard cannot resolve.
check guard-push.sh   deny  "git push origin \$BRANCH"
# The word main in unrelated text must not block a valid push.
check guard-push.sh   allow "git commit -m \"merge main into branch\" && git push origin issue/00001-example"
check guard-push.sh   allow "echo git push origin main"
# Unrelated commands pass untouched, expansions included.
check guard-push.sh   allow "echo \$(date)"
check guard-push.sh   allow "npm run build -- --env=\$NODE_ENV"
check guard-push.sh   allow "timeout 30 npm test"
check guard-push.sh   allow "ls -la"
# Commits are free on the issue branch, whatever the message says.
check guard-commit.sh allow "git commit -m \"fix\""
check guard-commit.sh allow "git commit -m \"revert main behaviour\""
check guard-commit.sh allow "git add . && git commit -m 'x'"
check guard-commit.sh allow "git commit -m \"var \$HOME\""
check guard-commit.sh allow "echo git commit"
check guard-commit.sh deny  "git \$(echo commit) -m x"
# Merges must be queued, never immediate, never bypassing checks.
check guard-merge.sh  allow "gh pr merge --auto --squash 12"
check guard-merge.sh  deny  "gh pr merge 12"
check guard-merge.sh  deny  "gh pr merge --squash 12"
check guard-merge.sh  deny  "gh pr merge --admin --auto 12"
check guard-merge.sh  deny  "git push origin issue/x && gh pr merge 12"
check guard-merge.sh  allow "git push origin issue/x && gh pr merge --auto 12"
check guard-merge.sh  deny  "timeout 30 gh pr merge 12"
check guard-merge.sh  deny  "gh pr merge --auto \$PR"
check guard-merge.sh  allow "gh pr create --fill"
check guard-merge.sh  allow "gh issue list"
check guard-merge.sh  allow "echo gh pr merge 12"

cd "$ORIGIN" || exit 1
echo "=== standing on the protected branch ==="
make_repo main
# Writes are refused from the protected branch.
check guard-commit.sh deny  "git commit -m \"fix\""
check guard-commit.sh deny  "git -C . commit -m x"
check guard-commit.sh deny  "git add . && git commit -m x"
check guard-push.sh   deny  "git push origin issue/00002-new"
# Read-only git and branch creation must keep working, or the cycle
# can never leave the protected branch to start an issue.
check guard-commit.sh allow "git status --short"
check guard-push.sh   allow "git status --short"
check guard-push.sh   allow "git log --oneline -3"
check guard-push.sh   allow "git fetch origin"
check guard-push.sh   allow "git switch -c issue/00002-new"

cd "$ORIGIN" || exit 1
echo "=== on a branch the cycle does not own ==="
make_repo develop
check guard-commit.sh deny  "git commit -m \"fix\""
check guard-push.sh   deny  "git push origin develop"
check guard-push.sh   allow "git fetch origin"

cd "$ORIGIN" || exit 1
echo "=== on a branch whose name breaks the issue shape ==="
make_repo issue/no-number
check guard-commit.sh deny  "git commit -m \"fix\""

cd "$ORIGIN" || exit 1
echo "=== on the maintenance lane ==="
make_repo kit/fix-push-guard
# Repairing the kit is not an issue, so it needs a lane of its own.
check guard-commit.sh allow "git commit -m \"fix the push guard\""
check guard-push.sh   allow "git push origin kit/fix-push-guard"
check guard-push.sh   deny  "git push origin main"

cd "$ORIGIN" || exit 1
printf -- '---\nfailures: %s\n' "$FAILURES"
[ "$FAILURES" -eq 0 ]
