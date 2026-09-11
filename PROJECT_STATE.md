# iNWEB Browser — Project State

> Single source of truth for project progress (Master Specification §60).
> Updated at the end of every development step. Must always reflect the real repository
> state — never a desired or simulated state (§57, §65).
>
> Last updated: **2026-09-12** — Step 25 (Phase 9 in progress; Phases 0–8 core done)

```yaml
project: iNWEB Browser
repository: iNAYATechLab/iNWEB-Browser
phase: 9
phase_title: Profiles / Sync / Backup
phase_status: in_progress   # decision engine implemented & tested; enforcement wiring awaits B-001
step: 25
chromium_baseline: 154.0.8037.21   # upstream Android stable, pinned 2026-09-12
fork_strategy: tracked-patch-overlay-on-pinned-tags  # ADR-001
build_status: not-built            # no Chromium artifact exists yet (B-001)
test_status: unit-tests-passing    # 30 Python + 265 Kotlin tests (local + CI)
ci_status: authoring-pipeline-live # Python + Kotlin core jobs (current action majors); upstream watch live
known_blockers: [B-001]
open_defects: 0
next_action: >-
  Phase 9 / Step 26 — profiles core in Kotlin (pure JVM, CI-tested):
  ProfileRecord, active-profile selection, per-profile store routing
  (namespace isolation enforced at construction), rename/delete with
  namespace retirement, persistence seam (alternative if directed:
  downloads/history surface polish).
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
  - `docs/PHASE2-INTEGRATION-PLAN.md`: new "Surface-by-surface
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
  - New `docs/PHASE4-ADBLOCK-DESIGN.md`: the `adblock/` patch-area
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
  - New `docs/PHASE4-POPUP-PROTECTION-DESIGN.md`: five protection
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
  - New `docs/PHASE4-COSMETIC-DESIGN.md`: per-frame stylesheet
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
  - New `docs/PHASE5-PERFORMANCE-DESIGN.md`: budgets as verification
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
  - New `docs/PHASE6-EXTENSION-DESIGN.md`: all eight §16 documentation
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
  - New `docs/PHASE7-OFFLINE-DESIGN.md`: reuse-not-rebuild strategy
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
  - New `docs/PHASE8-VPN-SECURITY-DESIGN.md`: VPN = REAL Android
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
  - New `docs/PHASE9-PROFILES-SYNC-DESIGN.md`: profiles with REAL
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

## In progress

- (none — awaiting continuation command for Step 26)

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
| ADR-018 | 2026-09-12 | Onboarding completion is a persisted `AppSettings` flag; the first-run engine choice writes the same real setting the settings screen uses; skip completes with the privacy default | Runs exactly once, no separate preference source; single source of truth for the engine setting (§10/§35) |
| ADR-019 | 2026-09-12 | Ad blocking intercepts via `URLLoaderThrottle` with deferred background-thread `decide()` calls; allowlist/toggle logic lives only inside the engine (one decision path); main-frame navigations are not filtered in v1; engine snapshots swap atomically after refresh | Chosen integration point of shipped Chromium-derived browsers; no C++-side policy divergence; defers never block the UI thread; §57-honest deferred scopes (websocket, cosmetic, popup) |
| ADR-020 | 2026-09-12 | Popup policy: user-activation-based blocking at the window-creation consent point with `$popup` engine consultation; one shared per-site allowlist across all protections; quiet-only notification prompts; no remote reputation claims (no bundled service); user-driven same-tab redirects never blocked | One shields list per site (no settings sprawl); §12 "where supported" satisfied honestly; web compatibility preserved |
| ADR-021 | 2026-09-12 | Cosmetic filtering v1 = stylesheet hiding only (`##` with `#@#` exceptions), selectors passed through verbatim (browser CSS parser validates); procedural `#?#` and scriptlet rules counted, never matched; injection is per-frame at document commit; no new §24 counters | Honest, testable subset now (§11/§57); network blocking stays the primary defense — cosmetic never claims to block requests |
| ADR-022 | 2026-09-12 | Performance work is measure-first: the benchmark is a tracked artifact; budgets are verification targets, never claims; the combined matcher (token-index candidates with a decision-equivalence contract vs v1) is mandatory before any enforcement ships, justified by the measured 16–18 ms/request baseline | §9 discipline ("measure, don't claim"); correctness guaranteed by equivalence, performance by re-measurement |
| ADR-023 | 2026-09-12 | Extension strategy: enable and complete the upstream WebExtensions subsystem on Android (Kiwi-proven), not a custom format or WebView shim; sideload-first installation with full permission-warning review; manual re-sideload updates (no CWS auto-update claims); MV3 primary with a stated MV2 grace policy; API support tables regenerated by a build-time audit from the real APK | §16 forbids extension-website pretense and demands documented real compatibility; security posture (no silent installs, no fake API success) |
| ADR-024 | 2026-09-12 | Offline strategy: reuse upstream DOM distiller (reader mode), MHTML offline pages (snapshots), and the browsing-data remover (cache); offline snapshots are USER DATA distinct from HTTP cache (clear-cache never deletes them); data saver = rule-class mode over the iNWEB filter engine counting REAL avoided requests; proxy/compression is a documented non-goal (§19/§20 truthfulness) | No client-only HTTPS compression claims ever; separation enforced in the data model, not just UI; one decision path reused for data saving |
| ADR-025 | 2026-09-12 | VPN = real VpnService + WireGuard client, bring-your-own-server (iNWEB operates no servers; hosted service would need its own §15/§20 design first); biometrics = BiometricPrompt + Keystore envelope so data is ACTUALLY encrypted; 2FA absent until an auth backend exists; encryption = platform primitives only, no hand-rolled crypto, no hard-coded secrets | §15/§25/§26/§27 honesty rules made architectural; no cosmetic toggles, no claims without infrastructure |
| ADR-026 | 2026-09-12 | Profiles isolate at the storage-namespace level (per-profile store dirs + Chromium user-data dirs; cookies never shared); cloud sync is a documented absence (§29) — the §30 queue/conflict model is transport-agnostic future infrastructure, not a feature; backups are versioned, checksummed, preview-gated, forward-migrating bundles encrypted at the Android layer; app data in SQLite, Chromium storage never duplicated | §28 isolation made structural; §29/§30/§31/§32 honesty rules enforced in the data model |

