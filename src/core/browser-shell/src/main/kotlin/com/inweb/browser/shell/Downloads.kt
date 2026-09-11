package com.inweb.browser.shell

/** Lifecycle states of a download. COMPLETED / FAILED / CANCELLED are terminal. */
enum class DownloadState { QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED }

/**
 * Metadata and state machine for one download (MASTER-SPEC §8, §33).
 * Illegal transitions are rejected so the UI can never show an impossible state.
 */
data class DownloadRecord(
    val id: String,
    val url: String,
    val fileName: String,
    val mimeType: String,
    val state: DownloadState = DownloadState.QUEUED,
    val bytesTotal: Long = -1L,
    val bytesReceived: Long = 0L,
) {
    init {
        require(fileName.isNotBlank()) { "fileName must not be blank" }
        require(bytesReceived >= 0L) { "bytesReceived must be >= 0" }
        require(bytesTotal == -1L || bytesTotal >= 0L) { "bytesTotal must be -1 or >= 0" }
        if (bytesTotal >= 0L) {
            require(bytesReceived <= bytesTotal) { "bytesReceived exceeds bytesTotal" }
        }
    }

    /** Progress in [0, 1], or null when the total size is unknown. */
    val progress: Float?
        get() = if (bytesTotal > 0L) bytesReceived.toFloat() / bytesTotal.toFloat() else null

    /** Applies a state transition; illegal transitions throw [IllegalArgumentException]. */
    fun transitionTo(newState: DownloadState): DownloadRecord {
        val allowed = when (state) {
            DownloadState.QUEUED ->
                setOf(DownloadState.RUNNING, DownloadState.FAILED, DownloadState.CANCELLED)
            DownloadState.RUNNING ->
                setOf(DownloadState.PAUSED, DownloadState.COMPLETED, DownloadState.FAILED, DownloadState.CANCELLED)
            DownloadState.PAUSED ->
                setOf(DownloadState.RUNNING, DownloadState.FAILED, DownloadState.CANCELLED)
            DownloadState.COMPLETED, DownloadState.FAILED, DownloadState.CANCELLED -> emptySet()
        }
        require(newState in allowed) { "illegal transition: $state -> $newState" }
        return copy(state = newState)
    }

    /** Records received bytes; only valid while RUNNING. Clamps at a known total. */
    fun addBytes(count: Long): DownloadRecord {
        require(count >= 0L) { "count must be >= 0" }
        check(state == DownloadState.RUNNING) { "bytes can only arrive while RUNNING (was $state)" }
        val updated = if (bytesTotal > 0L) {
            minOf(bytesReceived + count, bytesTotal)
        } else {
            bytesReceived + count
        }
        return copy(bytesReceived = updated)
    }
}
