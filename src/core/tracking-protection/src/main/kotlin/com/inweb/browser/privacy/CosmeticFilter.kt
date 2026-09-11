package com.inweb.browser.privacy

/**
 * A cosmetic (element-hiding) rule: a CSS selector hidden on a set of
 * document domains (MASTER-SPEC §11 "cosmetic filtering where technically
 * feasible").
 *
 * v1 subset (ADR-021):
 *  - `##`   — hide selector on matching domains (empty domain list = everywhere)
 *  - `#@#`  — exception: cancel hiding of that selector on matching domains
 *  - `#?#`  — procedural (JS-based) rules: recognized and counted, NOT matched
 *  - `#$#` / `#%#` — stylesheet-rewrite / scriptlet rules: counted as
 *    unsupported, NOT matched
 *
 * Selectors are passed through verbatim — the browser CSS parser is the
 * validator at injection time; nothing is invented here.
 */
data class CosmeticRule(
    val raw: String,
    val selector: String,
    /** Include (true) / exclude (false) domains; empty = applies everywhere. */
    val domains: Map<String, Boolean>,
    val isException: Boolean,
)

/** Result of parsing the cosmetic half of one filter list. */
data class ParsedCosmeticList(
    val listId: String,
    val rules: List<CosmeticRule>,
    val proceduralRuleCount: Int,
    val unsupportedRuleCount: Int,
    val invalidRuleCount: Int,
    val commentCount: Int,
)

/**
 * Parses the cosmetic rules of an EasyList-family list body. Network rules
 * and comments are not this parser's concern (see [FilterListParser]) —
 * lines without a cosmetic marker are simply skipped.
 */
object CosmeticFilterParser {

    private val markers = listOf("##", "#@#", "#?#", "#$#", "#%#")

    fun parse(text: String, listId: String = "default"): ParsedCosmeticList {
        var rules = 0
        var procedural = 0
        var unsupported = 0
        var invalid = 0
        var comments = 0
        val parsed = mutableListOf<CosmeticRule>()

        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.isEmpty() -> {}
                line.startsWith("!") || line.startsWith("[") -> comments++
                else -> {
                    val marker = earliestMarker(line) ?: return@forEach
                    val domainPart = line.substring(0, marker.index)
                    val selector = line.substring(marker.index + marker.text.length).trim()
                    if (selector.isEmpty()) {
                        invalid++
                        return@forEach
                    }
                    when (marker.text) {
                        "#?#" -> procedural++
                        "#$#", "#%#" -> unsupported++
                        else -> {
                            val domains = parseDomains(domainPart)
                            if (domains == null) {
                                invalid++
                                return@forEach
                            }
                            parsed += CosmeticRule(
                                raw = line,
                                selector = selector,
                                domains = domains,
                                isException = marker.text == "#@#",
                            )
                            rules++
                        }
                    }
                }
            }
        }
        return ParsedCosmeticList(
            listId = listId,
            rules = parsed,
            proceduralRuleCount = procedural,
            unsupportedRuleCount = unsupported,
            invalidRuleCount = invalid,
            commentCount = comments,
        )
    }

    private class Marker(val text: String, val index: Int)

    private fun earliestMarker(line: String): Marker? =
        markers
            .mapNotNull { text -> line.indexOf(text).takeIf { it >= 0 }?.let { Marker(text, it) } }
            .minByOrNull { it.index }

    /** Returns null on malformed domain lists (e.g. empty entries). */
    private fun parseDomains(domainPart: String): Map<String, Boolean>? {
        if (domainPart.isBlank()) return emptyMap()
        val domains = mutableMapOf<String, Boolean>()
        for (entry in domainPart.split(',')) {
            val e = entry.trim().lowercase()
            if (e.isEmpty()) return null
            if (e.startsWith("~")) {
                val domain = e.drop(1)
                if (domain.isEmpty()) return null
                domains[domain] = false
            } else {
                domains[e] = true
            }
        }
        return domains
    }
}

/**
 * Computes the stylesheet to inject into a page: every hiding selector that
 * applies to the document host, minus selectors cancelled by matching
 * exception (`#@#`) rules. Pure function of the parsed lists and the host —
 * the injection timing is the Chromium patch's concern.
 */
class CosmeticFilterEngine(cosmeticLists: List<ParsedCosmeticList>) {

    private val rules: List<CosmeticRule> = cosmeticLists.flatMap { it.rules }

    fun totalRules(): Int = rules.size

    /**
     * The grouped hiding CSS for [host], or an empty string when nothing
     * applies. Example: `.ad, .banner { display: none !important; }`
     */
    fun hideCssFor(host: String): String {
        val h = host.trim().lowercase().removeSuffix(".")
        if (h.isEmpty()) return ""

        val hidden = linkedSetOf<String>()
        val cancelled = linkedSetOf<String>()
        for (rule in rules) {
            if (!domainsMatch(rule.domains, h)) continue
            if (rule.isException) cancelled += rule.selector else hidden += rule.selector
        }
        hidden.removeAll(cancelled)
        if (hidden.isEmpty()) return ""
        return hidden.joinToString(", ") + " { display: none !important; }"
    }

    /** Same include/exclude semantics as the network `domain=` option. */
    private fun domainsMatch(domains: Map<String, Boolean>, host: String): Boolean {
        val excludes = domains.filterValues { !it }.keys
        if (excludes.any { hostMatchesDomain(host, it) }) return false
        val includes = domains.filterValues { it }.keys
        if (includes.isNotEmpty() && includes.none { hostMatchesDomain(host, it) }) return false
        return true
    }

    private fun hostMatchesDomain(host: String, domain: String): Boolean =
        host == domain || host.endsWith(".$domain")
}
