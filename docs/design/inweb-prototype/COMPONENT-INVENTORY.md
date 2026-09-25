# iNWEB Home — Native Component Inventory

**Purpose:** map the approved design to implementation-sized native components  
**Constraint:** component names are recommendations, not a required architecture

## 1. Screen composition

```text
InwebHomeScreen
├── InwebHomeHeader
│   ├── BrandIdentity
│   ├── PrivacyCenterChip
│   └── OverflowAction
├── HomeOmnibox
│   ├── SearchModeSelector
│   └── VoiceSearchAction
├── ShortcutSection
│   └── UtilityShortcutTile × 7
├── QuickAccessSection
│   ├── QuickAccessTile × n
│   └── AddSiteTile
├── PopularSitesSection
│   └── CuratedSiteTile × n
├── ContinueReadingCard
├── DailyWisdomCard
├── PrayerTimesCard
└── BrowserDestinationBar
```

Phone layout uses a `LazyColumn` for the Home feed. Header/omnibox stickiness is
an implementation decision to validate against content space and scrolling
behavior.

## 2. Root component

### `InwebHomeScreen`

**Responsibility**

- Order and compose Home sections.
- Own scroll state and restoration.
- Route child actions upward.
- Render global offline/degraded feedback.
- Apply system/navigation insets.

**Inputs**

- Header state
- Omnibox state
- Shortcut availability
- Quick Access state
- Curated sites state
- Reading/listening state
- Daily Wisdom state
- Prayer state
- Bottom destination state

**Actions**

Use one typed event stream or explicit callbacks. Avoid passing the whole
`BrowserViewModel` to every leaf component; previews and tests should accept
plain immutable state.

## 3. Header components

### `InwebHomeHeader`

**Visual inputs**

- Product identity
- Privacy summary state
- Header artwork resource

**Actions**

- Open Privacy Center
- Open Menu

**Design notes**

- Artwork is decorative.
- Logo uses the governed brand master in production.
- Privacy chip may shorten visually at compact widths.

### `PrivacyCenterChip`

**State variants**

- Active
- Partial
- Loading
- Unavailable

Do not model this as a free-form label/color pair. A typed state prevents
fabricated combinations such as `Protected` with unavailable counters.

## 4. Search components

### `HomeOmnibox`

**Inputs**

- Query text
- Mode: Web/Qur’an
- Focus state
- Voice state
- Submit/loading state
- Validation/error message

**Actions**

- Query changed
- Mode selected
- Submit
- Voice tapped
- Clear query

**Production binding**

The component emits intent to the authoritative browser/Qur’an navigation
controller; it does not implement URL classification itself.

### `SearchModeSelector`

Use Material tab/single-choice semantics. Both labels remain accessible at
compact width; visual fallback may use icon + shortened label.

## 5. Shortcut components

### `ShortcutSection`

**Inputs**

- Ordered list of seven curated utilities
- Per-item availability

**Actions**

- Utility selected

### `UtilityShortcutTile`

**Inputs**

- Stable utility ID
- Localized label
- Artwork resource
- Availability

**States**

- Default
- Focused/pressed
- Disabled/unavailable

The section has no add/edit/remove behavior in the approved design.

## 6. Quick Access components

### `QuickAccessSection`

**Inputs**

- Ordered persisted items
- Edit mode
- Save/reorder state

**Actions**

- Open item
- Enter/exit edit mode
- Add
- Remove
- Move/reorder
- Undo removal

### `QuickAccessTile`

**Inputs**

- Stable item ID
- Display name
- Normalized URL
- Local icon/favicon/brand reference
- Edit mode
- Position

Reordering must expose accessibility actions in addition to drag gestures.

### `AddSiteSheet`

**Inputs**

- Name/address fields
- Field validation
- Save state

**Actions**

- Field changes
- Save
- Cancel

Unsaved text remains after a failed save.

## 7. Curated website components

### `PopularSitesSection`

**Inputs**

- Ordered curated categories/sites
- Update/stale state where relevant

**Actions**

- Open tile
- View all

### `CuratedSiteTile`

**Inputs**

- Stable ID
- Localized title/caption
- Reviewed HTTPS URL
- Artwork resource
- Availability

Curation metadata lives outside the composable.

## 8. Reading and listening component

### `ContinueReadingCard`

Use a sealed state rather than nullable fields:

