package com.manishraj.saavnmusic.feature.player

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.WorkManager
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.formatDuration
import com.manishraj.saavnmusic.download.DownloadWorker
import com.manishraj.saavnmusic.playback.PlayerState
import com.manishraj.saavnmusic.playback.RepeatMode
import com.manishraj.saavnmusic.playback.SLEEP_TIMER_PRESETS
import com.manishraj.saavnmusic.playback.repeatModeFromEngine
import com.manishraj.saavnmusic.ui.components.Artwork
import com.manishraj.saavnmusic.ui.components.EmptyState
import com.manishraj.saavnmusic.ui.components.LocalOmegaSnackbar
import com.manishraj.saavnmusic.ui.components.OmegaFavoriteIcon
import com.manishraj.saavnmusic.ui.components.OmegaPlayPauseIcon
import com.manishraj.saavnmusic.ui.components.OmegaSegmentedContainer
import com.manishraj.saavnmusic.ui.components.animatePaletteColor
import com.manishraj.saavnmusic.ui.components.rememberArtworkPalette
import com.manishraj.saavnmusic.ui.components.rememberChromeWashBrush
import com.manishraj.saavnmusic.ui.components.safeGradientEnd
import com.manishraj.saavnmusic.ui.theme.OmegaMotion
import com.manishraj.saavnmusic.ui.theme.OmegaRadius
import com.manishraj.saavnmusic.ui.theme.OmegaSpacing
import com.manishraj.saavnmusic.ui.theme.OmegaType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shared-element key for the current song's artwork. The mini-player
 * and the full player share an element only while both show the same
 * song, so a track change never morphs between two different artworks.
 */
private fun artworkSharedElementKey(songId: String): String = "artwork-$songId"

/**
 * How long the mini-player waits before claiming a playback error
 * for its snackbar: one beat past the shell's mini-player exit
 * animation ([OmegaMotion.EXIT_MS]), so expanding the player hands
 * the error to the full player's inline row instead of racing it.
 * See [ErrorChannelArbiter].
 */
private const val ERROR_SNACKBAR_SETTLE_MS: Long = OmegaMotion.EXIT_MS + 100L

/** Playback-speed choices — the set the old permanent chip row offered. */
private val SPEED_OPTIONS: List<Float> = listOf(0.75f, 1f, 1.25f, 1.5f)

/** The speed label format the old chip row used: "0.75x", "1.0x", "1.25x", "1.5x". */
private fun speedLabel(v: Float): String = "${v}x"

