package com.inweb.browser.settings

import android.content.Context
import com.inweb.browser.shell.ZoomPreferencesStore
import com.inweb.browser.shell.ZoomStoreData

/**
 * SharedPreferences-backed zoom-preferences adapter (§23) for the app
 * layer. Deliberately THIN (ADR-031 layering): value-level validation
 * (factor bounds, blank hosts, normalization collisions) and
 * corrupt-data recovery live in the ZoomPreferences core.
 *
 * Wire form: the default factor as its string form, and per-site
 * overrides as comma-joined `host=factor` pairs. Hosts are normalized
 * (lowercase, no commas/equals by construction). An entry that does
 * not match the `host=factor` structure cannot be represented in the
 * typed wire form and is skipped by the adapter — that is a wire-format
 * boundary, not a silent value fix.
 */
class SharedPreferencesZoomPreferencesStore(context: Context) : ZoomPreferencesStore {

    private val prefs = context.getSharedPreferences("inweb_zoom", Context.MODE_PRIVATE)

    override fun load(): ZoomStoreData? {
        if (!prefs.contains(KEY_FACTOR) && !prefs.contains(KEY_SITES)) return null
        val factor = prefs.getString(KEY_FACTOR, null)?.toDoubleOrNull() ?: 1.0
        val sites = prefs.getString(KEY_SITES, "")
            ?.split(',')
            ?.mapNotNull { entry -> parseEntry(entry) }
            ?.toMap()
            ?: emptyMap()
        return ZoomStoreData(defaultFactor = factor, siteZooms = sites)
    }

    override fun save(data: ZoomStoreData) {
        prefs.edit()
            .putString(KEY_FACTOR, data.defaultFactor.toString())
            .putString(
                KEY_SITES,
                data.siteZooms.entries.joinToString(",") { "${it.key}=${it.value}" },
            )
            .apply()
    }

    private fun parseEntry(entry: String): Pair<String, Double>? {
        val separator = entry.indexOf('=')
        if (separator <= 0) return null
        val host = entry.substring(0, separator)
        val factor = entry.substring(separator + 1).toDoubleOrNull() ?: return null
        return host to factor
    }

    private companion object {
        const val KEY_FACTOR = "default_factor"
        const val KEY_SITES = "site_overrides"
    }
}
