package com.manishraj.saavnmusic.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Motion tokens (REDESIGN_SPEC §2.7, aligned with Material 3 motion):
 * decelerate on arrival, accelerate on leave, and exits run shorter
 * than their matching enters. No springs or overshoot anywhere — a
 * media app stays calm. Durations are the unscaled values; callers
 * multiply them by the system animator duration scale, and drop
 * spatial animation entirely when that scale is 0 (reduced motion:
 * crossfades replace slides).
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
