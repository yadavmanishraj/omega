package com.manishraj.saavnmusic.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.UiState
import com.manishraj.saavnmusic.playback.InsertNextResult
import com.manishraj.saavnmusic.ui.components.EditorialRowDivider
import com.manishraj.saavnmusic.ui.components.EmptyState
import com.manishraj.saavnmusic.ui.components.ErrorState
import com.manishraj.saavnmusic.ui.components.InlineErrorRow
import com.manishraj.saavnmusic.ui.components.LocalOmegaSnackbar
import com.manishraj.saavnmusic.ui.components.MediaCard
import com.manishraj.saavnmusic.ui.components.OmegaEyebrow
import com.manishraj.saavnmusic.ui.components.OmegaLoadingIndicator
import com.manishraj.saavnmusic.ui.components.OmegaRailEdgeFade
import com.manishraj.saavnmusic.ui.components.OmegaSectionLabel
import com.manishraj.saavnmusic.ui.components.PlaylistPickerDialog
import com.manishraj.saavnmusic.ui.components.RankedSongRow
import com.manishraj.saavnmusic.ui.components.ShimmerList
import com.manishraj.saavnmusic.ui.components.ShimmerRail
import com.manishraj.saavnmusic.ui.components.SongOverflowMenuButton
import com.manishraj.saavnmusic.ui.components.SongRow
import com.manishraj.saavnmusic.ui.theme.OmegaSpacing
import com.manishraj.saavnmusic.ui.theme.OmegaType
import java.util.Calendar

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

private fun greetingForHour(hour: Int): String =
    when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good listening"
    }

/**
 * Divider inset for the ranked chart (uplift §5.2, coordinator
 * ruling 2026-10-08): a [RankedSongRow]'s text column starts at 16dp
 * screen inset + 24dp rank column + 12dp gap + 44dp artwork + 12dp
 * gap = 108dp. The spec's §5.2 decomposition names 72dp — it omits
 * the rank column; the ruling holds that text-column alignment
 * governs, so the hairline starts where the text starts.
 */
private val RankedDividerInset = 108.dp

/**
 * A Home section header in the Editorial grammar (uplift §5.4,
 * §4.1): the kit [OmegaSectionLabel] placed on the 16dp line by the
 * caller-side padding (the kit adds no inset of its own — nested
 * insets are how the stair-step happened, rulebook LY-2), the 24dp
 * section gap above it, and the 12dp label-to-content gap below.
 * Emitted as two column items so every section — loaded, loading,
 * or errored — carries identical rhythm (rulebook LY-6).
 */
private fun LazyListScope.homeSection(text: String) {
    item {
        OmegaSectionLabel(
            text,
            Modifier.padding(
                start = OmegaSpacing.lg,
                end = OmegaSpacing.lg,
                top = OmegaSpacing.xl,
            ),
        )
    }
    item { Spacer(Modifier.height(OmegaSpacing.md)) }
}

/**
 * A Home rail in the §4.2 grammar: cards directly on the page
 * background — 16dp content padding (the first card lands on the
 * section line), 12dp item gaps, and the standard edge fade so a
 * partially visible card dissolves into the background instead of
 * slicing at an edge. No container, ever (TELL-FIVE-BACKGROUNDS,
 * TELL-SLICED-RAIL).
 */
