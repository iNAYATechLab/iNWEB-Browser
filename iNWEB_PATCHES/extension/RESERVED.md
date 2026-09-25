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
| 0014 | extension installer / manifest binding | reserved — no runtime on Android (ADR-042) |
| 0015 | registry persistence binding | reserved — ADR-042 |
| 0016 | management surface binding | reserved — ADR-042 |
| 0017 | runtime binding | reserved — ADR-042 |

## Conditions for un-reserving

See `docs/EXTENSION-DEVIATION.md` (ADR-042), "Re-evaluation path": an
upstream change to `enable_extensions` on Android, a supported Android
extension path in a future pin, or a different baseline. Anything else —
in particular a UI surface without a runtime — is explicitly prohibited.

## What is NOT blocked by this reservation

The Home/app-layer work, the build hops, and every other independent
track proceed normally. Nothing depends on these four numbers.
