# iNWEB Home — Interaction and State Specification

**Status:** design handoff  
**Scope:** Home/New Tab components, sheets, navigation and user feedback  
**Implementation rule:** every success/status state must be backed by real data

## 1. State language

Every interactive component should use this common state vocabulary where
applicable:

| State | Meaning | Visual response |
|---|---|---|
| Default | Available and idle | Standard content and surface treatment |
| Focused | Keyboard/accessibility focus | 2dp primary focus ring; no layout shift |
| Pressed | Pointer/finger is down | Subtle tonal elevation/brightness change |
| Selected | Current mode/destination/value | Primary container + selected semantics |
| Loading | Real work is in progress | Local progress treatment; preserve context |
| Success | Operation completed | Brief confirmation; do not block interaction |
| Empty | Valid data source has no records | Purposeful empty message/action |
| Offline | Network-dependent operation unavailable | Preserve cached/local content and explain |
| Permission required | Capability needs runtime permission | Explain before requesting permission |
| Error | Operation failed | Human-readable cause, retry where meaningful |
| Disabled | Action is intentionally unavailable | Lower emphasis plus accessible reason |

A component must not use `Success` for a simulated action in production.

## 2. Feedback hierarchy

Use the least disruptive feedback that communicates the result:

1. Inline state change
2. Snackbar for short confirmation/reversal
3. Banner for persistent offline/degraded state
4. Dialog only for destructive, sensitive or permission-explanation decisions
5. Full screen only when the destination itself is the task

Prototype toast messages map to Material 3 Snackbar behavior in production.

## 3. Header

### Privacy Center action

| Condition | Label/status | Action |
|---|---|---|
| Real protection state available and active | Privacy Center + active dot | Open center |
| Available but partially disabled | Privacy Center + warning-toned dot | Open center with affected items first |
| State loading | Privacy Center, no fabricated dot | Open disabled until state resolves or show local progress |
| State unavailable | Privacy Center + unavailable status | Open explanation/retry surface |

Do not collapse multiple independent protections into a `100` score unless a
reviewed, deterministic scoring model exists.

### Overflow

- Opens Menu sheet.
- Focus moves to Menu heading/first action.
- Closing restores focus to the overflow button.
- Tapping outside closes unless a destructive confirmation is active.

## 4. Search and omnibox

### Web/Qur’an mode selector

| Event | Result |
|---|---|
| Select Web | Web selected; placeholder becomes `Search or enter address` |
| Select Qur’an | Qur’an selected; placeholder becomes a localized Qur’an query prompt |
| Switch mode with text present | Preserve text; do not submit automatically |
| Back from focused empty field | Dismiss keyboard, preserve selected mode |

The mode group exposes tab or radio semantics and announces the selected mode.

### Input submission

#### Web

1. Trim input.
2. Empty input: retain focus; no error dialog.
3. URL-like input: route through the real URL parser.
4. Other input: route through the selected search engine.
5. Show page-loading state in the authoritative browser toolbar.

#### Qur’an

1. Trim and normalize Arabic/Latin input without altering user-visible text.
2. Search the real Qur’an index/provider.
3. Show native results with surah/ayah identity and translation source.
4. Empty results: show a truthful no-results state with query-edit action.
5. Offline: use an installed/local index or explain the dependency.

### Voice

| State | UI |
|---|---|
| Permission not requested | Tapping opens pre-permission explanation, then system request |
| Listening | Primary microphone, pulsing indicator if motion allowed, live announcement |
| Processing | Stop pulse; local progress |
| Result | Populate field; do not submit without the chosen product policy |
| No speech | Inline message and retry |
| Permission denied | Explain typing still works; offer Settings route after permanent denial |
| Unsupported | Hide or disable with reason; no simulated listening |

## 5. Shortcuts

Approved utilities: Qur’an, Hadith, Prayer, Qibla, Dua, Halal Life and News.

### Behavior

