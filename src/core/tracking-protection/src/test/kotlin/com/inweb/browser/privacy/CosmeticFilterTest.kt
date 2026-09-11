package com.inweb.browser.privacy

import com.inweb.browser.privacy.lists.FetchResult
import com.inweb.browser.privacy.lists.FileFilterListCache
import com.inweb.browser.privacy.lists.FilterListFetcher
import com.inweb.browser.privacy.lists.FilterListManager
import com.inweb.browser.privacy.lists.FilterListSource
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CosmeticFilterTest {

    private lateinit var tempDir: java.io.File

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("inweb-cosmetic").toFile()
    }

    @After
    fun tearDown() {
        tempDir.walkBottomUp().forEach { it.delete() }
    }

    // --- parser ------------------------------------------------------------

    @Test
    fun parsesUniversalHidingRule() {
        val list = CosmeticFilterParser.parse("##.ad-banner")
        val rule = list.rules.single()
        assertTrue(rule.domains.isEmpty())
        assertEquals(".ad-banner", rule.selector)
        assertEquals(false, rule.isException)
    }

    @Test
    fun parsesDomainIncludesAndExcludes() {
        val list = CosmeticFilterParser.parse("example.com,~meta.example.com##.promo")
        val rule = list.rules.single()
        assertEquals(mapOf("example.com" to true, "meta.example.com" to false), rule.domains)
    }

    @Test
    fun parsesExceptionRule() {
        val list = CosmeticFilterParser.parse("example.com#@#.trusted-box")
        val rule = list.rules.single()
        assertTrue(rule.isException)
        assertEquals(".trusted-box", rule.selector)
    }

    @Test
    fun proceduralRulesAreCountedNotMatched() {
        val list = CosmeticFilterParser.parse("example.com#?#div:has-text(ads)")
        assertEquals(0, list.rules.size)
        assertEquals(1, list.proceduralRuleCount)
    }

    @Test
    fun unsupportedMarkersAreCounted() {
        val list = CosmeticFilterParser.parse(
            "example.com#$#.x { color: red }\n" +
                "example.com#%#window.stop()\n",
        )
        assertEquals(0, list.rules.size)
        assertEquals(2, list.unsupportedRuleCount)
    }

    @Test
    fun invalidRulesAreCounted() {
        val list = CosmeticFilterParser.parse("example.com##\nnews.com,##.x\n")
        assertEquals(0, list.rules.size)
        assertEquals(2, list.invalidRuleCount)
    }

    @Test
    fun networkLinesAndCommentsAreSkipped() {
        val list = CosmeticFilterParser.parse(
            "||ads.example.com^\n" +
                "! a comment\n" +
                "##.ad\n",
        )
        assertEquals(1, list.rules.size)
        assertEquals(1, list.commentCount)
    }

    // --- engine ------------------------------------------------------------

    @Test
    fun universalRuleAppliesEverywhere() {
        val engine = CosmeticFilterEngine(listOf(CosmeticFilterParser.parse("##.ad")))
        assertTrue(engine.hideCssFor("anything.example.net").contains(".ad"))
        assertTrue(engine.hideCssFor("other.org").contains(".ad"))
    }

    @Test
    fun domainRuleAppliesToHostAndSubdomainsOnly() {
        val engine = CosmeticFilterEngine(listOf(CosmeticFilterParser.parse("example.com##.promo")))
        assertTrue(engine.hideCssFor("example.com").contains(".promo"))
        assertTrue(engine.hideCssFor("www.example.com").contains(".promo"))
        assertEquals("", engine.hideCssFor("notexample.com"))
        assertEquals("", engine.hideCssFor("example.com.evil.net"))
    }

    @Test
    fun excludeWinsOverInclude() {
        val engine = CosmeticFilterEngine(
            listOf(CosmeticFilterParser.parse("example.com,~shop.example.com##.banner")),
        )
        assertEquals("", engine.hideCssFor("shop.example.com"))
        assertTrue(engine.hideCssFor("example.com").contains(".banner"))
    }

    @Test
    fun exceptionCancelsSelectorOnMatchingDomains() {
        val engine = CosmeticFilterEngine(
            listOf(
                CosmeticFilterParser.parse("##.box"),
                CosmeticFilterParser.parse("trusted.example#@#.box"),
            ),
        )
        assertEquals("", engine.hideCssFor("trusted.example"))
        assertEquals("", engine.hideCssFor("www.trusted.example"))
        assertTrue(engine.hideCssFor("other.org").contains(".box"))
    }

    @Test
    fun groupedCssOrEmptyWhenNothingApplies() {
        val engine = CosmeticFilterEngine(
            listOf(CosmeticFilterParser.parse("##.ad\n##.banner\n")),
        )
        assertEquals(".ad, .banner { display: none !important; }", engine.hideCssFor("example.com"))
        assertEquals("", CosmeticFilterEngine(emptyList()).hideCssFor("example.com"))
    }

    // --- manager integration -------------------------------------------------

    private class BodyFetcher(private val body: String) : FilterListFetcher {
        override fun fetch(url: String, etag: String?, lastModified: String?): FetchResult =
            FetchResult.Success(body, null, null)
    }

    @Test
    fun managerBuildsCosmeticEngineFromRealLists() {
        val source = FilterListSource("unit-list", "Unit List", "https://lists.example.com/unit.txt")
        val body = "||ads.example.com^\nexample.com##.promo\n##.ad\n"
        val manager = FilterListManager(
            listOf(source),
            BodyFetcher(body),
            FileFilterListCache(tempDir),
        )
        manager.startup(nowMillis = 1_000)

        assertEquals(2, manager.totalCosmeticRules())
        val cosmetic = manager.buildCosmeticEngine()
        assertTrue(cosmetic.hideCssFor("example.com").contains(".promo"))
        assertTrue(cosmetic.hideCssFor("anything.org").contains(".ad"))
    }
}
