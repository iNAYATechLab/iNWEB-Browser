# iNWEB Home Page — Interactive Visual Prototype

This directory contains the approved static visual exploration for a future
iNWEB Browser Home / New Tab experience. It is a design reference, not a
production browser surface and not evidence that the represented features are
implemented.

## Scope

The prototype explores:

- a dark Material 3-inspired iNWEB visual language;
- browser search and privacy affordances;
- Islamic utilities and curated destinations;
- Quick Access and popular-site tiles;
- continue-reading/listening, daily-wisdom and prayer-time cards;
- Home, bookmarks, tabs, downloads, extensions and menu navigation.

Several controls are deliberately simulated with client-side JavaScript. Status
labels, counters, prayer data, Qur'an progress, extension entries and privacy
states are illustrative only. Production UI must bind every claim to real
application or Chromium state under the repository's truthfulness rules.

## Run locally

From the repository root:

```bash
python3 -m http.server 4173 --directory docs/design/inweb-prototype
```

Then open `http://localhost:4173`.

No dependency installation or network connection is required. All styles,
scripts, icons and image assets are local.

## Files

- `index.html` — accessible static structure and local SVG symbol library
- `styles.css` — responsive presentation and mobile application frame
- `app.js` — prototype-only interactions and demonstration state
- `assets/icons/` — local SVG tile and brand-reference artwork
- `assets/quran-ambient.jpg` — prototype card artwork
- `assets/mosque-dusk.jpg` — prototype header/card artwork
- `assets/logo-mark.jpg` — prototype crop derived from the provided iNWEB logo

## Integration boundary

Do not embed this HTML as a WebView. iNWEB remains a real Chromium-derived
Android browser. Production adoption requires a native Jetpack Compose
implementation, externalized English/Bengali strings, real data providers,
accessibility verification, asset provenance review, and integration through
the tracked Chromium patch/build process.