- Tap opens the corresponding native surface or reviewed destination.
- Pressed state affects the artwork tile, not only the label.
- No Add tile.
- No remove/reorder mode in this section unless a later product decision changes
  the curated utility model.
- If a feature is not implemented, do not expose an enabled tile.

### Per-utility state notes

| Utility | Special states |
|---|---|
| Qur’an | Content unavailable, download/update required, last location |
| Hadith | Source collection selected, offline/cached availability |
| Prayer | Location/calculation setup required |
| Qibla | Sensor unavailable, calibration required, location required |
| Dua | Category/list state, saved items |
| Halal Life | Curated destination unavailable/offline |
| News | Feed loading, stale cache, offline |

## 6. Quick Access

### View mode

- Tap a site to navigate through the normal browser path.
- Long press may enter edit mode if platform usability testing supports it.
- `Edit` is always available as an explicit alternative.

### Edit mode

| Action | Result |
|---|---|
| Enter | Tiles show removal affordance and reorder semantics |
| Reorder | Persist only after valid order update; announce new position |
| Remove | Remove after confirmation or provide Snackbar Undo |
| Done | Exit edit mode and persist final order |
| Back | Exit edit mode; preserve committed edits |

Reorder must support accessibility actions, not drag-only interaction.

### Add Site

Fields:

- Name
- Website address

Validation:

- Name is required after trimming.
- URL is normalized by the same browser URL policy.
- Unsupported/dangerous schemes are rejected.
- Duplicate URL policy is explicit: select existing, rename existing, or reject.
- Saving shows the real persisted tile immediately.
- Failure preserves entered values and explains the cause.

### Empty state

Show `Add your first site` with one Add action. Do not fill the list with fake
sites after the user removes defaults unless defaults are an explicit reset
option.

## 7. Popular Islamic Websites

### Tile interaction

- Tap opens the reviewed HTTPS URL.
- Pressed/keyboard focus remains visible over image artwork.
- External navigation uses the current/new-tab policy consistently.
- `View all` opens a complete curated directory, not a placeholder.

### Failure and safety

- Invalid or removed curated entry is hidden by configuration update.
- Offline tap may open cached content where available or show offline feedback.
- A destination blocked by Safe Browsing/protection follows normal warning flow;
  curation never bypasses browser security.

## 8. Continue Reading & Listening

### First-use state

- Eyebrow remains `Reading & listening` or equivalent localized wording.
- Title becomes `Start reading`.
- User chooses surah/ayah or resumes from an imported state.
- No fabricated default verse.

### Resume state

- Real surah/ayah and last-read time.
- Reading progress is derived from stored state.
- Primary action resumes reading.
- Listening action appears only when audio is available.

### Audio states

| State | Card/reader behavior |
|---|---|
| Not playing | Play/Listen action |
| Buffering | Local progress; action becomes Stop/Cancel where supported |
| Playing | Pause action + current ayah; media notification handled by production layer |
| Paused | Resume action |
| Offline without audio | Explain download/stream requirement; reading remains available |
| Failed | Retry; preserve reading state |

Audio and reading progress should not overwrite one another accidentally.

## 9. Daily Wisdom

### Loading policy

- Prefer cached daily content so Home does not jump during startup.
- Refresh without replacing readable content with a spinner.
- If refresh fails, retain the last verified item and label its date where
  required by product policy.

### Save

- Unsaved: outline bookmark.
- Saved: filled/gold bookmark plus selected semantics.
- Save writes to a real store before success feedback.
- Failure restores previous icon state and shows an actionable message.

### Share

- Use the Android Sharesheet.
- Shared payload includes text, source/citation and optional canonical link.
- Never omit the citation.
- Cancellation is not an error.

### No-content state

Hide the card or show a neutral unavailable message. Never substitute an
uncited quotation.

## 10. Prayer Times

### Setup states

| Condition | Card behavior |
|---|---|
| No location configured | Explain manual/automatic choice; no sample times |
| Automatic permission available | Show real location and schedule |
| Permission denied | Offer manual location; no repeated automatic prompt |
| Calculation preferences incomplete | Use documented default and make method discoverable |
| Provider/calculation error | Keep last schedule with stale indication or show setup error |

