package com.inweb.browser.home

import com.inweb.browser.offline.InMemoryOfflineStore
import com.inweb.browser.offline.LibraryResult
import com.inweb.browser.offline.OfflineLibrary
import com.inweb.browser.offline.OfflinePageRecord
import com.inweb.browser.privacy.CookiePolicy
import com.inweb.browser.privacy.SecurityCenterModel
import com.inweb.browser.shell.DownloadRecord
import com.inweb.browser.shell.DownloadState
import com.inweb.browser.shell.InMemoryBookmarkStore
import com.inweb.browser.shell.InMemoryDownloadsStore
import com.inweb.browser.shell.InMemoryHistoryStore
import com.inweb.browser.shell.TopSites
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAdaptersTest {

    @Test
    fun absentSecurityCenterIsUnavailable() {
        assertEquals(HomePrivacyState.Unavailable, HomePrivacyAdapter.from(null))
    }

    @Test
    fun realEnforcementExposesRealBlockedCount() {
        assertEquals(
            HomePrivacyState.Active(blockedCount = 7),
            HomePrivacyAdapter.from(securityModel(enabled = true, enforcing = true, blocked = 7)),
        )
    }

    @Test
    fun enabledButNotEnforcingIsPartialWithoutCount() {
        assertEquals(
            HomePrivacyState.Partial(
                trackingProtectionEnabled = true,
                enforcementActive = false,
            ),
            HomePrivacyAdapter.from(securityModel(enabled = true, enforcing = false, blocked = 99)),
        )
    }

    @Test
    fun disabledProtectionIsPartial() {
        assertEquals(
            HomePrivacyState.Partial(
                trackingProtectionEnabled = false,
                enforcementActive = true,
            ),
            HomePrivacyAdapter.from(securityModel(enabled = false, enforcing = true, blocked = 3)),
        )
    }

    @Test
    fun snapshotUsesExistingTopSitesAndRecentContracts() {
        val history = InMemoryHistoryStore().apply {
            recordVisit("https://a.example", "A old", 1)
            recordVisit("https://b.example", "B", 2)
            recordVisit("https://a.example", "A new", 3)
        }
        val snapshot = HomeBrowserSnapshotAdapter.from(
            history,
            InMemoryBookmarkStore(),
            InMemoryDownloadsStore(),
            OfflineLibrary(),
            HomeBrowserLimits(topSites = 2, recentPages = 2),
        )

        assertEquals(TopSites.compute(history.allVisits(), 2), snapshot.suggestedTopSites)
        assertEquals(history.recent(2), snapshot.recentPages)
        assertEquals("A new", snapshot.suggestedTopSites.first().title)
    }

    @Test
    fun snapshotLimitsAndOrdersRealStoreData() {
        val bookmarks = InMemoryBookmarkStore().apply {
            add("https://one.example", "One", 1)
            add("https://two.example", "Two", 2)
        }
        val downloads = InMemoryDownloadsStore().apply {
            add(download("one", "one.bin"))
            add(download("two", "two.bin"))
        }
        val offline = OfflineLibrary(InMemoryOfflineStore()).apply {
            assertTrue(save(offlinePage("one", lastAccessed = 10)) is LibraryResult.Ok)
            assertTrue(save(offlinePage("two", lastAccessed = 20)) is LibraryResult.Ok)
        }

        val snapshot = HomeBrowserSnapshotAdapter.from(
            InMemoryHistoryStore(),
            bookmarks,
            downloads,
            offline,
            HomeBrowserLimits(bookmarks = 1, downloads = 1, offlinePages = 1),
        )

        assertEquals(listOf("Two"), snapshot.bookmarks.map { it.title })
        assertEquals(listOf("two"), snapshot.recentDownloads.map { it.id })
        assertEquals(listOf("https://two.example"), snapshot.offlinePages.map { it.onlineUrl })
    }

    @Test
    fun emptyStoresProduceEmptySnapshot() {
        val snapshot = HomeBrowserSnapshotAdapter.from(
            InMemoryHistoryStore(),
            InMemoryBookmarkStore(),
            InMemoryDownloadsStore(),
            OfflineLibrary(),
        )
        assertEquals(HomeBrowserSnapshot.EMPTY, snapshot)
    }

    @Test
    fun everySnapshotLimitMustBePositive() {
        assertThrows(IllegalArgumentException::class.java) {
            HomeBrowserLimits(topSites = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            HomeBrowserLimits(offlinePages = -1)
        }
    }

    private fun securityModel(
        enabled: Boolean,
        enforcing: Boolean,
        blocked: Int,
    ) = SecurityCenterModel(
        enforcementActive = enforcing,
        trackingProtectionEnabled = enabled,
        cookiePolicy = CookiePolicy.BLOCK_THIRD_PARTY,
        allowlistedSiteCount = 0,
        filterLists = emptyList(),
        totalNetworkRules = 0,
        blockedCount = blocked,
        allowedCount = 0,
        passedCount = 0,
        topBlockedDomains = emptyList(),
    )

    private fun download(id: String, fileName: String) = DownloadRecord(
        id = id,
        url = "https://example.org/$fileName",
        fileName = fileName,
        mimeType = "application/octet-stream",
        state = DownloadState.QUEUED,
    )

    private fun offlinePage(id: String, lastAccessed: Long) = OfflinePageRecord(
        onlineUrl = "https://$id.example",
        title = id,
        snapshotFileName = "$id.mhtml",
        sizeBytes = 10,
        createdAtMillis = lastAccessed,
        lastAccessedAtMillis = lastAccessed,
    )
}
