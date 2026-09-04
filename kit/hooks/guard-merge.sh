#!/usr/bin/env bash
# PreToolUse guard: refuses a pull request merge that skips the queue or
# bypasses required checks. Only "gh pr merge --auto" is acceptable, because
# the merge itself must be performed by GitHub once the checks pass.
set -uo pipefail

GUARD_NAME="merge-guard"
. "${CLAUDE_PROJECT_DIR}/.claude/hooks/guard-common.sh"

load_vars
read_command
tokenize

SEGMENT_START=1
WRAPPER_ARGS=0
IS_GH=0
WORDS=0        # positional words seen after gh: 1 = pr, 2 = merge
IS_MERGE=0
HAS_AUTO=0
SEGMENT_TOKENS=""

# Evaluate the gh pr merge segment that just ended.
check_merge_segment() {
  [ "$IS_MERGE" -eq 1 ] || return 0
  [ "$HAS_AUTO" -eq 1 ] \
    || deny "gh pr merge without --auto merges immediately. Use --auto so GitHub merges only after the required checks pass."
}

for token in "${TOKENS[@]}"; do
  if is_separator "$token"; then
    check_merge_segment
    SEGMENT_START=1; WRAPPER_ARGS=0; IS_GH=0; WORDS=0; IS_MERGE=0; HAS_AUTO=0
    continue
  fi

  if [ "$SEGMENT_START" -eq 1 ]; then
    is_assignment "$token" && continue
    is_wrapper "$token" && { WRAPPER_ARGS=1; continue; }
    if [ "$WRAPPER_ARGS" -eq 1 ]; then
      case "$token" in -*|[0-9]*) continue ;; esac
    fi
    WRAPPER_ARGS=0
    SEGMENT_START=0
    [ "$token" = "gh" ] && IS_GH=1
    continue
  fi

  [ "$IS_GH" -eq 1 ] || continue

  case "$token" in
    --admin)
      [ "$IS_MERGE" -eq 1 ] \
        && deny "gh pr merge --admin bypasses the required checks. The branch ruleset is the control point and is not to be skipped." ;;
    --auto)
      HAS_AUTO=1; continue ;;
    -*)
      continue ;;
    *)
      # An expanded word can hide the subcommand or the flags.
      has_expansion "$token" \
        && { [ "$IS_MERGE" -eq 1 ] && deny "The merge command contains an expansion. Write the flags literally so the guard can verify them." ; continue ; }
      WORDS=$((WORDS + 1))
      if [ "$WORDS" -eq 1 ] && [ "$token" != "pr" ]; then IS_GH=0; continue; fi
      if [ "$WORDS" -eq 2 ] && [ "$token" = "merge" ]; then IS_MERGE=1; fi
      if [ "$WORDS" -eq 2 ] && [ "$token" != "merge" ]; then IS_GH=0; fi
      continue ;;
  esac
done

check_merge_segment
exit 0
