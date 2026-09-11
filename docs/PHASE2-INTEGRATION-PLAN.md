# Phase 2 — Chromium Integration Plan (Browser Shell)

**Status:** design (authored in Step 3; updated in Steps 4–12 as surfaces
landed). Execution happens on build infrastructure as tracked `ui/`
patches (blocker B-001).
**Scope:** how `src/android-app` and `src/core` become the running iNWEB
browser inside the Chromium Android build.

---

## 1. What exists today (honest state)

| Piece | State |
|---|---|
| `src/core/browser-shell` (pure JVM: tabs, omnibox, session, downloads, settings, history, bookmarks, top sites) | **Implemented and unit-tested — 115 tests** (kotlinc + JUnit, runs in sandbox and CI) |
| `src/core/tracking-protection` (pure JVM: filter parsing, request decisions, filter-list management) | **Implemented and unit-tested — 70 tests** |
| `src/android-app` (Compose + Material 3: browser, tab switcher, home with real data, onboarding, settings, downloads, history, bookmarks; bn/en strings) | **Authored** — compiles inside the Chromium build (validated on build host) |
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

## 3. Surface-by-surface engine-adapter binding contract

Every shell surface is already backed by real, tested core state. The
adapter (`0002-ui-engine-adapter.patch`) is the single translation layer
between Chromium events and these core contracts:

| Surface | Core contract (implemented) | Adapter must provide |
|---|---|---|
| Tabs | `TabsController` (open/close/select/restore, `allTabs()`) | Create/destroy tabs on user action; forward lifecycle (`onPageStarted` → LOADING, `onPageFinished` → IDLE + `PageSecurityState`); observe controller state to drive real ContentViews |
| Omnibox | `OmniboxParser`, `SearchEngine` | Current-URL updates into `TabState` history (real navigation stack, not omnibox-driven only) |
| History | `HistoryStore` port (`FileHistoryStore`, single `PrivacyFilterHistory` enforcement point) | Record page-load **events** with real page titles (today: omnibox navigations with URL as title); private tabs must use the off-the-record profile so nothing is written |
| Bookmarks | `BookmarkStore` port (`FileBookmarkStore`) | Real page titles for "add current page"; explicit user action only |
| Downloads | `DownloadsStore` port | Real download lifecycle events (QUEUED→RUNNING→… state machine already enforced in core) |
| Home / new tab | `TopSites.compute()`, `HistoryStore`, `BookmarkStore` | Nothing beyond the history/bookmark bindings — titles improve automatically |
| Onboarding / settings | `SettingsStore` port | Nothing — no engine dependency (persisted, already real) |
| Tracking protection | `TrackingProtectionEngine.decide(RequestContext)`, `EngineStatistics`, `FilterListManager` | The `adblock/`/`privacy/` patches call `decide()` per network request from the network stack; invoke `FilterListManager.startup()/refresh()` from app start and the host scheduler; statistics feed the Security Center (§24) — displayed counts must come from real decisions only |

## 4. Validation checklist (per patch, at build time)

- [ ] `apply_patches.py apply && verify` on pristine pinned tag
- [ ] GN target builds: `autoninja -C out/inweb-development inweb_public_apk`
- [ ] APK installs and launches on an arm64 device/emulator
- [ ] Omnibox: URL navigation, search via engine, back/forward
- [ ] Tabs: open, close, select, private tabs, tab switcher, session restore after force-stop
- [ ] History: page loads recorded with real titles; private tabs record nothing
- [ ] Bookmarks: add current page with real title; persists across restart
- [ ] Home: shortcuts/recent/bookmarks show real data; empty sections hidden
- [ ] Onboarding: runs once; engine choice persists; skip keeps the privacy default
- [ ] Tracking protection: `decide()` consulted per request; blocked/allowed counts real (§57)
- [ ] Strings: bn and en render; no missing-translation crashes
- [ ] Accessibility: TalkBack labels present on all controls

## 5. What is explicitly NOT claimed

- No browsing capability exists until patches 0001–0004 build and pass the
  checklist above.
- The Gradle project under `src/core/browser-shell` is a **validation
  vehicle** (tests run without Android); it is not the product build.