/**
 * The current song's artwork, shared between the mini-player and the
 * full player when [sharedTransitionScope] is present. A null scope is
 * the reduced-motion path (system animator duration scale 0, see
 * REDESIGN_SPEC §2.7): no spatial flight — the artwork crossfades with
 * the rest of its screen instead. Which branch runs follows the app
 * root's reactive reduced-motion state (the theme's
 * LocalReducedMotion provider, Wave 3), so a mid-session system
 * setting change swaps the branch when the user returns to the app.
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
 * swipe/back never stops playback. 48dp transport targets. Calm
 * surface: baseline type only, no emphasized twins (spec §2.2).
 *
 * Uplift construction (visual-uplift spec §5.5): the bar is CHROME
 * now, not an artwork surface. The container is the chrome role
 * `surfaceContainer`; artwork color survives only as the §3.3 wash —
 * the kit's chrome-tint brush behind the row, artwork-side 40% —
 * retargeted through the shared palette crossfade (§3.5). Progress
 * is the §3.4 hairline: 2dp on the chrome surface (indicator
 * `primary`, track `surfaceContainerHighest` — never raw primary on
 * a tinted ground), and buffering swaps to the wavy variant in the
 * same two colors, in the same fixed slot, so the bar never changes
 * height. The §4.3 chrome divider above the bar is the shell's to
 * draw, not this composable's.
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
    // Playback-error channel for the collapsed context (Task 4):
    // while only this surface is composed there is no inline error
    // row, so the snackbar is the one announcement — claimed via
    // the ViewModel's arbiter after a short settle, so an in-flight
    // expansion lets the full player present the error inline
    // instead (see ErrorChannelArbiter).
    val snackbar = LocalOmegaSnackbar.current
    LaunchedEffect(st.errorSeq) {
        val seq = st.errorSeq
        if (seq > 0) {
            delay(ERROR_SNACKBAR_SETTLE_MS)
            if (vm.claimErrorForSnackbar(seq)) {
                snackbar?.showMessage("Couldn't play. Check your connection.")
            }
        }
    }
    val cur = st.current ?: return
    // Artwork wash (uplift §3.3): the palette feeds ONLY the wash
    // brush now — the bar's fill is the chrome role below, and the
    // container color no longer animates (§3.5).
    val palette = rememberArtworkPalette(cur.imageUrl)
    val scheme = MaterialTheme.colorScheme
    // The theme-mode decision lives in MainActivity and is not
    // exposed to features; the wash band is picked for the chrome
    // surface it actually sits on — surfaceContainer's luminance
    // splits cleanly (dark ≈ 0.01, light ≈ 0.85) in every scheme
    // this app applies, static or dynamic.
    val darkTheme = scheme.surfaceContainer.luminance() < 0.5f
    val washBrush = rememberChromeWashBrush(palette, darkTheme)
    // Chrome container (uplift §5.5): shape and tonalElevation keep
    // their Surface defaults — RectangleShape ("none") and 0. The
    // old explicit tonalElevation = 3.dp tinted nothing over the
    // palette fill (dead elevation) and is deleted, not re-valued;
    // separation comes from the role plus the shell's chrome
    // divider, not elevation.
    Surface(onClick = onOpen, color = scheme.surfaceContainer) {
        Column {
            val progress =
                if (st.durationMs > 0) {
                    (st.positionMs.toFloat() / st.durationMs).coerceIn(0f, 1f)
                } else {
                    0f
                }
            // Fixed 2dp strip (§3.4/§5.5): the determinate hairline
            // and the buffering wave occupy the same slot in the
            // same two colors, so the bar never changes height when
            // buffering starts/stops. The 2dp is the spec's mandated
            // indicator thickness (no OmegaSpacing token exists at
            // 2dp; the kit's own 2dp gap in OmegaSegmentedList is
            // likewise a literal).
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (st.isBuffering) {
                    LinearWavyProgressIndicator(
                        color = scheme.primary,
                        trackColor = scheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = scheme.primary,
                        trackColor = scheme.surfaceContainerHighest,
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
                    .background(washBrush)
                    .padding(OmegaSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SharedArtwork(
                    song = cur,
                    size = 40,
                    corner = OmegaRadius.md,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    artworkBoundsTransform = artworkBoundsTransform,
                )
                Spacer(Modifier.width(OmegaSpacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        cur.name,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        cur.artist,
                        // Meta de-emphasis (uplift §2.4): the artist
                        // line takes onSurfaceVariant — it used to
                        // render at the title's full brightness. The
                        // fontScale > 1.3 bump to bodyMedium is
                        // preserved (§6).
                        color = scheme.onSurfaceVariant,
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

/** Synchronous (non-snapshot) scrub flag for [SeekBar]; see its use site. */
private class ScrubFlag {
    var active: Boolean = false
}

