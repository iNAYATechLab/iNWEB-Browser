# src/core/content-catalog — approved popular-site catalogue

Pure JVM, **Lead-owned**. The admission set behind the Home rail.

## What it decides

Which curated destinations the Home page may show, and under which
reviewed name and accessibility label. Nothing else. It does not fetch
anything, does not rank anything, and does not decide what is *good* —
only what has been **approved**.

## The rule

The rail shows only rows in `ApprovedPopularSitesCatalog.rows`, each
carrying `destinationName` and `accessibilityLabel` supplied by the
reviewer. Locale routing can select only an explicitly approved URL variant;
unknown locales fall back to the approved canonical URL. Production admission
has no arbitrary-list parameter, so a caller cannot substitute candidates.
A candidate that is not in the catalogue is dropped entirely. HTTPS validity
is enforced at both the approval record and Home model boundaries and is
still deliberately *not* the same thing as approval — see
`docs/POPULAR-SITES-CANDIDATES.md`.

## Why the catalogue ships empty

Because nothing has been approved. That is the current truth, and it is
encoded rather than commented: an empty catalogue means `admit()` returns
nothing for every category, so the section stays hidden. There is no
placeholder state and no "coming soon" text — the absence of approval and
the absence of UI are the same fact.

## Adding a row

A row needs reviewer role, date, scope and rationale, not just a URL. An
approval that cannot say who approved it, when, for what scope and why
cannot be audited later, and unauditable approvals get copy-pasted. The
product and security sign-off lives in
`docs/POPULAR-SITES-CANDIDATES.md`; this module is where the outcome is
enforced.

## Tests

`ApprovedPopularSitesCatalogTest` — the shipped catalogue is empty so every
category admits nothing; an unlisted candidate is not admitted; approved
locale routing selects only recorded URLs; required approval/name fields and
HTTPS are enforced; duplicate stable IDs are rejected; and admission remains
category-scoped.
