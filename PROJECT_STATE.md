# iNWEB Browser — Project State

> Single source of truth for project progress (Master Specification §60).
> Updated at the end of every development step. Must always reflect the real repository
> state — never a desired or simulated state (§57, §65).
>
> Last updated: **2026-09-12** — Step 4 (Phase 2 in progress)

```yaml
project: iNWEB Browser
repository: iNAYATechLab/iNWEB-Browser
phase: 2
phase_title: Browser Shell
phase_status: in_progress   # core shell logic implemented & tested; UI authored; engine integration awaits B-001
step: 4
chromium_baseline: 154.0.8037.21   # upstream Android stable, pinned 2026-09-12
fork_strategy: tracked-patch-overlay-on-pinned-tags  # ADR-001
build_status: not-built            # no Chromium artifact exists yet (B-001)
test_status: unit-tests-passing    # 30 Python + 70 Kotlin tests (local + CI)
ci_status: authoring-pipeline-live # Python + Kotlin core jobs; upstream watch live
known_blockers: [B-001]
open_defects: 0
next_action: >-
  Phase 3 / Step 5 — tracking-protection core: filter-list model, parser
  and matching engine (pure JVM, tested), per-site allowlist, and the
  privacy policy model that engine patches will enforce.
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

## In progress

- (none — awaiting continuation command for Step 5)

## Not started

- Phase 3 remainder: privacy UI surfaces, storage controls (engine patches)
- Phase 4 — Ad / Popup Protection
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

## Build status

- **Not built.** No APK/AAB has been produced. `fetch_chromium.sh`, `build_android.sh`,
  and the Docker container are authored but unexecuted (B-001). The Android UI sources
  compile inside the Chromium build once integration patch 0001 lands (see
  `docs/PHASE2-INTEGRATION-PLAN.md`).

## Test status

- **Python: 30/30 passing** — patch-series tooling, registry validation, baseline
  parsing, string-resource validation (`python3 -m unittest discover -s tests -t .`).
- **Kotlin: 70/70 passing** — `src/core/browser-shell` compiled with pinned
  kotlinc 2.4.20 + JUnit 4.13.2 (`bash scripts/validate_kotlin_core.sh`):
  tab navigation stack, controller, session round-trip/corruption + manager,
  omnibox parsing (incl. Bengali queries and scheme edge cases), search
  engines, download state machine + catalog, history store with private
  exclusion, settings.
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

**Phase 3 / Step 5 — tracking-protection core:** filter-list data model and parser
(EasyList-family syntax subset), URL-matching engine with exception (@@) rules and
per-site allowlist, request-decision API for the network-stack patches, and the
privacy policy model — all pure JVM with unit tests; engine enforcement wiring
documented for the adblock/tracking patch areas.
