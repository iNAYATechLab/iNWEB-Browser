# iNWEB Browser — Project State

> Single source of truth for project progress (Master Specification §60).
> Updated at the end of every development step. Must always reflect the real repository
> state — never a desired or simulated state (§57, §65).
>
> Last updated: **2026-09-12** — Step 49 (B-001 Stage 1 EXECUTED on GitHub Actions; authoring active on the hosted-runner path)

```yaml
project: iNWEB Browser
repository: iNAYATechLab/iNWEB-Browser
phase: 12
phase_title: Production Hardening
phase_status: authoring_complete   # all 5 design-order items done; every remaining Phase 12 deliverable is B-001-gated (device matrix)
step: 49
chromium_baseline: 154.0.8037.21   # upstream Android stable, pinned 2026-09-12
fork_strategy: tracked-patch-overlay-on-pinned-tags  # ADR-001
build_status: not-built            # no Chromium artifact exists yet (B-001); Stage-1 fetch/tag/B-1/B-2 PROVEN on hosted runners
test_status: unit-tests-passing    # 80 Python + 414 Kotlin tests (local + CI)
ci_status: authoring-pipeline-live # Python + Kotlin core jobs + 5 gates (registry, strings, externalization, structure, storage inventory); upstream watch live; B-001 Stage-1 hosted-runner workflow (dispatch-only)
known_blockers: [B-001]
open_defects: 0
next_action: >-
  Proposed Step 50 — RESUMABLE CHAINED-JOB BUILD on GitHub Actions
  (no local machine, per the standing directive): extend the Stage-1
  workflow so each job uploads the out/ build state (and any missing
  tree bits) as workflow artifacts and the next job downloads and
  RESUMES autoninja, chaining ~6-hour free hosted jobs until
  chrome_public_apk completes (corrected measurement: 267 edges/min,
  ~4.5–5.5 h remaining — an estimated ~2 hops);
  honest risks recorded up front: artifact transfer time per hop,
  retention limits, and possible flakiness — every hop keeps the
  PASS/FAIL/BLOCKED evidence discipline. Alternatives: (a) the user
  funds a larger-runner org plan or cloud build capacity (one job,
  no chaining); (b) record the current evidence as the Stage-1
  endpoint and pause.
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
  - `docs/phases/PHASE2-INTEGRATION-PLAN.md`: patch-by-patch Chromium integration plan
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
- [x] **Step 11 — Phase 3: Onboarding / first-run screens (§35)** (2026-09-12)
  - Core: `AppSettings.onboardingCompleted` (default false — fresh
    installs run onboarding exactly once) — 2 new tests (default
    incomplete, completion round-trips with the engine choice)
  - `SharedPreferencesSettingsStore` persists the flag alongside the
    engine and theme keys
  - `BrowserViewModel.needsOnboarding` + `completeOnboarding(
    searchEngineId)` — the chosen engine is written through the SAME
    real setting the settings screen uses (one source of truth,
    persisted immediately; skip completes with the privacy default)
  - New `OnboardingScreen` (Compose M3): step 1 privacy education
    (three concise points, §35 "do not overwhelm"), step 2 default
    search-engine choice from `SearchEngine.DEFAULTS`, step indicator,
    back/skip/next/get-started; `MainActivity` routes first-run to
    onboarding instead of the browser
  - 11 new strings (en + bn)
  - Kotlin total 185 (browser-shell 115 + tracking-protection 70)
- [x] **Step 12 — Phase 3: close-out & quality pass** (2026-09-12)
  - CI workflows bumped to current action majors (checkout v4→v7,
    setup-python v5→v7, setup-java v4→v6, cache v4→v6) — clears the
    Node 20 deprecation annotations; Kotlin job renamed to reflect the
    two modules it validates
  - `docs/phases/PHASE2-INTEGRATION-PLAN.md`: new "Surface-by-surface
    engine-adapter binding contract" (§3) covering tabs, omnibox,
    history, bookmarks, downloads, home, onboarding/settings, and
    tracking protection — plus an expanded build-time validation
    checklist per surface; honest-state table updated to Steps 4–11
  - `docs/ARCHITECTURE.md`: new §4a "Implemented today" table for the
    two core modules + authored UI
  - New `src/core/browser-shell/README.md` (module doc mirroring the
    tracking-protection module README)
  - Full validation sweep re-run: Kotlin 185/185, Python 30/30,
    strings OK; CI green on the bumped actions

- [x] **Step 13 — Phase 3: Security Center data model (§24)** (2026-09-12)
  - `SecurityCenter` / `SecurityCenterModel` /
    `FilterListStatus` / `BlockedDomainCount` in
    `src/core/tracking-protection`: the §24 dashboard contract built
    strictly from real state — real `EngineStatistics` counts, real
    registrable-domain top-blocked list (sorted, limited), real policy
    inputs (enabled, cookie policy, allowlist count), real per-list
    version/rule counts; `enforcementActive` is an explicit false until
    the engine patches wire `decide()` (B-001)
  - v1 covers tracker-blocking state; §24 items without real backing
    yet (connection security, permissions, certificates) are absent by
    design — they join when their patches land
  - 9 new tests — including the §24 honesty cases: loaded rules never
    fabricate decisions, and a whole-page allowlist bypass is not
    counted as a decision (engine contract)
  - Kotlin total 194 (browser-shell 115 + tracking-protection 79)

- [x] **Step 14 — Phase 4: adblock patch-series design document** (2026-09-12)
  - New `docs/phases/PHASE4-ADBLOCK-DESIGN.md`: the `adblock/` patch-area
    design — `URLLoaderThrottle` interception with deferred
    background-thread decisions (rationale table vs. alternatives),
    resource-type mapping table (`RequestDestination` → engine
    `ResourceType`), main-frame not filtered in v1 (documented policy),
    component design (`inweb_adblock_service` / `_throttle` /
    `_request_context` / `_jni`), atomic engine-snapshot swap on
    refresh, provisioning via `FilterListManager` (ADR-014 policy
    model), planned registry entries 0005–0007 (added only when
    generated against the real tree), 4-layer verification strategy,
    and explicit honest boundaries (websocket/cosmetic deferred,
    unknown options excluded)
  - ADR-019 recorded; Phase 3 closed out as complete (all pure-JVM
    deliverables landed; enforcement remains B-001-gated)

- [x] **Step 15 — Phase 4: popup-protection patch-series design (§12)** (2026-09-12)
  - New `docs/phases/PHASE4-POPUP-PROTECTION-DESIGN.md`: five protection
    surfaces — (A) popup blocking at the window-creation consent point
    (user-activation policy + the real call site for `$popup` engine
    rules + blocked-popup chip with open-once/allowlist), (B) unwanted
    redirects: subframe-initiated top-level navigation (tab-takeover)
    guard via NavigationThrottle — user-driven same-tab redirects never
    blocked, (C) abusive notifications: quiet prompts + autoblocking
    with NO Safe-Browsing-class claim (no bundled remote service —
    stated), (D) automatic-download confirmation + engine-backed host
    checks, (E) deceptive-interaction guards explicitly deferred
  - Single per-site allowlist reused across protections (one shields
    list); components share the `0005` JNI engine service; the
    Security Center model gains protection counters only when the real
    wiring lands (§24)
  - Planned registry entries 0008–0011 (added only when generated
    against the real tree); 4-layer verification strategy
  - ADR-020 recorded; Phase 4 design pair complete

- [x] **Step 16 — Phase 4: cosmetic filtering — engine + patch design (§11)** (2026-09-12)
  - Core (implemented, tested): `CosmeticFilterParser` — `##` hide rules
    with domain includes/excludes, `#@#` exceptions, `#?#` procedural
    rules counted-not-matched, `#$#`/`#%#` counted unsupported, invalid
    counting; `CosmeticFilterEngine.hideCssFor(host)` — grouped hiding
    CSS with exception cancellation; `FilterListManager` parses cosmetic
    beside network lists and exposes `buildCosmeticEngine()` — 13 new
    tests
  - New `docs/phases/PHASE4-COSMETIC-DESIGN.md`: per-frame stylesheet
    injection at document-commit via the content-layer CSS API (worker-
    thread query, host from the committed origin), one-path policy
    (same settings/allowlist), planned registry entry
    `0012-cosmetic-injection` (adblock/ area), 4-layer verification,
    honest boundaries (flash-of-content documented; procedural/
    scriptlet deferred; cosmetic hides, never claims to block)
  - Phase 4 design trio complete (network / popup / cosmetic)
  - Kotlin total 207 (browser-shell 115 + tracking-protection 92)

- [x] **Step 17 — Phase 5: performance design & measured baseline (§9/§21/§52)** (2026-09-12)
  - New benchmark artifact: `scripts/benchmark_filter_engine.sh` +
    `src/core/tracking-protection/src/benchmark/.../FilterEngineBenchmark.kt`
    (synthetic EasyList-scale corpus, 25k rules, warmup, block/pass
    workloads; NOT in CI — noise, §9 protocol documented)
  - **Measured v1 baseline** (sandbox, 2 vCPU, OpenJDK 11): parse
    ~187k rules/sec; decisions **~16.5 ms (block) / ~18.1 ms (pass)**;
    heap ~111 MB — honest conclusion: v1 is correct but NOT production-
    viable; a 50–100-request page would cost ~0.8–1.8 s CPU
  - New `docs/phases/PHASE5-PERFORMANCE-DESIGN.md`: budgets as verification
    targets (per-request p95 < 1 ms, engine memory < 60 MB, startup
    off-critical-path, cold start within 10% of vanilla same-tag build);
    combined-matcher design (Step 18) with an explicit decision-
    equivalence correctness contract; decision cache (measure-first);
    device measurement plan (perfetto/`am start -W`/meminfo/
    batterystats A/B vs vanilla); benchmark log
  - ADR-022 recorded

