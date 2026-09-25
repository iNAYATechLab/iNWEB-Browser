package com.inweb.browser.home

import java.time.ZoneId

/** Stable identity of each approved curated utility shortcut. */
enum class UtilityShortcutId(val wireId: String) {
    QURAN("quran"),
    HADITH("hadith"),
    PRAYER("prayer"),
    QIBLA("qibla"),
    DUA("dua"),
    HALAL_LIFE("halal_life"),
    NEWS("news"),
}

/** Whether an integration may expose and activate a shortcut. */
enum class ShortcutAvailability {
    AVAILABLE,
    DISABLED,
    HIDDEN,
}

data class UtilityShortcut(
    val id: UtilityShortcutId,
    val availability: ShortcutAvailability,
)

/** The seven-item product order. There is deliberately no Add item. */
object UtilityShortcutCatalog {
    val CANONICAL_ORDER: List<UtilityShortcutId> = listOf(
        UtilityShortcutId.QURAN,
        UtilityShortcutId.HADITH,
        UtilityShortcutId.PRAYER,
        UtilityShortcutId.QIBLA,
        UtilityShortcutId.DUA,
        UtilityShortcutId.HALAL_LIFE,
        UtilityShortcutId.NEWS,
    )

    fun build(availability: Map<UtilityShortcutId, ShortcutAvailability>): List<UtilityShortcut> =
        CANONICAL_ORDER.map { id ->
            UtilityShortcut(id, availability[id] ?: ShortcutAvailability.HIDDEN)
        }
}

/** Stable localized category identity; the renderer owns localized labels. */
enum class PopularSiteCategory(val wireId: String) {
    QURAN("quran"),
    HADITH("hadith"),
    ISLAMIC_QA("islamic_qa"),
    ARTICLES("articles"),
    HALAL_LIFE("halal_life"),
    NEWS("news"),
}

/**
 * One provider-supplied curated destination. No default URLs or approval list
 * live in this core; the provider admits only product-approved catalog rows.
 */
data class PopularSite(
    val id: String,
    val category: PopularSiteCategory,
    val url: String,
    val artworkId: String,
    /** Reviewed destination name, localized by the provider when appropriate. */
    val destinationName: String,
    /** Complete locale-appropriate announcement for the outbound tile. */
    val accessibilityLabel: String,
) {
    init {
        require(id.isNotBlank()) { "popular-site id must not be blank" }
        require(artworkId.isNotBlank()) { "popular-site artworkId must not be blank" }
        require(destinationName.isNotBlank()) { "popular-site destinationName must not be blank" }
        require(accessibilityLabel.isNotBlank()) { "popular-site accessibilityLabel must not be blank" }
        require(HomeWebAddresses.isHttps(url)) {
            "popular-site URL must be a valid HTTPS destination"
        }
    }
}

/** Integration-level failures; localized explanations belong to the renderer. */
enum class HomeFailureReason {
    OFFLINE,
    LOAD_FAILED,
    PERMISSION_REQUIRED,
    UNSUPPORTED,
}

enum class ReadingAudioState {
    UNAVAILABLE,
    STOPPED,
    BUFFERING,
    PLAYING,
    PAUSED,
    FAILED,
}

/** Real position supplied by a Qur'an reading store. */
data class ReadingPosition(
    val surahId: String,
    val localizedSurahName: String,
    val ayahNumber: Int,
    val progress: Double,
    val lastReadAtMillis: Long,
    val translationId: String?,
    val audioState: ReadingAudioState,
) {
    init {
        require(surahId.isNotBlank()) { "surahId must not be blank" }
        require(localizedSurahName.isNotBlank()) { "localizedSurahName must not be blank" }
        require(ayahNumber > 0) { "ayahNumber must be positive" }
        require(progress in 0.0..1.0) { "progress must be in 0..1" }
        require(lastReadAtMillis >= 0L) { "lastReadAtMillis must not be negative" }
        require(translationId == null || translationId.isNotBlank()) {
            "translationId must be null or non-blank"
        }
    }
}

sealed class ContinueReadingState {
    object FirstUse : ContinueReadingState()
    object Loading : ContinueReadingState()
    data class Resume(val position: ReadingPosition) : ContinueReadingState()
    object Unavailable : ContinueReadingState()
    data class Error(val reason: HomeFailureReason) : ContinueReadingState()
}

enum class WisdomKind {
    AYAH,
    HADITH,
}

/** Verified content data. A citation is mandatory by construction. */
data class WisdomItem(
    val id: String,
    val kind: WisdomKind,
    val body: String,
    val citation: String,
    val sourceUrl: String?,
    val contentDateEpochDay: Long,
    val saved: Boolean,
) {
    init {
        require(id.isNotBlank()) { "wisdom id must not be blank" }
        require(body.isNotBlank()) { "wisdom body must not be blank" }
        require(citation.isNotBlank()) { "wisdom citation must not be blank" }
        require(sourceUrl == null || HomeWebAddresses.isHttps(sourceUrl)) {
            "wisdom sourceUrl must be null or valid HTTPS"
        }
    }
}

sealed class DailyWisdomState {
    object Loading : DailyWisdomState()
    data class Content(val item: WisdomItem, val cached: Boolean) : DailyWisdomState()
    object Unavailable : DailyWisdomState()
    data class Error(val reason: HomeFailureReason) : DailyWisdomState()
}

enum class PrayerName(val wireId: String) {
    FAJR("fajr"),
    DHUHR("dhuhr"),
    ASR("asr"),
    MAGHRIB("maghrib"),
    ISHA("isha"),
}

data class PrayerOccurrence(
    val name: PrayerName,
    val atEpochMillis: Long,
) {
    init {
        require(atEpochMillis >= 0L) { "prayer occurrence time must not be negative" }
    }
}

/** A real calculated schedule with enough metadata to explain its result. */
data class PrayerSchedule(
    val locationId: String,
    val localizedLocationName: String,
    val timeZoneId: String,
    val calculationMethodId: String,
    val asrMethodId: String,
    val prayers: List<PrayerOccurrence>,
    val nextPrayer: PrayerOccurrence,
) {
    init {
        require(locationId.isNotBlank()) { "locationId must not be blank" }
        require(localizedLocationName.isNotBlank()) { "localizedLocationName must not be blank" }
        require(timeZoneId.isNotBlank()) { "timeZoneId must not be blank" }
        require(runCatching { ZoneId.of(timeZoneId) }.isSuccess) {
            "timeZoneId must be a valid zone"
        }
        require(calculationMethodId.isNotBlank()) { "calculationMethodId must not be blank" }
        require(asrMethodId.isNotBlank()) { "asrMethodId must not be blank" }
        require(prayers.map { it.name } == PrayerName.entries) {
            "prayers must contain each required prayer exactly once in canonical order"
        }
        require(prayers.zipWithNext().all { (first, second) ->
            first.atEpochMillis < second.atEpochMillis
        }) { "prayers must be in strictly increasing time order" }
    }
}

sealed class PrayerTimesState {
    object SetupRequired : PrayerTimesState()
    object Loading : PrayerTimesState()
    data class Schedule(val value: PrayerSchedule, val stale: Boolean) : PrayerTimesState()
    object Unavailable : PrayerTimesState()
    data class Error(val reason: HomeFailureReason) : PrayerTimesState()
}
