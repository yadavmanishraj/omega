package com.manishraj.saavnmusic.feature.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.UiState
import com.manishraj.saavnmusic.ui.components.Artwork
import com.manishraj.saavnmusic.ui.components.ErrorState
import com.manishraj.saavnmusic.ui.components.GradientHeader
import com.manishraj.saavnmusic.ui.components.LocalOmegaSnackbar
import com.manishraj.saavnmusic.ui.components.MediaCard
import com.manishraj.saavnmusic.ui.components.PlaylistPickerDialog
import com.manishraj.saavnmusic.ui.components.SectionHeader
import com.manishraj.saavnmusic.ui.components.ShimmerList
import com.manishraj.saavnmusic.ui.components.SongOverflowMenuButton
import com.manishraj.saavnmusic.ui.components.SongRow
import com.manishraj.saavnmusic.ui.theme.OmegaRadius

@Composable
fun AlbumScreen(
    id: String,
    vm: DetailViewModel = hiltViewModel(),
    onPlayQueue: (List<Song>, Int) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(id) { vm.loadAlbum(id) }
    val s by vm.album.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val requestAddToPlaylist = rememberPlaylistPicker(vm)
    DetailList(
        s,
        { it.songs },
        { a -> SongListHeader(a.name, a.artist, a.imageUrl, a.description, onBack) },
        onPlayQueue,
        favorites = favorites,
        onAddToPlaylist = requestAddToPlaylist,
        onDownload = { vm.download(it) },
        onToggleFavorite = { song, isFav -> vm.toggleFavorite(song, isFav) },
    ) { vm.loadAlbum(id) }
}

@Composable
fun PlaylistScreen(
    id: String,
    vm: DetailViewModel = hiltViewModel(),
    onPlayQueue: (List<Song>, Int) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(id) { vm.loadPlaylist(id) }
    val s by vm.playlist.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val requestAddToPlaylist = rememberPlaylistPicker(vm)
    DetailList(
        s,
        { it.songs },
        { p -> SongListHeader(p.name, "Playlist", p.imageUrl, p.description, onBack) },
        onPlayQueue,
        favorites = favorites,
        onAddToPlaylist = requestAddToPlaylist,
        onDownload = { vm.download(it) },
        onToggleFavorite = { song, isFav -> vm.toggleFavorite(song, isFav) },
    ) { vm.loadPlaylist(id) }
}

/**
 * Hosts the add-to-playlist picker for a detail screen: returns the
 * request callback handed to song rows. Confirmation rides the
 * app-shell snackbar (mounted above the mini-player): a successful
 * pick or create shows "Added to {playlist}" — the Wave 1 proof event
 * for the shared snackbar system.
 */
@Composable
private fun rememberPlaylistPicker(vm: DetailViewModel): (Song) -> Unit {
    val playlists by vm.playlists.collectAsState()
    val snackbar = LocalOmegaSnackbar.current
    var target by remember { mutableStateOf<Song?>(null) }
    target?.let { song ->
        PlaylistPickerDialog(
            playlists = playlists,
            onPick = { playlist ->
                vm.addToPlaylist(playlist.id, song)
                target = null
                snackbar?.showMessage("Added to ${playlist.name}")
            },
            onCreatePlaylist = { name ->
                vm.createPlaylistAndAdd(name, song)
                target = null
                snackbar?.showMessage("Added to $name")
            },
            onDismiss = { target = null },
        )
    }
    return { song -> target = song }
}

@Composable
fun <T> DetailList(
    state: UiState<T>,
    songs: (T) -> List<Song>,
    header: @Composable (T) -> Unit,
    play: (List<Song>, Int) -> Unit,
    favorites: List<Song>,
    onAddToPlaylist: (Song) -> Unit,
    onDownload: (Song) -> Unit,
    onToggleFavorite: (Song, Boolean) -> Unit,
    retry: () -> Unit,
) {
    when (state) {
        is UiState.Loading -> ShimmerList()
        is UiState.Error -> ErrorState(state.message, retry)
        is UiState.Success -> {
            val list = songs(state.data)
            LazyColumn {
                item { header(state.data) }
                item {
                    Row(Modifier.padding(16.dp)) {
                        Button(onClick = { play(list, 0) }) {
                            Icon(Icons.Default.PlayArrow, null)
                            Text(" Play all")
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { play(list.shuffled(), 0) }) { Text("Shuffle") }
                    }
                }
                items(list) { song ->
                    val isFavorite = favorites.any { it.id == song.id }
                    SongRow(
                        song,
                        { play(list, list.indexOf(song)) },
                        trailing = {
                            SongOverflowMenuButton(
                                song = song,
                                isFavorite = isFavorite,
                                onDownload = { onDownload(song) },
                                onToggleFavorite = { onToggleFavorite(song, isFavorite) },
                                onAddToPlaylist = { onAddToPlaylist(song) },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun SongListHeader(
    title: String,
    subtitle: String,
    image: String?,
    desc: String?,
    onBack: () -> Unit,
) {
    GradientHeader(image, onBack) {
        // Centered composition: left-aligned artwork left a wide
        // empty tinted region beside it (UI/UX audit, 2026-10-07).
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Artwork(image, 180, OmegaRadius.xl)
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            if (!desc.isNullOrBlank()) {
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun ArtistScreen(
    id: String,
    vm: DetailViewModel = hiltViewModel(),
    onAlbum: (String) -> Unit,
    onPlayQueue: (List<Song>, Int) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(id) { vm.loadArtist(id) }
    val s by vm.artist.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val requestAddToPlaylist = rememberPlaylistPicker(vm)
    when (val a = s) {
        is UiState.Loading -> ShimmerList()
        is UiState.Error -> ErrorState(a.message, onRetry = { vm.loadArtist(id) })
        is UiState.Success ->
            LazyColumn {
                item {
                    SongListHeader(
                        a.data.name,
                        listOfNotNull(a.data.followers?.let { "$it followers" }).joinToString(),
                        a.data.imageUrl,
                        a.data.bio,
                        onBack,
                    )
                }
                item { SectionHeader("Top songs") }
                items(a.data.topSongs) { song ->
                    val isFavorite = favorites.any { it.id == song.id }
                    SongRow(
                        song,
                        { onPlayQueue(a.data.topSongs, a.data.topSongs.indexOf(song)) },
                        trailing = {
                            SongOverflowMenuButton(
                                song = song,
                                isFavorite = isFavorite,
                                onDownload = { vm.download(song) },
                                onToggleFavorite = { vm.toggleFavorite(song, isFavorite) },
                                onAddToPlaylist = { requestAddToPlaylist(song) },
                            )
                        },
                    )
                }
                item { SectionHeader("Top albums") }
                item { LazyRow { items(a.data.topAlbums) { al -> MediaCard(al.name, al.artist, al.imageUrl) { onAlbum(al.id) } } } }
            }
    }
}
