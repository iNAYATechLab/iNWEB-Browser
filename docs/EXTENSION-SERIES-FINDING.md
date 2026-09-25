# Extension series (0014–0017) — feasibility finding before any patch

**Measured on the pinned tree, 2026-09-25. No patch has been written.**

This branch exists to prepare the extension series. The first thing
preparation required was checking whether the thing the series is
supposed to bind to actually exists in the baseline. It does not.

## The measurement

`extensions/buildflags/buildflags.gni` @ 154.0.8037.21:

```gn
declare_args() {
  enable_extensions = !is_android && !is_ios && !is_castos && !is_fuchsia
  ...
}

declare_args() {
  enable_extensions_core =
      enable_extensions || enable_desktop_android_extensions
}
```

Our build is `target_os = "android"` (`config/chromium/args-development.gn`,
and the release args), and neither args file sets `enable_extensions` or
`enable_desktop_android_extensions`. So at this pin:

- `enable_extensions = false`
- `enable_extensions_core = false`

`chrome/browser/extensions/BUILD.gn` confirms the shape: it opens with
`assert(enable_extensions_core)` and gates most of its sources behind
`if (enable_extensions)` and `if (!is_android)`. The small `is_android`
block that exists is not the runtime.

## What that means

Upstream does not build the WebExtensions platform for Android — this is
not a flag we forgot to set, it is a deliberate platform exclusion. So
there is no "real WebExtensions runtime" in our binary for the
`src/core/extensions` state machine to bind to.

Binding `ExtensionRegistry` (17 tests, ADR-023) to a runtime would
therefore mean **porting an entire upstream-unsupported configuration**:
pulling the extensions codebase into an Android build, resolving every
desktop assumption in it, and then owning that fork forever. That is a
platform port wearing a patch number, and it would arrive before the
ComposeView path has even run on a device.

## Why this is not written as patches anyway

The series could have been drafted against a runtime that does not
exist, and it would have looked like progress: four patches, clean GN,
green `gn gen`, nothing running. The failure would only surface later, as
a settings screen where "enable" does nothing — which is a fake feature,
and §57 is precisely about not shipping those.

## Options for the owner

1. **Documented deviation (recommended now).** Record that the baseline
   does not build extensions on Android; keep the tested core model as
   the behavioural specification, as ADR-040 did for the ad-block
   engine; ship no extension UI. Honest, zero risk, revisitable.
2. **Enable the subsystem anyway.** A platform port with an unquantified
   but certainly large cost, on an upstream-unsupported configuration,
   maintained by us alone.
3. **A scoped capability we can actually deliver.** User scripts or
   injected content we control through WebContents, with their own spec
   and ADR — not "extensions" by name, and not pretending to be.

## If option 1 is chosen

The series numbers 0014–0017 stay reserved and unused rather than being
reassigned, so that "where are the extension patches?" has a written
answer instead of a gap. `src/core/extensions` stays as it is: implemented
and tested, documented as the specification for a binding that the
baseline cannot host yet.
