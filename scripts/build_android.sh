#!/usr/bin/env bash
# =============================================================================
# iNWEB Browser — Android build
#
# BUILD HOST ONLY (blocker B-001). Generates the build directory with the
# committed GN arguments and builds the public APK target.
#
# Target note (honest scoping): until the custom `inweb_public_apk` target is
# introduced with the Phase 2 UI patches, this script builds the upstream
# `chrome_public_apk` target at the same GN configuration, proving the Android
# build pipeline end to end.
#
# Status: AUTHORED (Phase 1). First execution and validation happen on the
# build host; any fixes are committed back to this script.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

CHANNEL="${1:-development}"
case "$CHANNEL" in
  development|release) ;;
  *) echo "usage: $0 [development|release]" >&2; exit 2 ;;
esac

ARGS_FILE="$ROOT/config/chromium/args-$CHANNEL.gn"
if [ ! -f "$ARGS_FILE" ]; then
  echo "ERROR: missing GN args file: $ARGS_FILE" >&2
  exit 2
fi

WORKSPACE="${INWEB_WORKSPACE:-$ROOT/chromium}"
if [ ! -d "$WORKSPACE/src" ]; then
  echo "ERROR: Chromium source not found at $WORKSPACE/src" >&2
  echo "       run scripts/fetch_chromium.sh first" >&2
  exit 2
fi

export PATH="$WORKSPACE/depot_tools:$PATH"
cd "$WORKSPACE/src"

# Ensure the iNWEB patch series is applied before building (idempotent).
python3 "$ROOT/scripts/apply_patches.py" apply "$WORKSPACE/src"
python3 "$ROOT/scripts/apply_patches.py" verify "$WORKSPACE/src"

OUT_DIR="out/inweb-$CHANNEL"
gn gen "$OUT_DIR" --args="$(cat "$ARGS_FILE")"

# Record build metadata for reproducibility (stamp consumed by CI).
{
  echo "chromium_tag=$(sed -n 's/^CHROMIUM_TAG=//p' "$ROOT/config/chromium/BASELINE" | tr -d '[:space:]')"
  echo "chromium_head=$(git rev-parse HEAD)"
  echo "channel=$CHANNEL"
  echo "gn_args_file=$ARGS_FILE"
  echo "tree_hash=$(python3 "$ROOT/scripts/apply_patches.py" hash "$WORKSPACE/src")"
  echo "built_at_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
} > "$OUT_DIR/iweb_build_metadata.txt"

autoninja -C "$OUT_DIR" chrome_public_apk

echo "Build complete."
echo "  output dir : $OUT_DIR"
echo "  metadata   : $OUT_DIR/iweb_build_metadata.txt"
ls -lh "$OUT_DIR/apks/" || true
