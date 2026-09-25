# Home content-contract follow-up plan

**Branch:** `feature/home-content-contract`

**Base:** `origin/main` at `36e815c`

**Status:** implemented; local validation green

**Scope owner:** Junior — `src/core/home/**` and Home renderer semantics only

## 1. Context inspected

- `PopularSite.init` calls `HomeWebAddresses.isReviewedHttps`, but that
  helper validates URL shape and HTTPS scheme only; it cannot prove catalog
  approval.
- Product approval remains a Lead/provider concern. The current catalog has no
  approved rows and the Popular section must remain hidden.
- `PopularSite` carries category, URL, and artwork identity but no reviewed
  destination/accessibility name. The renderer consequently announces the
  generic category rather than the outbound destination.
- Canonical English/Bengali short labels already exist as `Qur’an` / `কুরআন`
  and `Hadith` / `হাদিস`.

## 2. Decision

Choose the honest validation boundary:

1. Rename `isReviewedHttps` to `isHttps`.
2. Keep HTTPS syntax validation in the pure Home model.
3. Leave approved-catalog membership to the Lead-owned provider/adapter, with
   provider tests before the section can be enabled.
4. Add immutable, non-blank `destinationName` and `accessibilityLabel` values
   to `PopularSite`; the provider supplies reviewed locale-appropriate values.
5. Keep the visible tile label category-based and short, but make its semantics
   announce the supplied full destination/accessibility label.

## 3. Planned files

- `src/core/home/src/main/kotlin/com/inweb/browser/home/WebAddresses.kt`
- `src/core/home/src/main/kotlin/com/inweb/browser/home/HomeContent.kt`
- `src/core/home/src/test/kotlin/com/inweb/browser/home/HomeContentTest.kt`
- `src/core/home/src/test/kotlin/com/inweb/browser/home/HomePageModelTest.kt`
- `src/android-app/src/main/kotlin/com/inweb/browser/ui/home/InwebHomeRenderer.kt`
- `docs/POPULAR-SITES-CANDIDATES.md`

No URL catalog will be hardcoded. No resource, Chromium patch, GN, native,
build, CI, extension, persistence, or navigation file will be changed.

## 4. Validation

- canonical Kotlin core suite;
- authored-source structural validation;
- Python, string/localization, storage, and manifest gates;
- focused scans proving no provider URL/domain is added to renderer/core;
- diff ownership review.

## 5. Parallel ownership boundary

Hop-32, mtime verification, Chromium build resume/APK work, and extension
patches `0014`–`0017` remain Lead-owned and are not started or modified on
this branch. This Junior track does not delay those longest-path operations.
