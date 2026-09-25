package com.inweb.browser.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inweb.browser.R
import com.inweb.browser.home.ContinueReadingState
import com.inweb.browser.home.DailyWisdomState
import com.inweb.browser.home.HomeFailureReason
import com.inweb.browser.home.HomePageModel
import com.inweb.browser.home.HomePrivacyState
import com.inweb.browser.home.HomeSearchMode
import com.inweb.browser.home.HomeSection
import com.inweb.browser.home.PopularSite
import com.inweb.browser.home.PopularSiteCategory
import com.inweb.browser.home.PrayerName
import com.inweb.browser.home.PrayerSchedule
import com.inweb.browser.home.PrayerTimesState
import com.inweb.browser.home.QuickAccessItem
import com.inweb.browser.home.ShortcutAvailability
import com.inweb.browser.home.UtilityShortcut
import com.inweb.browser.home.UtilityShortcutId
import com.inweb.browser.home.VoiceSearchState
import com.inweb.browser.home.WisdomItem
import com.inweb.browser.shell.BookmarkEntry
import com.inweb.browser.shell.DownloadRecord
import com.inweb.browser.shell.DownloadState
import com.inweb.browser.shell.HistoryEntry
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

/**
 * Actions emitted by the Home renderer. Navigation, persistence, permissions,
 * and providers stay in the integration layer; no leaf composable receives a
 * BrowserViewModel.
 */
data class InwebHomeActions(
    val onFocusOmnibox: () -> Unit,
    val onVoiceSearch: () -> Unit,
    val onOpenPrivacyCenter: () -> Unit,
    val onOpenMenu: () -> Unit,
    val onOpenUtility: (UtilityShortcutId) -> Unit,
    val onOpenUrl: (String) -> Unit,
    val onAddQuickAccess: () -> Unit,
    val onOpenDownload: (String) -> Unit,
    val onSeeAllRecent: () -> Unit,
    val onSeeAllBookmarks: () -> Unit,
    val onSeeAllDownloads: () -> Unit,
    val onStartReading: () -> Unit,
    val onContinueReading: () -> Unit,
    val onRetryReading: () -> Unit,
    val onToggleWisdomSaved: (String) -> Unit,
    val onShareWisdom: (String) -> Unit,
    val onRetryWisdom: () -> Unit,
    val onSetupPrayerTimes: () -> Unit,
    val onRetryPrayerTimes: () -> Unit,
)

/**
 * Thin Material 3 renderer over [HomePageModel].
 *
 * This is intentionally not wired into BrowserScreen while the Lead-owned
 * app-layer injection probe is running. The current HomePage remains the safe
 * authored fallback; the Lead swaps the call site after the probe proves the
 * real Chromium/Compose target.
 */
