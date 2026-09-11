package com.inweb.browser.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Combined-matcher tests — the ADR-022 correctness contract: candidate
 * selection must be a superset of all real matches, and decisions must be
 * IDENTICAL to the v1 full-scan reference.
 */
class CombinedMatcherTest {

    private val doc = "https://news.example.com/story"

    private fun engineOf(vararg lines: String): TrackingProtectionEngine =
        TrackingProtectionEngine(listOf(FilterListParser.parse(lines.joinToString("\n"), "test")))

    // --- index structure -----------------------------------------------------

    @Test
    fun ruleIndexesUnderItsLongestLiteralRun() {
        val matcher = CombinedMatcher(
            FilterListParser.parse("||ads.example.com^banner*x.gif\n").networkRules,
        )
        // longest literal run: "ads.example.com"
        assertTrue(matcher.candidates("https://ads.example.com/banner/1").isNotEmpty())
        assertTrue(matcher.candidates("https://tracker.example.org/other").isEmpty())
    }

    @Test
    fun shortLiteralRulesGoToAlwaysCheck() {
        val matcher = CombinedMatcher(
            FilterListParser.parse("||a.bc^\n||xy*\n").networkRules,
        )
        // "a.bc" is 4 chars -> token "a.bc"; "xy*" run "xy" < 3 -> always-check
        assertTrue(matcher.candidates("https://a.bc/x").isNotEmpty())          // token hit
        assertTrue(matcher.candidates("https://anything.example.net/").isNotEmpty()) // always-check
    }

    @Test
    fun wildcardOnlyRuleIsAlwaysACandidate() {
        // pattern "*" has no literal run at all -> always-check bucket
        val matcher = CombinedMatcher(FilterListParser.parse("*\n").networkRules)
        assertTrue(matcher.candidates("https://x.example/anything").isNotEmpty())
        assertTrue(matcher.candidates("https://y.other/thing").isNotEmpty())
    }

    @Test
    fun unmatchedCorpusYieldsNoCandidates() {
        val matcher = CombinedMatcher(
            FilterListParser.parse("||ads.example.com^\n||track.example.net^\n").networkRules,
        )
        assertEquals(0, matcher.candidates("https://clean.example.org/").size)
    }

    @Test
    fun candidatesPreserveOriginalRuleOrder() {
        val matcher = CombinedMatcher(
            FilterListParser.parse(
                "||zoo.example.com^\n||alpha.example.com^\n||mid.example.com^\n",
            ).networkRules,
        )
        val ordinals = matcher.candidates("https://alpha.example.com/https://mid.example.com/zoo.example.com")
        assertTrue(ordinals.size >= 3)
        // ascending = original list order, regardless of which token matched first
        assertEquals(ordinals.toList(), ordinals.sorted())
    }

    @Test
    fun tokenLookupIsCaseInsensitive() {
        val matcher = CombinedMatcher(
            FilterListParser.parse("||Ads.Example.COM^\n").networkRules,
        )
        assertTrue(matcher.candidates("https://ADS.example.COM/x").isNotEmpty())
    }

    @Test
    fun patternTokenSubstringOfLongerUrlRunIsFound() {
        // the classic whole-token trap: pattern "banner123" inside run "banner123x"
        val matcher = CombinedMatcher(FilterListParser.parse("banner123\n").networkRules)
        assertTrue(matcher.candidates("https://cdn.example.net/banner123x.gif").isNotEmpty())
    }

    @Test
    fun literalRunsSpanPunctuationAndCandidatesAreASuperset() {
        // '/' and '.' are literal; '*' and '^' split runs on BOTH sides.
        // candidates() is a pre-filter (superset of matches): the token
        // "x.gif" co-occurs in a non-matching URL too — RuleMatcher must
        // still reject it, and matchingRules/decide stay exact.
        val matcher = CombinedMatcher(FilterListParser.parse("/a/b*x.gif\n").networkRules)
        assertTrue(matcher.candidates("https://s.example/a/b*q/x.gif").isNotEmpty())
        assertTrue(matcher.candidates("https://s.example/a/z*x.gif").isNotEmpty())
        val engine = engineOf("/a/b*x.gif\n")
        assertEquals(
            FilterAction.PASS,
            engine.decide(RequestContext("https://s.example/a/z*x.gif", doc, ResourceType.IMAGE)).action,
        )
        assertEquals(
            FilterAction.BLOCK,
            engine.decide(RequestContext("https://s.example/a/b*q/x.gif", doc, ResourceType.IMAGE)).action,
        )
    }

    // --- decision equivalence (ADR-022 contract) -------------------------------

    @Test
    fun decisionsMatchNaiveScanOverMixedCorpus() {
        val corpus = StringBuilder()
        repeat(1200) { i ->
            corpus.appendLine(
                when (i % 10) {
                    in 0..2 -> "||shop$i.mall${i % 7}.example.com^"
                    3 -> "||cdn.example.net^\$script,third-party"
                    4 -> "/banner${i % 13}x*.gif\$image"
                    5 -> "@@||allowed$i.example.org^"
                    6 -> "tracker${i % 11}.metrics.example.io^"
                    7 -> "|https://secure.example.dev/ads$i.js"
                    8 -> "widget${i % 5}.example.me^"
                    else -> "||pure.example.me^\$~image"
                },
            )
        }
        val engine = engineOf(corpus.toString())

        val requests = buildList {
            repeat(50) { i ->
                add(RequestContext("https://shop$i.mall${i % 7}.example.com/i$i.gif", doc, ResourceType.IMAGE))
            }
            repeat(30) { i ->
                add(RequestContext("https://clean$i.cleanweb.example/page$i.html", doc, ResourceType.SCRIPT))
            }
            repeat(25) { i ->
                add(RequestContext("https://sub.allowed$i.example.org/x$i.js", doc, ResourceType.XHR))
            }
            repeat(25) { i ->
                add(RequestContext("https://cdn.example.net/lib$i.js", doc, ResourceType.SCRIPT))
                add(RequestContext("https://cdn.example.net/lib$i.css", doc, ResourceType.STYLESHEET))
            }
            repeat(20) { i ->
                add(RequestContext("https://s.example/banner${i % 13}x9.gif", doc, ResourceType.IMAGE))
            }
            repeat(20) { i ->
                add(RequestContext("https://widget${i % 5}.example.me/w$i.js", doc, ResourceType.OTHER))
            }
            add(RequestContext("https://secure.example.dev/ads1.js", doc, ResourceType.SCRIPT))
            add(RequestContext("https://tracker3.metrics.example.io/p.gif", doc, ResourceType.IMAGE))
            add(RequestContext("HTTPS://SHOP0.MALL0.EXAMPLE.COM/UPPER.GIF", doc, ResourceType.IMAGE))
        }

        for (request in requests) {
            assertEquals(
                "rules diverged for ${request.requestUrl}",
                engine.matchingRulesNaive(request),
                engine.matchingRules(request),
            )
        }
    }

    @Test
    fun candidateSetsAreTinyComparedToCorpus() {
        // the performance invariant that justifies the matcher at all
        val corpus = StringBuilder()
        repeat(1200) { i ->
            corpus.appendLine("||site$i.example${
                if (i % 3 == 0) ".com" else ".net"
            }^\n")
        }
        val matcher = CombinedMatcher(FilterListParser.parse(corpus.toString()).networkRules)
        val cleanCandidates = matcher.candidates("https://nothing-here.cleanweb.org/index.html")
        assertEquals(0, cleanCandidates.size)
        val hitCandidates = matcher.candidates("https://site42.example.net/ad.gif")
        assertTrue(hitCandidates.size <= 2)
    }
}
