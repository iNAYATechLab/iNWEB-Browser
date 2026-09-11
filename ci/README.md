# CI — Build Infrastructure

## Components

| Component | Purpose | Status |
|---|---|---|
| `Dockerfile` | Chromium build container (Ubuntu 24.04 x86-64, pinned depot_tools) | Authored (Phase 1); image build pending build-host provisioning (B-001) |
| `.github/workflows/ci-authoring.yml` | Live authoring CI on GitHub-hosted runners: unit tests + registry validation | **Live** |
| `.github/workflows/upstream-watch.yml` | Weekly baseline-drift watch against upstream Android stable | **Live** (scheduled + manual) |
| Full build pipeline (`.github/workflows/ci-chromium-build.yml`) | gclient sync → patches → build → test → artifacts on a self-hosted runner | Authored when the runner exists (B-001) |

## Build host setup (when provisioned)

1. Provision a machine per the specification in `docs/BUILD-INFRASTRUCTURE.md` §2
   (Ubuntu 22.04/24.04 x86-64, ≥16 cores, ≥64 GB RAM, ≥300 GB SSD, Docker).
2. Build the container image:
   `docker build -t inweb-build ci/`
3. Register a GitHub Actions self-hosted runner for this repository with the label
   `inweb-build` (Settings → Actions → Runners → New self-hosted runner).
4. Full-build workflows then execute entirely inside the container.

## Reproducibility invariant

Every build asserts: working tree ≡ `pristine@tag + ordered patch series`, verified
via `scripts/apply_patches.py` (apply → verify → hash) and recorded in the build
metadata stamp produced by `scripts/build_android.sh`.
