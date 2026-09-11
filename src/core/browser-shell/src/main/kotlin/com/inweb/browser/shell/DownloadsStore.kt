package com.inweb.browser.shell

/**
 * Port for the downloads catalog shown in the downloads surface
 * (MASTER-SPEC §8, §33). Records follow the [DownloadRecord] state machine.
 */
interface DownloadsStore {
    /** Adds a new download record; rejects duplicate ids. */
    fun add(record: DownloadRecord)

    /** Replaces the stored state of an existing record. */
    fun update(record: DownloadRecord)

    /** All records, oldest first. */
    fun all(): List<DownloadRecord>

    fun find(id: String): DownloadRecord?

    /** Removes a record. Returns true if it existed. */
    fun remove(id: String): Boolean
}

/** In-memory implementation for tests and engine-less previews. */
class InMemoryDownloadsStore : DownloadsStore {
    private val records = LinkedHashMap<String, DownloadRecord>()

    override fun add(record: DownloadRecord) {
        require(!records.containsKey(record.id)) { "download already exists: ${record.id}" }
        records[record.id] = record
    }

    override fun update(record: DownloadRecord) {
        require(records.containsKey(record.id)) { "unknown download: ${record.id}" }
        records[record.id] = record
    }

    override fun all(): List<DownloadRecord> = records.values.toList()

    override fun find(id: String): DownloadRecord? = records[id]

    override fun remove(id: String): Boolean = records.remove(id) != null
}
