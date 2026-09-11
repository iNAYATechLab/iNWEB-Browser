package com.inweb.browser.shell

/** Security state of the current page, reported by the engine adapter. */
enum class PageSecurityState { UNKNOWN, SECURE, INSECURE, BROKEN }

/** Page-load lifecycle of a tab. */
enum class TabLifecycle { IDLE, LOADING }

/**
 * Immutable state of a single browser tab, including its navigation history.
 *
 * Navigation-stack semantics mirror real browser engines:
 *  - [navigate] appends a new entry and truncates any forward entries;
 *  - [goBack] / [goForward] move the index without rewriting history.
 */
data class TabState(
    val id: String,
    val history: List<String> = emptyList(),
    val historyIndex: Int = -1,
    val lifecycle: TabLifecycle = TabLifecycle.IDLE,
    val security: PageSecurityState = PageSecurityState.UNKNOWN,
    val isPrivate: Boolean = false,
) {
    init {
        require(historyIndex >= -1) { "historyIndex must be >= -1 (was $historyIndex)" }
        require(historyIndex < history.size) {
            "historyIndex $historyIndex out of bounds for ${history.size} entries"
        }
    }

    /** URL of the current entry, or null for a fresh tab. */
    val currentUrl: String? get() = history.getOrNull(historyIndex)

    val canGoBack: Boolean get() = historyIndex > 0

    val canGoForward: Boolean get() = historyIndex < history.size - 1

    /** Navigate to a new URL: truncates the forward stack and starts loading. */
    fun navigate(url: String): TabState {
        require(url.isNotBlank()) { "url must not be blank" }
        val retained = if (historyIndex >= 0) history.subList(0, historyIndex + 1) else emptyList()
        val newHistory = retained + url
        return copy(
            history = newHistory,
            historyIndex = newHistory.size - 1,
            lifecycle = TabLifecycle.LOADING,
        )
    }

    fun goBack(): TabState {
        check(canGoBack) { "no back entry" }
        return copy(historyIndex = historyIndex - 1)
    }

    fun goForward(): TabState {
        check(canGoForward) { "no forward entry" }
        return copy(historyIndex = historyIndex + 1)
    }

    fun onPageStarted(): TabState = copy(lifecycle = TabLifecycle.LOADING)

    fun onPageFinished(pageSecurity: PageSecurityState): TabState =
        copy(lifecycle = TabLifecycle.IDLE, security = pageSecurity)
}
