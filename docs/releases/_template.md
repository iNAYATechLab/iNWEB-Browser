# Release vX.Y.Z-<channel> — <one-line summary>

> Copy this file to `vX.Y.Z.md` and fill every section. Delete nothing;
> write "none" where a section genuinely does not apply. Honesty over
> marketing — every claim must cite its evidence (§57).

| | |
|---|---|
| Version | `X.Y.Z-<prerelease>` (channel, §47 ladder) |
| Date | YYYY-MM-DD |
| Tag | `vX.Y.Z-<prerelease>` (annotated, §45 release record in the tag message) |
| Artifact | `<name>.apk` (size) |
| sha256 | `<hash>` (runner == artifact == release asset, bit-for-bit) |
| Chromium baseline | `<tag>` (must equal `config/chromium/BASELINE`) |
| Source state | commit `<sha>` — patch series `<first>–<last>` applied (b001-verify run `<id>`: `PATCHES: PASS`) |

## What's in this release

- Feature/patch highlights with their patch ids and design-doc references.

## What this is NOT (honest boundaries)

- Explicitly list known absences, deferred work, and non-goals.

## Verification evidence

- b001-verify run id + verdict line, build-hop run id, B-matrix rows
  satisfied (B-1…B-4 automatic; device rows cite the §48 matrix log).

## Known issues

- None recorded / listed issues with severity + workaround.

## Install / upgrade notes

- Fresh install vs in-place update (package id / signing key continuity,
  user-data policy), minimum Android version, target ABIs.

## Next

- The next planned release and its scope.
