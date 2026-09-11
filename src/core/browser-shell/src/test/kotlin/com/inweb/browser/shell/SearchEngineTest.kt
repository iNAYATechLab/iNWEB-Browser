package com.inweb.browser.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchEngineTest {

    @Test
    fun buildsEscapedSearchUrl() {
        assertEquals(
            "https://duckduckgo.com/?q=how+to+code",
            SearchEngine.DUCK_DUCK_GO.buildSearchUrl("how to code"),
        )
    }

    @Test
    fun defaultsStartWithPrivacyEngine() {
        assertEquals("duckduckgo", SearchEngine.DEFAULTS.first().id)
        assertEquals(3, SearchEngine.DEFAULTS.size)
    }

    @Test
    fun byIdResolvesKnownEngines() {
        assertEquals("google", SearchEngine.byId("google")?.id)
        assertEquals("bing", SearchEngine.byId("bing")?.id)
        assertNull(SearchEngine.byId("unknown"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun templateWithoutPlaceholderRejected() {
        SearchEngine("bad", "Bad", "https://example.com/search")
    }
}
