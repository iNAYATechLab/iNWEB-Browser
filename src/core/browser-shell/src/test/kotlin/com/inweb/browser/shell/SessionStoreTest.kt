package com.inweb.browser.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStoreTest {

    private fun controllerWithSession(): TabsController {
        val controller = TabsController()
        val first = controller.openTab()
        controller.updateTab(
            first.navigate("https://a.example").navigate("https://b.example").goBack(),
        )
        val second = controller.openTab(isPrivate = true)
        controller.updateTab(second.navigate("https://secret.example"))
        controller.selectTab(first.id)
        return controller
    }

    @Test
    fun roundTripPreservesSession() {
        val controller = controllerWithSession()
        val text = SessionStore.serialize(
            controller.tabIds.mapNotNull(controller::tab),
            controller.selectedTab?.id,
        )
        val snapshot = SessionStore.deserialize(text)

        assertEquals(controller.tabIds, snapshot.tabs.map { it.id })
        assertEquals(controller.selectedTab?.id, snapshot.selectedId)

        val first = snapshot.tabs[0]
        // navigated twice then went back: history keeps both entries, index at the first
        assertEquals(listOf("https://a.example", "https://b.example"), first.history)
        assertEquals(0, first.historyIndex)
        assertTrue(snapshot.tabs[1].isPrivate)
        assertEquals("https://secret.example", snapshot.tabs[1].currentUrl)
    }

    @Test
    fun emptySessionRoundTrip() {
        val text = SessionStore.serialize(emptyList(), null)
        val snapshot = SessionStore.deserialize(text)
        assertTrue(snapshot.tabs.isEmpty())
        assertNull(snapshot.selectedId)
    }

    @Test
    fun restoredSessionFeedsControllerRestore() {
        val controller = controllerWithSession()
        val snapshot = SessionStore.deserialize(
            SessionStore.serialize(controller.tabIds.mapNotNull(controller::tab), controller.selectedTab?.id),
        )
        val restored = TabsController()
        restored.restore(snapshot.tabs, snapshot.selectedId)
        assertEquals(controller.tabIds, restored.tabIds)
        assertEquals("https://a.example", restored.selectedTab?.currentUrl)
    }

    @Test(expected = SessionFormatException::class)
    fun badHeaderRejected() {
        SessionStore.deserialize("something else\ntab\tt\tfalse\t-1")
    }

    @Test(expected = SessionFormatException::class)
    fun unsupportedVersionRejected() {
        SessionStore.deserialize("iNWEB-SESSION v=2\ntab\tt\tfalse\t-1")
    }

    @Test(expected = SessionFormatException::class)
    fun unknownLineTypeRejected() {
        SessionStore.deserialize("iNWEB-SESSION v=1\nnonsense\tline")
    }

    @Test(expected = SessionFormatException::class)
    fun outOfBoundsHistoryIndexRejected() {
        SessionStore.deserialize("iNWEB-SESSION v=1\ntab\tt\tfalse\t5\thttps://a.example")
    }

    @Test(expected = SessionFormatException::class)
    fun danglingSelectedIdRejected() {
        SessionStore.deserialize("iNWEB-SESSION v=1\nselected\tghost")
    }
}