/**
 * Expressive seek bar (spec §3/§5/§8): the stateful [SliderState]
 * slider from material3 1.5. Playback position drives the thumb while
 * the user is not touching it — assigned, never animated: the seek
 * value is clock data and springs are forbidden on it (spec §4.7).
 * TalkBack hears a time value text ("1:23 of 3:45") via
 * [stateDescription], closing the standing slider a11y gap.
 *
 * Redesign (player spec §5): the state wiring above is untouched,
 * but the rendering is now specified, not library-default — the
 * defaults drew a 4×44dp pill thumb (which also set the row's 44dp
 * height) and a stray stop dot at the track's end. Thumb = a 16dp
 * circle in `primary` (so the slider row is 16dp tall); track = a
 * custom 4dp line drawn in the slot — active `primary`, inactive
 * [contentColor] at 24% — with no stop indicator. The library's own
 * Track cannot be used for this: in the pinned alpha29 it exposes no
 * track-height parameter (its height is token-fixed). The active
 * segment ends at the thumb center by the slider layout's own math:
 * in track-local coordinates the thumb center sits at
 * fraction × track width.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeekBar(
    st: PlayerState,
    fallbackDurationSec: Long?,
    contentColor: Color,
    onSeek: (Long) -> Unit,
) {
    val durationSec =
        if (st.durationMs > 0) st.durationMs / 1000 else fallbackDurationSec ?: 0L
    val sliderState: SliderState = rememberSliderState()
    // Scrub flag as a plain field, NOT Compose state (BUG-2): the
    // gesture callbacks set it synchronously on the main thread,
    // while a snapshot-state write only takes effect a recomposition
    // later. With a state flag, the position effect below could run
    // inside that gap and write the STALE playedFraction into the
    // slider mid-gesture — anchoring fast drags back at the
    // pre-gesture value so the finished seek targeted it too.
    val scrubbing = remember { ScrubFlag() }
    val playedFraction =
        if (st.durationMs > 0) {
            (st.positionMs.toFloat() / st.durationMs).coerceIn(0f, 1f)
        } else {
            0f
        }
    LaunchedEffect(playedFraction) {
        if (!scrubbing.active) {
            sliderState.value = playedFraction
        }
    }
    val shownSec = (sliderState.value * durationSec).toLong()
    val scheme = MaterialTheme.colorScheme
    val inactiveTrackColor = contentColor.copy(alpha = 0.24f)
    Slider(
        state = sliderState,
        onValueChange = {
            scrubbing.active = true
            sliderState.value = it
        },
        onValueChangeFinished = {
            scrubbing.active = false
            // Seek against the EFFECTIVE duration: st.durationMs can
            // still be 0 (unknown) while the fallback is known, and
            // fraction × 0 silently seeked to 0:00.
            onSeek((sliderState.value * durationSec * 1000).toLong())
        },
        colors =
            SliderDefaults.colors(
                thumbColor = scheme.primary,
                activeTrackColor = scheme.primary,
                inactiveTrackColor = inactiveTrackColor,
            ),
        thumb = {
            Box(
                Modifier
                    .size(16.dp)
                    .background(scheme.primary, CircleShape),
            )
        },
        track = { state ->
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp),
            ) {
                val range = state.trackRange
                val span = range.endInclusive - range.start
                val fraction =
                    if (span > 0f) {
                        ((state.value - range.start) / span).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                val centerY = size.height / 2f
                drawLine(
                    color = inactiveTrackColor,
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round,
                )
                if (fraction > 0f) {
                    val activeStart: Offset
                    val activeEnd: Offset
                    if (layoutDirection == LayoutDirection.Rtl) {
                        activeStart = Offset(size.width * (1f - fraction), centerY)
                        activeEnd = Offset(size.width, centerY)
                    } else {
                        activeStart = Offset(0f, centerY)
                        activeEnd = Offset(size.width * fraction, centerY)
                    }
                    drawLine(
                        color = scheme.primary,
                        start = activeStart,
                        end = activeEnd,
                        strokeWidth = size.height,
                        cap = StrokeCap.Round,
                    )
                }
            }
        },
        modifier =
            Modifier.semantics {
                // F-10: the stateful slider's own semantics surface
                // neither a name nor a value in the accessibility
                // dump — attach both. The label names the control;
                // the state description keeps the "m:ss of m:ss"
                // value text TalkBack announces.
                contentDescription = "Seek"
                stateDescription = "${formatDuration(shownSec)} of ${formatDuration(durationSec)}"
            },
    )
}

/**
 * Full player (player-redesign spec): THE hero surface, and the one
 * screen where the artwork palette still speaks — in the background
 * gradient only. The gradient top is the tempered player-scoped
 * derivation (ArtworkPaletteColors.playerGradientTop, §2); its end
 * is the byte-unchanged `safeGradientEnd`, so the content color
 * keeps its 4.5:1 guarantee. No control consumes a palette color
 * (§1, the one-accent rule): the 72dp play/pause disc is
 * `primary`/`onPrimary` — the screen's single accent moment — and
 * every other control speaks in the gradient content color, with
 * armed states in `primary`. The composition bottom-anchors the
 * control cluster inside a min-height column in the scroll
 * viewport (§3), so the resting screen has no dead void while
 * lyrics and large font scales still scroll. Shuffle/repeat state
 * is carried by tint + stateDescription (not shape), favorite
 * lives in the info block (§4), and the secondary cluster is four
 * labeled quick actions (§7).
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
    onDownloadEnqueued: () -> Unit = {},
) {
    val st by vm.state.collectAsState()
    val cur = st.current
    if (cur == null) {
        // The kit's empty-state grammar (polish item 7), not a
        // naked text: icon + headline + a way forward. "Browse
        // music" collapses the player back to the shell, where
        // Home / Search / Library live — the FullPlayer's existing
        // back callback, no new navigation.
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            EmptyState(
                title = "Nothing playing",
                subtitle = "Choose a song and it will play here.",
                actionLabel = "Browse music",
                onAction = onBack,
            )
        }
        return
    }
    val fav by vm.isFavorite(cur.id).collectAsState(false)
    val downloaded by vm.isDownloaded(cur.id).collectAsState(false)
    // Playback failures surface HERE as the inline error row (see
    // PlayerControls) — the ONE channel while this surface is
    // composed (Task 4). Claim each failure as presented so the
    // mini-player's snackbar can never repeat it: not while the
    // player is open (the old double announcement), not after a
    // collapse. The claim also runs on first composition over an
    // outstanding error, which the inline row is already showing.
    LaunchedEffect(st.errorSeq) {
        vm.markErrorPresented(st.errorSeq)
    }
    // Artwork gradient (player spec §2): the top stop is the
    // player-scoped tempered derivation — saturation scaled and
    // capped before the lightness clamp, so a saturated cover no
    // longer floods the hero (the mini-player's chrome lesson) —
    // crossfading on the shared palette helper on track change.
    // Header text/icons sit on the artwork color, so they use the
    // palette's contrast-checked on-color in both themes. Detail's
    // half-hero reads palette.mutedDark directly and is untouched
    // by this derivation.
    val palette = rememberArtworkPalette(cur.imageUrl)
    val gradientTop by animatePaletteColor(
        targetValue = palette.playerGradientTop,
        label = "playerGradientTop",
    )
    val artworkContentColor by animatePaletteColor(
        targetValue = palette.onMutedDark,
        label = "playerArtworkContent",
    )
    // Fade end must keep the content color at 4.5:1 (see
    // safeGradientEnd) — in light themes the title/artist washed
    // out over the near-white background end (UI/UX Phase B audit).
    // Byte-unchanged: it and the content-color selection above keep
    // operating on the raw palette fields.
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
    // No sleep-timer state here (F-05): the armed preset and the
    // countdown live in PlayerState, published by the controller —
    // a composition-local `remember` forgot the armed timer on
    // every collapse while the timer itself kept running.
    LaunchedEffect(showLyrics, cur.id) {
        if (showLyrics) {
            lyricsLoaded = false
            vm.lyrics(cur.id) {
                lyrics = it
                lyricsLoaded = true
            }
        }
    }
    // The controls cluster (info → lyrics), shared verbatim by the
    // portrait column and the landscape split's controls pane: one
    // implementation, two placements. The ColumnScope receiver is
    // what lets the cluster bottom-anchor itself with a weighted
    // spacer inside each pane's min-height column (spec §3).
    val controls: @Composable ColumnScope.() -> Unit = {
        PlayerControls(
            vm = vm,
            onDownloadEnqueued = onDownloadEnqueued,
            st = st,
            cur = cur,
            isFavorite = fav,
            isDownloaded = downloaded,
            contentColor = artworkContentColor,
            showLyrics = showLyrics,
            onToggleLyrics = { showLyrics = !showLyrics },
            lyrics = lyrics,
            lyricsLoaded = lyricsLoaded,
        )
    }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(playerBrush),
    ) {
        // Hoisted: the nested layout lambdas below cannot read this
        // scope's maxWidth/maxHeight through their own receivers,
        // so the §3 pane math uses these plain vals.
        val fullMaxWidth = maxWidth
        val fullMaxHeight = maxHeight
        // The palette content color covers the WHOLE player, not
        // just the header: transport icons, time labels and section
        // labels outside the provider fell back to theme colors
        // and rendered dark-on-gradient in light theme.
        CompositionLocalProvider(LocalContentColor provides artworkContentColor) {
            // Landscape (spec §6 NOW tier): medium/expanded width ×
            // compact height — a landscape phone — splits the player:
            // artwork pane left (on the same palette gradient),
            // controls pane right. Any other window keeps the
            // portrait column below, unchanged.
            if (maxWidth >= 600.dp && maxHeight < 480.dp) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(OmegaSpacing.xl),
                ) {
                    BoxWithConstraints(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        SharedArtwork(
                            song = cur,
                            size = minOf(maxWidth, maxHeight).value.toInt().coerceAtMost(340),
                            corner = OmegaRadius.xl,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            artworkBoundsTransform = artworkBoundsTransform,
                        )
                    }
                    Spacer(Modifier.width(OmegaSpacing.xl))
                    Column(
                        Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            // Scrollable, the same guarantee as the
                            // portrait column: at large font scales
                            // the controls overflow and scroll
                            // instead of clipping out of the layout
                            // (UI/UX Phase B audit).
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Min-height content column (player spec
                        // §3): the pane's weighted spacer can only
                        // distribute against a floor, and a
                        // scrollable column's height is its content
                        // — so the floor is the pane height (outer
                        // height minus its xl padding), and taller
                        // content simply grows past it and scrolls.
                        Column(
                            Modifier.heightIn(min = fullMaxHeight - OmegaSpacing.xl * 2),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            PlayerTopBar(onBack = onBack, onShowQueue = { showQueue = true })
                            controls()
                        }
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        // Scrollable: at large font scales the fixed column
                        // overflowed and the speed chips were clipped out of
                        // the layout entirely (UI/UX Phase B audit — the chips
                        // existed in code but never composed on the phone).
                        .verticalScroll(rememberScrollState())
                        .padding(OmegaSpacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Min-height content column (player spec §3):
                    // see the landscape pane — the floor is the
                    // padded viewport height; the weighted spacer
                    // inside the controls anchors the seek →
                    // secondary cluster to the bottom, so the only
                    // space below the secondary row is the column's
                    // own 24dp bottom padding.
                    Column(
                        Modifier.heightIn(min = fullMaxHeight - OmegaSpacing.xl * 2),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PlayerTopBar(onBack = onBack, onShowQueue = { showQueue = true })
                        Spacer(Modifier.height(OmegaSpacing.xl))
                        SharedArtwork(
                            song = cur,
                            size =
                                minOf(fullMaxWidth - OmegaSpacing.xl * 2, 340.dp)
                                    .value
                                    .toInt()
                                    .coerceAtLeast(0),
                            corner = OmegaRadius.xl,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            artworkBoundsTransform = artworkBoundsTransform,
                        )
                        Spacer(Modifier.height(OmegaSpacing.xxl))
                        controls()
                    }
                }
            }
        }
    }
    if (showQueue) {
        val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
        val sheetScope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = sheetState,
        ) {
            // "Queue", not "Up next" (F-21): the list is the WHOLE
            // queue — already-played tracks above the highlighted
            // current one included.
            Text(
                "Queue",
                modifier = Modifier.padding(OmegaSpacing.lg),
                style = MaterialTheme.typography.titleMedium,
            )
            val currentIndex = st.queue.indexOfFirst { it.id == cur.id }
            LazyColumn {
                itemsIndexed(st.queue, key = { index, s -> "$index-${s.id}" }) { index, s ->
                    val isCurrent = index == currentIndex
                    // Expressive ListItem (alpha29): the selectable
                    // overload — headline is the trailing `content`
                    // lambda, selection and click are first-class.
                    // The classic headlineContent overload is
                    // deprecated.
                    ListItem(
                        selected = isCurrent,
                        onClick = {
                            vm.player.playIndex(index)
                            // A selection completes the sheet's task
                            // (F-07): change the track AND dismiss, so
                            // the user sees the result of the tap.
                            sheetScope
                                .launch { sheetState.hide() }
                                .invokeOnCompletion { showQueue = false }
                        },
                        modifier =
                            Modifier
                                .animateItem()
                                .semantics {
                                    if (isCurrent) {
                                        stateDescription = "Now playing"
                                    }
                                },
                        leadingContent = { Artwork(s.imageUrl, 44, OmegaRadius.lg) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isCurrent) {
                                    Icon(
                                        Icons.Filled.GraphicEq,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                // Per-row management (polish item
                                // 19): every queue row can be removed
                                // without disturbing the rest — see
                                // PlayerController.removeFromQueue.
                                QueueRowMenu(
                                    songName = s.name,
                                    onRemove = { vm.player.removeFromQueue(index) },
                                )
                            }
                        },
                        supportingContent = { Text(s.artist) },
                        // Selected treatment = the kit's
                        // OmegaSegmentedListItem roles (uplift §5.9):
                        // primaryContainer with its on-colors. The
                        // alpha29 selectable overload reads the
                        // selected* slots of ListItemColors — the
                        // previous hand-mirror set the UNSELECTED
                        // slots in this branch, so the current row
                        // silently rendered the library default
                        // (selectedContainer = secondaryContainer,
                        // sampled on device in the polish pass)
                        // while the code claimed primaryContainer.
                        // Colors only: construction, jump and
                        // per-row remove are untouched (RULING C).
                        colors =
                            ListItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedSupportingContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                    ) {
                        Text(s.name)
                    }
                }
            }
        }
    }
}

/**
 * The full player's top row — collapse + queue — shared by the
 * portrait column and the landscape split's controls pane.
 */
