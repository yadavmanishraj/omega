package com.manishraj.saavnmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.manishraj.saavnmusic.ui.theme.OmegaRadius
import com.manishraj.saavnmusic.ui.theme.OmegaSpacing

/**
 * The app's loading indicator (M3 Expressive spec §3): the expressive
 * morphing [LoadingIndicator], wrapped so the alpha API lives in
 * exactly one place. Use for SHORT, undifferentiated waits (search
 * submit, lyrics fetch, queue resolution) — content-shaped loads keep
 * using the skeleton ([ShimmerList]), which remains the primary
 * loading pattern. (The grid skeleton ShimmerGrid was removed in the
 * A17 fix wave, F-28: defined, documented, and never called.)
 *
 * The indicator is pure motion, so per spec §8 it announces itself:
 * a polite live region with a content description gives TalkBack
 * users the state the sighted user gets from the morph.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OmegaLoadingIndicator(
    modifier: Modifier = Modifier,
    contained: Boolean = false,
    contentDescription: String = "Loading",
) {
    val announced =
        modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            this.contentDescription = contentDescription
        }
    if (contained) {
        ContainedLoadingIndicator(modifier = announced)
    } else {
        LoadingIndicator(modifier = announced)
    }
}

/**
 * Rail-shaped loading skeleton (polish item 9): card placeholders in
 * a horizontal row matching [MediaCard] geometry (uplift §5.3) — a
 * square artwork block at the card corner (16dp) over title/subtitle
 * bars, each card [cardWidth] wide with NO inner padding (the card
 * carries none; rails own spacing, §4.2) — so a section that
 * resolves into a horizontal card rail LOADS as a rail, not as the
 * list rows [ShimmerList] promises. The sweep is the shared shimmer
 * phase, so under reduced motion the blocks pin static, exactly like
 * [ShimmerList]. Overflow past the screen edge is clipped, never
 * scrolled: a skeleton is a promise of shape, not content.
 *
 * The silhouette stays rounded-square for EVERY rail, including
 * Artists: the circular-skeleton variant is deferred (coordinator
 * ruling D, uplift wave 2) — geometry aligns with MediaCard, the
 * shape swap on resolve is the recorded, accepted remainder.
 */
@Composable
fun ShimmerRail(
    modifier: Modifier = Modifier,
    itemCount: Int = 4,
    cardWidth: Int = 124,
) {
    val phase = rememberShimmerPhase()
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val blockColor = MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier
            .fillMaxWidth()
            .clipToBounds()
            .padding(horizontal = OmegaSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(OmegaSpacing.md),
    ) {
        repeat(itemCount) {
            Column(
                Modifier.width(cardWidth.dp),
            ) {
                Box(
                    Modifier
                        .size(cardWidth.dp)
                        .clip(RoundedCornerShape(OmegaRadius.xl))
                        .background(blockColor)
                        .shimmerSweep(phase, highlight),
                )
                Spacer(Modifier.height(OmegaSpacing.sm))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(blockColor)
                        .shimmerSweep(phase, highlight),
                )
                Spacer(Modifier.height(OmegaSpacing.xs))
                Box(
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(12.dp)
                        .background(blockColor)
                        .shimmerSweep(phase, highlight),
                )
            }
        }
    }
}
