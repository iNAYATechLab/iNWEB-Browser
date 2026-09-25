# ADR-039 (DRAFT) — Prayer Times

**Status: DRAFT — awaiting approval. No code, no dependency, no schedule.**

Decision recorded: Prayer Times starts **after** Home integration is
finished, not alongside it. It is not cancelled — offline calculation is
feasible and needs no network content provider — but it is deferred for a
stated reason, and this document fixes what must be decided before any of
it is built.

## Why deferred, in one paragraph

Offline calculation is feasible and needs no content provider, so this is
not a "wait for data" deferral. It is policy-dense: location, timezone,
calculation method, Asr method, high-latitude rule, rounding and
adjustments all have to be settled before a result can be called
truthful. Starting a second correctness-sensitive module before the
ComposeView path is proven on a device would make both the scope and any
diagnosis harder. A prayer time that is quietly wrong is worse than no
prayer time (§57).

## Gate — all three must be true before implementation starts

| # | Gate | Status (2026-09-25) |
|---|---|---|
| 1 | PR #3 integration/merge complete | **DONE** — merged, plus one Lead section-order fix |
| 2 | Real Chromium ComposeView path builds **and** device smoke test green | **NOT MET** — real-source probe in flight; no device smoke test yet |
| 3 | This ADR approved | **NOT MET** — draft |

## Decisions this ADR must fix before implementation

1. **Supported calculation methods.** Bangladesh must not be silently
   assumed into any single method: the method is a user-visible,
   selectable setting, and the default must be stated, not implied.
2. **Asr method.** Hanafi versus Standard is user-visible and selectable.
   Not a hidden constant.
3. **High-latitude and polar behaviour.** What the app does when the
   standard rules do not yield a time must be a stated fallback, not a
   blank card or a crash.
4. **Rounding and per-prayer adjustment policy.** Nearest minute? Which
   direction? Are adjustments visible to the user and reversible?
5. **Manual city/coordinates is the first path.** Not the fallback for a
   failed location lookup — the primary flow.
6. **Device location: explicit opt-in, on-device only, no background
   location.**
7. **Exact coordinates are not persisted by default and are never sent to
   the network** (ADR-037).
8. **Timezone/DST and local-date boundaries** — including what happens
   for a prayer near midnight and across a DST change.
9. **Localized disclosure:** a calculated-time notice telling the user to
   confirm with their local mosque or authority, in both English and
   Bengali.

## Reference tests (fixed now, so they cannot be softened later)

Cities: **Dhaka, Chattogram, Sylhet, Rajshahi, Makkah**, one city with a
**DST boundary**, plus **high-latitude and polar edge cases**.

- Tolerance is documented in minutes, per configured method.
- A result from a **different** calculation method is **not** a test
  failure — it is a different, equally valid answer. Tests must compare
  against a reference for the *same* configured method.

## Dependency decision

**No dependency is added now.** At implementation time, an audited
algorithm or library is reviewed for licence, precision, timezone
behaviour and Chromium compatibility, and only then does the Lead approve
it. Nothing gets vendored on the strength of a blog post.

## Ownership

- Module and tests: Lead (correctness-sensitive, and it lands with the
  rest of the core).
- Localized strings and UI copy: Junior, through the string-catalog
  process used for Home.
- Final approval before the section becomes visible: product + security,
  as with the Popular Sites rail.
