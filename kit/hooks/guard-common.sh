#!/usr/bin/env bash
# Shared helpers for PreToolUse guard hooks.
# Sourced by every guard-*.sh; never invoked directly.
set -uo pipefail

# Abort with exit 2 when the guard cannot evaluate the request.
# Exit 2 blocks the tool call and hands stderr back to Claude.
fail_closed() {
  printf 'HOOK ERROR [%s]: %s\n' "${GUARD_NAME:-guard}" "$1" >&2
  exit 2
}

# Refuse the tool call with a structured PreToolUse decision.
# jq -n --arg escapes quotes, backslashes and newlines in the reason.
# Every refusal is recorded. Without that record there is no evidence for
# deciding later whether this guard still earns its maintenance cost.
deny() {
  local log_dir="${CLAUDE_PROJECT_DIR}/.claude/logs"
  mkdir -p "$log_dir" 2>/dev/null \
    && jq -nc \
         --arg at "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
         --arg guard "${GUARD_NAME:-guard}" \
         --arg branch "${CURRENT_BRANCH:-unknown}" \
         --arg command "${COMMAND:-unknown}" \
         --arg reason "$1" \
         '{at: $at, guard: $guard, branch: $branch, command: $command, reason: $reason}' \
       >> "$log_dir/guard-denials.jsonl" 2>/dev/null

  jq -nc --arg reason "$1" '{
    hookSpecificOutput: {
      hookEventName: "PreToolUse",
      permissionDecision: "deny",
      permissionDecisionReason: $reason
    }
  }'
  exit 0
}

# Load the single per-project variables file. Missing or empty values abort.
load_vars() {
  local file="${CLAUDE_PROJECT_DIR}/.claude/kit.vars"
  [ -r "$file" ] || fail_closed "cannot read $file"
  set -a
  # shellcheck disable=SC1090
  . "$file"
  set +a
  [ -n "${KIT_MAIN_BRANCH:-}" ] \
    || fail_closed "KIT_MAIN_BRANCH is not defined in kit.vars"
  [ -n "${KIT_ISSUE_BRANCH_PREFIX:-}" ] \
    || fail_closed "KIT_ISSUE_BRANCH_PREFIX is not defined in kit.vars"
  [ -n "${KIT_MAINTENANCE_BRANCH_PREFIX:-}" ] \
    || fail_closed "KIT_MAINTENANCE_BRANCH_PREFIX is not defined in kit.vars"
}

# Refuse a write operation performed from a branch the cycle does not own.
# Called only once the subcommand is known, so read-only git commands keep
# working from any branch, including the protected one.
require_working_branch() {
  local operation="$1"
  local issue_pattern="^${KIT_ISSUE_BRANCH_PREFIX}[0-9]+-[a-z0-9][a-z0-9._-]*$"
  local maintenance_pattern="^${KIT_MAINTENANCE_BRANCH_PREFIX}[a-z0-9][a-z0-9._-]*$"

  [ "$CURRENT_BRANCH" != "$KIT_MAIN_BRANCH" ] \
    || deny "$operation on $KIT_MAIN_BRANCH is not allowed. $KIT_MAIN_BRANCH only changes through a pull request merged by GitHub."

  [[ "$CURRENT_BRANCH" =~ $issue_pattern ]] && return 0
  [[ "$CURRENT_BRANCH" =~ $maintenance_pattern ]] && return 0

  deny "$operation on branch '$CURRENT_BRANCH' is not allowed. Use ${KIT_ISSUE_BRANCH_PREFIX}<number>-<slug> to work an issue, or ${KIT_MAINTENANCE_BRANCH_PREFIX}<slug> to repair the kit itself."
}

# Read the Bash command from the hook JSON on stdin into COMMAND.
read_command() {
  command -v jq >/dev/null 2>&1 \
    || fail_closed "jq is not installed and is required by the hooks"
  local input
  input=$(cat)
  COMMAND=$(
    printf '%s' "$input" \
      | jq -er '.tool_input.command | select(type == "string" and length > 0)'
  ) || fail_closed "could not read a non-empty tool_input.command"
}

# Resolve the checked-out branch into CURRENT_BRANCH.
# symbolic-ref succeeds on an unborn branch; rev-parse HEAD does not.
resolve_branch() {
  CURRENT_BRANCH=$(git symbolic-ref --quiet --short HEAD 2>/dev/null) \
    || fail_closed "HEAD is detached; the cycle only works on issue branches"
  [ -n "$CURRENT_BRANCH" ] || fail_closed "the current branch resolved to an empty string"
}

# Split COMMAND into TOKENS, forcing shell separators to stand alone so that
# a trailing separator cannot hide inside a value ("main;" reads as "main" ";").
tokenize() {
  local spaced
  spaced=$(printf '%s' "$COMMAND" | sed -E 's/(\|\||&&|[;|&])/ \1 /g')
  read -r -a TOKENS <<< "$spaced"
}

# True when the token is a shell separator that starts a new command segment.
is_separator() {
  case "$1" in '&&'|'||'|';'|'|'|'&') return 0 ;; *) return 1 ;; esac
}

# True when the token is a leading VAR=value assignment.
is_assignment() {
  case "$1" in [A-Za-z_]*=*) return 0 ;; *) return 1 ;; esac
}

# True when the token is a process wrapper that runs the rest as a command.
is_wrapper() {
  case "$1" in timeout|time|nice|nohup|stdbuf|command|builtin) return 0 ;; *) return 1 ;; esac
}

# True when the token carries a value the guard cannot resolve statically.
has_expansion() {
  case "$1" in *'$'*|*'`'*) return 0 ;; *) return 1 ;; esac
}