- [x] **Step 18 — Phase 5: combined matcher (ADR-022 contract)** (2026-09-12)
  - `CombinedMatcher`: rules indexed by longest literal run (≥3 chars,
    specials `*`/`^` split runs); wildcard-only/short patterns →
    always-check bucket; substring-based URL lookup (no whole-token
    trap — pattern `banner123` found inside URL run `banner123x`);
    each rule in exactly one bucket → no dedupe; ordinals sorted to
    preserve first-exception/first-block semantics
  - `TrackingProtectionEngine.decide()` now evaluates only candidates;
    `matchingRulesNaive()` kept as the v1 equivalence oracle
  - 10 new tests incl. the 1200-rule mixed-corpus decision-EQUIVALENCE
    suite and the superset-semantics case (candidate ≠ match)
  - **Re-measured (same protocol/hardware):** block 16.5 ms → 0.10 ms
    (~165×), pass 18.1 ms → 0.07 ms (~259×), heap ~111 MB → ~21 MB,
    identical outcomes — target ≥100× met with zero differences;
    decision cache stays deferred (not warranted by 0.07–0.10 ms)
  - Kotlin total 217 (browser-shell 115 + tracking-protection 102)

- [x] **Step 19 — Phase 6: extension system design + Phase 5 close-out (§16)** (2026-09-12)
  - New `docs/phases/PHASE6-EXTENSION-DESIGN.md`: all eight §16 documentation
    items — supported/partial/unsupported API table (v1 target, to be
    confirmed by a build-time audit that regenerates the table from
    the real APK — release notes show reality, not intent);
    permission/installation (sideload-first, full warning review, no
    silent installs)/update (manual re-sideload; no CWS auto-update
    claim)/security models; MV3-primary with MV2 grace policy stated
    up front; Kiwi-proven architecture (enable + complete upstream
    subsystem on Android) with rejected-alternatives table; component
    design + planned registry entries 0013–0016 (high-risk enablement
    patch flagged for security review); 4-layer verification
  - Phase 5 CLOSED: measured baseline + budgets (verification
    targets), combined matcher (~165–259×, equivalence-verified,
    cache deferred); remaining Phase 5 work (device verification of
    budgets) is B-001-gated and listed with the build-infrastructure
    work items
  - ADR-023 recorded

- [x] **Step 20 — Phase 6: extension management core (§16 model)** (2026-09-12)
  - NEW MODULE `src/core/extensions` (pure JVM, 17 tests): the §16
    management model the `extension/` patches will bind to —
    `ExtensionRegistry` state machine (install → PENDING_REVIEW;
    review → DISABLED; enable ⇄ DISABLED/ENABLED; update with new
    unreviewed permissions → DISABLED_UPDATE — Chrome-style upgrade
    consent), `ExtensionVersion` (1–4 integer components, numeric
    comparison, trailing-zero normalization so equality/ordering/
    hashing agree), `ManifestVersion` MV2/MV3 (ADR-023 grace policy),
    `ExtensionStore` persistence seam + `InMemoryExtensionStore`,
    `RegistryResult`/`RegistryError` (expected failures are results,
    never fake success), real-state `RegistryCounts`
  - Rules enforced: no silent installs, no implicit permission
    grants, no enable without review — ever
  - Kotlin total 234 across THREE modules (browser-shell 115 +
    extensions 17 + tracking-protection 102); validate script runs
    the new module in CI

- [x] **Step 21 — Phase 7: offline reading & data-saving design (§17/§18/§19)** (2026-09-12)
  - New `docs/phases/PHASE7-OFFLINE-DESIGN.md`: reuse-not-rebuild strategy
    (upstream DOM distiller for reader mode, Android MHTML offline
    pages for snapshots, browsing-data remover for cache clearing);
    data saver = rule-class mode over OUR filter engine — counters
    count REAL avoided requests; "bytes saved" only as a labeled
    estimate; **proxy/compression explicitly OUT of scope** (§20
    truthfulness rules quoted); cache-vs-offline-storage separation
    ENFORCED in the data model (clear cache never deletes user
    snapshots); pure-JVM core scoping for Step 22 (offline library
    registry with quota + LRU eviction); planned registry entries
    0017–0019 (offline/ area); 4-layer verification
  - Phase 6 core work complete (design + management core); remaining
    extension work is patch-side (B-001)
  - ADR-024 recorded

- [x] **Step 22 — Phase 7: offline library core (§17 model)** (2026-09-12)
  - NEW MODULE `src/core/offline` (pure JVM, 14 tests): `OfflinePageRecord`
    (online URL key, snapshot file, REAL byte size, timestamps) +
    `OfflineLibrary` — insertion-order listing, same-URL saves REPLACE
    (old record returned, bytes stop counting), quota enforcement by LRU
    eviction (least-recently-accessed first; tie → older creation first;
    pinned pages never evicted; the saved record never evicts itself;
    quota failure is ATOMIC — no partial eviction), monotonic access
    times, explicit delete, real byte accounting
    (total/remaining), `OfflineStore` persistence seam +
    InMemoryOfflineStore; expected failures are results, not exceptions
  - ADR-024 invariants enforced in code: snapshots are user data
    (cache-clearing cannot touch them by construction); sizes are real
    values; eviction returns records — file deletion is the caller's job
  - Kotlin total 248 across FOUR modules (browser-shell 115 +
    extensions 17 + offline 14 + tracking-protection 102)

- [x] **Step 23 — Phase 8: VPN & security design (§15/§25/§26/§27)** (2026-09-12)
  - New `docs/phases/PHASE8-VPN-SECURITY-DESIGN.md`: VPN = REAL Android
    VpnService + WireGuard-protocol client with a BRING-YOUR-OWN-server
    model — iNWEB operates no servers, no traffic-protection claims
    beyond the user's endpoint; full-tunnel routing, DNS through the
    tunnel, kill-switch via Android always-on + block-without-VPN; a
    cosmetic VPN toggle is named as a §15 violation that does not
    exist here. Biometric = BiometricPrompt + Keystore
    (setUserAuthenticationRequired) AES/GCM envelope — data actually
    encrypted, v1 scope: app lock + incognito gate; "protected
    bookmarks" explicitly NOT claimed until encrypted stores exist
    (§25). 2FA = documented ABSENCE — no auth backend, no meaningless
    UI (§26); TOTP noted for whenever a backend is designed. Encryption
    = platform primitives only, envelope model, no hand-rolled crypto,
    no hard-coded secrets (repo-auditable). §28 profiles deferred to
    Phase 9
  - Pure-JVM core scoping for Step 24 (VPN config validation);
    planned registry entries 0020 (app lock) + 0021 (VPN tunnel,
    high-risk, security review); 4-layer verification incl. DNS/kill-
    switch leak tests
  - Phase 7 core work complete (design + offline library); remaining
    offline work is patch-side (B-001)
  - ADR-025 recorded

- [x] **Step 24 — Phase 8: VPN configuration validation core (§15 model)** (2026-09-12)
  - NEW MODULE `src/core/vpn` (pure JVM, 17 tests): `VpnConfigParser` —
    WireGuard-style INI parsing with STRICT structural validation
    (exact section/field names; unknown sections/fields REJECTED, never
    silently ignored — wg-quick host features named explicitly);
    required keys; base64 keys of exactly 32 bytes; IPv4/IPv6 + CIDR
    with correct prefix ranges (leading-zero prefixes rejected);
    host:port endpoints (domain / IPv4 / [IPv6]); port/keepalive
    ranges; list fields may span lines, scalars may not repeat; ALL
    issues collected, never fail-fast
  - `SecretValue`: private/preshared keys opaque by construction —
    toString always redacts (tested); the PUBLIC key is documented as
    public; no crypto in the core — the Android layer owns key
    handling (ADR-025)
  - Kotlin total 265 across FIVE modules (browser-shell 115 +
    extensions 17 + offline 14 + vpn 17 + tracking-protection 102)

- [x] **Step 25 — Phase 9: profiles/sync/backup design (§28/§29/§30/§31/§32)** (2026-09-12)
  - New `docs/phases/PHASE9-PROFILES-SYNC-DESIGN.md`: profiles with REAL
    isolation — per-profile namespaces for every iNWEB-owned store
    (isolation enforced at store construction, not cosmetic names) and
    per-profile Chromium user-data dirs for cookies/cache; cloud sync =
    DOCUMENTED ABSENCE (no backend, no sync UI — §29); §30 queue model
    designed transport-agnostic (append-only change log, retry/backoff,
    last-writer-wins + tombstones — no CRDT claims) as future
    infrastructure, never announced as a feature; §31 rule restated
    (SQLite for app data, no Chromium-storage duplication — already
    followed); §32 backup = versioned `iNWEB-BACKUP v=1` bundle with
    manifest, per-entry SHA-256, forward-only migration, restore
    PREVIEW before import, per-entry corruption tolerance, Keystore-
    envelope encryption at the Android layer (no plaintext secrets, no
    core crypto)
  - Pure-JVM core scoping: Step 26 profiles core, Step 27 backup
    bundle core; planned registry entries 0022-ui-multi-profile
    (high-risk, isolation review) + 0023-settings-backup-restore; NO
    sync patch (nothing to patch)
  - Phase 8 core work CLOSED (design + VPN config core; app-lock/VPN
    patches B-001-gated)
  - ADR-026 recorded

