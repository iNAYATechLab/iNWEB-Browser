package com.inweb.browser.home

import com.inweb.browser.shell.OmniboxInput
import com.inweb.browser.shell.SearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSearchTest {

    @Test
    fun blankInputIsNoOpInEitherMode() {
        assertSame(
            HomeSearchResult.Empty,
            HomeSearchRouter.submit("   ", HomeSearchMode.WEB, SearchEngine.DUCK_DUCK_GO, false),
        )
        assertSame(
            HomeSearchResult.Empty,
            HomeSearchRouter.submit("\n", HomeSearchMode.QURAN, SearchEngine.GOOGLE, true),
        )
    }

    @Test
    fun webUrlDelegatesToAuthoritativeOmniboxParser() {
        val result = HomeSearchRouter.submit(
            " example.com/path ",
            HomeSearchMode.WEB,
            SearchEngine.DUCK_DUCK_GO,
            quranProviderAvailable = false,
        )

        assertEquals(
            HomeSearchResult.Web(OmniboxInput.Url("https://example.com/path")),
            result,
        )
    }

    @Test
    fun webQueryUsesSelectedSearchEngine() {
        val result = HomeSearchRouter.submit(
            "privacy browser",
            HomeSearchMode.WEB,
            SearchEngine.GOOGLE,
            quranProviderAvailable = false,
        )

        val web = result as HomeSearchResult.Web
        assertEquals(
            OmniboxInput.Search(
                query = "privacy browser",
                searchUrl = "https://www.google.com/search?q=privacy+browser",
            ),
            web.input,
        )
    }

    @Test
    fun webModeNeverDependsOnQuranAvailability() {
        val unavailable = HomeSearchRouter.submit(
            "example.org",
            HomeSearchMode.WEB,
            SearchEngine.BING,
            quranProviderAvailable = false,
        )
        val available = HomeSearchRouter.submit(
            "example.org",
            HomeSearchMode.WEB,
            SearchEngine.BING,
            quranProviderAvailable = true,
        )
        assertEquals(unavailable, available)
    }

    @Test
    fun quranModeReturnsTrimmedProviderIntentWhenAvailable() {
        assertEquals(
            HomeSearchResult.Quran("mercy"),
            HomeSearchRouter.submit(
                "  mercy  ",
                HomeSearchMode.QURAN,
                SearchEngine.DUCK_DUCK_GO,
                quranProviderAvailable = true,
            ),
        )
    }

    @Test
    fun quranModeIsTruthfullyUnavailableWithoutProvider() {
        assertEquals(
            HomeSearchResult.ProviderUnavailable(HomeSearchMode.QURAN),
            HomeSearchRouter.submit(
                "mercy",
                HomeSearchMode.QURAN,
                SearchEngine.DUCK_DUCK_GO,
                quranProviderAvailable = false,
            ),
        )
    }

    @Test
    fun voiceStateUniverseIncludesPermissionAndFailureStates() {
        assertTrue(VoiceSearchState.entries.contains(VoiceSearchState.PERMISSION_REQUIRED))
        assertTrue(VoiceSearchState.entries.contains(VoiceSearchState.PERMISSION_DENIED))
        assertTrue(VoiceSearchState.entries.contains(VoiceSearchState.UNSUPPORTED))
        assertTrue(VoiceSearchState.entries.contains(VoiceSearchState.ERROR))
        assertEquals(7, VoiceSearchState.entries.size)
    }
}
