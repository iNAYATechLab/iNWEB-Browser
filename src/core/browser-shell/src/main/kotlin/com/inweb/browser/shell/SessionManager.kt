package com.inweb.browser.shell

/**
 * Persistence port for the serialized session (MASTER-SPEC §51).
 *
 * Implementations MUST write atomically (e.g. temp file + rename) so a crash
 * mid-write can never corrupt the previous snapshot.
 */
interface SessionPersistence {
    fun save(text: String)
    fun load(): String?
    fun clear()
}

/** In-memory implementation for tests and previews. */
class InMemorySessionPersistence : SessionPersistence {
    private var text: String? = null
    override fun save(text: String) {
        this.text = text
    }

    override fun load(): String? = text

    override fun clear() {
        text = null
    }
}

/**
 * Crash-safe session persistence manager (MASTER-SPEC §51): snapshots the
 * tab set into the persistence port and restores it later.
 *
 * Corrupted snapshots fall back to a fresh session instead of crashing
 * (§50: robust handling of corrupted data). Corruption is never silent —
 * the return value tells the caller the restore did not happen.
 */
class SessionManager(private val persistence: SessionPersistence) {

    /** Persists the current session of the controller. */
    fun persist(controller: TabsController) {
        persistence.save(
            SessionStore.serialize(
                controller.tabIds.mapNotNull(controller::tab),
                controller.selectedTab?.id,
            ),
        )
    }

    /**
     * Restores a previously persisted session into the controller.
     * Returns true if a snapshot existed and was restored; false when
     * nothing was persisted or the snapshot was corrupted (in which case
     * the corrupted data is dropped).
     */
    fun restore(controller: TabsController): Boolean {
        val text = persistence.load() ?: return false
        val snapshot = try {
            SessionStore.deserialize(text)
        } catch (e: SessionFormatException) {
            // Corrupted data: drop it, keep the app usable, report via false.
            persistence.clear()
            return false
        }
        controller.restore(snapshot.tabs, snapshot.selectedId)
        return true
    }
}
