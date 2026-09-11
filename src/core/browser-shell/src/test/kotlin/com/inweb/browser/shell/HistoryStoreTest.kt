package com.inweb.browser.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryStoreTest {

    @Test
    fun recentReturnsNewestDistinctUrlsFirst() {
        val store = InMemoryHistoryStore()
        store.recordVisit("https://a.example", "A", 100)
        store.recordVisit("https://b.example", "B", 200)
        store.recordVisit("https://a.example", "A again", 300)

        val recent = store.recent(10)
        assertEquals(2, recent.size)
        assertEquals("https://a.example", recent[0].url)
        assertEquals("A again", recent[0].title)
        assertEquals("https://b.example", recent[1].url)
    }

    @Test
    fun recentRespectsLimit() {
        val store = InMemoryHistoryStore()
        store.recordVisit("https://a.example", "A", 100)
        store.recordVisit("https://b.example", "B", 200)
        assertEquals(1, store.recent(1).size)
    }

    @Test
    fun searchMatchesUrlAndTitleCaseInsensitive() {
        val store = InMemoryHistoryStore()
        store.recordVisit("https://example.com/kotlin-guide", "Kotlin Docs", 100)
        store.recordVisit("https://example.com/other", "Cooking", 200)

        assertEquals(1, store.search("KOTLIN", 10).size)
        assertEquals(1, store.search("cooking", 10).size)
        assertEquals(0, store.search("no-such-thing", 10).size)
    }

    @Test
    fun searchWithBlankQueryFallsBackToRecent() {
        val store = InMemoryHistoryStore()
        store.recordVisit("https://a.example", "A", 100)
        assertEquals(1, store.search("   ", 10).size)
    }

    @Test
    fun deleteByIdRemovesEntry() {
        val store = InMemoryHistoryStore()
        store.recordVisit("https://a.example", "A", 100)
        val id = store.recent(1)[0].id
        assertTrue(store.delete(id))
        assertEquals(0, store.entryCount())
        assertFalse(store.delete(id))
    }

    @Test
    fun deleteRangeRemovesOnlyEntriesInRange() {
        val store = InMemoryHistoryStore()
        store.recordVisit("https://a.example", "A", 100)
        store.recordVisit("https://b.example", "B", 200)
        store.recordVisit("https://c.example", "C", 300)

        val deleted = store.deleteRange(fromMillis = 150, toMillis = 250)
        assertEquals(1, deleted)
        // recent() is newest-first: c (t=300) before a (t=100)
        assertEquals(listOf("https://c.example", "https://a.example"), store.recent(10).map { it.url })
    }

    @Test
    fun clearAllReturnsRemovedCount() {
        val store = InMemoryHistoryStore()
        store.recordVisit("https://a.example", "A", 100)
        store.recordVisit("https://b.example", "B", 200)
        assertEquals(2, store.clearAll())
        assertEquals(0, store.entryCount())
    }

    @Test
    fun privacyFilterNeverStoresPrivateVisits() {
        val backing = InMemoryHistoryStore()
        val store = PrivacyFilterHistory(backing)

        assertFalse(store.recordVisit("https://secret.example", "Secret", 100, isPrivate = true))
        assertEquals(0, backing.entryCount())

        assertTrue(store.recordVisit("https://open.example", "Open", 200, isPrivate = false))
        assertEquals(1, backing.entryCount())
        assertEquals(1, store.recent(10).size)
    }
}
