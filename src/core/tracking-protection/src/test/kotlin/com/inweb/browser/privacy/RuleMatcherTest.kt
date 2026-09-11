package com.inweb.browser.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleMatcherTest {

    private fun request(
        url: String,
        document: String = "https://news.example.com/story",
        type: ResourceType = ResourceType.IMAGE,
    ) = RequestContext(requestUrl = url, documentUrl = document, resourceType = type)

    private fun rule(line: String) = FilterListParser.parse(line).networkRules.single()

    private fun matches(line: String, request: RequestContext) =
        RuleMatcher.matches(rule(line), request)

    @Test
    fun domainAnchorMatchesHostAndSubdomains() {
        assertTrue(matches("||ads.example.com^", request("https://ads.example.com/pixel.gif")))
        assertTrue(matches("||ads.example.com^", request("https://cdn.ads.example.com/pixel.gif")))
        assertTrue(matches("||ads.example.com^", request("https://ads.example.com"))) // end-of-URL separator
    }

    @Test
    fun domainAnchorRejectsSimilarHosts() {
        assertFalse(matches("||ads.example.com^", request("https://notads.example.com/x")))
        assertFalse(matches("||ads.example.com^", request("https://ads.example.com.evil.net/x")))
        assertFalse(matches("||ads.example.com^", request("https://ads.example.company.com/x")))
    }

    @Test
    fun separatorRequiresBoundaryAfterPattern() {
        assertTrue(matches("||example.com^", request("https://example.com:8080/x")))
        assertTrue(matches("||example.com^", request("https://example.com?utm=1")))
        assertFalse(matches("||example.com^", request("https://example.community.com/x")))
    }

    @Test
    fun wildcardMatchesAnyCharacters() {
        assertTrue(matches("||cdn.example.com/ads/*.js", request("https://cdn.example.com/ads/foo.js")))
        assertTrue(matches("||cdn.example.com/ads/*.js", request("https://cdn.example.com/ads/a/b/c.js")))
        assertFalse(matches("||cdn.example.com/ads/*.js", request("https://cdn.example.com/ads/foo.css")))
    }

    @Test
    fun leftAnchorAnchorsToUrlStart() {
        assertTrue(matches("|https://example.com", request("https://example.com/page")))
        assertFalse(matches("|https://example.com", request("https://evil.com/?u=https://example.com")))
    }

    @Test
    fun rightAnchorAnchorsToUrlEnd() {
        assertTrue(matches("/banner.gif|", request("https://x.example.com/banner.gif")))
        assertFalse(matches("/banner.gif|", request("https://x.example.com/banner.gif?v=2")))
    }

    @Test
    fun plainSubstringMatchesAnywhere() {
        assertTrue(matches("tracker.js", request("https://static.evil.net/libs/tracker.js?x=1")))
        assertFalse(matches("tracker.js", request("https://static.evil.net/libs/other.js")))
    }

    @Test
    fun typeOptionConstrainsMatching() {
        val line = "||ads.example.com^\$script"
        assertTrue(matches(line, request("https://ads.example.com/x", type = ResourceType.SCRIPT)))
        assertFalse(matches(line, request("https://ads.example.com/x", type = ResourceType.IMAGE)))
    }

    @Test
    fun negatedTypeOption() {
        val line = "||ads.example.com^\$~script"
        assertFalse(matches(line, request("https://ads.example.com/x", type = ResourceType.SCRIPT)))
        assertTrue(matches(line, request("https://ads.example.com/x", type = ResourceType.IMAGE)))
    }

    @Test
    fun thirdPartyOption() {
        val line = "||media.net^\$third-party"
        // same registrable domain → first-party → no match (pattern matches, party fails)
        assertFalse(
            matches(
                line,
                request(
                    url = "https://cdn.media.net/t.gif",
                    document = "https://www.media.net/page",
                ),
            ),
        )
        // different registrable domain → third-party → match
        assertTrue(
            matches(
                line,
                request(
                    url = "https://cdn.media.net/t.gif",
                    document = "https://news.example.com/story",
                ),
            ),
        )
    }

    @Test
    fun domainOptionIncludesAndExcludes() {
        val line = "||ads.example^\$domain=www.news.example|~meta.news.example"
        assertTrue(
            matches(
                line,
                request("https://ads.example/x", document = "https://www.news.example/story"),
            ),
        )
        assertFalse(
            matches(
                line,
                request("https://ads.example/x", document = "https://meta.news.example/story"),
            ),
        )
        assertFalse(
            matches(
                line,
                request("https://ads.example/x", document = "https://blog.other.org/story"),
            ),
        )
    }

    @Test
    fun domainOptionMatchesDocumentSubdomains() {
        val line = "||ads.example^\$domain=news.example.com"
        assertTrue(
            matches(
                line,
                request("https://ads.example/x", document = "https://sub.news.example.com/story"),
            ),
        )
    }

    @Test
    fun domainOptionWithoutIncludesAppliesEverywhereButExcluded() {
        val line = "||ads.example^\$domain=~news.example.com"
        assertTrue(
            matches(
                line,
                request("https://ads.example/x", document = "https://blog.other.org/story"),
            ),
        )
        assertFalse(
            matches(
                line,
                request("https://ads.example/x", document = "https://sub.news.example.com/story"),
            ),
        )
    }

    @Test
    fun unsupportedRulesNeverMatch() {
        val list = FilterListParser.parse("||example.com^\$csp=script-src 'none'")
        assertEquals(0, list.networkRules.size)
    }
}
