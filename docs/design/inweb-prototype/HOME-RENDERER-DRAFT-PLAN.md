# iNWEB Home Renderer Draft — Implementation Plan

**Branch:** `feature/home-renderer-draft`
**Base:** `origin/main` at `fd92474`
**Status:** implementation in progress under the A9 ownership carve-out
**Probe dependency:** app-layer/Compose injection is Lead-owned and may still fail; this draft must not claim device or APK verification

## 1. Authority and temporary handoff

The standing ownership map remains authoritative. The Lead's recorded decisions
A1 and A9 open one narrow, revocable carve-out: the Junior may add **new files
only** under `src/android-app/src/main/kotlin/com/inweb/browser/ui/home/**`.
Existing Android files and all resources remain Lead-owned. The Junior supplies
string IDs as an English/Bengali catalog, not as XML edits. A10 additionally
forbids `androidx.compose.material.icons.*`, which does not exist in the pinned
Chromium tree.

The project owner's latest direction confirms that the renderer draft and A7
design mirrors may proceed while the injection probe runs. The Junior still
does not edit patch files, resources, native code, existing core modules,
scripts, workflows, toolbar customization, or Chromium-tree paths.

## 2. Existing architecture inspected

- `ui/HomePage.kt` currently renders real Top Sites, recent history, and
  bookmarks from `BrowserViewModel`; empty sections are hidden.
- `BrowserScreen.kt` chooses Home for `inweb://home` and owns the scaffold.
- `OmniboxBar.kt` is the only text input and routes through the existing
  `OmniboxParser`; the draft must not create a second text field.
- `BrowserBottomBar.kt` still renders the five-item toolbar model. A4 keeps its
  migration Lead-owned and outside `ui/0026`.
- `BrowserViewModel.kt` currently exposes real Home history/bookmark data and a
  real downloads snapshot, but not a complete `HomePageModel` or Security
  Center summary.
- `src/core/home` supplies immutable renderer state, canonical visibility,
  honest privacy/card states, Quick Access, and real browser snapshots.
- Android sources have no standalone Gradle project. Local verification is
  structural Kotlin validation plus resource/localization gates; real Compose
  compilation is the injection probe/Lead build.

## 3. Renderer draft strategy

Add a new, un-wired thin renderer rather than changing current production
navigation or `BrowserViewModel` during the probe:

- paths: new Kotlin files only under
  `src/android-app/src/main/kotlin/com/inweb/browser/ui/home/`
- input: immutable `HomePageModel`
- actions: explicit callback contract; no `BrowserViewModel` dependency
- no second `TextField`; the Home search card emits `onFocusOmnibox`
- no Material icon dependency; lightweight renderer glyphs use available
  Compose UI graphics until the Lead maps governed drawables
- Material 3 semantic color and typography roles only
- centered responsive content width, compact horizontal rails, 48dp+ targets
- headings and selected/disabled semantics for accessibility
- no animation dependency; no reduced-motion violation
- no demo content, default URLs, sample prayer times, privacy score, or fake
  feature status

The existing `HomePage(viewModel)` remains untouched until the Lead's injection
probe proves the app layer and provides the final wiring point.

## 4. Truthful rendering policy

- Privacy always reflects `HomePrivacyState`; unavailable never becomes
  “Protected.” The renderer shows no numeric count: on-device enforcement is
  C++ and exposes no counter until a separate Lead-owned bridge exists (A9).
- Curated shortcuts render only when not hidden. A disabled state is reserved
  for a real but temporarily unavailable integration.
- Provider-less Islamic features are represented by `Unavailable` and are
  omitted by `HomePageModel.visibleSections`, per A6.
- Quick Access renders only from explicit user-owned entries; its empty Add
  affordance calls a real integration callback.
- Popular sites render only reviewed HTTPS entries supplied by the model.
- Recent pages, bookmarks, and downloads use bounded real snapshots from
  `HomeBrowserSnapshot`, hide when empty, expose the displayed count, and route
  “See all” through callbacks (A2).
- Continue Reading, Daily Wisdom, and Prayer Times render only typed real states;
  citations and calculation metadata are never synthesized.

## 5. String and asset contract

Do not edit Android resources under the A9 carve-out. Deliver every required ID
as a blocking English/Bengali catalog for the Lead to land before generating
the injection/Home patch. Reuse existing IDs where semantics already match.
Dynamic religious content remains data, not UI resources.

The draft does not add app-layer imagery before the Lead establishes the
Chromium resource-copy path. It does not import unavailable Material icons.
The handoff lists governed asset IDs and their prototype sources for the Lead
to map after the probe:

- header ambient image
- brand mark
- per-shortcut artwork
- per-popular-site artwork
- Continue Reading image
- Daily Wisdom image
- Prayer Times header image

Until mapped, the renderer uses semantic Material icons/surfaces without
claiming final visual-asset parity.

## 6. A7 mirror

Mirror exactly these five Junior-owned files from `feature/home-prototype`:

1. `COMPONENT-INVENTORY.md`
2. `CONTENT-LOCALIZATION-SPEC.md`
3. `DESIGN-HANDOFF.md`
4. `DESIGN-TOKENS.json`
5. `INTERACTION-STATE-SPEC.md`

Do not mirror the web prototype, workflows, unrelated repository snapshot, or
Lead-owned files.

## 7. Lead handoff document

Add `HOME-RENDERER-HANDOFF.md` containing:

- Android source/resource paths
- required Chromium hook/resource/string paths from A1
- machine-readable `HOME-STRING-CATALOG.csv` plus the reviewed string table
- `HomePageModel` field-to-component mapping
- action/callback contract
- final wiring steps and known unavailable providers
- native/JNI assessment (expected: none)
- explicit limits of local verification

## 8. Validation

1. `bash scripts/validate_kotlin_core.sh` — 466 tests.
2. `python3 -m unittest discover -s tests -t .` — 80 tests.
3. `python3 scripts/validate_strings.py`.
4. `python3 scripts/validate_localization.py`.
5. `bash scripts/validate_authored_structure.sh`.
6. Storage inventory and patch manifest lint.
7. Verify the five mirrored artifacts are byte-identical to
   `origin/feature/home-prototype`.
8. Ownership diff review; no patch/native/script/workflow/core drift.
9. Commit and push only `feature/home-renderer-draft`; never push `main`.
