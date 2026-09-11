package com.inweb.browser.privacy

import com.inweb.browser.privacy.lists.FetchResult
import com.inweb.browser.privacy.lists.FileFilterListCache
import com.inweb.browser.privacy.lists.FilterListFetcher
import com.inweb.browser.privacy.lists.FilterListManager
import com.inweb.browser.privacy.lists.FilterListSource
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * §24: the Security Center model must be backed strictly by real state —
 * real decisions, real lists, real policy inputs. Nothing fabricated.
 */
class SecurityCenterTest {

    private lateinit var tempDir: java.io.File
    private val source = FilterListSource("unit-list", "Unit List", "https://lists.example.com/unit.txt")
    private val other = FilterListSource("other-list", "Other List", "https://lists.example.com/other.txt")

    private val body = "! Title: Unit\n! Version: v1\n||ads.example.com^\n||tracker.net^\$script\n"

    private val multiDomainBody =
        "! Title: Multi\n! Version: v1\n" +
            "||a.example.net^\n||b.example.org^\n||c.example.io^\n" +
            "||d.example.dev^\n||e.example.tv^\n||f.example.me^\n"

    private class OneShotFetcher(private var result: FetchResult) : FilterListFetcher {
        override fun fetch(url: String, etag: String?, lastModified: String?): FetchResult = result
    }

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("inweb-security-center").toFile()
    }

    @After
    fun tearDown() {
        tempDir.walkBottomUp().forEach { it.delete() }
    }

    private fun manager(
        vararg sources: FilterListSource,
        now: Long = 1_000,
        listBody: String = body,
    ): FilterListManager =
        FilterListManager(sources.toList(), OneShotFetcher(FetchResult.Success(listBody, "\"v1\"", null)), FileFilterListCache(tempDir))
            .apply { startup(now) }

    private fun request(url: String, documentUrl: String = "https://news.example.com/story") = RequestContext(
        requestUrl = url,
        documentUrl = documentUrl,
        resourceType = ResourceType.IMAGE,
    )

    @Test
    fun emptyStateIsHonest() {
        val manager = manager(source)
        val model = SecurityCenter.build(
            settings = TrackingProtectionSettings(),
            statistics = TrackingProtectionEngine(manager.parsedLists()).statistics,
            listManager = manager,
        )

        assertFalse(model.enforcementActive)
        assertTrue(model.trackingProtectionEnabled)
        assertEquals(0, model.blockedCount)
        assertEquals(0, model.allowedCount)
        assertEquals(0, model.passedCount)
        assertEquals(0, model.totalDecisions)
        assertTrue(model.topBlockedDomains.isEmpty())
    }

    @Test
    fun aggregatesRealDecisionCounts() {
        val manager = manager(source)
        val settings = TrackingProtectionSettings()
            .withAllowlistedSite("news.example.com")
        val engine = TrackingProtectionEngine(manager.parsedLists(), settings)

        engine.decide(request("https://ads.example.com/a.gif", documentUrl = "https://blog.other.org/page")) // block
        engine.decide(request("https://ads.example.com/b.gif", documentUrl = "https://blog.other.org/page")) // block
        engine.decide(request("https://clean.example.org/c.png", documentUrl = "https://blog.other.org/page")) // pass

        val beforeAllowlisted = engine.statistics.blockCount + engine.statistics.allowCount +
            engine.statistics.passCount

        // A whole-page allowlist BYPASS is not a counted decision (engine
        // contract): the page is exempt from filtering, nothing is decided.
        engine.decide(request("https://sub.news.example.com/x"))

        val afterAllowlisted = engine.statistics.blockCount + engine.statistics.allowCount +
            engine.statistics.passCount
        assertEquals(beforeAllowlisted, afterAllowlisted)

        val model = SecurityCenter.build(settings, engine.statistics, manager)
        assertEquals(2, model.blockedCount)
        assertEquals(0, model.allowedCount)
        assertEquals(1, model.passedCount)
        assertEquals(3, model.totalDecisions)
    }

    @Test
    fun topBlockedDomainsSortedDescendingAndLimited() {
        val manager = manager(source, listBody = multiDomainBody)
        val engine = TrackingProtectionEngine(manager.parsedLists())

        // six distinct registrable domains, different counts
        engine.decide(request("https://a.example.net/x")) // 1x
        engine.decide(request("https://b.example.org/x")) // 2x
        engine.decide(request("https://b.example.org/y"))
        engine.decide(request("https://c.example.io/x")) // 3x
        engine.decide(request("https://c.example.io/y"))
        engine.decide(request("https://c.example.io/z"))
        engine.decide(request("https://d.example.dev/x")) // 1x
        engine.decide(request("https://e.example.tv/x")) // 1x
        engine.decide(request("https://f.example.me/x")) // 1x

        val model = SecurityCenter.build(TrackingProtectionSettings(), engine.statistics, manager)
        assertEquals(5, model.topBlockedDomains.size)
        // domains are registrable domains; most-blocked first; ties by name
        // ascending; example.tv (1x) is dropped by the limit
        assertEquals("example.io", model.topBlockedDomains[0].domain)
        assertEquals(3, model.topBlockedDomains[0].count)
        assertEquals("example.org", model.topBlockedDomains[1].domain)
        assertEquals(2, model.topBlockedDomains[1].count)
        assertEquals("example.dev", model.topBlockedDomains[2].domain)
        assertEquals("example.me", model.topBlockedDomains[3].domain)
        assertEquals("example.net", model.topBlockedDomains[4].domain)
    }

    @Test
    fun filterListStatusCarriesVersionAndRuleCount() {
        val manager = manager(source, now = 42_000)
        val model = SecurityCenter.build(
            TrackingProtectionSettings(),
            TrackingProtectionEngine(manager.parsedLists()).statistics,
            manager,
        )

        val status = model.filterLists.single()
        assertEquals("unit-list", status.id)
        assertEquals(2, status.ruleCount)
        assertEquals("v1", status.version)
        assertEquals(42_000L, status.lastCheckedAtMillis)
        assertEquals(2, model.totalNetworkRules)
    }

    @Test
    fun policyStateIsReflected() {
        val manager = manager(source)
        val settings = TrackingProtectionSettings(
            enabled = false,
            cookiePolicy = CookiePolicy.BLOCK_ALL,
        ).withAllowlistedSite("a.example.com").withAllowlistedSite("b.example.com")

        val model = SecurityCenter.build(
            settings,
            TrackingProtectionEngine(manager.parsedLists(), settings).statistics,
            manager,
        )

        assertFalse(model.trackingProtectionEnabled)
        assertEquals(CookiePolicy.BLOCK_ALL, model.cookiePolicy)
        assertEquals(2, model.allowlistedSiteCount)
    }

    @Test
    fun disabledSourcesAreAbsent() {
        val manager = manager(source, other.copy(enabled = false))

        val model = SecurityCenter.build(
            TrackingProtectionSettings(),
            TrackingProtectionEngine(manager.parsedLists()).statistics,
            manager,
        )

        assertEquals(listOf("unit-list"), model.filterLists.map { it.id })
    }

    @Test
    fun totalsAggregateAcrossLists() {
        val manager = manager(source, other)

        val model = SecurityCenter.build(
            TrackingProtectionSettings(),
            TrackingProtectionEngine(manager.parsedLists()).statistics,
            manager,
        )

        assertEquals(2, model.filterLists.size)
        assertEquals(4, model.totalNetworkRules)
    }

    @Test
    fun enforcementFlagPassesThroughForFutureWiring() {
        val manager = manager(source)
        val model = SecurityCenter.build(
            TrackingProtectionSettings(),
            TrackingProtectionEngine(manager.parsedLists()).statistics,
            manager,
            enforcementActive = true,
        )
        assertTrue(model.enforcementActive)
    }

    @Test
    fun loadedRulesNeverFabricateDecisions() {
        // lists loaded, zero requests decided → zero counts (§24: real state only)
        val manager = manager(source)
        val engine = TrackingProtectionEngine(manager.parsedLists())

        val model = SecurityCenter.build(TrackingProtectionSettings(), engine.statistics, manager)
        assertEquals(2, model.totalNetworkRules)
        assertEquals(0, model.totalDecisions)
    }
}
