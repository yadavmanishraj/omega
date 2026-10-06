package com.manishraj.saavnmusic.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.UiState
import com.manishraj.saavnmusic.ui.components.EmptyState
import com.manishraj.saavnmusic.ui.components.ErrorState
import com.manishraj.saavnmusic.ui.components.MediaCard
import com.manishraj.saavnmusic.ui.components.PlaylistPickerDialog
import com.manishraj.saavnmusic.ui.components.SectionHeader
import com.manishraj.saavnmusic.ui.components.ShimmerList
import com.manishraj.saavnmusic.ui.components.SongOverflowMenuButton
import com.manishraj.saavnmusic.ui.components.SongRow
import com.manishraj.saavnmusic.ui.theme.OmegaSpacing
import java.util.Calendar

private fun greetingForHour(hour: Int): String =
    when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good listening"
    }

/**
 * Home (REDESIGN_SPEC §4): opens straight into music. Sections are
 * independently stateful; a section with no items is omitted entirely
 * (header included); offline is a mode with its own local-content
 * layout, not an error screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel = hiltViewModel(),
    onAlbum: (String) -> Unit,
    onPlaylist: (String) -> Unit,
    onArtist: (String) -> Unit,
    onPlayQueue: (List<Song>, Int) -> Unit,
    onOpenDownloads: () -> Unit,
) {
    val trending by vm.trending.collectAsState()
    val albums by vm.albums.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val artists by vm.artists.collectAsState()
    val history by vm.history.collectAsState()
    val downloads by vm.downloads.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val localPlaylists by vm.userPlaylists.collectAsState()
    val online by vm.online.collectAsState()
    var playlistTarget by remember { mutableStateOf<Song?>(null) }

    val refreshing = trending is UiState.Loading
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = { vm.load() }) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = OmegaSpacing.lg, end = OmegaSpacing.lg, top = OmegaSpacing.xl, bottom = OmegaSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            greetingForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)),
                            style = MaterialTheme.typography.displaySmall,
                        )
                        Text(
                            "No account. Just music.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = "Omega",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (!online) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = OmegaSpacing.lg, vertical = OmegaSpacing.sm)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = OmegaSpacing.lg, vertical = OmegaSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            "You're offline — showing downloads & library",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = OmegaSpacing.md),
                        )
                        TextButton(onClick = onOpenDownloads) { Text("Downloads") }
                    }
                }
            }

            if (history.isNotEmpty()) {
                item { SectionHeader("Jump back in") }
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

            if (!online) {
                // Offline variant: local content only; remote sections are
                // hidden rather than errored (spec §4).
                if (downloads.isNotEmpty()) {
                    item { SectionHeader("Your downloads") }
                    item {
                        LazyRow {
                            items(downloads) { d ->
                                MediaCard(d.name, d.artist, d.imageUrl) { onOpenDownloads() }
                            }
                        }
                    }
                }
                if (favorites.isNotEmpty()) {
                    item { SectionHeader("Your favorites") }
                    items(favorites.take(10)) { song ->
                        SongRow(
                            song,
                            { onPlayQueue(favorites, favorites.indexOf(song)) },
                            trailing = {
                                SongOverflowMenuButton(song) { playlistTarget = song }
                            },
                        )
                    }
                }
                if (downloads.isEmpty() && favorites.isEmpty() && history.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Nothing downloaded yet",
                            subtitle = "When you're back online, download songs to listen offline.",
                            actionLabel = "Go to Library",
                            onAction = onOpenDownloads,
                        )
                    }
                }
            } else {
                val allFailed =
                    trending is UiState.Error &&
                        albums is UiState.Error &&
                        playlists is UiState.Error &&
                        artists is UiState.Error
                if (allFailed) {
                    // Full-page error only when nothing loaded at all.
                    item {
                        ErrorState(
                            message = (trending as UiState.Error).message,
                            onRetry = { vm.load() },
                        )
                    }
                } else {
                    when (val s = trending) {
                        is UiState.Loading -> {
                            item { SectionHeader("Trending songs") }
                            item { ShimmerList() }
                        }
                        is UiState.Error -> {
                            item { SectionHeader("Trending songs") }
                            item { ErrorState(s.message, onRetry = { vm.load() }) }
                        }
                        is UiState.Success -> {
                            if (s.data.isNotEmpty()) {
                                item { SectionHeader("Trending songs") }
                                items(s.data.take(10)) { song ->
                                    SongRow(
                                        song,
                                        { onPlayQueue(s.data, s.data.indexOf(song)) },
                                        trailing = {
                                            SongOverflowMenuButton(song) { playlistTarget = song }
                                        },
                                    )
                                }
                            }
                        }
                    }
                    when (val s = albums) {
                        is UiState.Loading -> {
                            item { SectionHeader("New albums") }
                            item { ShimmerList() }
                        }
                        is UiState.Error -> {
                            item { SectionHeader("New albums") }
                            item { ErrorState(s.message, onRetry = { vm.load() }) }
                        }
                        is UiState.Success -> {
                            if (s.data.isNotEmpty()) {
                                item { SectionHeader("New albums") }
                                item {
                                    LazyRow {
                                        items(s.data) { a ->
                                            MediaCard(a.name, a.artist, a.imageUrl) { onAlbum(a.id) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    when (val s = playlists) {
                        is UiState.Loading -> {
                            item { SectionHeader("Playlists for you") }
                            item { ShimmerList() }
                        }
                        is UiState.Error -> {
                            item { SectionHeader("Playlists for you") }
                            item { ErrorState(s.message, onRetry = { vm.load() }) }
                        }
                        is UiState.Success -> {
                            if (s.data.isNotEmpty()) {
                                item { SectionHeader("Playlists for you") }
                                item {
                                    LazyRow {
                                        items(s.data) { p ->
                                            MediaCard(p.name, p.description.orEmpty(), p.imageUrl) { onPlaylist(p.id) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    when (val s = artists) {
                        is UiState.Loading -> {
                            item { SectionHeader("Artists") }
                            item { ShimmerList() }
                        }
                        is UiState.Error -> {
                            item { SectionHeader("Artists") }
                            item { ErrorState(s.message, onRetry = { vm.load() }) }
                        }
                        is UiState.Success -> {
                            if (s.data.isNotEmpty()) {
                                item { SectionHeader("Artists") }
                                item {
                                    LazyRow {
                                        items(s.data) { a ->
                                            MediaCard(a.name, "Artist", a.imageUrl, circular = true) { onArtist(a.id) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(OmegaSpacing.xl)) }
        }
    }

    // No snackbar host on Home: the picker closing is the confirmation.
    playlistTarget?.let { song ->
        PlaylistPickerDialog(
            playlists = localPlaylists,
            onPick = { playlist ->
                vm.addToPlaylist(playlist.id, song)
                playlistTarget = null
            },
            onCreatePlaylist = { name ->
                vm.createPlaylistAndAdd(name, song)
                playlistTarget = null
            },
            onDismiss = { playlistTarget = null },
        )
    }
}