- [x] **Step 26 — Phase 9: profiles core (§28 model)** (2026-09-12)
  - NEW MODULE `src/core/profiles` (pure JVM, 12 tests): `ProfileRecord`
    (monotonic `p-N` ids, NEVER reused — a deleted namespace can never
    resurrect stale data), `ProfileRegistry` — empty store seeds a
    default active profile (a real browser always starts with one);
    create (inactive) / rename (trim + blank rejection) / setActive /
    delete (active deletion falls back to the oldest remaining; the
    last profile cannot be deleted; deleted profile + new active
    reported for teardown); `namespaceOf(id)` = the store-routing
    contract (namespace = profile id; the app layer maps it to
    `<dataDir>/profiles/<id>/`); `ProfileStore` persistence seam +
    InMemoryProfileStore; expected failures are results, not exceptions
  - §28 isolation made structural (ADR-026): stores are constructed
    against exactly ONE namespace — shared storage cannot occur
  - Kotlin total 277 across SIX modules (browser-shell 115 +
    extensions 17 + offline 14 + vpn 17 + profiles 12 +
    tracking-protection 102)

- [x] **Step 27 — Phase 9: backup bundle core (§32 model)** (2026-09-12)
  - NEW MODULE `src/core/backup` (pure JVM, 13 tests): `iNWEB-BACKUP
    v=1` line format — manifest (format/app-version/created-at/entries;
    corruption aborts the whole restore), per-entry SHA-256 checksums
    (platform primitive — no custom crypto), canonical payload form
    (trailing-newline-free, exact round-trip), per-entry corruption
    tolerance (skipped + REPORTED, never silent), version gating
    (newer formats refused with an explicit upgrade message; older
    formats need an explicit migration function — none exist yet),
    restore PREVIEW (per-profile/per-store counts + warnings) before
    any import, and the §32 no-plaintext-secrets guard: only
    KNOWN_STORES may be bundled — unknown store names rejected at
    build and skipped at parse (credentials/VPN keys can never enter)
  - Kotlin total 290 across SEVEN modules (browser-shell 115 +
    extensions 17 + offline 14 + vpn 17 + profiles 12 + backup 13 +
    tracking-protection 102)

- [x] **Step 28 — Phase 9: sync queue core (§30 model, future infrastructure)** (2026-09-12)
  - NEW MODULE `src/core/sync` (pure JVM, 12 tests): `SyncEngine` —
    durable append-only change log with MONOTONIC revisions; batch
    flush with retry gating; `RetryPolicy` (exponential backoff,
    capped, max attempts — exhausted retries move the batch to FAILED
    with an explicit reason, never silently dropped); permanent
    transport failures reported immediately; `ConflictResolver` —
    last-writer-wins with tombstones (newer upsert legitimately beats
    an older tombstone and vice versa; exact ties resolve to the
    LOCAL entry deterministically; NO CRDT claims); `SyncTransport`
    seam exercised against a scriptable fake; `SyncQueueStore`
    persistence seam with round-trip incl. durable retry state
  - §29 honesty: the module README and KDoc state FUTURE
    INFRASTRUCTURE — no backend exists, nothing may present it as a
    feature
  - **Phase 9 core work COMPLETE** (design + profiles + backup +
    sync cores; patches 0022–0023 remain B-001-gated)
  - Kotlin total 302 across EIGHT modules (browser-shell 115 +
    extensions 17 + offline 14 + vpn 17 + profiles 12 + backup 13 +
    sync 12 + tracking-protection 102)

- [x] **Step 29 — Phase 10: localization & accessibility design + CI gate (§36/§49)** (2026-09-12)
  - NEW `scripts/validate_localization.py` — the §36 externalization
    gate, LIVE in CI: authored UI (ui/**, MainActivity.kt) may not
    contain hardcoded user-visible literals (Text positional +
    text/label/title/contentDescription/description/placeholder
    named params); documented exemptions (empty, digits, ${}
    interpolation, reviewed // NON-LOCALIZED hatch reported as a CI
    note); 8 unit tests for the gate itself (flags, exemptions,
    scope, entry file, missing-UI error)
  - Current authored UI: ZERO violations, ZERO escape hatches — fully
    externalized with full bn-BD parity (validate_strings continues
    to enforce bidirectional key + placeholder parity)
  - New `docs/phases/PHASE10-LOCALIZATION-ACCESSIBILITY-DESIGN.md`: §36
    policy (en+bn-BD first-class, same-commit parity, plurals/locale
    formatters, no machine-translation claims, prose quality =
    human-review deliverable) + §49 authoring contracts (TalkBack
    labels, 200% font scale, WCAG AA contrast via M3 tokens only,
    48dp targets, keyboard/D-pad, reduced motion) as mandatory review
    gates on every ui/settings patch; verification matrix (CI-now vs
    device-at-B-001)
  - ADR-027 recorded; Python total 38 (30 + 8)

- [x] **Step 30 — Phase 11: notifications & advanced features design (§33/§34/§23)** (2026-09-12)
  - New `docs/phases/PHASE11-NOTIFICATIONS-FEATURES-DESIGN.md`: §33 policy —
    minimal-by-default, real events only, every channel toggleable,
    POST_NOTIFICATIONS requested lazily at the first REAL notification
    (§41-aligned); v1 channel plan mapped to real event sources
    (downloads now; security/VPN when their patches land — NO stub
    channels); explicitly ABSENT: sync (no backend, §29), self-update
    (no infrastructure), promotional/engagement notifications (policy
    violation). §34 entertainment = documented NON-GOAL for v1 (base
    APK stays lean, §7); any future version = separate installable
    component with its own APK-size accounting. §23 customization
    table — every lever mapped to a REAL existing mechanism (no
    lever without a mechanism); toolbar configuration = Step 31 core
  - ADR-028 recorded; Phase 10 closed (gate + design live; device
    accessibility verification B-001-gated)

- [x] **Step 31 — Phase 11: customization core (§23 toolbar configuration)** (2026-09-12)
  - New pure-JVM module `src/core/customization` (package
    `com.inweb.browser.customization`), registered as the 9th core
    module in `scripts/validate_kotlin_core.sh`; 25 Kotlin tests,
    all passing (Kotlin total 327)
  - `ToolbarItem`: the item universe mirrors the AUTHORED bottom bar
    exactly (back, forward, home, tabs, menu — no invented items);
    stable string ids; mandatory = back/tabs/menu (navigation
    escape, session surface, settings/menu surface — the bar's
    structural controls); forward/home are genuine preferences
  - `ToolbarConfig.parse`: strict validation of untrusted stored
    data in a fixed check order — duplicates → unknown → missing →
    unknown-hidden → mandatory-hidden — every offender reported
    (nothing silently dropped or invented to "fix" a corrupt file)
  - `ToolbarConfigurator`: move (remove-then-insert, full-list
    index), setVisible (mandatory hide rejected; hidden keeps its
    slot so show restores position), reset; every mutation
    re-validated + persisted through the `ToolbarStore` seam
    (`InMemoryToolbarStore`; preference-backed store ships with the
    ui/ patch binding)
  - Corrupt stored config → authored-default fallback that is
    PERSISTED and reported via `lastRecovery()` (never a crash,
    never a silent ignore)
  - Planned registry entry ui/0024 (toolbar-configuration surface
    bound to this core) — B-001-gated like every patch
  - ADR-029 recorded

- [x] **Step 32 — Phase 11: notification policy core (§33)** (2026-09-12)
  - New pure-JVM module `src/core/notifications` (package
    `com.inweb.browser.notifications`), registered as the 10th core
    module in `scripts/validate_kotlin_core.sh`; 23 Kotlin tests,
    all passing (Kotlin total 350)
  - Registry = the design §1 table EXACTLY: 4 channels (downloads,
    security, vpn, background), 8 events each mapped to its channel;
    the unit test is the scriptable audit (registration matches §1,
    no extra channels). Deliberate absences are STRUCTURAL — no enum
    value exists for sync (no backend, §29), self-update (no
    infrastructure), promotional (§41 policy), or filter-list update
    failure (silent by design; Security Center surfaces it)
  - `NotificationPolicy.decide` is the ONLY notify path: a typed real
    event → Show / RequestPermission / Suppress(reason); no generic
    notify(channel, text) API exists, so promotional content has no
    way in. Fixed check order: availability → user toggle →
    permission (the lazy ask fires ONLY for a show-worthy event)
  - Availability is EXPLICIT (required constructor set, no default):
    the build states which event sources are real; an unavailable
    channel is never registered and its events are always suppressed
    (VPN channel absent until patch 0021 — no stub)
  - Lazy POST_NOTIFICATIONS state machine: NOT_REQUESTED → REQUESTED
    (first show-worthy event) → GRANTED/DENIED; never asked at
    startup, never twice, never re-asked after denial; an observed
    system-setting change (onSystemPermissionChanged) is authoritative
    from any phase (covers <Android 13 auto-grant and settings-app
    changes)
  - Per-channel toggles persist through the `NotificationStore` seam;
    corrupt stored preferences recover to defaults with the repair
    persisted + reported (lastRecovery) — never a crash, never a
    silent ignore
  - ADR-030 recorded

- [x] **Step 33 — Phase 11: authored binding of the customization + notification cores (§23/§33)** (2026-09-12)
  - BrowserViewModel wired to both cores: the bottom bar now RENDERS
    from the toolbar configuration (order + visibility from
    ToolbarConfigurator; mandatory items locked by core invariant);
    toolbar actions (move / setVisible / reset) and notification
    toggles write through the cores and refresh observable state
  - New `ui/CustomizeToolbarScreen` (§23): every entry in the user's
    order with accessible up/down buttons and a visibility switch
    (mandatory items: switch disabled + "Always visible" hint; the
    switch carries a semantics contentDescription); reset-to-default
    in the top bar; reached from Settings
  - SettingsScreen: Notifications section (§33) — ONLY the available
    channels are listed (downloads in this authored build; the set
    grows as patches 0018/0020/0021 land — no stub channels) with
    per-channel toggles and the honest policy caption;
    Customize-toolbar navigation row
  - decideNotification / onNotificationPermissionResult /
    onSystemNotificationPermissionChanged exposed as explicit binding
    points (B-001): the engine/download adapter is the only future
    caller — NOTHING notifies in the authored shell (§57)
  - Preference adapters (SharedPreferencesToolbarStore /
    SharedPreferencesNotificationStore) are deliberately thin: they
    round-trip the wire form only; validation and corrupt-preference
    recovery stay in the cores (ADR-029/030 layering)
  - 12 new strings in en + bn-BD in the same commit (§36): 6 toolbar
    + 6 notification keys; parity + externalization gates green
  - DEFECT FOUND AND FIXED (§64 loop): BrowserViewModel declared two
    properties named `history` (state var + PrivacyFilterHistory
    source) since Step 4 — a redeclaration that would have failed the
    B-001 compile; found by the first structural pass over the
    authored sources, fixed by renaming the source property
    (historySource)
  - NEW GATE: `scripts/validate_authored_structure.sh` — kotlinc
    structural check (conflicting declarations / redeclarations /
    syntax errors) over every authored .kt file; unresolved
    references are excluded by design (no AndroidX classpath).
    Verified: 18 files green and a planted redeclaration is caught;
    wired into the CI Kotlin job
  - ADR-031 recorded

- [x] **Step 34 — Phase 11: settings cores for the remaining §23 levers (page zoom + download preferences)** (2026-09-12)
  - Two new preference models in the browser-shell settings core
    (same module as AppSettings; own store seams — the AppSettings
    data class is unchanged, its authored-UI wiring comes with a
    future binding step, Step-33 pattern):
  - `ZoomPreferences`/`ZoomSettings` (§23 page zoom): validated
    default factor + per-site overrides keyed by normalized host
    (trim + lowercase); bounds match upstream Chromium's supported
    page-zoom range 25%–500% (verified against the upstream preset
    list); NaN/∞ rejected by the bounds check; parse validates in a
    fixed order (default bounds → blank hosts → site-factor bounds →
    hosts colliding after normalization = ambiguous) with every
    offender reported; corrupt stored data recovers to defaults with
    the repair persisted + reported (ADR-029/030 pattern); the
    settings/ patch binds to Chromium's OWN zoom mechanism — no
    custom renderer scaling
  - `DownloadPreferences`/`DownloadSettings` (§23 download
    preferences): ask-before-download (default YES — downloads never
    begin silently) + default folder (null = the platform's public
    Downloads directory, the only real default; otherwise a
    non-blank user-picked directory reference, trimmed on store;
    blank rejected); same corrupt-recovery contract
  - 32 new tests (ZoomPreferencesTest 21 + DownloadPreferencesTest
    11), both registered in the core script; browser-shell 115 →
    147, Kotlin total 382
  - ADR-032 recorded; Phase 11 authoring work complete (design §33/
    §34/§23 + cores + authored binding + settings cores — remaining
    Phase 11 items are B-001-gated patch entries)

- [x] **Step 35 — Phase 12: hardening design + storage inventory gate (§50/§51)** (2026-09-12)
  - New `docs/phases/PHASE12-PRODUCTION-HARDENING-DESIGN.md`: §53's seven
    bullets (security/performance audit, regression/crash testing,
    release engineering, documentation, store readiness) each mapped
    to concrete pre-B-001 vs B-001-gated work with status; proposed
    order for the remaining items; honest boundaries (no
    running-app claims before B-001; no crash-reporting
    infrastructure exists and none is claimed)
  - REFERENCE CORRECTION recorded: the Step-34 proposal's "§46 crash
    reporting / §47 storage clearing" citations were wrong (those are
    CI/CD and Release Channels); correct anchors are §50 Error
    Handling ("never silently lose user data") and §51 Crash/Recovery
  - FIRST ITEM IMPLEMENTED — storage inventory gate: new
    `docs/STORAGE-INVENTORY.yaml` (21 surfaces: 14 seams + 7 adapters;
    each documents what it stores, where, its corruption/recovery
    contract, and its clear semantics) + new
    `scripts/validate_storage_inventory.py` with 12 unit tests, wired
    into the CI Python job. Bidirectional CI enforcement: every
    persistence surface discovered in code (Store/Persistence/Cache
    interfaces, File*/SharedPreferences* adapters, prefs names) MUST
    have an entry; stale rows fail; `discoverable: false` is the
    reviewed manual-extra hatch, reported in gate output
  - The inventory's clear column is the contract for the future
    clear-data surface; the corruption column IS the §51 audit of
    recovery contracts; in-memory-only seams state which patch will
    persist them
  - Python total 50 (38 + 12); Kotlin unchanged 382 (10 modules);
    all 5 gates + registry + strings + externalization + structure
    green locally
  - ADR-033 recorded

