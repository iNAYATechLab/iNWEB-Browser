// iNWEB Browser — approved popular-site catalogue (pure JVM, Lead-owned).
//
// Why this is a separate module and not a file in the Home core: approval
// is the provider's job, not the model's, and it needs real tests.
// src/android-app has no Kotlin test harness, so the admission logic lives
// here where it can be tested, and the app layer stays a thin consumer.
//
// The rule it enforces: the Home rail shows ONLY rows that are in this
// catalogue, each carrying its reviewed name and accessibility label.

package com.inweb.browser.contentcatalog

import com.inweb.browser.home.PopularSite
import com.inweb.browser.home.PopularSiteCategory

/**
 * One reviewed destination the Home rail is allowed to show.
 *
 * The four approval fields are not decoration: an approval that cannot say
 * who approved it, when, for what scope and why cannot be audited later.
 * A row without them is not an approval.
 */
data class ApprovedCatalogRow(
    val id: String,
    val category: PopularSiteCategory,
    val url: String,
    val artworkId: String,
    val destinationName: String,
    val accessibilityLabel: String,
    val reviewerRole: String,
    val approvedOn: String,
    val scope: String,
    val rationale: String,
)

object ApprovedPopularSitesCatalog {
    /**
     * Empty — and that is the current truth, not an oversight: nothing has
     * been approved. docs/POPULAR-SITES-CANDIDATES.md holds the candidates
     * and the reason each one is not approved yet.
     *
     * An empty catalogue means the rail admits nothing and the section
     * stays hidden. Never a placeholder, never a "coming soon".
     */
    val rows: List<ApprovedCatalogRow> = emptyList()

    /** Catalogue members only: anything not listed is dropped, silently and completely. */
    fun admit(
        category: PopularSiteCategory,
        from: List<ApprovedCatalogRow> = rows,
    ): List<PopularSite> =
        from.filter { it.category == category }.map { row ->
            PopularSite(
                id = row.id,
                category = row.category,
                url = row.url,
                artworkId = row.artworkId,
                destinationName = row.destinationName,
                accessibilityLabel = row.accessibilityLabel,
            )
        }

    fun isApproved(id: String, from: List<ApprovedCatalogRow> = rows): Boolean =
        from.any { it.id == id }
}
