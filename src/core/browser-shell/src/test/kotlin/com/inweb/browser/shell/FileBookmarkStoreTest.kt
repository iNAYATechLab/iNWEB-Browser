package com.inweb.browser.shell

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FileBookmarkStoreTest {

    private lateinit var directory: File
    private lateinit var file: File

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("inweb-bookmarks").toFile()
        file = File(directory, "bookmarks.tsv")
    }

    @After
    fun tearDown() {
        directory.walkBottomUp().forEach { it.delete() }
    }

    @Test
    fun bookmarksPersistAcrossStoreInstances() {
        FileBookmarkStore(file).apply {
            add("https://a.example.com/", "A", 1_000, folder = "News")
            add("https://b.example.com/", "B", 2_000)
        }
        val reloaded = FileBookmarkStore(file)
        assertEquals(2, reloaded.entryCount())
        assertEquals(listOf("https://a.example.com/", "https://b.example.com/"), reloaded.all().map { it.url })
        assertEquals("A", reloaded.all().first().title)
        assertEquals("News", reloaded.all().first().folder)
    }

    @Test
    fun insertionOrderSurvivesReload() {
        FileBookmarkStore(file).apply {
            add("https://c.example.com/", "C", 3_000)
            add("https://a.example.com/", "A", 1_000)
            add("https://b.example.com/", "B", 2_000)
        }
        val reloaded = FileBookmarkStore(file)
        assertEquals(
            listOf("https://c.example.com/", "https://a.example.com/", "https://b.example.com/"),
            reloaded.all().map { it.url },
        )
    }

    @Test
    fun duplicateUrlsAreStillDeduplicatedAfterReload() {
        val store = FileBookmarkStore(file)
        store.add("https://a.example.com/", "First", 1_000)
        store.add("https://a.example.com/", "Second", 2_000)

        assertEquals(1, FileBookmarkStore(file).entryCount())
        assertEquals("First", FileBookmarkStore(file).all().single().title)
    }

    @Test
    fun renamePersistsAcrossInstances() {
        val store = FileBookmarkStore(file)
        val entry = store.add("https://a.example.com/", "Old", 1_000)

        store.rename(entry.id, "New title")

        assertEquals("New title", FileBookmarkStore(file).all().single().title)
    }

    @Test
    fun movePersistsAcrossInstancesIncludingUnfiled() {
        val store = FileBookmarkStore(file)
        val entry = store.add("https://a.example.com/", "A", 1_000)

        store.move(entry.id, "Reading")
        assertEquals("Reading", FileBookmarkStore(file).all().single().folder)

        store.move(entry.id, null)
        assertNull(FileBookmarkStore(file).all().single().folder)
    }

    @Test
    fun deletePersistsAcrossInstances() {
        val store = FileBookmarkStore(file)
        val entry = store.add("https://a.example.com/", "A", 1_000)

        assertTrue(store.delete(entry.id))
        assertEquals(0, FileBookmarkStore(file).entryCount())
    }

    @Test
    fun corruptHeaderYieldsAFreshWritableStore() {
        file.writeText("not a bookmarks file at all\n")

        val store = FileBookmarkStore(file)
        assertEquals(0, store.entryCount())
        assertEquals(0, store.lastLoadSkippedLines)

        store.add("https://a.example.com/", "A", 1_000)
        assertEquals(1, FileBookmarkStore(file).entryCount())
    }

    @Test
    fun malformedLinesUnderValidHeaderAreSkippedAndCounted() {
        val store = FileBookmarkStore(file)
        store.add("https://a.example.com/", "A", 1_000)
        val goodLine = file.readText().lines()[1]

        file.writeText(
            "iNWEB-BOOKMARKS v=1\n" +
                goodLine + "\n" +
                "bookmark\tonly\tfive\tcolumns\there\n" +
                "bookmark\tb-x\tnot-a-number\tNews\thttps://x.example.com/\tX\n" +
                "bookmark\tb-7\t5000\tReading\thttps://b.example.com/\tB\n",
        )

        val reloaded = FileBookmarkStore(file)
        assertEquals(2, reloaded.entryCount())
        assertEquals(2, reloaded.lastLoadSkippedLines)
        // insertion order preserved: A (file line first) then B
        assertEquals(
            listOf("https://a.example.com/", "https://b.example.com/"),
            reloaded.all().map { it.url },
        )
    }

    @Test
    fun tabsAndNewlinesAreSanitizedOnWrite() {
        FileBookmarkStore(file).apply {
            add("https://a.example.com/x", "Tab\tNew\nLine", 1_000, folder = "Fol\tder")
        }
        val entry = FileBookmarkStore(file).all().single()
        assertEquals("Tab New Line", entry.title)
        assertEquals("Fol der", entry.folder)
        // header + exactly one unambiguous bookmark line on disk
        assertEquals(2, file.readText().lines().count { it.isNotEmpty() })
    }

    @Test
    fun idsRemainUniqueAcrossReloads() {
        val store = FileBookmarkStore(file)
        store.add("https://a.example.com/", "A", 1_000)
        store.add("https://b.example.com/", "B", 2_000)

        val reloaded = FileBookmarkStore(file)
        reloaded.add("https://c.example.com/", "C", 3_000)

        val ids = reloaded.all().map { it.id }
        assertEquals(3, ids.size)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun blankUrlIsRejectedBeforeAnythingIsWritten() {
        val store = FileBookmarkStore(file)
        try {
            store.add("   ", "Blank", 1_000)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // documented contract — nothing persisted
        }
        assertEquals(0, store.entryCount())
        assertFalse(store.isBookmarked("   "))
    }
}
