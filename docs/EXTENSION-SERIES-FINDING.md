# Extension series (0014–0017) — feasibility finding before any patch

**Measured on the pinned tree, 2026-09-25. No patch has been written.**

> **Decision (2026-09-25): Option 1 — documented deviation, accepted by the
> project owner.** §16 is preserved as behavioural/specification intent; no
> extension UI, no runtime binding, no placeholder affordance; patch ids
> 0014–0017 stay reserved and unused. Recorded as **ADR-042** in
> `docs/EXTENSION-DEVIATION.md`, with the reservation held in
> `iNWEB_PATCHES/extension/RESERVED.md`. Nothing else is blocked by this.

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

## Correction (2026-09-25): a finished series already exists on a branch

After ADR-042 was accepted, `feature/extensions-0014-0017` was found on the
remote: **six commits ahead of main, five behind, and never opened as a
PR**. It contains all four extension patches plus design docs. My finding
above was therefore incomplete — I measured the baseline correctly but
presented the series as unwritten. It was written, and it is more careful
than the option set I offered.

| Patch | What it actually does |
|---|---|
| 0014 | build-time **audit tool** that regenerates the supported-API table from the built artifact ("reality, not intent"), plus a dedicated `enable_inweb_android_extensions` GN arg. Described as a *build probe, not an availability claim*. |
| 0015 | host-tested CRX3/ZIP structural inspector, manifest/version validation and native tests; **removes** the Chrome Web Store app-menu item so discovery is not misrepresented; UI stays gated. |
| 0016 | **closes by default** Chromium's existing Android extension management, toolbar action, popup and options surfaces; a device-verification run may opt in with `--enable-inweb-extension-ui`, so "normal builds expose nothing merely because the experimental backend compiled". |
| 0017 | sideload/update policy gate: invalid and non-newer packages blocked, every first install needs explicit review, permission-expanding updates cannot enable before renewed review, MV2 deprecation warning. "Stages decisions only and makes no runtime capability claim." |

Read against ADR-042's prohibitions, none of the four ships a capability,
enables a runtime, or exposes a default UI — 0016 *removes* surfaces and
0015 removes a misleading discovery path. The series matches the
deviation's intent, and 0016 is stricter than what I proposed.

Two things are still true and must be settled before any of it merges:

1. **It has never been built.** The branch predates ADR-042 and the
   current main; whether `enable_inweb_android_extensions` actually
   compiles on Android is an open, measurable question — which is
   precisely what patch 0014 calls itself a probe for.
2. **It needs a rebase** onto a main that now carries ADR-042, PR #4 and
   the content-catalog module.

ADR-042 as accepted says the ids are reserved and unused. That sentence
is now wrong as written, and the owner has been asked how to reconcile it.
