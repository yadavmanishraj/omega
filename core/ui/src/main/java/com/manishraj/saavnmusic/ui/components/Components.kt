package com.manishraj.saavnmusic.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.formatDuration
import com.manishraj.saavnmusic.ui.theme.LocalReducedMotion
import com.manishraj.saavnmusic.ui.theme.OmegaRadius
import com.manishraj.saavnmusic.ui.theme.OmegaSpacing
import com.manishraj.saavnmusic.ui.theme.OmegaType
import kotlinx.coroutines.delay

/*
 * Shared UI primitives (REDESIGN_SPEC §3.4): every screen composes these
 * so spacing, radii, type roles and accessibility behavior stay uniform.
 * All sizes come from the design tokens; no ad-hoc hex anywhere.
 */

/**
 * Count label that agrees in number — "1 song", otherwise
 * "N songs". Every song-count label in the app goes through here
 * (exhaustive QA BUG-4: picker, Downloads header, Library and
 * Search playlist rows all rendered "1 songs").
 */
fun songCountLabel(count: Int): String = if (count == 1) "1 song" else "$count songs"

/**
 * Compact count for large audience numbers (e.g. artist followers):
 * 950 → "950", 12_300 → "12.3K", 107_959_415 → "108M". Trailing ".0" is
 * dropped. (Raw interpolation rendered "107959415 followers".)
 */
fun compactCount(count: Long): String {
    fun scaled(
        value: Double,
        suffix: String,
    ): String {
        val text = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(java.util.Locale.US, "%.1f", value)
        return text + suffix
    }
    return when {
        count < 1_000 -> count.toString()
        count < 1_000_000 -> scaled(count / 1_000.0, "K")
        count < 1_000_000_000 -> scaled(count / 1_000_000.0, "M")
        else -> scaled(count / 1_000_000_000.0, "B")
    }
}

