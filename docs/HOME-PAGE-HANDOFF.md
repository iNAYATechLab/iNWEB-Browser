# Home Page — Handoff for the Junior Developer

Read this before writing code. It is the short path through a project that
is unusual in one important way: **the Chromium source is not in this
repository.**

---

## 1. What iNWEB is

A production Android browser built on **real Chromium**, pinned at
`154.0.8037.21` — not a WebView wrapper, not an SDK (MASTER-SPEC §71).
Brand: "iNWEB Browser", launcher name "iNWEB" (ADR-038), dark Material 3,
emerald/teal primary, subtle gold secondary. Positioning: privacy-focused
Islamic browser — Home = search + Islamic utilities (Qur'an, Hadith,
Prayer Times, Qibla, Dua) + normal browsing.

Current state (2026-09-25): Steps 1–50 done, `v1.0.0-alpha.1` released,
Stage-2 patch series **12/24 authored**, and the first real Android
compile chain is running (hop-24 reached `[64/1625]` with every iNWEB
source compiling; no APK yet).

## 2. How code reaches the browser (the part that surprises everyone)

```
src/native/<module>/*.{h,cc}        <- C++ authored HERE
src/core/<module>/*.kt              <- pure-JVM logic authored HERE (tests: scripts/validate_kotlin_core.sh)
        |
        |  Lead generates / regenerates
        v
iNWEB_PATCHES/<group>/00NN-*.patch  <- THE AUTHORITY for the Chromium tree
        |
        |  CI: b001-verify applies the series; b001-build-hop compiles for real
        v
   Chromium tree (GitHub Actions runner only — never on your machine)
```

Consequences for you:

- **You cannot compile or run the browser locally.** Nobody can; it builds
  on GitHub Actions in chained 6-hour hops. Local verification is the
  Kotlin core tests, the Python tests, and the patch-apply harness.
- **Editing a file under `chrome/...` is impossible here** — those paths
  exist only inside patches. Your Android-side code goes to the Lead, who
  turns it into patch entry `ui/0026-home-page`.
- Anything you put in `src/core/home/**` is testable locally today. That is
  why decision **D1** puts the logic there.

## 3. Repository map (what you will actually touch)

| Path | What it is |
|---|---|
| `src/core/home/**` | **Yours.** New pure-JVM Home Page module + tests. |
| `docs/design/inweb-prototype/**` | **Yours.** Interactive prototype (`index.html`, `styles.css`, `app.js`) + `DESIGN-HANDOFF.md`, `DESIGN-TOKENS.json`, `NAVIGATION-SPEC.md`, `INTERACTION-STATE-SPEC.md`, `VISUAL-QA-CHECKLIST.md`. |
| `src/core/browser-shell/**` | Existing: browser shell state (17 sources, 16 test files). Read it before modelling Home — reuse, don't duplicate. |
| `src/core/tracking-protection/**` | The reference example of a core module done right (19 sources, 14 test files). |
| `docs/MASTER-SPEC.md` §38 | The Home/New Tab requirement: search, shortcuts, bookmarks, recent pages, privacy status, downloads, customizable widgets/cards. |
| `docs/design/BRAND.md` | Naming (ADR-038), logo/asset derivation, QA record. |
| `docs/ACCESSIBILITY-AUDIT.md` | Accessibility rules your UI must satisfy. |
| `iNWEB_PATCHES/`, `src/native/`, `scripts/`, `.github/` | **Lead-owned — do not edit.** |

## 4. Working rules

1. **Branch:** `feature/home-<topic>`; push there, never to `main`. No pull
   requests (the repo has none; the Lead reviews the branch diff and lands
   it).
2. **Pure-JVM first.** `src/core/home` must not depend on Android, Chromium
   or the network. Same discipline as `tracking-protection`: a decision
   core that can be unit-tested on any JVM.
3. **Tests ship with code.** New behaviour without a test is not done. Run
   `bash scripts/validate_kotlin_core.sh` (pinned kotlinc 2.4.20 + JUnit
   4.13.2) before you signal ready.
