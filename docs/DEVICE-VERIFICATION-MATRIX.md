# iNWEB Browser — B-001 Device-Verification Matrix

**Status:** Phase 12 design-order item 4 (Step 41). This document
consolidates the B-001 checklists already written across the phase
designs into **one runnable checklist** — the acceptance document the
first real Chromium build is walked through.

**Everything here is PENDING, by definition.** No Chromium build
exists (blocker B-001 — the build host is not provisioned), and the
patch registry is intentionally empty until patches are generated
against the real pinned source (`patches: []`, MASTER-SPEC §57). What
exists today is authored, CI-gated source: the pure-JVM cores, the
authored Android shell, the patch-plan designs, and this matrix.

**Source authority:** each row cites the phase design it consolidates.
Where this matrix and a design doc disagree, the design doc wins and
this matrix is corrected (§59 documentation discipline).

---

## 1. Protocol — how this matrix is run

1. **Prerequisites:** a provisioned build host
   (BUILD-INFRASTRUCTURE.md), the pinned baseline from
   `PROJECT_STATE.md`, the patch registry state, and the build
   container digest. These become the §45 release-record fields.
2. **Stage 0 gates everything.** No device row runs before the
   build-level rows (Stage 0) pass on the exact same artifact.
3. **Order:** Stage 0 → per-phase rows in phase order. Within a phase,
   the phase's own design order applies (its C++ unit tests before its
   on-device rows).
4. **Recording:** every row run appends to the verification log
   (Appendix B) with date, build id (tag + `versionCode` per
   VERSIONING.md), and hardware. Result vocabulary in Appendix A.
5. **A failed row is a filed defect — never a silent skip. A skipped
   row means the feature is NOT claimed in that release** (§57).
6. **Re-runs:** the full matrix is the §46 release-validation gate —
   stable promotion requires a full pass on the exact artifact being
   promoted (VERSIONING.md §2).

## 2. Stage 0 — build & patch level (gate for every device row)

| ID | Verify | Gate | Closes / source |
|---|---|---|---|
| B-1 | `apply_patches.py apply && verify` succeeds on the pristine pinned tag | scripted | PHASE2 §4; registry tooling (ADR-004) |
| B-2 | Working tree ≡ `pristine@tag + ordered patch series` (hash invariant) | scripted | BUILD-INFRASTRUCTURE §4 |
| B-3 | GN + ninja builds `inweb_public_apk` for every channel's committed GN args | build | BUILD-INFRASTRUCTURE §3/§4 |
| B-4 | APK/AAB installs and launches on an arm64 device/emulator | manual | PHASE2 §4 |
| B-5 | C++/Android unit suites pass (per-phase rows below name theirs) | build-time tests | each phase design §"Verification" |
| B-6 | Build-time audits run: extension-API audit regenerates the real §3 table; notification-channel registration matches the §1 table exactly | scripted | PHASE6 §6.2; PHASE11 §5.2 |
| B-7 | Signing per policy: debug = ephemeral keys on the host; release = secrets/Play App Signing; **no signing material in the repo** | procedure | BUILD-INFRASTRUCTURE §6 |
| B-8 | SBOM generated for the artifact | scripted | threat review G-08 |

## 3. Per-phase device matrix

State legend: **authored** = source + CI gates exist today, device run
pending. Every row below is in that state; the column carries the
phase's planned patch ids (registered plan, registry empty).

### Phase 2 — Browser shell (ui/0001–0004)

| ID | Verify | Gate | Source |
|---|---|---|---|
| D2-1 | Omnibox: URL navigation, search via chosen engine, back/forward | manual | PHASE2 §4 |
| D2-2 | Tabs: open, close, select, private tabs, switcher; session restores after force-stop | manual | PHASE2 §4 (§51) |
| D2-3 | History: real titles recorded on page loads; private tabs record nothing | manual | PHASE2 §4 (§13/§14) |
| D2-4 | Bookmarks: add current page with real title; persists across restart | manual | PHASE2 §4 |
| D2-5 | Home: shortcuts/recent/bookmarks show real data; empty sections hidden | manual | PHASE2 §4 (ADR-017) |
| D2-6 | Onboarding runs once; engine choice persists; skip keeps the privacy default | manual | PHASE2 §4 (ADR-018) |
| D2-7 | Tracking protection: `decide()` consulted per request; blocked/allowed counters real | manual + debug surface | PHASE2 §4 (§57) |
| D2-8 | bn-BD and en render on every screen; no missing-translation crashes | manual | PHASE2 §4 (§36) |
| D2-9 | TalkBack labels present on all shell controls (full accessibility pass = Phase 10) | TalkBack spot pass | PHASE2 §4 (§49) |

### Phase 3/4 — Ad blocking (adblock/0005–0007)

