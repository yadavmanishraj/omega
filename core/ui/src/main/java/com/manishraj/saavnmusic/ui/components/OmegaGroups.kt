package com.manishraj.saavnmusic.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow

/**
 * Single-choice connected button group (M3 Expressive spec §3):
 * theme mode, playback/download quality, playback speed, list sort.
 * Exactly one option is checked; tapping another moves the selection
 * (tapping the checked option is a no-op — selection is required).
 * Items that don't fit overflow into the group's menu instead of
 * compressing their labels. The group APIs are wrapped in this file
 * (and [OmegaActionGroup] below) so the alpha surface lives in one
 * place; the legacy segmented buttons / choice chip rows are
 * deprecated under Expressive — Wave 2 swaps the call sites to these.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> OmegaChoiceGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    icon: ((T) -> ImageVector)? = null,
) {
    ButtonGroup(
        overflowIndicator = { menuState -> ButtonGroupDefaults.OverflowIndicator(menuState) },
        modifier = modifier,
    ) {
        options.forEach { option ->
            toggleableItem(
                checked = option == selected,
                label = label(option),
                onCheckedChange = { checked -> if (checked) onSelect(option) },
                icon =
                    icon?.let { iconFor ->
                        { Icon(iconFor(option), contentDescription = null) }
                    },
            )
        }
    }
}

/**
 * The Detail action cluster (spec §5): Play all (filled, M) + Shuffle
 * (tonal, M) as ONE connected group — Play all is the screen's single
 * filled action; Shuffle is its tonal peer, not a competitor.
 *
 * Assembled from [ButtonGroupDefaults]' connected shapes (leading /
 * trailing + their press shapes) so the pair shares the group's
 * geometry and press shape-morph while keeping the filled/tonal
 * emphasis split the raw group items can't express; M size comes from
 * [ButtonDefaults.MediumContentPadding]. If a future alpha lets group
 * items carry individual emphasis, this wrapper is the single place
 * that changes.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OmegaActionGroup(
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryIcon: ImageVector? = null,
    secondaryIcon: ImageVector? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        Button(
            onClick = onPrimary,
            shapes =
                ButtonShapes(
                    shape = ButtonGroupDefaults.connectedLeadingButtonShape,
                    pressedShape = ButtonGroupDefaults.connectedLeadingButtonPressShape,
                ),
            contentPadding = ButtonDefaults.MediumContentPadding,
        ) {
            if (primaryIcon != null) {
                Icon(primaryIcon, contentDescription = null)
            }
            Text(primaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FilledTonalButton(
            onClick = onSecondary,
            shapes =
                ButtonShapes(
                    shape = ButtonGroupDefaults.connectedTrailingButtonShape,
                    pressedShape = ButtonGroupDefaults.connectedTrailingButtonPressShape,
                ),
            contentPadding = ButtonDefaults.MediumContentPadding,
        ) {
            if (secondaryIcon != null) {
                Icon(secondaryIcon, contentDescription = null)
            }
            Text(secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
