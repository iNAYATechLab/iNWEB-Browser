package com.inweb.browser.privacy.lists

import com.inweb.browser.privacy.FilterAction
import com.inweb.browser.privacy.ResourceType
import com.inweb.browser.privacy.TrackingProtectionSettings
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterListManagerTest {

    private val source = FilterListSource("unit-list", "Unit List", "https://lists.example.com/unit.txt")

    private val bodyV1 = "! Title: Unit\n! Version: v1\n||ads.example.com^\n||tracker.net^\$script\n"
    private val bodyV2 = "! Title: Unit\n! Version: v2\n||ads.example.com^\n||tracker.net^\$script\n||extra.net^\n"

    private class ScriptedFetcher(vararg responses: FetchResult) : FilterListFetcher {
        private val queue = ArrayDeque(responses.toList())
        val requests = mutableListOf<Triple<String, String?, String?>>()
        override fun fetch(url: String, etag: String?, lastModified: String?): FetchResult {
            requests += Triple(url, etag, lastModified)
            return if (queue.isEmpty()) FetchResult.Failure("network unavailable") else queue.removeFirst()
        }
    }

    private fun newCache(): FileFilterListCache =
        FileFilterListCache(Files.createTempDirectory("inweb-manager").toFile())

    private fun request(url: String) = com.inweb.browser.privacy.RequestContext(
        requestUrl = url,
        documentUrl = "https://news.example.com/story",
        resourceType = ResourceType.IMAGE,
    )

    @Test
    fun startupWithEmptyCacheDownloadsParsesAndCaches() {
        val cache = newCache()
        val fetcher = ScriptedFetcher(FetchResult.Success(bodyV1, "\"v1\"", null))
        val manager = FilterListManager(listOf(source), fetcher, cache)

        val status = manager.startup(nowMillis = 1_000)

        assertEquals(ListUpdateStatus.Updated("v1", 2), status["unit-list"])
        assertEquals(2, manager.totalNetworkRules())
        assertEquals(bodyV1, cache.loadBody(source))
        assertEquals("\"v1\"", cache.loadMetadata(source)!!.etag)
        assertEquals("v1", manager.metadataOf("unit-list")!!.version)

        val engine = manager.buildEngine()
        assertEquals(FilterAction.BLOCK, engine.decide(request("https://ads.example.com/x.gif")).action)
    }

    @Test
    fun secondStartupServesFromCacheWithoutNetwork() {
        val cache = newCache()
        ScriptedFetcher(FetchResult.Success(bodyV1, "\"v1\"", null)).let {
            FilterListManager(listOf(source), it, cache).startup(1_000)
        }

        val noNetwork = ScriptedFetcher() // any fetch would fail
        val manager = FilterListManager(listOf(source), noNetwork, cache)
        val status = manager.startup(nowMillis = 2_000)

        assertEquals(ListUpdateStatus.LoadedFromCache, status["unit-list"])
        assertTrue(noNetwork.requests.isEmpty())
        assertEquals(2, manager.totalNetworkRules())
    }

    @Test
    fun refresh200UpdatesEngineVersionAndCache() {
        val cache = newCache()
        val fetcher = ScriptedFetcher(
            FetchResult.Success(bodyV1, "\"v1\"", null),
            FetchResult.Success(bodyV2, "\"v2\"", null),
        )
        val manager = FilterListManager(listOf(source), fetcher, cache, UpdatePolicy(refreshIntervalMs = 10_000))
        manager.startup(nowMillis = 0)

        val status = manager.refresh(nowMillis = 100_000)

        assertEquals(ListUpdateStatus.Updated("v2", 3), status["unit-list"])
        assertEquals(3, manager.totalNetworkRules())
        assertEquals("v2", manager.metadataOf("unit-list")!!.version)
        assertEquals("\"v2\"", cache.loadMetadata(source)!!.etag)
        // refresh must have been conditional on the cached validator
        assertEquals("\"v1\"", fetcher.requests.last().second)
    }

    @Test
    fun refresh304KeepsContentAndRestartsTheInterval() {
        val cache = newCache()
        val fetcher = ScriptedFetcher(
            FetchResult.Success(bodyV1, "\"v1\"", null),
            FetchResult.NotModified(etag = null, lastModified = null),
        )
        val manager = FilterListManager(listOf(source), fetcher, cache, UpdatePolicy(refreshIntervalMs = 10_000))
        manager.startup(nowMillis = 0)

        val status = manager.refresh(nowMillis = 100_000)

        assertEquals(ListUpdateStatus.NotModified("v1"), status["unit-list"])
        assertEquals(2, manager.totalNetworkRules())
        assertEquals(100_000, manager.metadataOf("unit-list")!!.downloadedAtMillis)
        // server sent no validators on 304 — the old etag is preserved
        assertEquals("\"v1\"", manager.metadataOf("unit-list")!!.etag)
    }

    @Test
    fun refreshFailureFallsBackToCachedCopy() {
        val cache = newCache()
        val fetcher = ScriptedFetcher(
            FetchResult.Success(bodyV1, "\"v1\"", null),
            FetchResult.Failure("dns lookup failed"),
        )
        val manager = FilterListManager(listOf(source), fetcher, cache, UpdatePolicy(refreshIntervalMs = 10_000))
        manager.startup(nowMillis = 0)

        val status = manager.refresh(nowMillis = 100_000)

        assertEquals(ListUpdateStatus.Failed("dns lookup failed", servedFromCache = true), status["unit-list"])
        assertEquals(2, manager.totalNetworkRules())
        assertEquals("v1", manager.metadataOf("unit-list")!!.version)
        // the cache still holds the last good copy
        assertEquals(bodyV1, cache.loadBody(source))
    }

    @Test
    fun refreshFailureWithoutAnyCacheIsNotServedFromCache() {
        val cache = newCache()
        val fetcher = ScriptedFetcher(FetchResult.Failure("offline"))
        val manager = FilterListManager(listOf(source), fetcher, cache)

        val status = manager.refresh(nowMillis = 0)

        assertEquals(ListUpdateStatus.Failed("offline", servedFromCache = false), status["unit-list"])
        assertEquals(0, manager.totalNetworkRules())
    }

    @Test
    fun disabledSourceIsSkippedEverywhere() {
        val cache = newCache()
        val fetcher = ScriptedFetcher()
        val disabled = source.copy(enabled = false)
        val manager = FilterListManager(listOf(disabled), fetcher, cache)

        val status = manager.startup(nowMillis = 0)

        assertEquals(ListUpdateStatus.Skipped("disabled"), status["unit-list"])
        assertTrue(fetcher.requests.isEmpty())
        assertEquals(0, manager.totalNetworkRules())
    }

    @Test
    fun refreshBeforeIntervalIsSkipped() {
        val cache = newCache()
        val fetcher = ScriptedFetcher(FetchResult.Success(bodyV1, "\"v1\"", null))
        val manager = FilterListManager(
            listOf(source), fetcher, cache,
            UpdatePolicy(refreshIntervalMs = 60 * 60 * 1000),
        )
        manager.startup(nowMillis = 0)

        val status = manager.refresh(nowMillis = 1_000)

        assertEquals(ListUpdateStatus.Skipped("not due"), status["unit-list"])
        assertEquals(1, fetcher.requests.size) // only the startup download
    }

    @Test
    fun defaultSourcesAreSane() {
        val defaults = FilterListSource.DEFAULTS
        assertTrue(defaults.size >= 2)
        assertEquals(defaults.size, defaults.map { it.id }.toSet().size)
        defaults.forEach { source ->
            assertTrue(source.downloadUrl.startsWith("https://"))
            assertTrue(source.enabled)
        }
    }
}
