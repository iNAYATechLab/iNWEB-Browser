package com.inweb.browser.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inweb.browser.BrowserViewModel
import com.inweb.browser.R
import com.inweb.browser.notifications.NotificationChannel
import com.inweb.browser.shell.SearchEngine
import com.inweb.browser.shell.ThemeMode

/**
 * Settings surface (MASTER-SPEC §39). Every control has real backing
 * behavior: engine choice feeds the omnibox parser; theme choice feeds
 * iNWEBTheme; both persist through SettingsStore. The toolbar and
 * page-zoom rows open the §23 customization surfaces; the downloads
 * section writes through the §23 download-preferences core (the
 * folder row launches the system SAF picker); notification toggles
 * write through the §33 notification-policy core (available channels
 * only — no stubs).
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
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_search_engine),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
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
                modifier = Modifier.padding(top = 24.dp).semantics { heading() },
            )
            for (mode in listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)) {
                SelectableRow(
                    label = themeLabel(mode),
                    selected = viewModel.settings.theme == mode,
                    onSelect = { viewModel.updateTheme(mode) },
                )
            }

            // --- Toolbar & page zoom (§23) ------------------------------------
            SettingsNavRow(
                label = stringResource(R.string.settings_customize_toolbar),
                onClick = { viewModel.openCustomizeToolbar() },
                modifier = Modifier.padding(top = 24.dp),
            )
            SettingsNavRow(
                label = stringResource(R.string.settings_page_zoom),
                onClick = { viewModel.openZoomSettings() },
            )

            // --- Clear browsing data (§39, Phase 12 core) ---------------------
            SettingsNavRow(
                label = stringResource(R.string.settings_clear_data),
                onClick = { viewModel.openClearData() },
                modifier = Modifier.padding(top = 24.dp),
            )

            // --- Downloads (§23, bound to the download-preferences core) --------
            DownloadsSection(viewModel)

            // --- Notifications (§33) -----------------------------------------
            Text(
                text = stringResource(R.string.settings_notifications),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp).semantics { heading() },
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

            // --- About (§39 — last section per the spec's ordering) -------------
            SettingsNavRow(
                label = stringResource(R.string.settings_about),
                onClick = { viewModel.openAbout() },
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

/**
 * Download preferences (§23): the ask-before-download toggle and the
 * default folder. The folder row launches the REAL system folder
 * picker (SAF OpenDocumentTree); the returned tree URI is persisted
 * through the core, and a custom folder can be released back to the
 * platform's public Downloads directory.
 */
@Composable
private fun DownloadsSection(viewModel: BrowserViewModel) {
    val context = LocalContext.current
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            // Persist the grant so the reference survives reboots. The
            // picker grants read+write persistable flags; the guard keeps
            // a refused grant from crashing — the URI reference is stored
            // either way and the download flow re-checks access.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            viewModel.setDownloadFolder(uri.toString())
        }
    }

    Text(
        text = stringResource(R.string.settings_downloads),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 24.dp).semantics { heading() },
    )
    Row(
        modifier = Modifier.fillMaxWidth()
            .toggleable(
                value = viewModel.downloadPreferences.askBeforeDownload,
                role = Role.Switch,
                onValueChange = { viewModel.setAskBeforeDownload(it) },
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.download_ask_before),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = viewModel.downloadPreferences.askBeforeDownload,
            onCheckedChange = null,
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { folderPicker.launch(null) })
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.download_folder),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = viewModel.downloadPreferences.downloadFolder
                    ?: stringResource(R.string.download_folder_system),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null, // decorative; the row text labels the action
        )
    }
    if (viewModel.downloadPreferences.downloadFolder != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { viewModel.setDownloadFolder(null) })
                .padding(vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.download_folder_reset),
                style = MaterialTheme.typography.bodyLarge,
            )
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
    // §49: the whole row is one switch target with its label; the
    // switch itself is display-only (audit finding A-4).
    Row(
        modifier = Modifier.fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = null)
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
