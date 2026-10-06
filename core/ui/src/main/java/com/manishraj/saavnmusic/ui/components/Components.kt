package com.manishraj.saavnmusic.ui.components

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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.formatDuration
import com.manishraj.saavnmusic.ui.theme.OmegaRadius
import com.manishraj.saavnmusic.ui.theme.OmegaSpacing

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
) {
    Box(
        Modifier
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
            Text(
                listOf(song.artist, formatDuration(song.durationSec))
                    .filter { it.isNotBlank() }
                    .joinToString(" • "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
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

@Composable
fun ShimmerList() {
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
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Spacer(Modifier.width(OmegaSpacing.md))
                Column {
                    Box(
                        Modifier
                            .fillMaxWidth(0.7f)
                            .height(14.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Spacer(Modifier.height(OmegaSpacing.sm))
                    Box(
                        Modifier
                            .fillMaxWidth(0.45f)
                            .height(12.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
            }
        }
    }
}

/** Card-grid skeleton matching MediaCard dimensions (Search grids, Home rails). */
@Composable
fun ShimmerGrid() {
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
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                        Spacer(Modifier.height(OmegaSpacing.sm))
                        Box(
                            Modifier
                                .fillMaxWidth(0.8f)
                                .height(12.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
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
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun GradientHeader(
    imageUrl: String?,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Column(Modifier.padding(top = OmegaSpacing.lg)) { content() }
    }
}
