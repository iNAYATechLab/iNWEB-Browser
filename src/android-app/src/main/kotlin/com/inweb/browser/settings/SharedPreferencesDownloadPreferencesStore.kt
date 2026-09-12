package com.inweb.browser.settings

import android.content.Context
import com.inweb.browser.shell.DownloadPreferencesStore
import com.inweb.browser.shell.DownloadStoreData

/**
 * SharedPreferences-backed download-preferences adapter (§23) for the
 * app layer. Deliberately THIN (ADR-031 layering): value-level
 * validation (blank-folder rejection) and corrupt-data recovery live
 * in the DownloadPreferences core.
 *
 * Wire form: the ask-before-download flag as a boolean, and the
 * default download folder as its string form — the platform's public
 * Downloads directory by default (key absent = null), or the SAF tree
 * URI the system folder picker returned. A null folder is stored by
 * removing the key, so "absent" and "system default" remain a single
 * state on the wire.
 */
class SharedPreferencesDownloadPreferencesStore(context: Context) : DownloadPreferencesStore {

    private val prefs = context.getSharedPreferences("inweb_downloads", Context.MODE_PRIVATE)

    override fun load(): DownloadStoreData? {
        if (!prefs.contains(KEY_ASK) && !prefs.contains(KEY_FOLDER)) return null
        return DownloadStoreData(
            askBeforeDownload = prefs.getBoolean(KEY_ASK, true),
            downloadFolder = prefs.getString(KEY_FOLDER, null),
        )
    }

    override fun save(data: DownloadStoreData) {
        prefs.edit()
            .putBoolean(KEY_ASK, data.askBeforeDownload)
            .putString(KEY_FOLDER, data.downloadFolder)
            .apply()
    }

    private companion object {
        const val KEY_ASK = "ask_before_download"
        const val KEY_FOLDER = "download_folder"
    }
}
