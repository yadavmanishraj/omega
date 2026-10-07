package com.manishraj.saavnmusic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
        headlineContent = {
            Text(
                headline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent =
            supporting?.let {
                {
                    Text(
                        it,
                        maxLines = 1,
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
    )
}
