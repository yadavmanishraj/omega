package com.manishraj.saavnmusic.feature.settings

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    var base by remember(s.apiEndpoint) { mutableStateOf(s.apiEndpoint) }
    LazyColumn(Modifier.padding(16.dp)) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                base,
                { base = it },
                label = { Text("API endpoint") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text("Default: https://www.jiosaavn.com/api.php - restart the app after changing")
                },
            )
            Button(onClick = { vm.update { it.copy(apiEndpoint = base) } }) { Text("Save API endpoint") }
            Spacer(Modifier.height(16.dp))
            Text("Playback quality")
            QualityChips(s.streamQuality) { q -> vm.update { it.copy(streamQuality = q) } }
            Spacer(Modifier.height(12.dp))
            Text("Download quality")
            QualityChips(s.downloadQuality) { q -> vm.update { it.copy(downloadQuality = q) } }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dark theme", Modifier.weight(1f))
                Switch(s.darkTheme, { v -> vm.update { it.copy(darkTheme = v) } })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dynamic / artwork colors", Modifier.weight(1f))
                Switch(s.dynamicColor, { v -> vm.update { it.copy(dynamicColor = v) } })
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "About: No login, no account, no tracking in this app. Favorites, downloads, history and playlists are stored only on your device.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QualityChips(
    selected: String,
    onSelect: (String) -> Unit,
) {
    // FlowRow, not Row: with a large system font size the four chips do
    // not fit on one line and a plain Row crushes the last chip to
    // zero width (its label renders one character per line).
    FlowRow(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
    ) {
        listOf("48kbps", "96kbps", "160kbps", "320kbps").forEach { q ->
            FilterChip(
                selected = selected == q,
                onClick = { onSelect(q) },
                label = { Text(q, maxLines = 1, softWrap = false) },
            )
        }
    }
}
