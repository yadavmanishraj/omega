package com.manishraj.saavnmusic.ui.components

import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

/**
 * The app's loading indicator (M3 Expressive spec §3): the expressive
 * morphing [LoadingIndicator], wrapped so the alpha API lives in
 * exactly one place. Use for SHORT, undifferentiated waits (search
 * submit, lyrics fetch, queue resolution) — content-shaped loads keep
 * using the skeletons ([ShimmerList] / [ShimmerGrid]), which remain
 * the primary loading pattern.
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
