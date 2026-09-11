package com.inweb.browser.privacy.lists

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FileFilterListCacheTest {

    private lateinit var directory: File
    private lateinit var cache: FileFilterListCache

    private val source = FilterListSource("test-list", "Test List", "https://example.com/list.txt")
    private val other = FilterListSource("other-list", "Other List", "https://example.com/other.txt")

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("inweb-cache").toFile()
        cache = FileFilterListCache(directory)
    }

    @After
    fun tearDown() {
        directory.walkBottomUp().forEach { it.delete() }
    }

    private fun sampleMetadata(etag: String? = "\"v1\"") = FilterListMetadata(
        sourceId = source.id,
        downloadedAtMillis = 1_757_683_200_000,
        etag = etag,
        lastModified = "Fri, 11 Sep 2026 00:00:00 GMT",
        version = "202609110000",
        ruleCount = 25_310,
    )

    @Test
    fun storeAndLoadBodyRoundTrip() {
        val body = "! Title: Test\n||ads.example.com^\n"
        cache.store(source, body, sampleMetadata())
        assertEquals(body, cache.loadBody(source))
    }

    @Test
    fun metadataRoundTripsWithAllFields() {
        cache.store(source, "body", sampleMetadata())
        val loaded = cache.loadMetadata(source)
        assertNotNull(loaded)
        assertEquals("test-list", loaded!!.sourceId)
        assertEquals(1_757_683_200_000, loaded.downloadedAtMillis)
        assertEquals("\"v1\"", loaded.etag)
        assertEquals("Fri, 11 Sep 2026 00:00:00 GMT", loaded.lastModified)
        assertEquals("202609110000", loaded.version)
        assertEquals(25_310, loaded.ruleCount)
    }

    @Test
    fun nullableFieldsSurviveRoundTrip() {
        cache.store(source, "body", FilterListMetadata(source.id, 42))
        val loaded = cache.loadMetadata(source)
        assertNotNull(loaded)
        assertNull(loaded!!.etag)
        assertNull(loaded.lastModified)
        assertNull(loaded.version)
        assertEquals(0, loaded.ruleCount)
    }

    @Test
    fun storeOverwritesPreviousContent() {
        cache.store(source, "v1-body", sampleMetadata("\"v1\""))
        cache.store(source, "v2-body", sampleMetadata("\"v2\""))
        assertEquals("v2-body", cache.loadBody(source))
        assertEquals("\"v2\"", cache.loadMetadata(source)!!.etag)
    }

    @Test
    fun missingEntryReturnsNull() {
        assertNull(cache.loadBody(source))
        assertNull(cache.loadMetadata(source))
    }

    @Test
    fun removeDeletesEntry() {
        cache.store(source, "body", sampleMetadata())
        cache.remove(source)
        assertNull(cache.loadBody(source))
        assertNull(cache.loadMetadata(source))
    }

    @Test
    fun clearRemovesAllEntries() {
        cache.store(source, "body-a", sampleMetadata())
        cache.store(other, "body-b", FilterListMetadata(other.id, 1))
        cache.clear()
        assertNull(cache.loadBody(source))
        assertNull(cache.loadBody(other))
        assertTrue(directory.listFiles()!!.isEmpty())
    }

    @Test
    fun corruptMetadataFileYieldsNullButBodyStaysUsable() {
        cache.store(source, "body", sampleMetadata())
        File(directory, "${source.id}.meta").writeText("not a metadata file\n")
        assertNull(cache.loadMetadata(source))
        assertEquals("body", cache.loadBody(source))
    }

    @Test
    fun corruptMetadataRoundTripIsRejected() {
        // truncated / tampered lines with bad numbers must not crash parsing
        val text = FileFilterListCache.serialize(sampleMetadata())
            .replace("1757683200000", "not-a-number")
        assertNull(FileFilterListCache.deserialize(text))
    }
}
