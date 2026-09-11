package com.inweb.browser.privacy

/**
 * Parses filter lists in the EasyList-family syntax (subset — see the module
 * README for the exact supported grammar).
 *
 * Line classification:
 *  - blank, `!` comment, `[...]` header   → comments
 *  - cosmetic rules (`##`, `#@#`, `#?#`)  → recognized and counted; the
 *    network matcher ignores them (cosmetic filtering is a later phase)
 *  - `@@` prefix                          → exception rule (allow)
 *  - options after the last `$`           → parsed; unknown options mark the
 *    rule unsupported (kept out of matching, counted honestly)
 *  - anything unparseable                 → invalid (counted, never silently
 *    dropped)
 */
object FilterListParser {

    fun parse(text: String, listId: String = "default"): ParsedFilterList {
        var comments = 0
        var cosmetic = 0
        var invalid = 0
        var unsupported = 0
        val rules = mutableListOf<NetworkFilterRule>()

        text.lineSequence().forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            when {
                line.isEmpty() -> {}
                line.startsWith("!") || line.startsWith("[") -> comments++
                line.contains("##") || line.contains("#@#") || line.contains("#?#") -> cosmetic++
                else -> when (val rule = parseRule(line, index + 1)) {
                    null -> invalid++
                    else -> if (rule.unsupported) unsupported++ else rules += rule
                }
            }
        }
        return ParsedFilterList(
            listId = listId,
            networkRules = rules,
            cosmeticRuleCount = cosmetic,
            commentCount = comments,
            invalidRuleCount = invalid,
            unsupportedRuleCount = unsupported,
        )
    }

    private fun parseRule(line: String, lineNumber: Int): NetworkFilterRule? {
        var body = line
        val isException = body.startsWith("@@")
        if (isException) body = body.removePrefix("@@")

        var options = FilterOptions()
        var unsupported = false
        val dollar = body.lastIndexOf('$')
        // `dollar == 0` means the pattern part is empty ("$script" options-only
        // body) — an invalid rule, so enter the branch and let the empty-body
        // check below reject it.
        if (dollar >= 0) {
            when (val result = parseOptions(body.substring(dollar + 1))) {
                is OptionsResult.Invalid -> return null
                is OptionsResult.Unsupported -> unsupported = true
                is OptionsResult.Ok -> options = result.options
            }
            body = body.substring(0, dollar)
        }
        if (body.isEmpty()) return null

        var domainAnchor = false
        var anchorStart = false
        var anchorEnd = false
        if (body.startsWith("||")) {
            domainAnchor = true
            body = body.substring(2)
        } else if (body.startsWith("|")) {
            anchorStart = true
            body = body.substring(1)
        }
        if (body.length > 1 && body.endsWith("|")) {
            anchorEnd = true
            body = body.dropLast(1)
        }
        if (body.isEmpty()) return null

        return NetworkFilterRule(
            raw = line,
            pattern = body,
            isException = isException,
            domainAnchor = domainAnchor,
            anchorStart = anchorStart,
            anchorEnd = anchorEnd,
            options = options,
            unsupported = unsupported,
            line = lineNumber,
        )
    }

    private val typeTokens: Map<String, ResourceType> = mapOf(
        "script" to ResourceType.SCRIPT,
        "image" to ResourceType.IMAGE,
        "stylesheet" to ResourceType.STYLESHEET,
        "xhr" to ResourceType.XHR,
        "xmlhttprequest" to ResourceType.XHR,
        "subdocument" to ResourceType.SUBDOCUMENT,
        "object" to ResourceType.OBJECT,
        "websocket" to ResourceType.WEBSOCKET,
        "popup" to ResourceType.POPUP,
        "document" to ResourceType.DOCUMENT,
        "media" to ResourceType.MEDIA,
        "font" to ResourceType.FONT,
        "other" to ResourceType.OTHER,
    )

    private val ignoredTokens = setOf("match-case")

    private fun parseOptions(text: String): OptionsResult {
        if (text.isBlank()) return OptionsResult.Invalid
        val types = mutableSetOf<ResourceType>()
        val negatedTypes = mutableSetOf<ResourceType>()
        var thirdParty: Boolean? = null
        val domains = mutableMapOf<String, Boolean>()

        for (token in text.split(',')) {
            val t = token.trim().lowercase()
            when {
                t == "third-party" -> thirdParty = true
                t == "first-party" -> thirdParty = false
                t == "~third-party" -> thirdParty = false
                t == "~first-party" -> thirdParty = true
                t.startsWith("domain=") -> {
                    if (!parseDomains(t.removePrefix("domain="), domains)) {
                        return OptionsResult.Invalid
                    }
                }
                t in typeTokens -> types += typeTokens.getValue(t)
                t.startsWith("~") && t.drop(1) in typeTokens ->
                    negatedTypes += typeTokens.getValue(t.drop(1))
                t in ignoredTokens -> {}
                else -> return OptionsResult.Unsupported
            }
        }
        return OptionsResult.Ok(FilterOptions(types.toSet(), negatedTypes.toSet(), thirdParty, domains.toMap()))
    }

    private fun parseDomains(text: String, into: MutableMap<String, Boolean>): Boolean {
        if (text.isBlank()) return false
        for (entry in text.split('|')) {
            val e = entry.trim().lowercase()
            if (e.isEmpty()) return false
            if (e.startsWith("~")) {
                val domain = e.drop(1)
                if (domain.isEmpty()) return false
                into[domain] = false
            } else {
                into[e] = true
            }
        }
        return true
    }

    private sealed interface OptionsResult {
        data class Ok(val options: FilterOptions) : OptionsResult
        object Unsupported : OptionsResult
        object Invalid : OptionsResult
    }
}
