#!/usr/bin/env bash
# SessionStart hook: states the repository policy and the issue in progress.
# Runs on startup, on every --resume, and after each compaction, so the facts
# are restored whenever the conversation loses them.
# Never blocks: missing state degrades the context, it does not stop the work.
set -uo pipefail

STATE_FILE="${CLAUDE_PROJECT_DIR}/.claude/kit-state/current-issue.json"
VARS_FILE="${CLAUDE_PROJECT_DIR}/.claude/kit.vars"

MAIN_BRANCH="main"
ISSUE_PREFIX="issue/"
MAINTENANCE_PREFIX="kit/"
if [ -r "$VARS_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  . "$VARS_FILE"
  set +a
  MAIN_BRANCH="${KIT_MAIN_BRANCH:-$MAIN_BRANCH}"
  ISSUE_PREFIX="${KIT_ISSUE_BRANCH_PREFIX:-$ISSUE_PREFIX}"
  MAINTENANCE_PREFIX="${KIT_MAINTENANCE_BRANCH_PREFIX:-$MAINTENANCE_PREFIX}"
fi

# The runner writes this file as JSON when it picks the next issue.
# It is read with jq and never sourced: the title comes from the forge and
# sourcing it would execute whatever an issue title happens to contain.
ISSUE_NUMBER=""; ISSUE_TITLE=""; ISSUE_BRANCH=""
if [ -r "$STATE_FILE" ] && command -v jq >/dev/null 2>&1; then
  ISSUE_NUMBER=$(jq -r '.number // empty' < "$STATE_FILE" 2>/dev/null || true)
  ISSUE_TITLE=$(jq -r '.title // empty' < "$STATE_FILE" 2>/dev/null || true)
  ISSUE_BRANCH=$(jq -r '.branch // empty' < "$STATE_FILE" 2>/dev/null || true)
fi

BRANCH=$(git symbolic-ref --quiet --short HEAD 2>/dev/null || echo "unknown")
PENDING=$(git status --porcelain 2>/dev/null | wc -l | tr -d ' ')

# Factual statements only. Text framed as out-of-band commands can trigger
# Claude's prompt-injection defences and be shown instead of used.
CONTEXT="Repository automation facts.
Protected branch: ${MAIN_BRANCH}. It changes only through a pull request that GitHub merges once the required checks pass.
Work branches: ${ISSUE_PREFIX}<number>-<slug> for an issue, ${MAINTENANCE_PREFIX}<slug> for repairs to the automation itself.
Guard hooks refuse commits and pushes from any other branch, force pushes, and pull request merges without --auto.
Current branch: ${BRANCH}. Uncommitted entries: ${PENDING}."

TITLE=""
if [ -n "$ISSUE_NUMBER" ]; then
  CONTEXT="${CONTEXT}
Issue in progress: #${ISSUE_NUMBER} ${ISSUE_TITLE}. Its branch is ${ISSUE_BRANCH}."
  TITLE="issue-${ISSUE_NUMBER}"
else
  CONTEXT="${CONTEXT}
No issue is recorded as in progress."
fi

# jq escapes the newlines and quotes; stdout carries the JSON and nothing else.
if command -v jq >/dev/null 2>&1; then
  if [ -n "$TITLE" ]; then
    jq -nc --arg c "$CONTEXT" --arg t "$TITLE" \
      '{hookSpecificOutput:{hookEventName:"SessionStart",additionalContext:$c,sessionTitle:$t}}'
  else
    jq -nc --arg c "$CONTEXT" \
      '{hookSpecificOutput:{hookEventName:"SessionStart",additionalContext:$c}}'
  fi
else
  # Plain stdout already reaches Claude for this event.
  printf '%s\n' "$CONTEXT"
fi
exit 0
