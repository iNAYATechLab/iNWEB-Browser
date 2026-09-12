#!/usr/bin/env bash
# =============================================================================
# iNWEB Browser — compile and run the pure-JVM core module tests.
#
# Validates every module under src/core/ using pinned kotlinc + JUnit
# directly (no Gradle), so the exact same validation runs in the authoring
# sandbox, on CI runners, and on developer machines.
#
# Adding a core module: create src/core/<name>/ with src/{main,test}/kotlin,
# then register it (and its test classes) in the run_module calls below.
#
# Toolchain cache: $HOME/.cache/inweb/kotlin (override with INWEB_KOTLIN_CACHE).
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

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

# --- per-module compile + test ------------------------------------------------
run_module() {
  local module_dir="$1"
  shift
  local name
  name="$(basename "$module_dir")"
  local out="$BUILD_DIR/$name"

  echo ""
  echo "[kotlin-core:$name] compiling main + tests ..."
  rm -rf "$out"
  mkdir -p "$out"
  "$KOTLINC" \
    "$ROOT/$module_dir/src/main/kotlin" \
    "$ROOT/$module_dir/src/test/kotlin" \
    -cp "$JUNIT_JAR:$HAMCREST_JAR" \
    -d "$out"

  echo "[kotlin-core:$name] running $# test classes ..."
  java -cp "$out:$STDLIB:$JUNIT_JAR:$HAMCREST_JAR" \
    org.junit.runner.JUnitCore "$@"

  echo "[kotlin-core:$name] PASSED"
}

run_module "src/core/browser-shell" \
  com.inweb.browser.shell.TabStateTest \
  com.inweb.browser.shell.TabsControllerTest \
  com.inweb.browser.shell.SessionStoreTest \
  com.inweb.browser.shell.SessionManagerTest \
  com.inweb.browser.shell.OmniboxParserTest \
  com.inweb.browser.shell.SearchEngineTest \
  com.inweb.browser.shell.DownloadRecordTest \
  com.inweb.browser.shell.DownloadsStoreTest \
  com.inweb.browser.shell.HistoryStoreTest \
  com.inweb.browser.shell.FileHistoryStoreTest \
  com.inweb.browser.shell.BookmarkStoreTest \
  com.inweb.browser.shell.FileBookmarkStoreTest \
  com.inweb.browser.shell.TopSitesTest \
  com.inweb.browser.shell.SettingsTest

run_module "src/core/extensions" \
  com.inweb.browser.extensions.ExtensionRegistryTest

run_module "src/core/offline" \
  com.inweb.browser.offline.OfflineLibraryTest

run_module "src/core/vpn" \
  com.inweb.browser.vpn.VpnConfigParserTest

run_module "src/core/profiles" \
  com.inweb.browser.profiles.ProfileRegistryTest

run_module "src/core/backup" \
  com.inweb.browser.backup.BackupBundleTest

run_module "src/core/tracking-protection" \
  com.inweb.browser.privacy.FilterListParserTest \
  com.inweb.browser.privacy.RuleMatcherTest \
  com.inweb.browser.privacy.SecurityCenterTest \
  com.inweb.browser.privacy.CosmeticFilterTest \
  com.inweb.browser.privacy.CombinedMatcherTest \
  com.inweb.browser.privacy.TrackingProtectionEngineTest \
  com.inweb.browser.privacy.DomainClassifierTest \
  com.inweb.browser.privacy.lists.FileFilterListCacheTest \
  com.inweb.browser.privacy.lists.FilterListManagerTest \
  com.inweb.browser.privacy.lists.FilterListVersionTest \
  com.inweb.browser.privacy.lists.HttpFilterListFetcherTest \
  com.inweb.browser.privacy.lists.UpdatePolicyTest

echo ""
echo "[kotlin-core] ALL MODULES PASSED"
