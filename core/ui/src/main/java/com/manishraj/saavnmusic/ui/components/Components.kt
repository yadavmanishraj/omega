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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import kotlinx.coroutines.delay

/**
 * Shared UI primitives (REDESIGN_SPEC §3.4): every screen composes these
 * so spacing, radii, type roles and accessibility behavior stay uniform.
 * All sizes come from the design tokens; no ad-hoc hex anywhere.
 */

@Composable
fun Artwork(
    url: String?,
    size: Int = 56,
    corner: Dp = OmegaRadius.md,
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

@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = {
            Text(
                song.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = {
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
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (duration.isNotBlank()) {
                    Text(
                        if (song.artist.isBlank()) duration else " • $duration",
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        leadingContent = { Artwork(song.imageUrl, contentDescription = song.name) },
        trailingContent = trailing,
        modifier = Modifier.clickable { onClick() },
    )
}

/** Artwork-led card for rails and grids (148dp per spec §3.4). */
@Composable
fun MediaCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    width: Int = 148,
    circular: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .width(width.dp)
            .clickable { onClick() }
            .padding(OmegaSpacing.sm),
    ) {
        if (circular) {
            CircularArtwork(imageUrl, width - 16, contentDescription = title)
        } else {
            Artwork(imageUrl, width - 16, OmegaRadius.lg, contentDescription = title)
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
private fun rememberShimmerPhase(): Float {
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
private fun Modifier.shimmerSweep(
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
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(OmegaSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(56.dp)
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
        }
    }
}

/** Card-grid skeleton matching MediaCard dimensions (Search grids, Home rails). */
@Composable
fun ShimmerGrid() {
    val phase = rememberShimmerPhase()
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    Column(Modifier.padding(horizontal = OmegaSpacing.lg)) {
        repeat(2) {
            Row {
                repeat(2) {
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(OmegaSpacing.sm),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .clip(RoundedCornerShape(OmegaRadius.lg))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .shimmerSweep(phase, highlight),
                        )
                        Spacer(Modifier.height(OmegaSpacing.sm))
                        Box(
                            Modifier
                                .fillMaxWidth(0.8f)
                                .height(12.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .shimmerSweep(phase, highlight),
                        )
                    }
                }
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
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error,
        )
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
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/**
 * Section title row. [emphasized] swaps the baseline style for the
 * emphasized twin (M3 Expressive spec §2.2: weight, not size, carries
 * the emphasis) — reserved for the ONE rail a calm screen promotes
 * (Home's "Jump back in"); every other caller keeps the default.
 */
@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    emphasized: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = OmegaSpacing.lg, vertical = OmegaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style =
                if (emphasized) {
                    MaterialTheme.typography.titleMediumEmphasized
                } else {
                    MaterialTheme.typography.titleLarge
                },
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
