# Phase 10 — Localization & Accessibility Design

**Status:** authored design + a NEW enforcement gate that is live in CI
today (Step 29). Android-runtime verification needs the built app
(blocker B-001).
**Governing requirements:** MASTER-SPEC §36 (localization — Bengali
(Bangladesh) + English, all user-visible strings externalized, Android
best practices), §49 (accessibility), roadmap Phase 10.

---

## 1. What is enforced in CI TODAY (real, not aspirational)

| Gate | What it proves |
|---|---|
| `scripts/validate_strings.py` (live since Phase 2) | Bidirectional key parity between `values/strings.xml` and every `values-*/strings.xml` (bn-BD); no empty values; format placeholders (`%s`, `%1$s`, …) identical across locales |
| `scripts/validate_localization.py` (NEW, live in CI) | §36 externalization at the SOURCE level: the authored UI (`ui/**.kt`, `MainActivity.kt`) may not contain hardcoded user-visible literals — positional `Text("…")` and the named user-visible parameters (`text`, `label`, `title`, `contentDescription`, `description`, `placeholder`). Deliberate exemptions (documented in the tool): empty strings, pure digits (badges/counts), dynamic `${…}` interpolation, and a reviewed `// NON-LOCALIZED` escape hatch that CI reports as a note |
| Unit tests (NEW: 8) | The gate itself is tested — flags, exemptions, scope (non-UI sources like store formats are NOT scanned), entry-activity coverage, missing-UI error |

Current state: the authored UI passes with **zero violations and zero
escape hatches** — every user-visible string is already externalized,
with full bn-BD parity.

## 2. Localization policy (§36)

- **Locales:** `en` (baseline) + `bn-BD` (Bengali, Bangladesh) — the two
  first-class locales; every new string lands in BOTH files in the same
  commit (CI enforces parity, so a partial string cannot merge).
- **Externalization:** all user-visible text via `stringResource`;
  plurals via Android `<plurals>`; dates/numbers via locale-aware
  platform formatters; never sentence concatenation (placeholders carry
  word order).
- **No machine-translation claims:** bn-BD strings are authored, not
  auto-translated at runtime.
- **Honest boundary:** string QUALITY (reading level, terminology
  consistency) is a human-review deliverable; the gates prove structure,
  not prose.

## 3. Accessibility policy (§49) — authoring contracts

These are binding contracts for the authored UI and a mandatory review
checklist for every `ui/` patch (0001+ and future):

1. **Screen readers:** every interactive element exposes a meaningful
   label — text content or `contentDescription`/semantics; images and
   icon-only buttons are NEVER unlabeled; focus order follows visual
   order; async state changes announce via live-region semantics.
2. **Content descriptions:** localized like any other user-visible
   string (the externalization gate already covers `contentDescription`
   literals).
3. **Scalable text:** `sp` units for text; layouts must survive 200%
   font scale without clipping or overlap (device-verified at B-001).
4. **Contrast:** Material 3 design tokens only — no ad-hoc colors; text
   contrast meets WCAG AA (4.5:1) in both light and dark themes.
5. **Touch accessibility:** interactive targets ≥ 48dp.
6. **Keyboard/navigation support where applicable:** external keyboard
   and D-pad navigation for focusable surfaces.
7. **Reduced motion:** animations respect the system
   remove-animations/reduced-motion setting.

## 4. Verification matrix

| Check | When | How |
|---|---|---|
| Key + placeholder parity | every commit (CI) | `validate_strings.py` |
| Source-level externalization | every commit (CI) | `validate_localization.py` |
| bn-BD prose review | per release | human review (documented responsibility) |
| TalkBack pass over every screen | at B-001 | scripted device test |
| 200% font scale render | at B-001 | screenshot diff on key screens |
| Contrast measurement | at B-001 | automated contrast audit of the theme tokens |
| Touch-target audit | at B-001 | layout inspection tooling |

## 5. Patch-plan impact

No new registry entries: localization resources live in
`src/android-app` (bound by `ui/0001`), and the §49 contracts are
enforced as review gates on every `ui/`/`settings/` patch — the
integration plan's per-patch checklist gains a §49 line.

## 6. Honest boundaries

- The CI gates prove STRUCTURE (externalization, parity); translation
  quality and on-device accessibility need the built product (B-001).
- No runtime machine translation; no locale is claimed beyond en +
  bn-BD.
- The `// NON-LOCALIZED` escape hatch is reviewed (CI prints a note for
  each) — today there are zero.
