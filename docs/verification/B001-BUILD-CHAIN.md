# B-001 — Step 50 Build-Chain Audit & Optimization

**Date:** 2026-09-22
**Trigger:** author directive — *stop duplicate long builds; audit every completed hop; separate verification from compilation; use legitimate revision-keyed caching.*
**Scope:** all 11 runs of `.github/workflows/b001-build-hop.yml` (runs 34706785899–35641788135), plus the workflow changes shipped in the same commit as this document.

---

## 1. Why every hop takes ~4.5 hours — by design, not by accident

A free GitHub-hosted `ubuntu-24.04` runner gives a job **6 h wall-clock, 4 vCPU, 16 GB RAM, ~14 GB free disk** (§ verified in run facts logs). The workflow is built to absorb the *maximum real compile* that one such job can hold:

| segment | minutes | note |
|---|---|---|
| facts + disk reclamation + 14 GiB swap | ~3 | documented runner tuning |
| fetch pinned Chromium (`fetch_chromium.sh`) | ~10–15 | irreducible on a cold runner (see §6) |
| tag verify + Android deps | ~2–8 | |
| cross-run state download + extract + verify | ~1.5–15 | grows as `out/` grows |
| mtime normalization to fixed epoch | ~1 | makes resume possible at all |
| **boxed build** | **265** | the *real compile* — inner `timeout 265m` |
| verdict + state package + upload | ~10–20 | tar.zstd, split ≤4 GiB parts, sha256 manifest |
| **total** | **≈280–325** | ≈4h40m–5h25m observed |

So **~95 % of a hop's wall-clock is genuine Chromium compilation**; per-hop overhead measured as low as **14.5 min** (hop 11: dispatch 18:57:52Z → build start 19:12:29Z). There is no duplicated dep-install, no matrix, no GN re-gen beyond the single `gn gen` the build script performs once per hop.

The chain exists because the *whole* build (~60k–78k edges depending on executor) cannot fit one 6 h job: it is **one build, chopped at the 6 h free-job ceiling, resumed across jobs via content-hashed state artifacts** — not repeated builds.

## 2. Hop ledger (all runs, evidence preserved)

| run id | hop | date (UTC) | wall | resume | executor | build evidence | net effect |
|---|---|---|---|---|---|---|---|
| 34706785899 | 1 | 09-12 | — | — | — | failed fast (pre-chain check) | none |
| 34706838336 | 2 | 09-12 | 4h42m | fresh | siso | `[29528/77670]` | **real** first compile ✓ |
| 34721208991 | 3 | 09-12 | 4h41m | state-2 | siso | `RESUME: FAIL — manifest missing` → recompiled `[28885/77978]` | **100 % wasted** (download bug) ✗ |
| 34734389881 | 4 | 09-13 | 4h41m | state-2 | siso | resume PASS, re-executed graph `[32290/76650]` | ~90 % wasted (siso restat) ✗ |
| 35378831028 | 5 | 09-18 | 4h44m | state-4 | siso | re-exec `[28719/77956]` | no frontier advance ✗ |
| 35404539669 | 6 | 09-18 | 4h41m | state-5 | siso | re-exec `[30519/77397]` | no frontier advance ✗ |
| 35420065165 | 7 | 09-19 | 4h41m | state-6 | siso | re-exec `[31045/72382]` | ~1.5k net advance ✗ |
| 35432678917 | 8 | 09-19 | 20m | state-7 | probe | dry-run plan only | legitimate cheap probe ✓ |
| 35434012516 | 9 | 09-19 | 4h40m | **fresh** | **ninja** | `[28452/59908]` | deliberate restart to switch executor — one-time cost of fixing §3.2 ✓ |
| 35447512278 | 10 | 09-19 | 4h41m | state-9 | ninja | plan `31456` = exact remainder; `6630` done (~25/min, blink CXX) | **real advance; resume math exact** ✓ |
| 35641788135 | 11 | 09-21 | 1h37m | state-10 | ninja | cancelled mid-box **by author order** (this audit) | ~1.3 h compile discarded; state-10 intact |
| 35652613459 | 12 | 09-21 | 4h46m | state-10 | ninja | plan `24826` = exact audit remainder; `12985` done (~49/min, content/browser region); state-12 = 2.6 GB / 37,523 objects | **real advance ✓** first hop on the audited workflow (4711311); depot_tools cache saved on first use |
| 35694777976 | 13 | 09-22 | 3h33m | state-12 | ninja | RESUME PASS; plan `11841` exact; stopped at `[10393/11841]` — first genuine build error: `AUTONINJA_BUILD_ID is not set` (build_server steps); state-13 = 3.4 GB | ~10,390 done ✓; **~1,451 edges remain**; fixed in build_android.sh (set AUTONINJA_BUILD_ID → verified local-execution fallback) |

**Totals:** ≈44 h of hosted-runner time over 12 runs; ≈24 h attributable to the defects/restart below; ≈20 h of genuine compile progress (hops 2, 9, 10, and 12).

## 3. Root causes of the waste — and their (already-landed) fixes

