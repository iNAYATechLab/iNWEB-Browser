package com.inweb.browser.shell

/**
 * Download preferences (MASTER-SPEC §23): how downloads start and
 * where they land. Bound to the real download flow via a settings/
 * patch (Phase 11 design §3): the ask-before-download flag gates the
 * download prompt, and the folder is the platform's public Downloads
 * directory by default (null) or a user-picked directory reference
 * (a SAF tree URI on Android).
 */
data class DownloadPreferences(
    /**
     * Ask before a download starts (default: yes — downloads never
     * begin silently).
     */
    val askBeforeDownload: Boolean = true,

    /**
     * The default download folder: null = the platform's public
     * Downloads directory (the only real default); otherwise a
     * non-blank, user-picked directory reference.
     */
    val downloadFolder: String? = null,
)

/** Why a download preference failed — results, not exceptions. */
sealed class DownloadPrefsError {
    /** An operation supplied a blank folder. */
    object BlankFolder : DownloadPrefsError()

    /** Stored preferences failed validation; defaults restored (persisted). */
    data class CorruptStoredData(val folder: String?) : DownloadPrefsError()
}

sealed class DownloadPrefsResult<out T> {
    data class Ok<T>(val value: T) : DownloadPrefsResult<T>()
    data class Err(val error: DownloadPrefsError) : DownloadPrefsResult<Nothing>()
}

/** What the persistence seam round-trips (the wire form). */
data class DownloadStoreData(
    val askBeforeDownload: Boolean,
    val downloadFolder: String?,
)

/** Persistence seam; the patch layer provides the real store (B-001). */
interface DownloadPreferencesStore {
    fun load(): DownloadStoreData?
    fun save(data: DownloadStoreData)
}

/** In-memory store (tests, and until the preference-backed store ships). */
class InMemoryDownloadPreferencesStore : DownloadPreferencesStore {
    private var data: DownloadStoreData? = null
    override fun load(): DownloadStoreData? = data
    override fun save(data: DownloadStoreData) {
        this.data = data.copy()
    }
}

/**
 * Holds the download preferences (§23): every mutation validated and
 * persisted through the seam; corrupt stored data recovers to
 * defaults with the repair persisted and reported (ADR-029/030
 * recovery rule).
 */
class DownloadSettings(
    private val store: DownloadPreferencesStore = InMemoryDownloadPreferencesStore(),
) {

    private var prefs: DownloadPreferences
    private var recovery: DownloadPrefsError? = null

    init {
        when (val stored = store.load()) {
            null -> {
                prefs = DownloadPreferences()
                persist()
            }
            else -> {
                if (!isValidFolder(stored.downloadFolder)) {
                    recovery = DownloadPrefsError.CorruptStoredData(stored.downloadFolder)
                    prefs = DownloadPreferences()
                    persist()
                } else {
                    prefs = DownloadPreferences(
                        askBeforeDownload = stored.askBeforeDownload,
                        downloadFolder = stored.downloadFolder,
                    )
                }
            }
        }
    }

    fun current(): DownloadPreferences = prefs

    fun lastRecovery(): DownloadPrefsError? = recovery

    fun setAskBeforeDownload(ask: Boolean): DownloadPreferences {
        prefs = prefs.copy(askBeforeDownload = ask)
        persist()
        return prefs
    }

    /**
     * Sets the default download folder: null restores the platform's
     * public Downloads directory; a non-blank user-picked reference is
     * stored as given. A blank value is rejected (state unchanged).
     */
    fun setDownloadFolder(folder: String?): DownloadPrefsResult<DownloadPreferences> {
        if (!isValidFolder(folder)) {
            return DownloadPrefsResult.Err(DownloadPrefsError.BlankFolder)
        }
        prefs = prefs.copy(downloadFolder = folder?.trim())
        persist()
        return DownloadPrefsResult.Ok(prefs)
    }

    /** Restores the defaults: ask before every download, system folder. */
    fun reset(): DownloadPreferences {
        prefs = DownloadPreferences()
        persist()
        return prefs
    }

    companion object {
        /** null (system default) or any non-blank reference. */
        fun isValidFolder(folder: String?): Boolean = folder == null || folder.isNotBlank()
    }

    private fun persist() = store.save(
        DownloadStoreData(
            askBeforeDownload = prefs.askBeforeDownload,
            downloadFolder = prefs.downloadFolder,
        )
    )
}
