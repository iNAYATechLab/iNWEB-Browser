package com.inweb.browser.settings

import android.content.Context
import com.inweb.browser.customization.ToolbarStore
import com.inweb.browser.customization.ToolbarStoreData

/**
 * SharedPreferences-backed toolbar-configuration adapter (§23) for the
 * app layer. Deliberately THIN: it round-trips the raw wire form only —
 * duplicate/unknown/missing ids and illegal hidden states are validated
 * and recovered by the customization core (ToolbarConfig.parse,
 * ADR-029), never here.
 *
 * Wire form: comma-joined stable item ids (the id alphabet contains no
 * commas by construction). Absent keys = never saved = null.
 */
class SharedPreferencesToolbarStore(context: Context) : ToolbarStore {

    private val prefs = context.getSharedPreferences("inweb_toolbar", Context.MODE_PRIVATE)

    override fun load(): ToolbarStoreData? {
        if (!prefs.contains(KEY_ORDER)) return null
        val order = prefs.getString(KEY_ORDER, null)
            ?.split(',')
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        val hidden = prefs.getString(KEY_HIDDEN, "")
            ?.split(',')
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        return ToolbarStoreData(order = order, hidden = hidden)
    }

    override fun save(data: ToolbarStoreData) {
        prefs.edit()
            .putString(KEY_ORDER, data.order.joinToString(","))
            .putString(KEY_HIDDEN, data.hidden.joinToString(","))
            .apply()
    }

    private companion object {
        const val KEY_ORDER = "toolbar_order"
        const val KEY_HIDDEN = "toolbar_hidden"
    }
}
