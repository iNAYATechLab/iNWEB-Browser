package com.inweb.browser.sync

/** One local change awaiting synchronization. */
enum class SyncOperation { UPSERT, DELETE }

/** An append-only change-log entry (§30). DELETE entries are tombstones. */
data class ChangeEntry(
    val revision: Long,
    val storeName: String,
    val itemKey: String,
    val operation: SyncOperation,
    /** Serialized item — empty for tombstones. */
    val payload: String,
    val timestampMillis: Long,
)

/** Transport seam. A real backend implements this; tests use a fake. */
interface SyncTransport {
    fun send(batch: List<ChangeEntry>): TransportResult
}

sealed class TransportResult {
    object Success : TransportResult()
    data class Failure(val retryable: Boolean) : TransportResult()
}

/** Exponential backoff with a cap and a maximum attempt count. */
class RetryPolicy(
    val baseDelayMillis: Long = 1_000L,
    val maxDelayMillis: Long = 300_000L,
    val maxAttempts: Int = 8,
) {
    fun delayFor(attempt: Int): Long {
        if (attempt <= 0) return 0L
        val exp = baseDelayMillis shl (attempt - 1.coerceAtMost(30))
        return exp.coerceAtMost(maxDelayMillis)
    }

    fun canRetry(attempt: Int): Boolean = attempt < maxAttempts
}

/** Persistence seam (§30 durable queue — survives restarts at the app layer). */
interface SyncQueueStore {
    fun load(): SyncQueueState
    fun save(state: SyncQueueState)
}

data class SyncQueueState(
    val pending: List<ChangeEntry>,
    val failed: List<ChangeEntry>,
    val nextRevision: Long,
    val attempts: Int,
    val nextAttemptAtMillis: Long?,
)

class InMemorySyncQueueStore : SyncQueueStore {
    private var state = SyncQueueState(emptyList(), emptyList(), 1L, 0, null)
    override fun load(): SyncQueueState = state
    override fun save(state: SyncQueueState) {
        this.state = state.copy(pending = state.pending.toList(), failed = state.failed.toList())
    }
}

/** Outcome of one flush attempt. */
sealed class FlushResult {
    data class Synced(val acknowledged: Int) : FlushResult()
    data class RetryScheduled(val attempt: Int, val notBeforeMillis: Long) : FlushResult()
    data class Failed(val reason: String) : FlushResult()
    object Deferred : FlushResult()
    object NothingToSync : FlushResult()
}

/**
 * Last-writer-wins conflict resolution with tombstones (§30; ADR-026):
 * the later timestamp wins — deletes are ordinary entries, so a newer
 * upsert legitimately beats an older tombstone and vice versa. On an
 * EXACT timestamp tie the local entry wins — a deterministic rule that
 * avoids resolution ping-pong. No CRDTs are claimed.
 */
object ConflictResolver {
    fun resolve(local: ChangeEntry, remote: ChangeEntry): ChangeEntry = when {
        remote.timestampMillis > local.timestampMillis -> remote
        else -> local
    }
}

/**
 * The sync queue engine (§30) — durable change log + retry + conflict
 * model. FUTURE INFRASTRUCTURE (§29): no backend exists; nothing in the
 * product may present this as a feature.
 */
class SyncEngine(
    private val store: SyncQueueStore = InMemorySyncQueueStore(),
    private val retryPolicy: RetryPolicy = RetryPolicy(),
    private val nowMillis: () -> Long = { 0L },
) {

    private var pending: MutableList<ChangeEntry>
    private var failed: MutableList<ChangeEntry>
    private var nextRevision: Long
    private var attempts: Int
    private var nextAttemptAtMillis: Long?

    init {
        val state = store.load()
        pending = state.pending.toMutableList()
        failed = state.failed.toMutableList()
        nextRevision = state.nextRevision
        attempts = state.attempts
        nextAttemptAtMillis = state.nextAttemptAtMillis
    }

    /** Appends a change; revisions are monotonic and never reused. */
    fun record(
        storeName: String,
        itemKey: String,
        operation: SyncOperation,
        payload: String,
        timestampMillis: Long = nowMillis(),
    ): ChangeEntry {
        val entry = ChangeEntry(
            revision = nextRevision,
            storeName = storeName,
            itemKey = itemKey,
            operation = operation,
            payload = payload,
            timestampMillis = timestampMillis,
        )
        nextRevision++
        pending += entry
        persist()
        return entry
    }

    /** Unsent changes in revision order (oldest first). */
    fun pendingChanges(): List<ChangeEntry> = pending.toList()

    /** Entries that exhausted retries or failed permanently — need attention. */
    fun failedChanges(): List<ChangeEntry> = failed.toList()

    /**
     * Sends every pending change as one batch. On retryable failure the
     * next attempt is scheduled with backoff; on permanent failure (or
     * exhausted retries) the batch moves to [failedChanges] and is
     * REPORTED, never silently dropped.
     */
    fun attemptFlush(transport: SyncTransport): FlushResult {
        val now = nowMillis()
        if (pending.isEmpty()) return FlushResult.NothingToSync
        val gate = nextAttemptAtMillis
        if (gate != null && now < gate) return FlushResult.Deferred

        return when (val result = transport.send(pending.toList())) {
            is TransportResult.Success -> {
                val acknowledged = pending.size
                pending.clear()
                attempts = 0
                nextAttemptAtMillis = null
                persist()
                FlushResult.Synced(acknowledged)
            }
            is TransportResult.Failure -> {
                if (result.retryable && retryPolicy.canRetry(attempts)) {
                    attempts++
                    val delay = retryPolicy.delayFor(attempts)
                    nextAttemptAtMillis = now + delay
                    persist()
                    FlushResult.RetryScheduled(attempt = attempts, notBeforeMillis = now + delay)
                } else {
                    val reason = if (result.retryable) {
                        "retry budget exhausted after $attempts attempt(s)"
                    } else {
                        "transport reported a permanent failure"
                    }
                    failed += pending
                    pending.clear()
                    attempts = 0
                    nextAttemptAtMillis = null
                    persist()
                    FlushResult.Failed(reason)
                }
            }
        }
    }

    private fun persist() = store.save(
        SyncQueueState(
            pending = pending.toList(),
            failed = failed.toList(),
            nextRevision = nextRevision,
            attempts = attempts,
            nextAttemptAtMillis = nextAttemptAtMillis,
        ),
    )
}
