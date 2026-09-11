# iNWEB Browser — Project State

> Single source of truth for project progress (Master Specification §60).
> Updated at the end of every development step. Must always reflect the real repository
> state — never a desired or simulated state (§57, §65).
>
> Last updated: **2026-09-12** — Step 10 (Phase 3 in progress)

```yaml
project: iNWEB Browser
repository: iNAYATechLab/iNWEB-Browser
phase: 3
phase_title: Privacy & Tracking Protection
phase_status: in_progress   # decision engine implemented & tested; enforcement wiring awaits B-001
step: 10
chromium_baseline: 154.0.8037.21   # upstream Android stable, pinned 2026-09-12
fork_strategy: tracked-patch-overlay-on-pinned-tags  # ADR-001
build_status: not-built            # no Chromium artifact exists yet (B-001)
test_status: unit-tests-passing    # 30 Python + 183 Kotlin tests (local + CI)
ci_status: authoring-pipeline-live # Python + Kotlin core jobs; upstream watch live
known_blockers: [B-001]
open_defects: 0
next_action: >-
  Phase 3 / Step 11 — onboarding / first-run screens (§35): privacy
  education + default search-engine choice wired to the real
  SettingsStore (alternative if directed: downloads surface polish or a
  quality/documentation pass).
```

## Completed

- [x] **Step 1 — Phase 0: Discovery and Feasibility** (2026-09-12)
  - Master specification analyzed and imported; environment measured; baseline pinned
    (`154.0.8037.21`); tracked-patch strategy selected (ADR-001); architecture,
    licensing, build-infrastructure, and threat-model documents created.
- [x] **Step 2 — Phase 1: Chromium Foundation (authoring foundation)** (2026-09-12)
  - `iNWEB_PATCHES/` framework + registry; `apply_patches.py` (peel+forward
    convergence), `lint_manifest.py`, `check_baseline.py` — all unit-tested;
    fetch/build orchestration scripts + Docker build container (authored, B-001);
    live CI (unit tests, registry validation, weekly upstream drift watch).
- [x] **Step 3 — Phase 2: Browser Shell (core + UI source)** (2026-09-12)
  - `src/core/browser-shell` pure-JVM module: TabState navigation stack
    (forward-truncation semantics), TabsController (open/close/select/restore),
    crash-safe SessionStore (versioned line format, corruption detection),
    OmniboxParser (URL-vs-search with known-scheme whitelist), SearchEngine
    (DDG default), DownloadRecord state machine, AppSettings/SettingsStore
  - 52 Kotlin unit tests — compiled and run with pinned kotlinc + JUnit
    (sandbox and CI), all passing
  - `src/android-app` shell UI source: single-activity Compose + Material 3
    theme (light/dark, iNWEB palette), omnibox bar, bottom navigation with
    accessibility labels, home screen, ViewModel binding to core
  - Localized resources: 36 strings in English + Bengali with placeholder
    parity; `validate_strings.py` validator + 9 Python tests
  - `docs/PHASE2-INTEGRATION-PLAN.md`: patch-by-patch Chromium integration plan
    (inweb_public_apk target, engine adapter, sync + validation checklist)
  - CI extended: Kotlin core test job + string parity validation
- [x] **Step 4 — Phase 2: Browser Shell (surfaces + persistence)** (2026-09-12)
  - History data layer: `HistoryStore` port + `PrivacyFilterHistory`
    (private-mode exclusion enforced in one decorator) + in-memory impl —
    recent/search/delete/deleteRange/clearAll, 8 tests
  - Downloads catalog: `DownloadsStore` port + in-memory impl, 5 tests
  - Session manager: `SessionManager` + `SessionPersistence` port —
    crash-safe snapshot/restore with corrupted-snapshot fallback to a fresh
    session, 5 tests
  - Settings surface (search engine + theme) with real persisted behavior;
    downloads surface backed strictly by real DownloadRecord state
  - App adapters: `FileSessionPersistence` (atomic temp+rename writes),
    `SharedPreferencesSettingsStore`
  - Lifecycle binding: session restore on start, snapshot on onStop (§51)
  - 3 new localized strings (downloads empty/queued/running) — 39 total
