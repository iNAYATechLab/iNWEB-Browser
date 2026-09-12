# iNWEB Browser

**A real Chromium-derived, privacy-focused Android browser.**

iNWEB Browser is developed from the upstream **Chromium source code** and follows the
architecture of genuine Chromium-derived browsers (the Brave / Vivaldi / Kiwi class). It
ships a custom iNWEB Android application layer, privacy / security / performance
modifications maintained as a **tracked patch series**, and a Material 3 user interface
written in Kotlin.

> **Architectural identity — non-negotiable**
>
> **iNWEB Browser = real Chromium-derived Android browser**
> **iNWEB Browser ≠ Android WebView wrapper**
>
> The primary engine is never Android WebView. No build limitation, convenience shortcut,
> or later instruction may silently convert this project into a WebView shell
> (Master Specification §2, §65, §71 — see `docs/MASTER-SPEC.md`).

## Current status

| Item | State |
|---|---|
| Development phase | **Phase 12 — Production Hardening: authoring complete** (all five design-order items: storage inventory gate, threat-model review, clear-data core + surface, §47 versioning policy, device-verification matrix, performance budgets, data-safety draft; plus §23 settings binding — every remaining Phase 12 deliverable is B-001-gated; Phases 0–11 core work complete) |
| Current step | 44 |
| Chromium baseline | `154.0.8037.21` (upstream Android **stable**, pinned 2026-09-12) |
| Fork strategy | Tracked iNWEB patch overlay on pinned upstream stable tags (ADR-001) |
| Core module (`src/core/browser-shell`) | **Implemented & unit-tested — 150 Kotlin tests** (tabs incl. switcher view, omnibox, session, history + bookmarks + top sites incl. persistent file stores, downloads, settings incl. onboarding flag, page-zoom §23 with per-site overrides + Chromium preset table, download preferences §23) |
| Privacy core (`src/core/tracking-protection`) | **Implemented & unit-tested — 116 Kotlin tests** (EasyList-family parser, URL matching, request decisions, per-site allowlist, statistics; filter-list download/cache/update management incl. in-memory cache + SHA-256 content pinning G-07; Security Center model §24; cosmetic filtering engine; combined matcher ~165–259× faster, equivalence-verified) |
| Extension core (`src/core/extensions`) | **Implemented & unit-tested — 17 Kotlin tests** (§16 management model: install/review/enable/disable/update/remove state machine with upgrade consent, version comparison, permission-review records) |
| Offline core (`src/core/offline`) | **Implemented & unit-tested — 15 Kotlin tests** (§17 library: real byte quota, LRU eviction with pinning, same-URL replace, atomic quota failure, persistence seam, clear-all) |
| VPN core (`src/core/vpn`) | **Implemented & unit-tested — 17 Kotlin tests** (§15 config validation: WireGuard-style parsing, strict fields/CIDR/endpoints/keys, secrets opaque by construction) |
| Profiles core (`src/core/profiles`) | **Implemented & unit-tested — 12 Kotlin tests** (§28: seeded default profile, monotonic never-reused ids, namespace routing contract, delete fallback + last-profile guard) |
| Backup core (`src/core/backup`) | **Implemented & unit-tested — 13 Kotlin tests** (§32: versioned bundle, per-entry SHA-256, corruption tolerance, restore preview, known-stores-only secret guard) |
| Sync core (`src/core/sync`) | **Implemented & unit-tested — 12 Kotlin tests** (§30 model: monotonic change log, backoff/retry with explicit failure reporting, LWW conflicts with tombstones — future infrastructure, §29) |
| Customization core (`src/core/customization`) | **Implemented & unit-tested — 25 Kotlin tests** (§23 toolbar configuration: item universe mirrors the authored bar, strict validation with all offenders reported, corrupt→default recovery, persistence seam) |
| Notification-policy core (`src/core/notifications`) | **Implemented & unit-tested — 23 Kotlin tests** (§33: registry = design §1 table exactly (audited), real-event-only decisions with no generic notify path, lazy POST_NOTIFICATIONS state machine, per-channel toggles, corrupt→default recovery) |
| Clear-data core (`src/core/clear-data`) | **Implemented & unit-tested — 14 Kotlin tests** (Phase 12: item universe = the storage inventory's clear column, dry-run previews with real counts, execution through real store APIs, unbound items never silently skipped; first cross-module core via the script's `--deps` mechanism) |
| Android shell UI (`src/android-app`) | Authored — Compose + Material 3 (browser with configuration-driven bottom bar §23, tab switcher, home page with real data, onboarding, settings incl. toolbar customization §23 + page-zoom surface with core-validated presets + download preferences §23 with system SAF folder picker + notification toggles §33 + clear-browsing-data §39 with real preview counts, downloads, history, bookmarks), bn/en strings; §49 accessibility audit passed (whole-row toggle semantics, heading semantics, 48dp touch targets, AA-contrast palette); compiles in the Chromium build (B-001); structural gate in CI |
| Patch framework | `iNWEB_PATCHES/` registry + apply/verify/hash tooling — tested |
| CI | **Live**: Python (50 tests) + registry + storage-inventory gate + string parity + UI externalization gate; Kotlin core (414 tests, 11 modules, cross-module `--deps` support) + authored-source structural gate; weekly upstream watch; benchmark on demand (not in CI) |
| Build | **Not yet executed** — requires external build infrastructure (blocker B-001) |
| Open defects | None recorded |

