package com.inweb.browser.home

/** Approved Home feed order. Header privacy/menu actions are outside the feed. */
enum class HomeSection(val wireId: String) {
    SEARCH("search"),
    SHORTCUTS("shortcuts"),
    QUICK_ACCESS("quick_access"),
    POPULAR_ISLAMIC_WEBSITES("popular_islamic_websites"),
    CONTINUE_READING_LISTENING("continue_reading_listening"),
    DAILY_WISDOM("daily_wisdom"),
    PRAYER_TIMES("prayer_times"),
}

/** Optional card visibility — structural search/shortcut/site sections are not widgets. */
enum class HomeWidget(val wireId: String, val section: HomeSection) {
    CONTINUE_READING_LISTENING(
        "continue_reading_listening",
        HomeSection.CONTINUE_READING_LISTENING,
    ),
    DAILY_WISDOM("daily_wisdom", HomeSection.DAILY_WISDOM),
    PRAYER_TIMES("prayer_times", HomeSection.PRAYER_TIMES),
    ;

    companion object {
        fun fromId(id: String): HomeWidget? = entries.firstOrNull { it.wireId == id }
    }
}

data class HomeLayoutStoreData(
    /** Visible optional widget ids in canonical order. Omission means hidden. */
    val visibleWidgets: List<String>,
)

sealed class HomeLayoutError {
    data class DuplicateWidgets(val ids: List<String>) : HomeLayoutError()
    data class UnknownWidgets(val ids: List<String>) : HomeLayoutError()
}

sealed class HomeLayoutResult<out T> {
    data class Ok<T>(val value: T) : HomeLayoutResult<T>()
    data class Err(val error: HomeLayoutError) : HomeLayoutResult<Nothing>()
}

/** Validated widget visibility. It cannot change the approved section order. */
class HomeLayoutConfig private constructor(
    visibleWidgets: Set<HomeWidget>,
) {
    val visibleWidgets: Set<HomeWidget> = visibleWidgets.toSet()

    fun isVisible(widget: HomeWidget): Boolean = widget in visibleWidgets

    fun toStoreData(): HomeLayoutStoreData = HomeLayoutStoreData(
        HomeWidget.entries.filter { it in visibleWidgets }.map { it.wireId },
    )

    override fun equals(other: Any?): Boolean =
        other is HomeLayoutConfig && visibleWidgets == other.visibleWidgets

    override fun hashCode(): Int = visibleWidgets.hashCode()

    override fun toString(): String = "HomeLayoutConfig(visibleWidgets=$visibleWidgets)"

    companion object {
        fun default(): HomeLayoutConfig = HomeLayoutConfig(HomeWidget.entries.toSet())

        fun parse(data: HomeLayoutStoreData): HomeLayoutResult<HomeLayoutConfig> {
            val duplicates = data.visibleWidgets.groupBy { it }
                .filterValues { it.size > 1 }
                .keys.toList()
            if (duplicates.isNotEmpty()) {
                return HomeLayoutResult.Err(HomeLayoutError.DuplicateWidgets(duplicates))
            }
            val unknown = data.visibleWidgets.filter { HomeWidget.fromId(it) == null }
            if (unknown.isNotEmpty()) {
                return HomeLayoutResult.Err(HomeLayoutError.UnknownWidgets(unknown))
            }
            return HomeLayoutResult.Ok(
                HomeLayoutConfig(data.visibleWidgets.mapNotNull { HomeWidget.fromId(it) }.toSet()),
            )
        }
    }
}

data class HomeLayoutUpdate(
    val config: HomeLayoutConfig,
    /** Complete canonical snapshot for the Lead-owned durable storage. */
    val storeData: HomeLayoutStoreData,
)

/**
 * Optional-card configuration and recovery from an untrusted stored snapshot.
 * Durable I/O stays in the Lead-owned integration; every mutation returns the
 * complete wire value that must be committed before save success is reported.
 */
