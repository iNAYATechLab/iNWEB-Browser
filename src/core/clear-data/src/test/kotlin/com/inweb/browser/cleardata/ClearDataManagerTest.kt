package com.inweb.browser.cleardata

import com.inweb.browser.offline.InMemoryOfflineStore
import com.inweb.browser.offline.OfflineLibrary
import com.inweb.browser.offline.OfflinePageRecord
import com.inweb.browser.privacy.lists.FileFilterListCache
import com.inweb.browser.privacy.lists.FilterListMetadata
import com.inweb.browser.privacy.lists.FilterListSource
import com.inweb.browser.shell.InMemoryHistoryStore
import com.inweb.browser.shell.InMemorySessionPersistence
import com.inweb.browser.shell.InMemoryZoomPreferencesStore
import com.inweb.browser.shell.SessionStore
import com.inweb.browser.shell.TabState
import com.inweb.browser.shell.ZoomSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class ClearDataManagerTest {

    private fun fullManager(): Pair<ClearDataManager, FixtureStores> {
        val fixture = FixtureStores()
        return Pair(ClearDataManager(fixture.allBindings()), fixture)
    }

    private fun okMap(result: ClearDataResult<Map<ClearDataItem, ClearDataPreview>>) =
        (result as ClearDataResult.Ok).value

    private fun okCounts(result: ClearDataResult<Map<ClearDataItem, Int?>>) =
        (result as ClearDataResult.Ok).value

    // --- the item universe mirrors the inventory's clear column ---------------

    @Test
    fun itemUniverseIsExactlyTheInventoryClearColumn() {
        assertEquals(
            listOf("history", "session", "filter_list_cache", "offline_pages", "site_zoom_overrides"),
            ClearDataItem.entries.map { it.id },
        )
    }

    @Test
    fun availableItemsAreExactlyTheBoundOnesInCanonicalOrder() {
        val fixture = FixtureStores()
        val manager = ClearDataManager(
            listOf(
                SiteZoomOverridesClearBinding(fixture.zoom),
                HistoryClearBinding(fixture.history),
            )
        )
        assertEquals(
            listOf(ClearDataItem.HISTORY, ClearDataItem.SITE_ZOOM_OVERRIDES),
            manager.availableItems(),
        )
    }

    @Test
    fun duplicateBindingsFailFastAtConstruction() {
        val fixture = FixtureStores()
        val thrown = try {
            ClearDataManager(
                listOf(
                    HistoryClearBinding(fixture.history),
                    HistoryClearBinding(fixture.history),
                )
            )
            null
        } catch (e: IllegalArgumentException) {
            e
        }
        assertTrue(thrown != null)
        assertTrue(thrown!!.message!!.contains("duplicate"))
    }

    // --- preview is a dry run (real counts, no mutation) ------------------------

    @Test
    fun previewReportsRealCountsAndMutatesNothing() {
        val (manager, fixture) = fullManager()
        fixture.seed()

        val previews = okMap(manager.preview(ClearDataItem.entries.toSet()))

        assertEquals(3, previews[ClearDataItem.HISTORY]?.count)
        assertEquals(2, previews[ClearDataItem.SESSION]?.count)
        assertNull(previews[ClearDataItem.FILTER_LIST_CACHE]?.count) // disposable cache
        assertEquals(2, previews[ClearDataItem.OFFLINE_PAGES]?.count)
        assertEquals(2, previews[ClearDataItem.SITE_ZOOM_OVERRIDES]?.count)

        // dry run: nothing was cleared
        assertEquals(3, fixture.history.allVisits().size)
        assertEquals(2, fixture.zoom.current().siteZooms.size)
        assertEquals(2, fixture.offline.all().size)
    }

    // --- clear executes through the real store APIs ------------------------------

    @Test
    fun clearHistoryActuallyClearsTheStore() {
        val (manager, fixture) = fullManager()
        fixture.seed()
        val counts = okCounts(manager.clear(setOf(ClearDataItem.HISTORY)))
        assertEquals(3, counts[ClearDataItem.HISTORY])
        assertTrue(fixture.history.allVisits().isEmpty())
        // untouched
        assertEquals(2, fixture.zoom.current().siteZooms.size)
    }

    @Test
    fun clearSessionClearsThePersistedSnapshot() {
        val (manager, fixture) = fullManager()
        fixture.seed()
        val counts = okCounts(manager.clear(setOf(ClearDataItem.SESSION)))
        assertEquals(2, counts[ClearDataItem.SESSION])
        assertNull(fixture.session.load())
    }

    @Test
    fun clearFilterListCacheRemovesBodiesAndMetadata() {
        val (manager, fixture) = fullManager()
        fixture.seed()
        val counts = okCounts(manager.clear(setOf(ClearDataItem.FILTER_LIST_CACHE)))
        assertNull(counts[ClearDataItem.FILTER_LIST_CACHE]) // not countable by design
        assertNull(fixture.cache.loadBody(fixture.listOne))
        assertNull(fixture.cache.loadMetadata(fixture.listOne))
        assertNull(fixture.cache.loadBody(fixture.listTwo))
    }

    @Test
    fun clearOfflinePagesEmptiesTheLibraryAndPersists() {
        val (manager, fixture) = fullManager()
        fixture.seed()
        val counts = okCounts(manager.clear(setOf(ClearDataItem.OFFLINE_PAGES)))
        assertEquals(2, counts[ClearDataItem.OFFLINE_PAGES])
        assertTrue(fixture.offline.all().isEmpty())
        assertTrue(fixture.offlineStore.load().isEmpty())
    }

    @Test
    fun clearSiteZoomOverridesKeepsTheDefaultFactor() {
        val (manager, fixture) = fullManager()
        fixture.seed()
        val counts = okCounts(manager.clear(setOf(ClearDataItem.SITE_ZOOM_OVERRIDES)))
        assertEquals(2, counts[ClearDataItem.SITE_ZOOM_OVERRIDES])
        assertTrue(fixture.zoom.current().siteZooms.isEmpty())
        assertEquals(1.5, fixture.zoom.current().defaultFactor, 0.0)
        assertEquals(1.5, fixture.zoom.zoomFor("a.example"), 0.0)
        assertTrue(fixture.zoomStore.load()?.siteZooms.isNullOrEmpty())
    }

    @Test
    fun clearOnlyTheChosenItemsLeavesEverythingElseUntouched() {
        val (manager, fixture) = fullManager()
        fixture.seed()
        okCounts(
            manager.clear(setOf(ClearDataItem.HISTORY, ClearDataItem.SITE_ZOOM_OVERRIDES))
        )
        assertTrue(fixture.history.allVisits().isEmpty())
        assertTrue(fixture.zoom.current().siteZooms.isEmpty())
        // session, cache, offline untouched
        assertEquals(2, fixture.sessionTabCount())
        assertEquals("rule-a", fixture.cache.loadBody(fixture.listOne))
        assertEquals(2, fixture.offline.all().size)
    }

    @Test
    fun clearingEverythingReportsPerItemCounts() {
        val (manager, fixture) = fullManager()
        fixture.seed()
        val counts = okCounts(manager.clear(ClearDataItem.entries.toSet()))
        assertEquals(3, counts[ClearDataItem.HISTORY])
        assertEquals(2, counts[ClearDataItem.SESSION])
        assertNull(counts[ClearDataItem.FILTER_LIST_CACHE])
        assertEquals(2, counts[ClearDataItem.OFFLINE_PAGES])
        assertEquals(2, counts[ClearDataItem.SITE_ZOOM_OVERRIDES])
        // a second clear of everything reports zeros
        val second = okCounts(manager.clear(ClearDataItem.entries.toSet()))
        assertEquals(0, second[ClearDataItem.HISTORY])
        assertEquals(0, second[ClearDataItem.OFFLINE_PAGES])
        assertEquals(0, second[ClearDataItem.SITE_ZOOM_OVERRIDES])
    }

    // --- validation: nothing silently skipped -------------------------------------

    @Test
    fun clearWithAnUnboundItemIsRejectedAndExecutesNothing() {
        val fixture = FixtureStores()
        val manager = ClearDataManager(listOf(HistoryClearBinding(fixture.history)))
        fixture.seed()
        val result = manager.clear(setOf(ClearDataItem.HISTORY, ClearDataItem.OFFLINE_PAGES))
        val error = (result as ClearDataResult.Err).error as ClearDataError.UnboundItems
        assertEquals(listOf(ClearDataItem.OFFLINE_PAGES), error.items)
        // nothing executed
        assertEquals(3, fixture.history.allVisits().size)
        assertEquals(2, fixture.offline.all().size)
    }

    @Test
    fun previewWithAnUnboundItemIsRejectedToo() {
        val fixture = FixtureStores()
        val manager = ClearDataManager(listOf(HistoryClearBinding(fixture.history)))
        val result = manager.preview(setOf(ClearDataItem.SESSION))
        assertTrue((result as ClearDataResult.Err).error is ClearDataError.UnboundItems)
    }

    @Test
    fun emptySelectionIsRejected() {
        val (manager, _) = fullManager()
        val result = manager.clear(emptySet())
        assertTrue((result as ClearDataResult.Err).error is ClearDataError.EmptySelection)
    }

    // --- real fixtures ------------------------------------------------------------

    /**
     * Real store implementations (the same classes the app binds):
     * in-memory stores, the file-backed filter-list cache, and the
     * real OfflineLibrary/ZoomSettings holders.
     */
    private class FixtureStores {
        val history = InMemoryHistoryStore()
        val session = InMemorySessionPersistence()
        val zoomStore = InMemoryZoomPreferencesStore()
        val zoom = ZoomSettings(zoomStore)
        val offlineStore = InMemoryOfflineStore()
        val offline = OfflineLibrary(offlineStore, quotaBytes = 1_000_000L)
        val cacheDir = Files.createTempDirectory("inweb-clear-cache").toFile()
        val cache = FileFilterListCache(cacheDir)

        val listOne = FilterListSource("easylist", "EasyList", "https://example.org/easylist.txt")
        val listTwo = FilterListSource("easyprivacy", "EasyPrivacy", "https://example.org/easyprivacy.txt")

        fun allBindings(): List<ClearDataBinding> = listOf(
            HistoryClearBinding(history),
            SessionClearBinding(session),
            FilterListCacheClearBinding(cache),
            OfflinePagesClearBinding(offline),
            SiteZoomOverridesClearBinding(zoom),
        )

        fun sessionTabCount(): Int =
            session.load()?.let { SessionStore.deserialize(it).tabs.size } ?: 0

        fun seed() {
            history.recordVisit("https://a.example", "A", 1_000L, isPrivate = false)
            history.recordVisit("https://b.example", "B", 2_000L, isPrivate = false)
            history.recordVisit("https://c.example", "C", 3_000L, isPrivate = false)

            val tabs = listOf(
                TabState(id = "t-1", history = listOf("https://a.example"), historyIndex = 0),
                TabState(id = "t-2", history = listOf("https://b.example"), historyIndex = 0),
            )
            session.save(SessionStore.serialize(tabs, "t-1"))

            cache.store(listOne, "rule-a", FilterListMetadata("easylist", 1_000L))
            cache.store(listTwo, "rule-b", FilterListMetadata("easyprivacy", 1_000L))

            offline.save(
                OfflinePageRecord(
                    onlineUrl = "https://a.example",
                    title = "A",
                    snapshotFileName = "a.mhtml",
                    sizeBytes = 100L,
                    createdAtMillis = 1_000L,
                    lastAccessedAtMillis = 1_000L,
                )
            )
            offline.save(
                OfflinePageRecord(
                    onlineUrl = "https://b.example",
                    title = "B",
                    snapshotFileName = "b.mhtml",
                    sizeBytes = 200L,
                    createdAtMillis = 2_000L,
                    lastAccessedAtMillis = 2_000L,
                )
            )

            zoom.setDefaultFactor(1.5)
            zoom.setSiteZoom("a.example", 2.0)
            zoom.setSiteZoom("b.example", 0.5)
        }
    }
}
