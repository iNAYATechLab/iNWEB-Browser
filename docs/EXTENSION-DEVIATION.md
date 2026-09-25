# ADR-042 — Extension subsystem: documented deviation from §16

**Status: ACCEPTED (2026-09-25). Owner: project owner. Author: Lead.**

## Decision

MASTER-SPEC §16 (extensions) is **preserved as behavioural and
specification intent**, and is **not implemented** on the current pinned
Chromium Android baseline. No extension UI, no extension runtime binding,
and no placeholder affordance is added. Patch numbers **0014–0017 stay
reserved and unused**.

## Baseline limitation (measured, not assumed)

`extensions/buildflags/buildflags.gni` @ 154.0.8037.21:

```gn
declare_args() {
  enable_extensions = !is_android && !is_ios && !is_castos && !is_fuchsia
}

declare_args() {
  enable_extensions_core =
      enable_extensions || enable_desktop_android_extensions
}
```

Our build is `target_os = "android"` (`config/chromium/args-development.gn`,
`args-release.gn`) and neither args file overrides those flags, so at this
pin both `enable_extensions` and `enable_extensions_core` are **false**.
`chrome/browser/extensions/BUILD.gn` opens with
`assert(enable_extensions_core)` and gates most sources behind
`if (enable_extensions)` and `if (!is_android)`; the small Android block
that exists is not the runtime.

**This is an upstream platform decision, not a flag we forgot.** Chrome
for Android does not ship the WebExtensions platform. Full evidence:
`docs/EXTENSION-SERIES-FINDING.md`.

## Why it is not implemented now

Binding `src/core/extensions` (17 tests, ADR-023) to a runtime would mean
porting an entire configuration upstream does not support or test on
Android: the whole extensions codebase, every desktop assumption in it,
and permanent ownership of that fork. It would be a platform port wearing
a patch number, attempted before the ComposeView path has run on a device.

The alternative — shipping the management surface without the runtime —
is worse than shipping nothing: a settings screen whose "enable" does
nothing is a fake feature, and §57 exists to prevent exactly that.

## Deviation from MASTER-SPEC §16

| §16 expects | This baseline supports | Resolution |
|---|---|---|
| Extension install / review / enable / disable / update / remove | the **state machine** is implemented and tested (17 tests) but has no runtime to drive | kept as the behavioural specification; no UI |
| Sideload with Chrome-style version comparison | yes, in core | unchanged |
| Permission-review records | yes, in core | unchanged |
| A running WebExtensions runtime | **no** — not built for Android | not implemented; documented |

§16 is therefore **not deleted and not weakened**: it remains the
specification of intended behaviour, and this ADR records why the
behaviour cannot be delivered on this baseline. This is the same shape as
ADR-040, where the ad-block engine was ported to C++ because the pinned
tree could not compile Kotlin.

## Explicit prohibitions while this ADR stands

- No extension button, entry point, badge or count in any UI.
- No placeholder screen, no "coming soon", no disabled-looking control
  that implies a future feature.
- No extension patch applied to the build path.
- No capability shipped under the name "extensions" that is not one.

The absence of the feature is the honest state. A user cannot discover a
surface that does not work.

## Reserved series

`0014–0017` are **reserved for extensions and not reused** for any other
area. Rationale: if the capability becomes available, the series resumes
with its original identity and documentation instead of being
renumbered around a gap. See `iNWEB_PATCHES/extension/RESERVED.md`.

## Re-evaluation path

This ADR is revisited if **any** of these becomes true:

1. Upstream builds the extensions platform for Android (a change to the
   `enable_extensions` default in a future pin).
2. A future pin exposes a supported Android extension path (for example a
   desktop-Android configuration we actually ship).
3. The project adopts a different baseline that supports extensions.

Re-evaluation is cheap by design: `src/core/extensions` is implemented
and tested, the finding documents what was checked, and the patch numbers
are unused — so the work resumes as a binding exercise, not a redesign.

## Not in scope

A limited user-script or content-injection capability is **not** started
here. It is a separate future feature requiring its own spec and ADR, and
it must not be called "extensions".