- [x] **Step 5 — Phase 3: Tracking-protection core (decision engine)** (2026-09-12)
  - New pure-JVM module `src/core/tracking-protection` (package
    `com.inweb.browser.privacy`), validated by the multi-module core script + CI
  - EasyList-family parser (documented subset): comments/headers counted,
    cosmetic rules recognized + counted but ignored, `@@` exceptions,
    `||` / `|…|` anchors, `*` / `^` wildcards, `$` options (type, party,
    `domain=` includes/excludes, `match-case` ignored); unknown options
    (csp=, rewrite=, removeparam=, …) counted as unsupported and excluded
    from matching — never silently treated as matching
  - URL-matching engine with per-rule lazy regex; `domain=` matches the
    document host or its subdomains; third-party decided by registrable domain
  - Request-decision API: `RequestContext` → `FilterDecision`
    (BLOCK / ALLOW / PASS) with matched-rule attribution and real decision
    statistics (`EngineStatistics`: block/allow/pass counts, blockedByDomain)
    — the contract the Chromium adblock/privacy patches will call (B-001)
  - Policy model: `CookiePolicy` + `TrackingProtectionSettings` with
    per-site allowlist semantics
  - Domain classifier with a multi-part public-suffix subset (co.uk, com.bd, …)
  - `scripts/validate_kotlin_core.sh` rewritten as multi-module
    (browser-shell 70 + tracking-protection 37 = 107 Kotlin tests)
- [x] **Step 6 — Phase 3: Filter-list management layer** (2026-09-12)
  - `lists` subpackage in `src/core/tracking-protection` (ADR-012 module):
    `FilterListSource` (EasyList + EasyPrivacy defaults, sanitized ids),
    `FilterListFetcher` port + real `HttpFilterListFetcher`
    (HttpURLConnection, timeouts, `If-None-Match`/`If-Modified-Since`
    revalidation, 304 handling, hard body-size cap)
  - `FileFilterListCache`: atomic (temp + rename) two-file cache per list
    (`<id>.txt` + `<id>.meta`), corrupt metadata degrades to missing —
    tested against real temp directories
  - `FilterListVersion`: `! Version:` / `! Last modified:` header extraction
  - `UpdatePolicy`: refresh-due decision (interval / startup fetch / enabled)
    — the policy never runs timers itself
  - `FilterListManager`: startup (cache first, download only what is
    missing), conditional refresh, graceful cached fallback on failure,
    per-source `ListUpdateStatus` report, `buildEngine()` snapshot
  - HTTP behavior tested against the JDK's real HttpServer (live sockets,
    captured conditional headers, 200/304/404/dead-server/size-cap paths)
  - 33 new tests → tracking-protection 70; Kotlin total 140
- [x] **Step 7 — Phase 3: History surface backed by the real HistoryStore** (2026-09-12)
  - `FileHistoryStore` (pure JVM, `src/core/browser-shell`): persistent
    TSV history — write-through atomic (temp + rename) on every mutation,
    header-corruption → fresh start (session precedent), malformed lines
    skipped and reported via `lastLoadSkippedLines`, tab/newline
    sanitization keeps the line format unambiguous, ids stay unique
    across reloads — 11 new tests
  - Android layer: `BrowserViewModel` gains injected `HistoryStore`,
    live search query state, `openHistory / setHistoryQuery /
    deleteHistoryEntry / clearHistory`; `MainActivity` injects
    `FileHistoryStore(filesDir/history.tsv)` — real persistence, not a
    mock
  - New `HistoryScreen` (Compose M3): search field, per-entry delete,
    clear-all with confirmation dialog, honest empty state, localized
    timestamps; routed via `Screen.HISTORY` + bottom-bar menu entry
  - 7 new strings (en + bn): history empty/search/clear/confirm/cancel
  - Kotlin total 151 (browser-shell 81 + tracking-protection 70)