Machine-readable, always-current progress: [`PROJECT_STATE.md`](PROJECT_STATE.md)

## Repository layout

```text
inweb-browser/
├── README.md                        # This file
├── PROJECT_STATE.md                 # Machine-readable project state (§60)
├── .gitignore                       # Chromium tree / artifacts are never committed
├── docs/
│   ├── MASTER-SPEC.md               # Governing master specification (verbatim)
│   ├── COMMUNICATION-PROTOCOL.md    # Step-completion format (user-amended)
│   ├── PHASE0-ENVIRONMENT-ASSESSMENT.md
│   ├── PHASE0-CHROMIUM-BASELINE.md
│   ├── ARCHITECTURE.md
│   ├── LICENSING.md
│   ├── BUILD-INFRASTRUCTURE.md
│   └── THREAT-MODEL.md
├── iNWEB_PATCHES/                   # Tracked patch series (§5)
│   ├── MANIFEST.yaml                # Patch registry — the single patch authority
│   ├── README.md                    # Rules, lifecycle, tooling usage
│   └── privacy/ security/ adblock/ popup_protection/ extension/
│       performance/ ui/ offline/ settings/ tests/
├── scripts/
│   ├── apply_patches.py             # Series convergence: apply / verify / hash (tested)
│   ├── lint_manifest.py             # Registry validation (tested)
│   ├── check_baseline.py            # Upstream Android-stable drift check (live)
│   ├── validate_strings.py          # bn/en string parity + placeholder validation (tested)
│   ├── validate_kotlin_core.sh      # Compile + run core module tests, multi-module (kotlinc + JUnit)
│   ├── benchmark_filter_engine.sh   # Filter-engine micro-benchmark (measured baselines, §9)
│   ├── fetch_chromium.sh            # BUILD HOST: pinned tag checkout (authored)
│   ├── build_android.sh             # BUILD HOST: GN + ninja build (authored)
│   └── bootstrap_env.sh             # Authoring-sandbox session bootstrap
├── src/
│   ├── core/browser-shell/          # Pure-JVM core (Gradle project; validated by script + CI)
│   │   ├── build.gradle.kts
│   │   └── src/{main,test}/kotlin/com/inweb/browser/shell/
│   ├── core/tracking-protection/     # Pure-JVM privacy core (parser, matcher, decision engine)
│   ├── core/extensions/               # Pure-JVM extension management core (§16 state machine)
│   ├── core/offline/                   # Pure-JVM offline library core (§17 quota + LRU eviction)
│   ├── core/vpn/                        # Pure-JVM VPN config validation core (§15)
│   ├── core/profiles/                    # Pure-JVM profiles core (§28 isolation contract)
│   ├── core/backup/                      # Pure-JVM backup bundle core (§32 checksums + version gating)
│   ├── core/sync/                        # Pure-JVM sync queue core (§30 model — future infrastructure, no backend)
│   │   ├── build.gradle.kts
│   │   └── src/{main,test}/kotlin/com/inweb/browser/privacy/
│   └── android-app/src/main/        # Android shell UI (compiled by the Chromium build, ADR-009)
│       ├── AndroidManifest.xml
│       ├── kotlin/com/inweb/browser/   (shell, ui, session, settings)
│       └── res/{values,values-bn}/strings.xml
├── config/chromium/
│   ├── BASELINE                     # Pinned Chromium tag
│   ├── args-development.gn          # Draft GN args (validated at first build)
│   └── args-release.gn
├── ci/
│   ├── Dockerfile                   # Chromium build container (authored)
│   └── README.md
├── tests/                           # Unit tests (run locally and in CI)
└── .github/workflows/
    ├── ci-authoring.yml             # LIVE: unit tests + registry validation
    └── upstream-watch.yml           # LIVE: weekly baseline drift check
```

