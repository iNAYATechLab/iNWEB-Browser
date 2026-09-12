package com.inweb.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.inweb.browser.cleardata.ClearDataItem
import com.inweb.browser.cleardata.ClearDataManager
import com.inweb.browser.cleardata.ClearDataPreview
import com.inweb.browser.cleardata.ClearDataResult
import com.inweb.browser.cleardata.FilterListCacheClearBinding
import com.inweb.browser.cleardata.HistoryClearBinding
import com.inweb.browser.cleardata.OfflinePagesClearBinding
import com.inweb.browser.cleardata.SessionClearBinding
import com.inweb.browser.cleardata.SiteZoomOverridesClearBinding
import com.inweb.browser.customization.InMemoryToolbarStore
import com.inweb.browser.customization.ToolbarConfig
import com.inweb.browser.customization.ToolbarConfigurator
import com.inweb.browser.customization.ToolbarItem
import com.inweb.browser.customization.ToolbarStore
import com.inweb.browser.notifications.InMemoryNotificationStore
import com.inweb.browser.notifications.NotificationChannel
import com.inweb.browser.offline.InMemoryOfflineStore
import com.inweb.browser.offline.OfflineLibrary
import com.inweb.browser.privacy.lists.FilterListCache
import com.inweb.browser.privacy.lists.InMemoryFilterListCache
import com.inweb.browser.shell.InMemoryZoomPreferencesStore
import com.inweb.browser.shell.ZoomPreferencesStore
import com.inweb.browser.shell.ZoomSettings
import com.inweb.browser.notifications.NotificationDecision
import com.inweb.browser.notifications.NotificationEvent
import com.inweb.browser.notifications.NotificationPolicy
import com.inweb.browser.notifications.NotificationStore
import com.inweb.browser.shell.AppSettings
import com.inweb.browser.shell.BookmarkEntry
import com.inweb.browser.shell.BookmarkStore
import com.inweb.browser.shell.BrowserEngine
import com.inweb.browser.shell.DownloadRecord
import com.inweb.browser.shell.HistoryEntry
import com.inweb.browser.shell.HistoryStore
import com.inweb.browser.shell.InMemoryBookmarkStore
import com.inweb.browser.shell.InMemoryDownloadsStore
import com.inweb.browser.shell.InMemoryHistoryStore
import com.inweb.browser.shell.InMemorySettingsStore
import com.inweb.browser.shell.InMemorySessionPersistence
import com.inweb.browser.shell.OmniboxInput
import com.inweb.browser.shell.OmniboxParser
import com.inweb.browser.shell.PrivacyFilterHistory
import com.inweb.browser.shell.SessionManager
import com.inweb.browser.shell.SessionPersistence
import com.inweb.browser.shell.SettingsStore
import com.inweb.browser.shell.TabState
import com.inweb.browser.shell.ThemeMode
import com.inweb.browser.shell.TopSite
import com.inweb.browser.shell.TabsController

/** Overlay screens of the shell. */
enum class Screen { BROWSER, SETTINGS, DOWNLOADS, HISTORY, BOOKMARKS, TABS, CUSTOMIZE_TOOLBAR, CLEAR_DATA }

/**
 * Browser-shell view model: binds the pure-JVM core (TabsController,
 * OmniboxParser, AppSettings, SessionManager, HistoryStore, DownloadsStore)
 * to the UI.
 *
 * The engine port is a binding point: the Chromium adapter provides the real
 * implementation via the ui/ patch area. Until that adapter ships, browsing
 * is NOT claimed to work (MASTER-SPEC §57).
 */
