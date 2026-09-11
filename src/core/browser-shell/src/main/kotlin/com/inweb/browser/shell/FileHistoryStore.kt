package com.inweb.browser.shell

import java.io.File

/**
 * File-backed [HistoryStore] — real, persistent browsing history.
 *
 * Storage: one TSV file ("iNWEB-HISTORY v=1" header + one
 * `visit <id> <timestamp> <url> <title>` line per entry). Every mutation is
 * written through atomically (temp file + rename — same crash-safety
 * strategy as session persistence, ADR-011), so a crash mid-write can
 * never corrupt the previously stored history.
 *
 * Load behavior:
 *  - wrong or missing header → fresh empty store (session-corruption
 *    precedent: never crash, never half-trust a broken file);
 *  - malformed lines under a valid header are skipped (data recovery) and
 *    counted in [lastLoadSkippedLines] — reported, not hidden (§57);
 *  - tabs / newlines in URLs and titles are sanitized to spaces on write
 *    so the line format stays unambiguous.
 */
class FileHistoryStore(private val file: File) : HistoryStore {

    private val entries = mutableListOf<HistoryEntry>()
    private var counter = 0

    /** Malformed lines skipped by the last load (diagnostic aid). */
    var lastLoadSkippedLines: Int = 0
        private set

    init {
        load()
    }

    override fun recordVisit(
        url: String,
        title: String,
        visitedAtMillis: Long,
        isPrivate: Boolean,
    ): Boolean {
        if (isPrivate) return false
        entries += HistoryEntry(
            id = "h-${counter++}",
            url = sanitize(url),
            title = sanitize(title),
            visitedAtMillis = visitedAtMillis,
        )
        persist()
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

    override fun allVisits(): List<HistoryEntry> = entries.toList()

    override fun delete(id: String): Boolean {
        val removed = entries.removeAll { it.id == id }
        if (removed) persist()
        return removed
    }

    override fun deleteRange(fromMillis: Long, toMillis: Long): Int {
        require(fromMillis <= toMillis) { "fromMillis must be <= toMillis" }
        val doomed = entries.filter { it.visitedAtMillis in fromMillis..toMillis }
        if (doomed.isEmpty()) return 0
        entries.removeAll(doomed.toSet())
        persist()
        return doomed.size
    }

    override fun clearAll(): Int {
        val count = entries.size
        entries.clear()
        persist()
        return count
    }

    /** Total stored entries including repeated URLs (test/diagnostic aid). */
    fun entryCount(): Int = entries.size

    // --- persistence ----------------------------------------------------------

    private fun persist() {
        val text = buildString {
            append(HEADER).append('\n')
            for (entry in entries) {
                append("visit\t").append(entry.id).append('\t')
                    .append(entry.visitedAtMillis).append('\t')
                    .append(entry.url).append('\t')
                    .append(entry.title).append('\n')
            }
        }
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            // Some filesystems refuse rename-over-existing: replace explicitly.
            file.delete()
            check(tmp.renameTo(file)) { "atomic history write failed" }
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
            if (parts.size != 5 || parts[0] != "visit") {
                lastLoadSkippedLines++
                continue
            }
            val id = parts[1]
            val timestamp = parts[2].toLongOrNull()
            val url = parts[3]
            val title = parts[4]
            if (id.isBlank() || timestamp == null || url.isBlank()) {
                lastLoadSkippedLines++
                continue
            }
            entries += HistoryEntry(
                id = id,
                url = url,
                title = title,
                visitedAtMillis = timestamp,
            )
        }
        // Ids must stay unique across reloads.
        counter = (entries.maxOfOrNull { idSequence(it.id) } ?: -1) + 1
    }

    private fun idSequence(id: String): Int = id.removePrefix("h-").toIntOrNull() ?: -1

    private fun sanitize(value: String): String =
        value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')

    private companion object {
        const val HEADER = "iNWEB-HISTORY v=1"
    }
}