4. **No hardcoded user-visible strings.** Every string goes through the
   strings pipeline (`scripts/validate_strings.py`,
   `validate_localization.py`).
5. **Honesty is a hard requirement (§57).** If a card shows a count, it
   must be a real count. No placeholder numbers, no "coming soon"
   surfaces, no claims the build cannot support. The prototype is a design
   artefact — shipping its demo data as if it were live state is a
   §57 violation.
6. **No WebView, ever (§71).** Home is a Chromium native surface.
7. **Do not touch core files** to make Home work. Need a core change? Raise
   it; the Lead makes it or refuses it with a reason.

## 5. Suggested build order

1. Read MASTER-SPEC §38 + the prototype handoff docs; list the cards that
   map to §38 items.
2. Model the data: search entry, shortcuts, bookmarks, recent pages,
   privacy status, downloads, widget/card configuration — as plain Kotlin
   types in `src/core/home`, with the state machine (what shows when,
   empty states, ordering, limits).
3. Tests for each behaviour, including empty/error/disabled states.
4. Explain the mapping Home → existing cores (`tracking-protection` for
   privacy status, `offline` for saved pages, `customization` for
   user-configured widgets) so the Lead can wire it without guessing.
5. Signal "ready for review" with: branch name, module path, test count,
   and the list of files you touched.

## 6. Review checklist the Lead will run

- [ ] Only Junior-owned paths changed (see TEAM-WORKFLOW.md §1)
- [ ] `validate_kotlin_core.sh` green, **414 + new** tests
- [ ] Python suite (80) and string/localization validators green
- [ ] Patch series still applies and verifies (`lint_manifest`, `verify`)
- [ ] No WebView, no fabricated data, no unbacked claim (§57/§71)
- [ ] Strings externalized; dark-M3 tokens from `DESIGN-TOKENS.json`, not
      ad-hoc colours
- [ ] Accessibility requirements from `ACCESSIBILITY-AUDIT.md` respected
- [ ] No duplicated logic that already exists in `browser-shell`,
      `tracking-protection`, `offline`, or `customization`

Integration instructions (how this becomes `ui/0026-home-page` and where
it hooks into the Chromium tree) are issued after this checklist passes.


---

## Lead decisions — 2026-09-25

Answers to the eight questions the Junior raised when the Home core merged.
These are Lead decisions, recorded here so they do not live only in a chat
thread. Where a fact is quoted it was verified against the pinned tree
(`154.0.8037.21`) or this repository, not recalled.

### A1 — Integration handoff and hook paths

Two layers, and a prerequisite that is new information:

**Authored app layer (this repository, Lead-owned, Compose + Material 3):**

| Path | Role |
|---|---|
| `src/android-app/src/main/kotlin/com/inweb/browser/ui/HomePage.kt` | the current Home; the approved Home replaces it |
| `.../ui/BrowserScreen.kt` (line ~50) | renders `HomePage` for `inweb://home` |
| `.../ui/OmniboxBar.kt` | **the single authoritative input** (see A3) |
| `.../ui/BrowserBottomBar.kt` | navigation (see A4) |
| `.../BrowserViewModel.kt` (lines 139-144, 480-483) | `homeShortcuts` / `homeRecent` / `homeBookmarks`, `refreshHome()` |

**Chromium tree (patch), verified at tag `154.0.8037.21`:**

- new sources under **`chrome/android/inweb/home/`** — the established
  convention for iNWEB code in the tree (cf. `chrome/android/inweb/adblock/`),
  with its own `BUILD.gn` and deps wiring into `chrome/android/BUILD.gn`
- Compose is **available** in the pinned tree — `third_party/androidx/build.gradle.template`
  pulls `androidx.compose.runtime`, `.material3:material3`, `.foundation`,
  `.foundation-layout`, `.ui`, `.ui-text`, `.ui-graphics`, `.ui-unit`,
  `activity-compose`, `fragment-compose`, `lifecycle-*-compose`,
  `navigation-compose` — so the Compose authored layer is buildable in
  principle. **How the GN target wires the Compose compiler plugin is NOT
  verified yet** and will be proven by a probe build hop before any Home
  code is written against it (no PASS-by-assumption).
