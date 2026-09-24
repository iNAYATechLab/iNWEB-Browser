# iNWEB Browser — Navigation Design Specification

**Status:** design recommendation for implementation review  
**Applies to:** Home / New Tab, browsing chrome, bottom navigation and sheets

## 1. Decision

Use one navigation architecture with two visual toolbar states:

1. **Home state** — brand header + Home omnibox/search treatment
2. **Page state** — compact browser toolbar + authoritative omnibox

Both states share the same navigation controller and omnibox data path. The
Home search field is not a separate search engine implementation.

## 2. Home state

### Top region

- Status bar/insets
- iNWEB logo and name
- Privacy Center
- Overflow menu
- Large Home omnibox immediately below the header

### Bottom destinations

| Position | Destination | Behavior |
|---:|---|---|
| 1 | Home | Scroll current Home to top; if browsing, open/select Home tab state |
| 2 | Bookmarks | Open persisted bookmarks surface |
| 3 | Tabs | Open tab switcher; show real open-tab count |
| 4 | Downloads | Open download manager |
| 5 | Extensions | Open extension manager only when real binding is available |
| 6 | Menu | Open browser menu sheet |

Home is selected only while the current visible content is Home/New Tab.

## 3. Page state

### Compact top toolbar

Recommended order:

1. Back
2. Forward when space permits; otherwise place in Menu
3. Security/site indicator
4. Omnibox
5. Reload/Stop
6. Overflow

The toolbar owns URL, search, loading and site-security state. System Back and
Android predictive-back behavior remain available.

### Bottom navigation

The six-destination bar may remain visible if device testing shows sufficient
content space and no conflict with Chromium controls. If it materially reduces
web content height, use this adaptive policy:

- show the six-destination bar on Home and first-level product surfaces;
- collapse it during active page browsing;
- expose Home, Tabs and Menu through the compact page toolbar;
- restore the destination bar when the user returns to Home.

This behavior must be decided through device screenshots and interaction tests,
not viewport assumptions alone.

## 4. Omnibox routing

### Web mode

- URL-like input uses the existing URL parser.
- Other input uses the selected default web search engine.
- Submission opens in the selected tab through the real engine adapter.

### Qur’an mode

- Input is routed to a dedicated Qur’an search provider.
- Results remain a native/product surface unless a reviewed external provider
  is intentionally selected.
- Mode selection must be announced to screen readers.
- The user can return to Web mode without losing an unfinished query.

### Voice

- Request microphone permission only after the user taps Voice Search.
- Denial leaves typing fully functional and provides a route to settings.
- Listening, processing, result and error states must be visible and announced.

## 5. Menu structure

### Primary actions

- New tab
- New private tab
- Bookmarks
- History
- Downloads
- Extensions, when available
- Privacy Center
- Settings

### Secondary page actions

Show only while a web page is active:

- Share
- Find in page
- Desktop site
- Save for offline, when implemented
- Add bookmark / remove bookmark

No menu item should open a placeholder that reports success.

## 6. Sheets and overlays

### Privacy Center

- Opens from the Header or Menu.
- Uses a modal bottom sheet on phones.
- Uses a side sheet or constrained dialog on wide layouts.
- Reads and updates real protection state.

### Extensions

- Opens from the bottom destination or Menu.
- If the Chromium binding is unavailable, do not expose an enabled destination.
- Installed counts, permissions and toggles come from `ExtensionRegistry` plus
  the real engine adapter.

### Add Quick Access site

- Opens only from Quick Access.
- Name and HTTPS URL fields.
- Validate and normalize before persistence.
- Confirmed entries appear immediately and survive restart.

## 7. Back behavior

Priority order:

1. Close open dialog/sheet
2. Exit edit mode
3. Return from product surface to previous product surface/Home
4. Navigate web history back
5. Close current tab or leave activity according to the final browser policy

Back behavior must be centralized; individual composables should not invent
conflicting rules.

## 8. Deep links and restoration

The following destinations require stable route IDs and restoration:

- Home
- Bookmarks
- Tabs
- Downloads
- Extensions
- Privacy Center
- Settings
- Qur’an reader/search
- Prayer Times
- Qibla
- Dua
- Daily Wisdom saved items

After process death, restore the selected browser tab and its page state. Modal
sheets do not need restoration unless they contain unsaved user input.

## 9. Accessibility

- Bottom destinations expose selected state and localized labels.
- The tab-count badge announces a localized sentence, not a bare number.
- Sheets move focus to their heading and restore focus to the opener on close.
- Qur’an/Web search modes expose radio/tab semantics.
- No destination relies on icon recognition alone.
- At 200% font scale, labels may truncate visually, but accessible names remain
  complete.

## 10. Implementation impact

The existing `ToolbarItem` universe is Back, Forward, Home, Tabs and Menu. The
approved design changes the conceptual bottom surface to six destinations.
Implementation therefore requires:

- a new versioned navigation/toolbar model;
- migration of stored toolbar configuration;
- updates to customization UI and tests;
- new real routes for Downloads and Extensions;
- a clear home-vs-page toolbar state machine;
- device verification before replacing the current authored model.

This is an intentional architecture change, not a Compose-only icon edit.
