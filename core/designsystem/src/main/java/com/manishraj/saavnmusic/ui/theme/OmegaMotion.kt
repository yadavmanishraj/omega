package com.manishraj.saavnmusic.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Legacy motion tokens — the documented FALLBACK set beside the
 * primary motion system, which is the theme's MotionScheme springs
 * (MotionScheme.expressive(), M3 Expressive spec §2.5/§4.3): scheme
 * specs drive themed components, skeletons, and the player
 * transition. These tween durations and emphasized easings predate
 * the scheme and remain for the shell chrome and call sites not yet
 * migrated to scheme specs; new animations take the scheme spec
 * first and reach for a token here only when no spec fits.
 *
 * The original tween doctrine still describes these tokens
 * themselves: decelerate on arrival, accelerate on leave, exits run
 * shorter than their matching enters. Durations are unscaled
 * values; the theme's binary reduced-motion state governs whether
 * spatial animation runs at all (crossfades replace slides).
 */
object OmegaMotion {
    /** Chip / press feedback; also the reduced-motion crossfade. */
    const val FAST_MS = 150

    /** State changes. */
    const val NORMAL_MS = 200

    /** Screen / sheet transitions, incl. the player container transform. */
    const val SLOW_MS = 300

    /** Exit companion to [SLOW_MS] (~67%: exit faster than enter). */
    const val EXIT_MS = 200

    /**
     * Player container transform (UIUX_DESIGN §7 signature transition):
     * 350 ms emphasized, run as a true mirror in both directions — for
     * this one transition the §7 spec supersedes the generic
     * exit-shorter rule above.
     */
    const val CONTAINER_MS = 350

    /** Material 3 emphasized-decelerate: arrivals land softly. */
    val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Material 3 emphasized-accelerate: departures leave quickly. */
    val emphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Material 3 emphasized: symmetric moves. */
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}
