package com.inweb.browser.privacy

/** One network request, as seen by the decision engine. */
data class RequestContext(
    val requestUrl: String,
    val documentUrl: String,
    val resourceType: ResourceType,
)

/** Decision for a request. */
enum class FilterAction { BLOCK, ALLOW, PASS }

/** The decision plus what caused it (for the Security Center, §24). */
data class FilterDecision(
    val action: FilterAction,
    val matchedRule: NetworkFilterRule? = null,
    val allowlistedSite: String? = null,
)

/**
 * Tracking-protection / ad-blocking decision engine (MASTER-SPEC §10, §11).
 *
 * Decision order:
 *  1. engine disabled → PASS (nothing recorded)
 *  2. the document's site is user-allowlisted → ALLOW for the whole page
 *  3. a matching exception rule (@@) → ALLOW
 *  4. a matching blocking rule → BLOCK (first matching rule wins)
 *  5. otherwise → PASS
 *
 * Statistics count real decisions only — never fabricated (§24 honesty rule).
 */
class TrackingProtectionEngine(
    parsedLists: List<ParsedFilterList>,
    private val settings: TrackingProtectionSettings = TrackingProtectionSettings(),
) {
    private val rules: List<NetworkFilterRule> = parsedLists.flatMap { it.networkRules }

    val statistics = EngineStatistics()

    fun decide(request: RequestContext): FilterDecision {
        if (!settings.enabled) return FilterDecision(FilterAction.PASS)

        val documentHost = RuleMatcher.hostOf(request.documentUrl)
        if (documentHost != null && settings.isSiteAllowlisted(documentHost)) {
            return FilterDecision(FilterAction.ALLOW, allowlistedSite = documentHost)
        }

        var blockMatch: NetworkFilterRule? = null
        for (rule in rules) {
            if (!RuleMatcher.matches(rule, request)) continue
            if (rule.isException) {
                statistics.recordAllow()
                return FilterDecision(FilterAction.ALLOW, matchedRule = rule)
            }
            if (blockMatch == null) blockMatch = rule
        }
        return when (blockMatch) {
            null -> {
                statistics.recordPass()
                FilterDecision(FilterAction.PASS)
            }
            else -> {
                statistics.recordBlock(RuleMatcher.hostOf(request.requestUrl))
                FilterDecision(FilterAction.BLOCK, matchedRule = blockMatch)
            }
        }
    }
}

/** Real decision counters, backing Security Center statistics (§24). */
class EngineStatistics internal constructor() {
    var blockCount: Int = 0
        private set
    var allowCount: Int = 0
        private set
    var passCount: Int = 0
        private set

    private val blockedByDomainInternal = mutableMapOf<String, Int>()

    fun blockedByDomain(): Map<String, Int> = blockedByDomainInternal.toMap()

    internal fun recordBlock(requestHost: String?) {
        blockCount++
        if (requestHost != null) {
            val domain = DomainClassifier.registrableDomain(requestHost)
            blockedByDomainInternal[domain] = (blockedByDomainInternal[domain] ?: 0) + 1
        }
    }

    internal fun recordAllow() {
        allowCount++
    }

    internal fun recordPass() {
        passCount++
    }
}
