package com.manishraj.saavnmusic.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.manishraj.saavnmusic.domain.*
import com.manishraj.saavnmusic.ui.components.*
import com.manishraj.saavnmusic.ui.viewmodel.*

@Composable fun LibraryScreen(
    vm: LibraryViewModel = hiltViewModel(),
    playerVm: PlayerViewModel = hiltViewModel(),
) {
    var tab by remember { mutableIntStateOf(0) }
    val favs by vm.favorites.collectAsState()
    val dls by vm.downloads.collectAsState()
    val hist by vm.history.collectAsState()
    val pls by vm.playlists.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var sortDesc by remember { mutableStateOf(true) }
    Column {
        TabRow(selectedTabIndex = tab) {
            listOf("Favorites", "Downloads", "History", "Playlists").forEachIndexed {
                    i,
                    t,
                ->
                Tab(tab == i, { tab = i }, text = { Text(t) })
            }
        }
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Sort: ${if (sortDesc) "Newest" else "Oldest"}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                sortDesc =
                    !sortDesc
            }) { Text("Toggle") }
        }
        when (tab) {
            0 ->
                if (favs.isEmpty()) {
                    EmptyState("No favorites yet", "Tap the heart on any song")
                } else {
                    LazyColumn {
                        items(
                            if (sortDesc) favs else favs.reversed(),
                        ) { f ->
                            SongRow(Song(f.songId, f.name, f.artist, f.album, f.imageUrl, f.durationSec, f.streamUrl), {
                                playerVm.play(
                                    favs.map { Song(it.songId, it.name, it.artist, it.album, it.imageUrl, it.durationSec, it.streamUrl) },
                                    favs.indexOf(f),
                                )
                            })
                        }
                    }
                }
            1 ->
                if (dls.isEmpty()) {
                    EmptyState("No downloads yet", "Download songs for offline listening")
                } else {
                    LazyColumn {
                        items(dls) { d ->
                            ListItem(headlineContent = {
                                Text(d.name)
                            }, supportingContent = {
                                Text("${d.artist} • ${d.quality} • ${d.status}")
                            }, leadingContent = {
                                Artwork(d.imageUrl)
                            }, trailingContent = {
                                IconButton(
                                    onClick = { vm.deleteDownload(d.songId) },
                                ) { Icon(Icons.Default.Delete, null) }
                            })
                        }
                    }
                }
            2 ->
                Column {
                    if (hist.isNotEmpty()) TextButton(onClick = { vm.clearHistory() }) { Text("Clear history") }
                    if (hist.isEmpty()) {
                        EmptyState("Nothing played yet", "Your history lives only on this device")
                    } else {
                        LazyColumn {
                            items(hist) { h ->
                                ListItem(headlineContent = {
                                    Text(h.name)
                                }, supportingContent = { Text(h.artist) }, leadingContent = { Artwork(h.imageUrl) })
                            }
                        }
                    }
                }
            else ->
                Column {
                    Button(onClick = { showCreate = true }, Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Add, null)
                        Text(" New local playlist")
                    }
                    if (pls.isEmpty()) {
                        EmptyState("No local playlists", "Create your own - stored in Room, no account needed")
                    } else {
                        LazyColumn {
                            items(pls) { p ->
                                ListItem(headlineContent = {
                                    Text(p.name)
                                }, trailingContent = {
                                    IconButton(
                                        onClick = { vm.deletePlaylist(p.id) },
                                    ) { Icon(Icons.Default.Delete, null) }
                                })
                            }
                        }
                    }
                }
        }
    }
    if (showCreate) {
        var name by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showCreate = false }, title = { Text("New playlist") }, text = {
            OutlinedTextField(name, {
                name =
                    it
            }, label = { Text("Name") })
        }, confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) vm.createPlaylist(name)
                showCreate = false
            }) { Text("Create") }
        }, dismissButton = {
            TextButton(onClick = {
                showCreate =
                    false
            }) { Text("Cancel") }
        })
    }
}

@Composable fun DownloadsScreen(vm: LibraryViewModel = hiltViewModel()) {
    val dls by vm.downloads.collectAsState()
    if (dls.isEmpty()) {
        EmptyState("No downloads", "Use the download action on a song, album or playlist")
    } else {
        LazyColumn {
            items(dls) { d ->
                ListItem(headlineContent = { Text(d.name) }, supportingContent = {
                    Text(
                        "${d.quality} • ${"%.1f".format(
                            d.sizeBytes / 1_000_000.0,
                        )} MB • ${d.filePath}",
                    )
                }, leadingContent = { Artwork(d.imageUrl) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    var base by remember(s.baseUrl) { mutableStateOf(s.baseUrl) }
    LazyColumn(Modifier.padding(16.dp)) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(base, {
                base = it
            }, label = {
                Text("API base URL")
            }, modifier = Modifier.fillMaxWidth(), supportingText = {
                Text(
                    "Default: https://saavn.dev/api/ - restart the app after changing",
                )
            })
            Button(onClick = { vm.update { it.copy(baseUrl = base) } }) { Text("Save API URL") }
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
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
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