- [x] **Step 8 — Phase 3: Bookmarks core + surface** (2026-09-12)
  - `BookmarkStore` port + `InMemoryBookmarkStore` (`src/core/browser-shell`):
    URLs unique (duplicate add returns the existing entry), insertion
    order, folders-lite (`null` = unfiled, blank folder treated as
    unfiled), rename-in-place, move (incl. back to unfiled), delete,
    clearAll — 9 tests
  - `FileBookmarkStore`: persistent TSV bookmarks ("iNWEB-BOOKMARKS
    v=1"), same crash-safety strategy as history (write-through atomic
    temp + rename, header corruption → fresh start, malformed lines
    skipped + counted, tab/newline sanitization, unique ids across
    reloads) — 11 tests
  - Android layer: `BrowserViewModel` gains injected `BookmarkStore`,
    `openBookmarks / addBookmarkForCurrentTab / deleteBookmark`;
    `MainActivity` injects `FileBookmarkStore(filesDir/bookmarks.tsv)`
  - New `BookmarkScreen` (Compose M3): add-current-page action,
    per-entry delete, folder labels, honest empty state; routed via
    `Screen.BOOKMARKS` + bottom-bar menu entry
  - 3 new strings (en + bn): empty state, add-current, remove
  - Bookmarks are an explicit user action only — never automatic (§57)
  - Kotlin total 171 (browser-shell 101 + tracking-protection 70)
- [x] **Step 9 — Phase 3: Tab-switcher surface (real TabsController)** (2026-09-12)
  - Core: `TabsController.allTabs()` — all open tabs in insertion order
    (tab-switcher view; also the contract the engine adapter will
    observe) — 3 new tests (insertion order incl. private, navigation
    updates, closures)
  - Android layer: `BrowserViewModel` exposes live `tabs` state;
    `openTabs()` routes to the new `Screen.TABS`
  - New `TabsScreen` (Compose M3): two-column grid of tab cards with
    selected-tab highlight, per-card close, private-tab badge (§13),
    new-tab / new-private-tab actions, honest empty state; selecting a
    tab returns to the browser
  - Bottom-bar tabs button now opens the switcher (was a placeholder
    that just opened a new tab)
  - 1 new string (en + bn): tabs empty state
  - Kotlin total 174 (browser-shell 104 + tracking-protection 70)
- [x] **Step 10 — Phase 3: Home / new-tab surface with real data (§38)** (2026-09-12)
  - Core: `HistoryStore.allVisits()` — raw stored visits, oldest first,
    including repeated URLs (private visits never present) — 2 new store
    tests (in-memory incl. duplicates + private exclusion, file
    persistence across instances)
  - Core: `TopSites.compute()` — the §38 "shortcuts" section computed
    from real browsing history: visit-count ranking, recency tie-break,
    latest-title selection, limit — 7 tests
  - Android layer: `BrowserViewModel` exposes `homeShortcuts` /
    `homeRecent` / `homeBookmarks`, refreshed on every navigation and
    bookmark mutation; history reads now go through the single
    privacy-decorated reference
  - `HomePage` rebuilt: brand + private-search prompt + three REAL data
    sections (shortcuts from top sites, recent pages, bookmarks); empty
    sections are hidden — nothing fabricated (§57); rows open via the
    real omnibox navigation path
  - Kotlin total 183 (browser-shell 113 + tracking-protection 70)

## In progress

- (none — awaiting continuation command for Step 11)

## Not started

- Phase 3 remainder: filter-list download/cache management, privacy UI surfaces, storage controls (engine patches)
- Phase 4 — Ad / Popup Protection
- Phase 5 — Performance
- Phase 6 — Extensions
- Phase 7 — Offline / Data Saving
- Phase 8 — VPN / Security
- Phase 9 — Profiles / Sync
- Phase 10 — Localization / Accessibility
- Phase 11 — Advanced Features
- Phase 12 — Production Hardening

(Definitions: `docs/MASTER-SPEC.md` §53.)

## Known blockers

### B-001 — Chromium cannot be compiled in the authoring sandbox

- **Exact blocker:** No Chromium build (checkout + compile + link) is possible in the
  development sandbox used for authoring.
