package com.inweb.browser.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadPreferencesTest {

    private fun ok(result: DownloadPrefsResult<DownloadPreferences>): DownloadPreferences =
        (result as DownloadPrefsResult.Ok).value

    private fun seeded(
        askBeforeDownload: Boolean = true,
        downloadFolder: String? = null,
    ): DownloadSettings {
        val store = InMemoryDownloadPreferencesStore()
        store.save(
            DownloadStoreData(
                askBeforeDownload = askBeforeDownload,
                downloadFolder = downloadFolder,
            )
        )
        return DownloadSettings(store)
    }

    // --- model -----------------------------------------------------------------

    @Test
    fun defaultsAskBeforeDownloadingToTheSystemFolder() {
        val prefs = DownloadPreferences()
        assertTrue(prefs.askBeforeDownload)
        assertNull(prefs.downloadFolder)
    }

    @Test
    fun folderValidationAcceptsNullOrNonBlankOnly() {
        assertTrue(DownloadSettings.isValidFolder(null))
        assertTrue(DownloadSettings.isValidFolder("content://tree/primary%3ADownloads"))
        assertTrue(DownloadSettings.isValidFolder("/storage/emulated/0/Download"))
        assertEquals(false, DownloadSettings.isValidFolder(""))
        assertEquals(false, DownloadSettings.isValidFolder("   "))
    }

    // --- operations (validated + persisted) --------------------------------------

    @Test
    fun emptyStoreIsSeededWithDefaultsAndPersisted() {
        val store = InMemoryDownloadPreferencesStore()
        val settings = DownloadSettings(store)
        assertTrue(settings.current().askBeforeDownload)
        assertNull(settings.current().downloadFolder)
        assertEquals(DownloadStoreData(askBeforeDownload = true, downloadFolder = null), store.load())
        assertNull(settings.lastRecovery())
    }

    @Test
    fun setAskBeforeDownloadPersists() {
        val store = InMemoryDownloadPreferencesStore()
        val settings = DownloadSettings(store)
        settings.setAskBeforeDownload(false)
        assertEquals(false, settings.current().askBeforeDownload)
        assertEquals(false, store.load()?.askBeforeDownload)
        settings.setAskBeforeDownload(true)
        assertEquals(true, store.load()?.askBeforeDownload)
    }

    @Test
    fun setFolderNullRestoresTheSystemFolderAndPersists() {
        val settings = seeded(askBeforeDownload = false, downloadFolder = "/sdcard/Inweb")
        val next = ok(settings.setDownloadFolder(null))
        assertNull(next.downloadFolder)
        assertNull(settings.current().downloadFolder)
        assertEquals(false, settings.current().askBeforeDownload) // untouched
    }

    @Test
    fun setFolderValidPersistsTrimmed() {
        val store = InMemoryDownloadPreferencesStore()
        val settings = DownloadSettings(store)
        ok(settings.setDownloadFolder("  content://tree/primary%3ADownload  "))
        assertEquals("content://tree/primary%3ADownload", settings.current().downloadFolder)
        assertEquals("content://tree/primary%3ADownload", store.load()?.downloadFolder)
    }

    @Test
    fun setFolderBlankIsRejectedAndStateUnchanged() {
        val settings = seeded(askBeforeDownload = true, downloadFolder = "/sdcard/Inweb")
        for (blank in listOf("", "   ")) {
            val result = settings.setDownloadFolder(blank)
            assertTrue(result is DownloadPrefsResult.Err)
            assertEquals(DownloadPrefsError.BlankFolder, (result as DownloadPrefsResult.Err).error)
        }
        assertEquals("/sdcard/Inweb", settings.current().downloadFolder)
    }

    @Test
    fun resetRestoresDefaults() {
        val settings = seeded(askBeforeDownload = false, downloadFolder = "/sdcard/Inweb")
        val reset = settings.reset()
        assertTrue(reset.askBeforeDownload)
        assertNull(reset.downloadFolder)
        assertEquals(reset, settings.current())
    }

    // --- stored data: load, corrupt recovery --------------------------------------

    @Test
    fun validStoredDataLoadsAsIs() {
        val settings = seeded(askBeforeDownload = false, downloadFolder = "/sdcard/Inweb")
        assertEquals(false, settings.current().askBeforeDownload)
        assertEquals("/sdcard/Inweb", settings.current().downloadFolder)
        assertNull(settings.lastRecovery())
    }

    @Test
    fun corruptBlankFolderRecoversToDefaultsAndPersistsTheRepair() {
        val store = InMemoryDownloadPreferencesStore()
        store.save(DownloadStoreData(askBeforeDownload = false, downloadFolder = "   "))
        val settings = DownloadSettings(store)
        val recovery = settings.lastRecovery() as DownloadPrefsError.CorruptStoredData
        assertEquals("   ", recovery.folder)
        assertTrue(settings.current().askBeforeDownload)
        assertNull(settings.current().downloadFolder)
        assertEquals(DownloadStoreData(askBeforeDownload = true, downloadFolder = null), store.load())
    }

    // --- store seam ----------------------------------------------------------------

    @Test
    fun inMemoryStoreRoundTripsTheWireForm() {
        val store = InMemoryDownloadPreferencesStore()
        assertNull(store.load())
        val data = DownloadStoreData(askBeforeDownload = false, downloadFolder = "/sdcard/Inweb")
        store.save(data)
        assertEquals(data, store.load())
    }
}
