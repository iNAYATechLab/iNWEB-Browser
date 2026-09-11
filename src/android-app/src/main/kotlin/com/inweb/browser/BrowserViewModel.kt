package com.inweb.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.inweb.browser.shell.TabsController

/** Overlay screens of the shell. */
enum class Screen { BROWSER, SETTINGS, DOWNLOADS, HISTORY, BOOKMARKS }

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

    val tabIds: List<String> get() = controller.tabIds

    private val controller = TabsController()
    private val sessionManager = SessionManager(sessionPersistence)
    private val downloadsStore = InMemoryDownloadsStore()
    private val history = PrivacyFilterHistory(historyStore)

    init {
        settings = settingsStore.load()
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
        history.recordVisit(
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

    fun updateTheme(mode: ThemeMode) {
        settings = settings.copy(theme = mode)
        settingsStore.save(settings)
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
        history.delete(id)
        refreshHistory()
    }

    fun clearHistory() {
        history.clearAll()
        refreshHistory()
    }

    private fun refreshHistory() {
        history = if (historyQuery.isBlank()) {
            historyStore.recent(HISTORY_LIMIT)
        } else {
            historyStore.search(historyQuery, HISTORY_LIMIT)
        }
    }

    // --- Bookmarks surface (explicit user action only, §28/§31) --------------

    fun openBookmarks() {
        refreshBookmarks()
        screen = Screen.BOOKMARKS
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
        downloads = downloadsStore.all()
    }

    private companion object {
        /** Safety cap for the history surface; full data stays on disk. */
        const val HISTORY_LIMIT = 200
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
