package com.manishraj.saavnmusic.feature.player

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.WorkManager
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.formatDuration
import com.manishraj.saavnmusic.download.DownloadWorker
import com.manishraj.saavnmusic.ui.components.Artwork
import com.manishraj.saavnmusic.ui.components.rememberArtworkPalette
import com.manishraj.saavnmusic.ui.theme.OmegaRadius
import com.manishraj.saavnmusic.ui.theme.OmegaSpacing
import com.manishraj.saavnmusic.ui.theme.TabularTimeStyle

/**
 * Shared-element key for the current song's artwork. The mini-player
 * and the full player share an element only while both show the same
 * song, so a track change never morphs between two different artworks.
 */
private fun artworkSharedElementKey(songId: String): String = "artwork-$songId"

/**
 * The current song's artwork, shared between the mini-player and the
 * full player when [sharedTransitionScope] is present. A null scope is
 * the reduced-motion path (system animator duration scale 0, see
 * REDESIGN_SPEC §2.7): no spatial flight — the artwork crossfades with
 * the rest of its screen instead. The scale is read once in the app
 * root, so which branch runs never changes during a composition's life.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedArtwork(
    song: Song,
    size: Int,
    corner: Dp,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope,
    artworkBoundsTransform: BoundsTransform,
) {
    if (sharedTransitionScope == null) {
        Artwork(song.imageUrl, size, corner, contentDescription = song.name)
        return
    }
    with(sharedTransitionScope) {
        Artwork(
            song.imageUrl,
            size,
            corner,
            contentDescription = song.name,
            modifier =
                Modifier.sharedElement(
                    state =
                        rememberSharedContentState(
                            key = artworkSharedElementKey(song.id),
                        ),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = artworkBoundsTransform,
                ),
        )
    }
}

/**
 * Mini-player (REDESIGN_SPEC §3.2): sacred — anchored above the nav bar,
 * swipe/back never stops playback. Progress hairline on top, a fixed
 * 24dp buffering slot so the layout never shifts, 48dp targets.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MiniPlayer(
    vm: PlayerViewModel = hiltViewModel(),
    onOpen: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope,
    artworkBoundsTransform: BoundsTransform,
) {
    val st by vm.state.collectAsState()
    val cur = st.current ?: return
    // Artwork tint (UIUX_DESIGN §3.1.3): the container takes the
    // artwork's darkened color, crossfading 300 ms on track change.
    val palette = rememberArtworkPalette(cur.imageUrl)
    val containerColor by animateColorAsState(
        targetValue = palette.mutedDark,
        animationSpec = tween(durationMillis = 300),
        label = "miniPlayerContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = palette.onMutedDark,
        animationSpec = tween(durationMillis = 300),
        label = "miniPlayerContent",
    )
    Surface(onClick = onOpen, tonalElevation = 3.dp, color = containerColor) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Column {
                val progress =
                    if (st.durationMs > 0) {
                        (st.positionMs.toFloat() / st.durationMs).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(OmegaSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SharedArtwork(
                        song = cur,
                        size = 48,
                        corner = OmegaRadius.md,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        artworkBoundsTransform = artworkBoundsTransform,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            cur.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            cur.artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style =
                                if (LocalDensity.current.fontScale > 1.3f) {
                                    MaterialTheme.typography.bodyMedium
                                } else {
                                    MaterialTheme.typography.bodySmall
                                },
                        )
                    }
                    // Fixed-size slot: the spinner appears here without
                    // shifting the transport buttons (no layout jumping).
                    Box(
                        Modifier.size(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (st.isBuffering) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                    IconButton(onClick = { vm.player.prev() }) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
                    }
                    IconButton(onClick = { vm.player.playPause() }) {
                        Icon(
                            if (st.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (st.isPlaying) "Pause" else "Play",
                        )
                    }
                    IconButton(onClick = { vm.player.next() }) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next")
                    }
                }
            }
        }
    }
}

/**
 * Full player (spec §3.3): the play button is the screen's single
 * primary CTA (primary container, onPrimary glyph, 64dp); time labels
 * use tabular figures; queue is a sheet with an "Up next" header.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FullPlayer(
    vm: PlayerViewModel = hiltViewModel(),
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope,
    artworkBoundsTransform: BoundsTransform,
) {
    val st by vm.state.collectAsState()
    val cur = st.current
    val ctx = LocalContext.current
    if (cur == null) {
        Column(Modifier.padding(OmegaSpacing.xxl)) { Text("Nothing playing") }
        return
    }
    val fav by vm.isFavorite(cur.id).collectAsState(false)
    // Artwork gradient (UIUX_DESIGN §3.1.3): mutedDark at the top
    // crossfading 300 ms on track change, theme background at the
    // bottom. Header text/icons sit on the artwork color, so they use
    // the palette's contrast-checked on-color in both themes.
    val palette = rememberArtworkPalette(cur.imageUrl)
    val gradientTop by animateColorAsState(
        targetValue = palette.mutedDark,
        animationSpec = tween(durationMillis = 300),
        label = "playerGradientTop",
    )
    val artworkContentColor by animateColorAsState(
        targetValue = palette.onMutedDark,
        animationSpec = tween(durationMillis = 300),
        label = "playerArtworkContent",
    )
    val playerBrush =
        Brush.verticalGradient(
            listOf(gradientTop, MaterialTheme.colorScheme.background),
        )
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var lyrics by remember { mutableStateOf<String?>(null) }
    var lyricsLoaded by remember { mutableStateOf(false) }
    var sleep by remember { mutableIntStateOf(0) }
    LaunchedEffect(showLyrics, cur.id) {
        if (showLyrics) {
            lyricsLoaded = false
            vm.lyrics(cur.id) {
                lyrics = it
                lyricsLoaded = true
            }
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(playerBrush)
            .padding(OmegaSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(LocalContentColor provides artworkContentColor) {
            Row(Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Collapse player")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showQueue = true }) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue")
                }
            }
            SharedArtwork(
                song = cur,
                size = 300,
                corner = OmegaRadius.xl,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                artworkBoundsTransform = artworkBoundsTransform,
            )
            Spacer(Modifier.height(OmegaSpacing.xl))
            Text(
                cur.name,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                cur.artist,
                style = MaterialTheme.typography.bodyLarge,
                color = artworkContentColor,
            )
        }
        Spacer(Modifier.height(OmegaSpacing.lg))
        var scrub by remember { mutableStateOf<Float?>(null) }
        Slider(
            value =
                scrub ?: if (st.durationMs > 0) {
                    st.positionMs.toFloat() / st.durationMs
                } else {
                    0f
                },
            onValueChange = { scrub = it },
            onValueChangeFinished = {
                scrub?.let { vm.player.seekTo((it * st.durationMs).toLong()) }
                scrub = null
            },
        )
        Row(Modifier.fillMaxWidth()) {
            Text(formatDuration(st.positionMs / 1000), style = TabularTimeStyle)
            Spacer(Modifier.weight(1f))
            Text(
                formatDuration(if (st.durationMs > 0) st.durationMs / 1000 else cur.durationSec),
                style = TabularTimeStyle,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.player.toggleShuffle() }) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = if (st.shuffle) "Shuffle on" else "Shuffle off",
                    tint = if (st.shuffle) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                )
            }
            IconButton(onClick = { vm.player.prev() }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
            }
            FilledIconButton(
                onClick = { vm.player.playPause() },
                modifier = Modifier.size(64.dp),
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Icon(
                    if (st.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (st.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(32.dp),
                )
            }
            IconButton(onClick = { vm.player.next() }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = { vm.player.cycleRepeat() }) {
                Icon(
                    if (st.repeatMode == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription =
                        when (st.repeatMode) {
                            1 -> "Repeat all"
                            2 -> "Repeat one"
                            else -> "Repeat off"
                        },
                    tint = if (st.repeatMode != 0) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                )
            }
        }
        Row {
            IconButton(onClick = { vm.toggleFavorite(cur, fav) }) {
                Icon(
                    if (fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (fav) "Remove from favorites" else "Add to favorites",
                    tint = if (fav) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                )
            }
            IconButton(onClick = {
                DownloadWorker.enqueue(WorkManager.getInstance(ctx), cur, vm.appSettings.value.downloadQuality)
            }) {
                Icon(Icons.Filled.Download, contentDescription = "Download")
            }
            IconButton(onClick = { showLyrics = !showLyrics }) {
                Icon(Icons.Filled.Lyrics, contentDescription = "Lyrics")
            }
            IconButton(onClick = {
                val next =
                    when (sleep) {
                        0 -> 15
                        15 -> 30
                        30 -> 60
                        else -> 0
                    }
                sleep = next
                vm.player.setSleepTimer(next)
            }) {
                Icon(Icons.Filled.Bedtime, contentDescription = "Sleep timer")
            }
            if (sleep > 0) {
                Text(
                    "${sleep}m",
                    modifier = Modifier.align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Text("Speed", style = MaterialTheme.typography.bodySmall)
        // FlowRow so the chips wrap instead of overflowing on narrow
        // screens / large font sizes (same bug as the settings chips).
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(OmegaSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OmegaSpacing.sm),
        ) {
            listOf(0.75f, 1f, 1.25f, 1.5f).forEach { v ->
                FilterChip(
                    selected = st.speed == v,
                    onClick = { vm.player.setSpeed(v) },
                    label = { Text("${v}x", maxLines = 1, softWrap = false) },
                )
            }
        }
        if (showLyrics) {
            Spacer(Modifier.height(OmegaSpacing.md))
            Text(
                text =
                    when {
                        lyrics != null -> lyrics!!
                        !lyricsLoaded -> "Loading lyrics…"
                        // The lyrics call is the test (validation §3):
                        // a null result after it completes means none.
                        else -> "No lyrics available for this song"
                    },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    if (showQueue) {
        ModalBottomSheet(onDismissRequest = { showQueue = false }) {
            Text(
                "Up next",
                modifier = Modifier.padding(OmegaSpacing.lg),
                style = MaterialTheme.typography.titleMedium,
            )
            LazyColumn {
                items(st.queue) { s ->
                    ListItem(
                        headlineContent = { Text(s.name) },
                        supportingContent = { Text(s.artist) },
                        leadingContent = { Artwork(s.imageUrl, 44, OmegaRadius.md) },
                        modifier = Modifier.clickable { vm.player.playIndex(st.queue.indexOf(s)) },
                    )
                }
            }
        }
    }
}
