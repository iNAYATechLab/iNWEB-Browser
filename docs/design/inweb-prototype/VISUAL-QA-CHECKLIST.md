# iNWEB Home — Visual QA Checklist

**Purpose:** repeatable design acceptance before the application developer
merges a native implementation  
**Reference build:** `docs/design/inweb-prototype/index.html`

## 1. Required review captures

Capture the complete Home surface and key overlays at these widths:

| Class | Reference viewport |
|---|---|
| Narrow Android phone | 320 × 720 |
| Compact phone | 360 × 800 |
| Standard phone | 390 × 844 |
| Large phone/reference | 464 × 920 |
| Foldable/tablet compact pane | 600 × 960 |
| Tablet | 840 × 1180 |

For native review, also capture the real target devices from the repository's
device matrix. Browser chrome, system bars and display cutouts must be visible
in at least one capture per device class.

## 2. Baseline screenshot set

Use this naming pattern:

```text
home_<width>x<height>_<theme>_<locale>_<state>.png
```

Minimum states:

- Home top/default
- Home middle: Popular + Continue
- Home lower: Daily Wisdom + Prayer Times
- Search focused with keyboard
- Qur’an search selected
- Quick Access edit mode
- Add Site sheet
- Privacy Center sheet
- Extensions surface
- Menu sheet
- Offline/degraded Home
- 200% font scale
- Bengali locale

## 3. Global hierarchy

- [ ] Header is visually distinct but not taller than its content requires.
- [ ] Search is the strongest actionable element near the top.
- [ ] Shortcuts and Quick Access are clearly different concepts.
- [ ] Section order matches the approved seven-section order.
- [ ] Large image cards do not make the page feel like a news feed.
- [ ] Emerald is used for interaction/status, not every surface.
- [ ] Gold remains secondary and never replaces primary action color.
- [ ] Borders are visible without creating a boxed grid everywhere.
- [ ] No Arabic ornament/pattern competes with content.

## 4. Imagery

### Header

- [ ] Uses `header-ambient.jpg`.
- [ ] Mosque/globe detail remains on the right at phone widths.
- [ ] Left/logo region stays dark enough for text.
- [ ] Privacy Center remains readable over the brightest crop.

### Continue Reading & Listening

- [ ] Uses `quran-ambient.jpg`.
- [ ] Qur’an remains visible on the right after compact crop.
- [ ] Card height remains approximately 108dp reference, not the original tall card.
- [ ] Title, metadata, action and last-read text remain legible.

### Daily Wisdom

- [ ] Uses `daily-wisdom.jpg`.
- [ ] Lantern/manuscript occupy the right side.
- [ ] Quote and citation do not overlap the focal artwork.
- [ ] Warm gold does not reduce text contrast.

### Prayer Times

- [ ] Uses `prayer-dawn.jpg`.
- [ ] Image is confined to/weighted toward the header region.
- [ ] Prayer rows sit on a stable dark surface.
- [ ] Highlighted next prayer is readable in bright and dark crops.

### Small tiles

- [ ] Every Shortcut has distinct artwork.
- [ ] Popular Website categories have distinct artwork.
- [ ] Quick Access uses recognizable local brand marks.
- [ ] Logos are not stretched, clipped or placed on mismatched corner radii.
- [ ] Decorative artwork is not announced by screen readers.

## 5. Header and Search

- [ ] Logo is crisp at all densities.
- [ ] App name and descriptor do not clip at 200% font scale.
- [ ] Privacy Center has a 48dp minimum target.
- [ ] Narrow layout uses `Privacy` while keeping full accessible label.
- [ ] Overflow remains visible at 320dp width.
- [ ] Search placeholder truncates gracefully.
- [ ] Web and Qur’an modes have clear selected state.
- [ ] Voice state is not represented by color alone.
- [ ] Focus ring does not shift surrounding layout.

## 6. Shortcuts

- [ ] Visible heading is `Shortcuts`.
- [ ] There are seven curated items.
- [ ] No Add (`+`) tile appears.
- [ ] All labels fit or ellipsize on one line.
- [ ] 320dp layout scrolls or adapts without overlapping artwork.
- [ ] Keyboard/D-pad focus follows visual order.
- [ ] Touch targets meet 48dp even when artwork is 42dp.

## 7. Quick Access

- [ ] Quick Access appears after Shortcuts.
- [ ] Six reference tiles fit at standard width.
- [ ] Add Site is visually an action, not a saved website.
- [ ] Edit/Done state is unambiguous.
- [ ] Removal affordance does not reduce the underlying touch target.
- [ ] Reorder feedback is visible and accessible.
- [ ] Long names ellipsize without changing tile height.
- [ ] Empty state remains balanced and actionable.

## 8. Popular Islamic Websites

