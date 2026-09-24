# PHASE6 — 0013 enablement map (pinned tree @154.0.8037.21)

Engineering notes for `0013-extension-enable-android`. Facts below were
verified against the sparse clone (HEAD 0fa6d91e) on 2026-09-24.
Sequencing decision at the end.

## 1. Flag mechanics (verified)

- `extensions/buildflags/buildflags.gni`:
  - `enable_extensions = !is_android && !is_ios && !is_castos && !is_fuchsia`
  - `enable_desktop_android_extensions = is_desktop_android`
    (upstream's in-progress desktop-Android effort, crbug 356905053 —
    very much in-development, targets desktop-form-factor Android)
  - `enable_extensions_core = enable_extensions || enable_desktop_android_extensions`
  - `enable_platform_apps`/`enable_hosted_apps` follow `enable_extensions`.
- `build/config/chrome_build.gni`: `is_desktop_android = false` default;
  asserts `target_os == "android"`. NOT for us (phone form factor).
- **Our switch:** iNWEB repo `config/chromium/args-*.gn` sets
  `enable_extensions = true` (repo-side arg, no Chromium patch needed
  for the flag itself). The PATCH carries the tree fixes the flip
  requires + the inweb components + the audit script.

## 2. Android gating, measured (static map — the compiler is the oracle)

- `chrome/browser/BUILD.gn`: 2 `if (enable_extensions)` blocks (deps
  pulls: platform_apps, controlled_frame, extensions_zero_state_promo,
  //apps, …). Extension sources flow in mostly through the flag, not
  raw `is_android` guards — the upstream desktop-android effort has
  already centralized much of it.
- `chrome/browser/extensions/BUILD.gn`: exists, 3281 lines, 28
  android/flag guards.
- `chrome/android/BUILD.gn`: 20 extension mentions.
- Compile fallout surface: the `.cc` files pulled in by the flag that
  call desktop-only chrome services. Cannot be enumerated statically
  with confidence — the build hop is the oracle (that is why it runs
  BEFORE 0013 authoring completes).

## 3. Sequencing decision (2026-09-24)

1. **Build hop first** (run 35953643726, dispatched 2026-09-24,
   resume from state-13, ninja -j6, ETA 2–4 h): compiles the 12-patch
   series for the first time and produces the first alpha.2 candidate
   with adblock + popup + cosmetic. Any compile error in 0005–0012 is
   fixed as patch-file updates BEFORE 0013 lands.
2. 0013 authoring then proceeds with a known-green baseline: flip
   `enable_extensions` in args, dispatch a probe/dry-run hop, fix the
   fallout iteratively (each fix = tree changes collected into the
   0013 patch), audit script already drafted
   (`src/native/extensions/tools/audit_extension_apis.py`, mirrored
   into the patch tree).
3. 0014–0016 depend on 0013 (strict order — extension code cannot
   compile against an unenabled subsystem).

## 4. Honest boundary

Until 0013 builds and passes PHASE6 §6, iNWEB has zero extension
support. The §3 API table stays a target until the audit script runs
against a real artifact.
