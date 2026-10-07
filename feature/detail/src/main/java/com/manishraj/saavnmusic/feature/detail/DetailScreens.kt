package com.manishraj.saavnmusic.feature.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.UiState
import com.manishraj.saavnmusic.playback.InsertNextResult
import com.manishraj.saavnmusic.ui.components.Artwork
import com.manishraj.saavnmusic.ui.components.EmptyState
import com.manishraj.saavnmusic.ui.components.ErrorState
import com.manishraj.saavnmusic.ui.components.GradientHeader
import com.manishraj.saavnmusic.ui.components.LocalOmegaSnackbar
import com.manishraj.saavnmusic.ui.components.MediaCard
import com.manishraj.saavnmusic.ui.components.OmegaActionGroup
import com.manishraj.saavnmusic.ui.components.PlaylistPickerDialog
import com.manishraj.saavnmusic.ui.components.SectionHeader
import com.manishraj.saavnmusic.ui.components.ShimmerList
import com.manishraj.saavnmusic.ui.components.SongOverflowMenuButton
import com.manishraj.saavnmusic.ui.components.SongRow
import com.manishraj.saavnmusic.ui.components.compactCount
import com.manishraj.saavnmusic.ui.components.songCountLabel
import com.manishraj.saavnmusic.ui.theme.LocalReducedMotion

/**
 * Corner for the Detail header artwork: the shape language's
 * extraLarge slot (28dp — `OmegaShapes.extraLarge`, spec §2.3),
 * matching the player hero. [Artwork] takes a Dp corner, so the
 * value is spelled out here; the two must move together.
 */
private val HeaderArtworkCorner = 28.dp

@Composable
fun AlbumScreen(
    id: String,
    vm: DetailViewModel = hiltViewModel(),
    onPlayQueue: (List<Song>, Int) -> Unit,
    onBack: () -> Unit,
    onDownloadEnqueued: () -> Unit = {},
) {
    LaunchedEffect(id) { vm.loadAlbum(id) }
    val s by vm.album.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val snackbar = LocalOmegaSnackbar.current
    val requestAddToPlaylist = rememberPlaylistPicker(vm)
    DetailList(
        s,
        { it.songs },
        { a -> SongListHeader(a.name, a.artist, a.imageUrl, a.description, onBack) },
        onPlayQueue,
        favorites = favorites,
        onPlayNext = { song ->
            snackbar?.showMessage(playNextMessage(vm.playNext(song), song.name))
        },
        onAddToPlaylist = requestAddToPlaylist,
        onDownload = { song ->
            vm.download(song)
            onDownloadEnqueued()
            snackbar?.showMessage("Download queued")
        },
        onToggleFavorite = { song, isFav -> vm.toggleFavorite(song, isFav) },
        onBack = onBack,
    ) { vm.loadAlbum(id) }
}

@Composable
fun PlaylistScreen(
    id: String,
    vm: DetailViewModel = hiltViewModel(),
    onPlayQueue: (List<Song>, Int) -> Unit,
    onBack: () -> Unit,
    onDownloadEnqueued: () -> Unit = {},
) {
    LaunchedEffect(id) { vm.loadPlaylist(id) }
    val s by vm.playlist.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val snackbar = LocalOmegaSnackbar.current
    val requestAddToPlaylist = rememberPlaylistPicker(vm)
    DetailList(
        s,
        { it.songs },
        { p ->
            // The subtitle carries scope like every other header in
            // the app (F-20): "Playlist · 45 songs", from the page's
            // own count, falling back to the loaded list.
            val count = p.songCount ?: p.songs.size
            SongListHeader(
                p.name,
                if (count > 0) "Playlist · ${songCountLabel(count)}" else "Playlist",
                p.imageUrl,
                p.description,
                onBack,
            )
        },
        onPlayQueue,
        favorites = favorites,
        onPlayNext = { song ->
            snackbar?.showMessage(playNextMessage(vm.playNext(song), song.name))
        },
        onAddToPlaylist = requestAddToPlaylist,
        onDownload = { song ->
            vm.download(song)
            onDownloadEnqueued()
            snackbar?.showMessage("Download queued")
        },
        onToggleFavorite = { song, isFav -> vm.toggleFavorite(song, isFav) },
        onBack = onBack,
    ) { vm.loadPlaylist(id) }
}

/**
 * Play-next snackbar copy (spec §3/§7): with a live queue the song
 * is inserted after the current track ("Will play next"), but with
 * nothing playing the engine APPENDS it without starting playback —
 * the copy must not promise "next" in that case.
 */
private fun playNextMessage(
    result: InsertNextResult,
    title: String,
): String =
    when (result) {
        InsertNextResult.INSERTED_NEXT -> "Will play next: $title"
        InsertNextResult.APPENDED -> "Added to queue: $title"
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

/**
 * Back affordance for Detail's Loading / Error states (F-15): the
 * success header carries its arrow inside the gradient, but these
 * states render before any header exists — without this the user
 * had NO visible exit during load or after a failure. Same icon,
 * same "Back" description, same top-left placement as the header's.
 */
@Composable
private fun DetailBackBar(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
        )
    }
}

