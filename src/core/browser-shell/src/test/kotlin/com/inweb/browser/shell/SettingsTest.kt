package com.inweb.browser.shell

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsTest {

    @Test
    fun defaultsArePrivacyPreserving() {
        val settings = AppSettings()
        assertEquals("duckduckgo", settings.searchEngineId)
        assertEquals(ThemeMode.SYSTEM, settings.theme)
        assertEquals("inweb://home", settings.homepageUrl)
        assertEquals("DuckDuckGo", settings.searchEngine.name)
    }

    @Test
    fun unknownEngineFallsBackToPrivacyDefault() {
        val settings = AppSettings(searchEngineId = "does-not-exist")
        assertEquals("duckduckgo", settings.searchEngine.id)
    }

    @Test
    fun inMemoryStoreRoundTrip() {
        val store = InMemorySettingsStore()
        store.save(AppSettings(theme = ThemeMode.DARK, searchEngineId = "google"))
        val loaded = store.load()
        assertEquals(ThemeMode.DARK, loaded.theme)
        assertEquals("google", loaded.searchEngineId)
    }
}