- [ ] Appears below Quick Access and above Continue card.
- [ ] Horizontal list communicates that more content is available.
- [ ] First and last tiles have correct edge insets.
- [ ] `View all` target is at least 48dp even if its visual text is smaller.
- [ ] Title/subtitle hierarchy remains readable at 200% font scale.
- [ ] Category artwork remains distinguishable without relying on hue alone.

## 9. Continue Reading & Listening

- [ ] Eyebrow text does not collide with artwork.
- [ ] First-use state and resume state have equal visual stability.
- [ ] Continue/Listen actions remain 48dp touch targets.
- [ ] Progress indicator has non-color context for accessibility.
- [ ] Last-read text is not smaller than the accepted caption floor.
- [ ] Bengali surah/ayah strings do not overflow the card.

## 10. Daily Wisdom

- [ ] Daily Wisdom appears above Prayer Times.
- [ ] Quote supports at least four lines without clipping.
- [ ] Citation stays attached to the quotation.
- [ ] Save selected state is visible and announced.
- [ ] Share and Save targets are 48dp.
- [ ] Long Bengali citations wrap without covering controls.

## 11. Prayer Times

- [ ] Location is visible and ellipsizes safely.
- [ ] Next-prayer countdown does not resize the header as values change.
- [ ] All five rows align names and times consistently.
- [ ] Active prayer highlight is not color-only.
- [ ] AM/PM or 24-hour formatting follows locale/user settings.
- [ ] Large fonts do not clip row labels or times.
- [ ] Manual-location/setup state is visually intentional, not an error blank.

## 12. Bottom navigation

- [ ] Six destinations fit at standard and compact widths.
- [ ] Menu never falls outside/clips at the right edge.
- [ ] Extensions and Menu are simultaneously visible.
- [ ] Selected state is clear without relying only on color.
- [ ] Labels have complete accessibility names even when visually truncated.
- [ ] Tab badge does not cover the icon or selected container.
- [ ] Gesture navigation inset is respected.
- [ ] Landscape and split-screen behavior is reviewed.

## 13. Sheets

- [ ] Sheet handle, heading and Close action are visible.
- [ ] Sheet top never hides under status/cutout areas.
- [ ] Content scrolls independently when height is constrained.
- [ ] Scrim provides sufficient separation.
- [ ] Focus is trapped/restored appropriately.
- [ ] Privacy switches use whole-row targets.
- [ ] Private Mode uses navigation styling, not switch styling.
- [ ] Extension pending-review/disabled states are distinguishable.
- [ ] Add Site keyboard does not cover the Save action without scroll access.

## 14. Localization

### English

- [ ] Capitalization is consistent.
- [ ] `Qur’an` typography is consistent across the surface.
- [ ] `Privacy Center` and `Continue Reading & Listening` use approved wording.

### Bengali

- [ ] Human review performed; no machine-only approval.
- [ ] Bengali line height prevents matra/descender clipping.
- [ ] Mixed Bengali/Arabic/Latin strings render in correct order.
- [ ] Numerals and prayer times follow the chosen locale policy.
- [ ] No English fallback appears except governed non-translatable brands.

## 15. Accessibility

- [ ] TalkBack traversal matches visual order.
- [ ] Every section heading is announced as a heading.
- [ ] Every icon-only action has a localized label.
- [ ] Selected modes/destinations announce selected state.
- [ ] Snackbar messages are announced politely.
- [ ] Error messages are associated with their field/control.
- [ ] All interactive targets meet or exceed 48dp.
- [ ] Text contrast meets WCAG AA targets.
- [ ] Focus indicator contrast is visible on image and solid surfaces.
- [ ] Reduced-motion mode removes decorative pulse/scale motion.

## 16. State truthfulness

- [ ] No fixed `Protected` state without real protection data.
- [ ] No fabricated blocked-tracker count.
- [ ] No static Qur’an progress on first use.
- [ ] No sample prayer times presented as the user's schedule.
- [ ] No uncited Daily Wisdom item.
- [ ] No enabled Extensions destination without real binding.
- [ ] No Adult Site Filter switch without real behavior.
- [ ] Loading/error/empty states are tested, not only ideal content.

## 17. Performance-sensitive visual checks

- [ ] Images use appropriately sized Android resources and do not decode at source resolution unnecessarily.
- [ ] Home scroll remains smooth with all sections populated.
- [ ] Minute countdown updates do not visibly recompose unrelated sections.
- [ ] Tile lists use stable keys.
- [ ] Cached imagery/content prevents layout jump at startup.
- [ ] No continuously running decorative animation while Home is idle.

## 18. Review result template

```text
Build/commit:
Device/emulator:
Android version:
Viewport/density:
Theme:
Locale:
Font scale:
Result: PASS / FAIL / BLOCKED
Screenshots:
Findings:
Required fixes:
Reviewer:
Date:
```

A PASS means the exact reviewed build meets this checklist. It does not imply
functional verification of Chromium/data providers; those remain in the
repository's implementation and device matrices.