@Composable
fun <T> DetailList(
    state: UiState<T>,
    songs: (T) -> List<Song>,
    header: @Composable (T) -> Unit,
    play: (List<Song>, Int) -> Unit,
    favorites: List<Song>,
    onPlayNext: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onDownload: (Song) -> Unit,
    onToggleFavorite: (Song, Boolean) -> Unit,
    onBack: () -> Unit,
    retry: () -> Unit,
) {
    when (state) {
        is UiState.Loading ->
            Column {
                DetailBackBar(onBack)
                ShimmerList()
            }
        is UiState.Error ->
            Column {
                DetailBackBar(onBack)
                ErrorState(state.message, retry)
            }
        is UiState.Success -> {
            val list = songs(state.data)
            LazyColumn {
                item { header(state.data) }
                if (list.isEmpty()) {
                    // Upstream can return a detail with zero songs:
                    // header over the shared empty state, not a bare
                    // header over void with a dead Play all.
                    item {
                        EmptyState(
                            title = "No songs here yet",
                            subtitle = "This list came back empty. Try again in a bit.",
                        )
                    }
                } else {
                    item {
                        // THE screen's primary action cluster (spec
                        // §5): Play all (filled M) + Shuffle (tonal M)
                        // as one connected group. The horizontal
                        // scroll is a guard only — at font scale 2.0
                        // the M labels must scroll, never crush.
                        OmegaActionGroup(
                            primaryLabel = "Play all",
                            onPrimary = { play(list, 0) },
                            secondaryLabel = "Shuffle",
                            onSecondary = { play(list.shuffled(), 0) },
                            primaryIcon = Icons.Filled.PlayArrow,
                            secondaryIcon = Icons.Filled.Shuffle,
                            modifier =
                                Modifier
                                    .padding(16.dp)
                                    .horizontalScroll(rememberScrollState()),
                        )
                    }
                    // Stable keys (F-29): unkeyed items make
                    // animateItem / item state positional, so a
                    // list change animates the WRONG rows. Indexed
                    // section-prefixed keys — bare song ids are NOT
                    // unique within a list upstream.
                    itemsIndexed(
                        list,
                        key = { index, song -> "song-$index-${song.id}" },
                    ) { index, song ->
                        val isFavorite = favorites.any { it.id == song.id }
                        SongRow(
                            song,
                            { play(list, index) },
                            trailing = {
                                SongOverflowMenuButton(
                                    song = song,
                                    isFavorite = isFavorite,
                                    onPlayNext = { onPlayNext(song) },
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
            Artwork(image, 180, HeaderArtworkCorner)
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                // Emphasized twin of the slot the header already
                // used (spec §2.2): same 24sp size as headlineSmall,
                // emphasis arrives as weight/family — no reflow at
                // font scale 1.33/2.0. titleLargeEmphasized (22sp)
                // would demote the half-hero's title under its
                // 180dp artwork.
                style = MaterialTheme.typography.headlineSmallEmphasized,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            // Upstream's baked header string repeats the subtitle's
            // artists and can be pure credit metadata — clean it
            // against the subtitle before display (see
            // HeaderDescriptionText). A fully-metadata description
            // cleans to null and the header hides it, exactly as it
            // already hides a blank one.
            val cleanedDesc = cleanHeaderDescription(desc, subtitle)
            if (!cleanedDesc.isNullOrBlank()) {
                HeaderDescription(cleanedDesc)
            }
        }
    }
}

/**
 * Header description, capped at 3 lines until the reader asks for
 * more (spec §5: descriptions used to truncate silently). The
 * "Read more" affordance only exists when the text actually
 * overflows the cap — measured, not guessed — and the size change
 * animates on the theme's default SPATIAL spec (spatial properties
 * may spring; snapped under reduced motion, per spec §2.5). The
 * affordance wears the header's contrast-checked content color, not
 * the theme primary: on the gradient's dark band (light theme) the
 * primary would fail the 4.5:1 check the header guarantees.
 */
@Composable
private fun HeaderDescription(text: String) {
    var expanded by remember(text) { mutableStateOf(false) }
    var overflows by remember(text) { mutableStateOf(false) }
    val sizeSpec: FiniteAnimationSpec<IntSize> =
        if (LocalReducedMotion.current) {
            snap()
        } else {
            MaterialTheme.motionScheme.defaultSpatialSpec()
        }
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        maxLines = if (expanded) Int.MAX_VALUE else 3,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        onTextLayout = { overflows = it.hasVisualOverflow },
        // Bounded to the header's padded content box (F-03): this
        // was the one header text with NO width constraint of its
        // own, and an unconstrained centered paragraph in the
        // gradient column laid out over-wide — its first glyphs
        // landed off the left screen edge ("ongs in Hindi.") while
        // every sibling text (all width-bounded) rendered centered
        // correctly. fillMaxWidth + the horizontal inset pin the
        // paragraph to the same box as the title above it.
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .animateContentSize(sizeSpec),
    )
    if (overflows || expanded) {
        TextButton(
            onClick = { expanded = !expanded },
            colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
        ) {
            Text(if (expanded) "Show less" else "Read more")
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
    onDownloadEnqueued: () -> Unit = {},
) {
    LaunchedEffect(id) { vm.loadArtist(id) }
    val s by vm.artist.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val snackbar = LocalOmegaSnackbar.current
    val requestAddToPlaylist = rememberPlaylistPicker(vm)
    when (val a = s) {
        is UiState.Loading ->
            Column {
                DetailBackBar(onBack)
                ShimmerList()
            }
        is UiState.Error ->
            Column {
                DetailBackBar(onBack)
                ErrorState(a.message, onRetry = { vm.loadArtist(id) })
            }
        is UiState.Success ->
            LazyColumn {
                item {
                    SongListHeader(
                        a.data.name,
                        listOfNotNull(a.data.followers?.let { "${compactCount(it)} followers" }).joinToString(),
                        a.data.imageUrl,
                        a.data.bio,
                        onBack,
                    )
                }
                // Sections render ONLY when they have content
                // (F-14): the headers used to be unconditional, so
                // an artist with no top songs showed a "Top songs"
                // header over nothing. The artist page also gains
                // the Play all / Shuffle cluster every other Detail
                // surface has, and the SINGLES the mapper used to
                // drop (see Artist.singles).
                if (a.data.topSongs.isNotEmpty()) {
                    item { SectionHeader("Top songs") }
                    item {
                        OmegaActionGroup(
                            primaryLabel = "Play all",
                            onPrimary = { onPlayQueue(a.data.topSongs, 0) },
                            secondaryLabel = "Shuffle",
                            onSecondary = { onPlayQueue(a.data.topSongs.shuffled(), 0) },
                            primaryIcon = Icons.Filled.PlayArrow,
                            secondaryIcon = Icons.Filled.Shuffle,
                            modifier =
                                Modifier
                                    .padding(16.dp)
                                    .horizontalScroll(rememberScrollState()),
                        )
                    }
                    itemsIndexed(
                        a.data.topSongs,
                        key = { index, song -> "topsong-$index-${song.id}" },
                    ) { index, song ->
                        ArtistSongRow(
                            song = song,
                            queue = a.data.topSongs,
                            index = index,
                            favorites = favorites,
                            onPlayQueue = onPlayQueue,
                            onPlayNext = { s2 ->
                                snackbar?.showMessage(playNextMessage(vm.playNext(s2), s2.name))
                            },
                            onDownload = { s2 ->
                                vm.download(s2)
                                onDownloadEnqueued()
                                snackbar?.showMessage("Download queued")
                            },
                            onToggleFavorite = { s2, isFav -> vm.toggleFavorite(s2, isFav) },
                            onAddToPlaylist = requestAddToPlaylist,
                        )
                    }
                }
                if (a.data.singles.isNotEmpty()) {
                    item { SectionHeader("Singles") }
                    itemsIndexed(
                        a.data.singles,
                        key = { index, song -> "single-$index-${song.id}" },
                    ) { index, song ->
                        ArtistSongRow(
                            song = song,
                            queue = a.data.singles,
                            index = index,
                            favorites = favorites,
                            onPlayQueue = onPlayQueue,
                            onPlayNext = { s2 ->
                                snackbar?.showMessage(playNextMessage(vm.playNext(s2), s2.name))
                            },
                            onDownload = { s2 ->
                                vm.download(s2)
                                onDownloadEnqueued()
                                snackbar?.showMessage("Download queued")
                            },
                            onToggleFavorite = { s2, isFav -> vm.toggleFavorite(s2, isFav) },
                            onAddToPlaylist = requestAddToPlaylist,
                        )
                    }
                }
                if (a.data.topAlbums.isNotEmpty()) {
                    item { SectionHeader("Top albums") }
                    item {
                        LazyRow {
                            itemsIndexed(
                                a.data.topAlbums,
                                key = { index, al -> "album-$index-${al.id}" },
                            ) { _, al ->
                                MediaCard(al.name, al.artist, al.imageUrl) { onAlbum(al.id) }
                            }
                        }
                    }
                }
            }
    }
}

/**
 * One artist-page song row — top songs and singles share it: the
 * same [SongRow] + full overflow menu the other Detail surfaces
 * use, playing within the section's own list.
 */
@Composable
private fun ArtistSongRow(
    song: Song,
    queue: List<Song>,
    index: Int,
    favorites: List<Song>,
    onPlayQueue: (List<Song>, Int) -> Unit,
    onPlayNext: (Song) -> Unit,
    onDownload: (Song) -> Unit,
    onToggleFavorite: (Song, Boolean) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
) {
    val isFavorite = favorites.any { it.id == song.id }
    SongRow(
        song,
        { onPlayQueue(queue, index) },
        trailing = {
            SongOverflowMenuButton(
                song = song,
                isFavorite = isFavorite,
                onPlayNext = { onPlayNext(song) },
                onDownload = { onDownload(song) },
                onToggleFavorite = { onToggleFavorite(song, isFavorite) },
                onAddToPlaylist = { onAddToPlaylist(song) },
            )
        },
    )
}
