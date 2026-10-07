package com.manishraj.saavnmusic

import com.manishraj.saavnmusic.feature.player.ErrorChannelArbiter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Error-channel arbitration (impeccable critique, Task 4): each
 * playback failure is announced on exactly one surface — the full
 * player's inline row, or the mini-player's snackbar when the
 * player is collapsed.
 */
class ErrorChannelArbiterTest {
    @Test
    fun `first snackbar claim for a seq wins`() {
        val arbiter = ErrorChannelArbiter()
        assertTrue(arbiter.claimForSnackbar(1))
    }

    @Test
    fun `same seq cannot be claimed twice`() {
        val arbiter = ErrorChannelArbiter()
        assertTrue(arbiter.claimForSnackbar(1))
        assertFalse(arbiter.claimForSnackbar(1))
    }

    @Test
    fun `seq presented inline cannot be claimed for the snackbar`() {
        val arbiter = ErrorChannelArbiter()
        arbiter.markPresented(1)
        assertFalse(arbiter.claimForSnackbar(1))
    }

    @Test
    fun `inline presentation of an old seq does not block a newer failure`() {
        val arbiter = ErrorChannelArbiter()
        arbiter.markPresented(1)
        assertTrue(arbiter.claimForSnackbar(2))
    }

    @Test
    fun `out-of-order older seq is never claimable`() {
        val arbiter = ErrorChannelArbiter()
        assertTrue(arbiter.claimForSnackbar(3))
        assertFalse(arbiter.claimForSnackbar(2))
    }

    @Test
    fun `marking a newer seq supersedes an earlier snackbar claim`() {
        val arbiter = ErrorChannelArbiter()
        assertTrue(arbiter.claimForSnackbar(1))
        arbiter.markPresented(2)
        assertFalse(arbiter.claimForSnackbar(2))
        assertTrue(arbiter.claimForSnackbar(3))
    }

    @Test
    fun `seq zero is never claimable`() {
        val arbiter = ErrorChannelArbiter()
        assertFalse(arbiter.claimForSnackbar(0))
    }
}