@Composable
fun InwebHomeRenderer(
    model: HomePageModel,
    actions: InwebHomeActions,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize(),
    ) {
        val horizontalInset = if (maxWidth < 600.dp) 16.dp else 28.dp
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 840.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = horizontalInset,
                    end = horizontalInset,
                    top = 16.dp,
                    bottom = 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item(key = "home-header") {
                    HomeHeader(
                        privacy = model.privacy,
                        onOpenPrivacyCenter = actions.onOpenPrivacyCenter,
                        onOpenMenu = actions.onOpenMenu,
                    )
                }

                if (HomeSection.SEARCH in model.visibleSections) {
                    item(key = HomeSection.SEARCH.wireId) {
                        HomeSearchAffordance(
                            mode = model.searchMode,
                            voiceState = model.voiceSearchState,
                            onFocusOmnibox = actions.onFocusOmnibox,
                            onVoiceSearch = actions.onVoiceSearch,
                        )
                    }
                }

                if (HomeSection.SHORTCUTS in model.visibleSections) {
                    item(key = HomeSection.SHORTCUTS.wireId) {
                        ShortcutSection(
                            shortcuts = model.shortcuts,
                            onOpen = actions.onOpenUtility,
                        )
                    }
                }

                if (HomeSection.QUICK_ACCESS in model.visibleSections) {
                    item(key = HomeSection.QUICK_ACCESS.wireId) {
                        QuickAccessSection(
                            items = model.quickAccess,
                            onOpen = { actions.onOpenUrl(it.url) },
                            onAdd = actions.onAddQuickAccess,
                        )
                    }
                }

                if (HomeSection.POPULAR_ISLAMIC_WEBSITES in model.visibleSections) {
                    item(key = HomeSection.POPULAR_ISLAMIC_WEBSITES.wireId) {
                        PopularSitesSection(
                            sites = model.popularSites,
                            onOpen = { actions.onOpenUrl(it.url) },
                        )
                    }
                }

                if (HomeSection.CONTINUE_READING_LISTENING in model.visibleSections) {
                    item(key = HomeSection.CONTINUE_READING_LISTENING.wireId) {
                        ContinueReadingCard(
                            state = model.continueReading,
                            onStart = actions.onStartReading,
                            onContinue = actions.onContinueReading,
                            onRetry = actions.onRetryReading,
                        )
                    }
                }

                if (HomeSection.DAILY_WISDOM in model.visibleSections) {
                    item(key = HomeSection.DAILY_WISDOM.wireId) {
                        DailyWisdomCard(
                            state = model.dailyWisdom,
                            onToggleSaved = actions.onToggleWisdomSaved,
                            onShare = actions.onShareWisdom,
                            onRetry = actions.onRetryWisdom,
                        )
                    }
                }

                if (HomeSection.PRAYER_TIMES in model.visibleSections) {
                    item(key = HomeSection.PRAYER_TIMES.wireId) {
                        PrayerTimesCard(
                            state = model.prayerTimes,
                            onSetup = actions.onSetupPrayerTimes,
                            onRetry = actions.onRetryPrayerTimes,
                        )
                    }
                }

                val snapshot = model.browserSnapshot
                if (snapshot.recentPages.isNotEmpty()) {
                    item(key = "recent-pages") {
                        RecentPagesSection(
                            pages = snapshot.recentPages,
                            onOpen = { actions.onOpenUrl(it.url) },
                            onSeeAll = actions.onSeeAllRecent,
                        )
                    }
                }
                if (snapshot.bookmarks.isNotEmpty()) {
                    item(key = "bookmarks") {
                        BookmarksSection(
                            bookmarks = snapshot.bookmarks,
                            onOpen = { actions.onOpenUrl(it.url) },
                            onSeeAll = actions.onSeeAllBookmarks,
                        )
                    }
                }
                if (snapshot.recentDownloads.isNotEmpty()) {
                    item(key = "downloads") {
                        DownloadsSection(
                            downloads = snapshot.recentDownloads,
                            onOpen = { actions.onOpenDownload(it.id) },
                            onSeeAll = actions.onSeeAllDownloads,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    privacy: HomePrivacyState,
    onOpenPrivacyCenter: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                HomeGlyph(
                    kind = HomeGlyphKind.BRAND,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_product_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.home_product_descriptor),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PrivacyCenterChip(privacy, onOpenPrivacyCenter)
        IconButton(onClick = onOpenMenu) {
            HomeGlyph(
                kind = HomeGlyphKind.MENU,
                contentDescription = stringResource(R.string.home_open_menu),
            )
        }
    }
}

@Composable
private fun PrivacyCenterChip(
    state: HomePrivacyState,
    onClick: () -> Unit,
) {
    val status = when (state) {
        is HomePrivacyState.Active -> stringResource(R.string.home_privacy_active)
        is HomePrivacyState.Partial -> stringResource(R.string.home_privacy_partial)
        HomePrivacyState.Loading -> stringResource(R.string.home_privacy_loading)
        HomePrivacyState.Unavailable -> stringResource(R.string.home_privacy_unavailable)
    }
    val enabled = state !is HomePrivacyState.Loading
    Surface(
        modifier = Modifier
            .widthIn(max = 156.dp)
            .heightIn(min = 48.dp)
            .semantics { stateDescription = status }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state is HomePrivacyState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                HomeGlyph(
                    kind = HomeGlyphKind.PRIVACY,
                    contentDescription = null,
                    size = 18.dp,
                )
            }
            Text(
                text = stringResource(R.string.home_privacy_center),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeSearchAffordance(
    mode: HomeSearchMode,
    voiceState: VoiceSearchState,
    onFocusOmnibox: () -> Unit,
    onVoiceSearch: () -> Unit,
) {
    val hint = when (mode) {
        HomeSearchMode.WEB -> stringResource(R.string.home_search_hint_web)
        HomeSearchMode.QURAN -> stringResource(R.string.home_search_hint_quran)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onFocusOmnibox),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HomeGlyph(HomeGlyphKind.SEARCH, contentDescription = null)
            Text(
                text = hint,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (voiceState != VoiceSearchState.UNSUPPORTED) {
                val voiceLabel = voiceStateLabel(voiceState)
                IconButton(
                    onClick = onVoiceSearch,
                    modifier = Modifier.semantics { contentDescription = voiceLabel },
                ) {
                    if (voiceState == VoiceSearchState.PROCESSING) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        HomeGlyph(
                            kind = HomeGlyphKind.VOICE,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun voiceStateLabel(state: VoiceSearchState): String = when (state) {
    VoiceSearchState.AVAILABLE,
    VoiceSearchState.PERMISSION_REQUIRED,
    -> stringResource(R.string.home_voice_search)
    VoiceSearchState.LISTENING -> stringResource(R.string.home_voice_listening)
    VoiceSearchState.PROCESSING -> stringResource(R.string.home_voice_processing)
    VoiceSearchState.PERMISSION_DENIED -> stringResource(R.string.home_voice_permission_denied)
    VoiceSearchState.UNSUPPORTED -> stringResource(R.string.home_voice_unavailable)
    VoiceSearchState.ERROR -> stringResource(R.string.home_voice_error)
}

@Composable
private fun ShortcutSection(
    shortcuts: List<UtilityShortcut>,
    onOpen: (UtilityShortcutId) -> Unit,
) {
    val rendered = shortcuts.filter { it.availability != ShortcutAvailability.HIDDEN }
    if (rendered.isEmpty()) return
    HomeSectionHeader(title = stringResource(R.string.home_shortcuts))
    Spacer(Modifier.height(10.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(rendered, key = { it.id.wireId }) { shortcut ->
            val label = shortcutLabel(shortcut.id)
            val enabled = shortcut.availability == ShortcutAvailability.AVAILABLE
            HomeRailTile(
                label = label,
                enabled = enabled,
                onClick = { onOpen(shortcut.id) },
            )
        }
    }
}

@Composable
private fun QuickAccessSection(
    items: List<QuickAccessItem>,
    onOpen: (QuickAccessItem) -> Unit,
    onAdd: () -> Unit,
) {
    HomeSectionHeader(title = stringResource(R.string.home_quick_access))
    Spacer(Modifier.height(10.dp))
    if (items.isEmpty()) {
        EmptyActionCard(
            title = stringResource(R.string.home_quick_access_empty),
            action = stringResource(R.string.home_add_site),
            onClick = onAdd,
        )
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(items, key = { it.id }) { item ->
            HomeRailTile(
                label = item.title,
                supporting = item.url,
                onClick = { onOpen(item) },
            )
        }
        item(key = "quick-access-add") {
            HomeRailTile(
                label = stringResource(R.string.home_add_site),
                icon = HomeGlyphKind.ADD,
                onClick = onAdd,
            )
        }
    }
}

@Composable
private fun RecentPagesSection(
    pages: List<HistoryEntry>,
    onOpen: (HistoryEntry) -> Unit,
    onSeeAll: () -> Unit,
) {
    SnapshotSection(
        title = stringResource(R.string.home_recent_pages),
        count = pages.size,
        icon = HomeGlyphKind.RECENT,
        rows = pages.map { SnapshotRow(it.title.ifBlank { it.url }, it.url, it) },
        onOpen = onOpen,
        onSeeAll = onSeeAll,
    )
}

@Composable
private fun BookmarksSection(
    bookmarks: List<BookmarkEntry>,
    onOpen: (BookmarkEntry) -> Unit,
    onSeeAll: () -> Unit,
) {
    SnapshotSection(
        title = stringResource(R.string.action_bookmarks),
        count = bookmarks.size,
        icon = HomeGlyphKind.BOOKMARK,
        rows = bookmarks.map { SnapshotRow(it.title.ifBlank { it.url }, it.url, it) },
        onOpen = onOpen,
        onSeeAll = onSeeAll,
    )
}

@Composable
private fun DownloadsSection(
    downloads: List<DownloadRecord>,
    onOpen: (DownloadRecord) -> Unit,
    onSeeAll: () -> Unit,
) {
    SnapshotSection(
        title = stringResource(R.string.action_downloads),
        count = downloads.size,
        icon = HomeGlyphKind.DOWNLOAD,
        rows = downloads.map {
            SnapshotRow(it.fileName, downloadStateLabel(it.state), it)
        },
        onOpen = onOpen,
        onSeeAll = onSeeAll,
    )
}

private data class SnapshotRow<T>(
    val title: String,
    val supporting: String,
    val source: T,
)

@Composable
private fun <T> SnapshotSection(
    title: String,
    count: Int,
    icon: HomeGlyphKind,
    rows: List<SnapshotRow<T>>,
    onOpen: (T) -> Unit,
    onSeeAll: () -> Unit,
) {
    HomeSectionHeader(
        title = title,
        count = stringResource(R.string.home_items_shown, count),
        action = stringResource(R.string.home_see_all),
        onAction = onSeeAll,
    )
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            HomeListRow(
                icon = icon,
                title = row.title,
                supporting = row.supporting,
                onClick = { onOpen(row.source) },
            )
        }
    }
}

@Composable
private fun PopularSitesSection(
    sites: List<PopularSite>,
    onOpen: (PopularSite) -> Unit,
) {
    if (sites.isEmpty()) return
    HomeSectionHeader(title = stringResource(R.string.home_popular_islamic_websites))
    Spacer(Modifier.height(10.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(sites, key = { it.id }) { site ->
            HomeRailTile(
                label = popularSiteLabel(site.category),
                supporting = site.url,
                onClick = { onOpen(site) },
            )
        }
    }
}

@Composable
private fun ContinueReadingCard(
    state: ContinueReadingState,
    onStart: () -> Unit,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        ContinueReadingState.Unavailable -> return
        ContinueReadingState.Loading -> LoadingCard(stringResource(R.string.home_continue_reading_listening))
        ContinueReadingState.FirstUse -> HomeFeatureCard(
            eyebrow = stringResource(R.string.home_continue_reading_listening),
            title = stringResource(R.string.home_start_reading),
            action = stringResource(R.string.home_start_reading),
            icon = HomeGlyphKind.PLAY,
            onAction = onStart,
        )
        is ContinueReadingState.Resume -> HomeFeatureCard(
            eyebrow = stringResource(R.string.home_continue_reading_listening),
            title = stringResource(
                R.string.home_reading_position,
                state.position.localizedSurahName,
                state.position.ayahNumber,
            ),
            action = stringResource(R.string.home_continue),
            icon = HomeGlyphKind.PLAY,
            progress = state.position.progress.toFloat(),
            onAction = onContinue,
        )
        is ContinueReadingState.Error -> ErrorCard(
            title = stringResource(R.string.home_continue_reading_listening),
            reason = failureReasonLabel(state.reason),
            onRetry = onRetry,
        )
    }
}

@Composable
private fun DailyWisdomCard(
    state: DailyWisdomState,
    onToggleSaved: (String) -> Unit,
    onShare: (String) -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        DailyWisdomState.Unavailable -> return
        DailyWisdomState.Loading -> LoadingCard(stringResource(R.string.home_daily_wisdom))
        is DailyWisdomState.Error -> ErrorCard(
            title = stringResource(R.string.home_daily_wisdom),
            reason = failureReasonLabel(state.reason),
            onRetry = onRetry,
        )
        is DailyWisdomState.Content -> WisdomContentCard(
            item = state.item,
            onToggleSaved = onToggleSaved,
            onShare = onShare,
        )
    }
}

@Composable
private fun WisdomContentCard(
    item: WisdomItem,
    onToggleSaved: (String) -> Unit,
    onShare: (String) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HomeSectionHeader(title = stringResource(R.string.home_daily_wisdom))
            Text(text = item.body, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.home_wisdom_source, item.citation),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onToggleSaved(item.id) }) {
                    HomeGlyph(HomeGlyphKind.BOOKMARK, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(
                            if (item.saved) R.string.home_remove_saved else R.string.home_save,
                        ),
                    )
                }
                OutlinedButton(onClick = { onShare(item.id) }) {
                    HomeGlyph(HomeGlyphKind.SHARE, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_share))
                }
            }
        }
    }
}

@Composable
private fun PrayerTimesCard(
    state: PrayerTimesState,
    onSetup: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        PrayerTimesState.Unavailable -> return
        PrayerTimesState.Loading -> LoadingCard(stringResource(R.string.home_prayer_times))
        PrayerTimesState.SetupRequired -> HomeFeatureCard(
            eyebrow = stringResource(R.string.home_prayer_times),
            title = stringResource(R.string.home_prayer_setup),
            action = stringResource(R.string.home_prayer_setup_action),
            icon = HomeGlyphKind.RETRY,
            onAction = onSetup,
        )
        is PrayerTimesState.Error -> ErrorCard(
            title = stringResource(R.string.home_prayer_times),
            reason = failureReasonLabel(state.reason),
            onRetry = onRetry,
        )
        is PrayerTimesState.Schedule -> PrayerScheduleCard(state.value, state.stale)
    }
}