- **Why:** Measured sandbox resources (2 cores, 1.9 GB RAM, 20 GB free disk, 30-minute
  command ceiling, ephemeral sessions) are far below upstream requirements (x86-64 Linux,
  ≥ 8 GB RAM with 16 GB+ recommended, ≥ 100 GB disk, multi-hour builds).
- **Attempted / verified:** Resources measured; upstream requirements verified; network
  to `chromium.googlesource.com` confirmed; no lighter legitimate path preserves the
  mandated architecture (WebView fallback prohibited by §2/§71).
- **Action required:** Provision a build host per `docs/BUILD-INFRASTRUCTURE.md` §2
  (self-hosted GitHub Actions runner or cloud VM; minimum 16 cores / 64 GB RAM /
  300 GB SSD, Ubuntu 22.04/24.04 x86-64, Docker).
- **Continues without blocker:** All authoring work; live CI (Python + Kotlin core);
  patch framework; application source; documentation.

Full record: `docs/PHASE0-ENVIRONMENT-ASSESSMENT.md` §5.

## Known defects

- None open. (Step 4 test defect found and fixed via the Rule 64 loop: an
  incorrect newest-first expectation in a history range-deletion test.
  Step 3 defect found and fixed via the Rule 64 loop: the omnibox
  scheme detection initially treated `host:port` inputs as URLs with an unknown
  scheme; replaced with an explicit `scheme://` authority check plus a
  known-scheme whitelist, with regression tests.)

## Architecture decision log

| ID | Date | Decision | Rationale |
|---|---|---|---|
| ADR-001 | 2026-09-12 | Fork strategy: tracked iNWEB patch overlay on pinned upstream Android-stable tags | Sustainable security-update path (§4); every modification traceable (§5) |
| ADR-002 | 2026-09-12 | Baseline pinned to `154.0.8037.21` (Android stable at pin date) | Newest stable line with full security support |
| ADR-003 | 2026-09-12 | Two-environment model: sandbox authoring + external build host | Matches B-001 without architectural compromise |
| ADR-004 | 2026-09-12 | Chromium tree never committed; reproduced as pinned tag + patch series | Reviewable repo; reproducible builds |
| ADR-005 | 2026-09-12 | iNWEB-authored code license proposed as MPL-2.0 | File-level copyleft, BSD-3-compatible |
| ADR-006 | 2026-09-12 | Step-completion communication: three continuation commands in three separate code blocks | Explicit user directive (supersedes §61 rendering detail) |
| ADR-007 | 2026-09-12 | CI split: live authoring pipeline on GitHub-hosted runners; Chromium build pipeline gated on self-hosted runner | Validates everything validatable today (B-001) |
| ADR-008 | 2026-09-12 | iNWEB Android UI layer: Jetpack Compose + Material 3, single-activity | Modern M3 (§37); full ownership of the iNWEB design system |
| ADR-009 | 2026-09-12 | Engine-independent shell logic lives in a pure-JVM module (`src/core/browser-shell`); the product APK is produced only by the Chromium/GN build (no parallel AGP product build) | Core logic continuously testable in CI without Android SDK; single source of truth for the product build |
| ADR-010 | 2026-09-12 | DuckDuckGo is the default search engine | Privacy-preserving defaults (§10); user-changeable in settings |
| ADR-011 | 2026-09-12 | History privacy contract enforced in a single decorator (`PrivacyFilterHistory`); session persistence writes atomically and falls back to a fresh session on corruption | One enforcement point regardless of backing store (§13/§14); crash-safe restore (§50/§51) |
| ADR-012 | 2026-09-12 | One pure-JVM Gradle module per concern under `src/core/` (browser-shell, tracking-protection), each registered in the shared multi-module core validation script | Independent testability and review per concern; CI stays toolchain-pinned and Android-SDK-free (ADR-009) |
| ADR-013 | 2026-09-12 | Filter engine v1 implements a documented EasyList-family subset with per-rule lazy regex; unknown options count as unsupported and are excluded from matching; combined-matcher optimization deferred | Honest, testable subset now (§57); correctness first, performance pass in Phase 4/5 |
| ADR-014 | 2026-09-12 | Filter-list management: transport port with a real HttpURLConnection implementation (conditional revalidation, size cap), atomic file cache, and pure refresh-due policy — background timers stay in the host layer | Real, testable download/cache behavior now (§44); no hidden scheduling or device downloads before engine/app wiring (§57) |
| ADR-015 | 2026-09-12 | Persistent history uses a write-through atomic TSV file store; header corruption restarts fresh, malformed lines are skipped and counted; the single `PrivacyFilterHistory` decorator remains the only privacy enforcement point | Crash-safe persistence with honest degradation (§14/§51); privacy contract stays in one place (ADR-011 pattern) |
| ADR-016 | 2026-09-12 | Bookmarks: URLs are unique (duplicate add is idempotent), folders are plain names with `null` = unfiled, and bookmarking happens only on explicit user action; `FileBookmarkStore` mirrors the ADR-015 persistence strategy | No accidental duplicates; simple folders-lite v1 (folders UI deferred); honest, crash-safe storage identical to history |
| ADR-017 | 2026-09-12 | Home "shortcuts" are top sites computed from real raw history visits (`allVisits()` + `TopSites.compute()`), never pinned or fabricated; home sections with no data are hidden entirely | §38 shortcuts grounded in real usage data (§57); pinned/custom shortcuts deferred to Phase 11 customization |

