# Popular Islamic Websites — candidate catalog (Lead draft, awaiting approval)

**Status: CANDIDATE. Nothing here is shipped, and no URL is hardcoded.**

The Home section "Popular Islamic Websites" (approved order position 4)
needs content, and content is a product decision, not an engineering one.
This document is the Lead's candidate list for review. Per the agreed
process:

- the Lead proposes (display name, canonical HTTPS URL, one-line reason,
  provenance, review date);
- the Junior reviews UI/content fit, label length, Bengali presentation
  and logo-slot requirements;
- **no URL enters the code until final product/security approval**;
- until then the model leaves the section hidden, so nothing fake or
  unreviewed reaches a user and the implementation is not blocked.

## How the provenance column was produced

Every row was probed from this workspace with `curl -L` (TLS, redirects
followed) on 2026-09-25, once without and once with a browser
User-Agent. The status codes below are **measured, not recalled**:

- `200` — reachable over HTTPS at the time of the probe.
- `403 to a scripted probe` — commonly bot protection, **not** evidence
  that the site is down; these need a human browser check before
  nomination, so they are listed separately rather than proposed.
- `000` — no response (DNS/connection); exclude until someone confirms.

A probe proves reachability only. It says nothing about content
correctness, licence, long-term stability, or whether the site is
appropriate for the product — that is what the review is for.

## Proposed candidates (probed 200)

| # | Display name | Canonical HTTPS URL | Why this one (one line) | Provenance (measured) | Logo slot |
|---|---|---|---|---|---|
| 1 | Quran.com | `https://quran.com/` | Full Qur'an text with translations; the reference most users already trust. | `200` direct, 2026-09-25 | yes — wordmark |
| 2 | Quran.com (Bengali) | `https://quran.com/bn` | Same source, Bengali locale — serves the primary audience directly. | `200` direct, 2026-09-25 (`/bn` resolves, not a redirect to `/ar`) | yes — wordmark |
| 3 | Sunnah.com | `https://sunnah.com/` | The standard English hadith corpus reference. | `200` with a browser UA; `403` to a bare scripted probe, 2026-09-25 | yes — wordmark |
| 4 | SeekersGuidance | `https://seekersguidance.org/` | Structured, cited fiqh and creed answers from an established teaching body. | `200` direct, 2026-09-25 | yes — wordmark |
| 5 | IslamicFinder | `https://www.islamicfinder.org/` | Prayer times and qibla — the practical surface users open most. | `200` (redirects to `www`), 2026-09-25 | yes — wordmark |
| 6 | Islamicity | `https://www.islamicity.org/` | Broad reference: articles, prayer times, zakat calculators. | `200` with a browser UA (redirects to `www`); `403` to a bare probe, 2026-09-25 | yes — wordmark |

## Observed but not proposed

| Site | Measured | Why it is not proposed yet |
|---|---|---|
| `islamqa.info` | `200`, but redirects to `/ar` | Arabic default locale. Needs a product decision on whether an Arabic-first site fits an English/Bengali audience, and whether to link a locale path. |
| `al-islam.org` | `403` even with a browser UA | Probe blocked, so reachability is unconfirmed; also a madhhab-specific source, which is a product/community decision before engineering. |
| `islamicfoundation.gov.bd` | `000` (no response) | No response from this workspace; cannot nominate something we could not reach. Worth a human check because it is the Bangladesh state body. |

## Notes for the reviewer

- **Labels:** names above are the shortest commonly used forms. If any
  overflows the tile at 200% font scale, the display name is the field to
  shorten — not the URL.
- **Bengali presentation:** rows 1-2 differ only by locale; if both are
  approved, decide whether they are two tiles or one tile with a locale
  affordance.
- **Logo slots:** each row assumes a wordmark or site mark from the
  governed bundled assets, with `HomeGlyph` as the dependency-free
  fallback only (A13 item 4).
- **Privacy:** these are ordinary outbound navigations. They open through
  the normal navigation path, so the ad-block and tracking-protection
  engine (patches `0005`-`0007`, `0012`) applies to them like any other
  page — no special path, no data leaves the device beyond the user's own
  navigation (ADR-037).

## Machine-readable form (for whichever store is chosen later)

```csv
id,display_name,url,locale,review_date,provenance,status
popular_quran_com,Quran.com,https://quran.com/,en,2026-09-25,"curl -L: 200",candidate
popular_quran_bn,Quran.com,https://quran.com/bn,bn,2026-09-25,"curl -L: 200",candidate
popular_sunnah,Sunnah.com,https://sunnah.com/,en,2026-09-25,"curl -L: 200 (UA) / 403 (bare)",candidate
popular_seekers,SeekersGuidance,https://seekersguidance.org/,en,2026-09-25,"curl -L: 200",candidate
popular_islamicfinder,IslamicFinder,https://www.islamicfinder.org/,en,2026-09-25,"curl -L: 200 (www)",candidate
popular_islamicity,Islamicity,https://www.islamicity.org/,en,2026-09-25,"curl -L: 200 (UA, www)",candidate
```

`status` stays `candidate` until product and security approvers sign off;
only rows marked `approved` may be hardcoded into a provider.
