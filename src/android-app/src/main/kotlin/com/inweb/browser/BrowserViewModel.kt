package com.inweb.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.inweb.browser.shell.AppSettings
import com.inweb.browser.shell.BrowserEngine
import com.inweb.browser.shell.OmniboxInput
import com.inweb.browser.shell.OmniboxParser
import com.inweb.browser.shell.TabState
import com.inweb.browser.shell.TabsController

/**
 * Browser-shell view model: binds the pure-JVM core (TabsController,
 * OmniboxParser, AppSettings) to the UI.
 *
 * The engine port is a binding point: the Chromium adapter provides the real
 * implementation via the ui/ patch area. Until that adapter ships, browsing
 * is NOT claimed to work (MASTER-SPEC §57).
 */
class BrowserViewModel(
    private val engine: BrowserEngine = DevelopmentEngineBinding,
    private val initialSettings: AppSettings = AppSettings(),
) {

    var settings by mutableStateOf(initialSettings)
        private set

    var selectedTab by mutableStateOf<TabState?>(null)
        private set

    val tabIds: List<String> get() = controller.tabIds

    private val controller = TabsController()

    init {
        openTab()
    }

    /** Submits omnibox input: URL navigation or search, per core parser rules. */
    fun submitOmniboxInput(rawInput: String) {
        val tab = controller.selectedTab ?: controller.openTab()
        when (val parsed = OmniboxParser.parse(rawInput, settings.searchEngine)) {
            is OmniboxInput.Url -> {
                controller.updateTab(tab.navigate(parsed.url))
                engine.loadUrl(tab.id, parsed.url)
            }
            is OmniboxInput.Search -> {
                controller.updateTab(tab.navigate(parsed.searchUrl))
                engine.loadUrl(tab.id, parsed.searchUrl)
            }
        }
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
        controller.selectedTab?.let { tab ->
            tab.currentUrl?.let { url -> engine.reload(tab.id) }
        }
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

    fun updateTheme(mode: com.inweb.browser.shell.ThemeMode) {
        settings = settings.copy(theme = mode)
    }

    private fun refresh() {
        selectedTab = controller.selectedTab
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
