package com.inweb.browser.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.inweb.browser.BrowserViewModel
import com.inweb.browser.R
import com.inweb.browser.shell.TabLifecycle

/**
 * Omnibox: single-line input with security indicator and reload/stop control.
 * Submits through the core OmniboxParser (URL vs search decision).
 */
@Composable
fun OmniboxBar(viewModel: BrowserViewModel) {
    val tab = viewModel.selectedTab
    var text by remember(tab?.id) { mutableStateOf(tab?.currentUrl.orEmpty()) }

    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = androidx.compose.ui.unit.dp(3)) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = androidx.compose.ui.unit.dp(8)),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = stringResource(R.string.omnibox_security),
                tint = MaterialTheme.colorScheme.primary,
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { androidx.compose.material3.Text(stringResource(R.string.omnibox_hint)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(
                    onGo = { viewModel.submitOmniboxInput(text) },
                ),
            )
            if (tab?.lifecycle == TabLifecycle.LOADING) {
                IconButton(onClick = { viewModel.stop() }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.omnibox_stop),
                    )
                }
            } else {
                IconButton(onClick = { viewModel.reload() }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.omnibox_reload),
                    )
                }
            }
        }
    }
}