- [x] **Step 36 — Phase 12: threat-model review pass (§42 re-validation, design item 1)** (2026-09-12)
  - New `docs/THREAT-MODEL-REVIEW.md`: THREAT-MODEL v0.1 (Phase 0)
    walked against the 10 core modules, the authored app, the ADR log
    (001–033), and the storage inventory — every threat row mapped to
    a status (implemented-with-evidence / designed / planned /
    documented-absence) citing module + tests + ADR
  - 14-row coverage matrix; NEW threat row registered: notification
    abuse (impersonation, promotional pressure) — structurally
    mitigated by ADR-028/030 (no generic notify path, no promotional
    code path, per-channel toggles, lazy permission)
  - GAP REGISTER: 10 tracked items (G-01..G-10), none silently
    assumed — download-safety logic, fingerprinting scope, Keystore/
    biometric binding (0020), backup encryption, per-profile dirs
    (0022), sync E2E (future infra, §29), filter-list content
    integrity (candidate, not committed), SBOM, device verification
    of corruption contracts, extension enforcement patches (0013–0016)
  - THREAT-MODEL.md updated to v0.2: notification-abuse row added,
    data-loss row extended to name the storage inventory (ADR-033),
    residual risks refreshed (extensions scope now DECIDED per
    ADR-023 — the Phase 0 "undecided" note was stale), review pointer
    added
  - Verdict: no threat row uncovered by design; all runtime claims
    remain B-001-gated
  - Docs-only step: local gates re-run green (Python 50/50, storage
    inventory 21 surfaces); CI skips by paths-filter design — last
    full-green run 34669595528 (Step 35)

- [x] **Step 37 — Phase 12: clear-browsing-data core (design item 2)** (2026-09-12)
  - New pure-JVM module `src/core/clear-data` (package
    `com.inweb.browser.cleardata`) — the FIRST cross-module core:
    `scripts/validate_kotlin_core.sh` gained a `--deps` mechanism
    (dependency modules' build outputs on the compile+test classpath;
    missing-dependency ordering fails the script) and clear-data is
    registered after browser-shell/tracking-protection/offline
  - `ClearDataItem`: the item universe is EXACTLY the
    STORAGE-INVENTORY clear column — history, session,
    filter-list cache, offline pages, per-site zoom overrides;
    deliberately NOT items: bookmarks (deliberate user data),
    preference stores (app reset only), downloads catalog (nothing
    persisted yet — joins when the patch lands)
  - Five REAL bindings, each delegating to a store API that already
    existed (HistoryStore.clearAll, SessionPersistence.clear,
    FilterListCache.clear) or was added with this step for the same
    purpose (OfflineLibrary.clearAll — offline 14→15 tests;
    ZoomSettings.clearSiteZooms — browser-shell 147→148 tests): NO
    invented clearing paths
  - `ClearDataManager`: duplicate-binding guard (fail-fast),
    canonical availableItems, `preview(items)` dry-run with REAL
    counts (null = not countable, e.g. the disposable filter-list
    cache) that never mutates, `clear(items)` with validation — a
    selection naming an UNBOUND item is rejected (never silently
    skipped); selection is transient dialog state, not a persisted
    preference
  - 14 tests using the REAL store implementations (in-memory stores,
    the file-backed FileFilterListCache with a temp directory, the
    real OfflineLibrary/ZoomSettings holders)
  - Kotlin total 398 (11 modules); all gates green locally
  - ADR-034 recorded

