package com.inweb.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import com.inweb.browser.privacy.lists.FileFilterListCache
import com.inweb.browser.settings.SharedPreferencesNotificationStore
import com.inweb.browser.settings.SharedPreferencesSettingsStore
import com.inweb.browser.settings.SharedPreferencesToolbarStore
import com.inweb.browser.settings.SharedPreferencesZoomPreferencesStore
import com.inweb.browser.session.FileSessionPersistence
import com.inweb.browser.shell.FileBookmarkStore
import com.inweb.browser.shell.FileHistoryStore
import com.inweb.browser.shell.ThemeMode
import java.io.File
import com.inweb.browser.ui.BrowserScreen
import com.inweb.browser.ui.OnboardingScreen
import com.inweb.browser.ui.theme.iNWEBTheme

/**
 * Single-activity browser shell (MASTER-SPEC §37: Material 3, adaptive layouts).
 *
 * Owns the shell lifecycle: theme from persisted settings, crash-safe
 * session restore on start and session snapshot on stop (§51). The Chromium
 * content surface binds through the engine adapter
 * (docs/PHASE2-INTEGRATION-PLAN.md).
 */
class MainActivity : ComponentActivity() {

    private val viewModel by lazy {
        BrowserViewModel(
            settingsStore = SharedPreferencesSettingsStore(this),
            sessionPersistence = FileSessionPersistence(this),
            historyStore = FileHistoryStore(File(filesDir, "history.tsv")),
            bookmarkStore = FileBookmarkStore(File(filesDir, "bookmarks.tsv")),
            toolbarStore = SharedPreferencesToolbarStore(this),
            notificationStore = SharedPreferencesNotificationStore(this),
            zoomStore = SharedPreferencesZoomPreferencesStore(this),
            filterListCache = FileFilterListCache(File(filesDir, "filter-lists")),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkTheme = when (viewModel.settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            iNWEBTheme(darkTheme = darkTheme) {
                if (viewModel.needsOnboarding) {
                    OnboardingScreen(viewModel)
                } else {
                    BrowserScreen(viewModel)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Crash-safe session persistence: snapshot whenever we leave the
        // foreground (MASTER-SPEC §51).
        viewModel.persistSession()
    }
}
