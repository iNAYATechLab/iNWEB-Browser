# iNWEB Brand & Logo

Brand identity documentation: naming, the launcher logo, its derivation into
Android icon assets, the recorded QA, and the regeneration runbook.

## Naming (ADR-038, author-directed)

| Surface | Value |
|---|---|
| Launcher / app label | **iNWEB** (one word — avoids launcher truncation, Chrome/Opera-style) |
| Full product name (store listings, legal) | **iNWEB Browser** |
| Android package id | `com.inweb.android` — set via Chromium's rebranding GN argument `chrome_public_manifest_package` (no upstream source change, rebase-proof) |

## Logo v1

**Provenance.** The v1 logo mark is the author's own design, provided as a
transparent-background PNG (`1495 × 1495`, RGBA) on 2026-09-22 and adopted by
explicit author decision ("প্রথম লোগো হিসেবে ব্যবহার কর"). The mark's opaque
bounding box is `(237, 281, 1259, 1257)` → a `1022 × 976` subject.

**Selection record.** Before adoption, two AI-refined candidate variants
(A: faithful refinement, B: minimal two-hue redesign) were generated and
compared against the author's design with programmatic launcher-legibility
metrics (contrast, 48 px luminance spread, edge density, hue-family count).
The author chose their own design as v1. The full study is preserved in
[`assets/logo-study-comparison.png`](assets/logo-study-comparison.png)
(variants: [`A`](assets/logo-study-variant-A.png),
[`B`](assets/logo-study-variant-B.png)) as the decision record.

**Master.** The pixel master is preserved at
`assets/logo/inweb_logo_master.png` (2.78 MB). It is the single
regeneration source for every icon asset; nothing else may be used to
regenerate them.

**Format decision.** The shipped Android launcher icons are **PNG rasters**
at fixed densities — the same format every major browser ships in its Android
app (Chrome's `res_chromium_base` mipmaps included) — while SVG masters are an
internal brand-source convenience for other browsers. A 1495 px raster master
covers every needed output (largest: 512 px store icon, 432 px adaptive
layer), so raster→vector conversion was rejected as lossy and unnecessary.

## Asset derivation spec (patch 0003)

Background gradient (top → bottom): `RGB(0, 23, 85)` navy → `RGB(0, 105, 140)`
teal, identical for legacy and adaptive icons.

| Asset | Densities | Size | Mode | Layout |
|---|---|---|---|---|
| `app_icon.png` (legacy round) | mdpi…xxxhdpi | 48/72/96/144/192 | RGBA | circle-cropped (supersampled ×8 antialiased mask), transparent corners; mark at **63 %** of canvas |
| `layered_app_icon.png` (adaptive fg) | mdpi…xxxhdpi | 108/162/216/324/432 | P (256-color, no dither) | full-bleed gradient + mark at **57 %** (inside the 66 dp safe zone) |
| `layered_app_icon_background.png` | mdpi…xxxhdpi | 108/162/216/324/432 | P (256-color, no dither) | pure vertical gradient |

The adaptive foreground is full-bleed (it already contains the gradient), so
the icon renders identically under every mask shape; the background layer is
the same gradient and is never visually relied upon.

All resampling: Lanczos. Dimensions and PNG modes match the upstream
Chromium files exactly per density (rebase-safe binary replacement).

Final renders for visual reference:
[`app_icon-xxxhdpi-192.png`](assets/app_icon-xxxhdpi-192.png),
[`layered_app_icon-xxxhdpi-432.png`](assets/layered_app_icon-xxxhdpi-432.png).

## QA record (2026-09-22, programmatic — no human vision in the loop)

| Metric | legacy 192 | adaptive-fg 432 | Reading |
|---|---|---|---|
| Center / corner brightness | 81 / 37 | 70 / 37 | subject present, gradient floor mid-dark |
| Center-corner contrast | 17/100 | 13/100 | low — the v1 mark is inherently soft |
| 48 px luminance std-dev | 39 | 36 | moderate small-size structure |
| 48 px luminance range | 23–249 | 24–249 | bright highlights survive downscaling ✓ |

Honest note: the v1 mark is soft/glow-styled, so icon legibility rests on its
bright highlights rather than a hard silhouette. If launcher legibility on
dark wallpapers disappoints in the §48 device matrix, a v2 mark with a
crisper silhouette is the documented remedy (author decision, as v1 was).

## Regeneration runbook

1. Master: `assets/logo/inweb_logo_master.png` (never edit in place).
2. Derive the 15 assets with the spec above (PIL; crop to the alpha bbox,
   scale to fit the target box, composite over the gradient, then
   circle-mask/quantize per asset class).
3. Author the binary patch in a scratch git tree containing the **pristine**
   upstream files (commit pristine **before** editing — an untracked file
   produces an empty diff), then `git diff --binary` after replacement.
4. Round-trip: `git apply --check`, apply, byte-compare all 15, reverse-apply,
   confirm a clean tree.
5. Register/refresh the `0003-launcher-icons` entry in
   `iNWEB_PATCHES/MANIFEST.yaml`; run `scripts/lint_manifest.py` and the
   `apply_patches.py` apply/verify/PEEL E2E.

## Pending brand work

- **Monochrome themed icon** (`themed_app_icon.xml`, Android 13+): done — patch
  0004 traces the v1 mark's silhouette (2 outer contours + 1 counter, evenOdd)
  into the upstream vector slot; preview:
  [`assets/themed-icon-silhouette-preview.png`](assets/themed-icon-silhouette-preview.png).
- **512 × 512 store icon** at Play Store submission time (derived from the
  same master).
- Non-English in-app strings fall back to English until iNWEB ships its own
  translations (bn infrastructure already exists).