- [x] **Step 38 — Phase 12: authored clear-browsing-data surface (§39, bound to the clear-data core)** (2026-09-12)
  - New `ui/ClearDataScreen`: every row IS a core item with its REAL
    dry-run count; the filter-list cache row says it re-downloads
    (not countable by design); rows use toggleable + Role.Checkbox
    semantics (§49); confirm disabled for an empty selection (the
    surface prevents the core's EmptySelection error); Settings nav
    row + screen routing
  - BrowserViewModel bound to ClearDataManager with the five REAL
    bindings (history via the PrivacyFilterHistory decorator, session
    persistence, filter-list cache, offline library, zoom settings);
    previews refreshed on open; selection is transient dialog state
    (ADR-034)
  - New `SharedPreferencesZoomPreferencesStore` adapter (app
    preferences `inweb_zoom`; wire form = factor string +
    comma-joined host=factor pairs; value-level validation/recovery
    stay in the core per ADR-031 layering — the wire-format boundary
    is documented in the inventory entry)
  - New `InMemoryFilterListCache` in the tracking-protection core
    (tests/previews; +4 tests, module 102 → 106) — the ViewModel
    default; MainActivity binds the real FileFilterListCache
    (`filesDir/filter-lists`)
  - STORAGE-INVENTORY.yaml: 22 surfaces (zoom adapter + seam location
    updated) — the gate caught the new prefs surface exactly as
    designed
  - 13 new strings in en + bn-BD same commit (§36); parity +
    externalization + structural gates green (20 authored files);
    Kotlin total 402 (11 modules)
  - Honest boundary: nothing clears on a device until the build
    exists (B-001); the surface is authored source verified by gates
- [x] **Step 39 — Phase 12: §23 settings-surface binding (zoom + download preferences, bound to the preference cores)** (2026-09-12)
  - `ZoomPreferences.PRESET_FACTORS` added to the browser-shell core
    (upstream Chromium's preset zoom steps, ascending — the table the
    surface AND the future engine-adapter page-zoom control reuse);
    +2 tests (ascending/unique/in-bounds; spans 25%–500%, contains
    the 100% default), module 148 → 150
  - New `ui/ZoomSettingsScreen`: default factor chosen from the core's
    preset table (bounds-validated through `ZoomSettings`; the surface
    offers only core-valid steps, Err branch kept defensive); per-site
    override management lists ONLY what actually exists (creators are
    the page-zoom control on the engine side, §57 honesty rule) with
    per-host removal; Settings nav row + screen routing
  - New `SharedPreferencesDownloadPreferencesStore` adapter (app
    preferences `inweb_downloads`; wire form = ask flag + folder
    string, absent folder key = the platform's public Downloads
    directory; value-level validation/recovery stay in the core,
    ADR-031/032)
  - Settings downloads section: ask-before-download Switch + default
    folder row bound to `DownloadSettings`; the folder row launches
    the REAL system SAF picker (OpenDocumentTree) and persists the
    tree URI through the core; a custom folder releases back to the
    system folder; SettingsScreen column made scrollable
  - STORAGE-INVENTORY.yaml: 23 surfaces (download adapter + seam
    location updated); 11 new strings en + bn same commit (§36);
    parity + externalization (0 hatches) + structural (22 authored
    files) gates green; Kotlin total 404 (11 modules)
  - Honest boundary: no preference affects a real download or page
    until the build exists (B-001); authored source verified by gates
- [x] **Step 40 — Phase 12: §47 versioning policy (docs/VERSIONING.md, design-order item 3)** (2026-09-12)
  - New `docs/VERSIONING.md`: SemVer 2.0.0 with the §47 example start
    `1.0.0-alpha.1` and an explicit pre-release ladder
    (alpha → beta → rc → stable); bump rules (MAJOR = data-format
    break without forward migration, MINOR = feature work, PATCH =
    fixes incl. ANY Chromium baseline refresh at minimum)
  - versionName = the exact semver string; versionCode = strictly
    monotonic integer with a documented derivation
    (MAJOR×1,000,000 + MINOR×10,000 + PATCH×100 + channelRank) PLUS
    an overflow rule for the patch-line-after-stable case, recorded
    per-tag in an authoritative release registry (Appendix A — empty
    today, filled only at real tags)
  - Tag naming: annotated `v<semver>` tags only, the pre-release
    identifier IS the channel marker; each tag's annotation carries
    the full §45 release record (semver, versionCode, date, Chromium
    baseline, patch-set hash, container digest, changelog summary);
    CHANGELOG.md is created with the first tagged release — no empty
    placeholder (§57)
  - Channels per §47: Development (untagged main builds), Beta
    (v*-beta.*), Stable (promoted only after the §46 release-
    validation gate); NO separate canary line in v1 — introducing one
    later requires a new ADR; the iNWEB version and the pinned
    Chromium baseline are independent axes (ADR-001)
  - ADR-035 recorded; Phase 12 design doc items 1–3 marked done with
    step numbers (§59 accuracy); docs-only change — no CI run by the
    paths-filter's design (same as Step 36)

- [x] **Step 41 — Phase 12: B-001 device-verification matrix (docs/DEVICE-VERIFICATION-MATRIX.md, design-order item 4)** (2026-09-12)
  - ONE runnable checklist consolidating the per-phase B-001
    verification items already written across the phase designs
    (PHASE2 §4, PHASE4 ×3, PHASE5 §5, PHASE6–PHASE11 verification
    sections, THREAT-MODEL-REVIEW gap register): every row cites its
    source design; where they disagree the design doc wins (§59)
  - Protocol: Stage 0 (build/patch level: apply+verify, hash
    invariant, GN build, install/launch, C++ suites, build-time
    audits, signing, SBOM) gates every device row; results recorded
    in a verification log with build id + hardware; FAIL = filed
    defect, SKIPPED = feature not claimed (§57); full matrix = the §46
    release-validation gate before stable promotion
  - 51 device rows across Phases 2–12 (D2-1…D12-5) + 8 Stage-0 build/patch
    rows (B-1…B-8) + 3 release gates
    (R-1…R-3 per VERSIONING.md) + G-01…G-10 closure map (G-03→D8-1,
    G-04→D9-2, G-05→D9-1, G-08→B-8, G-09→D12-1/2, G-10→D6-4;
    documented absences stay documented)
  - Honest state: every row PENDING — no build exists (B-001), the
    patch registry is intentionally empty (patches: [], §57); the
    verification log (Appendix B) is empty and that emptiness is the
    truth; no partial credit, no "should work"
  - ADR-035 unchanged (no new ADR — this is the consolidation of
    existing contracts, not a new decision); docs-only change — no CI
    run by the paths-filter's design

- [x] **Step 42 — Phase 12: performance budgets table + store-readiness data-safety draft (design-order item 5 — Phase 12 authoring COMPLETE)** (2026-09-12)
  - New `docs/PERFORMANCE-BUDGETS.md`: P-1…P-9 verification targets —
    the five PHASE5 §2 budgets (decision p95, page-load overhead,
    engine init, engine memory, cold start) consolidated plus the
    §7 APK-size budget (≤ vanilla same-tag + 10%; the absolute
    reference is fixed by the FIRST vanilla baseline build, never
    invented), §21 battery (< 5% drain delta, A/B scripted session)
    and §21/§52 background red line (patches never defeat upstream
    tab-freezing/timer-throttling), §9 scrolling; each row names its
    device-matrix verification row (D5-1…D5-4); rules: budgets are
    targets until measured, missed budget = filed defect (never a
    rewritten budget — ADR-gated), comparisons always vs a vanilla
    build of the SAME tag; measured-log discipline with hardware +
    build id; the ONLY measured facts today remain the JVM benchmark
    rows (v2 combined matcher 0.10 ms / ~21 MB, decision-equivalent)
  - New `docs/DATA-SAFETY-DRAFT.md`: Play "Data safety" answers
    GENERATED from the 23-surface storage inventory (inventory wins on
    disagreement, §59): NO data collected, NO data shared (no
    telemetry/analytics/crash-upload/developer servers — §41, ADR-025/
    026); per-surface on-device table (where it lives, what clears it,
    "leaves the device? never"); deletion story (clear-browsing-data
    5 items + per-entry deletions + app reset + uninstall); local
    security practices (Android sandbox, SecretValue, pending-patch
    rows stated as pending); honest boundary: source-level draft, the
    final declaration follows the D12-3 built-APK audit
  - Phase 12 design doc: item 5 marked done, status = ALL FIVE order
    items implemented — Phase 12 authoring COMPLETE; every remaining
    Phase 12 deliverable is B-001-gated (device matrix)
  - Docs-only change — no CI run by the paths-filter's design (as
    with Steps 36/40/41)

- [x] **Step 43 — §49 source-level accessibility audit of the authored surfaces + fixes** (2026-09-12)
  - Walked all 13 ui/ files + MainActivity against the §49 contracts
    (PHASE10 §3); findings register recorded in
    docs/ACCESSIBILITY-AUDIT.md and FIXED in the same step
  - 2 VIOLATIONS fixed: A-1 tab-close button was a 28dp touch target
    (§49 minimum 48dp) — now the default 48dp; A-2 light-theme primary
    #00897B measured 4.22:1 as text / 4.34:1 for white-on-primary
    (WCAG AA needs 4.5:1) — palette primary darkened to #00796B
    (5.16:1 / 5.28:1; dark theme 9.8:1 unchanged), computed ratios
    recorded in Theme.kt
  - 5 GAPS fixed: A-3 onboarding engine rows now whole-row
    selectable(Role.RadioButton); A-4 all switch rows (notification
    channels, ask-before-download, toolbar visibility) now whole-row
    toggleable(Role.Switch) with display-only Switch; A-5 tabs badge
    announces the localized tabs_count sentence, not a bare digit;
    A-6 heading semantics on 9 section headers/headlines; A-7 the
    onboarding step indicator is a polite live region
  - No new strings needed (A-5 reuses the existing tabs_count) —
    en/bn parity untouched; PASS table recorded (icon labels, checkbox
    pattern, sp text, M3 tokens, ProgressBarRangeInfo, focus order,
    zero animations = trivially reduced-motion compliant)
  - Honest boundary: source-level proof only — TalkBack, 200% scale,
    measured contrast, D-pad stay on device-matrix rows D10-1…D10-5
  - Gates green: structural 22 files, externalization 0, strings,
    registry, Python 50/50, Kotlin 404 (11 modules) — src/ changed,
    so CI runs on push

- [x] **Step 44 — G-07 filter-list content checksum pinning (tracking-protection core hardening, threat-register gap closed)** (2026-09-12)
  - New `FilterListChecksum` (SHA-256 via MessageDigest, lowercase
    hex; platform crypto only, ADR-025): every downloaded list body is
    PINNED with its SHA-256 in the cache metadata and re-verified on
    every cache load (startup + refresh) — a mismatching copy is
    NEVER served; the manager re-downloads when allowed or reports an
    honest Failed(servedFromCache=false) with nothing loaded
  - `FilterListMetadata.contentSha256` (nullable; wire format gains
    an optional line — no v=1 file exists in the wild since no
    artifact has ever shipped, B-001); legacy copies without a
    checksum are served as-is (documented tolerance)
  - +10 tests: FilterListChecksumTest (3: FIPS vectors,
    determinism/format, any-change-changes-digest) + cache wire
    round-trip + legacy-deserialize (2) + manager (5: pin-on-download,
    tampered-body re-download, never-serve-when-offline, legacy cache
    tolerance, corrupted-cache refresh); module 106 -> 116, Kotlin
    total 414; script registration (before the success banner)
  - Docs aligned: THREAT-MODEL-REVIEW G-07 CLOSED (row 12 now G-08
    only), DEVICE-VERIFICATION-MATRIX D12-1 + closure map updated,
    STORAGE-INVENTORY FilterListCache corruption contract extended
  - Honest boundary: integrity pinning, NOT a publisher signature —
    no upstream list signs content and no such claim is made (§57);
    on-device fault injection stays on D12-1

