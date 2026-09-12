# iNWEB Browser — §49 Source-Level Accessibility Audit (Step 43)

**Status:** audit of the authored Android UI (`src/android-app`) against
the §49 authoring contracts (PHASE10 §3), executed as source-level
review — every screen and composable walked. Findings were FIXED in
the same step; this register is the record. Device verification
(TalkBack pass, 200% font scale, contrast measurement, touch-target
audit) stays on the device-matrix rows **D10-1…D10-5** — this audit
proves source structure, not on-device behavior (§57).

**Scope:** all 13 `ui/` files + `MainActivity` (22 authored Kotlin
files total; `settings/`, `session/` adapters are non-UI). Audit date:
2026-09-12.

---

## 1. Findings register (all FIXED in this step)

| ID | Severity | Finding (contract broken) | Fix |
|---|---|---|---|
| A-1 | **violation** — §49 touch accessibility / PHASE10 §3.5 (targets ≥ 48dp) | `TabsScreen` tab-close `IconButton` forced to **28dp** — below the 48dp minimum | size override removed; default 48dp target, 18dp icon retained |
| A-2 | **violation** — §49 contrast / PHASE10 §3.4 (WCAG AA 4.5:1) | light-theme primary `#00897B` as text measured **4.22:1** on the background; white-on-primary buttons **4.34:1** — both below AA | primary darkened to `#00796B` (teal-700): **5.16:1** text on background, **5.28:1** white on primary; dark theme already 9.8:1 (unchanged). Ratios computed per WCAG relative-luminance and recorded in `Theme.kt` |
| A-3 | gap — §49 screen readers/touch | onboarding engine-choice rows: only the radio button itself was the touch target; the label was a dead zone; no `Role.RadioButton` on the row | row is now `selectable(selected, role = Role.RadioButton, onClick)`; `RadioButton(onCheckedChange = null)` — same pattern the clear-data surface already used |
| A-4 | gap — §49 touch/screen readers | switch rows (settings notifications, ask-before-download, toolbar visibility) — the switch was a separate small target from its label, no row-level `Role.Switch` | all three rows are now whole-row `toggleable(role = Role.Switch)` (toolbar row: `enabled = !mandatory` respected); `Switch(onCheckedChange = null)` |
| A-5 | gap — §49 content descriptions | the open-tabs badge announced a bare digit ("3"); the localized `tabs_count` sentence existed but was unused | the tabs button's content description is now `tabs_count` ("%1$d tabs open" / bn) |
| A-6 | gap — §49 screen-reader structure | section headers and step headlines were plain text — no heading semantics | `Modifier.semantics { heading() }` added: HomePage sections, Settings ×4 (search engine, theme, downloads, notifications), ZoomSettings ×2, onboarding headlines ×2 |
| A-7 | gap — §49 state announcements | onboarding step changes ("1 / 2") were silent for screen readers | the step indicator is now a polite live region |

No finding required new user-visible strings: A-5 reuses the existing
`tabs_count` resource — en/bn parity untouched.

## 2. Verified PASS (recorded, no change needed)

| Contract | Evidence |
|---|---|
| Every icon-only control labeled | all `IconButton`s carry localized `contentDescription` (back arrows, add/close/delete, reload/stop, menu, move up/down, new tab, private tab, remove site zoom) |
| Decorative icons marked null deliberately | chevrons in nav rows (`contentDescription = null` with row-text labeling), search leading icon, onboarding check bullets |
| Checkbox dialog pattern | `ClearDataScreen` rows already `toggleable` + `Role.Checkbox` (§49 pattern since Step 38) |
| Radio-row pattern (settings/zoom) | `SelectableRow`: whole row clickable, label text present, 48dp+ targets |
| Text in `sp` | all text uses `MaterialTheme.typography` styles; no hardcoded `dp` font sizes |
| M3 tokens only | no ad-hoc `Color(...)` in screen code; the palette lives in `theme/Theme.kt` as M3 color roles (see A-2 for the palette's measured ratios) |
| Progress semantics | M3 `LinearProgressIndicator` exposes `ProgressBarRangeInfo` semantics |
| Focus order | declaration order = visual order in every screen (no reordering modifiers) |
| Reduced motion (§49) | no `animate*`/transition APIs are used in the authored UI — nothing to disable; future motion must respect the system setting (patch-review gate, PHASE10 §3.7) |
| Externalization | `contentDescription` literals covered by the CI externalization gate — 0 escape hatches |
| Scrolling containers | every long surface scrolls (`verticalScroll`/`LazyColumn`/`LazyVerticalGrid`) — 200% font-scale survival is device-verified (D10-2) |

## 3. Honest boundaries

- This is a **source-level** audit: semantics and sizes are proven in
  code; actual TalkBack behavior, 200% rendering, measured contrast on
  real panels, and D-pad traversal need the built app (rows D10-1…D10-5).
- The A-2 contrast ratios are computed (WCAG relative luminance), not
  measured on a display — measurement happens in the D10-3 audit.
- §49 keyboard/navigation "where applicable": the authored surfaces are
  touch-first; Compose provides default focus traversal for free, and
  external-keyboard/D-pad verification is part of D10-1's scripted pass.
