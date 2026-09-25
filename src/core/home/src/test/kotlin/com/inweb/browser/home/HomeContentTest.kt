package com.inweb.browser.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeContentTest {

    @Test
    fun shortcutCatalogHasExactlyApprovedOrder() {
        assertEquals(
            listOf(
                UtilityShortcutId.QURAN,
                UtilityShortcutId.HADITH,
                UtilityShortcutId.PRAYER,
                UtilityShortcutId.QIBLA,
                UtilityShortcutId.DUA,
                UtilityShortcutId.HALAL_LIFE,
                UtilityShortcutId.NEWS,
            ),
            UtilityShortcutCatalog.CANONICAL_ORDER,
        )
        assertEquals(7, UtilityShortcutId.entries.size)
    }

    @Test
    fun missingShortcutAvailabilityDefaultsToHidden() {
        val built = UtilityShortcutCatalog.build(
            mapOf(UtilityShortcutId.QURAN to ShortcutAvailability.AVAILABLE),
        )
        assertEquals(ShortcutAvailability.AVAILABLE, built.first().availability)
        assertTrue(built.drop(1).all { it.availability == ShortcutAvailability.HIDDEN })
    }

    @Test
    fun popularSiteRequiresHttpsAddressAndProviderSuppliedNames() {
        val site = PopularSite(
            id = "quran-directory",
            category = PopularSiteCategory.QURAN,
            url = "https://example.org/quran",
            artworkId = "quran_art",
            destinationName = "Example Qur'an",
            accessibilityLabel = "Open Example Qur'an",
        )
        assertEquals("https://example.org/quran", site.url)
        assertEquals("Example Qur'an", site.destinationName)
        assertEquals("Open Example Qur'an", site.accessibilityLabel)

        assertThrows(IllegalArgumentException::class.java) {
            popularSite(url = "http://example.org")
        }
        assertThrows(IllegalArgumentException::class.java) {
            popularSite(url = "not a url")
        }
        assertThrows(IllegalArgumentException::class.java) {
            popularSite(destinationName = " ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            popularSite(accessibilityLabel = "")
        }
    }

    @Test
    fun httpsValidationDoesNotClaimCatalogApproval() {
        assertTrue(HomeWebAddresses.isHttps("https://unreviewed.example/path"))
        assertFalse(HomeWebAddresses.isHttps("http://unreviewed.example/path"))
        assertFalse(HomeWebAddresses.isHttps("not a url"))
    }

    @Test
    fun readingProgressRequiresRealIdentifiersAndBoundedProgress() {
        val valid = readingPosition()
        assertEquals(0.5, valid.progress, 0.0)

        assertThrows(IllegalArgumentException::class.java) {
            readingPosition(progress = 1.01)
        }
        assertThrows(IllegalArgumentException::class.java) {
            readingPosition(ayah = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            readingPosition(surahId = "")
        }
    }

    @Test
    fun firstUseReadingStateNeedsNoFabricatedVerse() {
        val state: ContinueReadingState = ContinueReadingState.FirstUse
        assertEquals(ContinueReadingState.FirstUse, state)
    }

    @Test
    fun wisdomRequiresBodyAndCitation() {
        val valid = wisdomItem()
        assertEquals("2:255", valid.citation)

        assertThrows(IllegalArgumentException::class.java) {
            wisdomItem(body = "")
        }
        assertThrows(IllegalArgumentException::class.java) {
            wisdomItem(citation = " ")
        }
    }

    @Test
    fun wisdomSourceMustBeHttpsWhenPresent() {
        assertThrows(IllegalArgumentException::class.java) {
            wisdomItem(sourceUrl = "http://example.org/source")
        }
        assertEquals(
            "https://example.org/source",
            wisdomItem(sourceUrl = "https://example.org/source").sourceUrl,
        )
    }

    @Test
    fun prayerScheduleRequiresEveryPrayerExactlyOnce() {
        val schedule = prayerSchedule(prayers = orderedPrayers())
        assertEquals(PrayerName.entries.toSet(), schedule.prayers.map { it.name }.toSet())

        assertThrows(IllegalArgumentException::class.java) {
            prayerSchedule(prayers = orderedPrayers().dropLast(1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            prayerSchedule(
                prayers = orderedPrayers().dropLast(1) + PrayerOccurrence(PrayerName.FAJR, 6_000),
            )
        }
    }

    @Test
    fun prayerScheduleRequiresStrictChronologicalOrder() {
        val unordered = orderedPrayers().toMutableList().apply {
            this[2] = this[2].copy(atEpochMillis = 1_500)
        }
        assertThrows(IllegalArgumentException::class.java) {
            prayerSchedule(prayers = unordered)
        }
    }

    @Test
    fun prayerScheduleCarriesCalculationMetadata() {
        val schedule = prayerSchedule(orderedPrayers())
        assertEquals("Asia/Vientiane", schedule.timeZoneId)
        assertEquals("method-id", schedule.calculationMethodId)
        assertEquals("standard", schedule.asrMethodId)
    }

    @Test
    fun prayerScheduleRejectsInvalidTimeZoneAndNonCanonicalPrayerOrder() {
        assertThrows(IllegalArgumentException::class.java) {
            prayerSchedule(orderedPrayers(), timeZoneId = "Not/A_Real_Zone")
        }
        val wrongOrder = orderedPrayers().toMutableList().apply {
            val first = this[0]
            this[0] = this[1].copy(atEpochMillis = 1_000)
            this[1] = first.copy(atEpochMillis = 2_000)
        }
        assertThrows(IllegalArgumentException::class.java) {
            prayerSchedule(wrongOrder)
        }
    }

    private fun popularSite(
        url: String = "https://example.org",
        destinationName: String = "Example",
        accessibilityLabel: String = "Open Example",
    ) = PopularSite(
        id = "example",
        category = PopularSiteCategory.NEWS,
        url = url,
        artworkId = "example_art",
        destinationName = destinationName,
        accessibilityLabel = accessibilityLabel,
    )

    private fun readingPosition(
        progress: Double = 0.5,
        ayah: Int = 10,
        surahId: String = "surah-2",
    ) = ReadingPosition(
        surahId = surahId,
        localizedSurahName = "Localized Surah Name",
        ayahNumber = ayah,
        progress = progress,
        lastReadAtMillis = 100,
        translationId = "translation-id",
        audioState = ReadingAudioState.STOPPED,
    )

    private fun wisdomItem(
        body: String = "Verified content",
        citation: String = "2:255",
        sourceUrl: String? = null,
    ) = WisdomItem(
        id = "wisdom-1",
        kind = WisdomKind.AYAH,
        body = body,
        citation = citation,
        sourceUrl = sourceUrl,
        contentDateEpochDay = 1,
        saved = false,
    )

    private fun orderedPrayers(): List<PrayerOccurrence> = listOf(
        PrayerOccurrence(PrayerName.FAJR, 1_000),
        PrayerOccurrence(PrayerName.DHUHR, 2_000),
        PrayerOccurrence(PrayerName.ASR, 3_000),
        PrayerOccurrence(PrayerName.MAGHRIB, 4_000),
        PrayerOccurrence(PrayerName.ISHA, 5_000),
    )

    private fun prayerSchedule(
        prayers: List<PrayerOccurrence>,
        timeZoneId: String = "Asia/Vientiane",
    ) = PrayerSchedule(
        locationId = "location-id",
        localizedLocationName = "Localized Place",
        timeZoneId = timeZoneId,
        calculationMethodId = "method-id",
        asrMethodId = "standard",
        prayers = prayers,
        nextPrayer = prayers.firstOrNull() ?: PrayerOccurrence(PrayerName.FAJR, 1_000),
    )
}