| ID | Verify | Gate | Source |
|---|---|---|---|
| D4-1 | A local verification filter list blocks a known test host | manual + test list | PHASE4-ADBLOCK §8.4 |
| D4-2 | The blocked counter in Security Center increments on real decisions only | manual (§24) | PHASE4-ADBLOCK §8.4 |
| D4-3 | Per-site allowlist bypasses all filtering; the global toggle disables filtering | manual | PHASE4-ADBLOCK §8.4 |
| D4-4 | Redirects to blocked hosts stay blocked | manual | PHASE4-ADBLOCK §8.4 |
| D4-5 | Cosmetic (`##`/`#@#`): element hidden on matching host, subdomain matching, exception honored, allowlisted site unfiltered, toggle disables (patch 0012) | manual + test list | PHASE4-COSMETIC §5.4 |

### Phase 4 — Popup & abuse protection (popup_protection/0008–0011)

| ID | Verify | Gate | Source |
|---|---|---|---|
| D4-6 | `window.open` without user gesture → blocked + chip visible + "open once" works | scripted test page | PHASE4-POPUP §5.4 |
| D4-7 | Subframe top-level navigation → blocked, page intact | scripted test page | PHASE4-POPUP §5.4 |
| D4-8 | Notification prompt on a fresh site stays quiet; a second automatic download requires confirmation | scripted | PHASE4-POPUP §5.4 |
| D4-9 | ALL Security Center counters are zero before any real event | manual (§24) | PHASE4-POPUP §5.4 |

### Phase 5 — Performance (§9/§21/§52; no patch series beyond the above)

| ID | Verify | Gate | Source |
|---|---|---|---|
| D5-1 | Startup: `am start -W` cold/warm, 10-run medians, vs a vanilla build of the SAME pinned tag | measurement | PHASE5 §5.1 |
| D5-2 | Navigation: perfetto traces — `decide()` and injection spans on worker threads; scrolling jank (frame timing) on a heavy page | measurement | PHASE5 §5.2 |
| D5-3 | Memory: `dumpsys meminfo` PSS deltas (engine, lists, caches) | measurement | PHASE5 §5.3 |
| D5-4 | Battery: `batterystats`/historian A/B over a scripted session; patches do not defeat tab-freezing / timer-throttling | measurement + patch review | PHASE5 §5.4 |

### Phase 6 — Extensions (extension/0013–0016)

| ID | Verify | Gate | Source |
|---|---|---|---|
| D6-1 | A known test extension (uBlock-class) installs: DNR rules fire, its popup opens and interacts, storage persists across restart | manual | PHASE6 §6.4 |
| D6-2 | Disable / enable / remove work; permission warnings shown pre-install | manual | PHASE6 §6.4 |
| D6-3 | Built-in protections remain active alongside the extension | manual | PHASE6 §6.4 |
| D6-4 | Enforcement surface: scoped API surface + kill-switch active; unsupported API returns standard `lastError`, never fake success (closes G-10) | manual + audit | PHASE6 §6.4/§7 |

### Phase 7 — Offline & data saver (offline/0017–0019)

| ID | Verify | Gate | Source |
|---|---|---|---|
| D7-1 | Save a page → airplane mode → it reopens from the library | manual | PHASE7 §7.4 |
| D7-2 | Reader mode offered on an article page, not on a web app | manual | PHASE7 §7.4 |
| D7-3 | Deleting an entry frees the shown bytes | manual | PHASE7 §7.4 |
| D7-4 | **Clear cache does NOT remove saved pages** (user data ≠ HTTP cache) | manual (ADR-024) | PHASE7 §7.4 |
| D7-5 | Data-saver mode blocks an image request; the counter increments by exactly one real request | scripted | PHASE7 §7.4 |

### Phase 8 — VPN & security (security/0020–0021)

| ID | Verify | Gate | Source |
|---|---|---|---|
| D8-1 | App lock actually blocks launch and re-entry; a wrong biometric does not decrypt (closes G-03) | manual | PHASE8 §7.4 |
| D8-2 | VPN with a real test endpoint: traffic passes AND leaks nothing — DNS + kill-switch tested with always-on and block-without-VPN enabled | measurement | PHASE8 §7.4 |
| D8-3 | Disconnect state truthfully shown; invalid config files rejected at import | manual | PHASE8 §7.4 |

### Phase 9 — Profiles & backup (ui/0022, 0024)

| ID | Verify | Gate | Source |
|---|---|---|---|
| D9-1 | Two profiles keep separate bookmarks/history/cookies; deleting a profile removes its namespace (closes G-05) | manual | PHASE9 §7.4 |
| D9-2 | An encrypted backup restores on a clean install with preview; a tampered bundle is refused; an old-format bundle migrates forward; a newer-format bundle is refused with a clear message (closes G-04) | manual | PHASE9 §7.4 |

### Phase 10 — Localization & accessibility (§36/§49)

| ID | Verify | Gate | Source |
|---|---|---|---|
| D10-1 | TalkBack pass over EVERY screen | scripted device test | PHASE10 §4 |
| D10-2 | 200% font scale renders on key screens | screenshot diff | PHASE10 §4 |
| D10-3 | Contrast audit of the theme tokens | automated audit | PHASE10 §4 |
| D10-4 | Touch-target audit | layout tooling | PHASE10 §4 |
| D10-5 | bn-BD prose human review | per release, documented responsibility | PHASE10 §4 |

