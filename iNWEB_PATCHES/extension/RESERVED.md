# Reserved patch ids: 0014–0017 (extensions)

**Reserved, unused, and deliberately not reused for anything else.**

`0014`, `0015`, `0016`, `0017` are held for the Chromium extension series
(MASTER-SPEC §16). The baseline cannot host them yet, so no patch files
exist in this directory and nothing here is wired into the build path.

## Why reserved rather than skipped

Renumbering around the gap would lose the association between these
numbers and the extension work they were created for. If the capability
returns, the series resumes with its identity and documentation intact
instead of being rediscovered from scratch — and the question "where are
the extension patches?" has a written answer here rather than a hole.

## Status

| Id | Intended content | Status |
|---|---|---|
| 0014 | build-time API audit + `enable_inweb_android_extensions` probe | **dormant / audit-only** — probe run 36159490822: gn accepted the arg, then failed on an upstream `!is_android` assert |
| 0015 | CRX3/ZIP inspector, manifest/version validation, native tests | **dormant / audit-only** — never merged |
| 0016 | closes Android extension UI/action/options surfaces by default | **dormant / audit-only** — never merged |
| 0017 | sideload/update policy gate (review required, MV2 warning) | **dormant / audit-only** — never merged |

"Reserved" here means **not in the main build path**. It does not mean the
files do not exist: a branch carries all four, unbuilt and unmerged, and
ADR-042's blanket wording ("reserved and unused") is under correction —
see `docs/EXTENSION-SERIES-FINDING.md`, "Correction".

## Conditions for un-reserving

See `docs/EXTENSION-DEVIATION.md` (ADR-042), "Re-evaluation path": an
upstream change to `enable_extensions` on Android, a supported Android
extension path in a future pin, or a different baseline. Anything else —
in particular a UI surface without a runtime — is explicitly prohibited.

## What is NOT blocked by this reservation

The Home/app-layer work, the build hops, and every other independent
track proceed normally. Nothing depends on these four numbers.
