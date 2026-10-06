package com.manishraj.saavnmusic.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.UiState
import com.manishraj.saavnmusic.ui.components.Artwork
import com.manishraj.saavnmusic.ui.components.EmptyState
import com.manishraj.saavnmusic.ui.components.ErrorState
import com.manishraj.saavnmusic.ui.components.ShimmerList
import com.manishraj.saavnmusic.ui.components.SongRow

// NOTE: SearchBarInputField (the non-deprecated SearchBar API) does not
// resolve against material3 1.3.1 (Compose BOM 2024.12.01) on the user's
// toolchain, so this screen deliberately uses the deprecated
// SearchBar(query, active, ...) overload until the BOM is bumped.
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    vm: SearchViewModel = hiltViewModel(),
    onAlbum: (String) -> Unit,
    onPlaylist: (String) -> Unit,
    onArtist: (String) -> Unit,
    onPlayQueue: (List<Song>, Int) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val songs by vm.songs.collectAsState()
    val albums by vm.albums.collectAsState()
    val artists by vm.artists.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val recent by vm.recent.collectAsState()
    val tab by vm.tab.collectAsState()
    Column(Modifier.fillMaxSize()) {
        SearchBar(
            query = text,
            onQueryChange = { text = it },
            onSearch = { vm.search(text) },
            active = false,
            onActiveChange = {},
            placeholder = { Text("Songs, albums, artists…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {}
        if (recent.isNotEmpty() &&
            songs is UiState.Success &&
            (songs as UiState.Success<List<Song>>).data.isEmpty()
        ) {
            Text("Recent searches", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
            LazyRow(Modifier.padding(horizontal = 12.dp)) {
                items(recent) { query ->
                    AssistChip(
                        onClick = {
                            text = query
                            vm.search(query)
                        },
                        label = { Text(query) },
                    )
                    Spacer(Modifier.width(8.dp))
                }
            }
            TextButton(onClick = { vm.clearRecent() }) { Text("Clear recent searches") }
        }
        TabRow(selectedTabIndex = tab) {
            listOf("Songs", "Albums", "Artists", "Playlists").forEachIndexed { i, t ->
                Tab(
                    selected = tab == i,
                    onClick = { vm.tab.value = i },
                    text = {
                        // Never let a tab label wrap ("Album\ns" at large
                        // font sizes); single line, ellipsis if needed.
                        Text(t, maxLines = 1, softWrap = false)
                    },
                )
            }
        }
        when (tab) {
            0 ->
                when (val s = songs) {
                    is UiState.Loading -> ShimmerList()
                    is UiState.Error -> ErrorState(s.message) { vm.search(text) }
                    is UiState.Success ->
                        if (s.data.isEmpty()) {
                            EmptyState("Search for music", "Results will appear here")
                        } else {
                            LazyColumn {
                                items(s.data) { song -> SongRow(song, { onPlayQueue(s.data, s.data.indexOf(song)) }) }
                            }
                        }
                }

            1 ->
                LazyColumn {
                    items(albums) { a ->
                        ListItem(
                            headlineContent = { Text(a.name) },
                            supportingContent = { Text(a.artist) },
                            leadingContent = { Artwork(a.imageUrl) },
                            modifier = Modifier.clickable { onAlbum(a.id) },
                        )
                    }
                }

            2 ->
                LazyColumn {
                    items(artists) { a ->
                        ListItem(
                            headlineContent = { Text(a.name) },
                            leadingContent = { Artwork(a.imageUrl) },
                            modifier = Modifier.clickable { onArtist(a.id) },
                        )
                    }
                }

            else ->
                LazyColumn {
                    items(playlists) { p ->
                        ListItem(
                            headlineContent = { Text(p.name) },
                            supportingContent = { Text("${p.songCount ?: ""} songs") },
                            leadingContent = { Artwork(p.imageUrl) },
                            modifier = Modifier.clickable { onPlaylist(p.id) },
                        )
                    }
                }
        }
    }
}
