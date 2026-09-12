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
