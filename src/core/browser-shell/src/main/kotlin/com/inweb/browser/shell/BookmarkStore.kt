package com.inweb.browser.shell

/**
 * A single bookmark. Bookmarks are an explicit user action — nothing is
 * bookmarked automatically (unlike history).
 */
data class BookmarkEntry(
    val id: String,
    val url: String,
    val title: String,
    val createdAtMillis: Long,
    /** Optional folder name; null = unfiled. */
    val folder: String? = null,
)

/**
 * Bookmarks port (MASTER-SPEC §28 profile data, §31 application database).
 *
 * Semantics:
 *  - URLs are unique: adding an already-bookmarked URL returns the existing
 *    entry instead of storing a duplicate;
 *  - [all] returns insertion order (oldest first);
 *  - folders are plain names; `null` means unfiled.
 */
interface BookmarkStore {

    /** Adds a bookmark; a duplicate URL returns the existing entry unchanged. */
    fun add(
        url: String,
        title: String,
        createdAtMillis: Long,
        folder: String? = null,
    ): BookmarkEntry

    /** All bookmarks in insertion order (oldest first). */
    fun all(): List<BookmarkEntry>

    /** Distinct folder names in first-seen order (unfiled not included). */
    fun folders(): List<String>

    /** Bookmarks in one folder; `null` returns the unfiled ones. */
    fun byFolder(folder: String?): List<BookmarkEntry>

    fun isBookmarked(url: String): Boolean

    fun find(id: String): BookmarkEntry?

    /** Renames a bookmark in place (keeps position). Returns null if the id is unknown. */
    fun rename(id: String, title: String): BookmarkEntry?

    /** Moves a bookmark to a folder (`null` = unfiled). Returns null if the id is unknown. */
    fun move(id: String, folder: String?): BookmarkEntry?

    /** Deletes one bookmark by id. Returns true if something was deleted. */
    fun delete(id: String): Boolean

    /** Deletes everything. Returns the count deleted. */
    fun clearAll(): Int
}

/** In-memory implementation for tests and engine-less previews. */
class InMemoryBookmarkStore : BookmarkStore {

    private val entries = mutableListOf<BookmarkEntry>()
    private var counter = 0

    override fun add(
        url: String,
        title: String,
        createdAtMillis: Long,
        folder: String?,
    ): BookmarkEntry {
        require(url.isNotBlank()) { "bookmark url must not be blank" }
        entries.firstOrNull { it.url == url }?.let { return it }
        val entry = BookmarkEntry(
            id = "b-${counter++}",
            url = url,
            title = title,
            createdAtMillis = createdAtMillis,
            folder = folder?.takeIf { it.isNotBlank() },
        )
        entries += entry
        return entry
    }

    override fun all(): List<BookmarkEntry> = entries.toList()

    override fun folders(): List<String> = entries.mapNotNull { it.folder }.distinct()

    override fun byFolder(folder: String?): List<BookmarkEntry> = entries.filter { it.folder == folder }

    override fun isBookmarked(url: String): Boolean = entries.any { it.url == url }

    override fun find(id: String): BookmarkEntry? = entries.firstOrNull { it.id == id }

    override fun rename(id: String, title: String): BookmarkEntry? {
        val index = entries.indexOfFirst { it.id == id }
        if (index < 0) return null
        val updated = entries[index].copy(title = title)
        entries[index] = updated
        return updated
    }

    override fun move(id: String, folder: String?): BookmarkEntry? {
        val index = entries.indexOfFirst { it.id == id }
        if (index < 0) return null
        val target = folder?.takeIf { it.isNotBlank() }
        val updated = entries[index].copy(folder = target)
        entries[index] = updated
        return updated
    }

    override fun delete(id: String): Boolean = entries.removeAll { it.id == id }

    override fun clearAll(): Int {
        val count = entries.size
        entries.clear()
        return count
    }

    /** Total stored entries (test/diagnostic aid). */
    fun entryCount(): Int = entries.size
}
