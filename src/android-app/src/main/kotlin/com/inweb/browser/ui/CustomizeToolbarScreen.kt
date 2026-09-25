package com.inweb.browser.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.inweb.browser.BrowserViewModel
import com.inweb.browser.R
import com.inweb.browser.customization.ToolbarEntry
import com.inweb.browser.customization.ToolbarItem

/**
 * Toolbar customization surface (MASTER-SPEC §23), bound to the
 * customization core: the list IS the user's configuration — order,
 * visibility, and the mandatory-item lock all come from (and write
 * through) the core. Every control is reachable non-visually and
 * carries a label (§49).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeToolbarScreen(viewModel: BrowserViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_customize_toolbar)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeOverlay() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_inweb_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.resetToolbar() }) {
                        Text(stringResource(R.string.toolbar_reset_to_default))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            val entries = viewModel.toolbarConfig.entries
            entries.forEachIndexed { index, entry ->
                ToolbarEntryRow(
                    entry = entry,
                    isFirst = index == 0,
                    isLast = index == entries.lastIndex,
                    onMoveUp = { viewModel.moveToolbarItem(entry.item.id, index - 1) },
                    onMoveDown = { viewModel.moveToolbarItem(entry.item.id, index + 1) },
                    onVisibilityChange = { viewModel.setToolbarItemVisible(entry.item.id, it) },
                )
            }
        }
    }
}

@Composable
private fun ToolbarEntryRow(
    entry: ToolbarEntry,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onVisibilityChange: (Boolean) -> Unit,
) {
    val visibilityLabel = stringResource(R.string.toolbar_item_visibility)
    // §49: the whole row is one switch target (mandatory items: the
    // toggle is disabled with the row); the switch itself is
    // display-only (audit finding A-4).
    Row(
        modifier = Modifier.fillMaxWidth()
            .toggleable(
                value = entry.visible,
                enabled = !entry.item.mandatory,
                role = Role.Switch,
                onValueChange = onVisibilityChange,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = toolbarItemLabel(entry.item),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (entry.item.mandatory) {
                Text(
                    text = stringResource(R.string.toolbar_item_always_visible),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onMoveUp, enabled = !isFirst) {
            Icon(
                painter = painterResource(R.drawable.ic_inweb_chevron_up),
                contentDescription = stringResource(R.string.toolbar_move_up),
            )
        }
        IconButton(onClick = onMoveDown, enabled = !isLast) {
            Icon(
                painter = painterResource(R.drawable.ic_inweb_chevron_down),
                contentDescription = stringResource(R.string.toolbar_move_down),
            )
        }
        Switch(
            checked = entry.visible,
            onCheckedChange = null,
            enabled = !entry.item.mandatory,
            modifier = Modifier.semantics { contentDescription = visibilityLabel },
        )
    }
}

@Composable
private fun toolbarItemLabel(item: ToolbarItem): String = when (item) {
    ToolbarItem.BACK -> stringResource(R.string.action_back)
    ToolbarItem.FORWARD -> stringResource(R.string.action_forward)
    ToolbarItem.HOME -> stringResource(R.string.action_home)
    ToolbarItem.TABS -> stringResource(R.string.action_tabs)
    ToolbarItem.MENU -> stringResource(R.string.action_menu)
}
