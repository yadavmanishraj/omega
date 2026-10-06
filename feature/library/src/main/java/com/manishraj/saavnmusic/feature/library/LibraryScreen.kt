package com.manishraj.saavnmusic.feature.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.ui.components.Artwork
import com.manishraj.saavnmusic.ui.components.EmptyState
import com.manishraj.saavnmusic.ui.components.SongRow

@Composable
fun LibraryScreen(
    vm: LibraryViewModel = hiltViewModel(),
    onPlayQueue: (List<Song>, Int) -> Unit,
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
            listOf("Favorites", "Downloads", "History", "Playlists").forEachIndexed { i, t ->
                Tab(tab == i, { tab = i }, text = { Text(t) })
            }
        }
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Sort: ${if (sortDesc) "Newest" else "Oldest"}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { sortDesc = !sortDesc }) { Text("Toggle") }
        }
        when (tab) {
            0 ->
                if (favs.isEmpty()) {
                    EmptyState("No favorites yet", "Tap the heart on any song")
                } else {
                    val ordered = if (sortDesc) favs else favs.reversed()
                    LazyColumn {
                        items(ordered) { song ->
                            SongRow(song, { onPlayQueue(ordered, ordered.indexOf(song)) })
                        }
                    }
                }

            1 ->
                if (dls.isEmpty()) {
                    EmptyState("No downloads yet", "Download songs for offline listening")
                } else {
                    LazyColumn {
                        items(dls) { d ->
                            ListItem(
                                headlineContent = { Text(d.name) },
                                supportingContent = { Text("${d.artist} • ${d.quality} • ${d.status}") },
                                leadingContent = { Artwork(d.imageUrl) },
                                trailingContent = {
                                    IconButton(onClick = { vm.deleteDownload(d.songId) }) {
                                        Icon(Icons.Default.Delete, null)
                                    }
                                },
                            )
                        }
                    }
                }

            2 ->
                Column {
                    if (hist.isNotEmpty()) {
                        TextButton(onClick = { vm.clearHistory() }) { Text("Clear history") }
                    }
                    if (hist.isEmpty()) {
                        EmptyState("Nothing played yet", "Your history lives only on this device")
                    } else {
                        LazyColumn {
                            items(hist) { song ->
                                ListItem(
                                    headlineContent = { Text(song.name) },
                                    supportingContent = { Text(song.artist) },
                                    leadingContent = { Artwork(song.imageUrl) },
                                )
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
                                ListItem(
                                    headlineContent = { Text(p.name) },
                                    trailingContent = {
                                        IconButton(onClick = { vm.deletePlaylist(p.id) }) {
                                            Icon(Icons.Default.Delete, null)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
        }
    }
    if (showCreate) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New playlist") },
            text = {
                OutlinedTextField(name, { name = it }, label = { Text("Name") })
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) vm.createPlaylist(name)
                        showCreate = false
                    },
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Cancel") }
            },
        )
    }
}
