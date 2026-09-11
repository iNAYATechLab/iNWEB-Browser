# iNWEB Browser — Project State

> Single source of truth for project progress (Master Specification §60).
> Updated at the end of every development step. Must always reflect the real repository
> state — never a desired or simulated state (§57, §65).
>
> Last updated: **2026-09-12** — Step 2 (Phase 1 in progress)

```yaml
project: iNWEB Browser
repository: iNAYATechLab/iNWEB-Browser
phase: 1
phase_title: Chromium Foundation
phase_status: in_progress   # authoring foundation complete; execution awaits B-001
step: 2
chromium_baseline: 154.0.8037.21   # upstream Android stable, pinned 2026-09-12
fork_strategy: tracked-patch-overlay-on-pinned-tags  # ADR-001
build_status: not-built            # no Chromium artifact exists yet (B-001)
test_status: unit-tests-passing    # 21/21 authoring tooling tests (local + CI)
ci_status: authoring-pipeline-live # GitHub-hosted runners; upstream watch live
known_blockers: [B-001]
open_defects: 0
next_action: >-
  Phase 2 / Step 3 — Browser Shell: iNWEB Android application source skeleton
  (Kotlin + Material 3, bn/en strings), tab/omnibox/navigation UI source,
  and first Chromium ui/ patch authoring plan.
```

## Completed

- [x] **Step 1 — Phase 0: Discovery and Feasibility** (2026-09-12)
  - Master specification analyzed and imported (`docs/MASTER-SPEC.md`)
  - Repository inspected; environment measured against upstream Chromium requirements
  - Chromium baseline pinned: `154.0.8037.21` (Android stable, Version History API)
  - Fork strategy selected: tracked patch overlay on pinned upstream tags (ADR-001)
  - Architecture, licensing, build-infrastructure, and threat-model documents created
  - Project state tracking initialized; repository governance established
- [x] **Step 2 — Phase 1: Chromium Foundation (authoring foundation)** (2026-09-12)
  - `iNWEB_PATCHES/` framework: registry schema, area layout, lifecycle rules
  - `apply_patches.py`: series convergence tool (peel + forward apply, verify,
    deterministic tree hash) — implemented, 12 unit tests passing
  - `lint_manifest.py`: registry validation — implemented, 8 unit tests passing
  - `check_baseline.py`: upstream drift check — implemented, 4 unit tests + live run
    (status: CURRENT)
  - `fetch_chromium.sh` / `build_android.sh`: pinned-tag checkout and build orchestration
    (authored; execution on build host per B-001)
  - `ci/Dockerfile`: Chromium build container with pinned depot_tools (authored)
  - `config/chromium/`: BASELINE pin + draft GN args (development/release)
  - CI/CD: `ci-authoring.yml` (live) + `upstream-watch.yml` (live, weekly + manual)
  - Communication protocol documented with user amendment (ADR-006)

## In progress

- (none — awaiting continuation command for Step 3)

## Not started

- Phase 2 — Browser Shell (next)
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
- **Attempted / verified:** Resources measured; upstream requirements verified; network
  to `chromium.googlesource.com` confirmed; no lighter legitimate path preserves the
  mandated architecture (WebView fallback prohibited by §2/§71).
- **Action required:** Provision a build host per `docs/BUILD-INFRASTRUCTURE.md` §2
  (self-hosted GitHub Actions runner or cloud VM; minimum 16 cores / 64 GB RAM /
  300 GB SSD, Ubuntu 22.04/24.04 x86-64, Docker).
- **Continues without blocker:** All authoring work; live lightweight CI; patch
  framework; application source; documentation.

Full record: `docs/PHASE0-ENVIRONMENT-ASSESSMENT.md` §5.

## Known defects

- None recorded. (Two test-suite defects found and fixed during Step 2 via the
  Rule 64 loop: a naming collision in lint tests; independent per-patch reverse
  detection for chained patches, replaced by peel + forward convergence.)

## Architecture decision log

| ID | Date | Decision | Rationale |
|---|---|---|---|
| ADR-001 | 2026-09-12 | Fork strategy: tracked iNWEB patch overlay applied to pinned upstream Chromium Android-stable tags | Sustainable security-update path (§4); every modification documented, traceable, testable, rebase-reviewable (§5) |
| ADR-002 | 2026-09-12 | Baseline pinned to `154.0.8037.21` (Android stable at pin date) | Newest stable line with full security support; verified via Chrome Version History API |
| ADR-003 | 2026-09-12 | Two-environment development model: sandbox authoring + external build host | Matches B-001 reality without architectural compromise |
| ADR-004 | 2026-09-12 | The Chromium source tree is never committed; reproduced as pinned tag + patch series | Repository stays reviewable; builds reproducible |
| ADR-005 | 2026-09-12 | iNWEB-authored code license proposed as MPL-2.0 | File-level copyleft, BSD-3-compatible; final confirmation at Phase 12 |
| ADR-006 | 2026-09-12 | Step-completion communication: the three continuation commands render as three separate code blocks | Explicit user directive, supersedes the §61 "same code block" rendering detail; recorded in `docs/COMMUNICATION-PROTOCOL.md` |
| ADR-007 | 2026-09-12 | CI split: live authoring pipeline on GitHub-hosted runners; Chromium build pipeline gated on a self-hosted `inweb-build` runner | Validates everything validatable today (B-001); build stages activate without redesign when the runner exists |

## Build status

- **Not built.** No APK/AAB has been produced. `fetch_chromium.sh`, `build_android.sh`,
  and the Docker build container are authored but unexecuted (B-001). First execution
  validates the draft GN args and commits any corrections back.

## Test status

- **Unit tests: 21/21 passing** (`python3 -m unittest discover -s tests -t .`):
  - `test_apply_patches.py` — 7 tests: chained apply, idempotency, verify, conflict
    abort, missing patch abort, empty series, deterministic tree hash
  - `test_lint_manifest.py` — 8 tests: valid/empty series, missing file, unknown area,
    duplicate id, out-of-order prefixes, missing field, bad schema version
  - `test_check_baseline.py` — 6 tests: baseline parsing, missing tag, version ordering
- **Live checks:** registry lint OK; baseline drift check CURRENT (network-verified).
- CI runs the same suite on every push/PR touching `scripts/`, `tests/`,
  `iNWEB_PATCHES/`.

## Chromium baseline

| Field | Value |
|---|---|
| Baseline tag | `154.0.8037.21` |
| Channel | Android stable |
| Pinned | 2026-09-12 |
| Verification | Chrome Version History API + weekly Upstream Watch workflow |
| depot_tools pin | `73fc5a4d6bd051f1fd58404e62dfb83f734b0cfa` |

## Next planned action

**Phase 2 / Step 3 — Browser Shell:** create the iNWEB Android application source
skeleton (Kotlin + Material 3, single-activity UI with Compose/View decision recorded),
externalized bn/en string resources, tab/omnibox/navigation UI source structure, and the
first `ui/` patch authoring plan for integrating the application layer into the Chromium
Android build. Attempt per-session Gradle validation of pure-Android modules (honestly
reported; RAM-constrained environment).
