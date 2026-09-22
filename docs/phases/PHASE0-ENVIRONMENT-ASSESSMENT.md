# Phase 0 — Environment Assessment & Chromium Build Feasibility

**Date:** 2026-09-12
**Scope:** Master Specification §67 (First Action), §53 (Phase 0), §56 (Blocker Protocol).
**Verdict:** The authoring sandbox **cannot compile Chromium**. All non-build work
proceeds. **No WebView fallback is used or permitted** (§2, §71).

---

## 1. Method

- Read and analyzed the governing master specification (`../MASTER-SPEC.md`).
- Inspected the project repository: pre-existing, empty, single branch `main`.
- Measured the authoring sandbox (CPU, RAM, disk, tooling, session model).
- Verified upstream Chromium-for-Android build requirements from the official
  `chromium.googlesource.com` documentation.
- Verified network reachability to Chromium source and binary endpoints.
- Queried the Chrome Version History API for the current Android stable release.

## 2. Measured authoring sandbox

| Property | Measured value |
|---|---|
| CPU | 2 × x86-64 cores |
| RAM | 1.9 GiB total |
| Free disk | 20 GiB (root filesystem) |
| Persisted workspace | `/home/user` only; snapshot capped ≈128 MB / 10 000 files |
| Session model | Ephemeral — installed system packages and running processes do **not** survive between sessions; only `/home/user` persists |
| Command ceiling | 30 minutes per command execution |
| Network | Outbound HTTPS verified: `chromium.googlesource.com` (HTTP 200), `storage.googleapis.com`, `commondatastorage.googleapis.com` reachable; GitHub API + git push working |
| Tooling present | git 2.47.3, Python 3.13.14, Node 20.20.2, OpenJDK 11, curl, jq; GitHub CLI 2.100.0 (reinstallable each session; credentials persist) |

## 3. Upstream requirements — building Chromium for Android

From the official upstream documentation (`docs/android_build_instructions.md`):

| Requirement | Upstream value |
|---|---|
| Host | x86-64 machine running Linux (Ubuntu recommended); Windows/macOS **not supported** for Android builds |
| RAM | At least 8 GB; **more than 16 GB highly recommended** |
| Disk | At least **100 GB** of free space |
| Toolchain | depot_tools, gclient, GN, Siso/Ninja; checkout alone is tens of GB |
| Build time | Hours to tens of hours depending on core count |

## 4. Gap analysis

| Requirement | Sandbox | Verdict |
|---|---|---|
| RAM ≥ 8 GB (16 GB+ recommended) | 1.9 GB | **FAIL** — ~4× below absolute minimum |
| Disk ≥ 100 GB | 20 GB free | **FAIL** — 5× below minimum; persisted workspace ≈128 MB makes a durable checkout impossible regardless |
| Practical build CPU | 2 cores | **FAIL** — far below the practical class for multi-hour C++ links |
| Build window | 30 min/command; ephemeral sessions | **FAIL** — Android Chromium links exceed this on machines many times larger |
| Network to Chromium sources | Verified working | PASS |
| Authoring: docs, patches, app source, CI, tests | Sufficient | PASS |

## 5. Blocker record — B-001 (Rule 56 format)

**Exact blocker.** No Chromium build (gclient checkout + GN/Ninja compile + APK link) can
be executed in the authoring sandbox. Consequently, no APK/AAB artifact can be produced
there, and no binary-level validation can run there.

**Why it exists.** The sandbox is an ephemeral, resource-capped authoring environment
(§2 above) — 1.9 GB RAM vs. an 8 GB minimum (16 GB+ recommended), 20 GB disk vs. 100 GB
required, 2 cores, a 30-minute command ceiling, and no persistence of installed tools or
processes between sessions. These are platform limits, not project decisions.

**What has been attempted / verified.**

1. Measured actual CPU/RAM/disk (values above).
2. Verified the official upstream Android build requirements against them.
3. Confirmed network access to `chromium.googlesource.com` and Google storage endpoints
   (so source *reference* and tooling authoring work fine).
4. Confirmed there is no lighter legitimate path that preserves the mandated
   architecture: pre-built Chromium "content shells" or system WebView would violate
   §2/§71 (no WebView, no silent substitution) and are rejected.

**Information / action actually required.** Provision one **build host** — either:

- a **self-hosted GitHub Actions runner**, or
- an equivalent cloud VM,

matching the specification in `../BUILD-INFRASTRUCTURE.md` §2 (minimum 16 cores /
64 GB RAM / 300 GB SSD, Ubuntu 22.04/24.04 x86-64, Docker, network to googlesource and
Google storage). This is a user-provisioned external resource. Until it exists, the
project authoring continues at full speed; only compile/link/emit-artifact steps wait.

**What continues without the blocker.**

- Complete patch framework (registry, application, verification tooling).
- All iNWEB application source authoring (Kotlin / Android / Material 3).
- CI/CD pipeline definitions and the build container definition.
- Documentation, licensing, threat modeling, test design.
- Lightweight per-session validations (script lint, YAML validation, unit-testable pure
  modules). Gradle-based Android module compilation will be attempted per-session in
  Phase 2 and honestly reported (SDK + Gradle footprint ≈2 GB; RAM-constrained; marked
  *to be validated*).

## 6. Secondary constraints and mitigations

| Constraint | Mitigation |
|---|---|
| Persisted workspace ≈128 MB | Chromium tree never enters the repo (ADR-004); repo carries patches/scripts/source only |
| Ephemeral tooling | A per-session bootstrap script re-installs the toolchain (authored in Phase 1) |
| 30-minute command ceiling | Long operations are structured as resumable scripts executed on the build host, not in the sandbox |

## 7. Conclusion

- Architecture: **feasible as specified** — a real Chromium-derived Android browser.
- Development model: split authoring (sandbox) / build (external host) — ADR-003.
- Phase 0 exits with **zero architectural compromise**: no WebView fallback, no fake
  claims, blocker honestly recorded and tracked as B-001.
