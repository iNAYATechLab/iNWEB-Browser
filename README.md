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
| Development phase | **Phase 0 — Discovery & Feasibility: COMPLETE** |
| Current step | 1 |
| Chromium baseline | `154.0.8037.21` (upstream Android **stable**, pinned 2026-09-12) |
| Fork strategy | Tracked iNWEB patch overlay on pinned upstream stable tags (ADR-001) |
| Build | **Not yet executed** — requires external build infrastructure (blocker B-001) |
| Tests | **Not yet applicable** — Phase 0 intentionally produced no compilable code |
| Open defects | None recorded |

Machine-readable, always-current progress: [`PROJECT_STATE.md`](PROJECT_STATE.md)

## Repository layout

Current (Phase 0):

```text
inweb-browser/
├── README.md                        # This file
├── PROJECT_STATE.md                 # Machine-readable project state (§60)
├── .gitignore                       # Chromium tree / artifacts are never committed
└── docs/
    ├── MASTER-SPEC.md               # Governing master specification (verbatim)
    ├── PHASE0-ENVIRONMENT-ASSESSMENT.md
    ├── PHASE0-CHROMIUM-BASELINE.md
    ├── ARCHITECTURE.md
    ├── LICENSING.md
    ├── BUILD-INFRASTRUCTURE.md
    └── THREAT-MODEL.md
```

Planned (introduced in later phases, never containing the Chromium source itself):

```text
├── iNWEB_PATCHES/                   # Tracked patch series (§5)
│   ├── MANIFEST.yaml                # Patch registry: order, metadata, rebase notes
│   ├── privacy/  security/  adblock/  popup_protection/
│   ├── extension/  performance/  ui/  offline/  settings/
│   └── tests/                       # Per-patch validation tests
├── src/                             # iNWEB Android application source (Kotlin, M3)
├── scripts/                         # fetch / apply-patches / verify / build orchestration
├── build/config/inweb/              # Committed GN args per channel
├── ci/                              # Build container image (Dockerfile) and tooling
└── .github/workflows/               # CI/CD pipeline definitions (§46)
```

The Chromium checkout (`chromium/`, ~100 GB) exists **only on build infrastructure** and is
git-ignored here by design.

## Documentation index

| Document | Purpose |
|---|---|
| [`docs/MASTER-SPEC.md`](docs/MASTER-SPEC.md) | Governing master specification (single source of truth) |
| [`docs/PHASE0-ENVIRONMENT-ASSESSMENT.md`](docs/PHASE0-ENVIRONMENT-ASSESSMENT.md) | Measured environment, Chromium build feasibility, blocker B-001 |
| [`docs/PHASE0-CHROMIUM-BASELINE.md`](docs/PHASE0-CHROMIUM-BASELINE.md) | Baseline pin, fork strategy, rebase & patch policy |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | System architecture and module map |
| [`docs/LICENSING.md`](docs/LICENSING.md) | License landscape and obligations |
| [`docs/BUILD-INFRASTRUCTURE.md`](docs/BUILD-INFRASTRUCTURE.md) | CI/CD design, build host specification, reproducibility |
| [`docs/THREAT-MODEL.md`](docs/THREAT-MODEL.md) | Security threat model (living document) |

## Development model

1. **Authoring environment (this repository).** Specifications, architecture, the tracked
   patch series, iNWEB application source, tests, and CI definitions are authored and
   reviewed here.
2. **Build environment (external, user-provisioned).** A self-hosted GitHub Actions runner
   or cloud VM that satisfies the upstream Chromium build requirements. The exact
   specification is in `docs/BUILD-INFRASTRUCTURE.md`.
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
