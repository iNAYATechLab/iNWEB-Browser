package com.inweb.browser.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inweb.browser.BrowserViewModel
import com.inweb.browser.R
import com.inweb.browser.shell.BookmarkEntry
import com.inweb.browser.shell.HistoryEntry
import com.inweb.browser.shell.TopSite

/**
 * New-tab / home experience (MASTER-SPEC §38): brand, private-search prompt,
 * and REAL data sections — shortcuts computed from actual browsing history
 * (top sites), recent pages, and bookmarks. Sections with no data are
 * simply not shown (§57: no fabricated entries).
 */
@Composable
fun HomePage(viewModel: BrowserViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.inweb_app_name),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.home_search_prompt),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HomeSections(
            shortcuts = viewModel.homeShortcuts,
            recent = viewModel.homeRecent,
            bookmarks = viewModel.homeBookmarks,
            onOpen = { url -> viewModel.submitOmniboxInput(url) },
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        )
    }
}

@Composable
private fun HomeSections(
    shortcuts: List<TopSite>,
    recent: List<HistoryEntry>,
    bookmarks: List<BookmarkEntry>,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (shortcuts.isNotEmpty()) {
            Section(stringResource(R.string.home_shortcuts)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    shortcuts.forEach { site ->
                        HomeRow(
                            title = site.title.ifBlank { site.url },
                            subtitle = site.url,
                            onClick = { onOpen(site.url) },
                        )
                    }
                }
            }
        }
        if (recent.isNotEmpty()) {
            Section(stringResource(R.string.home_recent_pages)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    recent.forEach { entry ->
                        HomeRow(
                            title = entry.title.ifBlank { entry.url },
                            subtitle = entry.url,
                            onClick = { onOpen(entry.url) },
                        )
                    }
                }
            }
        }
        if (bookmarks.isNotEmpty()) {
            Section(stringResource(R.string.action_bookmarks)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    bookmarks.forEach { entry ->
                        HomeRow(
                            title = entry.title.ifBlank { entry.url },
                            subtitle = entry.url,
                            onClick = { onOpen(entry.url) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // §49: home sections are headings for screen-reader structure
            // (audit finding A-6).
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
private fun HomeRow(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
