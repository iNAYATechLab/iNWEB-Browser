#!/usr/bin/env bash
# iNWEB — per-turn workspace bootstrap.
#
# Two things do not survive the workspace snapshot boundary:
#   1. .git/config is excluded from snapshots (credential path) -> the "origin"
#      remote vanishes between turns, so push fails with
#      "'origin' does not appear to be a git repository".
#   2. the exec bit on ~/bin/gh is lost -> "Permission denied".
#
# Run this at the start of any turn that needs git/gh:
#   bash /home/user/work/bootstrap_git.sh
set -euo pipefail

GH=""
for c in "$HOME/bin/gh" /usr/local/bin/gh "$(command -v gh || true)"; do
  [ -n "$c" ] && [ -e "$c" ] && { GH="$c"; break; }
done
[ -n "$GH" ] || { echo "gh not found" >&2; exit 1; }
chmod +x "$GH" 2>/dev/null || true
export PATH="$(dirname "$GH"):$PATH"

"$GH" auth status >/dev/null 2>&1 && echo "gh: authenticated ($GH)"

cd /home/user/inweb-browser
git config user.email "build@inweb.local"
git config user.name "iNWEB Build Agent"
if ! git remote get-url origin >/dev/null 2>&1; then
  git remote add origin https://github.com/iNAYATechLab/iNWEB-Browser.git
  echo "origin: re-added"
else
  echo "origin: $(git remote get-url origin)"
fi
git config --global --add safe.directory /home/user/inweb-browser 2>/dev/null || true
echo "bootstrap OK"

# The E2E tree is its own git repo (pristine fixtures + applied series) and
# loses user.name/user.email the same way, since .git/config is not persisted.
if [ -d /home/user/work/e2e/tree/.git ]; then
  git -C /home/user/work/e2e/tree config user.email "e2e@inweb.local"
  git -C /home/user/work/e2e/tree config user.name "iNWEB E2E"
  echo "e2e tree: identity restored"
fi
