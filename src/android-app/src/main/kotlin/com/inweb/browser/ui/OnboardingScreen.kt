package com.inweb.browser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inweb.browser.BrowserViewModel
import com.inweb.browser.R
import com.inweb.browser.shell.SearchEngine

/**
 * First-run onboarding (MASTER-SPEC §35): privacy education and the default
 * search-engine choice. Two short steps — never overwhelming. The engine
 * choice is persisted through the REAL SettingsStore (the same setting the
 * settings screen uses); completing the flow is persisted too, so it runs
 * exactly once.
 */
@Composable
fun OnboardingScreen(viewModel: BrowserViewModel) {
    var step by remember { mutableStateOf(0) }
    var selectedEngineId by remember { mutableStateOf(viewModel.settings.searchEngineId) }
    val engines = SearchEngine.DEFAULTS

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        when (step) {
            0 -> WelcomeStep()
            1 -> EngineChoiceStep(
                engines = engines,
                selectedEngineId = selectedEngineId,
                onSelect = { selectedEngineId = it },
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = {
                    if (step > 0) {
                        step -= 1
                    } else {
                        // Skip: complete with the privacy-preserving default.
                        viewModel.completeOnboarding(viewModel.settings.searchEngineId)
                    }
                },
            ) {
                Text(
                    text = if (step > 0) {
                        stringResource(R.string.action_back)
                    } else {
                        stringResource(R.string.onboarding_skip)
                    },
                )
            }
            Text(
                text = stringResource(R.string.onboarding_step, step + 1, 2),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    if (step == 0) {
                        step = 1
                    } else {
                        viewModel.completeOnboarding(selectedEngineId)
                    }
                },
            ) {
                Text(
                    text = if (step == 0) {
                        stringResource(R.string.onboarding_next)
                    } else {
                        stringResource(R.string.onboarding_get_started)
                    },
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.onboarding_welcome_text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        BulletPoint(stringResource(R.string.onboarding_point_privacy))
        BulletPoint(stringResource(R.string.onboarding_point_private_tabs))
        BulletPoint(stringResource(R.string.onboarding_point_protection))
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun EngineChoiceStep(
    engines: List<SearchEngine>,
    selectedEngineId: String,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_search_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.onboarding_search_text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        engines.forEach { engine ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RadioButton(
                    selected = engine.id == selectedEngineId,
                    onClick = { onSelect(engine.id) },
                )
                Text(
                    text = engine.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
