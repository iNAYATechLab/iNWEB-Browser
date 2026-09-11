package com.inweb.browser.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TabStateTest {

    @Test
    fun freshTabHasNoHistory() {
        val tab = TabState(id = "tab-0")
        assertNull(tab.currentUrl)
        assertFalse(tab.canGoBack)
        assertFalse(tab.canGoForward)
        assertEquals(-1, tab.historyIndex)
    }

    @Test
    fun navigateAppendsEntryAndStartsLoading() {
        val tab = TabState(id = "tab-0").navigate("https://inweb.example")
        assertEquals(listOf("https://inweb.example"), tab.history)
        assertEquals(0, tab.historyIndex)
        assertEquals("https://inweb.example", tab.currentUrl)
        assertEquals(TabLifecycle.LOADING, tab.lifecycle)
        assertFalse(tab.canGoBack)
        assertFalse(tab.canGoForward)
    }

    @Test
    fun navigateTruncatesForwardHistory() {
        val tab = TabState(id = "t")
            .navigate("https://a.example")
            .navigate("https://b.example")
            .navigate("https://c.example")
            .goBack() // now at b, forward entry c exists
        val moved = tab.navigate("https://d.example")
        assertEquals(
            listOf("https://a.example", "https://b.example", "https://d.example"),
            moved.history,
        )
        assertEquals(2, moved.historyIndex)
        assertFalse(moved.canGoForward)
        assertTrue(moved.canGoBack)
    }

    @Test
    fun goBackAndForwardMoveIndexWithoutRewritingHistory() {
        val tab = TabState(id = "t")
            .navigate("https://a.example")
            .navigate("https://b.example")
            .goBack()
        assertEquals("https://a.example", tab.currentUrl)
        assertTrue(tab.canGoForward)
        val forwarded = tab.goForward()
        assertEquals("https://b.example", forwarded.currentUrl)
        assertEquals(listOf("https://a.example", "https://b.example"), forwarded.history)
    }

    @Test
    fun pageLifecycleCallbacks() {
        val loading = TabState(id = "t").navigate("https://a.example")
        assertEquals(TabLifecycle.LOADING, loading.lifecycle)
        val finished = loading.onPageFinished(PageSecurityState.SECURE)
        assertEquals(TabLifecycle.IDLE, finished.lifecycle)
        assertEquals(PageSecurityState.SECURE, finished.security)
        assertEquals(TabLifecycle.LOADING, finished.onPageStarted().lifecycle)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankNavigationRejected() {
        TabState(id = "t").navigate("   ")
    }

    @Test(expected = IllegalStateException::class)
    fun goBackWithoutHistoryRejected() {
        TabState(id = "t").navigate("https://a.example").goBack()
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidHistoryIndexRejected() {
        TabState(id = "t", history = listOf("https://a.example"), historyIndex = 5)
    }
}
