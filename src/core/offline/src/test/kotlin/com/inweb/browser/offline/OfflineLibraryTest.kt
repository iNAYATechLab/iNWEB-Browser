package com.inweb.browser.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineLibraryTest {

    private fun page(
        url: String,
        size: Long = 100L,
        title: String = "Page",
        file: String = "snap.mhtml",
        createdAt: Long = 1_000L,
        accessedAt: Long = createdAt,
    ) = OfflinePageRecord(
        onlineUrl = url,
        title = title,
        snapshotFileName = file,
        sizeBytes = size,
        createdAtMillis = createdAt,
        lastAccessedAtMillis = accessedAt,
    )

    private fun ok(result: LibraryResult<SavedPage>): SavedPage = (result as LibraryResult.Ok).value

    private fun record(result: LibraryResult<OfflinePageRecord>): OfflinePageRecord =
        (result as LibraryResult.Ok).value

    // --- save / replace / validate ---------------------------------------

    @Test
    fun saveRegistersRecordInInsertionOrder() {
        val library = OfflineLibrary(quotaBytes = 1_000L)
        ok(library.save(page("https://a.example/one", file = "a.mhtml")))
        ok(library.save(page("https://b.example/two", file = "b.mhtml")))

        assertEquals(
            listOf("https://a.example/one", "https://b.example/two"),
            library.all().map { it.onlineUrl },
        )
        assertEquals("a.mhtml", library.get("https://a.example/one")?.snapshotFileName)
    }

    @Test
    fun savingSameUrlReplacesAndReturnsOldRecord() {
        val library = OfflineLibrary(quotaBytes = 1_000L)
        ok(library.save(page("https://a.example/", size = 300L, file = "old.mhtml")))
        val saved = ok(library.save(page("https://a.example/", size = 500L, file = "new.mhtml")))

        assertEquals("old.mhtml", saved.replaced?.snapshotFileName)
        assertTrue(saved.evicted.isEmpty())
        assertEquals("new.mhtml", library.get("https://a.example/")?.snapshotFileName)
        assertEquals(500L, library.totalBytes()) // old bytes no longer count
    }

    @Test
    fun invalidRecordsAreRejected() {
        val library = OfflineLibrary()
        assertTrue(library.save(page("  ", file = "f")) is LibraryResult.Err)
        assertTrue(library.save(page("https://a.example/", file = "")) is LibraryResult.Err)
        assertTrue(library.save(page("https://a.example/", size = 0L)) is LibraryResult.Err)
        assertTrue(library.save(page("https://a.example/", size = -5L)) is LibraryResult.Err)
        assertTrue(library.all().isEmpty())
    }

    // --- access / delete ---------------------------------------------------

    @Test
    fun accessUpdatesLastAccessedTime() {
        val library = OfflineLibrary()
        ok(library.save(page("https://a.example/", createdAt = 1_000L)))
        val accessed = record(library.access("https://a.example/", 5_000L))
        assertEquals(5_000L, accessed.lastAccessedAtMillis)
        assertEquals(5_000L, library.get("https://a.example/")?.lastAccessedAtMillis)
    }

    @Test
    fun accessTimeNeverRewinds() {
        val library = OfflineLibrary()
        ok(library.save(page("https://a.example/", createdAt = 1_000L)))
        record(library.access("https://a.example/", 5_000L))
        val accessed = record(library.access("https://a.example/", 2_000L))
        assertEquals(5_000L, accessed.lastAccessedAtMillis)
    }

    @Test
    fun accessToMissingPageFails() {
        assertTrue(OfflineLibrary().access("https://missing.example/", 1L) is LibraryResult.Err)
    }

    @Test
    fun deleteRemovesAndReturnsTheRecord() {
        val library = OfflineLibrary()
        ok(library.save(page("https://a.example/", file = "a.mhtml")))
        val removed = record(library.delete("https://a.example/"))
        assertEquals("a.mhtml", removed.snapshotFileName)
        assertNull(library.get("https://a.example/"))
        assertEquals(0L, library.totalBytes())
        assertTrue(library.delete("https://a.example/") is LibraryResult.Err)
    }

    // --- quota accounting + LRU eviction -----------------------------------

    @Test
    fun quotaEvictsLeastRecentlyAccessedFirst() {
        val library = OfflineLibrary(quotaBytes = 300L)
        ok(library.save(page("https://old.example/", size = 100L, createdAt = 100L, accessedAt = 100L)))
        ok(library.save(page("https://mid.example/", size = 100L, createdAt = 200L, accessedAt = 200L)))
        ok(library.save(page("https://new.example/", size = 100L, createdAt = 300L, accessedAt = 300L)))

        val saved = ok(library.save(page("https://incoming.example/", size = 100L, createdAt = 400L)))

        assertEquals(listOf("https://old.example/"), saved.evicted.map { it.onlineUrl })
        assertNull(library.get("https://old.example/"))
        assertEquals(300L, library.totalBytes())
    }

    @Test
    fun evictionSkipsPinnedPages() {
        val library = OfflineLibrary(quotaBytes = 300L)
        ok(library.save(page("https://pinned.example/", size = 100L, createdAt = 100L, accessedAt = 100L)))
        ok(library.save(page("https://next.example/", size = 100L, createdAt = 200L, accessedAt = 200L)))
        ok(library.save(page("https://new.example/", size = 100L, createdAt = 300L, accessedAt = 300L)))

        val saved = ok(
            library.save(
                page("https://incoming.example/", size = 100L, createdAt = 400L),
                pinnedUrls = setOf("https://pinned.example/"),
            ),
        )

        // the pinned LRU page is skipped; the next-least-recent is evicted
        assertEquals(listOf("https://next.example/"), saved.evicted.map { it.onlineUrl })
        assertTrue(library.get("https://pinned.example/") != null)
    }

    @Test
    fun quotaFailureIsAtomicAndKeepsEverything() {
        val library = OfflineLibrary(quotaBytes = 300L)
        ok(library.save(page("https://a.example/", size = 100L, createdAt = 100L, accessedAt = 100L)))
        ok(library.save(page("https://b.example/", size = 100L, createdAt = 200L, accessedAt = 200L)))
        ok(library.save(page("https://c.example/", size = 100L, createdAt = 300L, accessedAt = 300L)))

        val result = library.save(page("https://huge.example/", size = 500L))

        assertTrue(result is LibraryResult.Err)
        assertEquals(3, library.all().size)      // nothing evicted, nothing added
        assertEquals(300L, library.totalBytes())
    }

    @Test
    fun oversizedSingleRecordCannotEvictItsWayIn() {
        val library = OfflineLibrary(quotaBytes = 200L)
        ok(library.save(page("https://a.example/", size = 100L)))
        assertTrue(library.save(page("https://huge.example/", size = 10_000L)) is LibraryResult.Err)
        assertEquals(1, library.all().size)
    }

    @Test
    fun evictionTieBreaksByOlderCreationFirst() {
        val library = OfflineLibrary(quotaBytes = 300L)
        // both accessed at the same time; created at different times
        ok(library.save(page("https://older.example/", size = 100L, createdAt = 100L, accessedAt = 500L)))
        ok(library.save(page("https://younger.example/", size = 100L, createdAt = 200L, accessedAt = 500L)))
        ok(library.save(page("https://keeper.example/", size = 100L, createdAt = 300L, accessedAt = 900L)))

        val saved = ok(library.save(page("https://incoming.example/", size = 100L, createdAt = 400L)))

        assertEquals(listOf("https://older.example/"), saved.evicted.map { it.onlineUrl })
    }

    @Test
    fun remainingBytesReportsQuotaMinusRealTotal() {
        val library = OfflineLibrary(quotaBytes = 1_000L)
        assertEquals(1_000L, library.remainingBytes())
        ok(library.save(page("https://a.example/", size = 250L)))
        ok(library.save(page("https://b.example/", size = 250L)))
        assertEquals(500L, library.remainingBytes())
        assertEquals(500L, library.totalBytes())
    }

    // --- persistence seam ----------------------------------------------------

    @Test
    fun libraryStateSurvivesStoreRoundTrip() {
        val store = InMemoryOfflineStore()
        val first = OfflineLibrary(store, quotaBytes = 1_000L)
        ok(first.save(page("https://a.example/", file = "a.mhtml")))
        ok(first.save(page("https://b.example/", file = "b.mhtml")))
        record(first.access("https://a.example/", 9_000L))

        val second = OfflineLibrary(store, quotaBytes = 1_000L)
        assertEquals(2, second.all().size)
        assertEquals(9_000L, second.get("https://a.example/")?.lastAccessedAtMillis)
        assertEquals(listOf("a.mhtml", "b.mhtml"), second.all().map { it.snapshotFileName })
    }
}