- NTP / homepage hooks (all confirmed present at the tag):
  `chrome/android/java/src/org/chromium/chrome/browser/ntp/NewTabPage.java`,
  `NewTabPageCoordinator.java`, `NewTabPageLayout.java`,
  `NewTabPageManager.java`, `NewTabPageLayoutProperties.java`,
  `NewTabPageLayoutViewBinder.java`;
  `.../browser/homepage/HomepageManager.java`, `HomepagePolicyManager.java`
- strings: `chrome/browser/ui/android/strings/android_chrome_strings.grd`
  (as in `0002`); resources: `chrome/android/java/res_chromium_base/`
  (as in `0001`/`0003`/`0004`)

**Prerequisite (Lead, new): nothing injects `src/android-app` into the
Chromium build yet.** No registered patch carries `.kt`/`.java`
(`iNWEB_PATCHES/` contains only resource, string, and native C++ patches),
so the authored Kotlin — 23 files — compiles nowhere today and is **not in
the shipped APK**. The app-layer build injection therefore lands as its own
patch, proven by a probe hop, **before** `ui/0026-home-page` can mean
anything on a device.

**Handoff the Junior owes the Lead** (Junior still writes no patch file, D2):
the view code under `src/android-app`, the list of `chrome/...` paths it
touches, the string ids with `en`/`bn` text, the data contract (which
`HomePageModel` fields the renderer consumes and in what order), and any
native/JNI need.

### A2 — Bookmarks, Recent Pages, Downloads: sections or destinations?

**Both, under one rule.** MASTER-SPEC §38 wins over the design on conflict
(D3), and §38 lists bookmarks, recent pages and downloads as options of the
new-tab experience — so they must be reachable *on* Home, not only behind
navigation.

- Home shows **bounded, read-only snapshots**: top N items + a count + a
  "See all" affordance for each of the three.
- A section with no data is **hidden entirely** (ADR-017 — no empty shells,
  no placeholder counts).
- The full management surfaces stay **navigation destinations** in the
  approved bottom-nav order.
- Data comes only through `HomeAdapters` over the existing
  `HistoryStore` / `BookmarkStore` / `DownloadsStore` / `OfflineLibrary` —
  no duplicate ranking, no second copy of `TopSites.compute`.
- Privacy Center stays in the header (design §3): never duplicated as a
  bottom card, never showing a number that is not real state.

### A3 — One omnibox, one input path

**`OmniboxBar` is the only text input owner. The Home search treatment is a
presentation of it, not a second input.**

- In Home state the toolbar omnibox takes a Home visual treatment
  (centered, rounded, branded); the Home search surface is a
  **focus-forwarding affordance** — tapping it focuses the omnibox. No
  second `EditText`, no second classification path.
- Tapping through keeps one IME, one suggestion source, one incognito
  behaviour, one privacy path. Two inputs would silently fork all four, and
  the design handoff itself forbids "two competing omniboxes".
- `HomeSearch` (core) stays as the pure classification/intent layer
  delegating to `OmniboxParser` — that is correct and unchanged; the rule is
  about the **widget**, not the logic.
- Qur'an mode is a **mode selector** on the same omnibox (routed intent),
  not a separate field — and it stays hidden until a real provider exists
  (A6).

### A4 — Bottom navigation

The approved order is authoritative: **Home, Bookmarks, Tabs, Downloads,
Extensions, Menu**.

- **The migration is Lead-owned and is NOT part of `ui/0026`.** It is a data
  migration, and it lands as its own patch with its own build verification:
  today's model is `ToolbarItem{BACK, FORWARD, HOME, TABS, MENU}`
  (`src/core/customization/.../ToolbarConfigurator.kt`, lines 23-35)
  persisted in the `inweb_toolbar` preferences through
  `SharedPreferencesToolbarStore`, with ADR-029 corruption recovery.
