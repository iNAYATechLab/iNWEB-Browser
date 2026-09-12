# iNWEB Browser — Performance Budgets (§9/§21/§52/§7)

**Status:** Phase 12 design-order item 5a (Step 42). The consolidated
budget table — **verification targets, never claims** (§9: "Measure
performance rather than making unsupported claims"). No number below is
a claim about the product until it is measured on real hardware at
B-001, per the device-matrix rows D5-1…D5-4; until then the only
measured performance facts are the JVM filter-engine benchmark rows at
the bottom, with their hardware recorded.

**Source authority:** PHASE5-PERFORMANCE-DESIGN.md §2 remains the
design source; this table is the consolidated release-facing view
(adds the §7 APK-size and §21/§52 battery/resource targets). Where the
two disagree, the design doc wins and this table is corrected (§59).

---

## 1. Budget table

| # | Area | Budget (verification target) | Verified by | Status today |
|---|---|---|---|---|
| P-1 | Per-request filter decision (p95) | < 1 ms (target 0.2 ms) with the combined matcher | D5-2: perfetto span around `decide()` | JVM benchmark: **0.10 ms** block / 0.07 ms pass (v2, sandbox) — device number pending |
| P-2 | Page-load overhead vs vanilla Chromium (same pinned tag) | < 5% on a heavy news page | D5-2: instrumented A/B navigation | unmeasured (no build) |
| P-3 | Filter-engine init (parse + index, 2 lists) | < 500 ms, OFF the critical path (async startup, cache-first per ADR-014) | D5-1: startup trace | unmeasured (no build) |
| P-4 | Engine memory (EasyList + EasyPrivacy scale) | < 60 MB | D5-3: `dumpsys meminfo` PSS delta | JVM benchmark: ~21 MB heap (v2, sandbox) — device PSS pending |
| P-5 | App cold start | within 10% of a vanilla build of the SAME tag | D5-1: `am start -W`, 10-run median | unmeasured (no build) |
| P-6 | Base APK size (§7) | ≤ vanilla same-tag build with equivalent GN args **+ 10%** — the absolute reference number is fixed by the FIRST vanilla baseline build, not invented now | B-4 artifact size vs a vanilla build of the same tag | unmeasured (no build; §7 forbids trading Chromium for WebView to hit any size) |
| P-7 | Battery (§21) | scripted browsing session drains < 5% more battery than the vanilla build (A/B, same device, same script) | D5-4: `batterystats`/historian A/B | unmeasured (no build) |
| P-8 | Background behavior (§21/§52) | iNWEB patches never defeat upstream tab-freezing or timer-throttling; no new always-on background work | D5-4: measurement + patch review (every patch touching lifecycle needs this review) | patch series empty (B-001) |
| P-9 | Scrolling responsiveness (§9) | frame-timing distribution on a heavy page not worse than the vanilla build beyond measurement noise | D5-2: frame timing | unmeasured (no build) |

## 2. Rules

1. **A budget is a target until measured.** Measured values are
   appended to the log (§3) with hardware + build id (tag +
   `versionCode` per VERSIONING.md). Release notes cite the log, never
   the target column.
2. **A missed budget is a filed defect** against the release gate
   (device-matrix R-3), not a quietly rewritten budget. Budget changes
   require an ADR — same discipline as the versioning contract.
3. **Comparisons are always against a vanilla build of the SAME pinned
   tag** on the same hardware — absolute numbers across Chromium
   versions are noise, not evidence.
4. **§7 red line:** APK-size optimization uses component removal,
   build configuration, resource/ABI strategy, modular delivery,
   feature gating — NEVER replacing the Chromium architecture with a
   WebView (§2 non-negotiable).
5. CI does not run benchmarks (shared runners produce noise, not
   data — PHASE5 §6); the JVM benchmark script stays on-demand.

## 3. Measurement log

Appended per measurement run, with hardware and build id. The JVM rows
are the complete list of measured facts today.

| What | Result | Hardware / build | Date |
|---|---|---|---|
| Filter engine v1 (regex-per-rule), block/pass decision | 16.5 ms / 18.1 ms, ~111 MB heap | sandbox, 2 vCPU, OpenJDK 11 | Step 17 |
| Filter engine v2 (combined matcher), block/pass decision | **0.10 ms / 0.07 ms, ~21 MB heap** (~165× / ~259× vs v1, decision-equivalent) | sandbox, 2 vCPU, OpenJDK 11 | Step 18 |
| Device measurements (P-1…P-9) | *(none — no build exists; B-001)* | | |

## 4. Honest boundaries

- No device measurement exists: no Chromium build has ever been
  produced (B-001), and the patch registry is intentionally empty.
- The JVM benchmark proves the pure-JVM core's decision cost and the
  combined matcher's equivalence — it is NOT a device-performance
  claim (PHASE5 §6).
- Budgets P-6/P-7 deliberately reference a vanilla build instead of
  absolute numbers, because inventing absolute targets without a
  baseline measurement would be exactly the unsupported claim §9
  forbids.