@Composable
private fun PrayerScheduleCard(schedule: PrayerSchedule, stale: Boolean) {
    val formatter = remember(schedule.timeZoneId) {
        DateFormat.getTimeInstance(DateFormat.SHORT).apply {
            timeZone = TimeZone.getTimeZone(schedule.timeZoneId)
        }
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HomeSectionHeader(title = stringResource(R.string.home_prayer_times))
            Text(
                text = schedule.localizedLocationName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.home_prayer_next,
                    prayerNameLabel(schedule.nextPrayer.name),
                    formatter.format(Date(schedule.nextPrayer.atEpochMillis)),
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (stale) {
                Text(
                    text = stringResource(R.string.home_prayer_stale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            schedule.prayers.forEach { prayer ->
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = prayerNameLabel(prayer.name),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = formatter.format(Date(prayer.atEpochMillis)),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Text(
                text = stringResource(
                    R.string.home_prayer_method,
                    schedule.calculationMethodId,
                    schedule.asrMethodId,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeFeatureCard(
    eyebrow: String,
    title: String,
    action: String,
    icon: HomeGlyphKind,
    onAction: () -> Unit,
    progress: Float? = null,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            progress?.let {
                LinearProgressIndicator(
                    progress = { it.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FilledTonalButton(onClick = onAction) {
                HomeGlyph(icon, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(action)
            }
        }
    }
}

@Composable
private fun LoadingCard(title: String) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ErrorCard(title: String, reason: String, onRetry: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HomeSectionHeader(title = title)
            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onRetry) {
                Text(stringResource(R.string.home_retry))
            }
        }
    }
}

@Composable
private fun EmptyActionCard(title: String, action: String, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onClick) { Text(action) }
        }
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    count: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        if (count != null) {
            Text(
                text = count,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        if (action != null && onAction != null) {
            OutlinedButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun HomeRailTile(
    label: String,
    onClick: () -> Unit,
    supporting: String? = null,
    icon: HomeGlyphKind? = null,
    enabled: Boolean = true,
) {
    Surface(
        modifier = Modifier
            .width(112.dp)
            .heightIn(min = 96.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (icon != null) {
                        HomeGlyph(icon, contentDescription = null)
                    } else {
                        Text(
                            text = label.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                    }
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeListRow(
    icon: HomeGlyphKind,
    title: String,
    supporting: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HomeGlyph(
                kind = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HomeGlyph(HomeGlyphKind.FORWARD, contentDescription = null)
        }
    }
}

@Composable
private fun shortcutLabel(id: UtilityShortcutId): String = when (id) {
    UtilityShortcutId.QURAN -> stringResource(R.string.home_shortcut_quran)
    UtilityShortcutId.HADITH -> stringResource(R.string.home_shortcut_hadith)
    UtilityShortcutId.PRAYER -> stringResource(R.string.home_shortcut_prayer)
    UtilityShortcutId.QIBLA -> stringResource(R.string.home_shortcut_qibla)
    UtilityShortcutId.DUA -> stringResource(R.string.home_shortcut_dua)
    UtilityShortcutId.HALAL_LIFE -> stringResource(R.string.home_shortcut_halal_life)
    UtilityShortcutId.NEWS -> stringResource(R.string.home_shortcut_news)
}

@Composable
private fun popularSiteLabel(category: PopularSiteCategory): String = when (category) {
    PopularSiteCategory.QURAN -> stringResource(R.string.home_popular_quran)
    PopularSiteCategory.HADITH -> stringResource(R.string.home_popular_hadith)
    PopularSiteCategory.ISLAMIC_QA -> stringResource(R.string.home_popular_qa)
    PopularSiteCategory.ARTICLES -> stringResource(R.string.home_popular_articles)
    PopularSiteCategory.HALAL_LIFE -> stringResource(R.string.home_popular_halal_life)
    PopularSiteCategory.NEWS -> stringResource(R.string.home_popular_news)
}

@Composable
private fun prayerNameLabel(name: PrayerName): String = when (name) {
    PrayerName.FAJR -> stringResource(R.string.home_prayer_fajr)
    PrayerName.DHUHR -> stringResource(R.string.home_prayer_dhuhr)
    PrayerName.ASR -> stringResource(R.string.home_prayer_asr)
    PrayerName.MAGHRIB -> stringResource(R.string.home_prayer_maghrib)
    PrayerName.ISHA -> stringResource(R.string.home_prayer_isha)
}

@Composable
private fun failureReasonLabel(reason: HomeFailureReason): String = when (reason) {
    HomeFailureReason.OFFLINE -> stringResource(R.string.home_failure_offline)
    HomeFailureReason.LOAD_FAILED -> stringResource(R.string.home_failure_load)
    HomeFailureReason.PERMISSION_REQUIRED -> stringResource(R.string.home_failure_permission)
    HomeFailureReason.UNSUPPORTED -> stringResource(R.string.home_failure_unsupported)
}

@Composable
private fun downloadStateLabel(state: DownloadState): String = when (state) {
    DownloadState.QUEUED -> stringResource(R.string.download_state_queued)
    DownloadState.RUNNING -> stringResource(R.string.download_state_running)
    DownloadState.PAUSED -> stringResource(R.string.download_state_paused)
    DownloadState.COMPLETED -> stringResource(R.string.download_state_completed)
    DownloadState.FAILED -> stringResource(R.string.download_state_failed)
    DownloadState.CANCELLED -> stringResource(R.string.download_state_cancelled)
}
