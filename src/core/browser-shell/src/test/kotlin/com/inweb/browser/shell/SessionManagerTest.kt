package com.inweb.browser.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {

    private fun controllerWithSession(): TabsController {
        val controller = TabsController()
        val first = controller.openTab()
        controller.updateTab(
            first.navigate("https://a.example").navigate("https://b.example"),
        )
        val second = controller.openTab(isPrivate = true)
        controller.updateTab(second.navigate("https://secret.example"))
        controller.selectTab(first.id)
        return controller
    }

    @Test
    fun persistAndRestoreRoundTrip() {
        val persistence = InMemorySessionPersistence()
        val manager = SessionManager(persistence)
        val original = controllerWithSession()

        manager.persist(original)

        val restored = TabsController()
        assertTrue(manager.restore(restored))

        assertEquals(original.tabIds, restored.tabIds)
        assertEquals("https://b.example", restored.selectedTab?.currentUrl)
        assertTrue(restored.tab(restored.tabIds[1])!!.isPrivate)
    }

    @Test
    fun restoreReturnsFalseWhenNothingPersisted() {
        val manager = SessionManager(InMemorySessionPersistence())
        val controller = TabsController().also { it.openTab() }
        assertFalse(manager.restore(controller))
        // controller untouched by a failed restore
        assertEquals(1, controller.tabCount)
    }

    @Test
    fun corruptedSnapshotFallsBackToFreshSession() {
        val persistence = InMemorySessionPersistence()
        persistence.save("}corrupted garbage{")
        val manager = SessionManager(persistence)
        val controller = TabsController()

        assertFalse(manager.restore(controller))
        // corrupted data is dropped so the next launch starts clean
        assertNull(persistence.load())
        assertEquals(0, controller.tabCount)
    }

    @Test
    fun restoreReplacesExistingTabs() {
        val persistence = InMemorySessionPersistence()
        val manager = SessionManager(persistence)
        val original = controllerWithSession()
        manager.persist(original)

        val controller = TabsController()
        controller.openTab() // pre-existing "home" tab from app start
        controller.openTab()
        assertTrue(manager.restore(controller))

        assertEquals(original.tabIds, controller.tabIds)
        assertEquals(2, controller.tabCount)
    }

    @Test
    fun persistOverwritesPreviousSnapshot() {
        val persistence = InMemorySessionPersistence()
        val manager = SessionManager(persistence)

        val first = TabsController().also { it.openTab() }
        manager.persist(first)

        val second = controllerWithSession()
        manager.persist(second)

        val restored = TabsController()
        assertTrue(manager.restore(restored))
        assertEquals(second.tabIds, restored.tabIds)
    }
}
