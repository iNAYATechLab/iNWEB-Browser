package com.inweb.browser.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePageModelTest {

    @Test
    fun canonicalOrderMatchesApprovedDesign() {
        assertEquals(
            listOf(
                HomeSection.SEARCH,
                HomeSection.SHORTCUTS,
                HomeSection.QUICK_ACCESS,
                HomeSection.POPULAR_ISLAMIC_WEBSITES,
                HomeSection.CONTINUE_READING_LISTENING,
                HomeSection.DAILY_WISDOM,
                HomeSection.PRAYER_TIMES,
            ),
            HomePageComposer.CANONICAL_SECTION_ORDER,
        )
    }

    @Test
    fun emptyTruthfulStateKeepsOnlySearchAndQuickAccess() {
        val model = compose()
        assertEquals(
            listOf(HomeSection.SEARCH, HomeSection.QUICK_ACCESS),
            model.visibleSections,
        )
    }

    @Test
    fun populatedStateUsesCanonicalOrderRegardlessOfInputMapOrder() {
        val model = compose(
            shortcutAvailability = linkedMapOf(
                UtilityShortcutId.NEWS to ShortcutAvailability.AVAILABLE,
                UtilityShortcutId.QURAN to ShortcutAvailability.AVAILABLE,
            ),
            popularSites = listOf(popularSite()),
            continueReading = ContinueReadingState.FirstUse,
            dailyWisdom = DailyWisdomState.Loading,
            prayerTimes = PrayerTimesState.SetupRequired,
        )
        assertEquals(HomePageComposer.CANONICAL_SECTION_ORDER, model.visibleSections)
        assertEquals(UtilityShortcutCatalog.CANONICAL_ORDER, model.shortcuts.map { it.id })
    }

    @Test
    fun hiddenShortcutEntriesDoNotCreateEmptySection() {
        val model = compose(
            shortcutAvailability = UtilityShortcutId.entries.associateWith {
                ShortcutAvailability.HIDDEN
            },
        )
        assertFalse(HomeSection.SHORTCUTS in model.visibleSections)
    }

    @Test
    fun disabledShortcutIsRenderedButNotClaimedAvailable() {
        val model = compose(
            shortcutAvailability = mapOf(
                UtilityShortcutId.HADITH to ShortcutAvailability.DISABLED,
            ),
        )
        assertTrue(HomeSection.SHORTCUTS in model.visibleSections)
        assertEquals(
            ShortcutAvailability.DISABLED,
            model.shortcuts.single { it.id == UtilityShortcutId.HADITH }.availability,
        )
    }

    @Test
    fun unavailableCardsAreHiddenEvenWhenConfiguredVisible() {
        val model = compose(
            continueReading = ContinueReadingState.Unavailable,
            dailyWisdom = DailyWisdomState.Unavailable,
            prayerTimes = PrayerTimesState.Unavailable,
        )
        assertFalse(HomeSection.CONTINUE_READING_LISTENING in model.visibleSections)
        assertFalse(HomeSection.DAILY_WISDOM in model.visibleSections)
        assertFalse(HomeSection.PRAYER_TIMES in model.visibleSections)
    }

    @Test
    fun userCanHideOptionalCardWithoutReorderingOthers() {
        val manager = HomeLayoutManager()
        manager.setVisible(HomeWidget.DAILY_WISDOM, false)

        val model = compose(
            layout = manager.current(),
            continueReading = ContinueReadingState.FirstUse,
            dailyWisdom = DailyWisdomState.Loading,
            prayerTimes = PrayerTimesState.SetupRequired,
        )

        assertEquals(
            listOf(
                HomeSection.SEARCH,
                HomeSection.QUICK_ACCESS,
                HomeSection.CONTINUE_READING_LISTENING,
                HomeSection.PRAYER_TIMES,
            ),
            model.visibleSections,
        )
    }

    @Test
    fun layoutParseRejectsDuplicateAndUnknownIds() {
        assertTrue(
            HomeLayoutConfig.parse(
                HomeLayoutStoreData(listOf("daily_wisdom", "daily_wisdom")),
            ) is HomeLayoutResult.Err,
        )
        assertEquals(
            HomeLayoutError.UnknownWidgets(listOf("unknown")),
            (HomeLayoutConfig.parse(HomeLayoutStoreData(listOf("unknown"))) as HomeLayoutResult.Err).error,
        )
    }

    @Test
    fun corruptLayoutFallsBackAndReturnsDefaultStoreData() {
        val manager = HomeLayoutManager(HomeLayoutStoreData(listOf("unknown")))

        assertTrue(manager.lastRecovery() is HomeLayoutError.UnknownWidgets)
        assertEquals(HomeLayoutConfig.default(), manager.current())
        assertEquals(HomeLayoutConfig.default().toStoreData(), manager.storeData())
    }

    @Test
    fun resetRestoresAllOptionalCardsAndReturnsStoreData() {
        val manager = HomeLayoutManager()
        val hidden = manager.setVisible(HomeWidget.DAILY_WISDOM, false)
        assertFalse(manager.current().isVisible(HomeWidget.DAILY_WISDOM))
        assertEquals(manager.storeData(), hidden.storeData)

        val reset = manager.reset()

        assertEquals(HomeLayoutConfig.default(), manager.current())
        assertEquals(HomeLayoutConfig.default().toStoreData(), reset.storeData)
    }

    @Test
    fun duplicatePopularSiteIdsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            compose(popularSites = listOf(popularSite(), popularSite()))
        }
    }

    private fun compose(
        shortcutAvailability: Map<UtilityShortcutId, ShortcutAvailability> = emptyMap(),
        popularSites: List<PopularSite> = emptyList(),
        continueReading: ContinueReadingState = ContinueReadingState.Unavailable,
        dailyWisdom: DailyWisdomState = DailyWisdomState.Unavailable,
        prayerTimes: PrayerTimesState = PrayerTimesState.Unavailable,
        layout: HomeLayoutConfig = HomeLayoutConfig.default(),
    ) = HomePageComposer.compose(
        privacy = HomePrivacyState.Unavailable,
        searchMode = HomeSearchMode.WEB,
        voiceSearchState = VoiceSearchState.UNSUPPORTED,
        shortcutAvailability = shortcutAvailability,
        quickAccess = emptyList(),
        popularSites = popularSites,
        continueReading = continueReading,
        dailyWisdom = dailyWisdom,
        prayerTimes = prayerTimes,
        browserSnapshot = HomeBrowserSnapshot.EMPTY,
        layout = layout,
    )

    private fun popularSite() = PopularSite(
        id = "site-1",
        category = PopularSiteCategory.QURAN,
        url = "https://example.org/quran",
        artworkId = "art-1",
        destinationName = "Example Qur'an",
        accessibilityLabel = "Open Example Qur'an",
    )

}
