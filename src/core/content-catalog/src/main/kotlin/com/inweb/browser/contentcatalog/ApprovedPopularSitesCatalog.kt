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
import java.net.URI

/**
 * One reviewed destination the Home rail is allowed to show.
 *
 * Locale variants are part of the approval record. A locale can never rewrite
 * or synthesize a URL: it selects an explicitly reviewed entry or falls back
 * to [canonicalUrl].
 *
 * The four approval fields are not decoration: an approval that cannot say
 * who approved it, when, for what scope and why cannot be audited later.
 */
data class ApprovedCatalogRow(
    val id: String,
    val category: PopularSiteCategory,
    val canonicalUrl: String,
    val localeVariantUrls: Map<String, String> = emptyMap(),
    val artworkId: String,
    val destinationName: String,
    val accessibilityLabel: String,
    val reviewerRole: String,
    val approvedOn: String,
    val scope: String,
    val rationale: String,
) {
    init {
        require(id.matches(Regex("[a-z0-9]+(?:_[a-z0-9]+)*"))) {
            "approved row id must be a stable lowercase identifier"
        }
        require(artworkId.isNotBlank()) { "approved row artworkId must not be blank" }
        require(destinationName.isNotBlank()) { "approved row destinationName must not be blank" }
        require(accessibilityLabel.isNotBlank()) { "approved row accessibilityLabel must not be blank" }
        require(reviewerRole.isNotBlank()) { "approved row reviewerRole must not be blank" }
        require(approvedOn.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            "approved row approvedOn must be an ISO date"
        }
        require(scope.isNotBlank()) { "approved row scope must not be blank" }
        require(rationale.isNotBlank()) { "approved row rationale must not be blank" }
        require(isValidHttps(canonicalUrl)) { "approved row canonicalUrl must be valid HTTPS" }
        localeVariantUrls.forEach { (localeTag, url) ->
            require(
                normalizeLocaleTag(localeTag) == localeTag &&
                    localeTag.matches(Regex("[a-z]{2,3}(?:-[a-z0-9]{2,8})*")),
            ) {
                "locale variant keys must be normalized BCP-47 tags"
            }
            require(isValidHttps(url)) { "approved locale URL must be valid HTTPS" }
        }
    }

    fun urlFor(localeTag: String?): String {
        val normalized = normalizeLocaleTag(localeTag.orEmpty())
        if (normalized.isEmpty()) return canonicalUrl
        return localeVariantUrls[normalized]
            ?: localeVariantUrls[normalized.substringBefore('-')]
            ?: canonicalUrl
    }

    private fun isValidHttps(value: String): Boolean = runCatching {
        val uri = URI(value).normalize()
        uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null
    }.getOrDefault(false)

    private fun normalizeLocaleTag(value: String): String = value
        .trim()
        .replace('_', '-')
        .lowercase()
}

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

    /** Production admission has no arbitrary-list escape hatch. */
    fun admit(category: PopularSiteCategory, localeTag: String?): List<PopularSite> =
        admitApprovedRows(category, localeTag, rows)

    fun isApproved(id: String): Boolean = rows.any { it.id == id }

    /** Test seam is module-internal; production callers cannot substitute candidates. */
    internal fun admitApprovedRows(
        category: PopularSiteCategory,
        localeTag: String?,
        approvedRows: List<ApprovedCatalogRow>,
    ): List<PopularSite> {
        require(approvedRows.map { it.id }.distinct().size == approvedRows.size) {
            "approved catalog stable IDs must be unique"
        }
        return approvedRows.filter { it.category == category }.map { row ->
            PopularSite(
                id = row.id,
                category = row.category,
                url = row.urlFor(localeTag),
                artworkId = row.artworkId,
                destinationName = row.destinationName,
                accessibilityLabel = row.accessibilityLabel,
            )
        }
    }
}
