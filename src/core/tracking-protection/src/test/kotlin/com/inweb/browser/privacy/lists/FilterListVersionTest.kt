package com.inweb.browser.privacy.lists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FilterListVersionTest {

    @Test
    fun parsesEasyListStyleHeaders() {
        val info = FilterListVersion.parse(
            "[Adblock Plus 2.0]\n" +
                "! Title: EasyList\n" +
                "! Last modified: 12 Sep 2026 03:11 UTC\n" +
                "! Version: 202609120311\n" +
                "||ads.example.com^\n",
        )
        assertEquals("202609120311", info.version)
        assertEquals("12 Sep 2026 03:11 UTC", info.lastModified)
    }

    @Test
    fun missingHeadersYieldNulls() {
        val info = FilterListVersion.parse("||ads.example.com^\n##.banner\n")
        assertNull(info.version)
        assertNull(info.lastModified)
    }

    @Test
    fun firstOccurrenceWins() {
        val info = FilterListVersion.parse(
            "! Version: 1\n! Version: 2\n",
        )
        assertEquals("1", info.version)
    }

    @Test
    fun prefixMatchIsCaseInsensitive() {
        val info = FilterListVersion.parse("! version: beta-7\n")
        assertEquals("beta-7", info.version)
    }

    @Test
    fun emptyValueYieldsNull() {
        val info = FilterListVersion.parse("! Version:\n")
        assertNull(info.version)
    }
}
