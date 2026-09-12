# iNWEB Browser — Threat Model Review (Phase 12 re-validation)

**Date:** 2026-09-12 (Step 36)
**Reviewed:** `docs/THREAT-MODEL.md` v0.1 (Phase 0) against the 10 core
modules that existed at review time (the tree has 11 since Step 42),
the authored app, the ADR log (001–033 at review time; 001–036 since
Step 44), and the storage inventory — per §42's "re-validated at every
phase gate" policy and the Phase 12 design (§1, security audit row).
**Output:** this review (evidence + gap register) and THREAT-MODEL.md
**v0.2** (status refresh, one new threat row).
**Amended:** Step 44 — G-07 closed (ADR-036; row 12 and the gap
register updated in place). Step 47 (§59 accuracy pass) — present-tense
evidence counts refreshed to the current tree (row 5: 116 tests;
row 13: 23 surfaces).

Method: pure docs-vs-code walk. Every claim below cites its evidence
(module + tests + ADR). Gaps are tracked items, never silent
assumptions. Nothing here claims runtime security — no binary exists
(B-001).

## 1. Coverage matrix — threat rows vs what exists today

| # | Threat (model §3) | Status | Evidence |
|---|---|---|---|
| 1 | Malicious site code (renderer) | planned — upstream preserved | No engine patches exist; patch registry rules require a security-review note for high-risk entries; Chromium tree never committed, reproduced as pinned tag + series (ADR-004) |
| 2 | Phishing / deceptive sites | core model implemented; upstream warnings at patch time | `SecurityCenter` §24 model in tracking-protection (Step 13, tested); upstream deceptive-site class of protections preserved by patch policy (licensing noted) |
| 3 | Malicious downloads | catalog only — **G-01** | `DownloadRecord` state machine + `DownloadsStore` (browser-shell, tested) track downloads; no safety-scan logic exists or is claimed; upstream mechanisms bind at patch time |
| 4 | Network attacker (TLS MITM) | planned — upstream preserved | No engine patches; no convenience cert-override additions anywhere in authored code; TLS posture inherited at build time |
| 5 | Tracking / fingerprinting | blocking core implemented; fingerprinting = upstream scope — **G-02** | Parser/matchers/combined matcher, 116 tests, decision-equivalence contract (ADR-013, ADR-022); benchmark 0.10 ms block / 21 MB heap; fingerprinting-resistance is upstream-preserved scope with no custom claims |
| 6 | Popup / redirect abuse | designed; patches B-001-gated | Popup policy design: user-activation blocking, `$popup` consultation, ONE shared per-site allowlist (ADR-020); planned patches 0008–0011 |
| 7 | Compromised extension | registry core implemented; enforcement patch-side — **G-10** | Extension registry state machine + permission-review records (17 tests); sideload-first, no silent installs, manual updates (ADR-023); scoped API surface + kill-switch ship with patches 0013–0016 |
| 8 | Credential theft on device | partial — **G-03** | VPN secrets opaque by construction (`SecretValue`, ADR-025); backup secret guard (`KNOWN_STORES`, ADR-026); Keystore/BiometricPrompt envelope = patch 0020, pending |
| 9 | Sync compromise | model only — **G-06** | Sync engine (LWW, tombstones, backoff, 12 tests) is a transport-agnostic model; E2E encryption/scoped tokens are future infrastructure; documented absence (§29, ADR-026) |
| 10 | Backup leakage | integrity core implemented; encryption pending — **G-04** | Versioned bundles, per-entry SHA-256, preview gating, KNOWN_STORES plaintext-secret guard (13 tests, ADR-026); encryption at the Android layer ships with the binding |
| 11 | Profile leakage | isolation contract implemented; binding pending — **G-05** | Namespace isolation is structural in the registry (12 tests, ADR-026); per-profile data directories bind via patch 0022 |
| 12 | Supply chain (upstream/deps/filter lists) | authoring-side substantially implemented — **G-08** (G-07 closed, Step 44) | Patch registry + apply/verify/hash tooling (ADR-004); pinned baseline, depot_tools, kotlinc, actions versions (verified current); filter-list transport with size cap + conditional revalidation + atomic cache (ADR-014) + SHA-256 content pinning in the update path (Step 44, ADR-036); SBOM at release pending |
| 13 | Data loss from crashes/corruption | implemented at source level; device verification B-001 — **G-09** | Crash-safe session (ADR-011); atomic TSV stores with header-corruption recovery (ADR-015/016); corrupt-preference recovery pattern across toolbar/notifications/zoom/download-prefs (ADR-029/030/032); ALL 23 persisted surfaces carry CI-audited corruption contracts (STORAGE-INVENTORY.yaml, ADR-033) |
| 14 | **NEW** Notification abuse (impersonation, promotional pressure) | structurally mitigated | Real-events-only policy; no generic notify API exists; promotional/sync/update events have no code path; per-channel toggles; lazy permission (ADR-028/030, 23 tests) |

## 2. Gap register (tracked items — no silent assumptions)

| ID | Gap | Honest statement | Owner (where it lands) |
|---|---|---|---|
| G-01 | Download safety logic | A downloads CATALOG exists; no scan/verification logic exists or is claimed | Phase 4 patch design note; upstream mechanisms only |
| G-02 | Fingerprinting resistance | Upstream-preserved scope; no custom implementation, no claims | Phase 3/4 designs (already worded this way) |
| G-03 | Keystore/BiometricPrompt binding | Secrets are opaque in cores; at-rest encryption + biometric gating bind via patch 0020 (ADR-025 envelope design) | patch 0020, B-001 |
| G-04 | Backup encryption | Bundle integrity + no-plaintext-secrets are core-tested; archive encryption happens at the Android layer | backup binding step, B-001 |
| G-05 | Per-profile directories | The namespace contract is structural and tested; directory binding is patch 0022 | patch 0022, B-001 |
| G-06 | Sync E2E encryption | No sync backend exists; the core is a model only; §29 documented absence | future infrastructure design (if ever directed) |
| G-07 | Filter-list content integrity | **Closed (Step 44, ADR-036):** every downloaded list body is pinned with its SHA-256 in the cache metadata and re-verified on every cache load; a mismatching copy is never served — the manager re-downloads (when allowed) or reports an honest failure with nothing loaded. Integrity pinning, not a publisher signature (none claimed, §57) | closed in the pure-JVM core (tracking-protection, 116 tests); on-device fault injection on matrix row D12-1 |
| G-08 | SBOM | No software bill of materials yet | release engineering (Phase 12 design §1) |
| G-09 | Device verification of corruption contracts | All recovery contracts are source-level + CI-audited; on-device verification pending | B-001 checklist (Phase 12 design item 4) |
| G-10 | Extension enforcement surface | Registry/permission-review core done; scoped API surface + kill-switch enforcement ship with patches 0013–0016 | patches 0013–0016, B-001 |

## 3. Model changes made in v0.2

- Added threat row 14 (notification abuse) with the ADR-028/030
  structural mitigation.
- Extended the data-loss row to name the storage inventory (ADR-033)
  as the systematic contract.
- Residual risks refreshed: the Phase 0 "extensions scope undecided"
  note is stale — the Phase 6 design decided it (ADR-023); VPN/Sync
  absences unchanged; B-001 unchanged.
- Pointer to this review added (re-validation evidence).

## 4. Verdict

No threat row is uncovered-by-design: every mitigation is either
implemented (with tests), designed (with a patch plan), or a
documented, tracked gap. Ten gaps were registered (G-01…G-10); G-07
closed in Step 44 — nine remain open, none silently assumed away.
All runtime claims remain gated on B-001.
