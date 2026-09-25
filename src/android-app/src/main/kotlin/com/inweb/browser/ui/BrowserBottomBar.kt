package com.inweb.browser.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.inweb.browser.BrowserViewModel
import com.inweb.browser.R
import com.inweb.browser.customization.ToolbarItem

/**
 * Bottom navigation bar, rendered from the user's toolbar configuration
 * (MASTER-SPEC §23 — the customization core): item ORDER and VISIBILITY
 * come from [BrowserViewModel.toolbarVisibleItems]; mandatory items are
 * always present by core invariant. All controls carry content
 * descriptions for screen readers (MASTER-SPEC §49).
 */
@Composable
fun BrowserBottomBar(viewModel: BrowserViewModel) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (item in viewModel.toolbarVisibleItems) {
                ToolbarItemButton(item, viewModel, Modifier.weight(1f))
            }
        }
    }
}

/** One configured bar slot; the item set is the five authored controls. */
@Composable
private fun ToolbarItemButton(item: ToolbarItem, viewModel: BrowserViewModel, modifier: Modifier) {
    val tab = viewModel.selectedTab
    when (item) {
        ToolbarItem.BACK -> IconButton(
            onClick = { viewModel.goBack() },
            enabled = tab?.canGoBack == true,
            modifier = modifier,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_inweb_arrow_back),
                contentDescription = stringResource(R.string.action_back),
            )
        }
        ToolbarItem.FORWARD -> IconButton(
            onClick = { viewModel.goForward() },
            enabled = tab?.canGoForward == true,
            modifier = modifier,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_inweb_arrow_forward),
                contentDescription = stringResource(R.string.action_forward),
            )
        }
        ToolbarItem.HOME -> IconButton(
            onClick = { viewModel.openTab() },
            modifier = modifier,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_inweb_home),
                contentDescription = stringResource(R.string.action_home),
            )
        }
        ToolbarItem.TABS -> IconButton(
            onClick = { viewModel.openTabs() },
            modifier = modifier,
        ) {
            BadgedBox(
                badge = { Badge { androidx.compose.material3.Text("${viewModel.tabIds.size}") } },
            ) {
                // §49: the open-tab count is announced as the localized
                // sentence, not a bare digit (audit finding A-5).
                Icon(
                    painter = painterResource(R.drawable.ic_inweb_tab),
                    contentDescription = stringResource(
                        R.string.tabs_count,
                        viewModel.tabIds.size,
                    ),
                )
            }
        }
        ToolbarItem.MENU -> MenuButton(viewModel, modifier)
    }
}

@Composable
private fun MenuButton(viewModel: BrowserViewModel, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.ic_inweb_menu),
            contentDescription = stringResource(R.string.action_menu),
        )
    }
    androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        androidx.compose.material3.DropdownMenuItem(
            text = { androidx.compose.material3.Text(stringResource(R.string.action_new_tab)) },
            onClick = {
                expanded = false
                viewModel.openTab()
            },
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { androidx.compose.material3.Text(stringResource(R.string.action_new_private_tab)) },
            onClick = {
                expanded = false
                viewModel.openTab(isPrivate = true)
            },
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { androidx.compose.material3.Text(stringResource(R.string.action_bookmarks)) },
            onClick = {
                expanded = false
                viewModel.openBookmarks()
            },
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { androidx.compose.material3.Text(stringResource(R.string.action_history)) },
            onClick = {
                expanded = false
                viewModel.openHistory()
            },
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { androidx.compose.material3.Text(stringResource(R.string.action_downloads)) },
            onClick = {
                expanded = false
                viewModel.openDownloads()
            },
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { androidx.compose.material3.Text(stringResource(R.string.action_settings)) },
            onClick = {
                expanded = false
                viewModel.openSettings()
            },
        )
    }
}