@Composable
private fun PlayerTopBar(
    onBack: () -> Unit,
    onShowQueue: () -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Collapse player")
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onShowQueue) {
            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue")
        }
    }
}

/**
 * One option in a player preset menu (sleep / speed); the current
 * value carries a check so state reads beyond color — the same
 * pattern as Library's sort menu.
 */
@Composable
private fun PlayerMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label, maxLines = 1, softWrap = false) },
        leadingIcon =
            if (selected) {
                { Icon(Icons.Filled.Check, contentDescription = null) }
            } else {
                null
            },
        onClick = onClick,
    )
}

/**
 * Per-row overflow for queue rows (polish item 19): the queue
 * sheet's one management action, "Remove from queue". Mirrors the
 * kit's song-row menus (SongOverflowMenuButton in :core:ui): a
 * MoreVert button that names its song, opening a menu whose single
 * item carries the PlaylistRemove glyph.
 */
@Composable
private fun QueueRowMenu(
    songName: String,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "More options for $songName",
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Remove from queue", maxLines = 1, softWrap = false) },
                leadingIcon = { Icon(Icons.Filled.PlaylistRemove, contentDescription = null) },
                onClick = {
                    expanded = false
                    onRemove()
                },
            )
        }
    }
}

/**
 * The full player's controls cluster: the info block (title/artist
 * with favorite promoted beside them, §4), the always-reserved
 * error slot, the seek group (explicitly styled slider + buffering
 * slot + times, §5), the distributed transport row (§6), the
 * secondary cluster of labeled quick actions — Download / Lyrics /
 * Sleep / Speed (§7) — and the inline lyrics block (§8). The
 * [ColumnScope] receiver carries the §3 bottom-anchoring: a
 * weighted spacer above the seek group absorbs the min-height
 * column's remainder, so the cluster rests at the bottom and the
 * error slot's reservation never moves it. [vm] carries the
 * player/library actions; the sleep timer reads [st] directly (its
 * truth is the controller's, F-05); lyrics visibility and payload
 * arrive as values + callbacks. Every callback, menu, and state
 * flow is the pre-redesign wiring — only the presentation changed.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColumnScope.PlayerControls(
    vm: PlayerViewModel,
    onDownloadEnqueued: () -> Unit,
    st: PlayerState,
    cur: Song,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    contentColor: Color,
    showLyrics: Boolean,
    onToggleLyrics: () -> Unit,
    lyrics: String?,
    lyricsLoaded: Boolean,
) {
    val ctx = LocalContext.current
    val snackbar = LocalOmegaSnackbar.current
    val scheme = MaterialTheme.colorScheme
    // Secondary text on this screen (artist, times, quick-action
    // labels) is the gradient content color at 70% (spec §1);
    // control glyphs and the title use it at full strength.
    val secondaryContentColor = contentColor.copy(alpha = 0.7f)
    // Info block (§4): real start alignment — the old title's
    // apparent centering was the column centering a wrap-width text
    // box — with favorite promoted to a 48dp button trailing the
    // text column, centered against title + artist.
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                cur.name,
                style = MaterialTheme.typography.headlineMedium,
                color = contentColor,
                textAlign = TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(OmegaSpacing.xs))
            Text(
                cur.artist,
                style = MaterialTheme.typography.bodyLarge,
                color = secondaryContentColor,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        IconButton(
            onClick = { vm.toggleFavorite(cur, isFavorite) },
            modifier =
                Modifier.semantics {
                    stateDescription = if (isFavorite) "Favorite on" else "Favorite off"
                },
        ) {
            OmegaFavoriteIcon(
                isFavorite = isFavorite,
                tint = if (isFavorite) scheme.primary else secondaryContentColor,
            )
        }
    }
    // Error slot (§5): the height is reserved ALWAYS — the slot is
    // 48dp whether or not an error is showing (the row's own height
    // at font 1.0/1.33: the 40dp Retry button, or two wrapped text
    // lines, both fit inside it) — so an error appearing or clearing
    // shifts nothing below it. The row itself (message + Retry
    // wiring) is unchanged.
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (st.errorMessage != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(OmegaSpacing.xs))
                Text(
                    "Couldn't play. Check your connection.",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = { vm.player.retry() }) {
                    Text("Retry")
                }
            }
        }
    }
    // §3 rhythm: a fixed 28dp gap, then the flexible space — the
    // remainder pools ABOVE the seek group, never below the
    // secondary row.
    Spacer(Modifier.height(28.dp))
    Spacer(Modifier.weight(1f))
    SeekBar(
        st = st,
        fallbackDurationSec = cur.durationSec,
        contentColor = contentColor,
        onSeek = { vm.player.seekTo(it) },
    )
    // Fixed-height buffering slot under the slider: the wavy
    // strip (spec §3 — the wave belongs on media surfaces)
    // appears while buffering without shifting the time row.
    // It doubles as the §5 times gap: its 4dp is the space
    // between the slider and the times row in both states.
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp),
    ) {
        if (st.isBuffering) {
            LinearWavyProgressIndicator(
                color = scheme.primary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    Row(Modifier.fillMaxWidth()) {
        Text(
            formatDuration(st.positionMs / 1000),
            style = OmegaType.rowMeta,
            color = secondaryContentColor,
        )
        Spacer(Modifier.weight(1f))
        Text(
            formatDuration(if (st.durationMs > 0) st.durationMs / 1000 else cur.durationSec),
            style = OmegaType.rowMeta,
            color = secondaryContentColor,
        )
    }
    Spacer(Modifier.height(20.dp))
    // Transport (§6): distributed across the full content width
    // instead of a centered wrap block — 48/56/72/56/48 targets.
    // Armed shuffle/repeat read by primary tint + stateDescription;
    // the palette-tertiary tint and the shape morph are gone (§1).
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconToggleButton(
            checked = st.shuffle,
            onCheckedChange = { vm.player.toggleShuffle() },
            colors =
                IconButtonDefaults.iconToggleButtonColors(
                    contentColor = contentColor,
                    checkedContentColor = scheme.primary,
                ),
            modifier =
                Modifier.semantics {
                    stateDescription = if (st.shuffle) "Shuffle on" else "Shuffle off"
                },
        ) {
            Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle")
        }
        IconButton(
            onClick = { vm.player.prev() },
            modifier = Modifier.size(56.dp),
        ) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(32.dp))
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
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                ),
        ) {
            OmegaPlayPauseIcon(isPlaying = st.isPlaying, modifier = Modifier.size(28.dp))
        }
        IconButton(
            onClick = { vm.player.next() },
            modifier = Modifier.size(56.dp),
        ) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(32.dp))
        }
        // st.repeatMode is the ENGINE value — interpret it ONLY via
        // the RepeatMode mapping. Reading the raw Int here is what
        // swapped One/All in the UI (BUG-1: Media3's ONE=1, ALL=2).
        val repeat = repeatModeFromEngine(st.repeatMode)
        IconToggleButton(
            checked = repeat != RepeatMode.OFF,
            onCheckedChange = { vm.player.cycleRepeat() },
            colors =
                IconButtonDefaults.iconToggleButtonColors(
                    contentColor = contentColor,
                    checkedContentColor = scheme.primary,
                ),
            modifier =
                Modifier.semantics {
                    stateDescription =
                        when (repeat) {
                            RepeatMode.ONE -> "Repeat one"
                            RepeatMode.ALL -> "Repeat all"
                            RepeatMode.OFF -> "Repeat off"
                        }
                },
        ) {
            Icon(
                if (repeat == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = "Repeat",
            )
        }
    }
    Spacer(Modifier.height(OmegaSpacing.xl))
    // Secondary cluster (§7): four labeled quick actions in
    // fixed-weight slots — the slot width never changes, so a
    // ticking sleep countdown or a speed change moves nothing.
    // State reads through icon + label tint (primary when armed),
    // never a chip: the 36dp armed chips and their per-second
    // reflow are deleted. Favorite lives in the info block now (§4).
    var sleepMenuOpen by remember { mutableStateOf(false) }
    var speedMenuOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth()) {
        PlayerQuickAction(
            label = "Download",
            active = isDownloaded,
            contentColor = contentColor,
            modifier = Modifier.weight(1f),
        ) {
            IconButton(
                onClick = {
                    if (!isDownloaded) {
                        DownloadWorker.enqueue(
                            WorkManager.getInstance(ctx),
                            cur,
                            vm.appSettings.value.downloadQuality,
                        )
                        onDownloadEnqueued()
                    } else {
                        // State-aware (F-09): tapping the downloaded
                        // state says so instead of silently doing
                        // nothing.
                        snackbar?.showMessage("Already downloaded")
                    }
                },
            ) {
                Icon(
                    if (isDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                    contentDescription = if (isDownloaded) "Downloaded" else "Download",
                    tint = if (isDownloaded) scheme.primary else contentColor,
                )
            }
        }
        PlayerQuickAction(
            label = "Lyrics",
            active = showLyrics,
            contentColor = contentColor,
            modifier = Modifier.weight(1f),
        ) {
            // Lyrics is a toggle and must LOOK like one (F-09): the
            // open state tints icon and label primary (§7); TalkBack
            // hears the state too (F-10). The old tonal checked
            // container is gone — the unit grammar has no container.
            IconToggleButton(
                checked = showLyrics,
                onCheckedChange = { onToggleLyrics() },
                colors =
                    IconButtonDefaults.iconToggleButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = contentColor,
                        checkedContainerColor = Color.Transparent,
                        checkedContentColor = scheme.primary,
                    ),
                modifier =
                    Modifier.semantics {
                        stateDescription = if (showLyrics) "Lyrics on" else "Lyrics off"
                    },
            ) {
                Icon(Icons.Filled.Lyrics, contentDescription = "Lyrics")
            }
        }
        // Sleep timer: a labeled unit in both states — the menu is
        // the same preset menu as before (Task 4), and both the
        // armed preset and the countdown still read from
        // PlayerState (F-05), so the control survives
        // collapse/reopen and agrees with the timer that fires.
        // Armed, the label IS the remaining time, updating in
        // place inside the fixed slot.
        Box(Modifier.weight(1f)) {
            PlayerQuickAction(
                label =
                    if (st.sleepMinutes > 0) {
                        formatDuration(st.sleepRemainingMs / 1000)
                    } else {
                        "Sleep"
                    },
                active = st.sleepMinutes > 0,
                contentColor = contentColor,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(
                    onClick = { sleepMenuOpen = true },
                    modifier =
                        Modifier.semantics {
                            stateDescription =
                                if (st.sleepMinutes > 0) {
                                    "Sleep timer, ${formatDuration(st.sleepRemainingMs / 1000)} left"
                                } else {
                                    "Sleep timer off"
                                }
                        },
                ) {
                    Icon(
                        Icons.Filled.Bedtime,
                        contentDescription = "Sleep timer",
                        tint = if (st.sleepMinutes > 0) scheme.primary else contentColor,
                    )
                }
            }
            DropdownMenu(
                expanded = sleepMenuOpen,
                onDismissRequest = { sleepMenuOpen = false },
            ) {
                PlayerMenuItem(
                    label = "Off",
                    selected = st.sleepMinutes == 0,
                    onClick = {
                        vm.player.setSleepTimer(0)
                        sleepMenuOpen = false
                    },
                )
                SLEEP_TIMER_PRESETS.forEach { minutes ->
                    PlayerMenuItem(
                        label = "$minutes minutes",
                        selected = st.sleepMinutes == minutes,
                        onClick = {
                            vm.player.setSleepTimer(minutes)
                            sleepMenuOpen = false
                        },
                    )
                }
            }
        }
        // Playback speed, same labeled-unit + menu pattern as
        // sleep: at 1× the label reads "Speed"; off 1× it shows
        // the rate and tints primary. The menu (and its labels)
        // is unchanged; the state description always announces
        // the speed.
        Box(Modifier.weight(1f)) {
            PlayerQuickAction(
                label = if (st.speed == 1f) "Speed" else "${st.speed}×",
                active = st.speed != 1f,
                contentColor = contentColor,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(
                    onClick = { speedMenuOpen = true },
                    modifier =
                        Modifier.semantics {
                            stateDescription = "Playback speed, ${speedLabel(st.speed)}"
                        },
                ) {
                    Icon(
                        Icons.Filled.Speed,
                        contentDescription = "Playback speed",
                        tint = if (st.speed != 1f) scheme.primary else contentColor,
                    )
                }
            }
            DropdownMenu(
                expanded = speedMenuOpen,
                onDismissRequest = { speedMenuOpen = false },
            ) {
                SPEED_OPTIONS.forEach { v ->
                    PlayerMenuItem(
                        label = speedLabel(v),
                        selected = st.speed == v,
                        onClick = {
                            vm.player.setSpeed(v)
                            speedMenuOpen = false
                        },
                    )
                }
            }
        }
    }
    if (showLyrics) {
        Spacer(Modifier.height(OmegaSpacing.md))
        // Lyrics containment (polish item 25): the block joins the
        // app's segmented grammar — the kit's OmegaSegmentedContainer
        // with a "Lyrics" header — instead of bare text appended
        // under the controls. (Hand-rolled in that grammar at this
        // branch's original base, where the kit had no free-form
        // segmented container; adopted the kit container once P0
        // landed it — P5 review finding I-1.)
        OmegaSegmentedContainer(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(OmegaSpacing.lg),
        ) {
            Text(
                "Lyrics",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
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
        // Clearance (F-09): the lyrics block is the column's last
        // content; without tail room its last line can sit under the
        // shell's bottom chrome. The extra spacer guarantees the
        // final line scrolls fully into the clear.
        Spacer(Modifier.height(OmegaSpacing.xxl))
    }
}

/**
 * One labeled quick action in the player's secondary cluster
 * (player spec §7): a 48dp icon target with its label beneath —
 * `bodySmall`, the gradient content color at 70%, or `primary`
 * when the action is in an armed/active state. The caller gives
 * the unit a fixed slot (a Row weight), so the label's text
 * changing — a sleep countdown ticking, a speed rate appearing —
 * never moves a neighbor. The label is single-line, centered, and
 * allowed to overflow its slot visibly rather than clip: at font
 * 2.0 the words are wider than a quarter of the content width and
 * must stay intact (§9).
 */
@Composable
private fun PlayerQuickAction(
    label: String,
    active: Boolean,
    contentColor: Color,
    modifier: Modifier = Modifier,
    button: @Composable () -> Unit,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        button()
        Spacer(Modifier.height(OmegaSpacing.xs))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color =
                if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    contentColor.copy(alpha = 0.7f)
                },
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
