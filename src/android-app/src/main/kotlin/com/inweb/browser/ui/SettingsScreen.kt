package com.inweb.browser.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.inweb.browser.BrowserViewModel
import com.inweb.browser.R
import com.inweb.browser.notifications.NotificationChannel
import com.inweb.browser.shell.SearchEngine
import com.inweb.browser.shell.ThemeMode

/**
 * Settings surface (MASTER-SPEC §39). Every control has real backing
 * behavior: engine choice feeds the omnibox parser; theme choice feeds
 * iNWEBTheme; both persist through SettingsStore. The toolbar row opens
 * the §23 customization surface; notification toggles write through the
 * §33 notification-policy core (available channels only — no stubs).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: BrowserViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_settings)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeOverlay() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                text = stringResource(R.string.settings_search_engine),
                style = MaterialTheme.typography.titleMedium,
            )
            for (engine in SearchEngine.DEFAULTS) {
                SelectableRow(
                    label = engine.name,
                    selected = viewModel.settings.searchEngineId == engine.id,
                    onSelect = { viewModel.updateSearchEngine(engine.id) },
                )
            }

            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp),
            )
            for (mode in listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)) {
                SelectableRow(
                    label = themeLabel(mode),
                    selected = viewModel.settings.theme == mode,
                    onSelect = { viewModel.updateTheme(mode) },
                )
            }

            // --- Toolbar (§23) ----------------------------------------------
            SettingsNavRow(
                label = stringResource(R.string.settings_customize_toolbar),
                onClick = { viewModel.openCustomizeToolbar() },
                modifier = Modifier.padding(top = 24.dp),
            )

            // --- Clear browsing data (§39, Phase 12 core) ---------------------
            SettingsNavRow(
                label = stringResource(R.string.settings_clear_data),
                onClick = { viewModel.openClearData() },
                modifier = Modifier.padding(top = 24.dp),
            )

            // --- Notifications (§33) -----------------------------------------
            Text(
                text = stringResource(R.string.settings_notifications),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = stringResource(R.string.notif_policy_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            for ((channel, enabled) in viewModel.notificationChannels) {
                NotificationChannelRow(
                    label = notificationChannelLabel(channel),
                    checked = enabled,
                    onCheckedChange = { viewModel.setNotificationChannelEnabled(channel, it) },
                )
            }
        }
    }
}

@Composable
private fun notificationChannelLabel(channel: NotificationChannel): String = when (channel) {
    NotificationChannel.DOWNLOADS -> stringResource(R.string.notif_channel_downloads)
    NotificationChannel.SECURITY -> stringResource(R.string.notif_channel_security)
    NotificationChannel.VPN -> stringResource(R.string.notif_channel_vpn)
    NotificationChannel.BACKGROUND -> stringResource(R.string.notif_channel_background)
}

/** A settings entry that opens another surface; the text is the label. */
@Composable
private fun SettingsNavRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null, // decorative; the row text labels the action
        )
    }
}

@Composable
private fun NotificationChannelRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
}

@Composable
private fun SelectableRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