The Chromium checkout (`chromium/`, ~100 GB) exists **only on build infrastructure** and is
git-ignored here by design (ADR-004): the tree is always reproducible as
`pristine@tag + ordered patch series`.

## Documentation index

| Document | Purpose |
|---|---|
| [`docs/MASTER-SPEC.md`](docs/MASTER-SPEC.md) | Governing master specification (single source of truth) |
| [`docs/COMMUNICATION-PROTOCOL.md`](docs/COMMUNICATION-PROTOCOL.md) | Step-completion communication format (user-amended) |
| [`docs/PHASE0-ENVIRONMENT-ASSESSMENT.md`](docs/PHASE0-ENVIRONMENT-ASSESSMENT.md) | Measured environment, Chromium build feasibility, blocker B-001 |
| [`docs/PHASE0-CHROMIUM-BASELINE.md`](docs/PHASE0-CHROMIUM-BASELINE.md) | Baseline pin, fork strategy, rebase & patch policy |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | System architecture and module map |
| [`docs/LICENSING.md`](docs/LICENSING.md) | License landscape and obligations |
| [`docs/BUILD-INFRASTRUCTURE.md`](docs/BUILD-INFRASTRUCTURE.md) | CI/CD design, build host specification, reproducibility |
| [`docs/PHASE2-INTEGRATION-PLAN.md`](docs/PHASE2-INTEGRATION-PLAN.md) | Patch-by-patch plan binding `src/` into the Chromium Android build |
| [`docs/PHASE4-ADBLOCK-DESIGN.md`](docs/PHASE4-ADBLOCK-DESIGN.md) | `adblock/` patch-series design — request interception, type mapping, verification |
| [`docs/PHASE4-POPUP-PROTECTION-DESIGN.md`](docs/PHASE4-POPUP-PROTECTION-DESIGN.md) | `popup_protection/` patch-series design — popups, redirects, notifications, downloads |
| [`docs/PHASE4-COSMETIC-DESIGN.md`](docs/PHASE4-COSMETIC-DESIGN.md) | Cosmetic filtering — tested Kotlin engine + injection patch design |
| [`docs/PHASE5-PERFORMANCE-DESIGN.md`](docs/PHASE5-PERFORMANCE-DESIGN.md) | Performance budgets, measured v1 baseline, combined-matcher & cache design, device plan |
| [`docs/PHASE6-EXTENSION-DESIGN.md`](docs/PHASE6-EXTENSION-DESIGN.md) | Extension system design — API table, models, patch plan, build-time audit |
| [`docs/PHASE7-OFFLINE-DESIGN.md`](docs/PHASE7-OFFLINE-DESIGN.md) | Offline reading, snapshots, cache controls, honest data saver — patch plan |
| [`docs/PHASE8-VPN-SECURITY-DESIGN.md`](docs/PHASE8-VPN-SECURITY-DESIGN.md) | Bring-your-own VPN, biometric app lock, encryption model, documented 2FA absence |
| [`docs/PHASE9-PROFILES-SYNC-DESIGN.md`](docs/PHASE9-PROFILES-SYNC-DESIGN.md) | Isolated profiles, documented sync absence, queue model, encrypted backup format |
| [`docs/PHASE10-LOCALIZATION-ACCESSIBILITY-DESIGN.md`](docs/PHASE10-LOCALIZATION-ACCESSIBILITY-DESIGN.md) | bn-BD/en policy, CI externalization gate, §49 authoring contracts, verification matrix |
| [`docs/PHASE11-NOTIFICATIONS-FEATURES-DESIGN.md`](docs/PHASE11-NOTIFICATIONS-FEATURES-DESIGN.md) | Privacy-first notifications, entertainment non-goal, §23 customization mapping |
| [`docs/PHASE12-PRODUCTION-HARDENING-DESIGN.md`](docs/PHASE12-PRODUCTION-HARDENING-DESIGN.md) | Phase 12 audit scope (§53): pre-B-001 vs device-gated work, proposed order, honest boundaries |
| [`docs/STORAGE-INVENTORY.yaml`](docs/STORAGE-INVENTORY.yaml) | Every persisted-data surface: what/where, corruption recovery (§50/§51), clear semantics — CI-enforced |
| [`docs/THREAT-MODEL-REVIEW.md`](docs/THREAT-MODEL-REVIEW.md) | Phase 12 re-validation: 14-row coverage matrix vs cores/ADRs, 10-item gap register (tracked, never silent) |
| [`docs/THREAT-MODEL.md`](docs/THREAT-MODEL.md) | Security threat model (living document) |
| [`docs/VERSIONING.md`](docs/VERSIONING.md) | §47 versioning policy: semver ladder from `1.0.0-alpha.1`, versionCode derivation + overflow rule + release registry, tag naming & the §45 release record — written before the first tag exists |
| [`docs/DEVICE-VERIFICATION-MATRIX.md`](docs/DEVICE-VERIFICATION-MATRIX.md) | The B-001 acceptance checklist: Stage-0 build/patch gates, per-phase device rows (each citing its source design), release gates, G-01…G-10 closure map, empty verification log — nothing verified until the first real build |
| [`docs/PERFORMANCE-BUDGETS.md`](docs/PERFORMANCE-BUDGETS.md) | §9/§21/§52/§7 budget table — verification targets never claims (P-1…P-9 with their device-matrix verification rows), measured-log discipline, vanilla-same-tag comparison rule |
| [`docs/DATA-SAFETY-DRAFT.md`](docs/DATA-SAFETY-DRAFT.md) | Store-readiness Play data-safety draft generated from the 23-surface storage inventory: no collection, no sharing, per-surface deletion story, pending-patch rows stated as pending |
| [`docs/ACCESSIBILITY-AUDIT.md`](docs/ACCESSIBILITY-AUDIT.md) | §49 source-level accessibility audit of the authored UI: 7 findings (2 violations) fixed, PASS evidence table, device verification mapped to D10-* |

