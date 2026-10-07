package com.manishraj.saavnmusic.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Transport icon morphs (M3 Expressive spec §4.7): state changes on
 * the player's icon-only controls animate instead of swapping —
 * crossfade on the theme's FAST EFFECTS spec (color/alpha never
 * overshoot) + scale on FAST SPATIAL (the one sanctioned 0.6-damping
 * bounce, reserved for small user-initiated elements). Content
 * descriptions track the state, so TalkBack hears the same change
 * sighted users see. Wave 2's player adopts these; the mini-player
 * and full player must not hand-roll their own swaps.
 */
@Composable
fun OmegaPlayPauseIcon(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    val motion = MaterialTheme.motionScheme
    AnimatedContent(
        targetState = isPlaying,
        transitionSpec = {
            (fadeIn(motion.fastEffectsSpec()) + scaleIn(motion.fastSpatialSpec(), initialScale = 0.8f)) togetherWith
                (fadeOut(motion.fastEffectsSpec()) + scaleOut(motion.fastSpatialSpec(), targetScale = 0.8f))
        },
        label = "playPauseMorph",
    ) { playing ->
        Icon(
            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (playing) "Pause" else "Play",
            modifier = modifier,
            tint = tint,
        )
    }
}

/** Favorite heart with the pop morph; [isFavorite] drives both the
 * glyph (filled vs outline) and the description ("Add to favorites"
 * vs "Remove from favorites"). */
@Composable
fun OmegaFavoriteIcon(
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    val motion = MaterialTheme.motionScheme
    AnimatedContent(
        targetState = isFavorite,
        transitionSpec = {
            (fadeIn(motion.fastEffectsSpec()) + scaleIn(motion.fastSpatialSpec(), initialScale = 0.6f)) togetherWith
                (fadeOut(motion.fastEffectsSpec()) + scaleOut(motion.fastSpatialSpec(), targetScale = 0.6f))
        },
        label = "favoriteMorph",
    ) { favorite ->
        Icon(
            imageVector = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (favorite) "Remove from favorites" else "Add to favorites",
            modifier = modifier,
            tint = tint,
        )
    }
}
