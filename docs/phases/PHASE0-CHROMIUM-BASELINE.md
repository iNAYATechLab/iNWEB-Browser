# Phase 0 — Chromium Baseline & Fork Strategy

**Date:** 2026-09-12
**Scope:** Master Specification §3 (Source Strategy), §4 (Upstream Update Strategy),
§5 (Patch Architecture). Decision recorded as ADR-001 / ADR-002.

---

## 1. Baseline (pinned)

| Field | Value |
|---|---|
| Product | Upstream Chromium (open source) |
| Channel tracked | Android **Stable** |
| Baseline tag | `154.0.8037.21` |
| Pinned on | 2026-09-12 |
| Verification | Chrome Version History API — top entry for `platforms/android/channels/stable` on pin date |
| Host checkout (on build infrastructure) | `fetch --no-history chromium` then `gclient sync --revision src@154.0.8037.21` |

The baseline is re-pinned to the then-current Android stable at every routine or
emergency rebase (§3 below). The pin is recorded in `PROJECT_STATE.md` and in every
release's build metadata.

## 2. Strategy decision (ADR-001)

**Selected: Strategy 4 — a custom iNWEB tracked-patch overlay applied on pristine, pinned
upstream Chromium stable tags.**

Evaluation against the master specification's candidate strategies:

| # | Candidate strategy | Assessment | Decision |
|---|---|---|---|
| 1 | Direct long-lived fork (merge-based) | Highest merge burden; history divergence makes each security rebase expensive and risk-prone; conflicts with §4's sustainability requirement | Rejected |
| 2 | Fork of another Chromium-derived browser codebase | Inherits a third party's product decisions, cadence, and brand/legal exposure; reduces control over patch scope | Rejected as base |
| 3 | Legally compatible open-source derived baseline (e.g., the Bromite/Cromite lineage) | Valuable engineering reference for privacy patching; adopting wholesale imports someone else's roadmap and maintenance risk | Rejected as base; individual techniques may be re-implemented under our registry **with license review and attribution** |
| 4 | **Custom fork with tracked iNWEB patches (patch overlay on pinned tags)** | Patch series lives in `iNWEB_PATCHES/`, applied by tooling over a pristine checkout. Every modification documented, traceable, testable, rebase-reviewable. Matches §5 and the maintenance model used successfully by shipped Chromium-derived browsers | **Selected** |

**Technical justification.**

1. **Security sustainability (§4).** Rebasing a reviewed patch series across stable tags
   is dramatically cheaper and safer than merging a diverged multi-gigabyte tree. A
   one-time fork without an update path is explicitly unacceptable upstream of this
   decision.
2. **Full engine control.** All engine-level privacy/security/adblock work remains
   possible; no WebView anywhere in the stack (§2/§71).
3. **License hygiene (§58).** Upstream remains pristine BSD-3-class; our delta is exactly
   the reviewed patch series — the cleanest possible modification record.
4. **Traceability (§5).** `git log` of our repo + the ordered patch registry is a complete
   audit trail of every divergence from upstream.
5. **APK size levers (§7).** GN argument control, component removal, ABI strategy, and
   bundle/dynamic-delivery options all remain available.

## 3. Upstream update strategy (implements §4)

```text
UPSTREAM CHROMIUM (Android stable channel)
        ↓  watch: Version History API + Chromium release/security advisories
TRACK NEW STABLE TAG
        ↓  routine cadence: every stable refresh (~monthly)
        ↓  emergency cadence: critical CVEs (target ≤ 7 days)
PIN NEW TAG → gclient sync → APPLY PATCH SERIES
        ↓  conflicts resolved and recorded per patch
PATCH VALIDATION (per-patch tests + tree-hash check)
        ↓
BUILD (build host) → TEST (unit/integration/regression)
        ↓
RELEASE (channel promotion) + REBASE REPORT
```

Every rebase produces `docs/rebases/<old-tag>..<new-tag>.md` recording, for each patch:
`clean` | `conflict-resolved (how)` | `reworked (why)` | `dropped (why)`.

## 4. Patch architecture (implements §5)

```text
iNWEB_PATCHES/
├── MANIFEST.yaml          # ordered registry — the single patch authority
├── privacy/               # tracking protection, storage/cookie controls, fingerprinting resistance
├── security/              # hardening, secure defaults, abuse protections
├── adblock/               # engine-level request filtering + cosmetic filtering hooks
├── popup_protection/      # popup blocking, redirect-abuse protection
├── extension/             # WebExtensions-on-Android scope (Phase 6)
├── performance/           # startup / memory / battery-oriented changes
├── ui/                    # iNWEB Android application layer integration
├── offline/               # offline pages / reader mode plumbing
├── settings/              # iNWEB settings model integration
└── tests/                 # validation executed after patch application
```

**Patch rules (enforced by tooling, authored Phase 1):**

1. No untracked modification may exist in a Chromium working tree. The tree must always
   be reproducible as `pristine@tag + ordered patch series` (CI verifies by hashing).
2. Naming: `NNNN-<area>-<slug>.patch`, where `NNNN` is the global application order.
3. Every patch is registered in `MANIFEST.yaml` with: id, area, upstream files touched,
   purpose, risk class, validation method, and rebase notes.
4. Patches are kept small and orthogonal; unrelated changes may not be combined into one
   patch (§55 incremental discipline, applied at patch level).
5. Any patch touching the process model, sandboxing, IPC, or permissions requires a
   security review note before it may enter the series (see `../THREAT-MODEL.md`).
