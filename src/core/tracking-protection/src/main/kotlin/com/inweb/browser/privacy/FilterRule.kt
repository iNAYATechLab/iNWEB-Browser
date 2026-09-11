package com.inweb.browser.privacy

/**
 * Parsed network-filter rule (EasyList-family syntax subset).
 *
 * Pattern anchors:
 *  - `||pattern` — domain anchor: matches at a scheme/host boundary
 *  - `|pattern`  — left anchor: matches at the start of the URL
 *  - `pattern|`  — right anchor: matches at the end of the URL
 * Pattern specials: `*` (any characters), `^` (separator character or
 * end of URL).
 *
 * A rule marked [unsupported] carries options outside the supported subset;
 * it is never matched (counted separately by the parser — honest scoping).
 */
data class NetworkFilterRule(
    val raw: String,
    val pattern: String,
    val isException: Boolean,
    val domainAnchor: Boolean,
    val anchorStart: Boolean,
    val anchorEnd: Boolean,
    val options: FilterOptions,
    val unsupported: Boolean,
    val line: Int,
) {
    /** Compiled matcher regex (lazily — translation per [PatternCompiler]). */
    val regex: Regex by lazy { PatternCompiler.compile(this) }
}

/** Option constraints of a rule (the part after `$`). */
data class FilterOptions(
    val types: Set<ResourceType> = emptySet(),
    val negatedTypes: Set<ResourceType> = emptySet(),
    /** true = third-party requests only; false = first-party only; null = no constraint. */
    val thirdParty: Boolean? = null,
    /** `domain=` entries: include (true) / exclude (false). */
    val domains: Map<String, Boolean> = emptyMap(),
)

/** Result of parsing one filter list. */
data class ParsedFilterList(
    val listId: String,
    val networkRules: List<NetworkFilterRule>,
    val cosmeticRuleCount: Int,
    val commentCount: Int,
    val invalidRuleCount: Int,
    val unsupportedRuleCount: Int,
) {
    val totalLines: Int
        get() = networkRules.size + cosmeticRuleCount + commentCount +
            invalidRuleCount + unsupportedRuleCount
}
