#!/usr/bin/env bash
# Installs the kit into the repository it is run from.
# Idempotent: generated files are rewritten, edited files are never overwritten.
# Run from the repository root.
set -uo pipefail

KIT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" \
  || { echo "install: not inside a git repository" >&2; exit 1; }
cd "$REPO_ROOT" || exit 1

fail() { printf 'install: %s\n' "$1" >&2; exit 1; }
note() { printf 'install: %s\n' "$1"; }

command -v jq >/dev/null 2>&1 \
  || fail "jq is required by the hooks. Install it and run this again."

# Generated: always rewritten from the kit, never edited in place.
mkdir -p .claude/hooks .claude/kit-state
cp "$KIT_DIR"/hooks/*.sh .claude/hooks/
chmod +x .claude/hooks/*.sh
note "hooks installed from kit/hooks"

cp "$KIT_DIR/settings.json" .claude/settings.json
note "settings.json installed from kit/settings.json"

# Edited by you: created once from the template, then left alone.
if [ -f .claude/kit.vars ]; then
  note "kit.vars already exists, left untouched"
else
  cp "$KIT_DIR/kit.vars.example" .claude/kit.vars
  note "kit.vars created from the template. Review its values before working."
fi

# Appended once, recognised by its marker.
MARKER="# --- claude kit ---"
if [ -f .gitignore ] && grep -qF "$MARKER" .gitignore; then
  note ".gitignore already carries the kit block"
else
  cat "$KIT_DIR/gitignore.fragment" >> .gitignore
  note ".gitignore block appended"
fi

# The trust dialog cannot be accepted from a script. Project allow rules are
# ignored until it is, so a headless run would deny everything without it.
cat <<'MSG'

install: one manual step remains.
  Run "claude" once in this directory and accept the workspace trust dialog.
  Until then, permissions.allow from .claude/settings.json is not applied.
Then run: bash kit/doctor.sh
MSG
