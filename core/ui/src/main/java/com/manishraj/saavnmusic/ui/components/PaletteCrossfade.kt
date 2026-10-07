package com.manishraj.saavnmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
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