## Build status

- **Not built.** No APK/AAB has been produced. `fetch_chromium.sh`, `build_android.sh`,
  and the Docker container are authored but unexecuted (B-001). The Android UI sources
  compile inside the Chromium build once integration patch 0001 lands (see
  `docs/PHASE2-INTEGRATION-PLAN.md`).

## Test status

- **Python: 30/30 passing** — patch-series tooling, registry validation, baseline
  parsing, string-resource validation (`python3 -m unittest discover -s tests -t .`).
- **Kotlin: 183/183 passing** (`bash scripts/validate_kotlin_core.sh`, pinned
  kotlinc 2.4.20 + JUnit 4.13.2, multi-module):
  - `src/core/browser-shell` — 113 tests: tab navigation stack, controller
    (incl. `allTabs` switcher view), top-sites computation,
    session round-trip/corruption + manager, omnibox parsing (incl. Bengali
    queries and scheme edge cases), search engines, download state machine +
    catalog, history store with private exclusion, file-backed persistent
    history (round-trips, corruption fallback, sanitization, unique ids),
    bookmark store semantics (dedupe, folders, rename/move) + file-backed
    persistent bookmarks, settings.
  - `src/core/tracking-protection` — 70 tests: filter parsing (anchors,
    options, exceptions, cosmetic/unsupported/invalid counting), pattern
    matching (domain anchor, separators, wildcards, left/right anchors,
    type/party/domain constraints), engine decisions (block/allow/pass,
    per-site allowlist, statistics), domain classification (multi-part
    suffixes, third-party); list management (HTTP fetch via real JDK
    HttpServer, conditional revalidation + 304, size cap, atomic file
    cache, version parsing, update policy, manager lifecycle with cached
    fallback).
- **Live checks:** registry lint OK; string parity OK; baseline drift CURRENT.
- CI runs the Python suite, string validation, and the Kotlin core suite on every
  push/PR touching `scripts/`, `tests/`, `iNWEB_PATCHES/`, `src/`.

## Chromium baseline

| Field | Value |
|---|---|
| Baseline tag | `154.0.8037.21` |
| Channel | Android stable |
| Pinned | 2026-09-12 |
| Verification | Chrome Version History API + weekly Upstream Watch workflow |
| depot_tools pin | `73fc5a4d6bd051f1fd58404e62dfb83f734b0cfa` |
| Kotlin compiler pin (core validation) | `2.4.20` |

## Next planned action

**Phase 3 / Step 11 — onboarding / first-run screens (§35):** the
first-run flow (privacy education + default search-engine choice)
authored in Compose and wired to the real `SettingsStore` — the choice
persists for real. Alternative next step if directed: downloads surface
polish or a quality/documentation pass.
