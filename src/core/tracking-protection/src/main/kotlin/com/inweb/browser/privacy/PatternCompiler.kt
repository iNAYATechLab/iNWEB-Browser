package com.inweb.browser.privacy

/**
 * Translates a parsed filter pattern into a matcher regex.
 *
 * Translation rules (EasyList semantics):
 *  - `||x` becomes `^[scheme]://[optional subdomain-ish prefix.]x`
 *  - `|` at the start becomes `^`; `|` at the end becomes `$`
 *  - `*` becomes `.*`
 *  - `^` becomes a separator: any character that is NOT a letter, digit,
 *    `_`, `-`, `.` or `%` — or the end of the URL
 *  - everything else is matched literally (case-insensitively)
 */
internal object PatternCompiler {

    private const val SEPARATOR = "(?:[^a-zA-Z0-9_.%-]|\$)"
    private const val DOMAIN_ANCHOR_PREFIX =
        "^[a-zA-Z][a-zA-Z0-9+.-]*://(?:[^/?#]*\\.)?"

    fun compile(rule: NetworkFilterRule): Regex {
        val sb = StringBuilder()
        when {
            rule.domainAnchor -> sb.append(DOMAIN_ANCHOR_PREFIX)
            rule.anchorStart -> sb.append('^')
        }
        for (ch in rule.pattern) {
            when (ch) {
                '*' -> sb.append(".*")
                '^' -> sb.append(SEPARATOR)
                else -> sb.append(Regex.escape(ch.toString()))
            }
        }
        if (rule.anchorEnd) sb.append('$')
        return Regex(sb.toString(), RegexOption.IGNORE_CASE)
    }
}
