package com.manishraj.saavnmusic.feature.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.manishraj.saavnmusic.domain.LocalPlaylist
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.ui.components.EmptyState
import com.manishraj.saavnmusic.ui.components.LocalOmegaSnackbar
import com.manishraj.saavnmusic.ui.components.OmegaActionGroup
import com.manishraj.saavnmusic.ui.components.OmegaLoadingIndicator
import com.manishraj.saavnmusic.ui.components.PlaylistPickerDialog
import com.manishraj.saavnmusic.ui.components.SongOverflowMenuButton
import com.manishraj.saavnmusic.ui.components.SongRow
import com.manishraj.saavnmusic.ui.components.songCountLabel
import com.manishraj.saavnmusic.ui.theme.OmegaSpacing

/**
 * Local playlist detail as a REAL NavHost destination
 * (`library/playlist/{id}`, A17 F-01): system Back and the header
 * arrow now agree — both pop to the Playlists list — the Library
 * tab's saved state is untouched, and process death restores the
 * open playlist from the back stack (the playlist half of F-16).
 * The playlist is read by id from the Library VM; an unknown or
 * deleted id pops back gracefully instead of stranding the user.
 */
@Composable
fun LocalPlaylistDetailScreen(
    playlistId: Long,
    vm: LibraryViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onPlayQueue: (List<Song>, Int) -> Unit,
    onDownloadEnqueued: () -> Unit = {},
) {
    // `playlistResolved` separates "the flow hasn't spoken yet" from
    // "there is no such playlist": only the latter pops back.
    var playlistResolved by remember(playlistId) { mutableStateOf(false) }
    val playlist by produceState<LocalPlaylist?>(null, playlistId) {
        vm.playlist(playlistId).collect {
            value = it
            playlistResolved = true
        }
    }
    LaunchedEffect(playlistResolved, playlist) {
        if (playlistResolved && playlist == null) onBack()
    }
    val playlists by vm.playlists.collectAsState()
    val current = playlist
    if (current == null) {
        // Loading (or gone, with the pop already queued above): the
        // back affordance stays on screen so the state never reads
        // as a dead end.
        Column(Modifier.fillMaxSize()) {
            PlaylistDetailHeader(name = null, songCount = null, onBack = onBack)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                OmegaLoadingIndicator(contentDescription = "Loading playlist")
            }
        }
        return
    }
    LocalPlaylistDetailContent(
        vm = vm,
        playlist = current,
        playlists = playlists,
        onBack = onBack,
        onPlayQueue = onPlayQueue,
        onDownloadEnqueued = onDownloadEnqueued,
    )
}

/** Back arrow + name + count — the header local playlists share with remote Detail (F-19). */
@Composable
private fun PlaylistDetailHeader(
    name: String?,
    songCount: Int?,
    onBack: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(OmegaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Library")
        }
        Column {
            Text(
                name ?: "",
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (songCount != null) {
                Text(
                    songCountLabel(songCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LocalPlaylistDetailContent(
    vm: LibraryViewModel,
    playlist: LocalPlaylist,
    playlists: List<LocalPlaylist>,
    onBack: () -> Unit,
    onPlayQueue: (List<Song>, Int) -> Unit,
    onDownloadEnqueued: () -> Unit,
) {
    var songsResolved by remember(playlist.id) { mutableStateOf(false) }
    val songs by produceState<List<Song>>(emptyList(), playlist.id) {
        vm.playlistSongs(playlist.id).collect {
            value = it
            songsResolved = true
        }
    }
    val favs by vm.favorites.collectAsState()
    val snackbar = LocalOmegaSnackbar.current
    var playlistTarget by remember { mutableStateOf<Song?>(null) }
    Column(Modifier.fillMaxSize()) {
        PlaylistDetailHeader(name = playlist.name, songCount = playlist.songCount, onBack = onBack)
        when {
            !songsResolved ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    OmegaLoadingIndicator(contentDescription = "Loading playlist")
                }

            songs.isEmpty() ->
                EmptyState(
                    "No songs yet",
                    // F-27: no glyph-dependent copy — spell the path out.
                    "Find a song anywhere in the app, open its menu, and choose Add to playlist.",
                )

            else ->
                LazyColumn(
                    contentPadding =
                        PaddingValues(horizontal = OmegaSpacing.lg, vertical = OmegaSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    item {
                        // Play all / Shuffle, the remote Detail action
                        // cluster (F-19). The horizontal scroll is the
                        // same font-2.0 guard Detail uses: labels
                        // scroll, never crush.
                        OmegaActionGroup(
                            primaryLabel = "Play all",
                            onPrimary = { onPlayQueue(songs, 0) },
                            secondaryLabel = "Shuffle",
                            onSecondary = { onPlayQueue(songs.shuffled(), 0) },
                            primaryIcon = Icons.Filled.PlayArrow,
                            secondaryIcon = Icons.Filled.Shuffle,
                            modifier =
                                Modifier
                                    .padding(vertical = OmegaSpacing.sm)
                                    .horizontalScroll(rememberScrollState()),
                        )
                    }
                    items(songs, key = { it.id }) { song ->
                        val songIsFavorite = favs.any { it.id == song.id }
                        SegmentedSegment(Modifier.animateItem()) {
                            SongRow(
                                song,
                                { onPlayQueue(songs, songs.indexOf(song)) },
                                trailing = {
                                    SongOverflowMenuButton(
                                        song = song,
                                        isFavorite = songIsFavorite,
                                        onPlayNext = {
                                            snackbar?.showMessage(
                                                playNextMessage(vm.playNext(song), song.name),
                                            )
                                        },
                                        onDownload = {
                                            vm.download(song)
                                            // F-04: Library downloads announce
                                            // exactly like Home's.
                                            snackbar?.showMessage("Download queued")
                                            onDownloadEnqueued()
                                        },
                                        onToggleFavorite = { vm.toggleFavorite(song, songIsFavorite) },
                                        onRemoveFromPlaylist = {
                                            vm.removeFromPlaylist(playlist.id, song) { removed ->
                                                snackbar?.showMessage(
                                                    "Removed from ${playlist.name}",
                                                    actionLabel = "Undo",
                                                    onAction = { vm.restoreToPlaylist(removed) },
                                                )
                                            }
                                        },
                                        onAddToPlaylist = { playlistTarget = song },
                                    )
                                },
                            )
                        }
                    }
                }
        }
    }

    playlistTarget?.let { song ->
        PlaylistPickerDialog(
            playlists = playlists,
            onPick = { picked ->
                vm.addToPlaylist(picked.id, song)
                playlistTarget = null
                snackbar?.showMessage("Added to ${picked.name}")
            },
            onCreatePlaylist = { name ->
                vm.createPlaylistAndAdd(name, song)
                playlistTarget = null
                snackbar?.showMessage("Added to $name")
            },
            onDismiss = { playlistTarget = null },
        )
    }
}
