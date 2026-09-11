package com.inweb.browser.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopSitesTest {

    private fun visit(url: String, title: String = url, at: Long) =
        HistoryEntry("h-0", url, title, at)

    @Test
    fun ordersByVisitCountDescending() {
        val sites = TopSites.compute(
            listOf(
                visit("https://a.example.com/", at = 1_000),
                visit("https://b.example.com/", at = 2_000),
                visit("https://b.example.com/", at = 3_000),
                visit("https://b.example.com/", at = 4_000),
            ),
            limit = 10,
        )
        assertEquals(listOf("https://b.example.com/", "https://a.example.com/"), sites.map { it.url })
        assertEquals(3, sites[0].visitCount)
        assertEquals(1, sites[1].visitCount)
    }

    @Test
    fun tieBreaksByMostRecentVisit() {
        val sites = TopSites.compute(
            listOf(
                visit("https://old.example.com/", at = 1_000),
                visit("https://new.example.com/", at = 4_000),
                visit("https://old.example.com/", at = 2_000),
                visit("https://new.example.com/", at = 5_000),
            ),
            limit = 10,
        )
        // both have 2 visits; new.example.com was visited most recently
        assertEquals(listOf("https://new.example.com/", "https://old.example.com/"), sites.map { it.url })
    }

    @Test
    fun latestTitleWins() {
        val sites = TopSites.compute(
            listOf(
                visit("https://a.example.com/", title = "Old title", at = 1_000),
                visit("https://a.example.com/", title = "New title", at = 2_000),
            ),
            limit = 10,
        )
        assertEquals("New title", sites.single().title)
    }

    @Test
    fun respectsTheLimit() {
        val sites = TopSites.compute(
            listOf(
                visit("https://a.example.com/", at = 1_000),
                visit("https://b.example.com/", at = 2_000),
                visit("https://c.example.com/", at = 3_000),
            ),
            limit = 2,
        )
        assertEquals(2, sites.size)
        assertEquals(listOf("https://c.example.com/", "https://b.example.com/"), sites.map { it.url })
    }

    @Test
    fun emptyVisitsYieldNoSites() {
        assertTrue(TopSites.compute(emptyList(), limit = 5).isEmpty())
    }

    @Test
    fun singleVisitBecomesOneTopSite() {
        val sites = TopSites.compute(
            listOf(visit("https://a.example.com/", title = "A", at = 1_000)),
            limit = 5,
        )
        assertEquals(1, sites.size)
        assertEquals("A", sites.single().title)
        assertEquals(1, sites.single().visitCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroLimitRejected() {
        TopSites.compute(emptyList(), limit = 0)
    }
}
