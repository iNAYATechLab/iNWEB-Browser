#!/usr/bin/env bash
# iNWEB — rebuild the pinned E2E patch-application tree.
#
# /tmp is NOT persisted between agent turns, so the E2E tree lives under
# /home/user/work/e2e/tree instead. Content: the pristine upstream files at the
# pinned Chromium tag that the 12-patch series MODIFIES (new-file sections need
# nothing pre-existing), so `apply_patches.py apply|verify` can run end to end.
#
# Usage: bash /home/user/work/e2e/build_tree.sh
set -euo pipefail

TAG="154.0.8037.21"
BASE="https://chromium.googlesource.com/chromium/src/+/${TAG}"
TREE="/home/user/work/e2e/tree"

TEXT_PATHS=(
  chrome/android/java/res_chromium_base/drawable/themed_app_icon.xml
  chrome/android/java/res_chromium_base/values/channel_constants.xml
  chrome/android/expectations/lint-suppressions.xml
  chrome/browser/BUILD.gn
  chrome/browser/android/BUILD.gn
  chrome/browser/android/tab_web_contents_delegate_android.cc
  chrome/browser/chrome_content_browser_client.cc
  chrome/browser/download/BUILD.gn
  chrome/browser/download/chrome_download_manager_delegate.cc
  chrome/browser/permissions/BUILD.gn
  chrome/browser/permissions/chrome_permissions_client.cc
  chrome/browser/ui/android/strings/android_chrome_strings.grd
  chrome/test/BUILD.gn
  chrome/android/BUILD.gn
  build/config/android/internal_rules.gni
)

BIN_PATHS=(
  chrome/android/java/res_chromium_base/mipmap-hdpi/app_icon.png
  chrome/android/java/res_chromium_base/mipmap-hdpi/layered_app_icon.png
  chrome/android/java/res_chromium_base/mipmap-hdpi/layered_app_icon_background.png
  chrome/android/java/res_chromium_base/mipmap-mdpi/app_icon.png
  chrome/android/java/res_chromium_base/mipmap-mdpi/layered_app_icon.png
  chrome/android/java/res_chromium_base/mipmap-mdpi/layered_app_icon_background.png
  chrome/android/java/res_chromium_base/mipmap-xhdpi/app_icon.png
  chrome/android/java/res_chromium_base/mipmap-xhdpi/layered_app_icon.png
  chrome/android/java/res_chromium_base/mipmap-xhdpi/layered_app_icon_background.png
  chrome/android/java/res_chromium_base/mipmap-xxhdpi/app_icon.png
  chrome/android/java/res_chromium_base/mipmap-xxhdpi/layered_app_icon.png
  chrome/android/java/res_chromium_base/mipmap-xxhdpi/layered_app_icon_background.png
  chrome/android/java/res_chromium_base/mipmap-xxxhdpi/app_icon.png
  chrome/android/java/res_chromium_base/mipmap-xxxhdpi/layered_app_icon.png
  chrome/android/java/res_chromium_base/mipmap-xxxhdpi/layered_app_icon_background.png
)

# Resumable + parallel: each file is fetched only if it is not already
# present and non-empty, and the fetches run 6-wide because gitiles is
# slow enough that a 29-file serial walk can outlive a tool timeout.
# (The tree is small; correctness depends on CONTENT, not on order.)
fetch_one() {
  local p="$1"
  local out="$TREE/$p"
  if [ -s "$out" ]; then
    printf 'SKIP  %8d  %s\n' "$(wc -c < "$out")" "$p"
    return 0
  fi
  mkdir -p "$(dirname "$out")"
  curl -fsSL --retry 5 --retry-all-errors --retry-delay 2 --max-time 180 \
    "${BASE}/${p}?format=TEXT" | base64 -d > "$out"
  printf 'OK    %8d  %s\n' "$(wc -c < "$out")" "$p"
}
export -f fetch_one
export TREE BASE

mkdir -p "$TREE"
printf '%s\n' "${TEXT_PATHS[@]}" | xargs -P 6 -I{} bash -c 'fetch_one "$@"' _ {}
printf '%s\n' "${BIN_PATHS[@]}" | xargs -P 6 -I{} bash -c 'fetch_one "$@"' _ {}

cd "$TREE"
git init -q . 2>/dev/null || true
git config user.email e2e@inweb.local
git config user.name "iNWEB E2E"
git add -A
git commit -qm "pristine upstream @ ${TAG} (E2E fixtures only)"
echo "tree ready: $TREE"
