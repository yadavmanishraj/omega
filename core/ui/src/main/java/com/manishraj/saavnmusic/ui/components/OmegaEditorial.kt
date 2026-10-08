package com.manishraj.saavnmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.formatDuration
import com.manishraj.saavnmusic.ui.theme.OmegaRadius
import com.manishraj.saavnmusic.ui.theme.OmegaSpacing
import com.manishraj.saavnmusic.ui.theme.OmegaType

/*
 * Editorial kit (visual-uplift spec §5.2/§5.4, §2.2, §4.2/§4.3): the
 * calm-screen grammar — tracked-uppercase taxonomy labels with hairline
 * rules, the ranked chart row, list-side row dividers, and the rail edge
 * fade. Hairlines replace boxes as the structure on calm screens; every
 * value below is a spec value, expressed from the design tokens or as a
 * named constant citing its clause.
 */

/** Rank column width (spec §5.2). */
private val RankColumnWidth = 24.dp

/** Ranked-chart artwork edge in dp (spec §4.4 density table). */
private const val RANKED_ARTWORK_EDGE = 44

/** Ranked-row vertical padding (spec §5.2 / §4.4). */
private val RankedRowVerticalPadding = 6.dp

/** Ranked-row minimum height (spec §4.4); artwork + padding meets it exactly. */
private val RankedRowMinHeight = 56.dp

/**
 * Default start inset for [EditorialRowDivider] in a ranked list: the
 * §5.2 geometry — 16dp screen inset + 44dp artwork + 12dp gap = 72dp.
 */
private val EditorialRowDividerDefaultInset: Dp =
    OmegaSpacing.lg + RANKED_ARTWORK_EDGE.dp + OmegaSpacing.md

/** Rank numeral color alpha (spec §2.2: `onSurfaceVariant` at 60%). */
private const val RANK_NUMERAL_ALPHA = 0.6f

/** Row divider color alpha (spec §4.3: `outlineVariant` at 60%). */
private const val ROW_DIVIDER_ALPHA = 0.6f

/**
 * The masthead's companion label (spec §2.2): [OmegaType.eyebrow] in
 * `onSurfaceVariant`. The uppercase transform lives here, in the kit —
 * copy in strings stays natural case ("No account. Just music.") and no
 * feature module ever applies the transform itself. This and
 * [OmegaSectionLabel] are the only small-caps treatments the kit offers
 * (rulebook TY-7 ration); do not add variants.
 */
@Composable
fun OmegaEyebrow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = OmegaType.eyebrow,
    )
}

/**
 * Section taxonomy label for calm screens (spec §5.4): an UPPERCASE
 * [OmegaType.sectionLabel] in `onSurfaceVariant`, a section rule
 * ([Dp.Hairline], `outlineVariant`, spec §4.3) filling the remaining
 * width, and an optional trailing text action (`labelLarge`, `primary`)
 * that exists as a parameter only — no screen adds one without a ruling.
 *
 * The composable adds NO horizontal padding of its own: the caller
 * places it on the 16dp line (nested insets are how the stair-step
 * happened, rulebook LY-2), and owns all external spacing. The uppercase
 * transform is applied inside — copy stays natural case.
 *
 * Never regress (§5.4): this label is part of a section's non-empty
 * branch — it must never compose above a list that can be empty
 * (TELL-UNGUARDED-HEADER); emptiness belongs to the state components.
 */
