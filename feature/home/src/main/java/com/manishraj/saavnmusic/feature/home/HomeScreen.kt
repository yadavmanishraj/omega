package com.manishraj.saavnmusic.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.UiState
import com.manishraj.saavnmusic.ui.components.ErrorState
import com.manishraj.saavnmusic.ui.components.MediaCard
import com.manishraj.saavnmusic.ui.components.SectionHeader
import com.manishraj.saavnmusic.ui.components.ShimmerList
import com.manishraj.saavnmusic.ui.components.SongRow

@Composable
fun HomeScreen(
    vm: HomeViewModel = hiltViewModel(),
    onAlbum: (String) -> Unit,
    onPlaylist: (String) -> Unit,
    onArtist: (String) -> Unit,
    onPlayQueue: (List<Song>, Int) -> Unit,
) {
    val trending by vm.trending.collectAsState()
    val albums by vm.albums.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val artists by vm.artists.collectAsState()
    val history by vm.history.collectAsState()
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(16.dp)) {
                Text("Good listening", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "No account. Just music.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (history.isNotEmpty()) {
            item { SectionHeader("Recently played") }
            item {
                LazyRow {
                    items(history) { song ->
                        MediaCard(song.name, song.artist, song.imageUrl) {
                            onPlayQueue(history, history.indexOf(song))
                        }
                    }
                }
            }
        }
        item { SectionHeader("Trending songs") }
        when (val s = trending) {
            is UiState.Loading -> item { ShimmerList() }
            is UiState.Error -> item { ErrorState(s.message) { vm.load() } }
            is UiState.Success -> items(s.data.take(10)) { song -> SongRow(song, { onPlayQueue(s.data, s.data.indexOf(song)) }) }
        }
        item { SectionHeader("Albums") }
        item { LazyRow { items(albums) { a -> MediaCard(a.name, a.artist, a.imageUrl) { onAlbum(a.id) } } } }
        item { SectionHeader("Playlists") }
        item { LazyRow { items(playlists) { p -> MediaCard(p.name, p.description.orEmpty(), p.imageUrl) { onPlaylist(p.id) } } } }
        item { SectionHeader("Artists") }
        item { LazyRow { items(artists) { a -> MediaCard(a.name, "Artist", a.imageUrl) { onArtist(a.id) } } } }
    }
}
