# iNWEB Browser — Build Infrastructure & CI/CD

**Status:** Phase 0 design (§46). Workflow definitions, container image, and GN configs
are authored in Phase 1. Full build stages remain **non-executable** until blocker B-001
is resolved (build host provisioned).

---

## 1. Two-environment model (ADR-003)

| Environment | Role | Capability |
|---|---|---|
| Authoring sandbox (ephemeral) | Specifications, patches, application source, tests, CI definitions, lightweight validation | **No Chromium compilation** (B-001) |
| Build host (user-provisioned) | `gclient sync`, patch application, GN/Siso build, APK/AAB emission, signing | Full Chromium builds |

## 2. Build host specification (minimum / recommended)

| Property | Minimum | Recommended |
|---|---|---|
| OS | Ubuntu 22.04 LTS, x86-64 | Ubuntu 24.04 LTS, x86-64 |
| CPU | 16 cores | 32 cores |
| RAM | 64 GB | 128 GB |
| Disk | 300 GB SSD | 500 GB NVMe |
| Software | Docker; GitHub Actions self-hosted runner | + remote execution (RBE) evaluation (Phase 5) |
| Network | `chromium.googlesource.com`, `storage.googleapis.com`, `commondatastorage.googleapis.com`, GitHub — outbound HTTPS | same |

Provisioning form: a self-hosted GitHub Actions runner (preferred — integrates directly
with the pipelines below) or an equivalent cloud VM driven by the same scripts.

## 3. Pipeline (implements §46)

```text
SOURCE (push to main / tag)
↓
DEPENDENCY VALIDATION      gclient sync at pinned tag; patch series applies cleanly
↓                          on a pristine checkout; tree hash matches registry
BUILD                      committed GN args; autoninja inweb_public_apk
↓
UNIT TEST                  iNWEB module suites + selected upstream unit suites
↓
INTEGRATION TEST           instrumented Android tests (emulator / device farm)
↓
SECURITY CHECK             patch-security gates, secret scan, license check
↓
PERFORMANCE / SIZE CHECK   APK size budget; startup/memory benchmarks vs. budget
↓
APK/AAB ARTIFACT           signed APK (development channel) / AAB (release);
                           SHA-256 manifest; ProGuard mappings; symbol files
↓
RELEASE VALIDATION         channel-promotion checks, smoke suite, rollback plan
```

**Size budget gate (§7):** arm64 base install budget is set when the first real build
exists; regressions beyond budget block promotion. Baseline is measured, never assumed.

## 4. Reproducibility (§45)

- Chromium revision: pinned tag + `DEPS` via `gclient sync --revision src@<tag> -D`.
- Build container: image digest pinned; `Dockerfile` committed in `ci/` (Phase 1).
- GN args: committed per channel under `config/chromium/`.
- Build metadata stamp: upstream tag + iNWEB patch-set hash + container digest.
- CI invariant: working tree ≡ `pristine@tag + ordered patch series`, verified by
  hashing before every build.

## 5. Channels & versioning (§47)

| Channel | Trigger | Consumer |
|---|---|---|
| Development | every merge to `main` | internal |
| Beta | tagged `v*-beta.*` | opt-in testers |
| Stable | promoted after release validation | public |

Semantic versioning from `1.0.0-alpha.1`; Android `versionCode` strictly monotonic;
`versionName` carries iNWEB semver; the Chromium baseline tag is recorded in About and
release notes.

## 6. Signing

- Development: ephemeral debug keys generated on the build host (never committed).
- Release: keystore supplied via encrypted runner secrets or Play App Signing; rotation
  procedure documented at Phase 12. No signing material ever enters the repository.

## 7. Current status (honest, §57)

- Designed in Phase 0: this document.
- Authored in Phase 1: GitHub Actions workflows, `ci/Dockerfile`, GN arg files,
  `scripts/` orchestration.
- Authored in Step 45: the build-host pre-flight checker
  (`scripts/check_build_host.py`, 22 unit tests — verifies every §2 row
  BEFORE the multi-hour fetch) and the operational walkthrough
  ([`docs/BUILD-HOST-RUNBOOK.md`](BUILD-HOST-RUNBOOK.md)).
