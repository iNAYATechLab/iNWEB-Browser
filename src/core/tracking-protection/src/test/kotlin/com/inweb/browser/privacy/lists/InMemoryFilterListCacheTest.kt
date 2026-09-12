package com.inweb.browser.privacy.lists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryFilterListCacheTest {

    private val source = FilterListSource("easylist", "EasyList", "https://example.org/easylist.txt")
    private val other = FilterListSource("easyprivacy", "EasyPrivacy", "https://example.org/easyprivacy.txt")

    @Test
    fun storeLoadRoundTrip() {
        val cache = InMemoryFilterListCache()
        val meta = FilterListMetadata(sourceId = "easylist", downloadedAtMillis = 1_000L)
        cache.store(source, "rule-a", meta)
        assertEquals("rule-a", cache.loadBody(source))
        assertEquals(meta, cache.loadMetadata(source))
        assertNull(cache.loadBody(other))
        assertNull(cache.loadMetadata(other))
    }

    @Test
    fun storeReplacesPreviousContent() {
        val cache = InMemoryFilterListCache()
        cache.store(source, "rule-a", FilterListMetadata("easylist", 1_000L))
        cache.store(source, "rule-b", FilterListMetadata("easylist", 2_000L, etag = "\"v2\""))
        assertEquals("rule-b", cache.loadBody(source))
        assertEquals(2_000L, cache.loadMetadata(source)?.downloadedAtMillis ?: 0L)
    }

    @Test
    fun removeDeletesOnlyThatSource() {
        val cache = InMemoryFilterListCache()
        cache.store(source, "rule-a", FilterListMetadata("easylist", 1_000L))
        cache.store(other, "rule-b", FilterListMetadata("easyprivacy", 1_000L))
        cache.remove(source)
        assertNull(cache.loadBody(source))
        assertNull(cache.loadMetadata(source))
        assertEquals("rule-b", cache.loadBody(other))
    }

    @Test
    fun clearDeletesEverything() {
        val cache = InMemoryFilterListCache()
        cache.store(source, "rule-a", FilterListMetadata("easylist", 1_000L))
        cache.store(other, "rule-b", FilterListMetadata("easyprivacy", 1_000L))
        cache.clear()
        assertNull(cache.loadBody(source))
        assertNull(cache.loadBody(other))
        assertNull(cache.loadMetadata(source))
    }
}
