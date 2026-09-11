package com.inweb.browser.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkStoreTest {

    private val store = InMemoryBookmarkStore()

    @Test
    fun addStoresAndAllReturnsInsertionOrder() {
        store.add("https://a.example.com/", "A", 1_000)
        store.add("https://b.example.com/", "B", 2_000)
        store.add("https://c.example.com/", "C", 3_000)

        assertEquals(
            listOf("https://a.example.com/", "https://b.example.com/", "https://c.example.com/"),
            store.all().map { it.url },
        )
    }

    @Test
    fun duplicateUrlReturnsExistingEntryWithoutStoringTwice() {
        val first = store.add("https://a.example.com/", "First title", 1_000)
        store.add("https://other.example.com/", "Other", 2_000)
        val duplicate = store.add("https://a.example.com/", "Second title", 3_000)

        assertEquals(first.id, duplicate.id)
        assertEquals("First title", duplicate.title)
        assertEquals(2, store.entryCount())
    }

    @Test
    fun foldersAreDistinctInFirstSeenOrderWithoutUnfiled() {
        store.add("https://a.example.com/", "A", 1_000, folder = "News")
        store.add("https://b.example.com/", "B", 2_000) // unfiled
        store.add("https://c.example.com/", "C", 3_000, folder = "News")
        store.add("https://d.example.com/", "D", 4_000, folder = "Shopping")

        assertEquals(listOf("News", "Shopping"), store.folders())
    }

    @Test
    fun byFolderFiltersAndNullMeansUnfiled() {
        store.add("https://a.example.com/", "A", 1_000, folder = "News")
        store.add("https://b.example.com/", "B", 2_000)
        store.add("https://c.example.com/", "C", 3_000, folder = "Shopping")
        store.add("https://d.example.com/", "D", 4_000)

        assertEquals(listOf("https://a.example.com/"), store.byFolder("News").map { it.url })
        assertEquals(
            listOf("https://b.example.com/", "https://d.example.com/"),
            store.byFolder(null).map { it.url },
        )
    }

    @Test
    fun renameKeepsPositionAndUpdatesTitle() {
        val entry = store.add("https://a.example.com/", "Old", 1_000)
        store.add("https://b.example.com/", "B", 2_000)

        val renamed = store.rename(entry.id, "New title")

        assertNotNull(renamed)
        assertEquals("New title", store.all().first().title)
        assertEquals(entry.id, store.all().first().id)
        assertNull(store.rename("missing-id", "X"))
    }

    @Test
    fun moveChangesFolderIncludingBackToUnfiled() {
        val entry = store.add("https://a.example.com/", "A", 1_000)

        assertEquals("News", store.move(entry.id, "News")?.folder)
        assertEquals("News", store.folders().single())
        // moving back to unfiled
        assertNull(store.move(entry.id, null)?.folder)
        assertTrue(store.folders().isEmpty())
        assertNull(store.move("missing-id", "News"))
    }

    @Test
    fun blankFolderNameIsTreatedAsUnfiled() {
        store.add("https://a.example.com/", "A", 1_000, folder = "   ")
        assertNull(store.all().single().folder)
        assertTrue(store.folders().isEmpty())
    }

    @Test
    fun deleteAndClearAllWork() {
        val a = store.add("https://a.example.com/", "A", 1_000)
        store.add("https://b.example.com/", "B", 2_000)

        assertTrue(store.delete(a.id))
        assertFalse(store.delete(a.id))
        assertEquals(1, store.entryCount())

        assertEquals(1, store.clearAll())
        assertEquals(0, store.entryCount())
    }

    @Test
    fun findAndIsBookmarked() {
        val entry = store.add("https://a.example.com/", "A", 1_000)

        assertEquals(entry, store.find(entry.id))
        assertNull(store.find("missing-id"))
        assertTrue(store.isBookmarked("https://a.example.com/"))
        assertFalse(store.isBookmarked("https://other.example.com/"))
    }
}
