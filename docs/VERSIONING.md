# iNWEB Browser — Versioning Policy (§47)

**Status:** authoritative policy — Phase 12 design item 3 (Step 40).
Written **before the first build tag exists**, exactly as the design
order requires: no iNWEB version number, tag, or release artifact has
ever been created (blocker B-001 — no Chromium build exists yet). The
release registry in Appendix A is therefore **empty**, and that
emptiness is a fact, not a placeholder.

Governs: iNWEB semantic version numbers, Android `versionCode` /
`versionName` maintenance, git tag naming, and the channel mapping
(MASTER-SPEC §47). Does **not** govern: the pinned upstream Chromium
baseline (ADR-001 — see §6 below) or patch ids in
`iNWEB_PATCHES/MANIFEST.yaml` (patch series are identified by the
registry hash, not by product version).

---

## 1. Version scheme — semantic versioning

iNWEB uses [SemVer 2.0.0](https://semver.org/) `MAJOR.MINOR.PATCH`
with pre-release identifiers for channel builds. The first release is
`1.0.0-alpha.1` (the §47 example), progressing:

```text
1.0.0-alpha.1  →  1.0.0-alpha.2  →  …  →  1.0.0-beta.1
→  …  →  1.0.0-rc.1  →  …  →  1.0.0  →  1.0.1  →  1.1.0  →  2.0.0
```

**Pre-release ladder** (house precedence, consistent with SemVer
identifier comparison):

| Identifier | Rung | Meaning |
|---|---|---|
| `-alpha.N` | 1 | development-channel milestone build |
| `-beta.N` | 2 | beta-channel build for opt-in testers |
| `-rc.N` | 3 | release candidate — stable promotion validation |
| *(none)* | 4 | stable release |

**When each number bumps:**

- **MAJOR** — backward-incompatible change for users or their data:
  a persisted storage format break without an in-place forward
  migration (violating the ADR-033 corruption contracts / ADR-026
  backup forward-migration rule), or removal of a shipped feature.
- **MINOR** — new iNWEB feature work (phase features landing), any
  addition that is backward-compatible for user data.
- **PATCH** — fixes and artifact-changing refreshes with no new iNWEB
  features; **an upstream Chromium baseline refresh is at minimum a
  PATCH bump** (it changes the shipped artifact — see §6).

## 2. Channels (§47) and how versions map to them

| Channel | Trigger | Consumer | Version form |
|---|---|---|---|
| Development | every merge to `main` | internal | untagged (see below) |
| Beta | tagged `v*-beta.*` | opt-in testers | `x.y.z-beta.N` |
| Stable | promoted after §46 release validation | public | `x.y.z` |

- **Development builds are never tagged.** A development build's full
  identity is the *upcoming* version plus the build metadata stamp
  (BUILD-INFRASTRUCTURE §4: upstream tag + iNWEB patch-set hash +
  container digest + commit SHA). Alpha tags (`v*-alpha.N`) are
  milestone builds of the development line that are worth distributing.
- **Canary / Experimental:** §47 says "where appropriate". iNWEB runs
  **no separate canary channel in v1** — the development channel fills
  that role. If a canary line is introduced later it gets a `-canary.N`
  identifier between `alpha` and `beta` in the ladder, and the change
  is recorded as a new ADR — channel semantics never drift silently.
- **Stable promotion:** an `-rc.N` build that passes the full §46
  pipeline (build → unit → integration → security → performance/size →
  APK/AAB artifact → release validation) is re-tagged as the bare
  `x.y.z` stable. Nothing is called stable without passing that gate.

## 3. Android `versionCode` / `versionName` maintenance

- **`versionName` carries the iNWEB semver string EXACTLY** —
  `1.0.0-alpha.1`, `1.0.0-beta.1`, `1.0.0`. No decoration, no commit
  ids (those live in About / release notes / build metadata).
- **`versionCode` is a strictly monotonically increasing integer.**
  Android requires that no release track ever see a lower code than
  one it already shipped; codes are never reused, never decreased.

**Derivation (the default code for a tag):**

```text
versionCode = MAJOR × 1,000,000 + MINOR × 10,000 + PATCH × 100 + channelRank
channelRank: alpha.N → N (1–99) · beta.N → 100 + N · rc.N → 200 + N · stable → 300
```

Worked examples: `1.0.0-alpha.1` → 1,000,001; `1.0.0-beta.3` →
1,000,103; `1.0.0` → 1,000,300; `1.1.0` → 1,010,300.

**Overflow rule.** The derivation alone cannot stay monotonic when a
newer line starts after an older stable — e.g. `1.0.1-alpha.1` derives
1,000,101, which is LOWER than `1.0.0`'s 1,000,300. In that case the
assigned code is **the highest previously assigned code + 1**, and the
deviation is recorded in the registry (Appendix A). The derivation
also assumes `PATCH ≤ 99` and `N ≤ 99` per pre-release rung; beyond
those limits the overflow rule governs. **The registry, not the
formula, is the source of truth** — every code is assigned exactly
once, at tag time, and written down.

## 4. Tag naming and the release record (§45)

- Tags are **annotated** tags named `v` + the exact semver string:
  `v1.0.0-alpha.1`, `v1.0.0-beta.1`, `v1.0.0`. The pre-release
  identifier IS the channel marker (`v*-beta.*` per
  BUILD-INFRASTRUCTURE §5); a stable tag never carries a suffix.
- No other tag namespace marks releases. Upstream Chromium tags are
  upstream's, never iNWEB's (ADR-001 overlay); build-infrastructure
  tags, if any, use a `ci/` or `build/` prefix and are never releases.
- **Each tag's annotation (and release notes) must contain the full
  release record:** iNWEB semver, assigned `versionCode`, date,
  upstream Chromium baseline tag, iNWEB patch-set hash
  (`iNWEB_PATCHES/MANIFEST.yaml` state), build container digest, and
  the changelog summary. This is the §45 reproducible-build record;
  the build CI invariant (`pristine@tag + ordered patch series`) is
  what the patch-set hash refers to.
- `CHANGELOG.md` is created with the first tagged release. It does not
  exist today because nothing has been released — an empty placeholder
  would be a false claim of release history (§57).

## 5. What a version bump must never do

- Never ship a data format that a prior stable cannot either read or
  honestly recover (ADR-033 corruption contracts; ADR-026
  forward-migrating backups). If unavoidable → MAJOR + documented
  migration.
- Never change `versionName` semantics or `versionCode` assignment
  rules without an ADR — this document is the contract both CI and
  release tooling will enforce once build infrastructure exists
  (B-001).
- Never promote to stable without the §46 release-validation gate
  passing on the exact artifact being promoted.

## 6. Relationship to the pinned Chromium baseline (ADR-001)

The iNWEB product version and the upstream Chromium baseline are
**independent axes**:

- The baseline (`154.0.8037.21` today) is pinned in `PROJECT_STATE.md`
  and travels as **build metadata**: it is stamped into the build,
  shown in About, and named in every release record.
- A baseline refresh (security update) changes the shipped artifact,
  so it is **at least a PATCH bump** of the iNWEB version — even
  though upstream's own numbering (154.0.8037 → 154.0.8040) is not
  part of `versionName`.
- iNWEB feature work riding on a refresh makes it a MINOR bump as
  usual; the baseline change itself never inflates the bump.

## 7. Honest boundaries

- **Nothing here has run.** No tag exists, no `versionCode` has been
  assigned, no artifact has been built (B-001). This policy exists so
  that the **first** tag is created correctly, with nothing to migrate
  and nothing to renumber later.
- Mechanical enforcement (e.g. a gate that checks tag ↔ registry ↔
  `versionName` consistency) is authored together with the release
  pipeline on the build infrastructure — until then the registry below
  is the human-auditable record.

---

## Appendix A — Release registry (authoritative `versionCode` record)

Every tagged release appends exactly one row, at tag time. The table
starts empty.

| Tag | versionCode | Date | Chromium baseline | Patch-set hash | Container digest | Channel |
|---|---|---|---|---|---|---|
| *(none — no release exists; B-001)* | | | | | | |
