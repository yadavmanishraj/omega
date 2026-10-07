package com.manishraj.saavnmusic.ui.components

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
    fun showMessage(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
    ) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result =
                hostState.showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    duration = SnackbarDuration.Short,
                )
            if (result == SnackbarResult.ActionPerformed) {
                onAction?.invoke()
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

/** The host composable for the shell's Scaffold `snackbarHost` slot. */
@Composable
fun OmegaSnackbarHost(
    controller: OmegaSnackbarController,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = controller.hostState, modifier = modifier)
}
