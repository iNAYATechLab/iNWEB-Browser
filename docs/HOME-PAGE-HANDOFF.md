# Home Page — Handoff for the Junior Developer

Read this before writing code. It is the short path through a project that
is unusual in one important way: **the Chromium source is not in this
repository.**

---

## 1. What iNWEB is

A production Android browser built on **real Chromium**, pinned at
`154.0.8037.21` — not a WebView wrapper, not an SDK (MASTER-SPEC §71).
Brand: "iNWEB Browser", launcher name "iNWEB" (ADR-038), dark Material 3,
emerald/teal primary, subtle gold secondary. Positioning: privacy-focused
Islamic browser — Home = search + Islamic utilities (Qur'an, Hadith,
Prayer Times, Qibla, Dua) + normal browsing.

Current state (2026-09-25): Steps 1–50 done, `v1.0.0-alpha.1` released,
Stage-2 patch series **12/24 authored**, and the first real Android
compile chain is running (hop-24 reached `[64/1625]` with every iNWEB
source compiling; no APK yet).

## 2. How code reaches the browser (the part that surprises everyone)

```
src/native/<module>/*.{h,cc}        <- C++ authored HERE
src/core/<module>/*.kt              <- pure-JVM logic authored HERE (tests: scripts/validate_kotlin_core.sh)
        |
        |  Lead generates / regenerates
        v
iNWEB_PATCHES/<group>/00NN-*.patch  <- THE AUTHORITY for the Chromium tree
        |
        |  CI: b001-verify applies the series; b001-build-hop compiles for real
        v
   Chromium tree (GitHub Actions runner only — never on your machine)
```

Consequences for you:

- **You cannot compile or run the browser locally.** Nobody can; it builds
  on GitHub Actions in chained 6-hour hops. Local verification is the
  Kotlin core tests, the Python tests, and the patch-apply harness.
- **Editing a file under `chrome/...` is impossible here** — those paths
  exist only inside patches. Your Android-side code goes to the Lead, who
  turns it into patch entry `ui/0026-home-page`.
- Anything you put in `src/core/home/**` is testable locally today. That is
  why decision **D1** puts the logic there.

## 3. Repository map (what you will actually touch)

| Path | What it is |
|---|---|
| `src/core/home/**` | **Yours.** New pure-JVM Home Page module + tests. |
| `docs/design/inweb-prototype/**` | **Yours.** Interactive prototype (`index.html`, `styles.css`, `app.js`) + `DESIGN-HANDOFF.md`, `DESIGN-TOKENS.json`, `NAVIGATION-SPEC.md`, `INTERACTION-STATE-SPEC.md`, `VISUAL-QA-CHECKLIST.md`. |
| `src/core/browser-shell/**` | Existing: browser shell state (17 sources, 16 test files). Read it before modelling Home — reuse, don't duplicate. |
| `src/core/tracking-protection/**` | The reference example of a core module done right (19 sources, 14 test files). |
| `docs/MASTER-SPEC.md` §38 | The Home/New Tab requirement: search, shortcuts, bookmarks, recent pages, privacy status, downloads, customizable widgets/cards. |
| `docs/design/BRAND.md` | Naming (ADR-038), logo/asset derivation, QA record. |
| `docs/ACCESSIBILITY-AUDIT.md` | Accessibility rules your UI must satisfy. |
| `iNWEB_PATCHES/`, `src/native/`, `scripts/`, `.github/` | **Lead-owned — do not edit.** |

## 4. Working rules

1. **Branch:** `feature/home-<topic>`; push there, never to `main`. No pull
   requests (the repo has none; the Lead reviews the branch diff and lands
   it).
2. **Pure-JVM first.** `src/core/home` must not depend on Android, Chromium
   or the network. Same discipline as `tracking-protection`: a decision
   core that can be unit-tested on any JVM.
3. **Tests ship with code.** New behaviour without a test is not done. Run
   `bash scripts/validate_kotlin_core.sh` (pinned kotlinc 2.4.20 + JUnit
   4.13.2) before you signal ready.
4. **No hardcoded user-visible strings.** Every string goes through the
   strings pipeline (`scripts/validate_strings.py`,
   `validate_localization.py`).
5. **Honesty is a hard requirement (§57).** If a card shows a count, it
   must be a real count. No placeholder numbers, no "coming soon"
   surfaces, no claims the build cannot support. The prototype is a design
   artefact — shipping its demo data as if it were live state is a
   §57 violation.
6. **No WebView, ever (§71).** Home is a Chromium native surface.
7. **Do not touch core files** to make Home work. Need a core change? Raise
   it; the Lead makes it or refuses it with a reason.

## 5. Suggested build order

1. Read MASTER-SPEC §38 + the prototype handoff docs; list the cards that
   map to §38 items.
2. Model the data: search entry, shortcuts, bookmarks, recent pages,
   privacy status, downloads, widget/card configuration — as plain Kotlin
   types in `src/core/home`, with the state machine (what shows when,
   empty states, ordering, limits).
3. Tests for each behaviour, including empty/error/disabled states.
4. Explain the mapping Home → existing cores (`tracking-protection` for
   privacy status, `offline` for saved pages, `customization` for
   user-configured widgets) so the Lead can wire it without guessing.
5. Signal "ready for review" with: branch name, module path, test count,
   and the list of files you touched.

## 6. Review checklist the Lead will run

- [ ] Only Junior-owned paths changed (see TEAM-WORKFLOW.md §1)
- [ ] `validate_kotlin_core.sh` green, **414 + new** tests
- [ ] Python suite (80) and string/localization validators green
- [ ] Patch series still applies and verifies (`lint_manifest`, `verify`)
- [ ] No WebView, no fabricated data, no unbacked claim (§57/§71)
- [ ] Strings externalized; dark-M3 tokens from `DESIGN-TOKENS.json`, not
      ad-hoc colours
- [ ] Accessibility requirements from `ACCESSIBILITY-AUDIT.md` respected
- [ ] No duplicated logic that already exists in `browser-shell`,
      `tracking-protection`, `offline`, or `customization`

Integration instructions (how this becomes `ui/0026-home-page` and where
it hooks into the Chromium tree) are issued after this checklist passes.
