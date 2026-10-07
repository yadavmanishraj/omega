package com.manishraj.saavnmusic.feature.player

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconToggleButtonShapes
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.WorkManager
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.formatDuration
import com.manishraj.saavnmusic.download.DownloadWorker
import com.manishraj.saavnmusic.playback.PlayerState
import com.manishraj.saavnmusic.ui.components.Artwork
import com.manishraj.saavnmusic.ui.components.LocalOmegaSnackbar
import com.manishraj.saavnmusic.ui.components.OmegaChoiceGroup
import com.manishraj.saavnmusic.ui.components.OmegaFavoriteIcon
import com.manishraj.saavnmusic.ui.components.OmegaPlayPauseIcon
import com.manishraj.saavnmusic.ui.components.animatePaletteColor
import com.manishraj.saavnmusic.ui.components.rememberArtworkPalette
import com.manishraj.saavnmusic.ui.components.safeGradientEnd
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
 * Transport toggle shape morph (M3 Expressive spec §1.8 / §5): round
 * at rest, squared on press and while checked — the shape itself
 * reports the state, not just the tint. Shared by shuffle and repeat.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val transportToggleShapes =
    IconToggleButtonShapes(
        shape = CircleShape,
        pressedShape = RoundedCornerShape(8.dp),
        checkedShape = RoundedCornerShape(8.dp),
    )

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
                    sharedContentState =
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
 * swipe/back never stops playback. Progress hairline on top (wavy
 * while buffering — the media-surface wave, spec §5), a fixed 24dp
 * buffering slot so the layout never shifts, 48dp targets. Calm
 * surface: baseline type only, no emphasized twins (spec §2.2).
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
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
    // artwork's darkened color, crossfading on the shared palette
    // helper (spec §4.4) on track change.
    val palette = rememberArtworkPalette(cur.imageUrl)
    val containerColor by animatePaletteColor(
        targetValue = palette.mutedDark,
        label = "miniPlayerContainer",
    )
    val contentColor by animatePaletteColor(
        targetValue = palette.onMutedDark,
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
                // Fixed 4dp strip: the determinate hairline and the
                // buffering wave occupy the same slot, so the bar
                // never changes height when buffering starts/stops.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (st.isBuffering) {
                        LinearWavyProgressIndicator(Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(2.dp),
                        )
                    }
                }
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
                        OmegaPlayPauseIcon(isPlaying = st.isPlaying)
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
 * Expressive seek bar (spec §3/§5/§8): the stateful [SliderState]
 * slider from material3 1.5. Playback position drives the thumb while
 * the user is not touching it — assigned, never animated: the seek
 * value is clock data and springs are forbidden on it (spec §4.7).
 * TalkBack hears a time value text ("1:23 of 3:45") via
 * [stateDescription], closing the standing slider a11y gap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeekBar(
    st: PlayerState,
    fallbackDurationSec: Long?,
    onSeek: (Long) -> Unit,
) {
    val durationSec =
        if (st.durationMs > 0) st.durationMs / 1000 else fallbackDurationSec ?: 0L
    val sliderState: SliderState = rememberSliderState()
    var scrubbing by remember { mutableStateOf(false) }
    val playedFraction =
        if (st.durationMs > 0) {
            (st.positionMs.toFloat() / st.durationMs).coerceIn(0f, 1f)
        } else {
            0f
        }
    LaunchedEffect(playedFraction, scrubbing) {
        if (!scrubbing) {
            sliderState.value = playedFraction
        }
    }
    val shownSec = (sliderState.value * durationSec).toLong()
    Slider(
        state = sliderState,
        onValueChange = {
            sliderState.value = it
            scrubbing = true
        },
        onValueChangeFinished = {
            scrubbing = false
            onSeek((sliderState.value * st.durationMs).toLong())
        },
        modifier =
            Modifier.semantics {
                stateDescription = "${formatDuration(shownSec)} of ${formatDuration(durationSec)}"
            },
    )
}

