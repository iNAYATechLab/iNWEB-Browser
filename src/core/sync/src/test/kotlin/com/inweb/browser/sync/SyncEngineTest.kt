package com.inweb.browser.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncEngineTest {

    private class MutableClock(var now: Long = 1_000L) {
        fun tick(by: Long) { now += by }
    }

    /** Scriptable fake transport — the §30 model is exercised without a backend. */
    private class FakeTransport(var results: List<TransportResult>) : SyncTransport {
        var calls = 0
        var lastBatch: List<ChangeEntry> = emptyList()
        override fun send(batch: List<ChangeEntry>): TransportResult {
            lastBatch = batch
            val result = results.getOrElse(calls) { TransportResult.Success }
            calls++
            return result
        }
    }

    private fun entry(revision: Long, ts: Long, op: SyncOperation = SyncOperation.UPSERT) =
        ChangeEntry(revision, "bookmarks", "b-1", op, if (op == SyncOperation.UPSERT) "payload" else "", ts)

    // --- change log -----------------------------------------------------------

    @Test
    fun recordAssignsMonotonicRevisionsInInsertionOrder() {
        val engine = SyncEngine(nowMillis = { 5_000L })
        engine.record("bookmarks", "b-1", SyncOperation.UPSERT, "one")
        engine.record("history", "h-9", SyncOperation.UPSERT, "two")
        engine.record("bookmarks", "b-2", SyncOperation.DELETE, "")

        val pending = engine.pendingChanges()
        assertEquals(listOf(1L, 2L, 3L), pending.map { it.revision })
        assertEquals(5_000L, pending[0].timestampMillis)
        assertEquals(SyncOperation.DELETE, pending[2].operation)
        assertTrue(pending[2].payload.isEmpty())
    }

    // --- flush / retry / failure ------------------------------------------------

    @Test
    fun successfulFlushAcksEverythingAndClearsPending() {
        val engine = SyncEngine()
        engine.record("bookmarks", "b-1", SyncOperation.UPSERT, "x")
        engine.record("bookmarks", "b-2", SyncOperation.UPSERT, "y")

        val result = engine.attemptFlush(FakeTransport(emptyList()))

        assertEquals(2, (result as FlushResult.Synced).acknowledged)
        assertTrue(engine.pendingChanges().isEmpty())
        assertTrue(engine.failedChanges().isEmpty())
    }

    @Test
    fun retryableFailureSchedulesExponentialBackoff() {
        val clock = MutableClock()
        val engine = SyncEngine(nowMillis = { clock.now })
        engine.record("bookmarks", "b-1", SyncOperation.UPSERT, "x")

        val transport = FakeTransport(
            listOf(TransportResult.Failure(retryable = true), TransportResult.Failure(retryable = true)),
        )
        val first = engine.attemptFlush(transport) as FlushResult.RetryScheduled
        assertEquals(1, first.attempt)
        assertEquals(1_000L, first.notBeforeMillis - clock.now)

        // before the gate: deferred
        clock.tick(500)
        assertEquals(FlushResult.Deferred, engine.attemptFlush(transport))

        clock.tick(600)
        val second = engine.attemptFlush(transport) as FlushResult.RetryScheduled
        assertEquals(2, second.attempt)
        assertEquals(2_000L, second.notBeforeMillis - clock.now)
    }

    @Test
    fun retrySucceedsAfterEarlierFailure() {
        val clock = MutableClock()
        val engine = SyncEngine(nowMillis = { clock.now })
        engine.record("bookmarks", "b-1", SyncOperation.UPSERT, "x")

        val transport = FakeTransport(listOf(TransportResult.Failure(retryable = true)))
        assertTrue(engine.attemptFlush(transport) is FlushResult.RetryScheduled)

        clock.tick(1_500)
        val recovered = engine.attemptFlush(transport) as FlushResult.Synced
        assertEquals(1, recovered.acknowledged)
        assertTrue(engine.pendingChanges().isEmpty())
        // retry state reset: the next failure starts backoff from attempt 1
        engine.record("bookmarks", "b-2", SyncOperation.UPSERT, "y")
        val transport2 = FakeTransport(listOf(TransportResult.Failure(retryable = true)))
        val again = engine.attemptFlush(transport2) as FlushResult.RetryScheduled
        assertEquals(1, again.attempt)
    }

    @Test
    fun permanentFailureMovesEntriesToFailed() {
        val engine = SyncEngine()
        engine.record("bookmarks", "b-1", SyncOperation.UPSERT, "x")
        engine.record("history", "h-1", SyncOperation.UPSERT, "y")

        val result = engine.attemptFlush(
            FakeTransport(listOf(TransportResult.Failure(retryable = false))),
        ) as FlushResult.Failed

        assertTrue(result.reason.contains("permanent"))
        assertEquals(2, engine.failedChanges().size)
        assertTrue(engine.pendingChanges().isEmpty())
    }

    @Test
    fun exhaustedRetriesMoveEntriesToFailedAndReport() {
        val clock = MutableClock()
        val policy = RetryPolicy(baseDelayMillis = 1L, maxDelayMillis = 4L, maxAttempts = 2)
        val engine = SyncEngine(retryPolicy = policy, nowMillis = { clock.now })
        engine.record("bookmarks", "b-1", SyncOperation.UPSERT, "x")

        val failures = List(10) { TransportResult.Failure(retryable = true) }
        val transport = FakeTransport(failures)

        assertTrue(engine.attemptFlush(transport) is FlushResult.RetryScheduled) // attempt 1
        clock.tick(2)
        assertTrue(engine.attemptFlush(transport) is FlushResult.RetryScheduled) // attempt 2
        clock.tick(10)
        val exhausted = engine.attemptFlush(transport) as FlushResult.Failed
        assertTrue(exhausted.reason.contains("exhausted"))
        assertEquals(1, engine.failedChanges().size)
    }

    @Test
    fun emptyQueueHasNothingToSync() {
        val engine = SyncEngine()
        assertEquals(FlushResult.NothingToSync, engine.attemptFlush(FakeTransport(emptyList())))
    }

    // --- conflict resolution (LWW with tombstones) ---------------------------------

    @Test
    fun laterTimestampWins() {
        val older = entry(1, ts = 1_000L)
        val newer = entry(2, ts = 2_000L)
        assertEquals(newer, ConflictResolver.resolve(older, newer))
        assertEquals(newer, ConflictResolver.resolve(newer, older))
    }

    @Test
    fun exactTieBreaksToLocalDeterministically() {
        val local = entry(1, ts = 1_000L)
        val remote = entry(2, ts = 1_000L)
        assertEquals(local, ConflictResolver.resolve(local, remote))
    }

    @Test
    fun newerTombstoneBeatsOlderUpsert() {
        val upsert = entry(1, ts = 1_000L, op = SyncOperation.UPSERT)
        val tombstone = entry(2, ts = 2_000L, op = SyncOperation.DELETE)
        assertEquals(tombstone, ConflictResolver.resolve(upsert, tombstone))
    }

    @Test
    fun newerUpsertBeatsOlderTombstone() {
        val tombstone = entry(1, ts = 1_000L, op = SyncOperation.DELETE)
        val upsert = entry(2, ts = 2_000L, op = SyncOperation.UPSERT)
        assertEquals(upsert, ConflictResolver.resolve(tombstone, upsert))
    }

    // --- persistence seam --------------------------------------------------------------

    @Test
    fun queueStateSurvivesStoreRoundTrip() {
        val store = InMemorySyncQueueStore()
        val clock = MutableClock()
        val first = SyncEngine(store = store, nowMillis = { clock.now })
        first.record("bookmarks", "b-1", SyncOperation.UPSERT, "x")
        first.record("bookmarks", "b-2", SyncOperation.UPSERT, "y")
        assertTrue(
            first.attemptFlush(FakeTransport(listOf(TransportResult.Failure(retryable = true)))) is FlushResult.RetryScheduled,
        )
        first.record("history", "h-1", SyncOperation.UPSERT, "z")

        val second = SyncEngine(store = store, nowMillis = { clock.now })
        assertEquals(listOf(1L, 2L, 3L), second.pendingChanges().map { it.revision })
        // retry state is durable too
        val transport = FakeTransport(emptyList())
        assertEquals(FlushResult.Deferred, second.attemptFlush(transport))
        clock.tick(2_000)
        assertTrue(second.attemptFlush(transport) is FlushResult.Synced)
    }
}
