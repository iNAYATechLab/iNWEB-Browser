#!/usr/bin/env bash
# Host-side validation harness for src/native/adblock/ (patch 0005 sources).
#
# Compiles the native ad-block engine + its mirrored unit tests against
# shimmed Chromium APIs (base strings/locks, GURL, PSL table) and REAL
# RE2, then runs the tests. Catches C++ port bugs BEFORE the multi-hour
# b001-build-hop cycle. NOT part of the Chromium build or the patch —
# repo-side tooling only (ADR-040 quality loop).
#
# Requires: g++ (C++20), libre2-dev. Shim fidelity is documented in the
# shims' headers; authoritative validation is the Chromium build itself.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
ROOT="$HERE/root"
mkdir -p "$ROOT/chrome/android/inweb"
ln -sfn "$REPO/src/native/adblock" "$ROOT/chrome/android/inweb/adblock"
ln -sfn "$REPO/src/native/popup" "$ROOT/chrome/android/inweb/popup" 2>/dev/null || true
ln -sfn "$REPO/src/native/extensions" "$ROOT/chrome/android/inweb/extensions" 2>/dev/null || true
LIBRE2="$(find /usr/lib -name 'libre2.so' 2>/dev/null | head -1)"
if [ -z "$LIBRE2" ]; then
  echo "libre2.so not found — apt-get install libre2-dev" >&2
  exit 1
fi
# Chromium-only wrappers (need content/ or components/ headers) are
# excluded; their pure decision cores are what we test here.
mapfile -t ENGINE < <(ls "$REPO"/src/native/adblock/*.cc "$REPO"/src/native/popup/*.cc "$REPO"/src/native/extensions/*.cc 2>/dev/null | grep -v _unittest | grep -Ev 'inweb_redirect_throttle\.cc|inweb_notification_policy_ui_selector\.cc|inweb_cosmetic_injector\.cc' || true)
mapfile -t TESTS < <(ls "$REPO"/src/native/adblock/*_unittest.cc "$REPO"/src/native/popup/*_unittest.cc "$REPO"/src/native/extensions/*_unittest.cc 2>/dev/null)
# Style-plugin + include-path audit FIRST: fail fast locally on the bug
# classes that burn 30-minute CI hops (out-of-line ctor/dtor, invented
# include paths).
python3 "$HERE/audit_chromium_style.py" || exit 1

g++ -std=c++20 -I"$HERE/shims" -I"$ROOT" \
  "$HERE/shims/url/gurl.cc" "$HERE/shims/base/strings/string_util.cc" \
  "$HERE/shims/net/base/registry_controlled_domains/registry_controlled_domain.cc" \
  "${ENGINE[@]}" "${TESTS[@]}" "$HERE/main.cc" "$LIBRE2" -lcrypto -lz -o "$HERE/run_tests"
"$HERE/run_tests"
