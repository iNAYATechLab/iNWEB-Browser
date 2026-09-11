package com.inweb.browser.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TabsControllerTest {

    @Test
    fun openTabSelectsTheNewTab() {
        val controller = TabsController()
        val first = controller.openTab()
        assertEquals("tab-0", first.id)
        assertEquals(first.id, controller.selectedTab?.id)
        val second = controller.openTab(isPrivate = true)
        assertEquals(second.id, controller.selectedTab?.id)
        assertTrue(controller.tab(second.id)!!.isPrivate)
        assertEquals(2, controller.tabCount)
    }

    @Test
    fun selectTabSwitchesActiveTab() {
        val controller = TabsController()
        val a = controller.openTab()
        controller.openTab()
        controller.selectTab(a.id)
        assertEquals(a.id, controller.selectedTab?.id)
    }

    @Test
    fun closingSelectedTabSelectsMostRecentRemaining() {
        val controller = TabsController()
        val a = controller.openTab()
        val b = controller.openTab()
        controller.closeTab(b.id)
        assertEquals(a.id, controller.selectedTab?.id)
        assertEquals(listOf(a.id), controller.tabIds)
    }

    @Test
    fun closingNonSelectedTabKeepsSelection() {
        val controller = TabsController()
        val a = controller.openTab()
        val b = controller.openTab()
        controller.selectTab(a.id)
        controller.closeTab(b.id)
        assertEquals(a.id, controller.selectedTab?.id)
    }

    @Test
    fun restorePreservesStateAndGeneratesFreshIds() {
        val controller = TabsController()
        val snapshotTabs = listOf(
            TabState(
                id = "tab-7",
                history = listOf("https://a.example", "https://b.example"),
                historyIndex = 1,
            ),
            TabState(id = "custom", isPrivate = true),
        )
        controller.restore(snapshotTabs, selectedId = "tab-7")
        assertEquals(2, controller.tabCount)
        assertEquals("tab-7", controller.selectedTab?.id)
        assertEquals("https://b.example", controller.selectedTab?.currentUrl)
        assertTrue(controller.tab("custom")!!.isPrivate)
        // new ids never collide with restored numeric ids
        assertEquals("tab-8", controller.openTab().id)
    }

    @Test
    fun restoreWithNullSelectionSelectsLastTab() {
        val controller = TabsController()
        controller.restore(
            listOf(TabState(id = "tab-0"), TabState(id = "tab-1")),
            selectedId = null,
        )
        assertEquals("tab-1", controller.selectedTab?.id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun restoreRejectsUnknownSelectedId() {
        TabsController().restore(listOf(TabState(id = "tab-0")), selectedId = "nope")
    }

    @Test(expected = IllegalArgumentException::class)
    fun restoreRejectsDuplicateIds() {
        TabsController().restore(
            listOf(TabState(id = "tab-0"), TabState(id = "tab-0")),
            selectedId = "tab-0",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun closeUnknownTabRejected() {
        TabsController().closeTab("ghost")
    }

    @Test
    fun allTabsReturnsInsertionOrderIncludingPrivateTabs() {
        val controller = TabsController()
        controller.openTab()
        controller.openTab(isPrivate = true)
        controller.openTab()

        val all = controller.allTabs()
        assertEquals(3, all.size)
        assertEquals(controller.tabIds, all.map { it.id })
        assertTrue(all[1].isPrivate)
    }

    @Test
    fun allTabsReflectsNavigationUpdates() {
        val controller = TabsController()
        val tab = controller.openTab()
        controller.updateTab(tab.navigate("https://a.example.com/"))

        assertEquals("https://a.example.com/", controller.allTabs().single().currentUrl)
    }

    @Test
    fun allTabsExcludesClosedTabs() {
        val controller = TabsController()
        val first = controller.openTab()
        controller.openTab()
        controller.closeTab(first.id)

        assertEquals(listOf("tab-1"), controller.allTabs().map { it.id })
    }
}