- Migration shape: versioned preferences (schema bump), old keys mapped to
  new, old rows retained for rollback, unknown/new items falling back to the
  authored default, and the **Extensions destination hidden until a real
  binding exists** (`0013`-`0016`) — hidden, never fake-enabled.
- Until that patch lands, Home stays reachable through the existing `HOME`
  toolbar item and `inweb://home`.

### A5 — Durable storage for Quick Access and widget configuration

Lead-owned, following the existing seam/adapter pattern:

- a new seam **`HomeStore`** plus adapters (`FileHomeStore` for the canonical
  JSON snapshot; a `SharedPreferences` adapter only if the payload stays
  genuinely small)
- **atomic save:** write to a temp file → `fsync` → atomic rename; the
  previous good file is kept briefly; a half-written file is never left behind
- **corruption recovery:** parse/validation failure → authored default (empty
  Quick Access, canonical section order), the unreadable file is set aside
  for diagnosis, and the repair is reported through `lastRecovery` — the
  ADR-029 pattern already used by `ToolbarStore`. Never a crash, never a
  silent loss (§50/§51).
- **`docs/STORAGE-INVENTORY.yaml` rows are added by the Lead** (seam +
  adapters, with corruption and clear semantics — clear = app reset, since
  this is user configuration, not browsing data). CI enforces it through
  `scripts/validate_storage_inventory.py`.
- The Junior's side stays pure: keep returning **canonical wire snapshots**
  (the current design is right) and take the persistence port as a
  constructor parameter so it is unit-testable with a fake. Do not add
  repository or platform storage classes to `src/core/home`.

### A6 — Islamic features: what is real today

**None is integration-ready.** There is no provider, no licensed content, no
API key, and no reviewed URL list in this repository.

| Feature | State | What it needs to become real |
|---|---|---|
| Curated Islamic websites | closest | a reviewed, cited HTTPS list from product — a list, not an integration |
| Prayer times | feasible offline | astronomical computation: a chosen calculation method, reference validation, location handling (permission + privacy), tests |
| Qur'an search / reader, Continue Reading | hidden | licensed text + a real provider |
| Voice search | hidden | Android speech recognition + permission + a privacy review of where audio goes |
| Daily Wisdom | hidden | a reviewed, cited content source |

Rule (§57, ADR-037): **a feature ships only with a real provider; until then
it is hidden, not shown as disabled or "coming soon".** A permanently
unavailable tile is a fake affordance, and a fake success is worse than an
absent one. The sealed states in `HomeContent` (loading / unavailable /
error / first-use) exist for features that *are* real and can fail — not as
a way to display things we cannot deliver.

### A7 — `feature/home-prototype`

**It stays the reference/design branch; it is not landed as app code.**
Three reasons: it is the D3 design source, it carries a whole-repo snapshot
(workflows, docs) that would collide with `main`, and landing a
non-building prototype tree into `main` would pollute the build path.

I do not touch that branch (standing directive) and I read it from the
branch. Its design artifacts
(`COMPONENT-INVENTORY.md`, `CONTENT-LOCALIZATION-SPEC.md`,
`DESIGN-HANDOFF.md`, `DESIGN-TOKENS.json`, `INTERACTION-STATE-SPEC.md`)
are **not** mirrored on `main` today — only
`docs/design/inweb-prototype/HOME-IMPLEMENTATION-PLAN.md` is. Mirroring them
is the owner's move, not the Lead's: `docs/design/inweb-prototype/**` is
Junior-owned, so I will not write there. On conflict, MASTER-SPEC §38 wins
(D3).

### A8 — Stale status rows