- [x] **Step 45 — B-001 build-host provisioning assistance (pre-flight checker + runbook)** (2026-09-12)
  - New `scripts/check_build_host.py`: verifies EVERY
    BUILD-INFRASTRUCTURE §2 row on the host BEFORE the multi-hour
    fetch — OS/arch (Linux x86-64 mandatory), CPU (16/32), RAM
    (64/128 GB), disk (300/500 GB, workspace filesystem), tools
    (git/python3 mandatory, curl), depot_tools (INFO: the fetch
    script clones it), Docker (WARN), self-hosted-runner reminder,
    repository completeness (pinned baseline + GN args + scripts),
    and outbound HTTPS reachability of the four endpoints
    (--skip-network to skip); every row cites its spec line; exit
    0/1/2; +22 unit tests (Python 50 -> 72): meminfo parsing, spec
    band boundaries, disk-probe ancestor walk, os-release parsing,
    Ubuntu 22.04/24.04 classification, repo completeness, mocked
    network reachability, report counts/verdicts, structural smoke
    run on the real repo
  - Smoke-run on the authoring sandbox: honest FAIL (2 cores, 1.9
    GiB RAM, 19.6 GiB disk — exit 1) — measured proof that the
    sandbox is not the build host (B-001, ADR-003); repo/baseline
    rows PASS
  - New `docs/BUILD-HOST-RUNBOOK.md`: the operational walkthrough —
    §1 provision per spec, §2 pre-flight, §3 fetch (pinned
    depot_tools + no-history checkout + gclient sync at tag,
    resumable), §4 first build (honest scoping: upstream
    chrome_public_apk until the ui/ patches land; apply/verify +
    reproducibility stamp), §5 Stage-0 matrix recording (B-1..B-4,
    verification log, defects never silent), §6 optional self-hosted
    runner (+ security notes), §7 what returns to the repo, §8
    honest boundaries (incl. check_baseline.py drift note)
  - BUILD-INFRASTRUCTURE.md §7 records both; no CI run needed for
    docs but scripts/+tests/ changed -> CI runs and covers the new
    tests

- [x] **Step 46 — authored-app About surface (§39, honest by construction)** (2026-09-12)
  - New `ui/AboutScreen`: app name; "Development build — no release
    exists yet" (the release registry is empty — no version number is
    shown until a tagged release exists to name, VERSIONING.md); the
    Chromium baseline row pinned to the mirrored resource; §49
    discipline carried over (heading semantics, back label,
    externalized strings); Settings nav row (reuses the existing
    settings_about string) + screen routing
  - Baseline single-source enforcement: the non-translatable
    `chromium_baseline` resource (154.0.8037.21) mirrors
    config/chromium/BASELINE, and scripts/validate_strings.py gained a
    CI-enforced cross-check (must exist, must be translatable=false,
    must equal the pinned CHROMIUM_TAG) — the app can never show a
    baseline the build does not use; +5 unit tests (Python 72 -> 77:
    synced/missing/drifted/translatable/unreadable-baseline)
  - 3 new translatable strings en+bn same commit (§36) + the
    non-translatable mirror (parity gate excludes it by design)
  - Gates green: strings (mirror in sync), externalization 0,
    structural 23 authored files, registry, inventory 23, Python 77,
    Kotlin 414 unchanged; src/+scripts/+tests/ changed -> CI runs

- [x] **Step 47 — §59 full-repository documentation accuracy pass** (2026-09-12)
  - Method: every living document walked against the tree at this
    commit — counts, file lists, status claims; historical step
    entries in PROJECT_STATE were left untouched (they were true when
    written — history stays honest)
  - Fixed drift: ARCHITECTURE §4a rebuilt (all 11 core modules with
    current test counts 150/116/17/15/17/12/13/12/25/23/14 = 414;
    android-app row now lists every authored surface incl. About);
    PHASE2 §1 table same treatment; PHASE4-ADBLOCK 79→116 (×2) and
    Python 30→77; PHASE4-COSMETIC 92→116; PHASE4-POPUP 79→116;
    PHASE6 extensions core "proposed"→implemented (17 tests);
    PHASE12 storage inventory 21→23 surfaces (14 seams + 9 adapters);
    THREAT-MODEL-REVIEW counts refreshed (row 5: 116; row 13: 23
    surfaces) + explicit amendment log + verdict re-phrased (G-07
    closed → 9 open gaps); ACCESSIBILITY-AUDIT amendment note
    (AboutScreen post-audit addition; 23 authored files);
    README tree: +4 scripts (validate_localization,
    validate_storage_inventory, validate_authored_structure,
    check_build_host), +3 core modules (customization,
    notifications, clear-data), misplaced subtree under sync fixed,
    docs/ excerpt made explicitly partial
  - Verified accurate as-is: README docs-table 28/28 rows; device
    matrix 51 D-rows; patch-subdir list; BUILD-INFRASTRUCTURE (Step
    45); DATA-SAFETY 23-surface figure; PHASE7/8/9/11 "live" claims;
    VERSIONING empty-registry statements; cosmetic 13-test claim
  - Docs-only change — CI skips by paths-filter design; gates
    re-run locally below

- [x] **Step 48 — live upstream baseline drift check: CURRENT; authoring pauses for B-001** (2026-09-12)
  - `python3 scripts/check_baseline.py` against the live Chrome
    Version History API: pinned 154.0.8037.21 == latest Android
    stable 154.0.8037.21 → **CURRENT** (exit 0); no refresh plan
    needed — cross-referenced with the weekly Upstream Watch workflow
    (last run 34641290453, green, 2026-09-11)
  - Pause certification — FULL local gate suite re-run at this
    commit: Kotlin 414/414 (11 modules, ALL MODULES PASSED),
    structural 23 authored files, Python 77/77, strings + baseline
    mirror in sync, registry, storage inventory 23, externalization 0
  - Authoring side is complete and accurate as of this step; the
    next repository event is B-001 Stage-0 results (or an upstream
    baseline move)

- [x] **Step 49 — B-001 Stage 1 executed on GitHub Actions (user directive: no local machine)** (2026-09-12)
  - New dispatch-only workflow `.github/workflows/b001-stage1.yml`
    (audit + attempt jobs, ubuntu-24.04): commit-SHA build-id,
    runner-fact collection, existing checker run, documented disk
    reclamation, pinned fetch, authoritative tag proof, empty-series
    apply/verify/hash, install-build-deps --android, 75-min boxed
    chrome_public_apk attempt, APK discovery + SHA-256, all logs as
    artifacts, PASS/FAIL/BLOCKED verdicts per step
  - 5 dispatched runs, 4 legitimate defects found and FIXED back
    into the repo (depot_tools bootstrap + single tag sync; checker
    network semantics — HTTP status = reachable; symlink-aware tree
    hash; development GN args — is_component_build forbidden on
    Android); Python tests 77 -> 80, CI 34689818244 green
  - Run 34691154428 (commit ae247a9) — RECORDED EVIDENCE: B-1 PASS,
    B-2 PASS (pristine hash d4212cfb…, HEAD == remote
    refs/tags/154.0.8037.21, chrome/VERSION match), B-3 BLOCKED on
    hosted resources (gn gen OK; 18,104/81,578 edges in 1h07m44s
    on 4 vCPU — corrected rate 267 edges/min: ~4.5–5.5 h remained,
    still over one 6-h job with setup; larger
    runners are an org-plan feature unavailable to this User-account
    repo), B-4 BLOCKED (no artifact); APK: NONE (honest). Appendix B
    row appended; full per-row evidence in
    docs/verification/B001-STAGE1-RUN5.md
  - No WebView fallback, no engine downgrade, no simulated success
    (§57/§71) — the resource wall is measured, not assumed

## In progress

- Step 50 — **COMPLETE (2026-09-22T11:28Z): `chrome_public_apk` BUILT on
  GitHub-hosted runners** (hop 14, run 35715711786, 65 min, commit 85b2ccf).
  Evidence: APK: FOUND; build_exit=0; ChromePublic.apk 689 MB, sha256
  098fff9c37b9d940d036be276924c651655d4a5e1b82f9c1e213bcc381c9799b
  (runner-computed == downloaded-artifact-computed); inspection: 11 classes.dex,
  lib/arm64-v8a/libchrome.so 468.5 MB (real engine, not WebView), bn.pak+en.pak
  locales. **B-3 evidence: the real Chromium build completes; B-4 evidence:
  real verified artifact** (verdict updates in the B-001 verification docs).
  This APK is the PRISTINE baseline (no iNWEB patches — B-001 gate); Stage 2
  resumes from state-13 (3.4 GB) for the patched iNWEB APK.
- **FIRST RELEASE — v1.0.0-alpha.1 (2026-09-22, author-directed):** GitHub
  Release https://github.com/iNAYATechLab/iNWEB-Browser/releases/tag/v1.0.0-alpha.1
  (prerelease, semver per §47), asset iNWEB-1.0.0-alpha.1-engine-baseline.apk
  (689 MB). Release asset sha256 verified identical to the build hash
  098fff9c…c9799b (runner → artifact → release, bit-for-bit). Release notes
  state honestly what the alpha is (real-engine baseline) and is not (iNWEB
  patch layer pending, Stage 2).