class HomeLayoutManager(
    stored: HomeLayoutStoreData? = null,
) {
    private var config: HomeLayoutConfig
    private var recovery: HomeLayoutError? = null

    init {
        config = when (stored) {
            null -> HomeLayoutConfig.default()
            else -> when (val parsed = HomeLayoutConfig.parse(stored)) {
                is HomeLayoutResult.Ok -> parsed.value
                is HomeLayoutResult.Err -> {
                    recovery = parsed.error
                    HomeLayoutConfig.default()
                }
            }
        }
    }

    fun current(): HomeLayoutConfig = config

    fun storeData(): HomeLayoutStoreData = config.toStoreData()

    fun lastRecovery(): HomeLayoutError? = recovery

    fun setVisible(widget: HomeWidget, visible: Boolean): HomeLayoutUpdate {
        val updated = config.visibleWidgets.toMutableSet()
        if (visible) updated += widget else updated -= widget
        config = when (val parsed = HomeLayoutConfig.parse(
            HomeLayoutStoreData(HomeWidget.entries.filter { it in updated }.map { it.wireId }),
        )) {
            is HomeLayoutResult.Ok -> parsed.value
            is HomeLayoutResult.Err -> error("internally generated Home layout was invalid: ${parsed.error}")
        }
        return update()
    }

    fun reset(): HomeLayoutUpdate {
        config = HomeLayoutConfig.default()
        return update()
    }

    private fun update(): HomeLayoutUpdate = HomeLayoutUpdate(config, storeData())
}

/** Complete immutable input for the thin Home renderer. */
data class HomePageModel(
    val privacy: HomePrivacyState,
    val searchMode: HomeSearchMode,
    val voiceSearchState: VoiceSearchState,
    val shortcuts: List<UtilityShortcut>,
    val quickAccess: List<QuickAccessItem>,
    val popularSites: List<PopularSite>,
    val continueReading: ContinueReadingState,
    val dailyWisdom: DailyWisdomState,
    val prayerTimes: PrayerTimesState,
    /** Real §38 source data retained without silently inventing extra sections. */
    val browserSnapshot: HomeBrowserSnapshot,
    val visibleSections: List<HomeSection>,
)

object HomePageComposer {
    val CANONICAL_SECTION_ORDER: List<HomeSection> = listOf(
        HomeSection.SEARCH,
        HomeSection.SHORTCUTS,
        HomeSection.QUICK_ACCESS,
        HomeSection.POPULAR_ISLAMIC_WEBSITES,
        HomeSection.CONTINUE_READING_LISTENING,
        HomeSection.DAILY_WISDOM,
        HomeSection.PRAYER_TIMES,
    )

    fun compose(
        privacy: HomePrivacyState,
        searchMode: HomeSearchMode,
        voiceSearchState: VoiceSearchState,
        shortcutAvailability: Map<UtilityShortcutId, ShortcutAvailability>,
        quickAccess: List<QuickAccessItem>,
        popularSites: List<PopularSite>,
        continueReading: ContinueReadingState,
        dailyWisdom: DailyWisdomState,
        prayerTimes: PrayerTimesState,
        browserSnapshot: HomeBrowserSnapshot,
        layout: HomeLayoutConfig,
    ): HomePageModel {
        require(popularSites.map { it.id }.distinct().size == popularSites.size) {
            "popular-site ids must be unique"
        }
        val shortcuts = UtilityShortcutCatalog.build(shortcutAvailability)
        val availableSections = buildSet {
            add(HomeSection.SEARCH)
            if (shortcuts.any { it.availability != ShortcutAvailability.HIDDEN }) {
                add(HomeSection.SHORTCUTS)
            }
            // Quick Access stays visible in the empty state so users can add the first site.
            add(HomeSection.QUICK_ACCESS)
            if (popularSites.isNotEmpty()) add(HomeSection.POPULAR_ISLAMIC_WEBSITES)
            if (
                layout.isVisible(HomeWidget.CONTINUE_READING_LISTENING) &&
                continueReading !is ContinueReadingState.Unavailable
            ) {
                add(HomeSection.CONTINUE_READING_LISTENING)
            }
            if (
                layout.isVisible(HomeWidget.DAILY_WISDOM) &&
                dailyWisdom !is DailyWisdomState.Unavailable
            ) {
                add(HomeSection.DAILY_WISDOM)
            }
            if (
                layout.isVisible(HomeWidget.PRAYER_TIMES) &&
                prayerTimes !is PrayerTimesState.Unavailable
            ) {
                add(HomeSection.PRAYER_TIMES)
            }
        }

        return HomePageModel(
            privacy = privacy,
            searchMode = searchMode,
            voiceSearchState = voiceSearchState,
            shortcuts = shortcuts,
            quickAccess = quickAccess.toList(),
            popularSites = popularSites.toList(),
            continueReading = continueReading,
            dailyWisdom = dailyWisdom,
            prayerTimes = prayerTimes,
            browserSnapshot = browserSnapshot,
            visibleSections = CANONICAL_SECTION_ORDER.filter { it in availableSections },
        )
    }
}
