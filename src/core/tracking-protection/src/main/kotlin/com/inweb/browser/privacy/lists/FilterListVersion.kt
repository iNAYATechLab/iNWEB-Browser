package com.inweb.browser.privacy.lists

/** Version information extracted from EasyList-family header comments. */
data class FilterListVersionInfo(
    val version: String?,
    val lastModified: String?,
)

/**
 * Extracts `! Version:` and `! Last modified:` header comments from a filter
 * list (the EasyList-family convention). Matching is case-insensitive; the
 * first occurrence wins. Absent or empty headers yield null — never a made-up
 * value.
 */
object FilterListVersion {

    fun parse(text: String): FilterListVersionInfo {
        var version: String? = null
        var lastModified: String? = null
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (!trimmed.startsWith("!")) continue
            val comment = trimmed.removePrefix("!")
            if (version == null && comment.trimStart().startsWith("Version:", ignoreCase = true)) {
                version = comment.substringAfter(':').trim().takeIf { it.isNotEmpty() }
            } else if (lastModified == null && comment.trimStart().startsWith("Last modified:", ignoreCase = true)) {
                lastModified = comment.substringAfter(':').trim().takeIf { it.isNotEmpty() }
            }
            if (version != null && lastModified != null) break
        }
        return FilterListVersionInfo(version, lastModified)
    }
}
