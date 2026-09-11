package com.inweb.browser.shell

/** A single browsing-history visit (non-private only, by contract). */
data class HistoryEntry(
    val id: String,
    val url: String,
    val title: String,
    val visitedAtMillis: Long,
)

/**
 * Browsing-history port (MASTER-SPEC §14).
 *
 * Implementations provide storage; the privacy contract is enforced once, in
 * [PrivacyFilterHistory] — private-mode visits are NEVER stored, no matter
 * which backing store is used.
 */
interface HistoryStore {
    /**
     * Records a visit. Returns true if stored, false if skipped (private).
     */
    fun recordVisit(
        url: String,
        title: String,
        visitedAtMillis: Long,
        isPrivate: Boolean = false,
    ): Boolean

    /** Most recent visits first, deduplicated by URL. */
    fun recent(limit: Int): List<HistoryEntry>

    /** Case-insensitive substring search over URL and title, newest first. */
    fun search(query: String, limit: Int): List<HistoryEntry>

    /** Deletes one entry by id. Returns true if something was deleted. */
    fun delete(id: String): Boolean

    /** Deletes entries visited within [fromMillis, toMillis]. Returns the count deleted. */
    fun deleteRange(fromMillis: Long, toMillis: Long): Int

    /** Deletes everything. Returns the count deleted. */
    fun clearAll(): Int
}

/**
 * Privacy-enforcing decorator around any [HistoryStore] (MASTER-SPEC §13/§14:
 * private browsing never writes history). This is the single enforcement
 * point the engine adapter relies on.
 */
class PrivacyFilterHistory(private val delegate: HistoryStore) : HistoryStore by delegate {

    override fun recordVisit(
        url: String,
        title: String,
        visitedAtMillis: Long,
        isPrivate: Boolean,
    ): Boolean {
        if (isPrivate) return false
        return delegate.recordVisit(url, title, visitedAtMillis, isPrivate = false)
    }
}

/** In-memory implementation for tests and engine-less previews. */
class InMemoryHistoryStore : HistoryStore {
    private val entries = mutableListOf<HistoryEntry>()
    private var counter = 0

    override fun recordVisit(
        url: String,
        title: String,
        visitedAtMillis: Long,
        isPrivate: Boolean,
    ): Boolean {
        if (isPrivate) return false
        entries += HistoryEntry(
            id = "h-${counter++}",
            url = url,
            title = title,
            visitedAtMillis = visitedAtMillis,
        )
        return true
    }

    override fun recent(limit: Int): List<HistoryEntry> {
        require(limit > 0) { "limit must be > 0" }
        return entries
            .sortedByDescending { it.visitedAtMillis }
            .distinctBy { it.url }
            .take(limit)
    }

    override fun search(query: String, limit: Int): List<HistoryEntry> {
        require(limit > 0) { "limit must be > 0" }
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return recent(limit)
        return entries
            .filter { it.url.lowercase().contains(needle) || it.title.lowercase().contains(needle) }
            .sortedByDescending { it.visitedAtMillis }
            .distinctBy { it.url }
            .take(limit)
    }

    override fun delete(id: String): Boolean = entries.removeAll { it.id == id }

    override fun deleteRange(fromMillis: Long, toMillis: Long): Int {
        require(fromMillis <= toMillis) { "fromMillis must be <= toMillis" }
        val doomed = entries.filter { it.visitedAtMillis in fromMillis..toMillis }
        entries.removeAll(doomed.toSet())
        return doomed.size
    }

    override fun clearAll(): Int {
        val count = entries.size
        entries.clear()
        return count
    }

    /** Total stored entries (including repeated URLs). Test/diagnostic aid. */
    fun entryCount(): Int = entries.size
}
