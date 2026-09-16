#!/usr/bin/env bash
# Installs the kit into the repository it is run from. Everything the kit ships
# lives under kit/, so moving it to another repository is one directory copy
# followed by this script.
#
# Idempotent. Generated files are rewritten on every run; files you edit are
# created once and never overwritten.
set -uo pipefail

KIT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" \
  || { echo "install: not inside a git repository" >&2; exit 1; }
cd "$REPO_ROOT" || exit 1

fail() { printf 'install: %s\n' "$1" >&2; exit 1; }
note() { printf 'install: %s\n' "$1"; }

command -v jq >/dev/null 2>&1 \
  || fail "jq is required by the hooks. Install it and run this again."
command -v gh >/dev/null 2>&1 \
  || note "warning: gh is not installed. The cycle cannot reach the forge without it."

# --- generated, and ignored by git ------------------------------------------
# Rewritten on every run. kit/hooks is their only source, so editing the
# installed copy is lost work; doctor.sh reports the drift.
mkdir -p .claude/hooks .claude/kit-state
cp "$KIT_DIR"/hooks/*.sh .claude/hooks/
chmod +x .claude/hooks/*.sh
cp "$KIT_DIR/settings.json" .claude/settings.json
note "hooks and settings.json installed"

# --- copied, and committed to the repository --------------------------------
# The action that runs the review restores .claude from the protected branch
# because a pull request head is untrusted. If the subagents were generated and
# ignored, the protected branch would hold none and a pull request could not be
# reviewed. They are versioned for that reason.
mkdir -p .claude/agents .github/workflows
cp "$KIT_DIR"/agents/*.md .claude/agents/
cp "$KIT_DIR"/workflows/*.yml .github/workflows/
note "subagents and workflows installed; both must be committed"

# --- created once, then left alone ------------------------------------------
if [ -f .claude/kit.vars ]; then
  note "kit.vars already exists, left untouched"
else
  cp "$KIT_DIR/kit.vars.example" .claude/kit.vars
  note "kit.vars created from the template. Fill in the project commands."
fi

MARKER="# --- claude kit ---"
if [ -f .gitignore ] && grep -qF "$MARKER" .gitignore; then
  note ".gitignore already carries the kit block"
else
  cat "$KIT_DIR/gitignore.fragment" >> .gitignore
  note ".gitignore block appended"
fi

# --- workspace trust --------------------------------------------------------
# Trust is stored per absolute path in the user configuration, not in the
# repository, so it is lost on a clone or a rename. Without it, permissions.allow
# is ignored and a headless run denies everything while reporting success.
# The file is merged rather than replaced: it holds the rest of your setup.
CLAUDE_JSON="$HOME/.claude.json"
if [ -f "$CLAUDE_JSON" ]; then
  jq --arg p "$REPO_ROOT" '.projects[$p].hasTrustDialogAccepted = true' \
    "$CLAUDE_JSON" > "$CLAUDE_JSON.tmp" && mv "$CLAUDE_JSON.tmp" "$CLAUDE_JSON"
else
  jq -n --arg p "$REPO_ROOT" \
    '{projects: {($p): {hasTrustDialogAccepted: true}}}' > "$CLAUDE_JSON"
fi
note "workspace trusted for $REPO_ROOT"

# --- the review token -------------------------------------------------------
# claude setup-token is interactive, so the secret cannot be created from here.
# It is checked, because without it the review workflow fails on every run.
SECRET_PRESENT=no
if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
  gh secret list --json name --jq '.[].name' 2>/dev/null \
    | grep -qx CLAUDE_CODE_OAUTH_TOKEN && SECRET_PRESENT=yes
fi

echo
note "installed. What is left cannot be done from a script:"
echo
echo "  1. Fill in the project commands in .claude/kit.vars"
if [ "$SECRET_PRESENT" = "no" ]; then
  echo "  2. Create the review token secret, which does not exist yet:"
  echo "       claude setup-token"
  echo "       gh secret set CLAUDE_CODE_OAUTH_TOKEN"
else
  echo "  2. The review token secret is already set"
fi
cat <<'MSG'
  3. Commit .claude/agents, .claude/kit.vars, .github/workflows and kit/
  4. Apply the server barrier, which is the only layer the model cannot
     switch off:
       bash kit/github/apply-protection.sh
  5. Verify the whole installation:
       bash kit/doctor.sh
MSG