@Composable
fun Artwork(
    url: String?,
    size: Int = 56,
    corner: Dp = OmegaRadius.lg,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(size.dp)
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Circular artwork variant (artists). */
@Composable
fun CircularArtwork(
    url: String?,
    size: Int = 56,
    contentDescription: String? = null,
) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The one song row (uplift spec §5.1): a kit [Row] with explicit
 * colors, NOT a stock ListItem — the stock item painted its default
 * `surface` band behind every row (one of the audit's five competing
 * background treatments) and styled the headline from the theme slot
 * instead of the row tokens. The container is transparent: rows sit
 * directly on the page background.
 *
 * Geometry (§4.4): artwork 48dp @ lg (12), padding 16 horizontal /
 * 8 vertical on the §4.1 line, min height 64dp (the row grows past
 * it at large font scales; it never shrinks below the touch target).
 */
@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .heightIn(min = 64.dp)
            .padding(horizontal = OmegaSpacing.lg, vertical = OmegaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(song.imageUrl, size = 48, contentDescription = song.name)
        Spacer(Modifier.width(OmegaSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                song.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = OmegaType.rowTitle,
                color = scheme.onSurface,
            )
            // The duration gets a protected slot: as one joined
            // string it was ellipsized away (or cut mid-value,
            // "• 3:…") whenever the artist list ran long at large
            // font scales (UI/UX Phase B audit). The artist text
            // ellipsizes first; the duration never truncates.
            Row(verticalAlignment = Alignment.CenterVertically) {
                val duration = formatDuration(song.durationSec)
                Text(
                    song.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = OmegaType.rowMeta,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (duration.isNotBlank()) {
                    Text(
                        if (song.artist.isBlank()) duration else " • $duration",
                        maxLines = 1,
                        style = OmegaType.rowMeta,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

/**
 * Artwork-led card for rails and grids (uplift spec §5.3): artwork
 * 124dp @ xl (16) — the Editorial artwork-forward size. The card
 * carries NO outer padding: rails own spacing (§4.2 — contentPadding
 * at the 16dp line, 12dp item gaps), so a card is exactly its
 * artwork column and the first card lands on the section line.
 */
@Composable
fun MediaCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    width: Int = 124,
    circular: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .width(width.dp)
            .clickable { onClick() },
    ) {
        if (circular) {
            CircularArtwork(imageUrl, width, contentDescription = title)
        } else {
            // Card artwork corner = 16dp per the shape language (M3X spec §2.3: rows 12, cards 16, hero 28).
            Artwork(imageUrl, width, OmegaRadius.xl, contentDescription = title)
        }
        Spacer(Modifier.height(OmegaSpacing.sm))
        Text(
            title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall,
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Pause between shimmer sweeps, so the highlight reads as a pass,
 * not a strobe. */
private const val SHIMMER_PAUSE_MS = 350L

/**
 * Shimmer phase for the skeleton placeholders (M3 Expressive spec §3:
 * skeletons gain a sweep, closing the standing "static skeletons"
 * gap). One [Animatable] per skeleton block drives every placeholder
 * shape in sync; the sweep runs on the theme's SLOW EFFECTS spec — a
 * critically damped spring, so the highlight eases across with no
 * overshoot, in the same motion family as the rest of the app.
 *
 * Reduced motion ([LocalReducedMotion], animator scale 0): the phase
 * pins to 0 and the blocks are static, exactly as before.
 */
@Composable
internal fun rememberShimmerPhase(): Float {
    val reducedMotion = LocalReducedMotion.current
    val sweepSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
    val phase = remember { Animatable(0f) }
    LaunchedEffect(reducedMotion, sweepSpec) {
        if (reducedMotion) {
            phase.snapTo(0f)
        } else {
            while (true) {
                phase.animateTo(1f, sweepSpec)
                delay(SHIMMER_PAUSE_MS)
                phase.snapTo(0f)
            }
        }
    }
    return phase.value
}

/** Draws the moving highlight band over a skeleton block at [phase]
 * (0 = off-screen left, 1 = off-screen right). Draws nothing at phase
 * 0, which is the reduced-motion resting state. Applied AFTER the
 * block's clip + background so the band stays inside its corners. */
internal fun Modifier.shimmerSweep(
    phase: Float,
    highlight: Color,
): Modifier =
    drawWithContent {
        drawContent()
        if (phase > 0f) {
            val bandWidth = size.width * 0.6f
            val startX = -bandWidth + phase * (size.width + 2 * bandWidth)
            drawRect(
                brush =
                    Brush.linearGradient(
                        colors = listOf(Color.Transparent, highlight, Color.Transparent),
                        start = Offset(startX, 0f),
                        end = Offset(startX + bandWidth, 0f),
                    ),
            )
        }
    }

@Composable
fun ShimmerList() {
    val phase = rememberShimmerPhase()
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    Column {
        repeat(6) {
            // Skeleton geometry IS the SongRow geometry (§5.1/§4.4):
            // 48dp artwork @ lg, 16dp inset, 8dp vertical padding,
            // 64dp min height — the resolved rows land exactly where
            // these blocks promise (LY-9: no layout shift on load).
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = OmegaSpacing.lg, vertical = OmegaSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(OmegaRadius.lg))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .shimmerSweep(phase, highlight),
                )
                Spacer(Modifier.width(OmegaSpacing.md))
                Column {
                    Box(
                        Modifier
                            .fillMaxWidth(0.7f)
                            .height(14.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .shimmerSweep(phase, highlight),
                    )
                    Spacer(Modifier.height(OmegaSpacing.sm))
                    Box(
                        Modifier
                            .fillMaxWidth(0.45f)
                            .height(12.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .shimmerSweep(phase, highlight),
                    )
                }
            }
        }
    }
}

/**
 * Ranked-chart skeleton: [ShimmerList]'s construction (one phase,
 * `surfaceVariant` blocks, the same sweep) at the [RankedSongRow]
 * geometry (spec §5.2/§4.4), for Home's Trending loading branch.
 * The standard skeleton put its artwork at the screen inset and its
 * text at the SongRow column; the resolved chart sits one rank
 * column further right, so the section shifted horizontally on load
 * (verify-loop LY-9). Here every block promises the ranked layout:
 * 16dp inset, the 24dp rank column, 44dp artwork @ md, text at the
 * 108dp column — the loaded rows land exactly where these blocks
 * promise.
 *
 * The vertical promise is the per-row PITCH, not just the row box.
 * The resolved chart composes each row as a list item holding the
 * row plus, between rows, an [EditorialRowDivider] item — and the
 * row's REALIZED height is 60dp, not its 56dp floor: the trailing
 * overflow button's 48dp touch target is the tallest content
 * (artwork is 44dp), over 6dp padding each side. The divider item
 * adds one hairline (1px). The skeleton mirrors both terms: 60dp
 * rows (the 44dp artwork block centers in the 48dp content box, so
 * even the first row's artwork sits where the loaded one sits) and
 * a hairline slot at the 108dp text column between rows, never
 * after the last. An earlier cut drew 56dp rows and no slots, so
 * every row below the first settled ≈11.5px on load as the missing
 * row height and hairline appeared (TELL-SHIMMER-DIVIDER-SETTLE,
 * verify-loop F6).
 */
@Composable
fun ShimmerRankedList() {
    val phase = rememberShimmerPhase()
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    // One physical pixel in dp — the layout height the resolved
    // chart's Dp.Hairline divider item occupies.
    val hairline = with(LocalDensity.current) { 1f.toDp() }
    // The ranked text column (the inset Home passes its divider):
    // screen inset + rank column + gap + artwork + gap = 108dp.
    val dividerInset = OmegaSpacing.lg + 24.dp + OmegaSpacing.md + 44.dp + OmegaSpacing.md
    Column {
        repeat(6) { index ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .padding(horizontal = OmegaSpacing.lg, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The rank numeral's slot (§5.2): the full 24dp
                // column, so the block's edges are the column's.
                Box(
                    Modifier
                        .width(24.dp)
                        .height(10.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .shimmerSweep(phase, highlight),
                )
                Spacer(Modifier.width(OmegaSpacing.md))
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(OmegaRadius.md))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .shimmerSweep(phase, highlight),
                )
                Spacer(Modifier.width(OmegaSpacing.md))
                Column {
                    Box(
                        Modifier
                            .fillMaxWidth(0.7f)
                            .height(14.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .shimmerSweep(phase, highlight),
                    )
                    Spacer(Modifier.height(OmegaSpacing.sm))
                    Box(
                        Modifier
                            .fillMaxWidth(0.45f)
                            .height(12.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .shimmerSweep(phase, highlight),
                    )
                }
            }
            if (index < 5) {
                // The divider slot: exactly the loaded divider
                // item's height — one hairline, filled by the line
                // itself in the skeleton's block tone, starting at
                // the text column. Never after the last row.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(hairline)
                        .padding(start = dividerInset)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .shimmerSweep(phase, highlight),
                )
            }
        }
    }
}

/**
 * Full-block error with a friendly cause+fix message and a recovery
 * action. Raw exception text never reaches this component (spec §3.4:
 * errors pair icon + text + recovery, and are never silent).
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    icon: ImageVector = Icons.Outlined.ErrorOutline,
    title: String = "Something went wrong",
    actionLabel: String = "Retry",
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(OmegaSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // State skeleton (uplift §5.11, shared with EmptyState): the
        // icon sits in a surfaceContainerLow circle — a tonal badge,
        // not a bare glyph floating on the page.
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(OmegaSpacing.md))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(OmegaSpacing.xs))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(OmegaSpacing.lg))
        Button(onClick = onRetry) { Text(actionLabel) }
    }
}

/**
 * Compact inline error (polish item 10): the per-SECTION failure
 * treatment. Where [ErrorState] is a full block — 48dp icon, title,
 * message, filled Retry — for a surface that has nothing else, this
 * row is one subordinate line inside a feed: small error icon, the
 * message, a text Retry. It exists so a partial failure (one Home
 * section down, the rest playing) reads as a footnote to the content
 * around it, not as several stacked page errors.
 */
@Composable
fun InlineErrorRow(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.ErrorOutline,
    actionLabel: String = "Retry",
) {
    // The row is a BAND (uplift §5.11): a surfaceContainerLow rounded
    // block on the §4.1 line — the outer padding positions the band,
    // the band's own padding insets its content.
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = OmegaSpacing.lg)
            .clip(RoundedCornerShape(OmegaRadius.lg))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = OmegaSpacing.md, vertical = OmegaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.width(OmegaSpacing.sm))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) { Text(actionLabel) }
    }
}

/** Empty state with an icon, an explanation and an optional next step. */
@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Filled.MusicNote,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(OmegaSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // State skeleton (uplift §5.11, shared with ErrorState): icon
        // in a surfaceContainerLow circle, tinted onSurfaceVariant.
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(OmegaSpacing.md))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(OmegaSpacing.xs))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(OmegaSpacing.lg))
            // The next step out of an empty state is THE action of
            // the surface — a filled primary button, not a text
            // whisper (§5.11).
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/**
 * The one destructive-confirmation treatment (polish item 4):
 * emphasis follows consequence — the destructive action is a FILLED
 * button in the error family, and Cancel is de-emphasized to a
 * neutral text button (onSurfaceVariant). The pre-polish dialogs
 * inverted this: the destructive confirm whispered as error-colored
 * text while Cancel wore the brand primary.
 *
 * This composable owns PRESENTATION only. The caller keeps its copy
 * (title/text name the concrete consequence) and everything that
 * happens after confirmation (deletes, Undo snackbars) — the dialog
 * reports through [onConfirm] / [onDismiss] and holds no state.
 */
@Composable
fun OmegaDestructiveConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Cancel",
) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        // Container and shape are EXPLICIT (uplift §5.8): the grouped
        // role and the expressive extraLarge corner are named here,
        // never inherited from dialog defaults — a default that
        // happens to match today is a leak that breaks tomorrow.
        containerColor = scheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = scheme.error,
                        contentColor = scheme.onError,
                    ),
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = scheme.onSurfaceVariant),
            ) { Text(dismissLabel) }
        },
    )
}

