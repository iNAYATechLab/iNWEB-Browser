package com.inweb.browser.offline

/**
 * One saved page (MASTER-SPEC §17). Sizes and timestamps are REAL values
 * supplied by the snapshot pipeline — this model never invents them.
 */
data class OfflinePageRecord(
    /** The online URL the snapshot was taken from (the library key). */
    val onlineUrl: String,
    val title: String,
    /** Snapshot file name within the browser's offline-snapshots directory. */
    val snapshotFileName: String,
    /** Actual on-disk size of the snapshot file, in bytes (> 0). */
    val sizeBytes: Long,
    val createdAtMillis: Long,
    val lastAccessedAtMillis: Long,
)

/** Persistence seam; the patch layer provides the real store (B-001). */
interface OfflineStore {
    fun load(): List<OfflinePageRecord>
    fun save(entries: List<OfflinePageRecord>)
}

/** In-memory store (tests, and until the file-backed store ships with the patches). */
class InMemoryOfflineStore : OfflineStore {
    private var entries: List<OfflinePageRecord> = emptyList()
    override fun load(): List<OfflinePageRecord> = entries
    override fun save(entries: List<OfflinePageRecord>) {
        this.entries = entries.toList()
    }
}

/** Why a library operation failed — expected failures are results, not exceptions. */
sealed class LibraryError {
    data class InvalidRecord(val reason: String) : LibraryError()
    object NotFound : LibraryError()
    data class QuotaExceeded(val neededBytes: Long, val quotaBytes: Long) : LibraryError()
}

sealed class LibraryResult<out T> {
    data class Ok<T>(val value: T) : LibraryResult<T>()
    data class Err(val error: LibraryError) : LibraryResult<Nothing>()
}

/**
 * Successful [OfflineLibrary.save]: the stored record, the records evicted
 * to make quota (least-recently-accessed first), and the record REPLACED
 * because the same online URL was saved again. The caller deletes the
 * snapshot files of every evicted/replaced record.
 */
data class SavedPage(
    val record: OfflinePageRecord,
    val evicted: List<OfflinePageRecord>,
    val replaced: OfflinePageRecord?,
)

/**
 * The offline library (§17): saved pages with real quota accounting and
 * LRU eviction. Snapshots are USER DATA — distinct from the HTTP cache by
 * design (ADR-024); nothing in this class ever touches cache storage.
 */
class OfflineLibrary(
    private val store: OfflineStore = InMemoryOfflineStore(),
    private val quotaBytes: Long = DEFAULT_QUOTA_BYTES,
) {

    private val entries = LinkedHashMap<String, OfflinePageRecord>()

    init {
        store.load().forEach { entries[it.onlineUrl] = it }
    }

    /**
     * Registers a snapshot. Enforces the quota by evicting the
     * least-recently-accessed pages first — never a pinned page (e.g. the
     * currently-open one), never the record being saved. Saving a URL that
     * already exists REPLACES it (the old record is returned as `replaced`,
     * its bytes stop counting). On quota failure nothing changes (atomic).
     */
    fun save(
        record: OfflinePageRecord,
        pinnedUrls: Set<String> = emptySet(),
    ): LibraryResult<SavedPage> {
        if (record.onlineUrl.isBlank()) {
            return LibraryResult.Err(LibraryError.InvalidRecord("onlineUrl is blank"))
        }
        if (record.snapshotFileName.isBlank()) {
            return LibraryResult.Err(LibraryError.InvalidRecord("snapshotFileName is blank"))
        }
        if (record.sizeBytes <= 0L) {
            return LibraryResult.Err(LibraryError.InvalidRecord("sizeBytes must be positive"))
        }

        val replaced = entries[record.onlineUrl]

        // Candidate set after this save: everything except a replaced
        // same-URL record, plus the new record.
        val others = entries.values.filter { it.onlineUrl != record.onlineUrl }
        val evictable = others
            .filter { it.onlineUrl !in pinnedUrls }
            .sortedWith(compareBy({ it.lastAccessedAtMillis }, { it.createdAtMillis }, { it.onlineUrl }))

        var keptBytes = others.sumOf { it.sizeBytes } + record.sizeBytes
        val evicted = mutableListOf<OfflinePageRecord>()
        for (candidate in evictable) {
            if (keptBytes <= quotaBytes) break
            keptBytes -= candidate.sizeBytes
            evicted += candidate
        }
        if (keptBytes > quotaBytes) {
            return LibraryResult.Err(
                LibraryError.QuotaExceeded(neededBytes = record.sizeBytes, quotaBytes = quotaBytes),
            )
        }

        evicted.forEach { entries.remove(it.onlineUrl) }
        entries[record.onlineUrl] = record
        persist()
        return LibraryResult.Ok(SavedPage(record = record, evicted = evicted, replaced = replaced))
    }

    /** Marks a page accessed at [nowMillis] — feeds LRU eviction. */
    fun access(onlineUrl: String, nowMillis: Long): LibraryResult<OfflinePageRecord> {
        val current = entries[onlineUrl]
            ?: return LibraryResult.Err(LibraryError.NotFound)
        // access times never rewind
        val updated = current.copy(
            lastAccessedAtMillis = maxOf(current.lastAccessedAtMillis, nowMillis),
        )
        entries[onlineUrl] = updated
        persist()
        return LibraryResult.Ok(updated)
    }

    /** Removes a page; the caller deletes its snapshot file. */
    fun delete(onlineUrl: String): LibraryResult<OfflinePageRecord> {
        val removed = entries.remove(onlineUrl)
            ?: return LibraryResult.Err(LibraryError.NotFound)
        persist()
        return LibraryResult.Ok(removed)
    }

    fun get(onlineUrl: String): OfflinePageRecord? = entries[onlineUrl]

    /** Insertion order, oldest first (project-wide order contract). */
    fun all(): List<OfflinePageRecord> = entries.values.toList()

    fun totalBytes(): Long = entries.values.sumOf { it.sizeBytes }

    fun remainingBytes(): Long = (quotaBytes - totalBytes()).coerceAtLeast(0L)

    private fun persist() = store.save(entries.values.toList())

    companion object {
        const val DEFAULT_QUOTA_BYTES: Long = 512L * 1024 * 1024
    }
}
