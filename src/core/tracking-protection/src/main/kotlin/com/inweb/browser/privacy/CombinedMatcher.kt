package com.inweb.browser.privacy

/**
 * Token-index candidate selection for the filter engine (Phase 5, ADR-022;
 * docs/PHASE5-PERFORMANCE-DESIGN.md §3).
 *
 * Correctness contract: a rule's pattern, after translation
 * ([PatternCompiler]), can only match a URL if every literal run of the
 * pattern occurs in that URL. Each rule is therefore indexed under its
 * LONGEST literal run (its required token, ≥ [MIN_TOKEN_LENGTH] chars,
 * where "literal" = any character except the pattern specials `*` and
 * `^`). A rule whose token does not occur in the URL cannot match and is
 * safely skipped; rules without a usable token (short or purely wildcard
 * patterns — including the empty pattern, which matches everything) go
 * into the always-check bucket.
 *
 * Token occurrence is substring-based, not whole-token-based: the URL is
 * scanned for every substring (length [MIN_TOKEN_LENGTH]..[maxTokenLength])
 * of every literal run, so a pattern token that is a proper substring of a
 * longer URL run (e.g. pattern `banner123` vs URL `banner123x.gif`) is
 * still found. Lookup is case-insensitive, matching the compiled regexes.
 *
 * Each rule lives in exactly ONE bucket (its token list, or the always-check
 * list), so the candidate union needs no deduplication; ordinals are sorted
 * ascending to preserve the original list order — the engine's
 * first-exception-wins / first-block-wins semantics are untouched.
 */
internal class CombinedMatcher(private val rules: List<NetworkFilterRule>) {

    private val tokenIndex: MutableMap<String, MutableList<Int>> = HashMap()
    private val alwaysCheck: IntArray
    private val maxTokenLength: Int

    init {
        val always = mutableListOf<Int>()
        var maxLen = MIN_TOKEN_LENGTH
        for ((ordinal, rule) in rules.withIndex()) {
            val token = requiredToken(rule.pattern)
            if (token == null) {
                always += ordinal
            } else {
                tokenIndex.getOrPut(token) { mutableListOf() } += ordinal
                if (token.length > maxLen) maxLen = token.length
            }
        }
        alwaysCheck = always.toIntArray()
        maxTokenLength = maxLen
    }

    /**
     * Ordinals of every rule whose pattern COULD match [url] (ascending —
     * original rule order). Superset of all pattern matches; option checks
     * still happen in [RuleMatcher].
     */
    fun candidates(url: String): IntArray {
        if (rules.isEmpty()) return IntArray(0)
        val selected = mutableListOf<Int>()
        val u = url.lowercase()

        var runStart = 0
        var i = 0
        while (i <= u.length) {
            val atBoundary = i == u.length || u[i] == '*' || u[i] == '^'
            if (atBoundary) {
                collectRunTokens(u, runStart, i, selected)
                runStart = i + 1
            }
            i++
        }

        if (alwaysCheck.isNotEmpty()) selected.addAll(alwaysCheck.toList())
        if (selected.size > 1) selected.sort()
        return selected.toIntArray()
    }

    /** Emits every index token occurring as a substring of `u[from..until)`. */
    private fun collectRunTokens(u: String, from: Int, until: Int, out: MutableList<Int>) {
        val runLen = until - from
        if (runLen < MIN_TOKEN_LENGTH) return
        val maxLen = minOf(runLen, maxTokenLength)
        for (len in MIN_TOKEN_LENGTH..maxLen) {
            var start = from
            while (start + len <= until) {
                val bucket = tokenIndex[u.substring(start, start + len)]
                if (bucket != null) out.addAll(bucket)
                start++
            }
        }
    }

    /** Longest literal run (≥ [MIN_TOKEN_LENGTH]) of the pattern, or null. */
    private fun requiredToken(pattern: String): String? {
        var bestStart = -1
        var bestLen = 0
        var runStart = 0
        var i = 0
        while (i <= pattern.length) {
            val atBoundary = i == pattern.length || pattern[i] == '*' || pattern[i] == '^'
            if (atBoundary) {
                val runLen = i - runStart
                if (runLen > bestLen) {
                    bestLen = runLen
                    bestStart = runStart
                }
                runStart = i + 1
            }
            i++
        }
        if (bestLen < MIN_TOKEN_LENGTH) return null
        return pattern.substring(bestStart, bestStart + bestLen).lowercase()
    }

    private companion object {
        const val MIN_TOKEN_LENGTH = 3
    }
}
