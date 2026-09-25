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
# Every step here is checked explicitly: hop 31 reported "success" while
# gn gen had failed with an unknown-function error, because the failure
# was not propagated and the hop went on to package state as if nothing
# had happened. A hop that cannot build must fail loudly.
python3 "$ROOT/scripts/apply_patches.py" apply "$WORKSPACE/src" ||
  { echo "FATAL: patch series failed to apply" >&2; exit 3; }
python3 "$ROOT/scripts/apply_patches.py" verify "$WORKSPACE/src" ||
  { echo "FATAL: patch series failed verification" >&2; exit 3; }

OUT_DIR="out/inweb-$CHANNEL"

# mtime discipline, part 2 — the part that actually works.
#
# The hop workflow normalizes the tree to a fixed epoch BEFORE this script
# runs, then this script applies the patch series, which rewrites every
# patched file with a CURRENT mtime. Those files are then newer than the
# resumed out/ artifacts, so siso/ninja treat every patched input as
# changed and re-execute the graph instead of resuming. Measured: hop 29
# advanced 220 edges and hop 30 only 49, both re-running the C++ and
# errorprone edges of the patched targets — the resumed state was buying
# almost nothing.
#
# Normalizing again here, with the patches already in place, is what makes
# the inputs permanently "not newer" than the artifacts. Keep it after the
# apply/verify pair above; moving it earlier silently disables it.
find "$WORKSPACE/src" -path "$OUT_DIR" -prune -o \
  -exec touch -h -c -d '2020-01-01 00:00:00 UTC' {} +
gn gen "$OUT_DIR" --args="$(cat "$ARGS_FILE")" ||
  { echo "FATAL: gn gen failed (see the error above)" >&2; exit 4; }

# Record build metadata for reproducibility (stamp consumed by CI).
{
  echo "chromium_tag=$(sed -n 's/^CHROMIUM_TAG=//p' "$ROOT/config/chromium/BASELINE" | tr -d '[:space:]')"
  echo "chromium_head=$(git rev-parse HEAD)"
  echo "channel=$CHANNEL"
  echo "gn_args_file=$ARGS_FILE"
  echo "tree_hash=$(python3 "$ROOT/scripts/apply_patches.py" hash "$WORKSPACE/src")"
  echo "built_at_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
} > "$OUT_DIR/iweb_build_metadata.txt"

# Build executor (measured, Step 50): autoninja dispatches to siso, whose
# incremental state (action/deps records) does not survive the chained-hop
# cross-runner out/ transfer — hops 4-6 each re-executed the whole reachable
# graph (~30k edges per box, out/ frozen at 24,545 .o). Plain ninja rests on
# mtime/size restat, which the transferred state satisfies (tree mtimes
# normalized to a fixed epoch by the hop workflow; out/ artifact mtimes
# preserved by tar), so the CI chain hops set INWEB_BUILD_TOOL=ninja.
# Default remains autoninja for normal build hosts. INWEB_BUILD_DRYRUN=1
# makes the ninja path plan only (-n) and report the would-build count.
INWEB_BUILD_TOOL="${INWEB_BUILD_TOOL:-autoninja}"
INWEB_BUILD_DRYRUN="${INWEB_BUILD_DRYRUN:-false}"
if [ "$INWEB_BUILD_TOOL" = "ninja" ]; then
  # Chromium's android_static_analysis=build_server steps (Java validate_deps
  # etc.) hard-require AUTONINJA_BUILD_ID even under plain ninja — without it
  # they raise "AUTONINJA_BUILD_ID is not set ... requires autoninja
  # integration" (hit at hop 13, edge ~10390). With the ID set but no
  # AUTONINJA_STDOUT_NAME, server_utils.MaybeRunCommand deliberately falls
  # back to normal LOCAL execution — verified against the pinned tag's
  # build/android/gyp/util/server_utils.py (154.0.8037.21, lines 55-66).
  export AUTONINJA_BUILD_ID="${AUTONINJA_BUILD_ID:-inweb-$$-$(date +%s)}"
  if [ "$INWEB_BUILD_DRYRUN" = "true" ] || [ "$INWEB_BUILD_DRYRUN" = "1" ]; then
    ninja -C "$OUT_DIR" -n chrome_public_apk > "$OUT_DIR/ninja_dryrun.txt"
    echo "ninja dry-run: $(wc -l < "$OUT_DIR/ninja_dryrun.txt") commands would run"
    echo "--- first 20 planned commands ---"
    head -20 "$OUT_DIR/ninja_dryrun.txt"
    echo "--- why dirty (explain, first 40) ---"
    ninja -C "$OUT_DIR" -n -d explain chrome_public_apk 2>&1 | head -40
  else
    ninja -C "$OUT_DIR" -j "${INWEB_NINJA_JOBS:-6}" chrome_public_apk
  fi
else
  autoninja -C "$OUT_DIR" chrome_public_apk
fi

echo "Build complete."
echo "  output dir : $OUT_DIR"
echo "  metadata   : $OUT_DIR/iweb_build_metadata.txt"
ls -lh "$OUT_DIR/apks/" || true
