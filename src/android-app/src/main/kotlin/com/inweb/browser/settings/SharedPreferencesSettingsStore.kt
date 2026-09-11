package com.inweb.browser.settings

import android.content.Context
import com.inweb.browser.shell.AppSettings
import com.inweb.browser.shell.SettingsStore
import com.inweb.browser.shell.ThemeMode

/**
 * SharedPreferences-backed settings adapter for the app layer.
 * Unknown stored values fall back to the privacy-preserving defaults
 * (handled by [AppSettings] itself).
 */
class SharedPreferencesSettingsStore(context: Context) : SettingsStore {

    private val prefs = context.getSharedPreferences("inweb_settings", Context.MODE_PRIVATE)

    override fun load(): AppSettings {
        if (!prefs.contains(KEY_ENGINE) && !prefs.contains(KEY_THEME) &&
            !prefs.contains(KEY_ONBOARDING)
        ) {
            return AppSettings()
        }
        val engineId = prefs.getString(KEY_ENGINE, null) ?: AppSettings().searchEngineId
        val theme = when (prefs.getString(KEY_THEME, null)) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
        return AppSettings(
            searchEngineId = engineId,
            theme = theme,
            onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING, false),
        )
    }

    override fun save(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_ENGINE, settings.searchEngineId)
            .putString(KEY_THEME, settings.theme.name)
            .putBoolean(KEY_ONBOARDING, settings.onboardingCompleted)
            .apply()
    }

    private companion object {
        const val KEY_ENGINE = "search_engine_id"
        const val KEY_THEME = "theme"
        const val KEY_ONBOARDING = "onboarding_completed"
    }
}