## Build status

- **Not built.** No APK/AAB has been produced. `fetch_chromium.sh`, `build_android.sh`,
  and the Docker container are authored but unexecuted (B-001). The Android UI sources
  compile inside the Chromium build once integration patch 0001 lands (see
  `docs/PHASE2-INTEGRATION-PLAN.md`).

## Test status

- **Python: 30/30 passing** — patch-series tooling, registry validation, baseline
  parsing, string-resource validation (`python3 -m unittest discover -s tests -t .`).
- **Kotlin: 265/265 passing** (`bash scripts/validate_kotlin_core.sh`, pinned
  kotlinc 2.4.20 + JUnit 4.13.2, 5 modules):
  - `src/core/browser-shell` — 115 tests: tab navigation stack, controller
    (incl. `allTabs` switcher view), top-sites computation,
    session round-trip/corruption + manager, omnibox parsing (incl. Bengali
    queries and scheme edge cases), search engines, download state machine +
    catalog, history store with private exclusion, file-backed persistent
    history (round-trips, corruption fallback, sanitization, unique ids),
    bookmark store semantics (dedupe, folders, rename/move) + file-backed
    persistent bookmarks, settings.
  - `src/core/vpn` — 17 tests: config parsing (valid client configs,
    multi-line list fields, optional-but-validated preshared key),
    structural errors (missing/duplicate sections, unknown fields,
    duplicate scalars, fail-fast-free issue collection), value
    validation (key length, CIDR/IPv6 forms, endpoints, ports, DNS),
    secret opacity (toString redaction, public-vs-private key).
  - `src/core/offline` — 14 tests: library registry (insertion order,
    same-URL replace, validation), access monotonicity, delete, quota
    eviction order (LRU first, tie by creation, pinned skipped, atomic
    failure), real byte accounting, store round-trip.
  - `src/core/extensions` — 17 tests: registry state machine
    (install/review/enable/disable/update/remove, upgrade consent,
    never-reviewed stays pending), version model (numeric compare,
    zero-padding, normalization, invalid rejection), insertion order,
    real counts, store round-trip.
  - `src/core/tracking-protection` — 102 tests: filter parsing (anchors,
    options, exceptions, cosmetic/unsupported/invalid counting), pattern
    matching (domain anchor, separators, wildcards, left/right anchors,
    type/party/domain constraints), engine decisions (block/allow/pass,
    per-site allowlist, statistics), domain classification (multi-part
    suffixes, third-party); list management (HTTP fetch via real JDK
    HttpServer, conditional revalidation + 304, size cap, atomic file
    cache, version parsing, update policy, manager lifecycle with cached
    fallback); Security Center model (§24 — real-state aggregation,
    honest empty/bypass semantics, top-domain ranking); cosmetic
    filtering (selector parsing, domain semantics, exception
    cancellation, grouped CSS, manager integration); combined matcher
    (token index, always-check bucket, substring lookup, order
    preservation, decision-equivalence vs the v1 full scan).
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

**Phase 5 / Step 18 — combined matcher:** implement the token-index
candidate selection designed in `docs/PHASE5-PERFORMANCE-DESIGN.md`
§3 — required-token extraction at index time, candidate union per
request, always-check bucket — with a decision-EQUIVALENCE test suite
against the v1 matcher over the synthetic corpus and edge cases, then
re-run the benchmark and append the v2 row to the log. Alternative
next step if directed: downloads surface polish.
