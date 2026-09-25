package com.inweb.browser.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.inweb.browser.BrowserViewModel
import com.inweb.browser.R
import com.inweb.browser.cleardata.ClearDataItem
import com.inweb.browser.cleardata.ClearDataPreview

/**
 * Clear-browsing-data surface (MASTER-SPEC §39, bound to the clear-data
 * core): every row IS a core item with its REAL dry-run count; the
 * filter-list cache row says it re-downloads. Rows are toggleable with
 * checkbox semantics for screen readers (§49); the confirm button is
 * disabled for an empty selection (the surface prevents the core's
 * EmptySelection error).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearDataScreen(viewModel: BrowserViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_clear_data)) },
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
        Column(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.clear_data_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            for (item in viewModel.clearDataItems) {
                ClearItemRow(
                    label = clearItemLabel(item),
                    detail = clearItemDetail(item, viewModel.clearDataPreviews[item]),
                    checked = item in viewModel.clearDataSelection,
                    onToggle = { viewModel.toggleClearDataItem(item) },
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.confirmClearData() },
                enabled = viewModel.clearDataSelection.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.clear_data_action))
            }
        }
    }
}

@Composable
private fun ClearItemRow(
    label: String,
    detail: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun clearItemLabel(item: ClearDataItem): String = when (item) {
    ClearDataItem.HISTORY -> stringResource(R.string.clear_item_history)
    ClearDataItem.SESSION -> stringResource(R.string.clear_item_session)
    ClearDataItem.FILTER_LIST_CACHE -> stringResource(R.string.clear_item_filter_cache)
    ClearDataItem.OFFLINE_PAGES -> stringResource(R.string.clear_item_offline)
    ClearDataItem.SITE_ZOOM_OVERRIDES -> stringResource(R.string.clear_item_zoom)
}

@Composable
private fun clearItemDetail(item: ClearDataItem, preview: ClearDataPreview?): String {
    val count = preview?.count
    return when (item) {
        ClearDataItem.HISTORY -> stringResource(R.string.clear_preview_visits, count ?: 0)
        ClearDataItem.SESSION -> stringResource(R.string.clear_preview_tabs, count ?: 0)
        // the cache is disposable and not countable — say what happens instead
        ClearDataItem.FILTER_LIST_CACHE -> stringResource(R.string.clear_preview_cache)
        ClearDataItem.OFFLINE_PAGES -> stringResource(R.string.clear_preview_pages, count ?: 0)
        ClearDataItem.SITE_ZOOM_OVERRIDES -> stringResource(R.string.clear_preview_sites, count ?: 0)
    }
}
