# Popular Islamic Websites — candidate catalog

**Status: CANDIDATE. Nothing here is shipped, and no URL is hardcoded.**

Content is a product decision, not an engineering one. The catalog was
reviewed down from six proposed sites to three candidates, with two
deferred and one held for the future. Per the agreed process:

- the Lead proposes (display name, canonical HTTPS URL, one-line reason,
  provenance, review date);
- the Junior reviewed UI/content fit, label length, Bengali presentation
  and logo-slot requirements — findings below are **binding**;
- no URL enters the code until final product/security approval;
- until then the model leaves the section hidden, so nothing unreviewed
  reaches a user and the implementation is not blocked.

## Candidates (3)

### 1. Quran.com — QURAN category

| Field | Value |
|---|---|
| Stable ID | `popular_quran` |
| Canonical URL | `https://quran.com/` |
| Locale variant | `https://quran.com/bn` **when the UI is Bengali** |
| Why | Full Qur'an text with translations; the reference most users already trust. |
| Provenance | `curl -L`: `200` for `/` and `200` for `/bn`, 2026-09-25 |
| Artwork | one governed neutral QURAN-category artwork — **shared** by both locale variants |
| Tile label | `Qur'an` / `কুরআন` *(pending Junior confirmation)* |
| Accessibility name | `Quran.com` (full destination name) |
| Language | multi-language, including Bengali |
| Status | **candidate** |

**One logical tile, never two.** The Bengali UI resolves the `/bn`
variant; both variants carry the same stable ID and the same artwork.

### 2. Sunnah.com — HADITH category

| Field | Value |
|---|---|
| Stable ID | `popular_sunnah` |
| Canonical URL | `https://sunnah.com/` |
| Why | The standard English hadith corpus reference; matches the HADITH category directly. |
| Provenance | `curl -L`: `200` with a browser User-Agent, `403` to a bare scripted probe, 2026-09-25 |
| Tile label | `Hadith` / `হাদিস` *(pending Junior confirmation)* |
| Accessibility name | `Sunnah.com` |
| Language limitation | **English destination only** — recorded in the catalog metadata, and the tile must not imply a Bengali destination |
| Status | **candidate** |

### 3. SeekersGuidance — ISLAMIC_QA category

| Field | Value |
|---|---|
| Stable ID | `popular_seekers` |
| Canonical URL | `https://seekersguidance.org/` |
| Why | Structured, cited fiqh and creed answers from an established teaching body. |
| Provenance | `curl -L`: `200`, 2026-09-25 |
| Tile label | `Q&A` / `প্রশ্নোত্তর` (short form — see R2) |
| Accessibility name | `SeekersGuidance` |
| Status | **conditional candidate — not enabled without product, community and security approval** |

**Curation note, and it matters:** SeekersGuidance is a scholarly
institution, not a neutral corpus. Answers reflect that institution's
methodology and school. Any curation policy for this rail must say so
explicitly rather than presenting it as a neutral reference.

## Deferred (with the reason, so this is not re-litigated each time)

| Site | Decision | Why |
|---|---|---|
| IslamicFinder | deferred | A prayer/qibla utility, but `PopularSiteCategory` has no prayer category and it overlaps the planned offline Prayer/Qibla feature. |
| Islamicity | deferred | A broad portal spanning Articles, Prayer and Community at once; without a single clear editorial reason it does not belong in the initial rail. |
| Islamic Foundation Bangladesh | future candidate | Needs stable HTTPS reachability and a human content review first. Our probe returned `000` (no response) on 2026-09-25, which is why it is not proposed. |

**No new sites are being added at this stage.**

## Binding UI/content requirements (from the review)

- **R1 — the renderer must not know providers.** `PopularSite` currently
  carries no display name, so the renderer shows only the category label.
  Before the section is enabled, the model/adapter must carry a
  product-reviewed, localized destination name and an accessibility
  label. Verified today: the renderer hardcodes no URL or domain.
- **R2 — tile labels stay short.** "Islamic Q&A" / "ইসলামিক প্রশ্নোত্তর"
  is risky at 200% font scale. The tile shows `Q&A` / `প্রশ্নোত্তর`; the
  full destination name lives in the accessibility description.
- **R3 — 200% font-scale screenshot tests** for all three tiles. Silent
  truncation after two lines is not acceptable.
- **R4 — locale variants share one artwork.**
- **R5 — no third-party marks.** No wordmark bundling and no runtime
  favicon fetch without trademark/licence approval. Until approval:
  governed neutral category artwork.
- **R6 — every row stays `candidate`** until product and security sign
  off. Only `approved` rows may be hardcoded into a provider.

## Lead finding: `isReviewedHttps` promises more than it checks

`PopularSite.init` requires `HomeWebAddresses.isReviewedHttps(url)`, which
reads as if an approved set were being enforced. Its implementation
(`src/core/home/.../WebAddresses.kt:38`) checks **only** that the scheme
is `https`:

```kotlin
scheme.equals("https", ignoreCase = true)
```

That is a real gap, not a nitpick: the code's name makes a promise about
review that it does not keep, and §57 is about exactly this kind of
honesty. Two honest options, and it must be one of them before the
section is enabled:

1. **Implement the set** — the approved rows of this catalog become the
   reviewed set, and `isReviewedHttps` checks membership. Then the name
   tells the truth and the approval gate is enforced in code.
2. **Rename** to something like `isHttps`, and let the provider own
   approval — with a test asserting the approved list.

Ownership: `src/core/home` is the Junior's (D1); the provider/adapter is
the Lead's. Tracked as a blocking item for enabling the section.

## Machine-readable form (for whichever store is chosen later)

```csv
id,canonical_url,locale_variant_url,category,tile_label_en,tile_label_bn,accessibility_name,artwork_id,language_limitation,review_date,provenance,status
popular_quran,https://quran.com/,https://quran.com/bn,QURAN,Qur'an,কুরআন,Quran.com,artwork_quran,,2026-09-25,"curl -L: 200 (/ and /bn)",candidate
popular_sunnah,https://sunnah.com/,,HADITH,Hadith,হাদিস,Sunnah.com,artwork_hadith,"english_only",2026-09-25,"curl -L: 200 (UA) / 403 (bare)",candidate
popular_seekers,https://seekersguidance.org/,,ISLAMIC_QA,Q&A,প্রশ্নোত্তর,SeekersGuidance,artwork_islamic_qa,,2026-09-25,"curl -L: 200",conditional
```
