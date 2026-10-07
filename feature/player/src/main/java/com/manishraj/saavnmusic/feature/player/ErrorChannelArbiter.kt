package com.manishraj.saavnmusic.feature.player

/**
 * Playback-error presentation arbitration (impeccable critique,
 * Task 4): each failure ([com.manishraj.saavnmusic.playback.PlayerState.errorSeq])
 * is announced on exactly ONE surface.
 *
 * - The FULL player composed → its inline error row + Retry
 *   presents the failure; [markPresented] records the seq so the
 *   snackbar channel never repeats it.
 * - Only the MINI player composed (collapsed context, no inline
 *   surface) → the snackbar presents it; [claimForSnackbar] wins
 *   the seq exactly once.
 *
 * Both player surfaces share the activity-scoped [PlayerViewModel],
 * which owns the single instance of this arbiter, so a claim made
 * by either surface is visible to the other. Claims are plain
 * main-thread state writes — the check-and-set in
 * [claimForSnackbar] has no suspension point, so two surfaces
 * racing on the same seq cannot both win.
 *
 * Pure and clock-free so the arbitration is unit-testable without
 * a composition.
 */
class ErrorChannelArbiter {
    /** The highest error seq already presented on some surface. */
    private var presentedSeq: Int = 0

    /**
     * Records that [seq] is being presented by the inline surface
     * (the full player). Called on every error-seq change while
     * the full player is composed, including its first composition
     * over an outstanding error.
     */
    fun markPresented(seq: Int) {
        if (seq > presentedSeq) presentedSeq = seq
    }

    /**
     * Claims [seq] for the snackbar channel: true exactly once per
     * seq, and only when no surface has presented it yet. A false
     * result means the failure was (or is being) announced inline
     * and the snackbar must stay silent.
     */
    fun claimForSnackbar(seq: Int): Boolean {
        if (seq <= presentedSeq) return false
        presentedSeq = seq
        return true
    }
}
