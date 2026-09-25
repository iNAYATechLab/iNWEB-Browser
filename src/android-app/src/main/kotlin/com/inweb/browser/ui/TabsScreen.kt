package com.inweb.browser.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import com.inweb.browser.shell.TabState

/**
 * Tab switcher, backed strictly by the real [TabsController] state via the
 * view model. Selecting a tab switches back to the browser; closing the
 * last tab auto-opens a fresh one (existing controller contract).
 * Private tabs carry an explicit badge (MASTER-SPEC §13).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsScreen(viewModel: BrowserViewModel) {
    val tabs = viewModel.tabs

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_tabs)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeOverlay() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_inweb_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openTab(isPrivate = true) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_inweb_lock),
                            contentDescription = stringResource(R.string.action_new_private_tab),
                        )
                    }
                    IconButton(onClick = { viewModel.openTab() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_inweb_add),
                            contentDescription = stringResource(R.string.action_new_tab),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (tabs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.tabs_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(tabs, key = { it.id }) { tab ->
                    TabCard(
                        tab = tab,
                        isSelected = tab.id == viewModel.selectedTab?.id,
                        onClick = {
                            viewModel.selectTab(tab.id)
                            viewModel.closeOverlay()
                        },
                        onClose = { viewModel.closeTab(tab.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabCard(
    tab: TabState,
    isSelected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tab.currentUrl ?: stringResource(R.string.action_home),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                // §49 touch accessibility: full 48dp touch target (the
                // compact 28dp sizing was an audit finding, A-1).
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.ic_inweb_close),
                        contentDescription = stringResource(R.string.action_close_tab),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (tab.isPrivate) {
                Text(
                    text = stringResource(R.string.private_tab_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
