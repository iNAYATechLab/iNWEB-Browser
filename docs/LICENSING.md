# iNWEB Browser — Licensing

**Status:** Phase 0 assessment. Final legal confirmation is a Phase 12 release gate (§58).

---

## 1. License landscape

| Component | License | Notes |
|---|---|---|
| Upstream Chromium source | BSD-3-Clause (root `LICENSE`) plus per-component third-party licenses | Must be preserved verbatim in source and distributions |
| Chromium third-party dependencies | Individual licenses under `third_party/` (Apache-2.0, MIT, MPL-2.0, LGPL-family, etc.) | Upstream tracks them; notices must carry into our builds and attribution screens |
| Android SDK / NDK | Android Software Development Kit Terms (Google) | Applies to toolchain usage on the build host |
| iNWEB-authored code | Proposed: **MPL-2.0** (ADR pending final confirmation) | File-level copyleft; compatible with BSD-3 upstream integration |
| Filter lists (Phase 4) | Per-list licenses (EasyList-family and others vary) | **License vetting gate before any bundling**; per-list attribution required |

## 2. Obligations (implements §58)

1. **Preserve** all upstream copyright notices and `LICENSE` files in source and in
   distributed artifacts.
2. **Attribute**: ship a consolidated open-source licenses screen (Settings → About),
   generated from the Chromium build's license-collection tooling at build time.
3. **Document modifications**: satisfied structurally by the tracked patch registry —
   every divergence from upstream is a reviewed, registered patch
   (`PHASE0-CHROMIUM-BASELINE.md` §4).
4. **No proprietary code** may be introduced anywhere in the project.
5. **Trademark hygiene**: iNWEB is an independent browser and must not present itself as
   endorsed by or affiliated with Google or the Chromium project. No Chrome/Chromium
   logos; community-standard "Powered by Chromium" style attribution only.

## 3. Review gates by phase

| Phase | Gate |
|---|---|
| 4 — Ad blocking | Filter-list license audit (per list: license, attribution, update source) |
| 6 — Extensions | Extension-store / API terms review if applicable |
| 8 — VPN | VPN protocol and library licenses verified at selection (e.g., WireGuard-tooling family: MIT/Apache-2.0 class — verify actual components) |
| 9 — Sync | Backend component licenses; cryptographic library licenses |
| 12 — Release | Full SBOM + consolidated attribution + legal review before stable promotion |

## 4. Decision record

- **ADR-005 (proposed, Phase 0):** iNWEB-authored code license = MPL-2.0.
  Rationale: file-level copyleft keeps our modifications open and traceable while
  remaining trivially compatible with BSD-3-Clause upstream code. Final confirmation is
  deferred to release readiness; no incompatible mixing occurs before then.
