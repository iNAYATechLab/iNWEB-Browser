package com.inweb.browser.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Tab
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.inweb.browser.BrowserViewModel
import com.inweb.browser.R

/**
 * Bottom navigation bar: back, forward, home, tabs (with count badge), menu.
 * All controls carry content descriptions for screen readers (MASTER-SPEC §49).
 */
@Composable
fun BrowserBottomBar(viewModel: BrowserViewModel) {
    val tab = viewModel.selectedTab
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { viewModel.goBack() },
                enabled = tab?.canGoBack == true,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            IconButton(
                onClick = { viewModel.goForward() },
                enabled = tab?.canGoForward == true,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.action_forward),
                )
            }
            IconButton(
                onClick = { viewModel.openTab() },
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = stringResource(R.string.action_home),
                )
            }
            IconButton(
                onClick = { viewModel.openTab() },
                modifier = Modifier.weight(1f),
            ) {
                BadgedBox(
                    badge = { Badge { androidx.compose.material3.Text("${viewModel.tabIds.size}") } },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tab,
                        contentDescription = stringResource(R.string.action_tabs),
                    )
                }
            }
            MenuButton(viewModel, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MenuButton(viewModel: BrowserViewModel, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.Menu,
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
