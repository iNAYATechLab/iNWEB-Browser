package com.inweb.browser.shell

/**
 * Owns the set of open tabs and the selected tab.
 *
 * Pure state machine: the Chromium engine adapter (ui/ patch area — see
 * docs/PHASE2-INTEGRATION-PLAN.md) observes these states and performs the
 * actual page loading. Nothing here touches platform APIs.
 */
class TabsController {
    private val tabs = LinkedHashMap<String, TabState>()
    private var selectedIdInternal: String? = null
    private var nextCounter = 0

    /** Ordered ids of all open tabs. */
    val tabIds: List<String> get() = tabs.keys.toList()

    val tabCount: Int get() = tabs.size

    /** Currently selected tab, or null when no tabs are open. */
    val selectedTab: TabState? get() = selectedIdInternal?.let { tabs[it] }

    /** Opens a new tab (optionally private) and selects it. */
    fun openTab(isPrivate: Boolean = false): TabState {
        val id = "tab-${nextCounter++}"
        val tab = TabState(id = id, isPrivate = isPrivate)
        tabs[id] = tab
        selectedIdInternal = id
        return tab
    }

    fun tab(id: String): TabState? = tabs[id]

    fun selectTab(id: String): TabState {
        val tab = tabs[id] ?: throw IllegalArgumentException("unknown tab: $id")
        selectedIdInternal = id
        return tab
    }

    /** Replaces the stored state of an existing tab (TabState is immutable). */
    fun updateTab(tab: TabState) {
        require(tabs.containsKey(tab.id)) { "unknown tab: ${tab.id}" }
        tabs[tab.id] = tab
    }

    /** Closes a tab; closing the selected tab selects the most recently opened remaining tab. */
    fun closeTab(id: String) {
        if (!tabs.containsKey(id)) throw IllegalArgumentException("unknown tab: $id")
        val wasSelected = id == selectedIdInternal
        tabs.remove(id)
        if (wasSelected) selectedIdInternal = tabIds.lastOrNull()
    }

    /**
     * Restores a session snapshot (crash-safe recovery, MASTER-SPEC §51).
     * Ids must be unique; the selected id must reference one of the tabs.
     * New tab ids generated afterwards never collide with restored ids.
     */
    fun restore(tabsToRestore: List<TabState>, selectedId: String?) {
        require(tabsToRestore.map { it.id }.toSet().size == tabsToRestore.size) {
            "duplicate tab ids in snapshot"
        }
        if (selectedId != null) {
            require(tabsToRestore.any { it.id == selectedId }) {
                "selected id $selectedId not present in snapshot"
            }
        }
        tabs.clear()
        tabsToRestore.forEach { tabs[it.id] = it }
        selectedIdInternal = selectedId ?: tabsToRestore.lastOrNull()?.id
        nextCounter = maxOf(tabsToRestore.size, maxNumericSuffix(tabsToRestore) + 1)
    }

    private fun maxNumericSuffix(states: List<TabState>): Int =
        states.maxOfOrNull { state ->
            tabIdRegex.matchEntire(state.id)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        } ?: 0

    private companion object {
        val tabIdRegex = Regex("^tab-(\\d+)$")
    }
}