### Countdown

- Recalculate from wall clock and time zone.
- Update minute display without causing full-card recomposition where possible.
- At prayer time, move to the next valid prayer according to documented rules.
- Day rollover must select next-day Fajr correctly.
- Device time/time-zone changes trigger a refresh.

### Row interaction

A prayer row may open reminder settings or detail. If no row action exists,
render it as non-clickable content rather than a button with no result.

## 11. Bottom navigation

### Selection

- Exactly one destination is selected where destination semantics apply.
- Tabs badge reflects real tab count.
- Re-selecting Home scrolls Home to top.
- Re-selecting the active non-Home destination follows platform convention:
  retain state or scroll to top, decided consistently.

### Extensions availability

| State | Destination |
|---|---|
| Binding implemented and verified | Enabled |
| Compiled out/unsupported | Hidden, preferred |
| Temporarily unavailable | Disabled with accessible explanation |

Do not show a functioning visual manager backed only by prototype entries.

## 12. Privacy Center sheet

Each row has independent state:

- Enabled
- Disabled by user
- Managed/restricted
- Loading
- Unavailable
- Error

Toggling updates the real policy first, then commits the visual state. If the
operation fails, revert the switch and report the reason. Whole rows use Switch
semantics where the row toggles a boolean.

Private Mode is a navigation action, not a boolean setting. It opens a real
private tab and should use navigation semantics rather than a switch.

## 13. Extensions sheet/surface

### Installed extension states

- Pending review
- Disabled
- Enabled
- Disabled after update pending new permission review
- Error/corrupt

These map directly to the extension core state machine. The UI must not invent
simpler state that bypasses permission review.

### Toggle

- Enable succeeds only after required review.
- Disabling is immediate and persisted.
- Permission-changing updates remain disabled until reviewed.
- Removal requires confirmation and reports whether data is also removed.

### Store/Add

Do not label a destination `Extension store` unless a real reviewed source and
installation path exist. A sideload/import action must display permissions and
origin before installation.

## 14. Menu sheet

- Items vary between Home and page contexts as defined in
  `NAVIGATION-SPEC.md`.
- Selecting an item closes the sheet before navigation.
- Disabled actions include a discoverable reason.
- New Private Tab is visually identifiable but not sensationalized.
- Settings and Privacy Center remain reachable when Home is not active.

## 15. Offline and degraded Home

Home should remain useful offline:

| Surface | Offline expectation |
|---|---|
| Header/navigation | Fully available |
| Web search | Accept input; explain network on submission |
| Local Qur’an search | Available if local index/content is installed |
| Shortcuts | Native/local utilities remain available |
| Quick Access | List available; navigation may fail normally |
| Popular websites | Tiles available; network dependency explained on tap |
| Continue reading | Local reading state/content available according to install policy |
| Daily Wisdom | Last verified cached item |
| Prayer Times | Locally calculated schedule where inputs exist |

## 16. Snackbar messages

Messages are localized, concise and factual. Recommended patterns:

- `Saved to bookmarks`
- `Removed from Quick Access` + Undo
- `Shortcut added`
- `Protection setting could not be changed` + Retry
- `No network connection`

Avoid messages such as `opens here`, `visual demo` or `ready in the full app` in
production.

## 17. Motion

- Tile press: tonal/scale response under 180ms.
- Sheet enter/exit: standard Material motion around 300–360ms.
- Snackbar: platform Material behavior.
- Microphone pulse only while actually listening.
- Prayer countdown does not animate every minute.
- Reduced-motion setting removes decorative scale/pulse transitions while
  preserving state changes.

## 18. Analytics/privacy

Do not add interaction analytics as part of this design implementation. Any
future analytics proposal must follow the repository telemetry policy and must
not record Qur’an queries, reading state, prayer location, private browsing or
sensitive destination history without an explicit governed decision.
