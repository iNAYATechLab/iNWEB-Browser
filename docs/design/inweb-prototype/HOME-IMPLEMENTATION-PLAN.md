# iNWEB Home / New Tab — Implementation Plan

**Role:** Junior Developer, Home Page only
**Branch:** `feature/home-core`
**Base:** `origin/main` at `3651fe0`
**Plan status:** implementation gate; no production code was changed before this plan
**Design reference:** `feature/home-prototype` at `2b87dec`

## 1. Inspected architecture

The current authored Android Home is `ui/HomePage.kt`. `BrowserScreen.kt` shows it for `inweb://home`; `OmniboxBar.kt` already owns authoritative URL/search input; `BrowserBottomBar.kt` renders the configured Back/Forward/Home/Tabs/Menu controls; and `BrowserViewModel.kt` already reads real history and bookmarks. The existing browser-shell core owns `OmniboxParser`, `HistoryStore`, `BookmarkStore`, `DownloadsStore`, and `TopSites`; tracking-protection owns `SecurityCenterModel`; offline owns `OfflineLibrary`; customization currently owns toolbar configuration only.

The Junior ownership boundary permits changes only in `src/core/home/**` and `docs/design/inweb-prototype/**`. Android shell files, existing core modules, Chromium/native code, patches, scripts, workflows, and `main` remain Lead-owned.

## 2. Requirements mapped to existing architecture

| Home requirement | Junior core responsibility | Existing/Lead integration source |
|---|---|---|
| Browser identity, logo, responsive dark M3 header | Typed header/privacy state and design-token guidance only | Lead renderer uses governed assets, Material 3 theme, insets, and adaptive layout |
| Privacy Center | Map only real `SecurityCenterModel` fields into Active/Partial/Unavailable states; expose a real blocked count only when enforcement is active | `tracking-protection`; Lead opens the existing Privacy Center route |
| Menu | No new navigation stack | Existing Menu action and centralized browser navigation |
| Web search / URL input | Delegate classification to `OmniboxParser`; never reproduce URL detection | `browser-shell` and the authoritative omnibox/navigation path |
| Qur’an search mode | Typed route intent and availability; never fake results | Lead supplies a real native Qur’an provider before enabling |
| Voice search | Typed availability/listening/processing/denied/error states | Lead owns Android permission and speech-recognition integration |
| Shortcuts | Canonical seven utility IDs: Qur’an, Hadith, Prayer, Qibla, Dua, Halal Life, News; no Add item | Lead maps enabled items to real native routes/reviewed destinations |
| Quick Access | Home-owned validated add/remove/reorder/persistence model, separate from history-derived Top Sites | Lead provides durable store and routes URLs through normal navigation |
| Popular Islamic Websites | Validated stable-ID HTTPS configuration; no hard-coded unreviewed destinations | Lead/product supplies reviewed URLs, localized content, and curation policy |
| Continue Reading & Listening | Sealed first-use/resume/loading/unavailable/error state; no fabricated verse | Future real Qur’an reading/audio store |
| Daily Wisdom | Sealed verified/cached/loading/unavailable/error state; citation required for content | Future reviewed, licensed content source/store |
| Prayer Times | Sealed setup/schedule/stale/loading/error state with calculation metadata; no sample times | Future prayer/location/calculation provider |
| Bookmarks, recent pages, downloads, saved pages | Read-only Home snapshot adapter using existing stores and limits | `browser-shell` + `offline`; no duplicate ranking or storage |
| Widget/card configuration | Canonical section order, configurable visibility, and canonical wire snapshots only | Lead provides durable storage and its storage-inventory entry |
| Bottom destinations | No toolbar/customization edits | Lead must migrate the existing five-item toolbar model before adopting Home/Bookmarks/Tabs/Downloads/Extensions/Menu |

## 3. Planned pure-JVM module

Create `src/core/home` with no Android, Chromium, network, or UI-resource dependency.

### Files and responsibilities

1. `HomeSearch.kt`
   - Web/Qur’an modes and voice states.
   - Blank-input result.
   - Web submissions delegate directly to `OmniboxParser` and the configured `SearchEngine`.
   - Qur’an submissions return a typed intent only when the provider is available.

2. `HomeContent.kt`
   - Canonical shortcut IDs/order and availability.
   - Popular-site model with stable IDs and HTTPS validation.
   - Sealed Continue Reading, Daily Wisdom, and Prayer Times states with strict truthfulness invariants.

