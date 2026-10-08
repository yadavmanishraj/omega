package com.manishraj.saavnmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.manishraj.saavnmusic.ui.theme.LocalReducedMotion

/**
 * THE shared artwork-palette crossfade (M3 Expressive spec §4.4).
 *
 * Every palette-driven color in the app — the Detail [GradientHeader]
 * gradient and content color, the mini-player container/content tint,
 * the full player's gradient and content color — animates through this
 * one helper on the theme's default EFFECTS spec: a critically damped
 * spring, so color never overshoots and rapid track skips retarget
 * smoothly mid-flight. It replaces the hand-tuned `tween(300)` copies
 * that predated the motion scheme (three in [GradientHeader], five in
 * the player).
 *
 * Reduced motion (system animator scale 0, via [LocalReducedMotion]):
 * the color snaps — palette changes are state, not decoration, so the
 * new color must still land instantly.
 */
@Composable
fun animatePaletteColor(
    targetValue: Color,
    label: String,
): State<Color> {
    val spec: AnimationSpec<Color> =
        if (LocalReducedMotion.current) {
            snap()
        } else {
            MaterialTheme.motionScheme.defaultEffectsSpec()
        }
    return animateColorAsState(
        targetValue = targetValue,
        animationSpec = spec,
        label = label,
    )
}

/**
 * The gradient stops of the chrome wash (visual-uplift spec §3.3):
 * the chrome tint at the wash alpha ([CHROME_WASH_ALPHA_DARK] /
 * [CHROME_WASH_ALPHA_LIGHT]) at the artwork edge, fading to fully
 * transparent at [CHROME_WASH_FRACTION] of the bar's width. Pure, so
 * the §3.3 numbers are pinned by unit tests without a renderer; the
 * tint's RGB passes through untouched — only its alpha is set to the
 * wash alpha (every construction path yields an opaque tint).
 */
internal fun chromeWashStops(
    tint: Color,
    darkTheme: Boolean,
): Array<Pair<Float, Color>> {
    val washAlpha = if (darkTheme) CHROME_WASH_ALPHA_DARK else CHROME_WASH_ALPHA_LIGHT
    return arrayOf(
        0f to tint.copy(alpha = washAlpha),
        CHROME_WASH_FRACTION to Color.Transparent,
    )
}

/**
 * The mini-player's artwork wash (visual-uplift spec §3.3): a
 * horizontal brush of this palette's [ArtworkPaletteColors.chromeTint]
 * at the wash alpha, fading to transparent over the artwork-side 40%
 * of the bar's width. The consumer draws it BEHIND the bar's content,
 * clipped to the bar's bounds (e.g. as the first `background` layer
 * inside the clipped chrome container) — this helper ships color
 * only, no layout.
 *
 * Text never sits on tint alone: the bar's title/meta colors are the
 * scheme pair for `surfaceContainer` (`onSurface` /
 * `onSurfaceVariant`), whose contrast is guaranteed against the
 * container; the wash's alpha cap exists precisely so the local
 * background shift cannot break that guarantee.
 *
 * The tint retargets through [animatePaletteColor] — the ONE shared
 * effects-spec crossfade (§3.5, rulebook MF-5) — so three rapid skips
 * land on the final track's tint with no intermediate residue. The
 * bar's container color is NOT animated here or anywhere: it is the
 * constant `surfaceContainer` role now (§3.1), and no second
 * animation path may be added for it.
 */
@Composable
fun rememberChromeWashBrush(
    palette: ArtworkPaletteColors,
    darkTheme: Boolean,
    label: String = "chromeWash",
): Brush {
    val tint by animatePaletteColor(
        targetValue = palette.chromeTint(darkTheme),
        label = label,
    )
    return remember(tint, darkTheme) {
        Brush.horizontalGradient(colorStops = chromeWashStops(tint, darkTheme))
    }
}