Checked before answering: no "not started" text remains in either document —
the live-status table was refreshed when the Home core merged. What still
needed fixing, and is fixed with this commit: the row
"`ui/0026-home-page` — Lead, blocked until the Android view layer exists"
was vague, so it now names the real blockers (the app-layer build injection
plus the Junior's view code), and these eight decisions are recorded here
instead of living in a chat thread.

### A9 — Review of the Junior's execution plan (2026-09-25)

The Junior proposed: branch `feature/home-renderer-draft` from latest
`main`, an Android renderer draft, the Lead handoff artifacts, mirroring
the five design artifacts, and skipping `extension/0013`-`0016`.
**Approved, with the following conditions.** They exist because the plan
is sound but crosses one ownership line and makes one assumption that is
right for the renderer and wrong for one field.

**1. Ownership carve-out (explicit, narrow, revocable).**
The renderer code lands under **`src/android-app/src/main/kotlin/com/inweb/browser/ui/home/`**
— a new package, Lead-owned territory, opened for this work only:

- **new files only** — no edits to any existing file under `src/android-app`
  (no `BrowserScreen`, no `BrowserViewModel`, no `OmniboxBar`, no
  `BrowserBottomBar`); wiring the single call site is the Lead's move
- **no resource changes** — strings are delivered as a catalog, and the
  Lead lands them into `src/android-app/src/main/res/` so the localization
  and string gates stay under one owner
- the draft must compile as a **standalone composable** over immutable
  state plus lambdas: `HomePageModel` in, callbacks out. If it needs a
  change to existing Lead-owned code to build, that is a finding to
  report, not a change to make.

**2. The string catalog is a blocking deliverable, not a nice-to-have.**
The app-layer build injection carries `src/android-app` into the Chromium
tree, and a patch that references `R.string.*` ids that do not exist will
not compile. The catalog (id + `en` + `bn`) must therefore reach the Lead
before the injection patch is generated, or the Home sources land one hop
later.

**3. JNI: `expected none` is accepted for the renderer, with one recorded
exception.** No JNI exists anywhere in `src/android-app` today (no
`external fun`, no `System.loadLibrary`). The Home privacy summary is fed
by `SecurityCenterModel`, a pure-Kotlin data class built from the Kotlin
engine's statistics — so the renderer needs no bridge.

The exception: the count's **source of truth on a device is C++**, not
Kotlin. Enforcement happens in `chrome/android/inweb/adblock/`
(`0005`-`0007`), and that native engine exposes no counter today. A
Kotlin-side counter on a device where blocking happens in C++ would count
nothing while displaying a number — a fabricated figure (§57). So:

- until a Lead-owned bridge exists, Home shows the **state** (Active /
  Partial / Unavailable) and **no numeric count**
- the bridge, when built, is Lead-owned and is not part of `ui/0026`

**4. The renderer knows nothing about providers.** It renders what the
model exposes. Which Islamic features are available is a `HomePageModel`
decision, not a UI constant — so when a real provider appears the UI
lights up without a renderer change, and until then nothing is hardcoded
hidden in the view layer.

**5. Sequencing.** The injection patch (Lead) lands first and carries
`src/android-app` into the tree; the renderer's new files are picked up
when the Lead regenerates that patch. `src/android-app` stays the single
source of truth — the tree copy is generated, never edited.

**6. Agreed:** `extension/0013`-`0016` is Lead-owned and not a
prerequisite for Home; skipping it is correct.

**Review checklist the Lead will apply to the branch:**

- [ ] diff touches only `src/android-app/.../ui/home/**` (new), `src/core/home/**`,
      `docs/design/inweb-prototype/**` — nothing else
- [ ] `scripts/validate_authored_structure.sh` green (structural kotlinc gate)
- [ ] `scripts/validate_kotlin_core.sh` green — 466 Kotlin tests
- [ ] Python 80, storage inventory, manifest lint, string/localization parity
- [ ] string catalog present with `en` + `bn` for every id the draft uses
- [ ] data contract maps every rendered field to a `HomePageModel` field
- [ ] callback contract: no navigation performed inside the renderer
- [ ] provider-gated features absent from the view, driven by the model
- [ ] no numeric privacy count anywhere in the draft
- [ ] accessibility: 48dp targets, headings, focus order, 200% font scale,
      dark Material 3

### A10 — What the renderer may import (verified against the pinned tree)

Before writing the renderer draft, know what the real build can actually
resolve. This is checked against `third_party/androidx/BUILD.gn` at
`154.0.8037.21`, not assumed.

**Available** (GN targets under `//third_party/androidx`):

```
androidx_compose_runtime_runtime_java            androidx_compose_ui_ui_java
androidx_compose_runtime_runtime_saveable_java   androidx_compose_ui_ui_graphics_java
androidx_compose_runtime_runtime_retain_java     androidx_compose_ui_ui_geometry_java
androidx_compose_runtime_runtime_annotation_java androidx_compose_ui_ui_text_java
androidx_compose_foundation_foundation_java      androidx_compose_animation_animation_java
androidx_compose_foundation_foundation_layout_java
androidx_compose_material3_material3_java        androidx_compose_material3_material3_ripple_java
androidx_activity_activity_compose_java          androidx_lifecycle_lifecycle_*_compose_java
androidx_navigation_navigation_compose_java      //third_party/kotlin_stdlib:kotlin_stdlib_java
```

**Not available — and this one is load-bearing:** `androidx.compose.material.icons.*`
does not exist in the pinned tree. The string `icons` appears **zero**
times in the 4,422-line `third_party/androidx/BUILD.gn`.

- `androidx.compose.material.*` (Material 2) is likewise absent; only
  `material_ripple` is present.
- Consequence for the renderer draft: **do not import material icons.**
  Use our own drawables under `src/android-app/src/main/res/drawable/`
  with `painterResource(...)` — which is also what the design asks for
  ("individual image-based icons or known site marks on small tiles").
- Consequence for the Lead work: `androidx.compose.material.icons` is used
  in **12 of the 23** authored Kotlin files today (41 import lines,
  including `OmniboxBar.kt` and `BrowserBottomBar.kt`), so the existing
  app layer cannot compile in the Chromium build until those are replaced.
  That replacement is Lead-owned and precedes the full app-layer
  injection.

**Build mechanism (verified, not assumed):** `android_library {
enable_compose = true }` → `java_library_impl` → `compile_kt`, which
appends `--compiler-plugin-jar
//third_party/kotlinc/current/lib/compose-compiler-plugin.jar` to kotlinc
(`build/config/android/internal_rules.gni`, lines 3280-3286 and
4033-4034). This is what patch `0013` probes in the real build.

### A11 — The app-layer build path is proven (hop 29, 2026-09-25)

Patch `0013` asked one question of the real build: does the authored
Kotlin + Compose app layer compile inside Chromium? It does.

```
[11/2646] ACTION //chrome/android/java/src/org/chromium/chrome/browser/inweb/app:inweb_app_java__compile_kt(...)
  ** androidx.compose.ui.text.style.TextAlign (needed by org.chromium.chrome.browser.inweb.app.InwebProbeKt)
  ** androidx.compose.ui.text.TextStyle            (needed by ...InwebProbeKt)
```

- `gn gen` passed (the allowlist assert no longer fires), `compile_kt`
  ran for the iNWEB target, and the generated code references Compose
  runtime types — so the Compose compiler plugin ran, not merely kotlinc.
- 0 compile errors; the hop reached `[220/1534]` before its time box and
  packaged resumable state, so the APK is still to come in hop 30.
- Two upstream facts now recorded in the manifest and worth remembering:
  Kotlin in Chromium is gated by a four-pattern allowlist that we extend
  by one path, and `androidx.compose.material.icons` does not exist in
  the pinned tree.

What follows, in order: replace material-icons usage in the 12 authored
files, land the 59 catalog strings, then inject `src/core` + the real
sources. The renderer draft (PR #3, merged) is the view layer those
depend on, and it was reviewed against this reality — no material icons,
state-only privacy, focus-only search, model-driven provider gating.
