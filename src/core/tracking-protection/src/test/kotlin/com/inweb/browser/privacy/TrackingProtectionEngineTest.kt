package com.inweb.browser.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingProtectionEngineTest {

    private val list = FilterListParser.parse(
        "||ads.example.com^\n" +
            "||tracker.net^\$script,third-party\n" +
            "@@||good.example.com^\n",
        listId = "test-list",
    )

    private fun engine(settings: TrackingProtectionSettings = TrackingProtectionSettings()) =
        TrackingProtectionEngine(listOf(list), settings)

    private fun request(
        url: String,
        document: String = "https://news.example.com/story",
        type: ResourceType = ResourceType.IMAGE,
    ) = RequestContext(requestUrl = url, documentUrl = document, resourceType = type)

    @Test
    fun blocksMatchingRequestWithRule() {
        val decision = engine().decide(request("https://ads.example.com/pixel.gif"))
        assertEquals(FilterAction.BLOCK, decision.action)
        assertNotNull(decision.matchedRule)
        assertEquals("||ads.example.com^", decision.matchedRule?.raw)
    }

    @Test
    fun passesUnmatchedRequest() {
        val decision = engine().decide(request("https://cdn.example.org/logo.png"))
        assertEquals(FilterAction.PASS, decision.action)
        assertNull(decision.matchedRule)
    }

    @Test
    fun exceptionRuleOverridesBlocking() {
        val decision = engine().decide(request("https://good.example.com/api"))
        assertEquals(FilterAction.ALLOW, decision.action)
        assertEquals("@@||good.example.com^", decision.matchedRule?.raw)
    }

    @Test
    fun typeConstrainedRuleOnlyBlocksItsType() {
        val decision = engine().decide(
            request("https://tracker.net/lib.js", type = ResourceType.SCRIPT),
        )
        assertEquals(FilterAction.BLOCK, decision.action)

        val imageDecision = engine().decide(
            request("https://tracker.net/pixel.gif", type = ResourceType.IMAGE),
        )
        assertEquals(FilterAction.PASS, imageDecision.action)
    }

    @Test
    fun perSiteAllowlistBypassesFilteringForWholePage() {
        val settings = TrackingProtectionSettings()
            .withAllowlistedSite("news.example.com")
        val decision = TrackingProtectionEngine(listOf(list), settings)
            .decide(request("https://ads.example.com/pixel.gif"))
        assertEquals(FilterAction.ALLOW, decision.action)
        assertEquals("news.example.com", decision.allowlistedSite)
        assertNull(decision.matchedRule)
    }

    @Test
    fun disabledEnginePassesEverything() {
        val settings = TrackingProtectionSettings(enabled = false)
        val decision = TrackingProtectionEngine(listOf(list), settings)
            .decide(request("https://ads.example.com/pixel.gif"))
        assertEquals(FilterAction.PASS, decision.action)
    }

    @Test
    fun statisticsCountRealDecisions() {
        val engine = engine()
        engine.decide(request("https://ads.example.com/pixel.gif"))
        engine.decide(request("https://sub.ads.example.com/other.gif"))
        engine.decide(request("https://clean.example.org/logo.png"))

        assertEquals(2, engine.statistics.blockCount)
        assertEquals(1, engine.statistics.passCount)
        assertEquals(0, engine.statistics.allowCount)
        assertEquals(mapOf("example.com" to 2), engine.statistics.blockedByDomain())
    }

    @Test
    fun allowlistSiteHelpersRoundTrip() {
        val settings = TrackingProtectionSettings()
            .withAllowlistedSite("News.Example.com")
        assertEquals(setOf("news.example.com"), settings.allowlistedSites())
        assertTrue(settings.isSiteAllowlisted("news.example.com"))
        assertTrue(settings.isSiteAllowlisted("sub.news.example.com"))
        val removed = settings.withoutAllowlistedSite("news.example.com")
        assertTrue(removed.allowlistedSites().isEmpty())
    }
}