/**
 * Full player (spec §5): THE hero surface. Title in
 * displaySmallEmphasized, the play button is the screen's single
 * primary CTA (palette rolePrimary fill, 72dp, press shape morph),
 * shuffle/repeat are expressive toggle buttons whose state is carried
 * by shape + [stateDescription] (not tint alone), and the secondary
 * cluster (favorite/download/lyrics/sleep/speed) is de-emphasized
 * below them. Time labels use tabular figures; queue is a sheet with
 * an "Up next" header.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class,
)
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
    val downloaded by vm.isDownloaded(cur.id).collectAsState(false)
    // Playback failures surface here (and once per failure as a
    // snackbar): the silent 0:00 player on an offline tap was a
    // phone-QA minor; the controller now reports the error honestly.
    val snackbar = LocalOmegaSnackbar.current
    LaunchedEffect(st.errorSeq) {
        if (st.errorSeq > 0) {
            snackbar?.showMessage("Couldn't play — check your connection")
        }
    }
    // Artwork gradient (UIUX_DESIGN §3.1.3): mutedDark at the top
    // crossfading on the shared palette helper (spec §4.4) on track
    // change, theme background at the bottom. Header text/icons sit
    // on the artwork color, so they use the palette's
    // contrast-checked on-color in both themes.
    val palette = rememberArtworkPalette(cur.imageUrl)
    val gradientTop by animatePaletteColor(
        targetValue = palette.mutedDark,
        label = "playerGradientTop",
    )
    val artworkContentColor by animatePaletteColor(
        targetValue = palette.onMutedDark,
        label = "playerArtworkContent",
    )
    // Palette ROLE colors (spec §1.3/§2.4): the play fill is the
    // artwork's primary role; active toggles and the downloaded state
    // speak in the tertiary role — no raw vibrant/dominant at call
    // sites.
    val playFill by animatePaletteColor(
        targetValue = palette.rolePrimary,
        label = "playerPlayFill",
    )
    val playContent by animatePaletteColor(
        targetValue = palette.onRolePrimary,
        label = "playerPlayContent",
    )
    val tertiaryAccent by animatePaletteColor(
        targetValue = palette.roleTertiary,
        label = "playerTertiaryAccent",
    )
    // Fade end must keep the content color at 4.5:1 (see
    // safeGradientEnd) — in light themes the title/artist washed
    // out over the near-white background end (UI/UX Phase B audit).
    val gradientEnd by animatePaletteColor(
        targetValue = safeGradientEnd(palette, MaterialTheme.colorScheme.background),
        label = "playerGradientEnd",
    )
    val playerBrush =
        Brush.verticalGradient(
            listOf(gradientTop, gradientEnd),
        )
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var lyrics by remember { mutableStateOf<String?>(null) }
    var lyricsLoaded by remember { mutableStateOf(false) }
    var sleep by remember { mutableIntStateOf(0) }
    val cycleSleep: () -> Unit = {
        val next =
            when (sleep) {
                0 -> 15
                15 -> 30
                30 -> 60
                else -> 0
            }
        sleep = next
        vm.player.setSleepTimer(next)
    }
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
            // Scrollable: at large font scales the fixed column
            // overflowed and the speed chips were clipped out of
            // the layout entirely (UI/UX Phase B audit — the chips
            // existed in code but never composed on the phone).
            .verticalScroll(rememberScrollState())
            .padding(OmegaSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The palette content color covers the WHOLE player, not
        // just the header: transport icons, time labels and section
        // labels outside the provider fell back to theme colors
        // and rendered dark-on-gradient in light theme.
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
                style = MaterialTheme.typography.displaySmallEmphasized,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                cur.artist,
                style = MaterialTheme.typography.titleMedium,
                color = artworkContentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (st.errorMessage != null) {
                Spacer(Modifier.height(OmegaSpacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(OmegaSpacing.xs))
                    Text(
                        "Couldn't play — check your connection",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = { vm.player.retry() }) {
                        Text("Retry")
                    }
                }
            }
            Spacer(Modifier.height(OmegaSpacing.lg))
            SeekBar(
                st = st,
                fallbackDurationSec = cur.durationSec,
                onSeek = { vm.player.seekTo(it) },
            )
            // Fixed-height buffering slot under the slider: the wavy
            // strip (spec §3 — the wave belongs on media surfaces)
            // appears while buffering without shifting the time row.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp),
            ) {
                if (st.isBuffering) {
                    LinearWavyProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            Row(Modifier.fillMaxWidth()) {
                Text(formatDuration(st.positionMs / 1000), style = TabularTimeStyle)
                Spacer(Modifier.weight(1f))
                Text(
                    formatDuration(if (st.durationMs > 0) st.durationMs / 1000 else cur.durationSec),
                    style = TabularTimeStyle,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconToggleButton(
                    checked = st.shuffle,
                    onCheckedChange = { vm.player.toggleShuffle() },
                    shapes = transportToggleShapes,
                    colors =
                        IconButtonDefaults.iconToggleButtonColors(
                            checkedContentColor = tertiaryAccent,
                        ),
                    modifier =
                        Modifier.semantics {
                            stateDescription = if (st.shuffle) "Shuffle on" else "Shuffle off"
                        },
                ) {
                    Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle")
                }
                IconButton(onClick = { vm.player.prev() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
                }
                FilledIconButton(
                    onClick = { vm.player.playPause() },
                    modifier = Modifier.size(72.dp),
                    shapes =
                        IconButtonShapes(
                            shape = CircleShape,
                            pressedShape = RoundedCornerShape(16.dp),
                        ),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = playFill,
                            contentColor = playContent,
                        ),
                ) {
                    OmegaPlayPauseIcon(isPlaying = st.isPlaying, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { vm.player.next() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
                }
                IconToggleButton(
                    checked = st.repeatMode != 0,
                    onCheckedChange = { vm.player.cycleRepeat() },
                    shapes = transportToggleShapes,
                    colors =
                        IconButtonDefaults.iconToggleButtonColors(
                            checkedContentColor = tertiaryAccent,
                        ),
                    modifier =
                        Modifier.semantics {
                            stateDescription =
                                when (st.repeatMode) {
                                    1 -> "Repeat all"
                                    2 -> "Repeat one"
                                    else -> "Repeat off"
                                }
                        },
                ) {
                    Icon(
                        if (st.repeatMode == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Repeat",
                    )
                }
            }
            Row {
                IconButton(onClick = { vm.toggleFavorite(cur, fav) }) {
                    OmegaFavoriteIcon(
                        isFavorite = fav,
                        tint = if (fav) tertiaryAccent else LocalContentColor.current,
                    )
                }
                IconButton(
                    onClick = {
                        if (!downloaded) {
                            DownloadWorker.enqueue(
                                WorkManager.getInstance(ctx),
                                cur,
                                vm.appSettings.value.downloadQuality,
                            )
                        }
                    },
                ) {
                    Icon(
                        if (downloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                        contentDescription = if (downloaded) "Downloaded" else "Download",
                        tint = if (downloaded) tertiaryAccent else LocalContentColor.current,
                    )
                }
                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(Icons.Filled.Lyrics, contentDescription = "Lyrics")
                }
                // Sleep timer, de-emphasized in the secondary cluster:
                // a plain icon while off; once running it becomes a
                // tonal chip carrying the set minutes, as before.
                if (sleep > 0) {
                    FilledTonalButton(
                        onClick = cycleSleep,
                        contentPadding =
                            PaddingValues(horizontal = OmegaSpacing.md, vertical = 0.dp),
                        modifier =
                            Modifier
                                .align(Alignment.CenterVertically)
                                .height(36.dp),
                    ) {
                        Icon(Icons.Filled.Bedtime, contentDescription = "Sleep timer", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(OmegaSpacing.xs))
                        Text("${sleep}m", maxLines = 1, softWrap = false)
                    }
                } else {
                    IconButton(onClick = cycleSleep) {
                        Icon(Icons.Filled.Bedtime, contentDescription = "Sleep timer")
                    }
                }
            }
            Text("Speed", style = MaterialTheme.typography.bodySmall)
            // Connected choice group (spec §3): replaces the FilterChip
            // row; options that cannot fit overflow into the group's
            // menu instead of crushing at large font scales.
            OmegaChoiceGroup(
                options = listOf(0.75f, 1f, 1.25f, 1.5f),
                selected = st.speed,
                onSelect = { vm.player.setSpeed(it) },
                label = { v -> "${v}x" },
            )
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
    }
    if (showQueue) {
        ModalBottomSheet(onDismissRequest = { showQueue = false }) {
            Text(
                "Up next",
                modifier = Modifier.padding(OmegaSpacing.lg),
                style = MaterialTheme.typography.titleMedium,
            )
            val currentIndex = st.queue.indexOfFirst { it.id == cur.id }
            LazyColumn {
                itemsIndexed(st.queue, key = { index, s -> "$index-${s.id}" }) { index, s ->
                    val isCurrent = index == currentIndex
                    // Expressive ListItem (alpha29): the headline is
                    // the trailing `content` lambda and selection is a
                    // first-class state — the classic headlineContent
                    // overload is deprecated.
                    ListItem(
                        modifier =
                            Modifier
                                .animateItem()
                                .clickable { vm.player.playIndex(index) }
                                .semantics {
                                    if (isCurrent) {
                                        stateDescription = "Now playing"
                                    }
                                },
                        selected = isCurrent,
                        leadingContent = { Artwork(s.imageUrl, 44, OmegaRadius.md) },
                        trailingContent =
                            if (isCurrent) {
                                {
                                    Icon(
                                        Icons.Filled.GraphicEq,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            } else {
                                null
                            },
                        supportingContent = { Text(s.artist) },
                        colors =
                            if (isCurrent) {
                                ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                )
                            } else {
                                ListItemDefaults.colors()
                            },
                    ) {
                        Text(s.name)
                    }
                }
            }
        }
    }
}
