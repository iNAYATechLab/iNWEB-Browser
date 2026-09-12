# B-001 Stage 1 — hosted-runner execution record (run 34691154428)

**Date:** 2026-09-12 · **Commit built:** `ae247a9916e94c0496007d684c171b330e50f929`
**Execution path:** GitHub Actions exclusively (user directive — no local
machine, no local commands). Workflow: `.github/workflows/b001-stage1.yml`
(dispatch-only), jobs `audit` + `attempt`, both on `ubuntu-24.04`
GitHub-hosted runners (image `20260907.300.1`).
**Honest scope:** standard hosted runners are BELOW the BUILD-INFRASTRUCTURE
§2 build-host spec; larger runners are an organization-plan feature and are
NOT available to this user-account public repository (verified via the
GitHub API — `owner.type: User`). Every verdict below is backed by the
workflow's uploaded artifacts (`b001-stage1-audit`, `b001-stage1-attempt`).
Nothing is simulated; no WebView fallback exists (§71); no success is
inferred from silence (§57).

## Run history (the fix loop — 5 dispatched runs)

| Run | Outcome | Fix committed back |
|---|---|---|
| 34686416938 | fetch died immediately: a fresh depot_tools cannot run `fetch` (`python3_bin_reldir.txt not found`); checker misread GCS HTTP 400 as unreachable | `decbc65` — pin-preserving `ensure_bootstrap` + `DEPOT_TOOLS_UPDATE=0`; sync directly at the pinned tag (no double ToT pass); checker: an HTTP status response = reachable (+1 test) |
| 34686746853 | fetch PASSED (~15 min, 73 GiB still free) but tag check used `git describe` — `--no-history` syncs carry no tag refs | `21b571d` — authoritative proof: remote `refs/tags/<tag>` (peeled) vs local HEAD + `chrome/VERSION` cross-check |
| 34688494505 | tag DATA complete (HEAD == remote tag) but GitHub's default `bash -e` killed the step on an empty grep pipeline; `apply_patches.py hash` crashed on an NDK dangling symlink | `33be06e` — `|| true` on legitimately-empty pipelines; symlink-aware tree hash (links contribute `link:<target>`, +2 tests) |
| 34689824373 | full chain to `gn gen` — Chromium rejected the draft development args (`assert(!(current_os == "android" && is_component_build)`) | `ae247a9` — `is_component_build` removed from `config/chromium/args-development.gn` (the file's own header mandates exactly this correction) |
| **34691154428** | **full chain executed; boxed build produced the resource measurement** | (none needed) |

## Audit — hosted runner vs BUILD-INFRASTRUCTURE §2 (evidence)

Command: `python3 scripts/check_build_host.py` (job `audit`).
Measured: `ubuntu-24.04`, **4 vCPU**, **15.6 GiB RAM**, **86.1 GiB free** at
the workspace volume (145 GB disk), git/python3/curl present, all four
network endpoints reachable (GCS endpoints answer bare HEAD probes with
HTTP 400 — reachable by the corrected semantics). Checker verdict:
**FAIL** — cpu 4 < 16 minimum; memory 15.6 < 64; disk 86.1 < 300.
This is the honest hardware gap: the runner is NOT a §2 build host, and
everything below is a best-effort attempt on it, never a spec-compliant
build.

## Row evidence (commands, outputs, verdicts)

### B-1 — `apply_patches.py apply && verify` on the pristine pinned tag — **PASS**

- Command: `python3 scripts/apply_patches.py apply <src>` then `… verify <src>`
  (registry `patches: []`, per the Stage-1 expectation).
- Output: `series OK (0 already applied, 0 applied now)` ·
  `verify OK: full series applied` · `git status --porcelain` empty.
- Meaning: the empty series converges on the pristine tag tree; the
  registry tooling runs against the real Chromium checkout.

### B-2 — working tree ≡ `pristine@tag + ordered series` (hash invariant) — **PASS**

- Command: `python3 scripts/apply_patches.py hash <src>` (3m39s walk).
- Output: **`d4212cfb45d955aa5311c5ab35439b896f8f54ae3b616d546433f38005cabe1e`**
  — recorded as the pristine-tree invariant for the empty series at
  `154.0.8037.21`; any future patch-series state must reproduce its own
  registered hash deterministically.
- Supporting: local HEAD `0fa6d91e2faf313da8332688a96bd98b9d983a4d` ==
  remote `refs/tags/154.0.8037.21` (`git ls-remote`, peeled) ==
  `chrome/VERSION` `154.0.8037.21`; tree clean.

### B-3 — GN + ninja build — **BLOCKED on hosted-runner resources** (pipeline itself proven)

- Commands: `scripts/fetch_chromium.sh` (PASS, ~15 min, 73 GiB free after) →
  `sudo bash src/build/install-build-deps.sh --android --no-arm` (PASS) →
  `scripts/build_android.sh development` (75-minute evidence box).
- Output: `gn gen out/inweb-development` succeeded with the corrected
  committed args; `autoninja chrome_public_apk` compiled
  **18,104 / 81,578 targets (22.2%) in 1h07m44s** before the box ended;
  out dir 6.6 GB; 65 GiB still free (disk is NOT the limit here).
- Limitation (measured): ≈ 39.7 targets/min on 4 vCPU ⇒ remaining
  ~63,474 targets ≈ **~27 hours** of compile, versus the **6-hour maximum**
  for a GitHub-hosted job. Larger hosted runners (16–64 vCPU) would fit
  this comfortably but require a paid GitHub organization plan, which this
  user-account repository does not have. No engine downgrade, no
  component-build trick (Android forbids it), no WebView (§71) was used to
  force a pass.
- Note: the full `inweb_public_apk` form of B-3 additionally requires the
  ui/ patch series (Stage 2); this run targeted upstream `chrome_public_apk`
  as the registry is empty.

### B-4 — install + launch on arm64 device/emulator — **BLOCKED**

- No APK exists (see B-3), and no arm64 emulator was provisioned in this
  run. Marked BLOCKED with this record rather than pretended.

### APK artifact — **NONE (honest record)**

- `find out -name '*.apk'` → none. The workflow uploads the APK as an
  artifact the moment one exists; today there is nothing to upload.

## Defects found and fixed during the loop (all committed back)

1. `fetch_chromium.sh`: depot_tools bootstrap + single tag sync (`decbc65`)
2. `check_build_host.py`: network-reachability semantics (`decbc65`)
3. `apply_patches.py`: symlink-aware deterministic hash (`33be06e`)
4. `config/chromium/args-development.gn`: `is_component_build` removed (`ae247a9`)

Python unit tests: 77 → 80 (all green locally and in CI run 34689818244).

## Conclusion

Stage 1 is **genuinely verified as far as free GitHub-hosted runners
legitimately allow**: pinned fetch, exact-tag proof, dependency
provisioning, empty-series apply/verify, and the pristine hash invariant
are all PASS with uploaded evidence; the full compile is BLOCKED by a
measured, external resource limit (4 vCPU × 6 h job cap), not by any
repository defect. Completing `chrome_public_apk` requires either chained
resumable jobs on free hosted runners (proposed Step 50), a paid
larger-runner organization plan, or funded cloud build capacity.
