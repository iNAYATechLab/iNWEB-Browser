# iNWEB Browser — B-001 Build-Host Runbook

**Status:** the operational walkthrough for the first real Chromium
compile (Step 45). Everything below runs on the **user-provisioned
build host** — nothing here has been executed yet (blocker B-001,
§57): the authoring sandbox cannot compile Chromium by design
(ADR-003). This runbook sequences the ALREADY-AUTHORED tooling; when
the first run surfaces defects, fixes are committed back to the
scripts — that is the process working, not failing.

Spec authority: `docs/BUILD-INFRASTRUCTURE.md` (§2 hardware, §3
pipeline, §4 reproducibility). Where this runbook and that document
disagree, the spec wins and this runbook is corrected (§59).

---

## 1. Provision the host (BUILD-INFRASTRUCTURE §2)

| Property | Minimum | Recommended |
|---|---|---|
| OS | Ubuntu 22.04 LTS, x86-64 | Ubuntu 24.04 LTS |
| CPU | 16 cores | 32 cores |
| RAM | 64 GB | 128 GB |
| Disk | 300 GB SSD | 500 GB NVMe |
| Network | outbound HTTPS: `chromium.googlesource.com`, `storage.googleapis.com`, `commondatastorage.googleapis.com`, `github.com` | same |

A dedicated cloud VM (e.g. 32 vCPU / 128 GB / 500 GB NVMe) or bare
metal both work; the scripts do not care. Nothing else is installed by
hand — the toolchain comes from the pinned depot_tools revision and
the repository.

## 2. Pre-flight check (minutes)

```bash
git clone https://github.com/iNAYATechLab/iNWEB-Browser.git
cd iNWEB-Browser
python3 scripts/check_build_host.py
```

Every row cites the spec it verifies. **Fix every FAIL before
continuing** — the fetch takes hours and the check exists so that time
is never wasted on an under-provisioned host. `--skip-network` skips
the reachability probes (e.g. behind a proxy that blocks HEAD).

## 3. Fetch the pinned source (the long step)

```bash
scripts/fetch_chromium.sh
```

What it does (authored, first execution validates it):

1. reads the pinned tag from `config/chromium/BASELINE`
   (currently `154.0.8037.21`),
2. clones depot_tools at the pinned revision,
3. `fetch --no-history chromium` + `target_os = ['android']`,
4. `gclient sync --revision src@<tag> --with_branch_heads -D`,
5. prints the checked-out HEAD for the record.

Expect a multi-hour download and ~100+ GB. The sync is resumable —
re-run the script on failure.

## 4. First build (development channel)

```bash
scripts/build_android.sh development
```

Honest scoping (stated in the script itself): until the Phase 2 UI
patches introduce the custom `inweb_public_apk` target, this builds
the **upstream `chrome_public_apk`** at the committed GN args — which
is exactly what proving the pipeline end-to-end requires. The script
first applies and verifies the patch series (currently empty =
pristine upstream), then `gn gen` + `autoninja`, and writes the
reproducibility stamp `out/inweb-development/iweb_build_metadata.txt`
(upstream tag, HEAD, channel, GN args file, tree hash, timestamp —
the §45 record).

## 5. Record Stage-0 results (device matrix)

The first successful run closes matrix rows **B-1..B-3** (apply+verify,
hash invariant, GN build) and provides the artifact for **B-4**
(install + launch on an arm64 device/emulator). Append the run to the
verification log in `docs/DEVICE-VERIFICATION-MATRIX.md` Appendix B —
date, build id (no versionCode exists until the first tag per
VERSIONING.md — use the commit SHA), hardware, rows run, defects
filed. A failed row is a filed defect, never a silent skip.

## 6. Optional: self-hosted runner (preferred CI form)

To let GitHub Actions drive the same scripts: repository → Settings →
Actions → Runners → New self-hosted runner; apply the label used by
the build workflow; run it as a service on the host. Security notes:
never enable pulls from fork repositories on self-hosted runners, and
keep signing material in runner secrets only (BUILD-INFRASTRUCTURE §6).
Direct script builds (§4) need no runner at all.

## 7. What comes back to the repository

- the verification-log row (§5) and, if produced, the build metadata
  stamp values,
- every defect found, filed against the matrix row that caught it,
- fixes to the scripts/configs as ordinary reviewed commits — the
  scripts carry "first execution validates this" on their faces.

## 8. Honest boundaries

- **No build result exists or is claimed today.** This runbook is
  authored process, not evidence.
- Hardware realities: a 16-core/64 GB host is the floor — expect long
  link times; the recommended band exists for a reason. The
  performance budgets (docs/PERFORMANCE-BUDGETS.md) are verified
  against the VANILLA same-tag build produced here, which is also how
  the P-6 APK-size reference gets its first real number.
- The pinned baseline may have drifted by the time the host is ready:
  `python3 scripts/check_baseline.py` reports it, and a rebase follows
  docs/PHASE0-CHROMIUM-BASELINE.md before the first build.
