package com.inweb.browser.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomPreferencesTest {

    private fun ok(result: ZoomResult<ZoomPreferences>): ZoomPreferences =
        (result as ZoomResult.Ok).value

    private fun seeded(
        defaultFactor: Double = 1.0,
        siteZooms: Map<String, Double> = emptyMap(),
    ): ZoomSettings {
        val store = InMemoryZoomPreferencesStore()
        store.save(ZoomStoreData(defaultFactor = defaultFactor, siteZooms = siteZooms))
        return ZoomSettings(store)
    }

    // --- model -----------------------------------------------------------------

    @Test
    fun defaultsAreIdentityZoomWithNoSiteOverrides() {
        val prefs = ZoomPreferences()
        assertEquals(1.0, prefs.defaultFactor, 0.0)
        assertTrue(prefs.siteZooms.isEmpty())
        assertEquals(0.25, ZoomPreferences.MIN_FACTOR, 0.0)
        assertEquals(5.0, ZoomPreferences.MAX_FACTOR, 0.0)
    }

    @Test
    fun zoomForUnknownHostReturnsTheDefault() {
        assertEquals(1.0, ZoomPreferences().zoomFor("example.com"), 0.0)
        assertEquals(1.5, ZoomPreferences(defaultFactor = 1.5).zoomFor("example.com"), 0.0)
    }

    @Test
    fun hostsAreNormalizedForStorageAndLookup() {
        val prefs = ok(ZoomPreferences().setSiteZoom("  Example.COM  ", 2.0))
        assertEquals(2.0, prefs.zoomFor("example.com"), 0.0)
        assertEquals(2.0, prefs.zoomFor("EXAMPLE.com"), 0.0)
        assertTrue(prefs.siteZooms.containsKey("example.com"))
        assertFalse(prefs.siteZooms.containsKey("  Example.COM  "))
        assertTrue(prefs.hasSiteZoom("example.com"))
        assertFalse(prefs.hasSiteZoom("other.example"))
    }

    @Test
    fun setSiteZoomReplacesAnExistingOverride() {
        val prefs = ok(ZoomPreferences().setSiteZoom("example.com", 1.25))
        val updated = ok(prefs.setSiteZoom("example.com", 1.75))
        assertEquals(1.75, updated.zoomFor("example.com"), 0.0)
        assertEquals(1, updated.siteZooms.size)
    }

    @Test
    fun removeSiteZoomFallsBackToTheDefault() {
        val prefs = ok(ZoomPreferences(defaultFactor = 1.5).setSiteZoom("example.com", 2.0))
        val removed = prefs.removeSiteZoom("EXAMPLE.com")
        assertEquals(1.5, removed.zoomFor("example.com"), 0.0)
        assertTrue(removed.siteZooms.isEmpty())
    }

    @Test
    fun boundaryFactorsAreValid() {
        assertTrue(ZoomPreferences.isValidFactor(0.25))
        assertTrue(ZoomPreferences.isValidFactor(5.0))
        assertTrue(ZoomPreferences.isValidFactor(1.0))
    }

    @Test
    fun nonFiniteOrOutOfRangeFactorsAreInvalid() {
        for (bad in listOf(0.24, 5.01, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertFalse(ZoomPreferences.isValidFactor(bad))
        }
    }

    // --- operations through ZoomSettings (validated + persisted) -----------------

    @Test
    fun emptyStoreIsSeededWithDefaultsAndPersisted() {
        val store = InMemoryZoomPreferencesStore()
        val settings = ZoomSettings(store)
        assertEquals(1.0, settings.current().defaultFactor, 0.0)
        assertTrue(settings.current().siteZooms.isEmpty())
        assertEquals(ZoomStoreData(defaultFactor = 1.0, siteZooms = emptyMap()), store.load())
        assertNull(settings.lastRecovery())
    }

    @Test
    fun setSiteZoomStoresAndPersists() {
        val store = InMemoryZoomPreferencesStore()
        val settings = ZoomSettings(store)
        ok(settings.setSiteZoom("example.com", 1.5))
        assertEquals(1.5, settings.zoomFor("example.com"), 0.0)
        assertEquals(mapOf("example.com" to 1.5), store.load()?.siteZooms)
    }

    @Test
    fun setSiteZoomInvalidFactorIsRejectedAndStateUnchanged() {
        val settings = seeded(defaultFactor = 1.25, siteZooms = mapOf("a.example" to 1.5))
        val before = settings.current()
        for (bad in listOf(0.1, 5.5, Double.NaN, Double.POSITIVE_INFINITY)) {
            val result = settings.setSiteZoom("b.example", bad)
            assertTrue(result is ZoomResult.Err)
            assertEquals(ZoomError.FactorOutOfRange, (result as ZoomResult.Err).error)
        }
        assertEquals(before, settings.current())
    }

    @Test
    fun setSiteZoomBlankHostIsRejected() {
        val settings = ZoomSettings()
        for (blank in listOf("", "   ", "\t")) {
            val result = settings.setSiteZoom(blank, 1.5)
            assertEquals(ZoomError.BlankHost, (result as ZoomResult.Err).error)
        }
        assertTrue(settings.current().siteZooms.isEmpty())
    }

    @Test
    fun removeSiteZoomPersists() {
        val store = InMemoryZoomPreferencesStore()
        val settings = ZoomSettings(store)
        ok(settings.setSiteZoom("example.com", 1.5))
        settings.removeSiteZoom("example.com")
        assertTrue(store.load()?.siteZooms.isNullOrEmpty())
    }

    @Test
    fun setDefaultFactorPersists() {
        val store = InMemoryZoomPreferencesStore()
        val settings = ZoomSettings(store)
        ok(settings.setDefaultFactor(1.25))
        assertEquals(1.25, settings.zoomFor("anything.example"), 0.0)
        assertEquals(1.25, store.load()?.defaultFactor ?: 0.0, 0.0)
    }

    @Test
    fun setDefaultFactorInvalidIsRejectedAndStateUnchanged() {
        val settings = seeded(defaultFactor = 1.25)
        for (bad in listOf(0.24, 5.01, Double.NaN)) {
            val result = settings.setDefaultFactor(bad)
            assertTrue(result is ZoomResult.Err)
        }
        assertEquals(1.25, settings.current().defaultFactor, 0.0)
    }

    @Test
    fun clearSiteZoomsRemovesAllOverridesButKeepsTheDefault() {
        val store = InMemoryZoomPreferencesStore()
        val settings = ZoomSettings(store)
        ok(settings.setDefaultFactor(1.5))
        ok(settings.setSiteZoom("a.example", 2.0))
        ok(settings.setSiteZoom("b.example", 0.5))
        val cleared = settings.clearSiteZooms()
        assertTrue(cleared.siteZooms.isEmpty())
        assertEquals(1.5, cleared.defaultFactor, 0.0)
        assertEquals(1.5, settings.zoomFor("a.example"), 0.0)
        assertTrue(store.load()?.siteZooms.isNullOrEmpty())
    }

    @Test
    fun resetRestoresIdentityDefaults() {
        val settings = seeded(defaultFactor = 2.0, siteZooms = mapOf("a.example" to 0.5))
        val reset = settings.reset()
        assertEquals(1.0, reset.defaultFactor, 0.0)
        assertTrue(reset.siteZooms.isEmpty())
        assertEquals(1.0, settings.zoomFor("a.example"), 0.0)
    }

    // --- stored data: load, corrupt recovery ------------------------------------

    @Test
    fun validStoredDataLoadsAsIs() {
        val settings = seeded(defaultFactor = 1.5, siteZooms = mapOf("example.com" to 2.0))
        assertEquals(1.5, settings.current().defaultFactor, 0.0)
        assertEquals(2.0, settings.zoomFor("example.com"), 0.0)
        assertNull(settings.lastRecovery())
    }

    @Test
    fun corruptDefaultFactorRecoversToDefaultsAndPersistsTheRepair() {
        val store = InMemoryZoomPreferencesStore()
        store.save(ZoomStoreData(defaultFactor = 9.0, siteZooms = emptyMap()))
        val settings = ZoomSettings(store)
        val recovery = settings.lastRecovery() as ZoomError.DefaultFactorOutOfRange
        assertEquals(9.0, recovery.factor, 0.0)
        assertEquals(1.0, settings.current().defaultFactor, 0.0)
        assertEquals(ZoomStoreData(defaultFactor = 1.0, siteZooms = emptyMap()), store.load())
    }

    @Test
    fun corruptSiteFactorsReportEveryOffenderAndRecover() {
        val store = InMemoryZoomPreferencesStore()
        store.save(
            ZoomStoreData(
                defaultFactor = 1.0,
                siteZooms = mapOf("a.example" to 9.0, "b.example" to 1.0, "c.example" to 0.0),
            )
        )
        val settings = ZoomSettings(store)
        val recovery = settings.lastRecovery() as ZoomError.SiteFactorsOutOfRange
        assertEquals(setOf("a.example", "c.example"), recovery.hosts.toSet())
        assertTrue(settings.current().siteZooms.isEmpty())
    }

    @Test
    fun blankStoredHostsRecoverToDefaults() {
        val store = InMemoryZoomPreferencesStore()
        store.save(ZoomStoreData(defaultFactor = 1.0, siteZooms = mapOf("  " to 1.5)))
        val settings = ZoomSettings(store)
        val recovery = settings.lastRecovery() as ZoomError.BlankHosts
        assertEquals(listOf("  "), recovery.hosts)
        assertTrue(settings.current().siteZooms.isEmpty())
    }

    @Test
    fun hostsCollidingAfterNormalizationAreAmbiguousAndRecover() {
        val store = InMemoryZoomPreferencesStore()
        store.save(
            ZoomStoreData(
                defaultFactor = 1.0,
                siteZooms = mapOf("Example.com" to 1.5, "example.com " to 2.0),
            )
        )
        val settings = ZoomSettings(store)
        val recovery = settings.lastRecovery() as ZoomError.AmbiguousHosts
        assertEquals(listOf("example.com"), recovery.hosts)
        assertTrue(settings.current().siteZooms.isEmpty())
    }

    // --- store seam --------------------------------------------------------------

    @Test
    fun inMemoryStoreRoundTripsTheWireForm() {
        val store = InMemoryZoomPreferencesStore()
        assertNull(store.load())
        val data = ZoomStoreData(defaultFactor = 1.25, siteZooms = mapOf("a.example" to 2.0))
        store.save(data)
        assertEquals(data, store.load())
    }
}
