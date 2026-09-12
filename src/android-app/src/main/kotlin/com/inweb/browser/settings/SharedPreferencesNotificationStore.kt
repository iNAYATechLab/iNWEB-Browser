package com.inweb.browser.settings

import android.content.Context
import com.inweb.browser.notifications.NotificationStore
import com.inweb.browser.notifications.NotificationStoreData

/**
 * SharedPreferences-backed notification-preferences adapter (§33) for
 * the app layer. Deliberately THIN: unknown channel ids and unknown
 * permission phases are validated and recovered by the
 * notification-policy core (ADR-030), never here.
 *
 * Wire form: comma-joined stable channel ids + the permission-phase
 * name. Absent keys = never saved = null.
 */
class SharedPreferencesNotificationStore(context: Context) : NotificationStore {

    private val prefs = context.getSharedPreferences("inweb_notifications", Context.MODE_PRIVATE)

    override fun load(): NotificationStoreData? {
        if (!prefs.contains(KEY_DISABLED) && !prefs.contains(KEY_PHASE)) return null
        val disabled = prefs.getString(KEY_DISABLED, "")
            ?.split(',')
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        val phase = prefs.getString(KEY_PHASE, null) ?: "NOT_REQUESTED"
        return NotificationStoreData(disabledChannels = disabled, permissionPhase = phase)
    }

    override fun save(data: NotificationStoreData) {
        prefs.edit()
            .putString(KEY_DISABLED, data.disabledChannels.joinToString(","))
            .putString(KEY_PHASE, data.permissionPhase)
            .apply()
    }

    private companion object {
        const val KEY_DISABLED = "disabled_channels"
        const val KEY_PHASE = "permission_phase"
    }
}