@Composable
private fun HomeRail(content: LazyListScope.() -> Unit) {
    Box {
        LazyRow(
            contentPadding = PaddingValues(horizontal = OmegaSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(OmegaSpacing.md),
            content = content,
        )
        OmegaRailEdgeFade(Modifier.align(Alignment.CenterEnd))
    }
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
    onDownloadEnqueued: () -> Unit = {},
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
    val snackbar = LocalOmegaSnackbar.current
    var playlistTarget by remember { mutableStateOf<Song?>(null) }

    val refreshing = trending is UiState.Loading
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { vm.load() },
        state = pullToRefreshState,
        // The expressive loading morph replaces the stock refresh
        // spinner (spec §5): a contained chip fades/scales in with the
        // pull fraction and stays while the refresh runs.
        indicator = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 3.dp,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = OmegaSpacing.md)
                        .graphicsLayer {
                            val fraction =
                                if (refreshing) {
                                    1f
                                } else {
                                    pullToRefreshState.distanceFraction.coerceIn(0f, 1f)
                                }
                            alpha = fraction
                            scaleX = 0.7f + 0.3f * fraction
                            scaleY = 0.7f + 0.3f * fraction
                        },
            ) {
                OmegaLoadingIndicator(
                    modifier = Modifier.padding(OmegaSpacing.sm),
                    contained = true,
                    contentDescription = "Refreshing",
                )
            }
        },
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = OmegaSpacing.lg, end = OmegaSpacing.lg, top = OmegaSpacing.xl),
                ) {
                    Text(
                        greetingForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)),
                        // The screen's one emphasis (uplift §2.3):
                        // the Editorial masthead — Poppins Bold with
                        // tight tracking; Righteous leaves the Home
                        // header. The 40dp badge that shared this
                        // header is deleted (uplift §10.4): the
                        // masthead owns the header, no replacement
                        // chrome.
                        style = OmegaType.masthead,
                    )
                    Spacer(Modifier.height(OmegaSpacing.xs))
                    // The kit eyebrow applies the tracked-uppercase
                    // treatment; the copy stays natural case (§2.2).
                    OmegaEyebrow("No account. Just music.")
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
                            "You're offline. Showing downloads & library",
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
                // "Jump back in" is Home's most important rail: it
                // leads the page directly under the masthead. The
                // polish-era slab — a grouped container behind the
                // rail — is deleted outright (uplift §4.2/§5.3): the
                // rail sits on the page background like every other
                // rail, and its cards fade at the edge instead of
                // slicing against the container. Tap behavior is
                // unchanged.
                homeSection("Jump back in")
                item {
                    HomeRail {
                        items(history, key = { it.id }) { song ->
                            Box(Modifier.animateItem()) {
                                MediaCard(song.name, song.artist, song.imageUrl) {
                                    onPlayQueue(history, history.indexOf(song))
                                }
                            }
                        }
                    }
                }
            }

            if (!online) {
                // Offline variant: local content only; remote sections are
                // hidden rather than errored (spec §4).
                if (downloads.isNotEmpty()) {
                    homeSection("Your downloads")
                    item {
                        HomeRail {
                            items(downloads, key = { it.songId }) { d ->
                                Box(Modifier.animateItem()) {
                                    MediaCard(d.name, d.artist, d.imageUrl) { onOpenDownloads() }
                                }
                            }
                        }
                    }
                }
                if (favorites.isNotEmpty()) {
                    homeSection("Your favorites")
                    items(favorites.take(10), key = { it.id }) { song ->
                        Box(Modifier.animateItem()) {
                            SongRow(
                                song,
                                { onPlayQueue(favorites, favorites.indexOf(song)) },
                                trailing = {
                                    SongOverflowMenuButton(
                                        song = song,
                                        isFavorite = true,
                                        onPlayNext = {
                                            snackbar?.showMessage(
                                                playNextMessage(vm.playNext(song), song.name),
                                            )
                                        },
                                        onDownload = {
                                            vm.download(song)
                                            onDownloadEnqueued()
                                            snackbar?.showMessage("Download queued")
                                        },
                                        onToggleFavorite = { vm.toggleFavorite(song, true) },
                                        onAddToPlaylist = { playlistTarget = song },
                                    )
                                },
                            )
                        }
                    }
                }
                if (downloads.isEmpty() && favorites.isEmpty() && history.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Nothing downloaded yet",
                            subtitle = "When you're back online, download songs to listen offline.",
                            actionLabel = "Go to Downloads",
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
                            homeSection("Trending songs")
                            item { ShimmerList() }
                        }
                        is UiState.Error -> {
                            homeSection("Trending songs")
                            item { InlineErrorRow(s.message, onRetry = { vm.load() }) }
                        }
                        is UiState.Success -> {
                            if (s.data.isNotEmpty()) {
                                homeSection("Trending songs")
                                // The Editorial chart (uplift §5.2):
                                // explicit ranks by chart position,
                                // hairlines between rows, never after
                                // the last. Play behavior is identical
                                // — a tap plays the same queue from the
                                // same index, and the trailing menu is
                                // the shared overflow menu unchanged.
                                val chart = s.data.take(10)
                                itemsIndexed(chart, key = { _, song -> song.id }) { index, song ->
                                    val songIsFavorite = favorites.any { it.id == song.id }
                                    Column(Modifier.animateItem()) {
                                        RankedSongRow(
                                            song,
                                            rank = index + 1,
                                            onClick = { onPlayQueue(s.data, s.data.indexOf(song)) },
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
                                                        onDownloadEnqueued()
                                                        snackbar?.showMessage("Download queued")
                                                    },
                                                    onToggleFavorite = { vm.toggleFavorite(song, songIsFavorite) },
                                                    onAddToPlaylist = { playlistTarget = song },
                                                )
                                            },
                                        )
                                        if (index < chart.lastIndex) {
                                            EditorialRowDivider(startInset = RankedDividerInset)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    when (val s = albums) {
                        is UiState.Loading -> {
                            homeSection("New albums")
                            item { ShimmerRail() }
                        }
                        is UiState.Error -> {
                            homeSection("New albums")
                            item { InlineErrorRow(s.message, onRetry = { vm.load() }) }
                        }
                        is UiState.Success -> {
                            if (s.data.isNotEmpty()) {
                                homeSection("New albums")
                                item {
                                    HomeRail {
                                        items(s.data, key = { it.id }) { a ->
                                            Box(Modifier.animateItem()) {
                                                MediaCard(a.name, a.artist, a.imageUrl) { onAlbum(a.id) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    when (val s = playlists) {
                        is UiState.Loading -> {
                            homeSection("Playlists for you")
                            item { ShimmerRail() }
                        }
                        is UiState.Error -> {
                            homeSection("Playlists for you")
                            item { InlineErrorRow(s.message, onRetry = { vm.load() }) }
                        }
                        is UiState.Success -> {
                            if (s.data.isNotEmpty()) {
                                homeSection("Playlists for you")
                                item {
                                    HomeRail {
                                        items(s.data, key = { it.id }) { p ->
                                            Box(Modifier.animateItem()) {
                                                MediaCard(p.name, p.description.orEmpty(), p.imageUrl) { onPlaylist(p.id) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    when (val s = artists) {
                        is UiState.Loading -> {
                            homeSection("Artists")
                            item { ShimmerRail() }
                        }
                        is UiState.Error -> {
                            homeSection("Artists")
                            item { InlineErrorRow(s.message, onRetry = { vm.load() }) }
                        }
                        is UiState.Success -> {
                            if (s.data.isNotEmpty()) {
                                homeSection("Artists")
                                item {
                                    HomeRail {
                                        items(s.data, key = { it.id }) { a ->
                                            Box(Modifier.animateItem()) {
                                                MediaCard(a.name, "Artist", a.imageUrl, circular = true) { onArtist(a.id) }
                                            }
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

    // Picker confirmation rides the shell snackbar (spec §7),
    // matching Detail's Wave 1 proof wiring.
    playlistTarget?.let { song ->
        PlaylistPickerDialog(
            playlists = localPlaylists,
            targetSong = song,
            onPick = { playlist ->
                vm.addToPlaylist(playlist.id, song)
                playlistTarget = null
                snackbar?.showMessage("Added to ${playlist.name}")
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
