package com.inweb.browser.privacy

import com.inweb.browser.privacy.lists.FilterListManager

/** Status of one subscribed filter list, for the Security Center (§24). */
data class FilterListStatus(
    val id: String,
    val ruleCount: Int,
    val version: String?,
    val lastCheckedAtMillis: Long?,
)

/** A domain with its real blocked-request count, for the Security Center. */
data class BlockedDomainCount(
    val domain: String,
    val count: Int,
)

/**
 * The Security Center dashboard contract (MASTER-SPEC §24).
 *
 * Every number in this model comes from real state: real decisions made by
 * [TrackingProtectionEngine], real parsed filter lists, real policy inputs.
 * Nothing is estimated or fabricated. Items the spec lists that do not have
 * a real backing yet (connection security, permissions, certificates) are
 * simply absent from v1 — they join the model when their patches land.
 *
 * [enforcementActive] is false until the Chromium `adblock/`/`privacy/`
 * patches wire `decide()` into the network stack (B-001): until then the
 * model honestly reports a decision engine that is armed but not enforcing.
 */
data class SecurityCenterModel(
    val enforcementActive: Boolean,
    val trackingProtectionEnabled: Boolean,
    val cookiePolicy: CookiePolicy,
    val allowlistedSiteCount: Int,
    val filterLists: List<FilterListStatus>,
    val totalNetworkRules: Int,
    val blockedCount: Int,
    val allowedCount: Int,
    val passedCount: Int,
    val topBlockedDomains: List<BlockedDomainCount>,
) {
    val totalDecisions: Int get() = blockedCount + allowedCount + passedCount
}

/**
 * Builds the [SecurityCenterModel] from real engine statistics, real policy
 * inputs, and the real filter-list state. The dashboard UI renders this
 * model; it never reads raw internals itself.
 */
object SecurityCenter {

    const val TOP_DOMAINS_LIMIT = 5

    fun build(
        settings: TrackingProtectionSettings,
        statistics: EngineStatistics,
        listManager: FilterListManager,
        enforcementActive: Boolean = false,
    ): SecurityCenterModel {
        val parsedLists = listManager.parsedLists()
        val filterLists = parsedLists.map { parsed ->
            val metadata = listManager.metadataOf(parsed.listId)
            FilterListStatus(
                id = parsed.listId,
                ruleCount = parsed.networkRules.size,
                version = metadata?.version,
                lastCheckedAtMillis = metadata?.downloadedAtMillis,
            )
        }
        return SecurityCenterModel(
            enforcementActive = enforcementActive,
            trackingProtectionEnabled = settings.enabled,
            cookiePolicy = settings.cookiePolicy,
            allowlistedSiteCount = settings.allowlistedSites().size,
            filterLists = filterLists,
            totalNetworkRules = listManager.totalNetworkRules(),
            blockedCount = statistics.blockCount,
            allowedCount = statistics.allowCount,
            passedCount = statistics.passCount,
            topBlockedDomains = statistics.blockedByDomain()
                .map { (domain, count) -> BlockedDomainCount(domain, count) }
                .sortedWith(compareByDescending<BlockedDomainCount> { it.count }.thenBy { it.domain })
                .take(TOP_DOMAINS_LIMIT),
        )
    }
}
