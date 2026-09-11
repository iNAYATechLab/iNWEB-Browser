package com.inweb.browser.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadsStoreTest {

    private fun record(id: String, state: DownloadState = DownloadState.QUEUED) =
        DownloadRecord(
            id = id,
            url = "https://example.com/$id",
            fileName = "$id.bin",
            mimeType = "application/octet-stream",
            state = state,
            bytesTotal = 1000L,
        )

    @Test
    fun addListAndFind() {
        val store = InMemoryDownloadsStore()
        store.add(record("d0"))
        store.add(record("d1"))

        assertEquals(listOf("d0", "d1"), store.all().map { it.id })
        assertNotNull(store.find("d0"))
        assertEquals("d0.bin", store.find("d0")?.fileName)
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateAddRejected() {
        val store = InMemoryDownloadsStore()
        store.add(record("d0"))
        store.add(record("d0"))
    }

    @Test
    fun updateReplacesRecordState() {
        val store = InMemoryDownloadsStore()
        store.add(record("d0"))
        val updated = store.find("d0")!!.transitionTo(DownloadState.RUNNING).addBytes(500)
        store.update(updated)
        assertEquals(DownloadState.RUNNING, store.find("d0")?.state)
        assertEquals(500L, store.find("d0")?.bytesReceived)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateUnknownRecordRejected() {
        InMemoryDownloadsStore().update(record("ghost"))
    }

    @Test
    fun removeBehavior() {
        val store = InMemoryDownloadsStore()
        store.add(record("d0"))
        assertTrue(store.remove("d0"))
        assertFalse(store.remove("d0"))
        assertEquals(0, store.all().size)
    }
}
