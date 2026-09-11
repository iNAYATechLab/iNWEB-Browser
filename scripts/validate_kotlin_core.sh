#!/usr/bin/env bash
# =============================================================================
# iNWEB Browser — compile and run the pure-JVM core module tests.
#
# Uses pinned kotlinc + JUnit directly (no Gradle) so the exact same
# validation runs in the authoring sandbox, on CI runners, and on developer
# machines. Keeps in sync with src/core/browser-shell/build.gradle.kts.
#
# Toolchain cache: $HOME/.cache/inweb/kotlin (override with INWEB_KOTLIN_CACHE).
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
MODULE="$ROOT/src/core/browser-shell"

KOTLIN_VERSION="2.4.20"
JUNIT_VERSION="4.13.2"
HAMCREST_VERSION="1.3"

CACHE_DIR="${INWEB_KOTLIN_CACHE:-$HOME/.cache/inweb/kotlin}"
KOTLINC_HOME="$CACHE_DIR/kotlinc-$KOTLIN_VERSION"
KOTLINC="$KOTLINC_HOME/bin/kotlinc"
LIBS="$CACHE_DIR/libs"
BUILD_DIR="${TMPDIR:-/tmp}/inweb-core-build"

mkdir -p "$CACHE_DIR" "$LIBS" "$BUILD_DIR"

# --- toolchain: kotlinc at the pinned version -------------------------------
if [ ! -x "$KOTLINC" ]; then
  echo "[kotlin-core] downloading kotlin-compiler-$KOTLIN_VERSION ..."
  curl -sL --retry 3 --max-time 600 -o "$CACHE_DIR/kotlin-compiler.zip" \
    "https://github.com/JetBrains/kotlin/releases/download/v${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.zip"
  python3 -c "import zipfile; zipfile.ZipFile('$CACHE_DIR/kotlin-compiler.zip').extractall('$CACHE_DIR/tmp-extract')"
  mkdir -p "$KOTLINC_HOME"
  mv "$CACHE_DIR/tmp-extract/kotlinc"/* "$KOTLINC_HOME"/
  chmod +x "$KOTLINC_HOME/bin/"*
  rm -rf "$CACHE_DIR/tmp-extract" "$CACHE_DIR/kotlin-compiler.zip"
fi

# --- toolchain: JUnit + Hamcrest --------------------------------------------
JUNIT_JAR="$LIBS/junit-$JUNIT_VERSION.jar"
HAMCREST_JAR="$LIBS/hamcrest-core-$HAMCREST_VERSION.jar"
[ -f "$JUNIT_JAR" ] || curl -sL --retry 3 --max-time 120 -o "$JUNIT_JAR" \
  "https://repo1.maven.org/maven2/junit/junit/$JUNIT_VERSION/junit-$JUNIT_VERSION.jar"
[ -f "$HAMCREST_JAR" ] || curl -sL --retry 3 --max-time 120 -o "$HAMCREST_JAR" \
  "https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/$HAMCREST_VERSION/hamcrest-core-$HAMCREST_VERSION.jar"

STDLIB="$KOTLINC_HOME/lib/kotlin-stdlib.jar"

# --- compile module + tests ---------------------------------------------------
echo "[kotlin-core] compiling src/core/browser-shell (main + tests) ..."
rm -rf "$BUILD_DIR/classes"
mkdir -p "$BUILD_DIR/classes"
"$KOTLINC" \
  "$MODULE/src/main/kotlin" \
  "$MODULE/src/test/kotlin" \
  -cp "$JUNIT_JAR:$HAMCREST_JAR" \
  -d "$BUILD_DIR/classes"

# --- run tests ----------------------------------------------------------------
TEST_CLASSES=(
  com.inweb.browser.shell.TabStateTest
  com.inweb.browser.shell.TabsControllerTest
  com.inweb.browser.shell.SessionStoreTest
  com.inweb.browser.shell.OmniboxParserTest
  com.inweb.browser.shell.SearchEngineTest
  com.inweb.browser.shell.DownloadRecordTest
  com.inweb.browser.shell.SettingsTest
)

echo "[kotlin-core] running ${#TEST_CLASSES[@]} test classes ..."
java -cp "$BUILD_DIR/classes:$STDLIB:$JUNIT_JAR:$HAMCREST_JAR" \
  org.junit.runner.JUnitCore "${TEST_CLASSES[@]}"

echo "[kotlin-core] ALL TESTS PASSED"