- **First field use (2026-09-22, author-reported, anecdotal — not a formal
  B-5 verdict):** the author installed v1.0.0-alpha.1 on their phone and
  conducted an interactive Arena.ai chat session through it — install,
  launch, networking, rendering and input all worked in real use. The
  author is the browser's first user; the build conversation now runs
  inside the browser it built. Author-verified data points (2026-09-22):
  Bengali text renders correctly in the alpha; device = Android 16
  (build BP2A.250605.031.A3), 1080x2340 FHD+ phone; install-time Settings screenshot — the image was held privately in the workspace uploads/ folder (never pushed to the public repo) and was removed on 2026-09-22 by explicit author direction after its corroborated facts were recorded here. Device-matrix data point #1 (author device, §48):
  Xiaomi Redmi 15 — Android 16 / HyperOS 3.0.304.0.WBOMIXM.CO7 (build
  BP2A.250605.031.A3, security patch 2026-07-01), Snapdragon 685 (SM6225,
  6 nm, arm64) + Adreno 610, 8 GB RAM (+8 GB virtual), 128 GB storage,
  6.9" IPS LCD 144 Hz 1080x2340, 7000 mAh. Build target arm64-v8a matches
  the SoC; EXIF build ID and resolution independently corroborate the
  author's report. Mid-tier hardware running the alpha smoothly = good
  low-RAM-class compatibility signal for §48. Full chain ledger,
  audit and result: docs/verification/B001-BUILD-CHAIN.md. Chain history: 14
  runs / ~45 h; ~24 h defect-attributable, all root causes fixed and proven by
  exact-remainder resumes (hops 10, 12, 13); verification separated from
  compilation (b001-verify.yml); depot_tools cached on its pinned rev.

## In progress

- Stage 2 — iNWEB patch series (B-001 gate CLEARED 2026-09-22). Registry
  structure ready (9 areas, lint, apply/verify/hash tooling, PEEL-idempotent
  convergence). Patch 0001 AUTHORED+REGISTERED+LOCALLY VALIDATED:
  ui/0001-app-identity.patch — rebrands the user-visible launcher/widget
  names Chromium -> iNWEB Browser via
  chrome/android/java/res_chromium_base/values/channel_constants.xml
  (authored against the pinned tag content; git round-trip + tool E2E +
  lint all PASS). CI validation: b001-verify (patches mode) on a real tree.
  Next: remaining 23 patches (ui strings/grd, icons, adblock, popup,
  extension, offline, security, settings), then ONE incremental build from
  state-13 -> iNWEB-branded APK.

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

Full record: `docs/phases/PHASE0-ENVIRONMENT-ASSESSMENT.md` §5.

## Known defects

