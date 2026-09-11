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
| Development phase | **Phase 3 — Privacy & Tracking Protection: in progress** (decision engine complete; enforcement wiring awaits B-001) |
| Current step | 6 |
| Chromium baseline | `154.0.8037.21` (upstream Android **stable**, pinned 2026-09-12) |
| Fork strategy | Tracked iNWEB patch overlay on pinned upstream stable tags (ADR-001) |
| Core module (`src/core/browser-shell`) | **Implemented & unit-tested — 70 Kotlin tests** (tabs, omnibox, session, history, downloads, settings) |
| Privacy core (`src/core/tracking-protection`) | **Implemented & unit-tested — 70 Kotlin tests** (EasyList-family parser, URL matching, request decisions, per-site allowlist, statistics; filter-list download/cache/update management) |
| Android shell UI (`src/android-app`) | Authored — Compose + Material 3, bn/en strings; compiles in the Chromium build (B-001) |
| Patch framework | `iNWEB_PATCHES/` registry + apply/verify/hash tooling — tested |
| CI | **Live**: Python (30 tests) + registry + string parity; Kotlin core (140 tests, 2 modules); weekly upstream watch |
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
│   ├── fetch_chromium.sh            # BUILD HOST: pinned tag checkout (authored)
│   ├── build_android.sh             # BUILD HOST: GN + ninja build (authored)
│   └── bootstrap_env.sh             # Authoring-sandbox session bootstrap
├── src/
│   ├── core/browser-shell/          # Pure-JVM core (Gradle project; validated by script + CI)
│   │   ├── build.gradle.kts
│   │   └── src/{main,test}/kotlin/com/inweb/browser/shell/
│   ├── core/tracking-protection/     # Pure-JVM privacy core (parser, matcher, decision engine)
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
| [`docs/THREAT-MODEL.md`](docs/THREAT-MODEL.md) | Security threat model (living document) |

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
   semantic versioning starting at `1.0.0-alpha.1`.

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
