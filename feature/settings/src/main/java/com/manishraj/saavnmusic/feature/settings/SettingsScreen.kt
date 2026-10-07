package com.manishraj.saavnmusic.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.manishraj.saavnmusic.ui.components.OmegaChoiceGroup
import com.manishraj.saavnmusic.ui.components.OmegaSegmentedList
import com.manishraj.saavnmusic.ui.components.OmegaSegmentedListItem
import com.manishraj.saavnmusic.ui.theme.OmegaSpacing
import kotlinx.coroutines.launch

private val QUALITIES = listOf("12kbps", "48kbps", "96kbps", "160kbps", "320kbps")

private val THEME_MODES = listOf("SYSTEM", "DARK", "LIGHT")

private fun themeModeLabel(mode: String): String =
    when (mode) {
        "DARK" -> "Dark"
        "LIGHT" -> "Light"
        else -> "System"
    }

/**
 * Settings (REDESIGN_SPEC §7 + M3 Expressive spec §5 — tertiary, the
 * subtlest dosing): Appearance / Playback / Downloads are segmented
 * groupings (filled segments, 2dp gaps, no dividers); the theme and
 * quality pickers are connected button groups ([OmegaChoiceGroup] —
 * options that stop fitting overflow into the group's menu instead of
 * crushing their labels at large font scales); the dynamic-color
 * switch carries the expressive handle check icon. API endpoint
 * (visible label + helper + URI keyboard + inline validation +
 * full-width save + success snackbar) and About keep their cards.
 * Everything persists immediately except the endpoint, which is
 * save-button based; datastore keys and update semantics unchanged.
 */
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
            SettingsSection(title = "Appearance") {
                OmegaSegmentedList {
                    SettingsSegment {
                        Text("Theme", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(OmegaSpacing.sm))
                        OmegaChoiceGroup(
                            options = THEME_MODES,
                            selected = s.themeMode,
                            onSelect = { value ->
                                vm.update { it.copy(themeMode = value, darkTheme = value != "LIGHT") }
                            },
                            label = { themeModeLabel(it) },
                        )
                    }
                    OmegaSegmentedListItem(
                        headline = "Dynamic / artwork colors",
                        supporting = "Tints surfaces from artwork colors",
                        trailing = {
                            Switch(
                                checked = s.dynamicColor,
                                onCheckedChange = { checked ->
                                    vm.update { it.copy(dynamicColor = checked) }
                                },
                                thumbContent =
                                    if (s.dynamicColor) {
                                        {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        }
                                    } else {
                                        null
                                    },
                            )
                        },
                    )
                }
            }

            // ---- Playback ----
            SettingsSection(title = "Playback") {
                OmegaSegmentedList {
                    SettingsSegment {
                        Text("Playback quality", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Higher quality uses more data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(OmegaSpacing.sm))
                        OmegaChoiceGroup(
                            options = QUALITIES,
                            selected = s.streamQuality,
                            onSelect = { q -> vm.update { it.copy(streamQuality = q) } },
                            label = { it },
                        )
                    }
                }
            }

            // ---- Downloads ----
            SettingsSection(title = "Downloads") {
                OmegaSegmentedList {
                    SettingsSegment {
                        Text("Download quality", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            // Verified against DownloadWorker: downloads run on
                            // NetworkType.CONNECTED — any network, metered or not.
                            "Downloads use this quality on Wi-Fi and mobile data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(OmegaSpacing.sm))
                        OmegaChoiceGroup(
                            options = QUALITIES,
                            selected = s.downloadQuality,
                            onSelect = { q -> vm.update { it.copy(downloadQuality = q) } },
                            label = { it },
                        )
                    }
                }
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

/** A settings section: primary-toned header over its segmented grouping. */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(OmegaSpacing.md))
        content()
    }
}

/**
 * One filled segment with free-form content (a label + a choice
 * group) — the same surfaceContainerHigh + large-shape treatment as
 * [OmegaSegmentedListItem], for segments the item's
 * headline/supporting/trailing slots can't express.
 */
@Composable
private fun SettingsSegment(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            Modifier.padding(OmegaSpacing.lg),
            content = content,
        )
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
