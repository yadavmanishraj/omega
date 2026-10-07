package com.manishraj.saavnmusic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Segmented list kit (M3 Expressive spec §3): grouped rows for
 * Settings groups and Library's Favorites / Downloads / History,
 * replacing divider-separated flat lists. Each item is its own filled
 * segment (surfaceContainerHigh) separated by a 2dp gap — grouping is
 * carried by containment and shape, not hairlines. Wave 2 applies
 * these per screen; the kit lands first so the pattern exists once.
 */
@Composable
fun OmegaSegmentedList(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content,
    )
}

/**
 * Free-form segmented container (polish item 13): the segment
 * surface — surfaceContainerHigh fill, large shape — hosting
 * arbitrary content that [OmegaSegmentedListItem]'s
 * headline/supporting/trailing slots can't express (a label over a
 * choice group, a full [SongRow], a progress row). This is the one
 * implementation of the treatment Settings and Library previously
 * hand-rolled per screen.
 *
 * Content is laid out in a [Column]; [contentPadding] defaults to
 * zero because row content (ListItem, SongRow) carries its own
 * padding — free-standing blocks (Settings groups) pass
 * `PaddingValues(OmegaSpacing.lg)`. Separate containers stack with
 * the kit's 2dp gap via [OmegaSegmentedList]; grouping is carried by
 * containment, not dividers, inside a container too.
 *
 * ```
 * OmegaSegmentedContainer(Modifier.fillMaxWidth()) {
 *     Text("Appearance", style = MaterialTheme.typography.titleMedium)
 *     OmegaChoiceGroup(...)
 * }
 * ```
 */
@Composable
fun OmegaSegmentedContainer(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        Column(
            Modifier.padding(contentPadding),
            content = content,
        )
    }
}

/**
 * One segment. [selected] gives the highlighted treatment (primary
 * container + its on-colors) — e.g. the now-playing row. Rows keep
 * baseline type (spec §2.2: rows are never emphasized) and the
 * duration/artist rules of [SongRow] are unaffected — this is the
 * generic container for settings/library rows, not a song row.
 */
@Composable
fun OmegaSegmentedListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    ListItem(
        supportingContent =
            supporting?.let {
                {
                    // Supporting copy is sentence-length prose (a
                    // setting's explanation), not a name: it gets two
                    // lines so it wraps at large font scales instead
                    // of truncating mid-word — at font 1.33 the
                    // one-line cap cut "Surfaces follow your
                    // wallpaper's colors" to "…wallpap…". The
                    // headline above stays at one line: titles
                    // ellipsize, explanations wrap.
                    Text(
                        it,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
        leadingContent = leading,
        trailingContent = trailing,
        colors =
            ListItemDefaults.colors(
                containerColor =
                    if (selected) scheme.primaryContainer else scheme.surfaceContainerHigh,
                headlineColor =
                    if (selected) scheme.onPrimaryContainer else Color.Unspecified,
                supportingColor =
                    if (selected) scheme.onPrimaryContainer else Color.Unspecified,
            ),
        modifier =
            modifier
                .clip(MaterialTheme.shapes.large)
                .then(
                    if (onClick != null) {
                        Modifier.clickable { onClick() }
                    } else {
                        Modifier
                    },
                ),
    ) {
        Text(
            headline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
