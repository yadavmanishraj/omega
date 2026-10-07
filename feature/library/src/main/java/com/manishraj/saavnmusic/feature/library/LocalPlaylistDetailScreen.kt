package com.manishraj.saavnmusic.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.manishraj.saavnmusic.domain.LocalPlaylist
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.ui.components.Artwork
import com.manishraj.saavnmusic.ui.components.EmptyState
import com.manishraj.saavnmusic.ui.components.GradientHeader
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

/**
 * Back arrow + name + count for the UNRESOLVED state only (playlist
 * still loading, or gone with the pop already queued): the gradient
 * header needs the playlist, so this plain bar — the role Detail's
 * DetailBackBar plays for its Loading/Error states — keeps the back
 * affordance on screen. Resolved playlists get the full gradient
 * header below (LocalPlaylistDetailHeader).
 */
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
    // Distinct artworks in playlist order drive the header cover
    // (single art or 2x2 mosaic) and the gradient's palette seed.
    val coverArtworks =
        remember(songs) { songs.mapNotNull { it.imageUrl }.distinct().take(4) }
    // One scrolling list, header first — the remote Detail shape:
    // the gradient header scrolls away with the songs instead of
    // pinning a second, plainer bar above them.
    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            LocalPlaylistDetailHeader(
                playlist = playlist,
                coverArtworks = coverArtworks,
                onBack = onBack,
            )
        }
        when {
            !songsResolved ->
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        OmegaLoadingIndicator(contentDescription = "Loading playlist")
                    }
                }

            songs.isEmpty() ->
                item {
                    EmptyState(
                        "No songs yet",
                        // F-27: no glyph-dependent copy — spell the path out.
                        "Find a song anywhere in the app, open its menu, and choose Add to playlist.",
                    )
                }

            else -> {
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
                                .padding(horizontal = OmegaSpacing.lg, vertical = OmegaSpacing.sm)
                                .horizontalScroll(rememberScrollState()),
                    )
                }
                items(songs, key = { it.id }) { song ->
                    val songIsFavorite = favs.any { it.id == song.id }
                    SegmentedSegment(
                        Modifier
                            .animateItem()
                            .padding(horizontal = OmegaSpacing.lg),
                    ) {
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
                item { Spacer(Modifier.height(OmegaSpacing.sm)) }
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

/**
 * Corner for the header cover: the shape language's extraLarge
 * slot (28dp — `OmegaShapes.extraLarge`), matching the remote
 * Detail header and the player hero. [Artwork] takes a Dp corner,
 * so the value is spelled out here; the two must move together.
 */
private val HeaderArtworkCorner = 28.dp

/**
 * The gradient detail header, at parity with remote Album/Playlist
 * detail (critique P1): the shared [GradientHeader] (palette derived
 * from the cover, contrast-checked content color, back affordance
 * inside the gradient), a centered cover, the playlist name as the
 * header title, and the scoped subtitle "Playlist · N songs" — the
 * F-20 form remote playlist headers use, built from the playlist
 * row's own count. Local playlists have no upstream description,
 * so the header ends at the subtitle.
 */
@Composable
private fun LocalPlaylistDetailHeader(
    playlist: LocalPlaylist,
    coverArtworks: List<String>,
    onBack: () -> Unit,
) {
    GradientHeader(coverArtworks.firstOrNull(), onBack) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(OmegaSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlaylistCoverArt(coverArtworks)
            Spacer(Modifier.height(12.dp))
            Text(
                playlist.name,
                // Same emphasized slot as remote Detail headers:
                // 24sp headlineSmall with emphasis by weight/family,
                // so the title never reflows at font scale 1.33/2.0.
                style = MaterialTheme.typography.headlineSmallEmphasized,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                if (playlist.songCount > 0) {
                    "Playlist · ${songCountLabel(playlist.songCount)}"
                } else {
                    "Playlist"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The local playlist cover: the first song's artwork when the
 * playlist has a single distinct artwork; a 2x2 mosaic of up to
 * four distinct artworks otherwise (cycling the available ones
 * when only two or three exist, so the frame is never part-empty);
 * the shared [Artwork] placeholder (note glyph on surfaceVariant)
 * when the playlist is empty or no song carries artwork. Tiles are
 * the shared [Artwork] at 90dp with square corners — the mosaic
 * frame supplies the 28dp silhouette — so image loading, cropping,
 * and the missing-art state stay the design system's, not a local
 * re-implementation.
 */
@Composable
private fun PlaylistCoverArt(coverArtworks: List<String>) {
    when (coverArtworks.size) {
        0 -> Artwork(null, 180, HeaderArtworkCorner)
        1 -> Artwork(coverArtworks[0], 180, HeaderArtworkCorner)
        else ->
            Box(
                Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(HeaderArtworkCorner))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column {
                    Row {
                        MosaicTile(coverArtworks[0])
                        MosaicTile(coverArtworks[1])
                    }
                    Row {
                        MosaicTile(coverArtworks[2 % coverArtworks.size])
                        MosaicTile(coverArtworks[3 % coverArtworks.size])
                    }
                }
            }
    }
}

@Composable
private fun MosaicTile(url: String) {
    Artwork(url, 90, 0.dp)
}