## Development model

1. **Authoring environment (this repository).** Specifications, architecture, the tracked
   patch series, iNWEB application source, tests, and CI definitions are authored and
   reviewed here. Lightweight validation (unit tests, registry lint, baseline watch) runs
   live on GitHub-hosted runners.
2. **Build environment (external, user-provisioned).** A self-hosted GitHub Actions runner
   or cloud VM that satisfies the upstream Chromium build requirements. The exact
   specification is in `docs/BUILD-INFRASTRUCTURE.md`. Until provisioned (B-001),
   `fetch_chromium.sh` / `build_android.sh` / the Docker image are **authored but not yet
   executed** — no build result is claimed.
3. **Release engineering.** Channel-based releases (development → beta → stable) with
   semantic versioning starting at `1.0.0-alpha.1`, governed by the versioning policy
   ([`docs/VERSIONING.md`](docs/VERSIONING.md)): semver ladder, monotonic `versionCode`
   with an authoritative release registry, annotated `v<semver>` tags carrying the full
   §45 release record.

## Language policy

All source code, technical documentation, identifiers, comments, commit messages, and
project artifacts are written in **professional international English**. User-facing
product strings are localized in **Bengali (bn-BD)** and **English (en)** using Android
resource externalization (Master Specification §36).

## License

Upstream Chromium code remains under its original licenses (predominantly BSD-3-Clause)
with all notices preserved. Licensing for iNWEB-authored code is specified in
[`docs/LICENSING.md`](docs/LICENSING.md); final legal confirmation is a Phase 12 release
gate.
