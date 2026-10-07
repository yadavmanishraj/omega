package com.manishraj.saavnmusic.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Snackbar serialization (F-06): an action-bearing message (Undo)
 * must hold the stage for its full window — a later message queues
 * behind it instead of displacing it.
 */
class SnackbarPresenterTest {
    private fun request(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
    ) = SnackbarRequest(
        message = message,
        actionLabel = actionLabel,
        duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
        onAction = onAction,
    )

    @Test
    fun `a later message waits until the undo message completes`() =
        runBlocking {
            val shown = mutableListOf<String>()
            val undoGate = CompletableDeferred<Unit>()
            val presenter =
                SnackbarPresenter { req ->
                    shown += req.message
                    if (req.message == "undo") undoGate.await()
                    SnackbarResult.Dismissed
                }

            val undo = async { presenter.present(request("undo", actionLabel = "Undo")) }
            val plain = async { presenter.present(request("plain")) }
            // Let both coroutines run as far as they can: the plain
            // message must still be queued behind the gated one.
            repeat(10) { yield() }
            assertEquals(listOf("undo"), shown)
            assertFalse(plain.isCompleted)

            undoGate.complete(Unit)
            awaitAll(undo, plain)
            assertEquals(listOf("undo", "plain"), shown)
        }

    @Test
    fun `messages present in FIFO order`() =
        runBlocking {
            val shown = mutableListOf<String>()
            val presenter =
                SnackbarPresenter { req ->
                    shown += req.message
                    SnackbarResult.Dismissed
                }
            listOf("one", "two", "three").map { async { presenter.present(request(it)) } }.awaitAll()
            assertEquals(listOf("one", "two", "three"), shown)
        }

    @Test
    fun `the action runs only when the result is ActionPerformed`() =
        runBlocking {
            var acted = false
            val presenter = SnackbarPresenter { SnackbarResult.ActionPerformed }
            presenter.present(request("undo", actionLabel = "Undo", onAction = { acted = true }))
            assertTrue(acted)

            var actedOnDismiss = false
            val dismissing = SnackbarPresenter { SnackbarResult.Dismissed }
            dismissing.present(request("undo", actionLabel = "Undo", onAction = { actedOnDismiss = true }))
            assertFalse(actedOnDismiss)
        }
}
