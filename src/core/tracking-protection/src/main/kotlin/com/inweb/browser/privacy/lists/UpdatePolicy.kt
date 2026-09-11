package com.inweb.browser.privacy.lists

/**
 * Update-scheduling *decision* model. The actual timer (alarm, WorkManager,
 * or the engine patch's own scheduler on the build host) lives in the host
 * layer and calls [FilterListManager.refresh] when this policy says a list
 * is due — the policy itself never runs background work.
 */
data class UpdatePolicy(
    val enabled: Boolean = true,
    /** Download a list on startup when no cached copy exists. */
    val fetchOnStartup: Boolean = true,
    /** Minimum age of the last confirmed download before a refresh is due. */
    val refreshIntervalMs: Long = 24L * 60 * 60 * 1000,
) {
    init {
        require(refreshIntervalMs >= 0) { "refreshIntervalMs must be >= 0" }
    }

    fun isRefreshDue(metadata: FilterListMetadata?, nowMillis: Long): Boolean {
        if (!enabled) return false
        if (metadata == null) return true
        return nowMillis - metadata.downloadedAtMillis >= refreshIntervalMs
    }
}
