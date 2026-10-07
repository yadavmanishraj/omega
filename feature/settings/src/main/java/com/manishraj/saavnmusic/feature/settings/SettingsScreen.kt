package com.manishraj.saavnmusic.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.manishraj.saavnmusic.ui.components.LocalOmegaSnackbar
import com.manishraj.saavnmusic.ui.components.OmegaChoiceGroup
import com.manishraj.saavnmusic.ui.components.OmegaSegmentedContainer
import com.manishraj.saavnmusic.ui.components.OmegaSegmentedList
import com.manishraj.saavnmusic.ui.components.OmegaSegmentedListItem
import com.manishraj.saavnmusic.ui.theme.OmegaSpacing

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
 * subtlest dosing): every group is a section header over a segmented
 * grouping (filled segments, 2dp gaps, no dividers — the kit's
 * [OmegaSegmentedContainer], polish item 13); the theme and
 * quality pickers are connected button groups ([OmegaChoiceGroup] —
 * options that stop fitting overflow into the group's menu instead of
 * crushing their labels at large font scales, and wrap to chip rows
 * at very large scales so a selection is never hidden); the
 * wallpaper-color switch carries the expressive handle check icon. API
 * endpoint (visible label + helper + URI keyboard + inline validation
 * + full-width save + success snackbar), Help and About sit in the
 * same section + segment grammar. Everything persists immediately
 * except the endpoint, which is save-button based; datastore keys and
 * update semantics unchanged. Feedback rides the SHELL snackbar via
 * LocalOmegaSnackbar (F-08) — this screen used to mount a private
 * stock host that rendered mis-anchored, off the shell's channel
 * entirely.
 */
@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    var endpoint by remember(s.apiEndpoint) { mutableStateOf(s.apiEndpoint) }
    var endpointError by remember { mutableStateOf<String?>(null) }
    val snackbar = LocalOmegaSnackbar.current
    val context = LocalContext.current
    // The version comes from the installed package (F-20) — a
    // hardcoded "1.0.0" here would silently go stale on the next
    // release bump.
    val versionName =
        remember(context) {
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull()
        }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(OmegaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(OmegaSpacing.lg),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        // ---- Appearance ----
        SettingsSection(title = "Appearance") {
            OmegaSegmentedList {
                OmegaSegmentedContainer(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(OmegaSpacing.lg),
                ) {
                    Text("Theme", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        // The current value is always readable as
                        // text (F-23) — a selected option can sit
                        // in the group's overflow menu, where
                        // "nothing highlighted" reads as "nothing
                        // selected".
                        "Current: ${themeModeLabel(s.themeMode)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(OmegaSpacing.sm))
                    OmegaChoiceGroup(
                        options = THEME_MODES,
                        selected = s.themeMode,
                        onSelect = { value ->
                            vm.update { it.copy(themeMode = value, darkTheme = value != "LIGHT") }
                        },
                        label = { themeModeLabel(it) },
                        wrapAtLargeFont = true,
                    )
                }
                OmegaSegmentedListItem(
                    headline = "Wallpaper colors",
                    supporting = "Surfaces follow your wallpaper's colors",
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
                OmegaSegmentedContainer(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(OmegaSpacing.lg),
                ) {
                    Text("Playback quality", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Current: ${s.streamQuality} · Higher quality uses more data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(OmegaSpacing.sm))
                    OmegaChoiceGroup(
                        options = QUALITIES,
                        selected = s.streamQuality,
                        onSelect = { q -> vm.update { it.copy(streamQuality = q) } },
                        label = { it },
                        wrapAtLargeFont = true,
                    )
                }
            }
        }

        // ---- Downloads ----
        SettingsSection(title = "Downloads") {
            OmegaSegmentedList {
                OmegaSegmentedContainer(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(OmegaSpacing.lg),
                ) {
                    Text("Download quality", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        // Verified against DownloadWorker: downloads run on
                        // NetworkType.CONNECTED — any network, metered or not.
                        "Current: ${s.downloadQuality} · Downloads use this quality on Wi-Fi and mobile data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(OmegaSpacing.sm))
                    OmegaChoiceGroup(
                        options = QUALITIES,
                        selected = s.downloadQuality,
                        onSelect = { q -> vm.update { it.copy(downloadQuality = q) } },
                        label = { it },
                        wrapAtLargeFont = true,
                    )
                }
            }
        }

        // ---- API ----
        SettingsSection(title = "API") {
            OmegaSegmentedList {
                OmegaSegmentedContainer(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(OmegaSpacing.lg),
                ) {
                    // No inner "API endpoint" title (F-20): the section
                    // header + the field label already say it — the
                    // group used to repeat the phrase three times in
                    // 200dp.
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
                                    "Default: https://www.jiosaavn.com/api.php. The app talks to JioSaavn directly. Restart the app after changing.",
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
                                        "That doesn't look like a URL. It should start with https://"
                                    else -> null
                                }
                            if (endpointError == null) {
                                vm.update { it.copy(apiEndpoint = value) }
                                snackbar?.showMessage("API endpoint saved. Restart the app to apply")
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(OmegaSpacing.xxxl),
                    ) {
                        Text("Save API endpoint")
                    }
                }
            }
        }

        // ---- Help ----
        SettingsSection(title = "Help") {
            OmegaSegmentedList {
                OmegaSegmentedContainer(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(OmegaSpacing.lg),
                ) {
                    Text("Where downloads live", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Downloads are saved on this device and play offline, no account needed. Library, then Downloads, shows their total size.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(OmegaSpacing.md))
                    Text("Quality and data use", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Higher playback quality uses more data while streaming, and higher download quality saves bigger files. Playback and download quality are set separately.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(OmegaSpacing.md))
                    Text("Sleep timer and queue", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Set the sleep timer in the player to pause playback after 15, 30 or 60 minutes. The player's queue button shows the full queue; tap a song to jump to it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ---- About ----
        SettingsSection(title = "About") {
            OmegaSegmentedList {
                OmegaSegmentedContainer(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(OmegaSpacing.lg),
                ) {
                    Text(
                        "No login, no account, no tracking. Favorites, downloads, history and playlists live only on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(OmegaSpacing.sm))
                    Text(
                        if (versionName != null) "Omega · version $versionName" else "Omega",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
