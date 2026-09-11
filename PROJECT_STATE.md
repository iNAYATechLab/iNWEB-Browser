# iNWEB Browser — Project State

> Single source of truth for project progress (Master Specification §60).
> Updated at the end of every development step. Must always reflect the real repository
> state — never a desired or simulated state (§57, §65).
>
> Last updated: **2026-09-12** — Step 1 (Phase 0 complete)

```yaml
project: iNWEB Browser
repository: iNAYATechLab/iNWEB-Browser
phase: 0
phase_title: Discovery and Feasibility
phase_status: complete
step: 1
chromium_baseline: 154.0.8037.21   # upstream Android stable, pinned 2026-09-12
fork_strategy: tracked-patch-overlay-on-pinned-tags  # ADR-001
build_status: not-built            # no compilable artifact exists yet
test_status: not-applicable        # Phase 0 produced documentation only
known_blockers: [B-001]
open_defects: 0
next_action: >-
  Phase 1 / Step 2 — Chromium Foundation:
  patch framework (registry + apply/verify tooling),
  fetch/rebase/build orchestration scripts,
  build container (Dockerfile), CI/CD pipeline scaffold.
```

## Completed

- [x] **Step 1 — Phase 0: Discovery and Feasibility** (2026-09-12)
  - Master specification analyzed and imported (`docs/MASTER-SPEC.md`)
  - Repository inspected (pre-existing empty `iNAYATechLab/iNWEB-Browser`, branch `main`)
  - Environment measured and compared against upstream Chromium build requirements
  - Chromium baseline pinned: `154.0.8037.21` (Android stable, verified via the Chrome
    Version History API)
  - Fork strategy selected: tracked patch overlay on pinned upstream tags (ADR-001)
  - Architecture, licensing, build-infrastructure, and threat-model documents created
  - Project state tracking initialized; repository governance established

## In progress

- (none)

## Not started

- Phase 1 — Chromium Foundation (next)
- Phase 2 — Browser Shell
- Phase 3 — Privacy
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
- **Attempted / verified:** Resources measured; upstream requirement documents verified;
  network reachability to `chromium.googlesource.com` and Google storage endpoints
  confirmed working; no lighter legitimate path exists that preserves the Chromium
  architecture (WebView fallback is prohibited by §2/§71).
- **Action required:** Provision a build host — self-hosted GitHub Actions runner or cloud
  VM per `docs/BUILD-INFRASTRUCTURE.md` §2. This is a user-provisioned resource.
- **Continues without blocker:** Everything except linking Chromium: patch framework,
  iNWEB application source, tests, CI definitions, documentation, licensing, and
  lightweight per-session validations.

Full record: `docs/PHASE0-ENVIRONMENT-ASSESSMENT.md` §5.

## Known defects

- None recorded.

## Architecture decision log

| ID | Date | Decision | Rationale |
|---|---|---|---|
| ADR-001 | 2026-09-12 | Fork strategy: tracked iNWEB patch overlay applied to pinned upstream Chromium Android-stable tags (not a long-lived merge fork) | Sustainable security-update path (§4); every modification documented, traceable, testable, rebase-reviewable (§5); lowest long-term maintenance cost |
| ADR-002 | 2026-09-12 | Baseline pinned to `154.0.8037.21` (Android stable at pin date) | Newest stable line with full security support; verified via Chrome Version History API |
| ADR-003 | 2026-09-12 | Two-environment development model: sandbox authoring + external build host | Matches B-001 reality without any architectural compromise |
| ADR-004 | 2026-09-12 | The Chromium source tree is never committed to this repository; it exists only on build infrastructure and is reproduced by pinned tag + patch series | Repository stays reviewable; builds reproducible; workspace limits respected |

## Build status

- **Not built.** No APK/AAB has ever been produced for this project. No build claims are
  made. First build executes on B-001 resolution (Phase 1 infrastructure).

## Test status

- **Not applicable.** Phase 0 delivered specifications and governance documents only.
  Test suites are introduced with the first compilable code (Phase 1 tooling, Phase 2
  application code).

## Chromium baseline

| Field | Value |
|---|---|
| Baseline tag | `154.0.8037.21` |
| Channel | Android stable |
| Pinned | 2026-09-12 |
| Verification | Chrome Version History API (`platforms/android/channels/stable`) |

## Next planned action

**Phase 1 / Step 2 — Chromium Foundation:** create `iNWEB_PATCHES/` framework (registry
format + apply/verify tooling), Chromium fetch/rebase/build orchestration scripts, build
container definition, and the CI/CD pipeline scaffold, per `docs/BUILD-INFRASTRUCTURE.md`.
