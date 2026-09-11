package com.inweb.browser.privacy

/** Cookie policy defaults (§10). Enforcement ships with the privacy engine patch. */
enum class CookiePolicy { ALLOW_ALL, BLOCK_THIRD_PARTY, BLOCK_ALL }

/**
 * Tracking-protection settings model (§10).
 *
 * These are the policy inputs the engine patches enforce; they are NOT yet
 * user-facing toggles (no UI until the enforcing patches exist — §57).
 */
data class TrackingProtectionSettings(
    val enabled: Boolean = true,
    val cookiePolicy: CookiePolicy = CookiePolicy.BLOCK_THIRD_PARTY,
    private val allowlistedSites: Set<String> = emptySet(),
) {
    /** True when filtering is bypassed for pages on this host. */
    fun isSiteAllowlisted(host: String): Boolean {
        val h = host.trim().lowercase().removeSuffix(".")
        if (h.isEmpty()) return false
        return allowlistedSites.any { site ->
            h == site || h.endsWith(".$site") ||
                DomainClassifier.registrableDomain(h) == site
        }
    }

    fun withAllowlistedSite(site: String): TrackingProtectionSettings =
        copy(allowlistedSites = allowlistedSites + site.trim().lowercase())

    fun withoutAllowlistedSite(site: String): TrackingProtectionSettings =
        copy(allowlistedSites = allowlistedSites - site.trim().lowercase())

    fun allowlistedSites(): Set<String> = allowlistedSites.toSet()
}
