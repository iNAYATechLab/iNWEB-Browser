package com.inweb.browser.privacy.lists

import java.io.File
import java.io.IOException

/** Cache metadata persisted beside each cached list body. */
data class FilterListMetadata(
    val sourceId: String,
    /** When this content was last successfully downloaded or confirmed (304). */
    val downloadedAtMillis: Long,
    val etag: String? = null,
    val lastModified: String? = null,
    /** `! Version:` header parsed from the list text, if present. */
    val version: String? = null,
    /** Network rules parsed from the stored body at store time. */
    val ruleCount: Int = 0,
    /**
     * SHA-256 of the stored body, pinned at download time (G-07). A
     * cached copy whose body no longer matches is never served. Null =
     * stored before pinning existed (legacy copy) — verified as-is.
     */
    val contentSha256: String? = null,
)

/** Port: raw filter-list cache keyed by source id. */
interface FilterListCache {
    fun store(source: FilterListSource, body: String, metadata: FilterListMetadata)
    fun loadBody(source: FilterListSource): String?
    fun loadMetadata(source: FilterListSource): FilterListMetadata?
    fun remove(source: FilterListSource)
    fun clear()
}

/**
 * Real file-backed cache: one directory, two files per source
 * (`<id>.txt` body + `<id>.meta` metadata). Writes are atomic
 * (temp file + rename), mirroring the session-persistence strategy
 * (ADR-011). A corrupt metadata file is reported as missing — the body
 * stays usable; the manager re-downloads on the next refresh.
 */
class FileFilterListCache(private val directory: File) : FilterListCache {

    init {
        directory.mkdirs()
    }

    private fun bodyFile(id: String) = File(directory, "$id.txt")
    private fun metaFile(id: String) = File(directory, "$id.meta")

    override fun store(source: FilterListSource, body: String, metadata: FilterListMetadata) {
        writeAtomically(bodyFile(source.id), body)
        writeAtomically(metaFile(source.id), serialize(metadata))
    }

    override fun loadBody(source: FilterListSource): String? =
        bodyFile(source.id).takeIf { it.isFile }?.readText()

    override fun loadMetadata(source: FilterListSource): FilterListMetadata? =
        metaFile(source.id).takeIf { it.isFile }?.let { deserialize(it.readText()) }

    override fun remove(source: FilterListSource) {
        bodyFile(source.id).delete()
        metaFile(source.id).delete()
    }

    override fun clear() {
        directory.listFiles()?.forEach { it.delete() }
    }

    private fun writeAtomically(target: File, content: String) {
        val tmp = File(directory, target.name + ".tmp")
        tmp.writeText(content)
        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) {
            throw IOException("failed to persist ${target.name}")
        }
    }

    companion object {
        private const val HEADER = "iNWEB-FILTERLIST-META v=1"

        fun serialize(metadata: FilterListMetadata): String = buildString {
            append(HEADER).append('\n')
            append("sourceId\t").append(metadata.sourceId).append('\n')
            append("downloadedAt\t").append(metadata.downloadedAtMillis).append('\n')
            if (metadata.etag != null) append("etag\t").append(metadata.etag).append('\n')
            if (metadata.lastModified != null) append("lastModified\t").append(metadata.lastModified).append('\n')
            if (metadata.version != null) append("version\t").append(metadata.version).append('\n')
            append("ruleCount\t").append(metadata.ruleCount).append('\n')
            if (metadata.contentSha256 != null) append("contentSha256\t").append(metadata.contentSha256).append('\n')
        }

        /** Returns null for anything not matching the exact format (corruption). */
        fun deserialize(text: String): FilterListMetadata? {
            val lines = text.lines()
            if (lines.firstOrNull() != HEADER) return null
            val fields = mutableMapOf<String, String>()
            for (line in lines.drop(1)) {
                if (line.isEmpty()) continue
                val separator = line.indexOf('\t')
                if (separator <= 0) return null
                fields[line.substring(0, separator)] = line.substring(separator + 1)
            }
            val sourceId = fields["sourceId"] ?: return null
            if (sourceId.isBlank()) return null
            val downloadedAt = fields["downloadedAt"]?.toLongOrNull() ?: return null
            return FilterListMetadata(
                sourceId = sourceId,
                downloadedAtMillis = downloadedAt,
                etag = fields["etag"],
                lastModified = fields["lastModified"],
                version = fields["version"],
                ruleCount = fields["ruleCount"]?.toIntOrNull() ?: 0,
                contentSha256 = fields["contentSha256"],
            )
        }
    }
}

/** In-memory cache for tests and engine-less UI previews (no file system). */
class InMemoryFilterListCache : FilterListCache {
    private val bodies = HashMap<String, String>()
    private val metadata = HashMap<String, FilterListMetadata>()

    override fun store(source: FilterListSource, body: String, metadata: FilterListMetadata) {
        bodies[source.id] = body
        this.metadata[source.id] = metadata
    }

    override fun loadBody(source: FilterListSource): String? = bodies[source.id]

    override fun loadMetadata(source: FilterListSource): FilterListMetadata? = metadata[source.id]

    override fun remove(source: FilterListSource) {
        bodies.remove(source.id)
        metadata.remove(source.id)
    }

    override fun clear() {
        bodies.clear()
        metadata.clear()
    }
}