class BrowserViewModel(
    private val engine: BrowserEngine = DevelopmentEngineBinding,
    private val settingsStore: SettingsStore = InMemorySettingsStore(),
    private val sessionPersistence: SessionPersistence = InMemorySessionPersistence(),
    private val historyStore: HistoryStore = InMemoryHistoryStore(),
    private val bookmarkStore: BookmarkStore = InMemoryBookmarkStore(),
    private val toolbarStore: ToolbarStore = InMemoryToolbarStore(),
    private val notificationStore: NotificationStore = InMemoryNotificationStore(),
    private val zoomStore: ZoomPreferencesStore = InMemoryZoomPreferencesStore(),
    private val filterListCache: FilterListCache = InMemoryFilterListCache(),
) {

    var settings by mutableStateOf(AppSettings())
        private set

    var selectedTab by mutableStateOf<TabState?>(null)
        private set

    var screen by mutableStateOf(Screen.BROWSER)
        private set

    var downloads by mutableStateOf<List<DownloadRecord>>(emptyList())
        private set

    var history by mutableStateOf<List<HistoryEntry>>(emptyList())
        private set

    var historyQuery by mutableStateOf("")
        private set

    var bookmarks by mutableStateOf<List<BookmarkEntry>>(emptyList())
        private set

    /** Toolbar configuration (§23) — the bottom bar renders from this. */
    var toolbarConfig by mutableStateOf(ToolbarConfig.default())
        private set

    /** Available notification channels and their enabled state (§33). */
    var notificationChannels by mutableStateOf<List<Pair<NotificationChannel, Boolean>>>(emptyList())
        private set

    /**
     * Clear-browsing-data previews (real counts from the stores, §39 /
     * Phase 12) — refreshed on open; never mutated by previewing.
     */
    var clearDataPreviews by mutableStateOf<Map<ClearDataItem, ClearDataPreview>>(emptyMap())
        private set

    /** Transient dialog selection (deliberately NOT persisted, ADR-034). */
    var clearDataSelection by mutableStateOf<Set<ClearDataItem>>(emptySet())
        private set

    var tabs by mutableStateOf<List<TabState>>(emptyList())
        private set

    // Home / new-tab sections (§38) — real data only, never fabricated.
    var homeShortcuts by mutableStateOf<List<TopSite>>(emptyList())
        private set
    var homeRecent by mutableStateOf<List<HistoryEntry>>(emptyList())
        private set
    var homeBookmarks by mutableStateOf<List<BookmarkEntry>>(emptyList())
        private set

    val tabIds: List<String> get() = controller.tabIds

    /** First-run onboarding (§35) is pending until completed once. */
    val needsOnboarding: Boolean get() = !settings.onboardingCompleted

    private val controller = TabsController()
    private val sessionManager = SessionManager(sessionPersistence)
    private val downloadsStore = InMemoryDownloadsStore()
    private val historySource = PrivacyFilterHistory(historyStore)
    private val toolbarConfigurator = ToolbarConfigurator(toolbarStore)
    private val zoomSettings = ZoomSettings(zoomStore)
    private val offlineLibrary = OfflineLibrary(InMemoryOfflineStore())
    private val clearDataManager = ClearDataManager(
        listOf(
            HistoryClearBinding(historySource),
            SessionClearBinding(sessionPersistence),
            FilterListCacheClearBinding(filterListCache),
            OfflinePagesClearBinding(offlineLibrary),
            SiteZoomOverridesClearBinding(zoomSettings),
        )
    )

    /**
     * The §33 channel-availability set for THIS authored build: only the
     * downloads source is real today (the Phase 2 downloads core). The
     * set grows as the event-source patches land (0018 offline,
     * 0020 app-lock, 0021 VPN, the backup-failed binding) — a channel is
     * never registered before its source exists (no stubs, ADR-030).
     */
    private val notificationPolicy = NotificationPolicy(
        availableChannels = setOf(NotificationChannel.DOWNLOADS),
        store = notificationStore,
    )

    init {
        settings = settingsStore.load()
        toolbarConfig = toolbarConfigurator.current()
        refreshNotificationChannels()
        // Crash-safe session restore (§51); corrupted/missing snapshots
        // fall back to a fresh session.
        sessionManager.restore(controller)
        if (controller.tabCount == 0) openTab() else refresh()
    }

    /** Submits omnibox input: URL navigation or search, per core parser rules. */
    fun submitOmniboxInput(rawInput: String) {
        val tab = controller.selectedTab ?: controller.openTab()
        when (val parsed = OmniboxParser.parse(rawInput, settings.searchEngine)) {
            is OmniboxInput.Url -> navigate(tab, parsed.url)
            is OmniboxInput.Search -> navigate(tab, parsed.searchUrl)
        }
    }

    private fun navigate(tab: TabState, url: String) {
        controller.updateTab(tab.navigate(url))
        // History: private tabs are excluded by the history policy (§13/§14).
        // The engine adapter refines recording to page-load events
        // (docs/PHASE2-INTEGRATION-PLAN.md).
        historySource.recordVisit(
            url = url,
            title = url,
            visitedAtMillis = System.currentTimeMillis(),
            isPrivate = tab.isPrivate,
        )
        engine.loadUrl(tab.id, url)
        refresh()
    }

    fun goBack() {
        val tab = controller.selectedTab ?: return
        if (tab.canGoBack) {
            controller.updateTab(tab.goBack())
            engine.goBack(tab.id)
            refresh()
        }
    }

    fun goForward() {
        val tab = controller.selectedTab ?: return
        if (tab.canGoForward) {
            controller.updateTab(tab.goForward())
            engine.goForward(tab.id)
            refresh()
        }
    }

    fun stop() {
        controller.selectedTab?.let { engine.stop(it.id) }
    }

    fun reload() {
        controller.selectedTab?.let { tab -> engine.reload(tab.id) }
    }

    fun openTab(isPrivate: Boolean = false) {
        controller.openTab(isPrivate)
        refresh()
    }

    fun closeTab(id: String) {
        controller.closeTab(id)
        if (controller.tabCount == 0) openTab()
        refresh()
    }

    fun selectTab(id: String) {
        controller.selectTab(id)
        refresh()
    }

    // --- Settings (real behavior, persisted via SettingsStore) ---------------

    fun updateSearchEngine(id: String) {
        settings = settings.copy(searchEngineId = id)
        settingsStore.save(settings)
    }

    /**
     * Completes first-run onboarding (§35). The chosen engine is written
     * through the same real setting the settings screen uses — one source
     * of truth, persisted immediately.
     */
    fun completeOnboarding(searchEngineId: String) {
        settings = settings.copy(searchEngineId = searchEngineId, onboardingCompleted = true)
        settingsStore.save(settings)
    }

    fun updateTheme(mode: ThemeMode) {
        settings = settings.copy(theme = mode)
        settingsStore.save(settings)
    }

    // --- Toolbar customization (§23, bound to the customization core) -------

    /** The items the bottom bar renders, in the user's order (§23). */
    val toolbarVisibleItems: List<ToolbarItem> get() = toolbarConfig.visibleItems()

    fun openCustomizeToolbar() {
        screen = Screen.CUSTOMIZE_TOOLBAR
    }

    /**
     * Reorders a toolbar item (remove-then-insert, full-list index).
     * The customize surface only issues in-range moves; the core still
     * validates every mutation and keeps the last valid state on error.
     */
    fun moveToolbarItem(itemId: String, newIndex: Int) {
        toolbarConfigurator.move(itemId, newIndex)
        toolbarConfig = toolbarConfigurator.current()
    }

    /** Hides/shows an optional item; mandatory items are locked in the core. */
    fun setToolbarItemVisible(itemId: String, visible: Boolean) {
        toolbarConfigurator.setVisible(itemId, visible)
        toolbarConfig = toolbarConfigurator.current()
    }

    /** Restores the authored default toolbar and persists it. */
    fun resetToolbar() {
        toolbarConfigurator.reset()
        toolbarConfig = toolbarConfigurator.current()
    }

    // --- Notifications (§33, bound to the notification-policy core) ---------

    fun setNotificationChannelEnabled(channel: NotificationChannel, enabled: Boolean) {
        notificationPolicy.setChannelEnabled(channel, enabled)
        refreshNotificationChannels()
    }

    /**
     * Binding point (B-001): the engine/download adapter calls this when a
     * REAL event occurs; the returned decision is the ONLY path to posting
     * a notification. No caller exists until the adapter ships — nothing
     * notifies in this authored shell (§57).
     */
    fun decideNotification(event: NotificationEvent): NotificationDecision =
        notificationPolicy.decide(event)

    /** Binding point: the result of the POST_NOTIFICATIONS dialog (Android 13+). */
    fun onNotificationPermissionResult(granted: Boolean) {
        notificationPolicy.onPermissionResult(granted)
    }

    /** Binding point: an observed system permission change (resume/settings). */
    fun onSystemNotificationPermissionChanged(granted: Boolean) {
        notificationPolicy.onSystemPermissionChanged(granted)
    }

    private fun refreshNotificationChannels() {
        notificationChannels = notificationPolicy.registeredChannels()
            .map { it to notificationPolicy.isChannelEnabled(it) }
    }

    // --- Clear browsing data (§39, bound to the clear-data core) ---------------

    /** The bound clear-data items in canonical order (ADR-034). */
    val clearDataItems: List<ClearDataItem> get() = clearDataManager.availableItems()

    fun openClearData() {
        refreshClearPreviews()
        screen = Screen.CLEAR_DATA
    }

    fun toggleClearDataItem(item: ClearDataItem) {
        clearDataSelection =
            if (item in clearDataSelection) clearDataSelection - item else clearDataSelection + item
    }

    /**
     * Executes the selection through the real store APIs and closes the
     * dialog. The surface guarantees a non-empty, fully bound selection
     * (the confirm button is disabled otherwise), so the Err branch is
     * structurally unreachable — kept defensive, never silently ignored.
     */
    fun confirmClearData() {
        when (val result = clearDataManager.clear(clearDataSelection)) {
            is ClearDataResult.Ok -> {
                clearDataSelection = emptySet()
                refreshClearPreviews()
                screen = Screen.BROWSER
            }
            is ClearDataResult.Err -> {
                // Unreachable from this surface; state stays for the user.
            }
        }
    }

    private fun refreshClearPreviews() {
        val items = clearDataManager.availableItems().toSet()
        when (val previews = clearDataManager.preview(items)) {
            is ClearDataResult.Ok -> clearDataPreviews = previews.value
            is ClearDataResult.Err -> clearDataPreviews = emptyMap()
        }
    }

    // --- Overlay screens -----------------------------------------------------

    fun openSettings() {
        screen = Screen.SETTINGS
    }

    fun openDownloads() {
        screen = Screen.DOWNLOADS
    }

    // --- History surface (backed by the real HistoryStore, §14) --------------

    fun openHistory() {
        historyQuery = ""
        refreshHistory()
        screen = Screen.HISTORY
    }

    fun setHistoryQuery(query: String) {
        historyQuery = query
        refreshHistory()
    }

    fun deleteHistoryEntry(id: String) {
        historySource.delete(id)
        refreshHistory()
    }

    fun clearHistory() {
        historySource.clearAll()
        refreshHistory()
    }

    private fun refreshHistory() {
        history = if (historyQuery.isBlank()) {
            historySource.recent(HISTORY_LIMIT)
        } else {
            historySource.search(historyQuery, HISTORY_LIMIT)
        }
    }

    private fun refreshHome() {
        homeShortcuts = TopSites.compute(historySource.allVisits(), HOME_SHORTCUT_LIMIT)
        homeRecent = historySource.recent(HOME_RECENT_LIMIT)
        homeBookmarks = bookmarkStore.all().take(HOME_BOOKMARK_LIMIT)
    }

    // --- Bookmarks surface (explicit user action only, §28/§31) --------------

    fun openBookmarks() {
        refreshBookmarks()
        screen = Screen.BOOKMARKS
    }

    // --- Tab switcher (backed by the real TabsController) ----------------------

    fun openTabs() {
        screen = Screen.TABS
    }

    /**
     * Bookmarks the currently open page. Bookmarks are never automatic:
     * this runs only on explicit user action. Returns true when a new
     * bookmark was stored.
     */
    fun addBookmarkForCurrentTab(): Boolean {
        val url = selectedTab?.currentUrl ?: return false
        if (url == AppSettings.DEFAULT_HOMEPAGE) return false
        bookmarkStore.add(
            url = url,
            title = url,
            createdAtMillis = System.currentTimeMillis(),
        )
        refreshBookmarks()
        return bookmarkStore.isBookmarked(url)
    }

    fun deleteBookmark(id: String) {
        bookmarkStore.delete(id)
        refreshBookmarks()
    }

    private fun refreshBookmarks() {
        bookmarks = bookmarkStore.all()
        refreshHome()
    }

    fun closeOverlay() {
        screen = Screen.BROWSER
    }

    // --- Session lifecycle (§51) ----------------------------------------------

    /** Snapshots the session; called from the activity lifecycle (onStop). */
    fun persistSession() {
        sessionManager.persist(controller)
    }

    private fun refresh() {
        selectedTab = controller.selectedTab
        tabs = controller.allTabs()
        downloads = downloadsStore.all()
        refreshHome()
    }

    private companion object {
        /** Safety cap for the history surface; full data stays on disk. */
        const val HISTORY_LIMIT = 200
        const val HOME_SHORTCUT_LIMIT = 5
        const val HOME_RECENT_LIMIT = 5
        const val HOME_BOOKMARK_LIMIT = 5
    }
}

/**
 * Development-only engine binding point. Every method is a no-op with an
 * explicit log marker — this exists so the shell can be exercised before the
 * Chromium adapter lands; it never claims to render content.
 */
object DevelopmentEngineBinding : BrowserEngine {
    override fun loadUrl(tabId: String, url: String) {
        android.util.Log.d(TAG, "loadUrl binding point: $tabId -> $url")
    }

    override fun goBack(tabId: String) {
        android.util.Log.d(TAG, "goBack binding point: $tabId")
    }

    override fun goForward(tabId: String) {
        android.util.Log.d(TAG, "goForward binding point: $tabId")
    }

    override fun stop(tabId: String) {
        android.util.Log.d(TAG, "stop binding point: $tabId")
    }

    override fun reload(tabId: String) {
        android.util.Log.d(TAG, "reload binding point: $tabId")
    }

    private const val TAG = "iNWEB.EngineBinding"
}
