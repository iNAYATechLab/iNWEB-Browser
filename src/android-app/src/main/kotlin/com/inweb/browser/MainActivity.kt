package com.inweb.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.inweb.browser.ui.BrowserScreen
import com.inweb.browser.ui.theme.iNWEBTheme

/**
 * Single-activity browser shell (MASTER-SPEC §37: Material 3, adaptive layouts).
 *
 * The Chromium content surface binds through the engine adapter
 * (docs/PHASE2-INTEGRATION-PLAN.md); this activity owns only the shell UI.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            iNWEBTheme {
                BrowserScreen()
            }
        }
    }
}