@Composable
fun OmegaSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text.uppercase(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = OmegaType.sectionLabel,
        )
        Spacer(Modifier.width(OmegaSpacing.md))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = Dp.Hairline,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.width(OmegaSpacing.md))
            TextButton(onClick = onAction) {
                Text(
                    actionLabel,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/**
 * Ranked chart row (spec §5.2) — Home's "Trending" grammar. The API
 * mirrors [SongRow]'s: the caller passes the [Song], an explicit
 * [rank], an [onClick], and the same trailing-slot shape, so Home can
 * attach the shared overflow menu unchanged.
 *
 * The rank is the caller's 1-based position in this one chart, rendered
 * zero-padded ("01"–"05", [OmegaType.rankNumeral], tabular) in a 24dp
 * column. The composable never derives or renumbers it — ranks must
 * never shift with sort or search state (§5.2 never-regress). The
 * numeral is decoration: it is excluded from the semantics tree, and
 * the row announces title + meta only (the artwork is decorative for
 * the same reason; the title carries it).
 *
 * The meta line keeps [SongRow]'s protected-duration construction: the
 * artist ellipsizes first, the duration never truncates. Row dividers
 * are list-side ([EditorialRowDivider]); this row draws none.
 */
@Composable
fun RankedSongRow(
    song: Song,
    rank: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .heightIn(min = RankedRowMinHeight)
                .padding(
                    horizontal = OmegaSpacing.lg,
                    vertical = RankedRowVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString().padStart(2, '0'),
            modifier =
                Modifier
                    .width(RankColumnWidth)
                    .clearAndSetSemantics {},
            maxLines = 1,
            softWrap = false,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = RANK_NUMERAL_ALPHA,
                ),
            style = OmegaType.rankNumeral,
        )
        Spacer(Modifier.width(OmegaSpacing.md))
        Artwork(
            url = song.imageUrl,
            size = RANKED_ARTWORK_EDGE,
            corner = OmegaRadius.md,
            contentDescription = null,
        )
        Spacer(Modifier.width(OmegaSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                song.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                style = OmegaType.rowTitle,
            )
            // Protected duration slot, mirroring SongRow: the artist
            // ellipsizes first; the duration never truncates.
            Row(verticalAlignment = Alignment.CenterVertically) {
                val duration = formatDuration(song.durationSec)
                Text(
                    song.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = OmegaType.rowMeta,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (duration.isNotBlank()) {
                    Text(
                        if (song.artist.isBlank()) duration else " • $duration",
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = OmegaType.rowMeta,
                    )
                }
            }
        }
        trailing?.invoke()
    }
}

/**
 * List-side divider between ranked/list rows (spec §4.3): [Dp.Hairline]
 * in `outlineVariant` at 60% alpha, inset to the text column — never
 * under the artwork, and never after the last row: whether a divider
 * follows a row is the caller's list logic, not this composable's.
 *
 * [startInset] defaults to [EditorialRowDividerDefaultInset] (72dp,
 * the §5.2 geometry); a list whose row geometry differs passes its own
 * text-column offset.
 */
@Composable
fun EditorialRowDivider(
    modifier: Modifier = Modifier,
    startInset: Dp = EditorialRowDividerDefaultInset,
) {
    HorizontalDivider(
        modifier = modifier.padding(start = startInset),
        thickness = Dp.Hairline,
        color =
            MaterialTheme.colorScheme.outlineVariant.copy(
                alpha = ROW_DIVIDER_ALPHA,
            ),
    )
}

/**
 * The standard rail scroll affordance (spec §4.2): a 32dp-wide gradient
 * overlay (transparent → page `background`) for a rail's right edge, so
 * a partially visible card dissolves into the background instead of
 * slicing hard at the edge (TELL-SLICED-RAIL). Drawn above the cards;
 * a plain overlay with no input handling — touches pass through to the
 * rail beneath.
 *
 * Usage contract: place it in the `Box` wrapping the rail's `LazyRow`,
 * aligned to the center-end —
 * `OmegaRailEdgeFade(Modifier.align(Alignment.CenterEnd))` — and the
 * rail must sit directly on the page `background`: the fade's solid end
 * is the `background` role, never a container color, so a rail on any
 * other ground would fade to the wrong color.
 */
@Composable
fun OmegaRailEdgeFade(modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(OmegaSpacing.xxl)
            .fillMaxHeight()
            .background(
                Brush.horizontalGradient(
                    colors =
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background,
                        ),
                ),
            ),
    )
}
