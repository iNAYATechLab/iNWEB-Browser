# Native Ad-Block Engine — authoring sources for patch 0005

These are the **authoritative C++ sources** of the iNWEB tracking-protection
/ ad-block decision engine. They are ported 1:1 (behavior-preserving) from
the tested Kotlin reference implementation in
`src/core/tracking-protection/` (116 unit tests, CI-enforced) — that Kotlin
code remains the behavioral specification.

## Why a native port (ADR-040)

The Phase 4 design (see `docs/phases/PHASE4-ADBLOCK-DESIGN.md`) originally
wired the Kotlin engine into the browser via JNI. Verification against the
pinned Chromium tree `154.0.8037.21` showed the tree has **no first-party
Kotlin compilation** (`build/android/gyp/` contains no Kotlin step;
`third_party/kotlin_stdlib` is a prebuilt runtime jar for AAR dependencies;
zero `.kt` sources in `chrome/android/BUILD.gn`). The engine is therefore
ported to native C++ and compiled by the Chromium build directly.

## Layout → patch destination

| Source here | Lands at (patch 0005) |
|---|---|
| `inweb_resource_type.h` | `chrome/android/inweb/adblock/` |
| `inweb_filter_rule.h` | 〃 |
| `inweb_pattern_compiler.{h,cc}` | 〃 |
| `inweb_domain_classifier.{h,cc}` | 〃 |
| `inweb_request_context.h` | 〃 |
| `inweb_rule_matcher.{h,cc}` | 〃 |
| `inweb_filter_list_parser.{h,cc}` | 〃 |
| `inweb_tracking_protection_engine.{h,cc}` | 〃 |

Intentionally **not** ported here: `CombinedMatcher` (Phase 5 performance
deliverable → patch 0012), `CosmeticFilter` (later series, §9 of the design),
and the `lists/` provisioning stack (patch 0007).

## Deviations from the Kotlin reference (documented, deliberate)

- Regexes are **RE2** (`//third_party/re2`) instead of `java.util.regex`;
  the translated patterns are RE2-compatible by construction.
- `DomainClassifier` uses Chromium's full Public Suffix List
  (`net::registry_controlled_domains`) instead of the Kotlin built-in
  suffix table — the documented engine-side integration intent. Edge cases
  (IP literals, `localhost`) follow Chromium PSL semantics, with the
  lowercased host itself as fallback.
- Regexes compile **once at parse time** (the Kotlin version compiled
  lazily); RE2 objects are immutable and thread-safe for matching.
- `EngineStatistics` is guarded by a lock: decisions run on worker-pool
  threads (per the throttle design), so counters must be thread-safe.
- `decide()` uses the **v1 full scan** (`matchingRulesNaive` semantics);
  the combined matcher is patch 0012 — per PHASE4-ADBLOCK-DESIGN §5.

## Regenerating patch 0005

Copy these files into a scratch Chromium tree at
`chrome/android/inweb/adblock/` (plus the `BUILD.gn` registration, authored
with the patch), then `git diff` → `iNWEB_PATCHES/adblock/0005-…patch`
(the scratch-tree procedure in `docs/design/BRAND.md` §Regeneration, but
text files). Round-trip + `apply_patches.py` E2E + `lint_manifest.py`
before registering.
