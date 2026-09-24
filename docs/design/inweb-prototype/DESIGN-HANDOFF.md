# iNWEB Home / New Tab — Design Handoff

**Status:** approved visual direction; production implementation pending  
**Reference:** `index.html` in this directory  
**Target UI technology:** native Jetpack Compose + Material 3  
**Non-goal:** embedding this prototype in a WebView

## 1. Product intent

The Home / New Tab surface presents iNWEB as a modern privacy-focused browser
with useful Islamic utilities. The interface must remain recognizably a browser:
search and normal web destinations are primary, Islamic features are purposeful,
and decorative religious treatment stays restrained.

### Visual direction

- Dark Material 3 foundation
- Deep green / teal surfaces
- Emerald as the primary interactive accent
- Restrained gold for citations, warmth and selected details
- White/high-contrast type
- Contextual photography only on large cards
- Individual image-based icons or known site marks on small tiles
- No dense mosque pattern, oversized ornament or all-green visual overload

## 2. Approved Home order

1. Search Bar
2. Shortcuts
3. Quick Access
4. Popular Islamic Websites
5. Continue Reading & Listening
6. Daily Wisdom
7. Prayer Times

The Privacy Center entry belongs in the Header. It must not be duplicated as a
large card near the bottom of Home.

## 3. Header

### Content

- iNWEB mark
- `iNWEB` product name and `Browser` descriptor
- Privacy Center action with shield and live-state indicator
- Overflow menu

### Behavior

- Header remains visible while Home scrolls.
- The background may use the supplied mosque image with a strong dark overlay.
- `Privacy Center` opens a real protection sheet/surface.
- On narrow widths, the text may shorten to `Privacy`, but the action and
  accessible label remain `Privacy Center`.
- Never display `Protected`, a numeric score or blocked count unless the value
  comes from real browser state.

## 4. Search Bar

### Content

- Search/URL input
- Web search mode
- Qur’an search mode
- Voice-search action

### Integration rule

Production must not create two competing omniboxes. The Home search treatment
and the browser's existing `OmniboxBar` need one authoritative input/state path.
Qur’an search is a distinct routed provider, not a fake URL/search state.

### States

- Idle
- Focused
- Input present
- Voice listening
- Web mode selected
- Qur’an mode selected
- Disabled/unavailable voice permission

## 5. Shortcuts

### Label

Use **Shortcuts**, not `Islamic Shortcuts`.

### Approved items

1. Qur’an
2. Hadith
3. Prayer
4. Qibla
5. Dua
6. Halal Life
7. News

There is no Add (`+`) tile in this section. Every item uses a separate
illustrated image asset. These are curated product utilities, not user-created
web shortcuts.

### Layout

- One row where width permits
- Horizontal scroll only when required on narrow screens
- Minimum touch target: 48dp
- Icon artwork: approximately 44–48dp
- Label: one line with ellipsis rather than wrapping

## 6. Quick Access

Quick Access is the editable user-owned website area, equivalent to common
browser new-tab shortcuts.

### Default visual references

- YouTube
- Google
- Wikipedia
- Facebook
- X
- Add Site

Use real/recognizable local brand marks on neutral surfaces. Do not use a
photographic background per tile.

### Required production behavior

- Add
- Remove
- Reorder
- Persist
- Open through the normal browser navigation path
- Do not silently derive this user-owned list from browsing history

Top Sites computed from history may exist as a separate feature, but must not
replace user-owned Quick Access without an explicit product decision.

## 7. Popular Islamic Websites

### Approved categories

- Qur’an — Read & listen
- Hadith — Sunnah
- Islamic Q&A — Learn & ask
- Articles — Knowledge
- Halal Life — Travel & food
- News — Latest updates

Each tile uses a distinct contextual image/illustration. The production list
needs stable IDs, reviewed HTTPS URLs, localized labels, and a curation/update
policy. External destinations must never imply iNWEB endorsement beyond the
published curation policy.

## 8. Continue Reading & Listening

### Compact card

The approved card is intentionally about half the height of the first mockup.
It contains:

- `Continue reading & listening` eyebrow
- Surah name
- Ayah number and optional localized subtitle
- Continue action
- Last-read timestamp
- Subtle progress indicator
- Qur’an image positioned on the right

### Required real state

- Surah ID
- Ayah ID
- Reading progress
- Last-read time
- Selected translation
- Audio/reciter state where listening is enabled

If no reading state exists, show a truthful first-use state such as `Start
reading`; never fabricate Al-Baqarah 255.

## 9. Daily Wisdom

### Content

- Daily Wisdom label
- Ayah or Hadith
- Exact source/citation
- Save
- Share

Use the mosque-at-blue-hour image with a dark overlay. A citation is mandatory.
Production content needs a reviewed source, licensing/provenance, stable content
ID, localization policy and offline/cache behavior. A daily item must not be
presented as authenticated unless its source metadata supports that claim.

## 10. Prayer Times

### Content

- Current location
- Next-prayer name and countdown
- Fajr
- Dhuhr
- Asr
- Maghrib
- Isha

The image appears only in the upper/header region so the time rows remain highly
legible.

