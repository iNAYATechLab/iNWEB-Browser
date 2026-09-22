#!/usr/bin/env bash
# =============================================================================
# iNWEB Browser — filter-engine micro-benchmark (MASTER-SPEC §9: measure,
# don't claim).
#
# Compiles the tracking-protection main sources + the benchmark main with the
# pinned kotlinc, then runs the synthetic EasyList-scale benchmark. Numbers
# are hardware- and JVM-specific; record them with hardware context
# (docs/phases/PHASE5-PERFORMANCE-DESIGN.md keeps the baseline log).
#
# Usage:   bash scripts/benchmark_filter_engine.sh [rules] [decisions]
# Default: 25000 rules, 200 decisions per workload.
#
# Not run in CI: benchmarks on shared runners produce noise, not data.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

KOTLIN_VERSION="2.4.20"
CACHE_DIR="${INWEB_KOTLIN_CACHE:-$HOME/.cache/inweb/kotlin}"
KOTLINC_HOME="$CACHE_DIR/kotlinc-$KOTLIN_VERSION"
KOTLINC="$KOTLINC_HOME/bin/kotlinc"
STDLIB="$KOTLINC_HOME/lib/kotlin-stdlib.jar"
BUILD_DIR="${TMPDIR:-/tmp}/inweb-benchmark-build"

MODULE_DIR="$ROOT/src/core/tracking-protection"
RULES="${1:-25000}"
DECISIONS="${2:-200}"

mkdir -p "$CACHE_DIR" "$BUILD_DIR"

if [ ! -x "$KOTLINC" ]; then
  echo "[benchmark] downloading kotlin-compiler-$KOTLIN_VERSION ..."
  curl -sL --retry 3 --max-time 600 -o "$CACHE_DIR/kotlin-compiler.zip" \
    "https://github.com/JetBrains/kotlin/releases/download/v${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.zip"
  python3 -c "import zipfile; zipfile.ZipFile('$CACHE_DIR/kotlin-compiler.zip').extractall('$CACHE_DIR/tmp-extract')"
  mkdir -p "$KOTLINC_HOME"
  mv "$CACHE_DIR/tmp-extract/kotlinc"/* "$KOTLINC_HOME"/
  chmod +x "$KOTLINC_HOME/bin/"*
  rm -rf "$CACHE_DIR/tmp-extract" "$CACHE_DIR/kotlin-compiler.zip"
fi

echo "[benchmark] compiling (rules=$RULES decisions=$DECISIONS) ..."
rm -rf "$BUILD_DIR/tracking-protection"
mkdir -p "$BUILD_DIR/tracking-protection"
"$KOTLINC" \
  "$MODULE_DIR/src/main/kotlin" \
  "$MODULE_DIR/src/benchmark/kotlin" \
  -d "$BUILD_DIR/tracking-protection"

echo "[benchmark] running on: $(uname -srm), $(nproc 2>/dev/null || echo '?') cpus, java $(java -version 2>&1 | head -1)"
java -Xmx1g -cp "$BUILD_DIR/tracking-protection:$STDLIB" \
  com.inweb.browser.privacy.FilterEngineBenchmark "$RULES" "$DECISIONS"
