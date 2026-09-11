# Phase 2 — Chromium Integration Plan (Browser Shell)

**Status:** design (authored in Step 3). Execution happens on build
infrastructure as tracked `ui/` patches (blocker B-001).
**Scope:** how `src/android-app` and `src/core` become the running iNWEB
browser inside the Chromium Android build.

---

## 1. What exists today (honest state)

| Piece | State |
|---|---|
| `src/core/browser-shell` (pure JVM: tabs, omnibox, session, downloads, settings) | **Implemented and unit-tested** (kotlinc + JUnit, runs in sandbox and CI) |
| `src/android-app` (Compose + Material 3 shell UI, bn/en strings) | **Authored** — compiles inside the Chromium build (validated on build host) |
| Chromium engine adapter | **Not started** — delivered as `ui/` patches |

No page rendering exists yet anywhere. The UI contains an explicit,
user-visible binding-point label rather than a fake page view (§57).

## 2. Integration mechanism

The final application is produced by the **Chromium build**, not by a parallel
Gradle/AGP build (ADR-009). `src/` is overlaid into the Chromium tree by a
tracked patch and compiled by GN/Siso together with the engine:

```text
chromium/src/chrome/android/inweb/
    java/...          ← synced from src/android-app (Kotlin + resources)
    java/.../core/    ← synced from src/core/browser-shell (pure JVM sources)
    BUILD.gn          ← added by patch 0001 (ui/)
```

Steps (each a registered patch in `iNWEB_PATCHES/ui/`):

1. **`0001-ui-inweb-app-target.patch`** — adds the `inweb_app` android_library
   target (Kotlin sources + resources) and the `inweb_public_apk` target
   modeled on upstream `chrome_public_apk`, with the iNWEB manifest,
   package id and branding.
2. **`0002-ui-engine-adapter.patch`** — implements `BrowserEngine` over the
   Chromium content layer (tabs/ContentView wiring, navigation callbacks →
   `EngineEvents`), replacing `DevelopmentEngineBinding`.
3. **`0003-ui-session-persistence.patch`** — binds `SessionStore` to tab
   persistence for crash-safe restore (§51).
4. **`0004-ui-shell-surface.patch`** — replaces the binding-point label with
   the real content surface compositor view inside the Compose layout.

A `scripts/sync_android_sources.sh` (authored together with patch 0001, when
the real tree is available) copies `src/` into the overlay and verifies the
copy with the tree hash — the patch series remains the single authority.

## 3. Validation checklist (per patch, at build time)

- [ ] `apply_patches.py apply && verify` on pristine pinned tag
- [ ] GN target builds: `autoninja -C out/inweb-development inweb_public_apk`
- [ ] APK installs and launches on an arm64 device/emulator
- [ ] Omnibox: URL navigation, search via engine, back/forward
- [ ] Tabs: open, close, select, private tabs, session restore after force-stop
- [ ] Strings: bn and en render; no missing-translation crashes
- [ ] Accessibility: TalkBack labels present on all controls

## 4. What is explicitly NOT claimed

- No browsing capability exists until patches 0001–0004 build and pass the
  checklist above.
- The Gradle project under `src/core/browser-shell` is a **validation
  vehicle** (tests run without Android); it is not the product build.
