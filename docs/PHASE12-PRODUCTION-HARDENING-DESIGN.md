# Phase 12 — Production Hardening Design

**Status:** authored design + first item implemented (Step 35).
Phase 12 is defined by MASTER-SPEC §53: security audit, performance
audit, regression testing, crash testing, release engineering,
documentation, store readiness.

> **Reference correction:** the Step-34 proposal cited "crash reporting
> §46" and "storage/data-clearing §47" — those numbers are CI/CD and
> Release Channels. The correct anchors are **§50 Error Handling**
> ("never silently lose user data") and **§51 Crash / Recovery**
> (crash-safe state, corrupted-data detection). This document uses the
> corrected references.

## 1. Audit scope — §53 bullets mapped to concrete, verifiable work

| §53 bullet | Concrete scope | Pre-B-001 (authoring/CI) | B-001-gated (device/build) |
|---|---|---|---|
| Security audit | Threat-model review vs implemented cores; secrets handling (SecretValue, KNOWN_STORES); pinned toolchain/actions; patch risk labels | review pass over `docs/THREAT-MODEL.md` vs cores + ADR log | full audit on the built APK (permissions, network, storage) |
| Performance audit | Filter-engine benchmark (done: 0.10 ms block, 21 MB heap); startup/RAM/battery budgets | keep benchmark on demand; budget table | on-device measurements incl. Chromium multi-process behavior (§52) |
| Regression testing | The CI suite IS the regression net: 382 Kotlin core + 50 Python + 5 gates (registry, strings, externalization, structure, storage inventory) | keep green; add gates when a new defect class appears | instrumentation/UI tests on the built app |
| Crash testing | §51 contracts: every persisted store has a corruption/recovery behavior; crash-safe writes | **storage inventory gate (Step 35, live)** + recovery contracts in core tests | kill/fuzz testing on device; ANR/crash loops |
| Release engineering | §47 semver + versionCode/versionName; tagging; APK/AAB artifact pipeline (§46) | versioning policy note below | real pipeline on build infra |
| Documentation | Docs accuracy vs reality (§59) | PROJECT_STATE + README discipline; this design | — |
| Store readiness | Data-safety forms, privacy policy content | **storage inventory feeds the data-safety form** (what is stored, where, what clears it) | final forms with the shipped app |

## 2. First item — implemented (Step 35): the storage inventory gate

`docs/STORAGE-INVENTORY.yaml` + `scripts/validate_storage_inventory.py`
(12 unit tests, wired into CI):

- **21 surfaces** (14 seams + 7 adapters) each documented with: what it
  stores, where it lives, its **corruption/recovery contract** (§50:
  never silently lose user data), and its **clear semantics**
  (clear-browsing-data vs app reset — the contract the future
  clear-data surface must honor).
- **CI-enforced, bidirectional:** every persistence surface discovered
  in code (interfaces ending Store/Persistence/Cache; File*/
  SharedPreferences* adapters; `getSharedPreferences` names) MUST have
  an entry, and every entry must exist in code (stale rows fail).
  `discoverable: false` is the reviewed manual-extra hatch, reported in
  the gate output.
- In-memory-only seams state honestly which patch will persist them
  (e.g. ExtensionStore → 0013–0016) — no store is left ambiguous.

## 3. Remaining items (proposed order)

1. **Threat-model review pass** (pure docs vs code): walk
   `docs/THREAT-MODEL.md` against the 10 cores and the ADR log; record
   gaps as tracked items, not silent assumptions.
2. **Clear-data core** (pure JVM): a `ClearBrowsingData` plan model —
   which stores a "clear" action touches, driven by the inventory's
   clear column (history, session, filter-list cache, offline entries,
   per-site zoom overrides) with per-item user choice; the surface +
   patch bind later.
3. **Versioning policy** (docs): §47 semver (`1.0.0-alpha.1` style),
   versionCode/versionName maintenance, tag naming — written before
   the first build tag exists.
4. **Device-verification matrix** (docs): the B-001 checklist per
   phase (already partially in phase designs) consolidated into one
   runnable checklist.
5. Performance budgets table + store-readiness data-safety draft.

## 4. Honest boundaries

- No security/performance/crash claim about a RUNNING app can be made
  before B-001 — audits here are source-level and CI-level only.
- The inventory documents contracts; on-device verification of each
  corruption path (e.g. corrupted `history.tsv` on a real device) is a
  B-001 checklist item.
- No crash-reporting infrastructure exists and none is claimed; if ever
  added it must be opt-in, documented, and §41-compliant (telemetry
  rules).
