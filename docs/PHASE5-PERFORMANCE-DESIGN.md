# Phase 5 — Performance Design & Measurement Plan

**Status:** authored design + MEASURED v1 baseline (Step 17). Device-level
execution needs the built product (B-001); everything measurable today has
been measured (MASTER-SPEC §9: "Measure performance rather than making
unsupported claims").
**Governing requirements:** MASTER-SPEC §9 (performance), §21 (energy
saver), §52 (resource management); ADR-013 deferred the combined matcher
to this phase.

---

## 1. Measured v1 baseline (filter engine)

Protocol: `bash scripts/benchmark_filter_engine.sh 25000 200` — synthetic
EasyList-scale corpus (25,000 network rules, deterministic mix of domain
anchors, options, exceptions, path wildcards), warmup included so every
rule regex is compiled before timing.

**Hardware context:** authoring sandbox — 2 vCPU x86_64, Linux, OpenJDK 11,
`-Xmx1g`, no other load. Numbers are honest for THIS setup only.

| Metric | v1 (regex-per-rule) |
|---|---|
| Parse throughput | ~187,000 rules/sec (25,000 rules in 134 ms) |
| Lazy regex warmup (12 decisions) | 920 ms |
| Heap after warmup (25k rules) | ~111 MB |
| Block-path decision | **~16.5 ms/request** (60/sec) |
| Pass-path decision (worst case — scans all rules) | **~18.1 ms/request** (55/sec) |

**Interpretation (honest):** v1 is correct and CI-validated, but a typical
page triggering 50–100 requests would spend **~0.8–1.8 s of CPU** in
decisions — unusable for production. This is precisely why ADR-013
deferred the combined matcher; the measurement above is the concrete
justification, not a guess. Enforcement must not ship without the matcher.

## 2. Budgets (verification targets — not claims)

All budgets are verified on real hardware when the product builds (B-001);
they fail CI-style review until measured:

| Area | Budget | How verified |
|---|---|---|
| Per-request decision (p95) | < 1 ms (target 0.2 ms) after combined matcher | perfetto span around `decide()` |
| Page-load overhead vs vanilla Chromium (same tag) | < 5% on a heavy news page | instrumented A/B navigation test |
| Filter-engine init (parse + index build, 2 lists) | < 500 ms, OFF the critical path (async at startup, cache-first per ADR-014) | startup trace |
| Engine memory (EasyList + EasyPrivacy scale) | < 60 MB | `dumpsys meminfo` PSS delta |
| App cold start | within 10% of vanilla Chromium build of the same tag | `am start -W`, 10-run median |

## 3. Combined matcher (Step 18 — the immediate deliverable)

uBlock-style **token-index candidate selection**, keeping the public API
(`decide`) and semantics untouched:

1. At parse/index time, each network rule contributes a **required token**
   — the longest literal run (≥ 3 chars) of its pattern. Rules without a
   usable literal go into a small **always-check** bucket.
2. Lookup: tokenize the lowercased request URL; union the rule lists of
   every token present; evaluate only those candidates + the always-check
   bucket, in list order (first-match-wins semantics preserved).
3. **Correctness contract:** a rule whose required token does not occur in
   the request URL cannot match — its pattern contains that token
   literally. Therefore the candidate set is a superset of all possible
   matches. Verified by an equivalence test: for a large probe set of
   requests, combined-matcher decisions must be IDENTICAL to v1's, over
   the full synthetic corpus plus real EasyList-shaped edge cases.
4. Exceptions (`@@`) index the same way; option checks (type/party/domain)
   run after candidate selection as today.
5. Target: candidate sets of ~1–50 rules per request → re-run the same
   benchmark; success = ≥ 100× decision throughput with zero decision
   differences.

## 4. Decision cache

- Per-engine-instance bounded LRU (4096 entries) keyed by
  `(requestUrl, documentUrl, resourceType)` → decision. The engine is
  deterministic per snapshot, so entries cannot go stale within an
  instance; an engine swap after a list refresh creates a NEW engine
  (with a fresh cache) — invalidation by construction.
- Only added **after** measuring real hit-rates; memory cost counted
  against the §2 budget. The combined matcher may make the cache
  unnecessary — measure first (§9).

## 5. Device measurement plan (B-001 gated)

1. **Startup:** `am start -W` cold/warm, 10-run medians, vs a vanilla
   build of the SAME pinned tag.
2. **Navigation:** perfetto traces; `decide()` and injection spans on the
   worker threads; scrolling jank via frame timing on a heavy page.
3. **Memory:** `dumpsys meminfo` PSS deltas (engine, lists, caches).
4. **Battery (§21):** `batterystats`/historian A/B over a scripted
   browsing session; tab-freezing and timer-throttling behavior audited
   at the baseline (upstream provides the machinery; our patches must not
   defeat it — checked in patch review).
5. Every number lands in the log below with hardware + build id.

## 6. Benchmark protocol & log

- Same script, same corpus seed, same JVM flags; hardware recorded; CI
  does NOT run benchmarks (shared runners produce noise, not data).
- Re-run after the combined matcher lands; append a row per engine
  generation.

| Generation | Decision (block) | Decision (pass) | Heap | Hardware |
|---|---|---|---|---|
| v1 regex-per-rule (Step 17) | 16.5 ms | 18.1 ms | ~111 MB | sandbox, 2 vCPU, OpenJDK 11 |
| v2 combined matcher (Step 18) | **0.10 ms** | **0.07 ms** | ~21 MB | sandbox, 2 vCPU, OpenJDK 11 |

**Step 18 result:** block-path **~165×**, pass-path **~259×** faster than
v1; heap ~5× lower (only candidate rules' regexes compile); decision
outputs identical (equivalence suite + all 102 module tests green; the
benchmark confirmed the same 120/200 block and 200/200 pass outcomes as
v1). The ≥ 100× target is met with zero decision differences. The
decision cache (§4) is NOT warranted by these numbers — 0.07–0.10 ms is
within the §2 budget; cache stays deferred unless device measurement
(§5) says otherwise.

## 7. Honest boundaries

- No performance claim is made for the product until §5 runs on real
  hardware (B-001).
- Energy-saver features (§21) beyond "our patches must not defeat
  upstream resource management" are runtime behaviors of the built
  product; their detailed design is a separate later step, not quietly
  assumed here.
- The benchmark corpus is synthetic; real EasyList distribution may
  differ — the equivalence contract (§3.3) is what guarantees correctness,
  the numbers only guide performance.
