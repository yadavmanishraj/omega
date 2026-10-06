package com.manishraj.saavnmusic.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.manishraj.saavnmusic.ui.theme.OmegaSpacing
import kotlinx.coroutines.launch

private val QUALITIES = listOf("12kbps", "48kbps", "96kbps", "160kbps", "320kbps")

/**
 * Settings (REDESIGN_SPEC §7): grouped cards — Appearance (System/Dark/
 * Light segmented theme + dynamic color), Playback, Downloads, API
 * endpoint (visible label + helper + URI keyboard + inline validation +
 * full-width save + success snackbar) and About. Everything persists
 * immediately except the endpoint, which is save-button based.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    var endpoint by remember(s.apiEndpoint) { mutableStateOf(s.apiEndpoint) }
    var endpointError by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(OmegaSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(OmegaSpacing.lg),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)

            // ---- Appearance ----
            SettingsCard(title = "Appearance") {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(OmegaSpacing.sm))
                val modes = listOf("SYSTEM" to "System", "DARK" to "Dark", "LIGHT" to "Light")
                val selectedIndex = modes.indexOfFirst { it.first == s.themeMode }.coerceAtLeast(0)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = index == selectedIndex,
                            onClick = {
                                vm.update { it.copy(themeMode = value, darkTheme = value != "LIGHT") }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                            icon = {},
                        ) {
                            Text(label, maxLines = 1, softWrap = false)
                        }
                    }
                }
                Spacer(Modifier.height(OmegaSpacing.lg))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Dynamic / artwork colors", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Tints surfaces from artwork colors",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = s.dynamicColor,
                        onCheckedChange = { checked -> vm.update { it.copy(dynamicColor = checked) } },
                    )
                }
            }

            // ---- Playback ----
            SettingsCard(title = "Playback") {
                Text("Playback quality", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Higher quality uses more data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(OmegaSpacing.sm))
                QualityChips(
                    selected = s.streamQuality,
                    onSelect = { q -> vm.update { it.copy(streamQuality = q) } },
                )
            }

            // ---- Downloads ----
            SettingsCard(title = "Downloads") {
                Text("Download quality", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Downloads use the quality chosen here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(OmegaSpacing.sm))
                QualityChips(
                    selected = s.downloadQuality,
                    onSelect = { q -> vm.update { it.copy(downloadQuality = q) } },
                )
            }

            // ---- API ----
            SettingsCard(title = "API") {
                Text("API endpoint", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(OmegaSpacing.sm))
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = {
                        endpoint = it
                        endpointError = null
                    },
                    label = { Text("API endpoint") },
                    placeholder = { Text("https://www.jiosaavn.com/api.php") },
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = endpointError != null,
                    supportingText = {
                        val error = endpointError
                        if (error != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                                Text(error, color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Text(
                                "Default: https://www.jiosaavn.com/api.php — the app talks to JioSaavn directly. Restart the app after changing.",
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(OmegaSpacing.md))
                Button(
                    onClick = {
                        val value = endpoint.trim()
                        endpointError =
                            when {
                                value.isBlank() -> "Enter an endpoint URL."
                                !value.startsWith("http://") && !value.startsWith("https://") ->
                                    "That doesn't look like a URL — it should start with https://"
                                else -> null
                            }
                        if (endpointError == null) {
                            vm.update { it.copy(apiEndpoint = value) }
                            scope.launch {
                                snackbar.showSnackbar("API endpoint saved — restart the app to apply")
                            }
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                ) {
                    Text("Save API endpoint")
                }
            }

            // ---- About ----
            SettingsCard(title = "About") {
                Text(
                    "No login, no account, no tracking. Favorites, downloads, history and playlists live only on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(OmegaSpacing.sm))
                Text(
                    "Omega · version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(OmegaSpacing.lg)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(OmegaSpacing.md))
            content()
        }
    }
}

/** Quality chips that wrap as a collection before any label shrinks (chip-reflow rule). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QualityChips(
    selected: String,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(OmegaSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(OmegaSpacing.sm),
    ) {
        QUALITIES.forEach { q ->
            FilterChip(
                selected = selected == q,
                onClick = { onSelect(q) },
                label = {
                    Text(
                        q,
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
            )
        }
    }
}
