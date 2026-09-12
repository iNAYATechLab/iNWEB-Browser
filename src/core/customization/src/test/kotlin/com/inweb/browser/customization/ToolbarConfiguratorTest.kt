package com.inweb.browser.customization

import com.inweb.browser.customization.ToolbarItem.BACK
import com.inweb.browser.customization.ToolbarItem.FORWARD
import com.inweb.browser.customization.ToolbarItem.HOME
import com.inweb.browser.customization.ToolbarItem.MENU
import com.inweb.browser.customization.ToolbarItem.TABS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolbarConfiguratorTest {

    private fun ids(items: List<ToolbarItem>) = items.map { it.id }

    private fun okConfig(result: ToolbarResult<ToolbarConfig>): ToolbarConfig =
        (result as ToolbarResult.Ok).value

    private fun seeded(vararg order: ToolbarItem, hidden: List<ToolbarItem> = emptyList()): ToolbarConfigurator {
        val store = InMemoryToolbarStore()
        store.save(
            ToolbarStoreData(
                order = ids(order.toList()),
                hidden = ids(hidden),
            )
        )
        return ToolbarConfigurator(store)
    }

    private fun storedOf(configurator: ToolbarConfigurator): ToolbarStoreData =
        configurator.current().toStoreData()

    // --- item universe (mirrors the authored bottom bar) -----------------------

    @Test
    fun itemUniverseMirrorsTheAuthoredBottomBarExactly() {
        assertEquals(listOf("back", "forward", "home", "tabs", "menu"), ids(ToolbarItem.ALL))
        assertEquals(ToolbarItem.ALL, ToolbarItem.DEFAULT_ORDER)
        assertEquals(listOf("back", "tabs", "menu"), ids(ToolbarItem.ALL.filter { it.mandatory }))
        assertEquals(listOf("forward", "home"), ids(ToolbarItem.ALL.filter { !it.mandatory }))
    }

    @Test
    fun fromIdRoundTripsEveryItemAndRejectsUnknown() {
        for (item in ToolbarItem.ALL) {
            assertEquals(item, ToolbarItem.fromId(item.id))
        }
        assertNull(ToolbarItem.fromId("share"))
        assertNull(ToolbarItem.fromId(""))
    }

    // --- seeding & load --------------------------------------------------------

    @Test
    fun emptyStoreIsSeededWithAuthoredDefaultAndPersisted() {
        val store = InMemoryToolbarStore()
        val configurator = ToolbarConfigurator(store)
        assertEquals(ids(ToolbarItem.DEFAULT_ORDER), ids(configurator.visibleItems()))
        // the seed is persisted, not just held in memory
        assertEquals(configurator.current().toStoreData(), store.load())
        assertNull(configurator.lastRecovery())
    }

    @Test
    fun validStoredConfigurationLoadsAsIs() {
        val configurator = seeded(MENU, BACK, TABS, HOME, FORWARD, hidden = listOf(FORWARD, HOME))
        assertEquals(listOf("menu", "back", "tabs"), ids(configurator.visibleItems()))
        assertEquals(
            ToolbarStoreData(
                order = listOf("menu", "back", "tabs", "home", "forward"),
                hidden = listOf("home", "forward"),
            ),
            configurator.current().toStoreData(),
        )
        assertNull(configurator.lastRecovery())
    }

    @Test
    fun allOptionalItemsHiddenIsAValidToolbar() {
        val configurator = seeded(BACK, FORWARD, HOME, TABS, MENU, hidden = listOf(FORWARD, HOME))
        assertEquals(listOf("back", "tabs", "menu"), ids(configurator.visibleItems()))
    }

    // --- corrupt stored data: fall back to default, persist, report ------------

    @Test
    fun duplicateStoredOrderFallsBackToDefaultAndPersistsRepair() {
        val store = InMemoryToolbarStore()
        store.save(
            ToolbarStoreData(
                order = listOf("back", "back", "forward", "home", "tabs", "menu"),
                hidden = emptyList(),
            )
        )
        val configurator = ToolbarConfigurator(store)
        assertEquals(ToolbarError.DuplicateItems(listOf("back")), configurator.lastRecovery())
        assertEquals(ids(ToolbarItem.DEFAULT_ORDER), ids(configurator.visibleItems()))
        assertEquals(ToolbarConfig.default().toStoreData(), store.load())
    }

    @Test
    fun unknownStoredItemFallsBackToDefaultAndPersistsRepair() {
        val store = InMemoryToolbarStore()
        store.save(
            ToolbarStoreData(
                order = listOf("back", "share", "forward", "home", "tabs", "menu"),
                hidden = emptyList(),
            )
        )
        val configurator = ToolbarConfigurator(store)
        assertEquals(ToolbarError.UnknownItems(listOf("share")), configurator.lastRecovery())
        assertEquals(ids(ToolbarItem.DEFAULT_ORDER), ids(configurator.visibleItems()))
        assertEquals(ToolbarConfig.default().toStoreData(), store.load())
    }

    @Test
    fun missingStoredItemFallsBackToDefaultAndPersistsRepair() {
        val store = InMemoryToolbarStore()
        store.save(ToolbarStoreData(order = listOf("back", "home", "tabs", "menu"), hidden = emptyList()))
        val configurator = ToolbarConfigurator(store)
        assertEquals(ToolbarError.MissingItems(listOf("forward")), configurator.lastRecovery())
        assertEquals(ids(ToolbarItem.DEFAULT_ORDER), ids(configurator.visibleItems()))
    }

    @Test
    fun hiddenIdOutsideTheOrderFallsBackToDefault() {
        val store = InMemoryToolbarStore()
        store.save(
            ToolbarStoreData(
                order = listOf("back", "forward", "home", "tabs", "menu"),
                hidden = listOf("forward", "back2"), // "back2" is not a slot
            )
        )
        val configurator = ToolbarConfigurator(store)
        assertEquals(ToolbarError.UnknownHidden(listOf("back2")), configurator.lastRecovery())
        assertEquals(ids(ToolbarItem.DEFAULT_ORDER), ids(configurator.visibleItems()))
    }

    @Test
    fun mandatoryItemHiddenOnLoadFallsBackToDefault() {
        val store = InMemoryToolbarStore()
        store.save(
            ToolbarStoreData(
                order = listOf("back", "forward", "home", "tabs", "menu"),
                hidden = listOf("menu"),
            )
        )
        val configurator = ToolbarConfigurator(store)
        assertEquals(ToolbarError.MandatoryHidden(listOf(ToolbarItem.MENU)), configurator.lastRecovery())
        assertEquals(ids(ToolbarItem.DEFAULT_ORDER), ids(configurator.visibleItems()))
    }

    @Test
    fun repeatedHiddenIdIsIdempotentNotCorrupt() {
        val store = InMemoryToolbarStore()
        store.save(
            ToolbarStoreData(
                order = listOf("back", "forward", "home", "tabs", "menu"),
                hidden = listOf("forward", "forward"),
            )
        )
        val configurator = ToolbarConfigurator(store)
        assertNull(configurator.lastRecovery())
        assertEquals(listOf("back", "home", "tabs", "menu"), ids(configurator.visibleItems()))
    }

    // --- parse: every offender reported, checks in fixed order -----------------

    @Test
    fun parseCollectsAllDuplicateIds() {
        val result = ToolbarConfig.parse(
            ToolbarStoreData(
                order = listOf("back", "back", "tabs", "tabs", "menu"),
                hidden = emptyList(),
            )
        )
        val error = (result as ToolbarResult.Err).error
        assertEquals(
            setOf("back", "tabs"),
            (error as ToolbarError.DuplicateItems).ids.toSet(),
        )
    }

    @Test
    fun parseReportsUnknownBeforeMissing() {
        // "share" is unknown AND three known items are missing: unknown wins
        val result = ToolbarConfig.parse(
            ToolbarStoreData(order = listOf("back", "share"), hidden = emptyList())
        )
        val error = (result as ToolbarResult.Err).error
        assertEquals(listOf("share"), (error as ToolbarError.UnknownItems).ids)
    }

    @Test
    fun parseCollectsAllMissingItems() {
        val result = ToolbarConfig.parse(
            ToolbarStoreData(order = listOf("back", "tabs", "menu"), hidden = emptyList())
        )
        val error = (result as ToolbarResult.Err).error
        assertEquals(
            setOf("forward", "home"),
            (error as ToolbarError.MissingItems).ids.toSet(),
        )
    }

    // --- move ------------------------------------------------------------------

    @Test
    fun moveRemovesThenInsertsAndPersists() {
        val configurator = ToolbarConfigurator()
        val moved = okConfig(configurator.move("menu", 0))
        assertEquals(listOf("menu", "back", "forward", "home", "tabs"), ids(moved.visibleItems()))
        assertEquals(ids(moved.visibleItems()), ids(configurator.visibleItems()))
        assertEquals(
            ToolbarStoreData(
                order = listOf("menu", "back", "forward", "home", "tabs"),
                hidden = emptyList(),
            ),
            storedOf(configurator),
        )
    }

    @Test
    fun moveToEndRemovesThenInsertsAtLastPosition() {
        val configurator = ToolbarConfigurator()
        okConfig(configurator.move("back", 4))
        assertEquals(listOf("forward", "home", "tabs", "menu", "back"), ids(configurator.visibleItems()))
    }

    @Test
    fun moveKeepsAHiddenItemsPositionAmongAllSlots() {
        val configurator = ToolbarConfigurator()
        okConfig(configurator.setVisible("forward", false))
        okConfig(configurator.move("back", 3))
        // full order becomes forward, home, tabs, back, menu; forward stays hidden
        assertEquals(listOf("home", "tabs", "back", "menu"), ids(configurator.visibleItems()))
        assertEquals(
            listOf("forward", "home", "tabs", "back", "menu"),
            ids(configurator.current().entries.map { it.item }),
        )
    }

    @Test
    fun moveUnknownItemIsRejectedAndStateUnchanged() {
        val configurator = ToolbarConfigurator()
        val before = storedOf(configurator)
        val result = configurator.move("share", 2)
        assertTrue(result is ToolbarResult.Err)
        assertEquals(ToolbarError.NotFound, (result as ToolbarResult.Err).error)
        assertEquals(before, storedOf(configurator))
    }

    @Test
    fun moveOutOfRangeIndexIsRejectedAndStateUnchanged() {
        val configurator = ToolbarConfigurator()
        val before = storedOf(configurator)
        for (bad in listOf(-1, 5)) {
            val result = configurator.move("back", bad)
            assertEquals(ToolbarError.IndexOutOfRange(bad, 5), (result as ToolbarResult.Err).error)
            assertEquals(before, storedOf(configurator))
        }
        okConfig(configurator.move("back", 4)) // 4 is the last valid index
    }

    // --- visibility ------------------------------------------------------------

    @Test
    fun hideOptionalItemRemovesItFromVisibleButKeepsItsSlot() {
        val configurator = ToolbarConfigurator()
        okConfig(configurator.setVisible("forward", false))
        assertEquals(listOf("back", "home", "tabs", "menu"), ids(configurator.visibleItems()))
        // hidden keeps its position in the full order
        assertEquals(listOf("back", "forward", "home", "tabs", "menu"), ids(configurator.current().entries.map { it.item }))
        assertEquals(listOf("forward"), storedOf(configurator).hidden)
    }

    @Test
    fun hideMandatoryItemIsRejectedAndStateUnchanged() {
        val configurator = ToolbarConfigurator()
        val before = storedOf(configurator)
        for (mandatory in listOf(ToolbarItem.BACK, ToolbarItem.TABS, ToolbarItem.MENU)) {
            val result = configurator.setVisible(mandatory.id, false)
            assertEquals(
                ToolbarError.MandatoryHidden(listOf(mandatory)),
                (result as ToolbarResult.Err).error,
            )
            assertEquals(before, storedOf(configurator))
        }
    }

    @Test
    fun showHiddenItemRestoresItAtItsStoredPosition() {
        val configurator = ToolbarConfigurator()
        okConfig(configurator.setVisible("home", false))
        okConfig(configurator.setVisible("home", true))
        assertEquals(ids(ToolbarItem.DEFAULT_ORDER), ids(configurator.visibleItems()))
        assertEquals(ToolbarConfig.default().toStoreData(), storedOf(configurator))
    }

    @Test
    fun setVisibleUnknownItemIsRejected() {
        val configurator = ToolbarConfigurator()
        val result = configurator.setVisible("share", false)
        assertEquals(ToolbarError.NotFound, (result as ToolbarResult.Err).error)
    }

    // --- reset -----------------------------------------------------------------

    @Test
    fun resetRestoresAuthoredDefaultAndPersists() {
        val configurator = ToolbarConfigurator()
        okConfig(configurator.setVisible("forward", false))
        okConfig(configurator.move("menu", 0))
        val reset = configurator.reset()
        assertEquals(ids(ToolbarItem.DEFAULT_ORDER), ids(reset.visibleItems()))
        assertEquals(ToolbarConfig.default().toStoreData(), storedOf(configurator))
    }

    // --- store seam ------------------------------------------------------------

    @Test
    fun inMemoryStoreRoundTripsTheWireForm() {
        val store = InMemoryToolbarStore()
        val data = ToolbarStoreData(
            order = listOf("tabs", "back", "menu", "forward", "home"),
            hidden = listOf("home"),
        )
        store.save(data)
        assertEquals(data, store.load())
        assertEquals(null, InMemoryToolbarStore().load())
    }
}