- None open. (Step 4 test defect found and fixed via the Rule 64 loop: an
  incorrect newest-first expectation in a history range-deletion test.
  Step 3 defect found and fixed via the Rule 64 loop: the omnibox
  scheme detection initially treated `host:port` inputs as URLs with an unknown
  scheme; replaced with an explicit `scheme://` authority check plus a
  known-scheme whitelist, with regression tests.
  Step 33 defect found and fixed via the Rule 64 loop: two properties
  named `history` in the authored BrowserViewModel (state var +
  PrivacyFilterHistory source, present since Step 4) — a redeclaration
  that would have failed the first real compile; fixed by renaming the
  source property, and the whole defect class is now blocked by the
  authored-structure gate.)

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
| ADR-018 | 2026-09-12 | Onboarding completion is a persisted `AppSettings` flag; the first-run engine choice writes the same real setting the settings screen uses; skip completes with the privacy default | Runs exactly once, no separate preference source; single source of truth for the engine setting (§10/§35) |
| ADR-019 | 2026-09-12 | Ad blocking intercepts via `URLLoaderThrottle` with deferred background-thread `decide()` calls; allowlist/toggle logic lives only inside the engine (one decision path); main-frame navigations are not filtered in v1; engine snapshots swap atomically after refresh | Chosen integration point of shipped Chromium-derived browsers; no C++-side policy divergence; defers never block the UI thread; §57-honest deferred scopes (websocket, cosmetic, popup) |
| ADR-020 | 2026-09-12 | Popup policy: user-activation-based blocking at the window-creation consent point with `$popup` engine consultation; one shared per-site allowlist across all protections; quiet-only notification prompts; no remote reputation claims (no bundled service); user-driven same-tab redirects never blocked | One shields list per site (no settings sprawl); §12 "where supported" satisfied honestly; web compatibility preserved |
| ADR-021 | 2026-09-12 | Cosmetic filtering v1 = stylesheet hiding only (`##` with `#@#` exceptions), selectors passed through verbatim (browser CSS parser validates); procedural `#?#` and scriptlet rules counted, never matched; injection is per-frame at document commit; no new §24 counters | Honest, testable subset now (§11/§57); network blocking stays the primary defense — cosmetic never claims to block requests |
| ADR-022 | 2026-09-12 | Performance work is measure-first: the benchmark is a tracked artifact; budgets are verification targets, never claims; the combined matcher (token-index candidates with a decision-equivalence contract vs v1) is mandatory before any enforcement ships, justified by the measured 16–18 ms/request baseline | §9 discipline ("measure, don't claim"); correctness guaranteed by equivalence, performance by re-measurement |
| ADR-023 | 2026-09-12 | Extension strategy: enable and complete the upstream WebExtensions subsystem on Android (Kiwi-proven), not a custom format or WebView shim; sideload-first installation with full permission-warning review; manual re-sideload updates (no CWS auto-update claims); MV3 primary with a stated MV2 grace policy; API support tables regenerated by a build-time audit from the real APK | §16 forbids extension-website pretense and demands documented real compatibility; security posture (no silent installs, no fake API success) |
| ADR-024 | 2026-09-12 | Offline strategy: reuse upstream DOM distiller (reader mode), MHTML offline pages (snapshots), and the browsing-data remover (cache); offline snapshots are USER DATA distinct from HTTP cache (clear-cache never deletes them); data saver = rule-class mode over the iNWEB filter engine counting REAL avoided requests; proxy/compression is a documented non-goal (§19/§20 truthfulness) | No client-only HTTPS compression claims ever; separation enforced in the data model, not just UI; one decision path reused for data saving |
| ADR-025 | 2026-09-12 | VPN = real VpnService + WireGuard client, bring-your-own-server (iNWEB operates no servers; hosted service would need its own §15/§20 design first); biometrics = BiometricPrompt + Keystore envelope so data is ACTUALLY encrypted; 2FA absent until an auth backend exists; encryption = platform primitives only, no hand-rolled crypto, no hard-coded secrets | §15/§25/§26/§27 honesty rules made architectural; no cosmetic toggles, no claims without infrastructure |
| ADR-026 | 2026-09-12 | Profiles isolate at the storage-namespace level (per-profile store dirs + Chromium user-data dirs; cookies never shared); cloud sync is a documented absence (§29) — the §30 queue/conflict model is transport-agnostic future infrastructure, not a feature; backups are versioned, checksummed, preview-gated, forward-migrating bundles encrypted at the Android layer; app data in SQLite, Chromium storage never duplicated | §28 isolation made structural; §29/§30/§31/§32 honesty rules enforced in the data model |
| ADR-027 | 2026-09-12 | Localization is enforced by CI gates, not convention: bidirectional key/placeholder parity (existing) + source-level externalization of the authored UI (new) with documented exemptions and a reviewed escape hatch; bn-BD is authored, never machine-translated; §49 accessibility is a binding authoring contract + mandatory patch-review gate, device-verified at B-001 | §36/§49 rules become merge-blocking checks; structure proven by CI, prose quality and on-device accessibility honestly labeled as human/device deliverables |
| ADR-028 | 2026-09-12 | Notifications report real user-relevant events only — minimal by default, lazily-requested permission, every channel toggleable, zero promotional content; absent channels (sync, self-update) are stated, never stubbed; the entertainment module is a non-goal for v1 (base APK stays lean); every §23 customization lever must map to a real mechanism | §33/§34/§41/§7 honesty rules; no engagement bait, no claims without infrastructure |
| ADR-029 | 2026-09-12 | Toolbar configuration core: the item universe mirrors the authored bar exactly (no invented items, stable wire ids); validation is strict (duplicate/unknown/missing/unknown-hidden/mandatory-hidden are hard errors, all offenders reported); a corrupt stored configuration falls back to the authored default, persisted and surfaced — never a crash, never a silent ignore; mandatory items (back/tabs/menu) can never be hidden; a hidden item keeps its slot | §23 lever backed by a real tested mechanism; user customization can never produce a broken or empty-control bar |
| ADR-030 | 2026-09-12 | Notification policy core: the channel/event registry is the design §1 table exactly and the unit test is its audit; absent events (sync, self-update, promotional, filter-list failure) are structurally absent — no enum value, no code path — never merely undocumented; the only notify entry is a typed real event (no generic notify API); channel availability is an explicit build fact (no default) so unavailable channels are never registered (no stubs); permission is asked lazily at the first show-worthy event, never re-asked after denial; per-channel toggles + corrupt-preference recovery follow the ADR-029 pattern | §33/§41 rules become structural: real events only, user-respecting, zero promotional paths |
| ADR-031 | 2026-09-12 | The authored app binds the §23/§33 cores directly: the bar renders from the toolbar configuration and settings toggles write through the cores; channel availability in the authored build is downloads-only and grows only as event-source patches land; preference adapters are thin wire-form round-trippers (validation + recovery stay in the cores); authored sources get a permanent structural gate (kotlinc parse/declaration check, dependency resolution excluded by design) because they compile only at B-001 | §23/§33 become reachable in authored source honestly; the B-001 compile cannot again be broken by latent redeclaration/parse defects |
| ADR-032 | 2026-09-12 | Page-zoom and download preferences are validated models in the browser-shell settings core with their own store seams (AppSettings itself unchanged until its binding step): zoom bounds mirror upstream Chromium's 25%–500% supported range and bind to Chromium's own zoom mechanism via a settings/ patch (no custom renderer scaling); per-site zoom keys are normalized hosts with collision detection (ambiguous = corrupt); downloads ask before starting by default and use the platform's public Downloads directory unless the user picks a folder; both follow the ADR-029/030 corrupt-recovery contract | the remaining §23 levers get real, tested mechanisms; no setting without behavior, no silent fallback |
| ADR-033 | 2026-09-12 | Every persisted-data surface is inventoried in docs/STORAGE-INVENTORY.yaml and the inventory is CI-enforced bidirectionally (discovered surfaces must have entries; entries must exist in code); each entry's corruption column is that store's §50/§51 recovery contract and its clear column is the clear-data contract; in-memory-only seams must name the patch that will persist them; `discoverable: false` is the reviewed manual-extra hatch, surfaced in gate output | "never silently lose user data" becomes an auditable, merge-blocking property of the codebase; the data-safety story for store readiness is generated from the same inventory |
| ADR-034 | 2026-09-12 | The clear-browsing-data item universe is the storage inventory's clear column and NOTHING else (bookmarks/preference stores/downloads catalog are deliberately excluded with reasons); every binding delegates to a real store API — no invented clearing paths; a selection naming an unbound item is rejected, never silently skipped; previews are real counts from the stores (null = not countable) and never mutate; selection is transient dialog state; cross-module cores are validated via the script's --deps classpath with build-order enforcement | clearing user data is orchestrated, auditable, and honest; the dialog can never pretend to clear something it cannot |
| ADR-035 | 2026-09-12 | Versioning policy (docs/VERSIONING.md): SemVer with the alpha→beta→rc→stable ladder from 1.0.0-alpha.1; versionName = exact semver; versionCode strictly monotonic via a documented derivation + overflow rule, recorded per-tag in an authoritative release registry; annotated v<semver> tags only, channel identified by the pre-release identifier, each annotation carrying the full §45 release record; development channel = untagged main builds, no canary line in v1 (a later canary needs a new ADR); the iNWEB version is independent of the pinned Chromium baseline (baseline = build metadata; a refresh is at minimum a PATCH bump); the policy was written BEFORE the first build tag exists, registry empty | §47/§45 rules become a written, auditable contract before any release artifact exists; version assignments can never drift because every code is recorded at tag time and nothing predates the policy |
| ADR-036 | 2026-09-12 | Filter-list content integrity (G-07): every downloaded body is pinned with its SHA-256 (platform MessageDigest, ADR-025) in the cache metadata and re-verified on every cache load; a mismatch is corruption/tampering and the copy is NEVER served — re-download when allowed, otherwise an honest Failed(servedFromCache=false); metadata wire format gains an optional contentSha256 line (safe: no v=1 file exists in the wild, B-001); legacy checksum-less copies are served as-is (documented tolerance, not silent fixing) | closes the threat-register supply-chain gap: cached list content can no longer be silently corrupted or tampered with; integrity pinning is claimed, a publisher signature is NOT (no upstream source signs, §57) |
| ADR-037 | 2026-09-22 | Privacy-preserving usage statistics pledge (author-directed): any future usage/install metrics are strictly OPT-IN (default off), anonymous, aggregated-only, and disclosed in settings; no personal data, browsing history, or per-user tracking is ever collected by iNWEB — the browser that blocks trackers must never secretly track its own users; distribution signals (GitHub download counter, future store consoles) remain the only passive sources | preserves the project's core privacy promise while enabling legitimate adoption measurement for the §48 device matrix |
| ADR-038 | 2026-09-22 | Product naming (author-directed): launcher/app label = "iNWEB" (one-word, Chrome/Opera-style; avoids launcher truncation); full product name in store listings/legal = "iNWEB Browser" (master prompt #1); Android package id = com.inweb.android set via Chromium's supported rebranding GN argument chrome_public_manifest_package (declare_args in chrome/android/chrome_public_apk_tmpl.gni) — no upstream source patch, rebase-proof; permanent once released | professional brand identity: short iconic launcher name, product-domain package (DuckDuckGo-style .android suffix), traceable and reproducible |
| ADR-039 | 2026-09-22 | Repository structure professionalization (author-directed): docs/ organized into phases/ (14 phase designs), design/ (BRAND.md + logo study assets), releases/ (per-version notes + template), verification/ (existing) with docs/README.md as the full index; brand masters in a dedicated assets/logo/ folder; stale README/PROJECT_STATE build & test status reconciled with the recorded v1.0.0-alpha.1 release; src/, scripts/, tests/, config/, .github/, ci/ and all script-read data paths (docs/STORAGE-INVENTORY.yaml) unchanged — CI path triggers preserved and the local Python suite re-run green | navigability and a single truthful status picture without breaking any CI path contract; phase/design/release documents discoverable by group |
| ADR-040 | 2026-09-22 | Ad-block engine implementation language (Stage 2): native C++ port of the tested Kotlin tracking-protection engine instead of the originally designed JNI-into-Kotlin bridge — pinned-tree verification (154.0.8037.21) shows no first-party Kotlin compilation exists (no Kotlin step in build/android/gyp/, kotlin_stdlib is a prebuilt AAR-runtime jar only, zero .kt in chrome/android/BUILD.gn); the Kotlin implementation remains the behavioral specification with its tests mirrored at build time; the URLLoaderThrottle interception point is confirmed on the pinned tree as ContentBrowserClient::CreateURLLoaderThrottles | the only buildable path on this baseline; reuses 116-test-verified behavior; avoids vendoring a toolchain or a parallel build system; every Phase-4 design decision except the language/bridge is preserved |
| ADR-041 | 2026-09-22 | Ad-block provisioning scope (patch 0007): ship the embedded conservative starter list loaded at engine-holder construction (real blocking from the first request, no network dependency) with the full lists/ stack ported (source model, header versioning, update-policy decisions, G-07 SHA-256 integrity pinning via crypto::SHA256HashString, wire-compatible cache, fetcher interface + decision logic, manager orchestration); DEFER the SimpleURLLoader subscription-download transport and its profile/scheduling wiring to a follow-up; FilterListSource.download_url becomes optional for embedded lists; ParsedFilterList is move-only so the manager hands rules to the engine via TakeParsedLists() | v1 keeps zero new upstream hunks (no profile-dependent startup hook), keeps blocking deterministic on-device, and lands the full decision logic now; the Kotlin reference remains the behavioral spec; deviations documented in-code and in the patch manifest |

## Build status

- **v1.0.0-alpha.1 RELEASED (2026-09-22)** — the pristine engine-baseline
  APK (689 MB, sha256 098fff9c…c9799b, bit-for-bit verified
  runner → artifact → release asset). Built on GitHub-hosted runners by
  the 14-run resumable B-001 chain (hop 14 = run 35715711786; full ledger:
  `docs/verification/B001-BUILD-CHAIN.md`). B-3 (real build completes) and
  B-4 (real verified artifact) evidence recorded.
- **Stage 2 in progress** — iNWEB patch series: **0001–0008 authored**
  (8/24). CI-verified on the real pinned tree: 0001–0004 (b001-verify
  35749687902 @75c71fa) and 0005–0006 — the native ad-block engine +
  URL-loader throttle wiring (b001-verify **35756172332 @4bb5024:
  `TAG: PASS`, `PATCHES: PASS`**; ci-authoring 35756172362 green).
  0007 (provisioning: lists/ stack port, embedded starter list with the
  RFC-2606 device-test fixture, Security Center read path — ADR-041)
  CI-verified too: b001-verify **35758519450 @9b2b7ab: `TAG: PASS`,
  `PATCHES: PASS` (7-patch series)**; ci-authoring 35758519623 green.
  0008 (popup window guard at the WebContentsDelegate consent point —
  first real call site for $popup rules, §24 counters) CI-verified:
  b001-verify **35763178532 @d585887: `PATCHES: PASS` (8-patch series)**;
  ci-authoring 35763178530 green. The ad-block core (0005–0007) and the
  popup guard (0008) are complete and verified; the popup series
  continues with the redirect throttle (0009), notification policy
  (0010), and download guard (0011). The Android UI sources compile inside
  the Chromium build once the integration patches land (see
  `docs/phases/PHASE2-INTEGRATION-PLAN.md`).
- **Next build:** incremental resume from cached state-13 → first
  iNWEB-branded APK → v1.0.0-alpha.2.

## Test status

- **Python: 80/80 passing** — patch-series tooling, registry
  validation, baseline parsing, string-resource validation
  (`python3 -m unittest discover -s tests -t .`); green in ci-authoring run
  35739593180 (commit af5bc39) and re-run locally green after the ADR-039
  restructure (tests/ untouched by it).
- **Kotlin: 414/414 passing** (`bash scripts/validate_kotlin_core.sh`,
  pinned kotlinc 2.4.20 + JUnit 4.13.2, 11 modules incl. cross-module
  `--deps`); green in ci-authoring run 35739593180 (commit af5bc39):
  browser-shell 150, tracking-protection 116, customization 25,
  notifications 23, extensions 17, vpn 17, offline 15, clear-data 14,
  backup 13, sync 12, profiles 12.

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

**Stage 2 — continue the series (0008+):** the adblock core (0005–0007)
is authored — 0005–0006 CI-verified (35756172332), 0007 pushed with its
verify pending (record the 7-patch verdict lines here when green). Next:
the popup-protection series (0008–0011) per the design docs, then
extensions, offline, security, settings; after the series verifies,
dispatch the incremental build hop (resume from state-13) for the first
iNWEB-branded APK **with working ad-block** → v1.0.0-alpha.2, then the
B-5…B-8 device matrix (the 0007 `.invalid` fixture exercises
block/type-constraint/exception on-device) and G-closure per
`docs/DEVICE-VERIFICATION-MATRIX.md`.
