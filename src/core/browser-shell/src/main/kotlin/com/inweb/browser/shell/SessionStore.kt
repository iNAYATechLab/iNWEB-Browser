package com.inweb.browser.shell

/** Immutable snapshot of a browsing session for persistence and restore. */
data class SessionSnapshot(val tabs: List<TabState>, val selectedId: String?)

/**
 * Thrown when persisted session data is corrupted. Callers fall back to a
 * fresh session (MASTER-SPEC §50: never silently lose data — corruption is
 * reported, not hidden).
 */
class SessionFormatException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * Serializes/deserializes tab sessions to a compact, versioned, line-based
 * format (crash-safe session persistence, MASTER-SPEC §51).
 *
 * Format (tab-separated lines):
 *
 *     iNWEB-SESSION v=1
 *     tab\t<id>\t<isPrivate>\t<historyIndex>\t<entry1>\t<entry2>...
 *     selected\t<id>
 *
 * URLs cannot contain raw tab characters, so entries need no escaping.
 */
object SessionStore {
    private const val HEADER = "iNWEB-SESSION v=1"
    private const val TYPE_TAB = "tab"
    private const val TYPE_SELECTED = "selected"

    fun serialize(tabs: List<TabState>, selectedId: String?): String = buildString {
        appendLine(HEADER)
        for (tab in tabs) {
            append(TYPE_TAB)
            append('\t').append(tab.id)
            append('\t').append(tab.isPrivate)
            append('\t').append(tab.historyIndex)
            for (entry in tab.history) append('\t').append(entry)
            appendLine()
        }
        if (selectedId != null) appendLine("$TYPE_SELECTED\t$selectedId")
    }

    /** Parses a serialized session; throws [SessionFormatException] on any corruption. */
    fun deserialize(text: String): SessionSnapshot {
        val lines = text.lines()
        if (lines.isEmpty() || lines.first() != HEADER) {
            throw SessionFormatException("missing or unsupported session header")
        }
        val tabs = mutableListOf<TabState>()
        var selectedId: String? = null
        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            val parts = line.split('\t')
            when (parts.first()) {
                TYPE_TAB -> tabs += parseTab(parts)
                TYPE_SELECTED -> {
                    if (parts.size != 2) throw SessionFormatException("malformed selected line")
                    selectedId = parts[1]
                }
                else -> throw SessionFormatException("unknown line type: ${parts.first()}")
            }
        }
        if (selectedId != null && tabs.none { it.id == selectedId }) {
            throw SessionFormatException("selected id not present: $selectedId")
        }
        return SessionSnapshot(tabs, selectedId)
    }

    private fun parseTab(parts: List<String>): TabState {
        if (parts.size < 4) throw SessionFormatException("malformed tab line")
        val id = parts[1]
        val isPrivate = when (parts[2]) {
            "true" -> true
            "false" -> false
            else -> throw SessionFormatException("malformed isPrivate flag: ${parts[2]}")
        }
        val index = parts[3].toIntOrNull()
            ?: throw SessionFormatException("malformed historyIndex: ${parts[3]}")
        val history = parts.drop(4)
        return try {
            TabState(id = id, history = history, historyIndex = index, isPrivate = isPrivate)
        } catch (e: IllegalArgumentException) {
            throw SessionFormatException("invalid tab state for $id: ${e.message}", e)
        }
    }
}
