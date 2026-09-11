package com.inweb.browser.privacy

import java.net.URI

/**
 * Matches a parsed rule against a request: option constraints first
 * (type, party, document domain), then the pattern regex.
 */
internal object RuleMatcher {

    fun matches(rule: NetworkFilterRule, request: RequestContext): Boolean {
        if (rule.unsupported) return false
        if (!optionsSatisfied(rule.options, request)) return false
        return rule.regex.containsMatchIn(request.requestUrl)
    }

    private fun optionsSatisfied(options: FilterOptions, request: RequestContext): Boolean {
        val type = request.resourceType
        if (options.types.isNotEmpty() && type !in options.types) return false
        if (type in options.negatedTypes) return false
        if (options.thirdParty != null) {
            if (isThirdPartyRequest(request) != options.thirdParty) return false
        }
        if (options.domains.isNotEmpty() && !documentDomainAllowed(options.domains, request)) {
            return false
        }
        return true
    }

    private fun isThirdPartyRequest(request: RequestContext): Boolean {
        val docHost = hostOf(request.documentUrl) ?: return true
        val reqHost = hostOf(request.requestUrl) ?: return false
        return DomainClassifier.isThirdParty(docHost, reqHost)
    }

    /**
     * `domain=` semantics (ABP): an entry matches when the document HOST is
     * equal to the entry or a subdomain of it — full-host matching, not
     * registrable-domain reduction.
     */
    private fun documentDomainAllowed(domains: Map<String, Boolean>, request: RequestContext): Boolean {
        val docHost = hostOf(request.documentUrl) ?: return false

        val excludes = domains.filterValues { !it }.keys
        if (excludes.any { hostMatchesDomain(docHost, it) }) return false

        val includes = domains.filterValues { it }.keys
        if (includes.isNotEmpty() && includes.none { hostMatchesDomain(docHost, it) }) return false
        return true
    }

    private fun hostMatchesDomain(host: String, domain: String): Boolean =
        host == domain || host.endsWith(".$domain")

    /** Robust host extraction: URI parser first, manual fallback second. */
    fun hostOf(url: String): String? {
        val normalized = url.trim()
        val fromUri = try {
            URI(normalized).host
        } catch (e: Exception) {
            null
        }
        if (!fromUri.isNullOrEmpty()) return fromUri.lowercase()

        val noScheme = normalized.substringAfter("://", normalized)
        val authority = noScheme
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
        val host = authority.substringBefore('@').substringBefore(':')
        return host.ifEmpty { null }?.lowercase()
    }
}
