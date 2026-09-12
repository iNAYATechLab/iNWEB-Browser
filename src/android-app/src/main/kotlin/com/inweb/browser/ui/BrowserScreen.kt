package com.inweb.browser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.inweb.browser.BrowserViewModel
import com.inweb.browser.R
import com.inweb.browser.Screen
import com.inweb.browser.shell.AppSettings

/**
 * Root shell: switches between the browser scaffold and overlay surfaces
 * (settings, downloads) — MASTER-SPEC §37–§39.
 */
@Composable
fun BrowserScreen(viewModel: BrowserViewModel = remember { BrowserViewModel() }) {
    when (viewModel.screen) {
        Screen.BROWSER -> BrowserScaffold(viewModel)
        Screen.SETTINGS -> SettingsScreen(viewModel)
        Screen.DOWNLOADS -> DownloadsScreen(viewModel)
        Screen.HISTORY -> HistoryScreen(viewModel)
        Screen.BOOKMARKS -> BookmarkScreen(viewModel)
        Screen.TABS -> TabsScreen(viewModel)
        Screen.CUSTOMIZE_TOOLBAR -> CustomizeToolbarScreen(viewModel)
        Screen.CLEAR_DATA -> ClearDataScreen(viewModel)
        Screen.ZOOM_SETTINGS -> ZoomSettingsScreen(viewModel)
        Screen.ABOUT -> AboutScreen(viewModel)
    }
}

@Composable
private fun BrowserScaffold(viewModel: BrowserViewModel) {
    val tab = viewModel.selectedTab
    Scaffold(
        topBar = { OmniboxBar(viewModel) },
        bottomBar = { BrowserBottomBar(viewModel) },
    ) { padding ->
        val currentUrl = tab?.currentUrl
        if (currentUrl == null || currentUrl == AppSettings.DEFAULT_HOMEPAGE) {
            HomePage(viewModel, modifier = Modifier.padding(padding))
        } else {
            // The Chromium content surface binds here via the engine adapter
            // (ui/ patch area — docs/PHASE2-INTEGRATION-PLAN.md). Until the
            // adapter ships this is an explicit binding point, not a
            // functioning page view (MASTER-SPEC §57).
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.engine_binding_point),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
