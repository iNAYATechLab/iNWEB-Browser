# iNWEB Browser — Documentation Index

Complete map of the project documentation. The main
[`README.md`](../README.md) carries the project overview and current status;
this index is the authoritative navigation for everything under `docs/`.

## Governing documents

| Document | Purpose |
|---|---|
| [`MASTER-SPEC.md`](MASTER-SPEC.md) | Governing master specification (single source of truth) |
| [`COMMUNICATION-PROTOCOL.md`](COMMUNICATION-PROTOCOL.md) | Step-completion communication format (user-amended) |
| [`VERSIONING.md`](VERSIONING.md) | §47 versioning policy: semver ladder from `1.0.0-alpha.1`, versionCode derivation + release registry |
| [`LICENSING.md`](LICENSING.md) | License landscape and obligations |

## Architecture & infrastructure

| Document | Purpose |
|---|---|
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | System architecture and module map |
| [`BUILD-INFRASTRUCTURE.md`](BUILD-INFRASTRUCTURE.md) | CI/CD design, build host specification, reproducibility |
| [`BUILD-HOST-RUNBOOK.md`](BUILD-HOST-RUNBOOK.md) | B-001 operational walkthrough (provision → pre-flight → pinned fetch → build → matrix recording) |

## Phase designs — [`phases/`](phases/)

| Document | Purpose |
|---|---|
| [`phases/PHASE0-ENVIRONMENT-ASSESSMENT.md`](phases/PHASE0-ENVIRONMENT-ASSESSMENT.md) | Measured environment, Chromium build feasibility, blocker B-001 |
| [`phases/PHASE0-CHROMIUM-BASELINE.md`](phases/PHASE0-CHROMIUM-BASELINE.md) | Baseline pin, fork strategy, rebase & patch policy |
| [`phases/PHASE2-INTEGRATION-PLAN.md`](phases/PHASE2-INTEGRATION-PLAN.md) | Patch-by-patch plan binding `src/` into the Chromium Android build |
| [`phases/PHASE4-ADBLOCK-DESIGN.md`](phases/PHASE4-ADBLOCK-DESIGN.md) | `adblock/` patch-series design — request interception, type mapping, verification |
| [`phases/PHASE4-POPUP-PROTECTION-DESIGN.md`](phases/PHASE4-POPUP-PROTECTION-DESIGN.md) | `popup_protection/` patch-series design — popups, redirects, notifications, downloads |
| [`phases/PHASE4-COSMETIC-DESIGN.md`](phases/PHASE4-COSMETIC-DESIGN.md) | Cosmetic filtering — tested Kotlin engine + injection patch design |
| [`phases/PHASE5-PERFORMANCE-DESIGN.md`](phases/PHASE5-PERFORMANCE-DESIGN.md) | Performance budgets, measured v1 baseline, combined-matcher & cache design, device plan |
| [`phases/PHASE6-EXTENSION-DESIGN.md`](phases/PHASE6-EXTENSION-DESIGN.md) | Extension system design — API table, models, patch plan, build-time audit |
| [`phases/PHASE7-OFFLINE-DESIGN.md`](phases/PHASE7-OFFLINE-DESIGN.md) | Offline reading, snapshots, cache controls, honest data saver — patch plan |
| [`phases/PHASE8-VPN-SECURITY-DESIGN.md`](phases/PHASE8-VPN-SECURITY-DESIGN.md) | Bring-your-own VPN, biometric app lock, encryption model, documented 2FA absence |
| [`phases/PHASE9-PROFILES-SYNC-DESIGN.md`](phases/PHASE9-PROFILES-SYNC-DESIGN.md) | Isolated profiles, documented sync absence, queue model, encrypted backup format |
| [`phases/PHASE10-LOCALIZATION-ACCESSIBILITY-DESIGN.md`](phases/PHASE10-LOCALIZATION-ACCESSIBILITY-DESIGN.md) | bn-BD/en policy, CI externalization gate, §49 authoring contracts, verification matrix |
| [`phases/PHASE11-NOTIFICATIONS-FEATURES-DESIGN.md`](phases/PHASE11-NOTIFICATIONS-FEATURES-DESIGN.md) | Privacy-first notifications, entertainment non-goal, §23 customization mapping |
| [`phases/PHASE12-PRODUCTION-HARDENING-DESIGN.md`](phases/PHASE12-PRODUCTION-HARDENING-DESIGN.md) | Phase 12 audit scope (§53): pre-B-001 vs device-gated work, honest boundaries |

## Security & privacy

| Document | Purpose |
|---|---|
| [`THREAT-MODEL.md`](THREAT-MODEL.md) | Security threat model (living document) |
| [`THREAT-MODEL-REVIEW.md`](THREAT-MODEL-REVIEW.md) | Phase 12 re-validation: 14-row coverage matrix, 10-item gap register |
| [`DATA-SAFETY-DRAFT.md`](DATA-SAFETY-DRAFT.md) | Play data-safety draft generated from the storage inventory |

## QA, budgets & audits

| Document | Purpose |
|---|---|
| [`DEVICE-VERIFICATION-MATRIX.md`](DEVICE-VERIFICATION-MATRIX.md) | B-001 acceptance checklist: Stage-0 build/patch gates, per-phase device rows, G-01…G-10 closure map |
| [`PERFORMANCE-BUDGETS.md`](PERFORMANCE-BUDGETS.md) | §9/§21/§52/§7 budget table — verification targets, never claims |
| [`ACCESSIBILITY-AUDIT.md`](ACCESSIBILITY-AUDIT.md) | §49 source-level accessibility audit of the authored UI |

## Data

| Document | Purpose |
|---|---|
| [`STORAGE-INVENTORY.yaml`](STORAGE-INVENTORY.yaml) | Every persisted-data surface: what/where, corruption recovery, clear semantics — CI-enforced |

## Design & brand — [`design/`](design/)

| Document | Purpose |
|---|---|
| [`design/BRAND.md`](design/BRAND.md) | Brand identity: naming (ADR-038), logo v1 provenance, asset-derivation spec, QA record, regeneration runbook |
| [`design/assets/`](design/assets/) | Logo study record + final xxxhdpi icon renders (visual reference) |

## Releases — [`releases/`](releases/)

| Document | Purpose |
|---|---|
| [`releases/v1.0.0-alpha.1.md`](releases/v1.0.0-alpha.1.md) | Engine-baseline release notes (first release) |
| [`releases/_template.md`](releases/_template.md) | Release-notes template (copy per version) |

## Verification evidence — [`verification/`](verification/)

| Document | Purpose |
|---|---|
| [`verification/B001-BUILD-CHAIN.md`](verification/B001-BUILD-CHAIN.md) | Full B-001 chain ledger: 14 runs / ~45 h, root causes, exact-remainder resumes, alpha.1 result |
| [`verification/B001-STAGE1-RUN5.md`](verification/B001-STAGE1-RUN5.md) | Stage-1 boxed-compile evidence (18,104/81,578 targets before the hosted-job wall) |
