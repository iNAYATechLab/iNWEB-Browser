package com.inweb.browser.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.inweb.browser.BrowserViewModel
import com.inweb.browser.R
import com.inweb.browser.shell.ZoomPreferences
import kotlin.math.roundToInt

/**
 * Page-zoom settings surface (MASTER-SPEC §23), bound to the zoom core
 * through the view model. The default-factor list is the core's preset
 * table (upstream Chromium's zoom steps); every write is
 * bounds-validated by the core. Per-site overrides are CREATED by the
 * page-zoom control in the browser surface (engine adapter, patch
 * side); this surface manages what actually exists — no fabricated
 * entries (§57 honesty rule).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoomSettingsScreen(viewModel: BrowserViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_page_zoom)) },
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
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.zoom_default_factor),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            for (factor in ZoomPreferences.PRESET_FACTORS) {
                SelectableRow(
                    label = factorLabel(factor),
                    selected = viewModel.zoomPreferences.defaultFactor == factor,
                    onSelect = { viewModel.setZoomDefaultFactor(factor) },
                )
            }

            Text(
                text = stringResource(R.string.zoom_site_overrides),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp).semantics { heading() },
            )
            val siteZooms = viewModel.zoomPreferences.siteZooms.entries.sortedBy { it.key }
            if (siteZooms.isEmpty()) {
                Text(
                    text = stringResource(R.string.zoom_no_site_overrides),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                for ((host, factor) in siteZooms) {
                    SiteZoomRow(
                        host = host,
                        factor = factor,
                        onRemove = { viewModel.removeSiteZoom(host) },
                    )
                }
            }
        }
    }
}

/** A preset factor shown as its percentage form (0.667 -> "67%"). */
@Composable
private fun factorLabel(factor: Double): String =
    stringResource(R.string.zoom_factor_percent, (factor * 100).roundToInt())

/** One existing per-site override: the host, its factor, and removal. */
@Composable
private fun SiteZoomRow(host: String, factor: Double, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = host,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = factorLabel(factor),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(R.drawable.ic_inweb_close),
                contentDescription = stringResource(R.string.zoom_remove_site_override, host),
            )
        }
    }
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