/**
 * Section title row. Per M3 Expressive spec §2.2, section headers ARE
 * titleMediumEmphasized — weight, not size, carries the emphasis —
 * and every same-role header renders identically, so there is no
 * emphasis flag: the pre-polish API defaulted to titleLarge (22sp)
 * and its opt-in `emphasized` variant rendered SMALLER (16sp) than
 * the default it claimed to promote.
 */
@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = OmegaSpacing.lg, vertical = OmegaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMediumEmphasized,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun GradientHeader(
    imageUrl: String?,
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    // Artwork-derived header (UIUX_DESIGN §3.1.3): the gradient runs
    // from the artwork's darkened palette color into the theme
    // background, crossfading on the shared effects-spec helper
    // (animatePaletteColor, spec §4.4) when the artwork changes;
    // header content uses the palette's contrast-checked on-color.
    // Falls back to the deep-teal palette when there is no artwork.
    val palette = rememberArtworkPalette(imageUrl)
    val gradientTop by animatePaletteColor(
        targetValue = palette.mutedDark,
        label = "headerGradientTop",
    )
    // The fade must end where the content color still passes 4.5:1 —
    // fading straight to a light theme background made lower header
    // text invisible (UI/UX Phase B audit). safeGradientEnd keeps
    // the seamless fade in dark themes and a legible dark band in
    // light ones.
    val gradientEnd by animatePaletteColor(
        targetValue = safeGradientEnd(palette, MaterialTheme.colorScheme.background),
        label = "headerGradientEnd",
    )
    val contentColor by animatePaletteColor(
        targetValue = palette.onMutedDark,
        label = "headerContent",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        gradientTop,
                        gradientEnd,
                    ),
                ),
            ),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Column(Modifier.padding(top = OmegaSpacing.lg)) {
                // Up affordance on pushed destinations (album /
                // playlist / artist), tinted with the header's
                // contrast-checked content color.
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                }
                content()
            }
        }
    }
}