- `FirstUse`
- `ResumeReading`
- `Loading`
- `Unavailable`
- `Error`

**Resume data**

- Surah ID/display name
- Ayah number
- Optional translation label
- Progress
- Last-read display value
- Audio availability/playback state

**Actions**

- Start reading
- Continue reading
- Play/pause/resume audio
- Retry

Artwork stays decorative; the text describes the content.

## 9. Daily Wisdom component

### `DailyWisdomCard`

**Inputs**

- Loading/cached/content/unavailable/error state
- Item ID and kind: Ayah/Hadith
- Body text
- Source/citation
- Date/update metadata
- Saved state

**Actions**

- Save/remove
- Share
- Retry refresh

Do not accept content without citation/source metadata in the content state.

## 10. Prayer components

### `PrayerTimesCard`

Recommended state variants:

- `SetupRequired`
- `ScheduleAvailable`
- `StaleSchedule`
- `Loading`
- `Error`

**Schedule data**

- Location display
- Time zone
- Calculation-method display/ID
- Current date
- Ordered five prayer rows
- Next-prayer ID
- Countdown

**Actions**

- Start setup
- Choose manual location
- Request automatic location
- Open calculation settings
- Select prayer/reminder, only if implemented
- Retry

### `PrayerTimeRow`

Render as static content unless a real row action exists. Selected/next state
must include an icon/text semantic, not background color alone.

## 11. Bottom navigation component

### `BrowserDestinationBar`

**Inputs**

- Available destinations
- Selected destination
- Real tab count
- Extensions availability
- Window size/insets

**Actions**

- Destination selected
- Destination reselected

The six destinations require migration from the existing toolbar model as
specified in `NAVIGATION-SPEC.md`.

## 12. Overlay components

### `PrivacyCenterSheet`

**Inputs**

- Independent real protection states
- Real blocked statistics
- Update-in-progress/error state

**Actions**

- Toggle supported policy
- Open details
- Open private tab
- Retry

### `ExtensionsSurface`

**Inputs**

- Extension registry states
- Availability/binding state
- Permission review data

**Actions**

- Review
- Enable/disable
- Update
- Remove
- Import/add through a real source

### `BrowserMenuSheet`

Inputs depend on Home/page context. Items are typed actions, not labels with
arbitrary click lambdas spread through the UI.

## 13. Shared design components

Recommended reusable primitives:

- `InwebSectionHeader`
- `InwebImageCard`
- `InwebArtworkTile`
- `InwebStatusChip`
- `InwebEmptyState`
- `InwebInlineError`
- `InwebLoadingPlaceholder`
- `InwebBottomSheetHeader`

Reuse must not erase semantic differences. For example, a privacy toggle and a
menu action may share spacing but require different accessibility roles.

## 14. Preview/state fixtures

Compose previews should use clearly named design fixtures, never production
fallback values:

- Header active/partial/unavailable
- Web/Qur’an search focused
- Shortcuts standard/compact
- Quick Access view/edit/empty
- Popular sites standard/large font
- Continue first-use/resume/audio/error
- Daily Wisdom loaded/saved/offline/error
- Prayer setup/schedule/stale/error
- Bottom navigation 320dp/390dp/600dp
- Bengali + 200% font scale

Fixtures live in preview/test source sets and must not ship as runtime data.

## 15. State ownership boundary

| Layer | Owns |
|---|---|
| Compose leaf | Visual state only; transient press/focus where appropriate |
| Screen state holder | Selection, sheets, edit mode, normalized display state |
| Core/store/provider | Persistent data, validation, calculations, policy |
| Chromium adapter | Navigation, tabs, downloads, extension engine, protection state |

A composable must not calculate prayer times, parse browser URLs, decide
protection policy or validate extension permissions.

## 16. Testing expectations

### Component tests

- All sealed state variants render without crashes.
- Actions emit correct typed intent.
- No action is exposed for unavailable behavior.
- Semantics include labels, roles, selected/disabled state and headings.

### Screenshot tests

Use the viewport/locale/font-scale matrix from `VISUAL-QA-CHECKLIST.md`.

### Integration tests

- Search intent reaches the correct provider.
- Quick Access changes persist and restore.
- Privacy toggles reflect actual core results.
- Reading state and prayer schedule do not use preview fixtures.
- Extension states map to the real registry.
