package com.inweb.browser.shell

import java.io.File

/**
 * File-backed [BookmarkStore] — real, persistent bookmarks.
 *
 * Storage: one TSV file ("iNWEB-BOOKMARKS v=1" header + one
 * `bookmark <id> <createdAt> <folder> <url> <title>` line per entry; an
 * empty folder column means unfiled). Every mutation is written through
 * atomically (temp file + rename — the ADR-011/ADR-015 crash-safety
 * strategy).
 *
 * Load behavior mirrors [FileHistoryStore]: a wrong or missing header
 * yields a fresh empty store; malformed lines under a valid header are
 * skipped and counted in [lastLoadSkippedLines]; tabs/newlines in text
 * fields are sanitized to spaces on write so the line format stays
 * unambiguous; ids stay unique across reloads.
 */
class FileBookmarkStore(private val file: File) : BookmarkStore {

    private val entries = mutableListOf<BookmarkEntry>()
    private var counter = 0

    /** Malformed lines skipped by the last load (diagnostic aid). */
    var lastLoadSkippedLines: Int = 0
        private set

    init {
        load()
    }

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
            url = sanitize(url),
            title = sanitize(title),
            createdAtMillis = createdAtMillis,
            folder = sanitizeOptional(folder),
        )
        entries += entry
        persist()
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
        val updated = entries[index].copy(title = sanitize(title))
        entries[index] = updated
        persist()
        return updated
    }

    override fun move(id: String, folder: String?): BookmarkEntry? {
        val index = entries.indexOfFirst { it.id == id }
        if (index < 0) return null
        val updated = entries[index].copy(folder = sanitizeOptional(folder))
        entries[index] = updated
        persist()
        return updated
    }

    override fun delete(id: String): Boolean {
        val removed = entries.removeAll { it.id == id }
        if (removed) persist()
        return removed
    }

    override fun clearAll(): Int {
        val count = entries.size
        entries.clear()
        persist()
        return count
    }

    /** Total stored entries (test/diagnostic aid). */
    fun entryCount(): Int = entries.size

    // --- persistence ----------------------------------------------------------

    private fun persist() {
        val text = buildString {
            append(HEADER).append('\n')
            for (entry in entries) {
                append("bookmark\t").append(entry.id).append('\t')
                    .append(entry.createdAtMillis).append('\t')
                    .append(entry.folder ?: "").append('\t')
                    .append(entry.url).append('\t')
                    .append(entry.title).append('\n')
            }
        }
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            // Some filesystems refuse rename-over-existing: replace explicitly.
            file.delete()
            check(tmp.renameTo(file)) { "atomic bookmarks write failed" }
        }
    }

    private fun load() {
        lastLoadSkippedLines = 0
        entries.clear()
        counter = 0
        if (!file.isFile) return

        val lines = file.readText().lines()
        if (lines.firstOrNull() != HEADER) return // corrupt → fresh start

        for (line in lines.drop(1)) {
            if (line.isEmpty()) continue
            val parts = line.split('\t')
            if (parts.size != 6 || parts[0] != "bookmark") {
                lastLoadSkippedLines++
                continue
            }
            val id = parts[1]
            val createdAt = parts[2].toLongOrNull()
            val url = parts[4]
            if (id.isBlank() || createdAt == null || url.isBlank()) {
                lastLoadSkippedLines++
                continue
            }
            entries += BookmarkEntry(
                id = id,
                url = url,
                title = parts[5],
                createdAtMillis = createdAt,
                folder = parts[3].takeIf { it.isNotBlank() },
            )
        }
        // Ids must stay unique across reloads.
        counter = (entries.maxOfOrNull { idSequence(it.id) } ?: -1) + 1
    }

    private fun idSequence(id: String): Int = id.removePrefix("b-").toIntOrNull() ?: -1

    private fun sanitize(value: String): String =
        value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')

    private fun sanitizeOptional(value: String?): String? =
        value?.takeIf { it.isNotBlank() }?.let { sanitize(it) }

    private companion object {
        const val HEADER = "iNWEB-BOOKMARKS v=1"
    }
}
