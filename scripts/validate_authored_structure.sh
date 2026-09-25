#!/usr/bin/env bash
# =============================================================================
# iNWEB Browser — structural syntax gate for the AUTHORED Android app sources.
#
# src/android-app compiles only inside the Chromium build (blocker B-001), so
# a plain redeclaration or parse error would otherwise surface months from
# now at the first real build (a defect of exactly this class was found by
# this check: two properties named `history` in BrowserViewModel, Step 33).
#
# The gate runs kotlinc over every authored .kt file WITHOUT dependencies:
# unresolved references are EXPECTED noise on this classpath (AndroidX and
# Compose are absent by design) — but STRUCTURAL errors (conflicting
# declarations, redeclarations, syntax errors) fail the check.
#
# Shares the validate_kotlin_core.sh toolchain cache; run that script first
# locally if the cache is cold. Runs in CI (Kotlin job) and standalone.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

KOTLIN_VERSION="2.4.20"
CACHE_DIR="${INWEB_KOTLIN_CACHE:-$HOME/.cache/inweb/kotlin}"
KOTLINC="$CACHE_DIR/kotlinc-$KOTLIN_VERSION/bin/kotlinc"

# Forbidden imports: the authored app layer compiles INSIDE the Chromium
# build, so it may only import what the pinned tree actually provides.
# Each entry below was checked against the pinned tag and is absent from
# it (docs/HOME-PAGE-HANDOFF.md A10), which means a file importing it
# compiles here and in CI and then fails in the real build — the exact
# class of defect this gate exists to catch early.
#
#   androidx.compose.material.icons   no icons artifact in third_party/androidx
#   androidx.compose.material.*       Material 2 absent (material3 is present)
FORBIDDEN='androidx\.compose\.material\.icons|androidx\.compose\.material\.[a-z0-9.]*'
SCAN_STATUS=0
while IFS= read -r file; do
  HITS="$(grep -nE "$FORBIDDEN" "$file" || true)"
  if [ -n "$HITS" ]; then
    echo "[authored-structure] FORBIDDEN IMPORT in ${file#"$ROOT"/}:"
    printf '%s\n' "$HITS"
    echo "[authored-structure]   (absent from the pinned Chromium tree — see A10)"
    SCAN_STATUS=1
  fi
done < <(find "$ROOT/src/android-app/src" -name "*.kt" | sort)
if [ "$SCAN_STATUS" -ne 0 ]; then
  echo "[authored-structure] forbidden-import scan FAILED"
  exit 1
fi

if [ ! -x "$KOTLINC" ]; then
  echo "[authored-structure] kotlinc not found at $KOTLINC —"
  echo "[authored-structure] run scripts/validate_kotlin_core.sh first (shared toolchain cache)."
  exit 1
fi

OUT_DIR="$(mktemp -d)"
trap 'rm -rf "$OUT_DIR"' EXIT

# Structural failures that mean a file cannot parse or declare cleanly,
# independent of any missing dependency (exact kotlinc wording, verified):
PATTERN='conflicting declarations|redeclaration|syntax error:'

STATUS=0
COUNT=0
while IFS= read -r file; do
  COUNT=$((COUNT + 1))
  OUT="$("$KOTLINC" -nowarn "$file" -d "$OUT_DIR" 2>&1 || true)"
  HITS="$(printf '%s' "$OUT" | grep -E "$PATTERN" || true)"
  if [ -n "$HITS" ]; then
    echo "[authored-structure] STRUCTURAL ERROR in ${file#"$ROOT"/}:"
    printf '%s\n' "$HITS"
    STATUS=1
  fi
done < <(find "$ROOT/src/android-app/src" -name "*.kt" | sort)

if [ "$COUNT" -eq 0 ]; then
  echo "[authored-structure] no authored sources found under src/android-app/src"
  exit 1
fi

if [ "$STATUS" -eq 0 ]; then
  echo "[authored-structure] $COUNT authored source file(s) structurally valid (dependency resolution excluded by design)"
fi
exit "$STATUS"
