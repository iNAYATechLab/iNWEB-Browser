package com.inweb.browser.shell

/** UI theme preference (MASTER-SPEC §22). */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Shell-level application settings. Settings grow phase by phase, always
 * together with their real backing behavior (MASTER-SPEC §10: no setting
 * without behavior).
 */
data class AppSettings(
    val searchEngineId: String = SearchEngine.DUCK_DUCK_GO.id,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val homepageUrl: String = DEFAULT_HOMEPAGE,
    /** First-run onboarding (§35) completed? Fresh installs start false. */
    val onboardingCompleted: Boolean = false,
) {
    /** Resolves the configured engine, falling back to the privacy default. */
    val searchEngine: SearchEngine
        get() = SearchEngine.byId(searchEngineId) ?: SearchEngine.DUCK_DUCK_GO

    companion object {
        /** Internal home URL the UI recognizes (no network involved). */
        const val DEFAULT_HOMEPAGE = "inweb://home"
    }
}

/**
 * Persistence port for settings. The Android adapter (DataStore/Keystore-
 * aware) is delivered with the app build; tests and previews use the
 * in-memory implementation.
 */
interface SettingsStore {
    fun load(): AppSettings
    fun save(settings: AppSettings)
}

/** In-memory implementation for tests and engine-less UI previews. */
class InMemorySettingsStore(initial: AppSettings = AppSettings()) : SettingsStore {
    private var current: AppSettings = initial
    override fun load(): AppSettings = current
    override fun save(settings: AppSettings) {
        current = settings
    }
}
