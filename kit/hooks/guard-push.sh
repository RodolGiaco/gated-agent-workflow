#!/usr/bin/env bash
# PreToolUse guard: refuses any git push that targets the protected branch,
# rewrites history, or cannot be verified. Decides on resolved repository
# state, never on a keyword search over the command text.
set -uo pipefail

GUARD_NAME="push-guard"
. "${CLAUDE_PROJECT_DIR}/.claude/hooks/guard-common.sh"

load_vars
read_command
resolve_branch
tokenize

# Walk the token stream one command segment at a time.
# SEGMENT_START marks the position where the command word is expected.
SEGMENT_START=1   # next non-assignment, non-wrapper token is the command word
WRAPPER_ARGS=0    # a process wrapper was seen; its own arguments follow
IS_GIT=0          # current segment invokes git
SUBCOMMAND=""     # git subcommand once identified
SKIP_NEXT=0       # current token is the argument of a git global option
REMOTE_SEEN=0     # first positional after "push" is the remote
REFSPECS=0        # explicit refspecs given to push

for token in "${TOKENS[@]}"; do
  if is_separator "$token"; then
    SEGMENT_START=1; WRAPPER_ARGS=0; IS_GIT=0; SUBCOMMAND=""; SKIP_NEXT=0
    REMOTE_SEEN=0; REFSPECS=0
    continue
  fi

  # Identify the command word of the segment.
  if [ "$SEGMENT_START" -eq 1 ]; then
    is_assignment "$token" && continue
    # A wrapper runs what follows, so the command word is still ahead.
    is_wrapper "$token" && { WRAPPER_ARGS=1; continue; }
    # Skip the wrapper's own arguments (timeout 30, nice -n 10).
    if [ "$WRAPPER_ARGS" -eq 1 ]; then
      case "$token" in -*|[0-9]*) continue ;; esac
    fi
    WRAPPER_ARGS=0
    SEGMENT_START=0
    [ "$token" = "git" ] && IS_GIT=1
    continue
  fi

  [ "$IS_GIT" -eq 1 ] || continue

  # Consume the argument of a git global option such as -C <path>.
  if [ "$SKIP_NEXT" -eq 1 ]; then SKIP_NEXT=0; continue; fi

  # Skip git global options until the subcommand appears.
  if [ -z "$SUBCOMMAND" ]; then
    case "$token" in
      -C|-c|--git-dir|--work-tree|--namespace|--exec-path) SKIP_NEXT=1; continue ;;
      -*) continue ;;
    esac
    # An expanded subcommand hides which git operation will run.
    has_expansion "$token" \
      && deny "The git subcommand comes from an expansion. Write it literally so the guard can verify it."
    SUBCOMMAND="$token"
    # The branch only matters once we know this is a push.
    [ "$SUBCOMMAND" = "push" ] && require_working_branch "Pushing"
    continue
  fi

  [ "$SUBCOMMAND" = "push" ] || continue

  # An unresolvable value inside a push cannot be verified, so it is refused.
  has_expansion "$token" \
    && deny "The push contains a variable or command substitution. Write the literal remote and branch so the guard can verify the target."

  case "$token" in
    -f|--force|--force-with-lease|--force-with-lease=*|--force-if-includes)
      deny "Force push is not allowed. Shared history is never rewritten." ;;
    --delete|-d)
      deny "Deleting a remote branch is not allowed in this workflow." ;;
    --all|--mirror|--tags|--follow-tags)
      deny "This flag pushes refs beyond the current branch. Push the issue branch explicitly." ;;
    -*)
      continue ;;
    +*)
      deny "A refspec prefixed with + forces the update. Force push is not allowed." ;;
    *)
      if [ "$REMOTE_SEEN" -eq 0 ]; then REMOTE_SEEN=1; continue; fi
      REFSPECS=$((REFSPECS + 1))
      case "$token" in
        "$KIT_MAIN_BRANCH"|*:"$KIT_MAIN_BRANCH"|*/"$KIT_MAIN_BRANCH")
          deny "The push targets $KIT_MAIN_BRANCH. GitHub performs the merge into $KIT_MAIN_BRANCH." ;;
      esac ;;
  esac
done

# A push with no explicit refspec follows the upstream, which the text hides.
if [ "$IS_GIT" -eq 1 ] && [ "$SUBCOMMAND" = "push" ] && [ "$REFSPECS" -eq 0 ]; then
  UPSTREAM=$(git rev-parse --abbrev-ref '@{upstream}' 2>/dev/null || true)
  case "$UPSTREAM" in
    */"$KIT_MAIN_BRANCH")
      deny "The push has no refspec and the upstream of $CURRENT_BRANCH is $UPSTREAM." ;;
  esac
fi

exit 0