1. **Cross-run state download (hop 3, −4h41m).** `actions/download-artifact@v4` defaults to the *current* run's artifacts; resume needs `run-id` + `github-token` (`actions: read`). Fixed in 6005770. Hop 3's recompile was never resumed-from (hop 4 resumed state-2), so its entire box was discarded.
2. **siso restat state does not survive `out/` transfer (hops 4–7, ≈−14 h).** siso's incremental records are not mtime/size-restat based like plain ninja, so every hop re-executed ~30k edges regardless of transferred artifacts. Fixed by commit 4aa53f4 (normalize all tree mtimes to 2020-01-01, `out/` excluded) **plus** a015943 (`INWEB_BUILD_TOOL=ninja`, the mtime/size executor). Proof the fix works: hop 10's plan (31,456) was *exactly* hop 9's remainder (59,908 − 28,452) and it resumed 22,093 objects, adding 6,630.
3. **Executor switch forced a fresh restart (hop 9, −4h40m, necessary).** Switching siso→ninja invalidates incremental state; the alternative (continue in siso) meant infinite no-advance hops. One-time cost, paid once, deliberately.
4. **Static step timeouts.** `timeout-minutes:` cannot contain arithmetic — fixed by 3a1dfcc (static 340 m safety net around the inner precise `timeout 265m` box).
5. **Hop 11 cancellation (−1.3 h, author-ordered).** Not a defect: the audit directive arrived mid-box; cancellation preserved state-10 and stopped further spend pending this document.

## 4. Measured build rates (corrected — see erratum 0d96f17)

- Stage-1 B-3 (autoninja/siso, early graph): **267 edges/min** (18,104 edges in 1h07m44s). The 39.7/min figure was an arithmetic error — do not reuse.
- Hop 9 (ninja, early graph: ACTION/STAMP-heavy): **~107 edges/min** (28,452 in 265 m).
- Hop 10 (ninja, blink CXX region — the slowest part of any Chromium build): **~25 edges/min** (6,630 in 265 m).
- Hop 12 (ninja, content/browser CXX region): **~49 edges/min** (12,985 in 265 m).

**Remaining after state-13:** ~1,451 edges (11,841 − ~10,390 done; the two FAILED stamp edges rerun). Region: Java/ACTION + final links + packaging — expect one final short hop. **Hop-13 root cause (fixed):** plain-ninja chain hops lacked `AUTONINJA_BUILD_ID`, which Chromium's `android_static_analysis=build_server` steps hard-require; with the ID set and no `AUTONINJA_STDOUT_NAME`, `server_utils.MaybeRunCommand` falls back to normal local execution (verified against the pinned tag source). Fix: build_android.sh now exports it on the ninja path. Estimates must always quote the measured per-region rate, never a single blended number.

## 5. Optimizations shipped with this audit (this commit)

1. **`b001-verify.yml` — verification is now a separate, cheap job (no compile).** Repo-side changes (patch series, Chromium pin, fetch/patch tooling) trigger a ~35–60 min job: fetch → tag verify → *(optional)* pristine-tree hash vs the recorded B-2 digest → patch-series apply+verify → patched-tree digest → verdict. **A repo push can never again spend a 4.5 h compile box.** The compile chain is dispatch-only and resumes the single build. Modes: `patches` (default; the series is empty until the B-001 gate opens — then this becomes the Stage-2 patch gate), `pristine-hash`, `both`.
2. **`ninja-jobs` input on the hop workflow** (default 6, passed as `INWEB_NINJA_JOBS`). Tunable parallelism without a script edit. We deliberately keep `-j6` for the next hops: 4 vCPU/16 GB with heavy blink translations can exceed 16 GB at `-j8`; an OOM box would be exactly the waste this audit exists to prevent. Evidence first, speed second.
3. **`actions/cache` for depot_tools, keyed on the pinned revision** (`depot-tools-v1-73fc5a4d`, ~1.5 GB incl. CIPD bootstrap). Saves ~2–4 min per hop and per verify run. The pin in `scripts/fetch_chromium.sh` still enforces the checkout, so a restored cache cannot change what gets built.
4. **This ledger** — every hop's evidence stays in its run artifacts (`b001-hop-<n>-logs`, `b001-state-<n>-part-*` + `-manifest`); this document records the audit conclusions so they survive log rotation.

## 6. What is deliberately NOT cached (honesty about limits)

`actions/cache` caps at **10 GB per repository**. The Chromium tree is ~30 GB and a meaningful ccache for it is 20–50 GB — **both exceed the cap and are not faked**. The legitimate "cache" for compiled objects is the chain's own content-hashed state artifacts (sha256-manifested, verified on every resume). The tree itself must be re-fetched per hop (~10–15 min) — the cheaper of the two alternatives (artifact round-trip of an equal volume would cost the same or more and burn artifact quota).

## 7. Standing rules going forward

- Never dispatch a compile hop from a repo push; pushes get `b001-verify.yml` only.
- One chain in flight at a time (`concurrency: b001-build-hop`, no-cancel).
- Resume only from a state whose manifest verified (`RESUME: PASS` in `25-resume.log`).
- Rate estimates quote the measured per-region rate (§4).
- No PASS-by-inheritance: B-3/B-4 verdicts come only from the chain's own `.verdict`/log evidence (§57).

## 8. Next action

Dispatch hop 12: `resume-from=b001-state-10`, `resume-run-id=35447512278`, `build-tool=ninja`, `ninja-jobs=6`. Expected: ~25 edges/min in the blink region, state-11 packaged, APK not yet expected this hop.