3. `QuickAccess.kt`
   - User-owned site list, never derived from history.
   - Add/remove/reorder/restore operations with typed errors.
   - URL normalization delegated to `OmniboxParser`; only valid HTTP(S) web destinations accepted.
   - Duplicate and capacity guards; each successful mutation returns the complete canonical wire snapshot for Lead-owned durable storage.

4. `HomePageModel.kt`
   - Canonical approved order: Search, Shortcuts, Quick Access, Popular Islamic Websites, Continue Reading & Listening, Daily Wisdom, Prayer Times.
   - Required-vs-optional visibility rules and stored widget configuration validation.
   - Search always present; no duplicate Privacy section/ribbon.

5. `HomeAdapters.kt`
   - `SecurityCenterModel` → honest Home privacy summary.
   - Read-only snapshot from `HistoryStore`, `BookmarkStore`, `DownloadsStore`, and `OfflineLibrary`.
   - Reuse `TopSites.compute`; do not copy ranking logic.

6. Unit tests
   - Search delegation for URL, web query, blank input, Qur’an availability, and voice state.
   - Exact shortcut membership/order and proof that Add is absent.
   - Quick Access validation, normalization, duplicate rejection, reorder boundaries, remove/restore, capacity, canonical save snapshots, and recovery from invalid stored data.
   - Privacy active/partial/unavailable mapping and real-count rules.
   - Empty/loading/error/disabled states for cards; citation, progress, schedule, and HTTPS invariants.
   - Canonical section ordering and optional-card visibility/config recovery.
   - Adapter limits and proof that Top Sites/recent/bookmarks/downloads/offline data come from existing stores.

## 4. Rendering contract for Lead integration

The Android/Chromium layer should be a thin renderer over immutable Home state:

- Use one scroll container with canonical section order and 48dp minimum targets.
- Use Material 3 semantic color roles derived from `DESIGN-TOKENS.json`; no ad-hoc per-component palette.
- Expose headings, selected mode, disabled reason, live voice state, and reorder accessibility actions.
- Keep declaration, visual, and focus order aligned; survive 200% font scale; test compact and wide widths.
- Keep all user-visible UI strings in the existing English/Bengali resource pipeline.
- Use header/large-card contextual imagery only; small options use individual artwork or recognizable site marks.
- Do not show placeholder counts, sample prayer times, fabricated verses, uncited wisdom, simulated voice listening, unavailable extensions, or fake success.

## 5. Known decisions requiring Lead review

1. **Current Home vs approved order:** MASTER-SPEC §38/current Home include bookmarks and recent pages, while the approved design order omits separate sections and reaches Bookmarks/Downloads through navigation. The core will preserve these real datasets in a read-only snapshot but will not silently insert extra sections into the approved order. Lead must decide their final visual placement.
2. **Single omnibox:** the current shell renders `OmniboxBar` above Home. The Lead patch must choose the Home visual treatment without creating a second authoritative input.
3. **Bottom navigation:** the approved six-destination bar conflicts with the existing five-item `ToolbarItem` model. Extensions must remain a destination in the approved design, but it must be hidden/disabled until the real binding exists; Menu must remain. This requires a Lead-owned versioned customization migration.
4. **Content providers:** Qur’an, wisdom, reading/audio, prayer, curated-site, and voice providers are not backed by current core modules. The core will model unavailable/first-use states; Lead must not enable them without real integrations.
5. **Durable Home storage:** the core intentionally returns canonical Quick Access/layout wire snapshots instead of adding new repository storage seams, because `docs/STORAGE-INVENTORY.yaml` and the platform adapters are Lead-owned. The Lead must provide durable storage, define failure/atomicity behavior, and add its inventory entry before reporting save success.
6. **Validator registration:** `scripts/validate_kotlin_core.sh` is Lead-owned and currently has an explicit module list. The Junior will run the unchanged canonical validator plus a local equivalent compile/test command for `src/core/home`; the Lead must register the new module (and its browser-shell/tracking-protection/offline dependencies) before landing.
7. **Chromium patch:** only the Lead generates `ui/0026-home-page`, runs patch verification, builds Chromium, and performs device/APK checks.

## 6. Validation and delivery sequence

1. Implement only the planned Junior-owned module and tests.
2. Run a local pure-JVM compile/test for Home with the repository-pinned Kotlin 2.4.20 and JUnit 4.13.2 classpath.
3. Run unchanged `bash scripts/validate_kotlin_core.sh`, Python tests, string/localization checks, manifest lint, storage inventory, and authored-structure validation.
4. Review `git diff origin/main...HEAD` for ownership compliance and fabricated data.
5. Commit once with a clear Home-core message and push `feature/home-core`.
6. Report branch, commit, Home test count, changed files, unresolved Lead decisions, and exact integration steps.