### Phase 11 — Notifications & settings bindings (settings/0023 + preference bindings)

| ID | Verify | Gate | Source |
|---|---|---|---|
| D11-1 | No notification without a real event; every channel toggle works; permission requested lazily | manual (§33) | PHASE11 §5.3 |
| D11-2 | Airplane-mode download failure notifies honestly; zero promotional notifications ever | manual (§33/§41) | PHASE11 §5.3 |
| D11-3 | Page zoom binds to Chromium's OWN zoom mechanism (no custom scaling, ADR-032): default factor + per-site overrides take effect; clear-data clears site zooms | manual | PHASE11 §6; ADR-032 |
| D11-4 | Download preferences behave: ask-before-download gates the download prompt; a picked SAF folder actually receives downloads; releasing back to the system folder works | manual (ADR-032) | PHASE11 §6; ADR-032 |
| D11-5 | Toolbar customization renders from the real persisted config; mandatory items cannot be lost (ADR-029) | manual (§23) | PHASE11 §5 |

### Phase 12 — Hardening & data integrity (cross-cutting; closes G-09)

| ID | Verify | Gate | Source |
|---|---|---|---|
| D12-1 | On-device verification of EVERY STORAGE-INVENTORY corruption contract: corrupted `history.tsv`/`bookmarks.tsv` → fresh start + skipped-line counts; corrupt preferences (toolbar/notifications/zoom/downloads) → defaults restored, persisted, reported; corrupted session snapshot → fresh session; corrupt filter-list metadata → body usable, next refresh re-downloads; a filter-list body whose pinned SHA-256 mismatches → copy never served, re-download attempted (G-07, ADR-036) | fault injection on device | PHASE12 §4; ADR-033 |
| D12-2 | Kill/fuzz: force-kill during writes never corrupts (atomic temp+rename); no ANR/crash loops | kill testing | PHASE12 §1 (§51) |
| D12-3 | Full security audit of the built APK: declared permissions, network egress, storage surfaces | audit vs THREAT-MODEL | PHASE12 §1 |
| D12-4 | Clear-browsing-data on device: preview counts match the real stores; each item clears its store; the filter-list cache re-downloads | manual (ADR-034) | PHASE12 §3.2 |
| D12-5 | Store data-safety form drafted from the storage inventory (what is stored, where, what clears it) | form draft | PHASE12 §1 |

## 4. Release gates (VERSIONING.md)

| ID | Verify | Gate | Source |
|---|---|---|---|
| R-1 | First tag: a release-registry row is assigned (derivation or overflow rule) and the tag annotation carries the full §45 record | registry + annotation check | VERSIONING §3/§4 |
| R-2 | Tag ↔ `versionName` ↔ `versionCode` consistency on every artifact | scripted when release tooling exists | VERSIONING §3 |
| R-3 | Stable promotion only after a FULL matrix pass on the exact artifact (§46) | this document | VERSIONING §2 |

## 5. Threat-review gap-register closure map

| Gap | Closed by | Notes |
|---|---|---|
| G-01 download safety logic | — (documented absence) | upstream mechanisms only; no iNWEB scan/verification claim (THREAT-MODEL-REVIEW row 3) |
| G-02 fingerprinting resistance | — (upstream-preserved scope) | no custom implementation, no claims |
| G-03 Keystore/BiometricPrompt binding | D8-1 | ADR-025 envelope design |
| G-04 backup encryption | D9-2 | Android-layer encryption |
| G-05 per-profile directories | D9-1 | patch 0022 |
| G-06 sync E2E encryption | — (future infrastructure) | §29 documented absence; no backend exists |
| G-07 filter-list content pinning | D12-1 (checksum-mismatch path) | closed in the core at Step 44 (ADR-036): pin on download, verify on cache load, never serve a mismatch |
| G-08 SBOM | B-8 | release engineering |
| G-09 device verification of corruption contracts | D12-1 + D12-2 | ADR-033 contracts |
| G-10 extension enforcement surface | D6-4 | patches 0013–0016 |

## 6. Honest boundaries

- **No row has ever run.** No build exists (B-001); the patch registry
  is empty by design; every "State" is authored source + CI gates only.
- A row whose feature's patches are not yet authored simply stays
  pending — there is no partial credit and nothing is marked "should
  work".
- This matrix is authored documentation: nothing can mechanically
  enforce it until build infrastructure exists. Its discipline is the
  verification log — an empty log means "nothing verified", and that
  is the truth today (§57).

---

## Appendix A — Result vocabulary

| Result | Meaning |
|---|---|
| PASS | verified on the recorded build/hardware |
| FAIL | defect filed against the row; blocks the dependent release gate |
| BLOCKED | prerequisite row failed; re-run after the fix |
| SKIPPED | feature deliberately not claimed in this release — stated in release notes, never silent |

## Appendix B — Verification log

One row per matrix run (not per item — item-level detail goes in the
filed results/defects). Empty today.

| Date | Build (tag + versionCode) | Hardware | Rows run | Summary | Defects filed |
|---|---|---|---|---|---|
| *(none — no build exists; B-001)* | | | | | |
