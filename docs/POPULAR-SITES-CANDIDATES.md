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
| Status | **`conditional_candidate` — held indefinitely. Hidden in the Home model; no bundled logo or wordmark, no production URL wiring, and no security review scheduled.** |

**Curation note, and it matters:** SeekersGuidance is a scholarly
institution, not a neutral corpus. Answers reflect that institution's
methodology and school. Any curation policy for this rail must say so
explicitly rather than presenting it as a neutral reference.

**Why it is held, and by whom.** Showing a site as a *default Home
recommendation* is a product endorsement; being a safe HTTPS destination
does not by itself make something suitable to recommend. That judgement
needs a community/scholarly suitability and editorial-neutrality review,
which is **not** a technical security review and cannot be substituted
for one. The reviewer of this document is a UI/content reviewer — not a
product approver and not a scholarly authority — so that review is
advisory only (label length, Bengali presentation, accessibility and
category fit). No reviewer's name is recorded here, because a name would
have to be invented, and an invented approver is worse than none.

**Governance record — why this is held (2026-09-25)**

| Field | Value |
|---|---|
| `status` | no Product Owner nominated |
| `recorded_on` | 2026-09-25 |
| `decision_owner` | project owner |
| `affected_candidate` | SeekersGuidance |
| `effect` | remains `conditional_candidate` and hidden |
| `technical_security_review` | not initiated |
| `blocking_other_sites` | no |

Rationale, as recorded:

> SeekersGuidance একটি scholarly institution এবং default Home
> recommendation হিসেবে এর inclusion-এর জন্য product/editorial ও
> community-suitability approval প্রয়োজন। Project owner এখনো কোনো
> accountable Product Owner মনোনীত করেননি। তাই candidate-টি প্রত্যাখ্যাত
> নয়, তবে approval chain শুরু না হওয়া পর্যন্ত hidden থাকবে।

No name or role is entered on anyone's behalf: nominating a Product Owner
is the project owner's decision, and the reviewer of this catalog is a
UI/content reviewer without that authority. The Quran.com and Sunnah.com
technical review already completed is **not affected** by this hold
(`blocking_other_sites: no`) — those two proceed on their own track.

**Consulted, 2026-09-25.** The project owner was asked directly and
declined to appoint a Product Owner for now: they act as the owner of this
workflow, but not as Product Owner for content. So the two fields above
are confirmed unchanged — `no Product Owner nominated`, `decision_owner:
project owner` — and no role or date has been invented to fill them.
SeekersGuidance stays hidden until the chain starts; Quran.com,
Sunnah.com and every other already-approved source proceed unaffected.

**Reconsideration triggers** — any one of these reopens the question:

1. The project owner nominates a Product Owner.
2. The nominated Product Owner names a community/scholarly reviewer, or
   establishes a documented approval process.
3. That decision is added to this catalog (or an ADR) with **role, date,
   scope and rationale**.

**Exit criteria — all five, in this order, before this row is enabled:**

1. The project owner nominates an accountable **Product Owner**.
   *(status 2026-09-25: not nominated — see the governance record above)*
2. The Product Owner nominates a **community/scholarly reviewer**, or
   takes documented responsibility for that judgement personally.
3. That reviewer **approves in writing** the site's suitability as a
   default recommendation.
4. Only then does the Lead run the **technical/security review**.
5. Every approval is recorded in this catalog (and the ADR if one is
   opened) with **reviewer role, date, scope and rationale**.

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

## Lead technical/security review — QURAN and HADITH (measured, 2026-09-25)

Measured with `openssl s_client` and `curl -sSI -L` from this workspace.
This is the Lead's part of the review only; it says nothing about content
suitability or editorial neutrality, which is the Product Owner's and the
community reviewer's call.

| | Quran.com (`/` and `/bn`) | Sunnah.com |
|---|---|---|
| TLS issuer | Google Trust Services (WE1) | Let's Encrypt (YE2) |
| Certificate valid until | 2026-12-20 | 2026-12-10 |
| HTTP/2 | yes | yes |
| HSTS | `max-age=15552000; preload` | **absent** |
| Content-Security-Policy | present, but permissive (`'unsafe-inline'`, `'unsafe-eval'`, ~20 third-party script sources) | **absent** |
| `x-content-type-options: nosniff` | yes | absent |
| `referrer-policy` | `origin-when-cross-origin` | absent |
| Reachability | `200` | `200` with a browser UA; `403` to a bare scripted probe |

**What that means, stated plainly.** Both are legitimate HTTPS
destinations with valid certificates; neither fails a security check in
the sense of being unsafe to visit. They differ in posture: Quran.com
sends HSTS with preload and a CSP, Sunnah.com sends neither. Those are
observations about the destinations, not about us, and neither is a
disqualifier — but they are recorded rather than smoothed over.

**One thing we control: tracking on arrival.** Quran.com's policy lists
Google Analytics/Tag Manager, Amplitude, LogRocket, Mouseflow, Clarity,
LinkedIn, Stripe, PayPal and Vercel Insights among its script sources,
and `connect-src *`. We cannot change what a site loads. We *can* say
honestly that these destinations are opened through the ordinary
navigation path, so the ad-block and tracking-protection engine (patches
`0005`-`0007`, `0012`) applies to them like any other page — the same
protection the user has everywhere else, not a special case.

**Status of these two rows: technical review complete (measured);
product and community approval pending. They are not hardcoded and the
section is not enabled.** That does not change until an approver signs
off, which is exactly the discipline the SeekersGuidance hold exists to
protect.

## Machine-readable form (for whichever store is chosen later)

```csv
id,canonical_url,locale_variant_url,category,tile_label_en,tile_label_bn,accessibility_name,artwork_id,language_limitation,review_date,provenance,status
popular_quran,https://quran.com/,https://quran.com/bn,QURAN,Qur'an,কুরআন,Quran.com,artwork_quran,,2026-09-25,"curl -L: 200 (/ and /bn); tech review done 2026-09-25",candidate
popular_sunnah,https://sunnah.com/,,HADITH,Hadith,হাদিস,Sunnah.com,artwork_hadith,"english_only",2026-09-25,"curl -L: 200 (UA) / 403 (bare); tech review done 2026-09-25",candidate
popular_seekers,https://seekersguidance.org/,,ISLAMIC_QA,Q&A,প্রশ্নোত্তর,SeekersGuidance,artwork_islamic_qa,,2026-09-25,"curl -L: 200; no security review scheduled",conditional_hold
```