### Required real state

- Location source: manual or permission-based
- Time zone
- Calculation method
- Asr method
- High-latitude rule where relevant
- Current date and computed schedule
- Next-prayer countdown

Do not ship the prototype's Vientiane times as global constants. Permission must
be requested only when the user chooses automatic location; manual location is
a privacy-preserving alternative.

## 11. Bottom browser navigation

The visual prototype uses:

1. Home
2. Bookmarks
3. Tabs
4. Downloads
5. Extensions
6. Menu

This conflicts with the currently authored Back/Forward/Home/Tabs/Menu toolbar
model. Production adoption requires a documented navigation decision and a
versioned migration of the toolbar customization core. Extensions must remain
hidden or unavailable until the real Chromium extension binding is present and
verified.

## 12. Privacy Center

### Intended controls/statuses

- Ad and tracker blocking
- Pop-up protection
- Safe browsing
- Adult-site filter, only if genuinely implemented
- Private mode entry

Every status, counter and toggle must bind to real state and behavior. Hide an
unimplemented feature rather than displaying an always-on switch. Privacy Center
is a status/control surface, not marketing decoration.

## 13. Design tokens

The web prototype is the visual source; production should map these to Material
3 roles rather than copy raw CSS mechanically.

| Role | Reference |
|---|---|
| App background | `#061513` |
| Primary surface | `#091C1A` |
| Raised surface | `#0D2421` |
| Strong surface | `#12302B` |
| Primary emerald | `#2BE1AE` |
| Secondary emerald | `#0CB68E` |
| Gold accent | `#E1B659` |
| Primary text | `#F3FAF8` |
| Secondary text | `#9BB5B0` |
| Subtle text | `#64847D` |
| Error/removal | `#FF6478` |

### Shape references

- Large cards: 20–24dp
- Search field: 22–24dp
- Icon tiles: 13–16dp
- Bottom sheet: 28dp top corners
- Small status chip: pill / 50% radius

Production colors must pass the repository's contrast gates in both dark and
light modes. The approved prototype is dark-first; a light-theme treatment
still needs design and verification before a light build claims parity.

## 14. Asset map

| Asset | Intended use |
|---|---|
| `assets/logo-mark.jpg` | Prototype header mark; production should derive from the governed logo master |
| `assets/quran-ambient.jpg` | Continue Reading & Listening card |
| `assets/mosque-dusk.jpg` | Header, Daily Wisdom and Prayer header with distinct crops/overlays |
| `assets/icons/quran.svg` | Qur’an utility/category |
| `assets/icons/hadith.svg` | Hadith utility/category |
| `assets/icons/prayer.svg` | Prayer utility |
| `assets/icons/qibla.svg` | Qibla utility |
| `assets/icons/dua.svg` | Dua utility |
| `assets/icons/halal.svg` | Halal Life utility/category |
| `assets/icons/news.svg` | News utility/category |
| `assets/icons/qa.svg` | Islamic Q&A category |
| `assets/icons/articles.svg` | Articles category |
| Brand-reference SVGs | Quick Access visual references |

Before production use, convert photographic artwork to optimized Android
resources, record provenance/licensing, and provide density/size validation.
Decorative images should have null accessibility descriptions; the surrounding
text/action labels carry meaning.

## 15. Responsive and accessibility requirements

- Use `LazyColumn` for the complete Home feed.
- Keep declaration order equal to visual/focus order.
- Every section title is a semantic heading.
- Minimum interactive target is 48dp.
- Icon-only controls require localized content descriptions.
- Support 200% font scaling without clipped actions or inaccessible content.
- Horizontal tile lists expose scroll semantics and do not trap focus.
- Respect reduced-motion settings.
- Do not encode status by color alone.
- English and Bengali strings must remain in parity.

## 16. Production acceptance checklist

### Visual

- [ ] Approved section order matches this document
- [ ] Header and cards use the intended image hierarchy
- [ ] Small tiles use distinct image-based icons/logos
- [ ] Continue card remains compact
- [ ] Text remains readable over every image crop
- [ ] Narrow phone and tablet layouts are reviewed

### Functional truth

- [ ] Every privacy status comes from real browser state
- [ ] Quick Access CRUD persists
- [ ] Curated website URLs are reviewed
- [ ] Qur’an progress is real or first-use state is shown
- [ ] Daily Wisdom carries a verifiable citation
- [ ] Prayer times use a documented calculation/location policy
- [ ] Extensions is not exposed before the binding is real

### Quality

- [ ] English/Bengali string validation passes
- [ ] Storage inventory covers every new persisted value
- [ ] Unit tests cover stores/providers and migrations
- [ ] TalkBack and 200% font-scale checks pass
- [ ] Contrast and 48dp target audits pass
- [ ] Patched Chromium APK builds
- [ ] Device verification is recorded before merge/release

## 17. Ownership and merge workflow

- Design work stays on a design/feature branch; never directly on `main`.
- The application developer implements production Compose/Chromium bindings on
  a separate integration branch.
- Design reviews compare implementation screenshots and behavior against this
  handoff and the interactive reference.
- The application developer owns build verification and the final merge after
  tests pass.
