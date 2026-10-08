package com.manishraj.saavnmusic.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import com.manishraj.saavnmusic.ui.theme.OmegaSpacing

/**
 * Font scale at/above which [OmegaChoiceGroup]'s wrap arrangement
 * takes over when a caller opts in: between the user's everyday
 * 1.33 (where the connected group + overflow menu works) and the
 * 2.0 maximum, where the A17 audit (F-23) found groups collapsing
 * into the overflow menu until NO selection was visible.
 */
private const val LARGE_FONT_WRAP_THRESHOLD = 1.6f

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
 *
 * [wrapAtLargeFont] is the caller's opt-in escape for surfaces where
 * a hidden selection is a correctness problem (Settings, F-23): at
 * very large font scales the group renders as wrapping filter chips
 * instead — every option stays visible, so the checked one can
 * never hide inside the overflow menu. Compact in-context groups
 * (player speed) keep the default overflow behavior.
 *
 * Selected treatments (uplift spec §5.7, verified against the pinned
 * material3 1.5.0-alpha29 AAR + rendered Settings captures — re-check
 * both on any alpha bump, this is the silent-drift class):
 * - Connected group: `toggleableItem` exposes NO colors/shapes slot
 *   in alpha29; its checked segment renders `primary` / `onPrimary`
 *   (the expressive connected-group treatment) and the unchecked
 *   segments `surfaceContainer` / `onSurfaceVariant`. There is no
 *   supported override — a different fill means replacing the group,
 *   which CP-8 forbids.
 * - Chip fallback: FilterChip's selected fill IS the §5.7 treatment —
 *   `secondaryContainer` / `onSecondaryContainer` — and its shapes
 *   are the expressive chip tokens (base medium, pressed full,
 *   checked small = OmegaShapes.small, 8). Do not pin a static
 *   shape: the morph is the design.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun <T> OmegaChoiceGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    icon: ((T) -> ImageVector)? = null,
    wrapAtLargeFont: Boolean = false,
) {
    if (wrapAtLargeFont && LocalDensity.current.fontScale >= LARGE_FONT_WRAP_THRESHOLD) {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(OmegaSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OmegaSpacing.sm),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { if (option != selected) onSelect(option) },
                    label = { Text(label(option), maxLines = 1, softWrap = false) },
                    leadingIcon =
                        icon?.let { iconFor ->
                            { Icon(iconFor(option), contentDescription = null) }
                        },
                )
            }
        }
        return
    }
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
