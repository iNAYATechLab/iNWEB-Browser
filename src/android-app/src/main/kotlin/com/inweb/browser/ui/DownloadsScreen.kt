package com.inweb.browser.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inweb.browser.BrowserViewModel
import com.inweb.browser.R
import com.inweb.browser.shell.DownloadRecord
import com.inweb.browser.shell.DownloadState

/**
 * Downloads surface, backed strictly by real [DownloadRecord] state
 * (MASTER-SPEC §24 honesty rule: no fabricated data). Records arrive from
 * the engine adapter; until it ships, the honest empty state is shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(viewModel: BrowserViewModel) {
    val downloads = viewModel.downloads
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_downloads)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeOverlay() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_inweb_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.downloads_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(downloads, key = { it.id }) { record ->
                    DownloadRow(record)
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(record: DownloadRecord) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = record.fileName,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = record.url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = downloadStateLabel(record.state),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        record.progress?.let { progress ->
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
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
