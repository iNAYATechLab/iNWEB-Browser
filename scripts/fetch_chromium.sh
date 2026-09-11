#!/usr/bin/env bash
# =============================================================================
# iNWEB Browser — Chromium source fetch
#
# BUILD HOST ONLY (blocker B-001 — the authoring sandbox cannot run this).
# Reproduces a pristine upstream Chromium checkout at the pinned baseline tag
# with Android as a target OS, per upstream Android build instructions.
#
# Status: AUTHORED (Phase 1). First execution and validation happen on the
# build host; any fixes are committed back to this script.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Pinned tooling (kept in sync with ci/Dockerfile)
DEPOT_TOOLS_URL="https://chromium.googlesource.com/chromium/tools/depot_tools.git"
DEPOT_TOOLS_PINNED_REV="73fc5a4d6bd051f1fd58404e62dfb83f734b0cfa"

WORKSPACE="${INWEB_WORKSPACE:-$ROOT/chromium}"

# -----------------------------------------------------------------------------
# 1. Read the pinned Chromium tag
# -----------------------------------------------------------------------------
TAG="$(sed -n 's/^CHROMIUM_TAG=//p' "$ROOT/config/chromium/BASELINE" | tr -d '[:space:]')"
if [ -z "$TAG" ]; then
  echo "ERROR: CHROMIUM_TAG missing from config/chromium/BASELINE" >&2
  exit 2
fi
echo "iNWEB pinned Chromium tag: $TAG"

command -v git >/dev/null || { echo "ERROR: git is required" >&2; exit 2; }
command -v python3 >/dev/null || { echo "ERROR: python3 is required" >&2; exit 2; }

mkdir -p "$WORKSPACE"
cd "$WORKSPACE"

# -----------------------------------------------------------------------------
# 2. depot_tools at the pinned revision
# -----------------------------------------------------------------------------
if [ ! -d depot_tools ]; then
  echo "Cloning depot_tools..."
  git clone "$DEPOT_TOOLS_URL" depot_tools
fi
(
  cd depot_tools
  git fetch origin main --quiet
  git checkout --quiet "$DEPOT_TOOLS_PINNED_REV"
)
export PATH="$WORKSPACE/depot_tools:$PATH"

# -----------------------------------------------------------------------------
# 3. Chromium checkout (no-history keeps the checkout small and tag-reproducible)
# -----------------------------------------------------------------------------
if [ ! -d src ]; then
  echo "Fetching Chromium (no history)..."
  fetch --no-history chromium
fi

# Upstream instruction for Android: add android to target_os in .gclient
# ("the only difference between fetch android and fetch chromium").
if ! grep -q "'android'" .gclient; then
  printf "\ntarget_os = [ 'android' ]\n" >> .gclient
fi

# -----------------------------------------------------------------------------
# 4. Sync dependencies at the pinned tag (-D deletes stale dependencies)
# -----------------------------------------------------------------------------
echo "Syncing at src@$TAG (this is the long step)..."
gclient sync --revision "src@$TAG" --with_branch_heads -D

# -----------------------------------------------------------------------------
# 5. Confirm the checked-out revision
# -----------------------------------------------------------------------------
ACTUAL="$(git -C src rev-parse HEAD)"
echo "Chromium source ready:"
echo "  path    : $WORKSPACE/src"
echo "  tag     : $TAG"
echo "  HEAD    : $ACTUAL"
echo "Next: scripts/build_android.sh <channel>"
