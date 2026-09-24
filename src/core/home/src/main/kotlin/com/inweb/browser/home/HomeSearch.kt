package com.inweb.browser.home

import com.inweb.browser.shell.OmniboxInput
import com.inweb.browser.shell.OmniboxParser
import com.inweb.browser.shell.SearchEngine

/** The two search providers designed for Home. */
enum class HomeSearchMode {
    WEB,
    QURAN,
}

/**
 * Voice-search capability and operation state.
 *
 * Android permission prompts and speech recognition stay in the renderer; the
 * core carries only truthful state and never simulates a listening result.
 */
enum class VoiceSearchState {
    AVAILABLE,
    PERMISSION_REQUIRED,
    LISTENING,
    PROCESSING,
    PERMISSION_DENIED,
    UNSUPPORTED,
    ERROR,
}

/** Result of submitting the Home search field. */
sealed class HomeSearchResult {
    /** Blank input is a no-op; the renderer keeps focus instead of navigating. */
    object Empty : HomeSearchResult()

    /** The authoritative browser-shell parser classified this web-mode input. */
    data class Web(val input: OmniboxInput) : HomeSearchResult()

    /** A real Qur'an provider should receive this trimmed query. */
    data class Quran(val query: String) : HomeSearchResult()

    /** The selected provider is not genuinely available in this build/state. */
    data class ProviderUnavailable(val mode: HomeSearchMode) : HomeSearchResult()
}

/**
 * Routes Home submissions without owning a competing omnibox implementation.
 * Web-mode classification always delegates to [OmniboxParser].
 */
object HomeSearchRouter {

    fun submit(
        rawInput: String,
        mode: HomeSearchMode,
        searchEngine: SearchEngine,
        quranProviderAvailable: Boolean,
    ): HomeSearchResult {
        val input = rawInput.trim()
        if (input.isEmpty()) return HomeSearchResult.Empty

        return when (mode) {
            HomeSearchMode.WEB -> HomeSearchResult.Web(
                OmniboxParser.parse(input, searchEngine),
            )
            HomeSearchMode.QURAN -> {
                if (quranProviderAvailable) HomeSearchResult.Quran(input)
                else HomeSearchResult.ProviderUnavailable(HomeSearchMode.QURAN)
            }
        }
    }
}