- Executed in Step 49 (user directive — GitHub-Actions-only path, no
  local machine): `.github/workflows/b001-stage1.yml` (dispatch-only).
  On free hosted `ubuntu-24.04` runners: pinned fetch, exact-tag
  proof, Android deps, and empty-series apply/verify + pristine hash
  all PASS with uploaded evidence; the compile itself is measured
  infeasible on 4 vCPU within the 6-hour hosted-job cap
  (18,104/81,578 edges in 1h07m44s — 267 edges/min, ≈ 4.5–5.5 h remaining, still over a single 6-h hosted job with setup) — see
  `docs/verification/B001-STAGE1-RUN5.md`. Larger hosted runners
  (16–64 vCPU) satisfy §2 but require a paid organization plan,
  unavailable to this User-account repository; the §2 spec stands
  unchanged for full builds.
- Still blocked until B-001 resolution: the first complete BUILD
  artifact. **No APK exists or is claimed.**

## Hop 33 — the two fixes, measured at runtime

Hop 33 is the first hop that failed out loud, and that is the useful
part: it proves the integrity fixes are reachable rather than merely
written.

**mtime discipline (runtime evidence).** `26-normalize.log`:

```
normalized: all tree mtimes -> 2020-01-01 (out/ excluded)
--- sanity: sources must all be <= 2020-01-01 ---   (empty: nothing newer)
--- sanity: out artifacts (sample) are newer ---    chromium/src/out/.../toolchain.ninja
```

Both sanity checks pass, so the resumed `out/` artifacts are newer than
every source input — which is the condition that makes siso/ninja resume
instead of re-execute.

**Failure truthfulness (runtime evidence).** `gn gen` failed, and the hop
reported it instead of packaging state:

```
30-build.log:  FATAL: gn gen failed (see the error above)   build_exit=4
45-verdict.txt: BUILD: FAILED — the build exited non-zero (see 30-build.log).
                No resumable state is published: resuming from a stale out/
                would hide the failure instead of fixing it.
run conclusion: failure
```

Hops 28 and 31 produced the same class of error and both concluded
success. The chain that failed before — exit code captured only when the
step survived, `build_status` written before the verdict read it, and the
verdict running at all — now works end to end.

**The GN rule (learned twice).** `glob()` is rejected in this file, at
file scope as well as inside a template invocation block; both hop 31 and
hop 33 died on it with "Unknown function". The other iNWEB targets
(`chrome/android/inweb/{adblock,popup}/BUILD.gn`) never use it — they list
`sources` explicitly — so patch 0013 does the same. Explicit lists also
make the patch diff show exactly which files enter the build.

## hop-36 — run 36162922524 @ ab7fc2a2 (completed failure) — R fix CONFIRMED

Step 13 "Boxed build" reported success, step 14 (evidence/APK check) failed;
`45-verdict.txt` is authoritative: `BUILD: FAILED`, `build_exit=1`, no state
published. (Confirms the standing trap: `continue-on-error` on the build step
masks the outcome; the verdict step is the only trustworthy signal.)

**The `resources_package` fix worked.** compile_kt now receives the generated
R srcjar:

    --java-srcjars=[\"gen/.../inweb_app_java__assetres.srcjar\"]

and `unresolved reference 'R'` — the hop-34 killer — is **gone**. Progress
moved from `[30/1367]` (hop-34) to `[33/1367]`, and `SOLINK ./libchrome.so`
completed cleanly at `[20/1367]` with no OOM at `ninja-jobs=6`.

Two NEW, independent blockers (both fixed in commit 597e52d3):

1. **compile_kt failed on a warning, not an error.** `compile_kt.py` runs with
   `--warnings-as-errors`, so a deprecation is fatal:
   `HomeGlyph.kt:77,78 warning: quadraticBezierTo is deprecated`.
   Fix: `quadraticTo()`. This is also a latent drawing bug — `quadraticBezierTo`
   is RELATIVE and pushed the shield control points to (1.49w, 1.44h), far off
   canvas; `quadraticTo` is ABSOLUTE and restores the intended shape.

2. **Resource value conflict.** `error: resource 'string/app_name' has a
   conflicting value for configuration ()` — ours vs Chromium's
   `chrome_base_module_resources`/`values/channel_constants.xml:10`, then
   `error: failed to merge resource table` / `failed parsing input`.
   Fix: rename ours to `inweb_app_name`.

Residual risk recorded honestly: aapt2 aborts on the FIRST conflict, so further
our-resource-vs-Chromium collisions (e.g. `action_back`, `theme_light`,
`omnibox_hint`) may surface after this one is cleared. We have no local
Chromium tree to pre-compute the full collision set.
