package com.inweb.browser.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class DownloadRecordTest {

    private fun record(state: DownloadState = DownloadState.QUEUED, total: Long = 100L) =
        DownloadRecord(
            id = "d0",
            url = "https://example.com/f.bin",
            fileName = "f.bin",
            mimeType = "application/octet-stream",
            state = state,
            bytesTotal = total,
            bytesReceived = 0L,
        )

    @Test
    fun queuedDownloadsCanStart() {
        assertEquals(DownloadState.RUNNING, record().transitionTo(DownloadState.RUNNING).state)
    }

    @Test(expected = IllegalArgumentException::class)
    fun queuedCannotJumpToCompleted() {
        record().transitionTo(DownloadState.COMPLETED)
    }

    @Test
    fun runningCanPauseAndResume() {
        val paused = record(DownloadState.RUNNING).transitionTo(DownloadState.PAUSED)
        assertEquals(DownloadState.PAUSED, paused.state)
        assertEquals(DownloadState.RUNNING, paused.transitionTo(DownloadState.RUNNING).state)
    }

    @Test
    fun runningCanComplete() {
        val running = record(DownloadState.RUNNING)
        assertEquals(DownloadState.COMPLETED, running.transitionTo(DownloadState.COMPLETED).state)
    }

    @Test
    fun terminalStatesAreLocked() {
        val terminalStates = listOf(
            DownloadState.COMPLETED,
            DownloadState.FAILED,
            DownloadState.CANCELLED,
        )
        for (terminal in terminalStates) {
            try {
                record(terminal).transitionTo(DownloadState.RUNNING)
                fail("expected transition out of $terminal to be rejected")
            } catch (expected: IllegalArgumentException) {
                // expected
            }
        }
    }

    @Test
    fun bytesAccumulateAndClampAtTotal() {
        val running = record(DownloadState.RUNNING)
        val half = running.addBytes(50)
        assertEquals(50L, half.bytesReceived)
        assertEquals(0.5f, half.progress!!, 0.0001f)
        val clamped = half.addBytes(999)
        assertEquals(100L, clamped.bytesReceived)
        assertEquals(1.0f, clamped.progress!!, 0.0001f)
    }

    @Test(expected = IllegalStateException::class)
    fun bytesRejectedWhenNotRunning() {
        record(DownloadState.QUEUED).addBytes(1)
    }

    @Test
    fun unknownTotalHasNoProgressValue() {
        val running = DownloadRecord(
            id = "d1",
            url = "https://example.com/x",
            fileName = "x",
            mimeType = "text/plain",
            state = DownloadState.RUNNING,
            bytesTotal = -1L,
        )
        assertNull(running.progress)
        assertEquals(42L, running.addBytes(42).bytesReceived)
    }

    @Test(expected = IllegalArgumentException::class)
    fun receivedGreaterThanTotalRejected() {
        DownloadRecord(
            id = "d2",
            url = "https://example.com/y",
            fileName = "y",
            mimeType = "text/plain",
            bytesTotal = 10L,
            bytesReceived = 11L,
        )
    }
}
