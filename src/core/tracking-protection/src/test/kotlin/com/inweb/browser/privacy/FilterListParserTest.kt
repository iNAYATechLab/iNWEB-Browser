package com.inweb.browser.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterListParserTest {

    @Test
    fun commentsAndHeaderAreCountedNotParsed() {
        val list = FilterListParser.parse(
            "[Adblock Plus 2.0]\n" +
                "! a comment\n" +
                "! another comment\n",
            listId = "test",
        )
        assertEquals(0, list.networkRules.size)
        assertEquals(3, list.commentCount)
    }

    @Test
    fun cosmeticRulesAreRecognizedAndCounted() {
        val list = FilterListParser.parse(
            "example.com##.ad-banner\n" +
                "##.sponsor\n" +
                "||ads.example.com^\n",
        )
        assertEquals(2, list.cosmeticRuleCount)
        assertEquals(1, list.networkRules.size)
    }

    @Test
    fun parsesDomainAnchoredRule() {
        val list = FilterListParser.parse("||ads.example.com^")
        val rule = list.networkRules.single()
        assertTrue(rule.domainAnchor)
        assertFalse(rule.anchorStart)
        assertFalse(rule.anchorEnd)
        assertFalse(rule.isException)
        assertEquals("ads.example.com^", rule.pattern)
        assertEquals("||ads.example.com^", rule.raw)
    }

    @Test
    fun parsesExceptionRule() {
        val rule = FilterListParser.parse("@@||example.com^").networkRules.single()
        assertTrue(rule.isException)
        assertEquals("example.com^", rule.pattern)
    }

    @Test
    fun parsesLeftAndRightAnchors() {
        val left = FilterListParser.parse("|https://ads.example.com/x").networkRules.single()
        assertTrue(left.anchorStart)
        assertFalse(left.domainAnchor)

        val right = FilterListParser.parse("/banner*.gif|").networkRules.single()
        assertTrue(right.anchorEnd)
        assertFalse(right.anchorStart)
        assertFalse(right.domainAnchor)
    }

    @Test
    fun parsesTypeAndPartyOptions() {
        val rule = FilterListParser.parse("||ads.example.com^\$script,third-party")
            .networkRules.single()
        assertEquals(setOf(ResourceType.SCRIPT), rule.options.types)
        assertEquals(true, rule.options.thirdParty)
    }

    @Test
    fun parsesDomainIncludeAndExcludeOptions() {
        val rule = FilterListParser.parse("||ads.example.com^\$domain=news.example|~meta.news.example")
            .networkRules.single()
        assertEquals(mapOf("news.example" to true, "meta.news.example" to false), rule.options.domains)
    }

    @Test
    fun negatedTypeOptions() {
        val rule = FilterListParser.parse("||example.com^\$~script,~image").networkRules.single()
        assertEquals(setOf(ResourceType.SCRIPT, ResourceType.IMAGE), rule.options.negatedTypes)
        assertTrue(rule.options.types.isEmpty())
    }

    @Test
    fun unknownOptionMarksRuleUnsupported() {
        val list = FilterListParser.parse("||example.com^\$csp=script-src 'none'")
        assertEquals(0, list.networkRules.size)
        assertEquals(1, list.unsupportedRuleCount)
    }

    @Test
    fun malformedRulesAreCountedInvalid() {
        val list = FilterListParser.parse("||\n@@\n@@\$script\n")
        assertEquals(0, list.networkRules.size)
        assertEquals(3, list.invalidRuleCount)
    }
}
