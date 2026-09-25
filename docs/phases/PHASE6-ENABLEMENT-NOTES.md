# PHASE6 — 0014 enablement map (pinned tree @154.0.8037.21)

Engineering notes for `0014-extension-enable-android`, verified against pinned
Chromium commit `0fa6d91e2faf313da8332688a96bd98b9d983a4d` on 2026-09-25.

## 1. Flag mechanics (inspected)

- `extensions/buildflags/buildflags.gni` currently defines:
  - `enable_extensions = !is_android && !is_ios && !is_castos && !is_fuchsia`;
  - `enable_desktop_android_extensions = is_desktop_android` as an explicitly
    experimental, unstable Android extension path;
  - `enable_extensions_core = enable_extensions ||
    enable_desktop_android_extensions`;
  - `enable_platform_apps` and `enable_hosted_apps` follow the full desktop
    `enable_extensions` flag and remain false for the iNWEB phone build.
- Patch `0014` adds a narrow, default-false
  `enable_inweb_android_extensions` argument and folds only that argument into
  `enable_desktop_android_extensions`.
- The iNWEB development and release GN configs set the new argument to true.
  They do **not** set the broader `enable_extensions` flag.
- `components/guest_view/buildflags/buildflags.gni` already defaults
  `enable_guest_view` to true on Android; no duplicate iNWEB override is needed.

## 2. Existing Android implementation (inspected)

The pinned tree already contains Android management/developer bridges,
action-popup contents, toolbar coordination, app-menu entries, options routing,
resources, JNI headers, and tests. iNWEB does not add parallel placeholder
surfaces.

The critical honesty finding is that the pinned
`ExtensionUiBackendImpl.isEnabled()` returns true whenever this implementation
is compiled. Patch `0016` therefore changes that backend to require the
explicit `--enable-inweb-extension-ui` verification switch. Compilation is not
treated as proof of runtime support.

## 3. Prepared patch sequence

1. `0014` — narrow compile-time opt-in and artifact API-audit tool.
2. `0015` — CRX3/ZIP package inspector, Chromium build/test hooks, and removal
   of the Chrome Web Store submenu row so discovery is not represented as a
   supported installation path.
3. `0016` — default-closed gate around Chromium's existing Android extension
   surfaces; opt-in is for device verification only.
4. `0017` — deterministic first-install/update/permission/MV2 review policy
   and tests. It is a decision model, not a claim that runtime installation is
   wired.

The patches were generated after the current non-extension build hop had
started and therefore do not alter that hop.

## 4. Build and release gates

The patch series remains prepared but unproven until all of the following are
green on the exact pin:

1. independent and cumulative patch apply/reverse/re-apply;
2. GN generation and affected Java/C++ compilation;
3. Chromium Android extension unit/instrumentation tests;
4. APK production and artifact API audit;
5. physical-device permission, enable/disable/remove, popup, persistence, DNR,
   and built-in-protection coexistence checks.

Until those gates pass, normal builds keep the UI closed and iNWEB has **zero
claimed extension support**. The API table remains a design target; no fake
counts, placeholder capability, Web Store installation promise, or unsupported
API success is permitted.
