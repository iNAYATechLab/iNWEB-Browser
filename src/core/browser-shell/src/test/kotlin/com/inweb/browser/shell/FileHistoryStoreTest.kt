package com.inweb.browser.shell

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FileHistoryStoreTest {

    private lateinit var directory: File
    private lateinit var file: File

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("inweb-history").toFile()
        file = File(directory, "history.tsv")
    }

    @After
    fun tearDown() {
        directory.walkBottomUp().forEach { it.delete() }
    }

    @Test
    fun visitsPersistAcrossStoreInstances() {
        FileHistoryStore(file).apply {
            recordVisit("https://a.example.com/", "A", 1_000)
            recordVisit("https://b.example.com/", "B", 2_000)
        }
        val reloaded = FileHistoryStore(file)
        assertEquals(2, reloaded.entryCount())
        val recent = reloaded.recent(10)
        assertEquals(listOf("https://b.example.com/", "https://a.example.com/"), recent.map { it.url })
        assertEquals("B", recent[0].title)
        assertEquals(2_000, recent[0].visitedAtMillis)
    }

    @Test
    fun recentDeduplicatesByUrlNewestFirst() {
        FileHistoryStore(file).apply {
            recordVisit("https://a.example.com/", "first", 1_000)
            recordVisit("https://b.example.com/", "other", 2_000)
            recordVisit("https://a.example.com/", "second", 3_000)
        }
        val recent = FileHistoryStore(file).recent(10)
        assertEquals(listOf("https://a.example.com/", "https://b.example.com/"), recent.map { it.url })
        assertEquals("second", recent[0].title)
    }

    @Test
    fun searchMatchesUrlAndTitleCaseInsensitively() {
        FileHistoryStore(file).apply {
            recordVisit("https://news.example.com/politics", "Politics", 1_000)
            recordVisit("https://shop.example.com/", "Best OFFERS", 2_000)
        }
        val store = FileHistoryStore(file)
        assertEquals("Politics", store.search("POLITICS", 10).single().title)
        assertEquals("Best OFFERS", store.search("offers", 10).single().title)
        assertTrue(store.search("nomatch", 10).isEmpty())
    }

    @Test
    fun deletePersistsAcrossInstances() {
        val store = FileHistoryStore(file)
        store.recordVisit("https://a.example.com/", "A", 1_000)
        val id = store.recent(10).single().id

        assertTrue(store.delete(id))
        assertFalse(store.delete(id))

        assertEquals(0, FileHistoryStore(file).entryCount())
    }

    @Test
    fun deleteRangeRemovesOnlyTheRange() {
        val store = FileHistoryStore(file)
        store.recordVisit("https://a.example.com/", "A", 1_000)
        store.recordVisit("https://b.example.com/", "B", 2_000)
        store.recordVisit("https://c.example.com/", "C", 3_000)

        assertEquals(1, store.deleteRange(2_000, 2_000))

        val reloaded = FileHistoryStore(file)
        assertEquals(listOf("https://c.example.com/", "https://a.example.com/"), reloaded.recent(10).map { it.url })
    }

    @Test
    fun clearAllPersistsAnEmptyStore() {
        val store = FileHistoryStore(file)
        store.recordVisit("https://a.example.com/", "A", 1_000)
        store.recordVisit("https://b.example.com/", "B", 2_000)

        assertEquals(2, store.clearAll())
        assertEquals(0, FileHistoryStore(file).entryCount())
    }

    @Test
    fun corruptHeaderYieldsAFreshStore() {
        file.writeText("this is not a history file\nvisits go here\n")

        val store = FileHistoryStore(file)
        assertEquals(0, store.entryCount())
        assertEquals(0, store.lastLoadSkippedLines)

        // The store stays writable: the next mutation replaces the junk file.
        store.recordVisit("https://a.example.com/", "A", 1_000)
        assertEquals(1, FileHistoryStore(file).entryCount())
    }

    @Test
    fun malformedLinesUnderValidHeaderAreSkippedAndCounted() {
        val id = FileHistoryStore(file).apply {
            recordVisit("https://a.example.com/", "A", 1_000)
        }.recent(10).single().id
        val goodLine = file.readText().lines()[1]

        file.writeText(
            "iNWEB-HISTORY v=1\n" +
                goodLine + "\n" +
                "visit\tonly\tfour\tfields\n" +
                "visit\th-x\tnot-a-number\thttps://x.example.com/\tX\n" +
                "visit\th-99\t5000\thttps://b.example.com/\tB\n",
        )

        val store = FileHistoryStore(file)
        assertEquals(2, store.entryCount())
        assertEquals(2, store.lastLoadSkippedLines)
        // newest first: B (5000) then A (1000)
        assertEquals(id, store.recent(10).last().id)
    }

    @Test
    fun privateVisitsNeverReachDisk() {
        val decorated = PrivacyFilterHistory(FileHistoryStore(file))

        decorated.recordVisit("https://secret.example.com/", "Secret", 1_000, isPrivate = true)
        decorated.recordVisit("https://public.example.com/", "Public", 2_000, isPrivate = false)

        val onDisk = file.readText()
        assertFalse(onDisk.contains("secret"))
        assertEquals(1, FileHistoryStore(file).entryCount())
    }

    @Test
    fun tabsAndNewlinesAreSanitizedOnWrite() {
        FileHistoryStore(file).apply {
            recordVisit("https://a.example.com/", "Tab\tNew\nLine", 1_000)
        }
        val entry = FileHistoryStore(file).recent(10).single()
        assertEquals("Tab New Line", entry.title)
        // exactly one visit line on disk (sanitizing kept the format intact)
        assertEquals(2, file.readText().lines().count { it.isNotEmpty() })
    }

    @Test
    fun idsRemainUniqueAcrossReloads() {
        val store = FileHistoryStore(file)
        store.recordVisit("https://a.example.com/", "A", 1_000)
        store.recordVisit("https://b.example.com/", "B", 2_000)

        val reloaded = FileHistoryStore(file)
        reloaded.recordVisit("https://c.example.com/", "C", 3_000)

        val ids = reloaded.recent(10).map { it.id }
        assertEquals(3, ids.size)
        assertEquals(ids.size, ids.toSet().size)
    }
}
