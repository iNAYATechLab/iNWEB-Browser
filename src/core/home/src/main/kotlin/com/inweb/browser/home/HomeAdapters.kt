package com.inweb.browser.home

import com.inweb.browser.offline.OfflineLibrary
import com.inweb.browser.offline.OfflinePageRecord
import com.inweb.browser.privacy.SecurityCenterModel
import com.inweb.browser.shell.BookmarkEntry
import com.inweb.browser.shell.BookmarkStore
import com.inweb.browser.shell.DownloadRecord
import com.inweb.browser.shell.DownloadsStore
import com.inweb.browser.shell.HistoryEntry
import com.inweb.browser.shell.HistoryStore
import com.inweb.browser.shell.TopSite
import com.inweb.browser.shell.TopSites

/** Honest, renderer-ready privacy summary for the Home header. */
sealed class HomePrivacyState {
    object Loading : HomePrivacyState()
    object Unavailable : HomePrivacyState()

    /** Protection is enabled and real Chromium enforcement is active. */
    data class Active(val blockedCount: Int) : HomePrivacyState() {
        init {
            require(blockedCount >= 0) { "blockedCount must not be negative" }
        }
    }

    /** Settings/model exist, but protection is disabled or not yet enforcing. */
    data class Partial(
        val trackingProtectionEnabled: Boolean,
        val enforcementActive: Boolean,
    ) : HomePrivacyState()
}

object HomePrivacyAdapter {
    /** null means the real Security Center state is unavailable, not "protected". */
    fun from(model: SecurityCenterModel?): HomePrivacyState = when {
        model == null -> HomePrivacyState.Unavailable
        model.trackingProtectionEnabled && model.enforcementActive ->
            HomePrivacyState.Active(model.blockedCount)
        else -> HomePrivacyState.Partial(
            trackingProtectionEnabled = model.trackingProtectionEnabled,
            enforcementActive = model.enforcementActive,
        )
    }
}

data class HomeBrowserLimits(
    val topSites: Int = 8,
    val recentPages: Int = 8,
    val bookmarks: Int = 8,
    val downloads: Int = 4,
    val offlinePages: Int = 4,
) {
    init {
        require(topSites > 0) { "topSites limit must be positive" }
        require(recentPages > 0) { "recentPages limit must be positive" }
        require(bookmarks > 0) { "bookmarks limit must be positive" }
        require(downloads > 0) { "downloads limit must be positive" }
        require(offlinePages > 0) { "offlinePages limit must be positive" }
    }
}

/**
 * Real §38 browser data available to Home.
 *
 * [suggestedTopSites] remains explicitly separate from user-owned Quick Access.
 * The approved renderer may route bookmarks/downloads through destinations
 * rather than adding feed sections, but it never needs fabricated data.
 */
data class HomeBrowserSnapshot(
    val suggestedTopSites: List<TopSite>,
    val recentPages: List<HistoryEntry>,
    val bookmarks: List<BookmarkEntry>,
    val recentDownloads: List<DownloadRecord>,
    val offlinePages: List<OfflinePageRecord>,
) {
    companion object {
        val EMPTY = HomeBrowserSnapshot(
            suggestedTopSites = emptyList(),
            recentPages = emptyList(),
            bookmarks = emptyList(),
            recentDownloads = emptyList(),
            offlinePages = emptyList(),
        )
    }
}

object HomeBrowserSnapshotAdapter {
    fun from(
        history: HistoryStore,
        bookmarks: BookmarkStore,
        downloads: DownloadsStore,
        offline: OfflineLibrary,
        limits: HomeBrowserLimits = HomeBrowserLimits(),
    ): HomeBrowserSnapshot = HomeBrowserSnapshot(
        suggestedTopSites = TopSites.compute(history.allVisits(), limits.topSites),
        recentPages = history.recent(limits.recentPages),
        bookmarks = bookmarks.all().asReversed().take(limits.bookmarks),
        recentDownloads = downloads.all().asReversed().take(limits.downloads),
        offlinePages = offline.all()
            .sortedWith(
                compareByDescending<OfflinePageRecord> { it.lastAccessedAtMillis }
                    .thenByDescending { it.createdAtMillis }
                    .thenBy { it.onlineUrl },
            )
            .take(limits.offlinePages),
    )
}
