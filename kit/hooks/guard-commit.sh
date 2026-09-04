#!/usr/bin/env bash
# PreToolUse guard: refuses a git commit made while standing on the protected
# branch. The branch never appears in the command text, so it is resolved from
# repository state.
set -uo pipefail

GUARD_NAME="commit-guard"
. "${CLAUDE_PROJECT_DIR}/.claude/hooks/guard-common.sh"

load_vars
read_command
resolve_branch
tokenize

SEGMENT_START=1
WRAPPER_ARGS=0
IS_GIT=0
SUBCOMMAND=""
SKIP_NEXT=0

for token in "${TOKENS[@]}"; do
  if is_separator "$token"; then
    SEGMENT_START=1; WRAPPER_ARGS=0; IS_GIT=0; SUBCOMMAND=""; SKIP_NEXT=0
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
    [ "$token" = "git" ] && IS_GIT=1
    continue
  fi

  [ "$IS_GIT" -eq 1 ] || continue

  if [ "$SKIP_NEXT" -eq 1 ]; then SKIP_NEXT=0; continue; fi

  # Skip git global options until the subcommand appears.
  if [ -z "$SUBCOMMAND" ]; then
    case "$token" in
      -C|-c|--git-dir|--work-tree|--namespace|--exec-path) SKIP_NEXT=1; continue ;;
      -*) continue ;;
    esac
    # An expanded subcommand hides which git operation will run.
    has_expansion "$token" \
      && deny "The git subcommand is produced by an expansion. Write it literally so the guard can verify it."
    SUBCOMMAND="$token"
    # The message body may contain anything; only the branch decides.
    [ "$SUBCOMMAND" = "commit" ] && require_working_branch "Committing"
    continue
  fi
done

exit 0
