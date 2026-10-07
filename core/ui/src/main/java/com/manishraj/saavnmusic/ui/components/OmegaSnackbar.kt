package com.manishraj.saavnmusic.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * App-wide snackbar feedback (M3 Expressive spec §3/§7): short
 * confirmations — "Added to {playlist}", "Download queued",
 * "Removed …" with Undo where destructive. ONE host is mounted in the
 * app shell's Scaffold (its snackbar slot sits above the bottom bar,
 * i.e. above the mini-player); any screen reaches it through
 * [LocalOmegaSnackbar] instead of growing a private host.
 *
 * The local is null outside the shell (previews, tests) — call sites
 * use `LocalOmegaSnackbar.current?.showMessage(...)` and simply skip
 * the feedback there, the same way the picker closing used to be the
 * only confirmation.
 */
class OmegaSnackbarController internal constructor(
    val hostState: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    private val presenter =
        SnackbarPresenter { request ->
            hostState.showSnackbar(
                message = request.message,
                actionLabel = request.actionLabel,
                duration = request.duration,
            )
        }

    fun showMessage(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
    ) {
        val request =
            SnackbarRequest(
                message = message,
                actionLabel = actionLabel,
                // A message carrying an action (Undo) gets the long
                // duration: its recovery window is the whole point.
                duration =
                    if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
                onAction = onAction,
            )
        scope.launch { presenter.present(request) }
    }
}

/** One queued snackbar: what to show and what its action does. */
internal class SnackbarRequest(
    val message: String,
    val actionLabel: String?,
    val duration: SnackbarDuration,
    val onAction: (() -> Unit)? = null,
)

/**
 * Serializes snackbar presentation (F-06). The old controller called
 * `currentSnackbarData?.dismiss()` before every show, so ANY later
 * message silently killed the one on screen — an Undo snackbar for a
 * destructive action could be stolen ~2.5 s into its window by an
 * unrelated "Will play next", turning a reversible action into a
 * permanent one.
 *
 * Here a [Mutex] guarantees one presentation at a time: a message
 * displays only after the previous one completes (action tapped,
 * timeout, or swipe-dismiss), in FIFO order. An action-bearing
 * message is therefore never displaced by a later one.
 *
 * The [show] seam keeps the queue testable without a composed host.
 */
internal class SnackbarPresenter(
    private val show: suspend (SnackbarRequest) -> SnackbarResult,
) {
    private val mutex = Mutex()

    suspend fun present(request: SnackbarRequest) {
        mutex.withLock {
            if (show(request) == SnackbarResult.ActionPerformed) {
                request.onAction?.invoke()
            }
        }
    }
}

/** The shell-mounted controller; null where no host is mounted. */
val LocalOmegaSnackbar = compositionLocalOf<OmegaSnackbarController?> { null }

@Composable
fun rememberOmegaSnackbarController(): OmegaSnackbarController {
    val hostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    return remember(hostState, scope) { OmegaSnackbarController(hostState, scope) }
}

/**
 * The host composable for the shell's Scaffold `snackbarHost` slot.
 *
 * The `snackbar` slot brands the bar (impeccable critique P1): the stock
 * Material snackbar renders the inverse scheme — an off-white bar with
 * a purple action in dark theme — which reads as a foreign app inside
 * Omega's indigo/green system. Here the container is the same raised
 * surface role the app's grouped surfaces use (`surfaceContainerHigh`
 * in segmented lists, cards, and the search bar), the message takes
 * its paired on-color, the action takes brand primary (green in both
 * themes — the dynamic scheme keeps Omega's primary by theme rule),
 * and the dismiss affordance recedes to `onSurfaceVariant`. Scheme
 * roles only, so the bar tracks dark, light, and dynamic color with
 * no per-theme branching. Presentation behavior (FIFO queue, durations)
 * lives in [SnackbarPresenter] and is untouched by this styling.
 */
@Composable
fun OmegaSnackbarHost(
    controller: OmegaSnackbarController,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = controller.hostState,
        modifier = modifier,
    ) { data ->
        val scheme = MaterialTheme.colorScheme
        Snackbar(
            snackbarData = data,
            shape = MaterialTheme.shapes.large,
            containerColor = scheme.surfaceContainerHigh,
            contentColor = scheme.onSurface,
            actionContentColor = scheme.primary,
            dismissActionContentColor = scheme.onSurfaceVariant,
        )
    }
}
