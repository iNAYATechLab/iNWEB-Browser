package com.inweb.browser.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inweb.browser.BrowserViewModel
import com.inweb.browser.R
import com.inweb.browser.shell.BookmarkEntry

/**
 * Bookmarks surface, backed strictly by the real [BookmarkStore] data layer
 * (MASTER-SPEC §28/§31). Bookmarks are an explicit user action: the add
 * button bookmarks the currently open page; nothing is ever bookmarked
 * automatically (§57 — no fabricated data).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen(viewModel: BrowserViewModel) {
    val bookmarks = viewModel.bookmarks
    val canBookmarkCurrent = viewModel.selectedTab?.currentUrl?.let {
        it != com.inweb.browser.shell.AppSettings.DEFAULT_HOMEPAGE
    } == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_bookmarks)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeOverlay() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.addBookmarkForCurrentTab() },
                        enabled = canBookmarkCurrent,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.bookmark_add_current),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.bookmarks_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(bookmarks, key = { it.id }) { entry ->
                    BookmarkRow(
                        entry = entry,
                        onDelete = { viewModel.deleteBookmark(entry.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarkRow(entry: BookmarkEntry, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = entry.title.ifBlank { entry.url },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            entry.folder?.let { folder ->
                Text(
                    text = folder,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.bookmark_delete_entry),
            )
        }
    }
}
