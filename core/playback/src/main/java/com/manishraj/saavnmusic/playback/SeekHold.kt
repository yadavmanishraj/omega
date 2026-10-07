package com.manishraj.saavnmusic.playback

import kotlin.math.abs

/**
 * Holds a user-issued seek as authoritative until the player
 * confirms it (BUG-2).
 *
 * Seeks travel asynchronously: UI → MediaController → session →
 * ExoPlayer, and the controller-side position snapshot only updates
 * once the session reports back. Meanwhile the 500 ms position poll
 * in [PlayerController] kept publishing the PRE-seek position, so
 * the UI snapped back to the exact pre-gesture value and fast
 * gestures looked (and, via the slider, sometimes were) dropped.
 *
 * While a seek is pending, [resolve] returns the seek target instead
 * of the player's stale position. The hold ends when the player
 * reports a position within [confirmToleranceMs] of the target, when
 * a SEEK discontinuity confirms it ([onSeekConfirmed]), when the
 * current item changes (the seek is moot), or after
 * [holdTimeoutMs] — so a seek the engine truly lost degrades to the
 * player's real position instead of a permanent lie.
 *
 * Pure and clock-injected so it is unit-testable without a player.
 */
class PendingSeekTracker(
    private val holdTimeoutMs: Long = 2_500,
    private val confirmToleranceMs: Long = 1_500,
) {
    private var targetMs: Long? = null
    private var mediaId: String? = null
    private var issuedAtMs: Long = 0

    /** A new user seek supersedes any previous pending one. */
    fun onSeek(
        targetMs: Long,
        mediaId: String?,
        nowMs: Long,
    ) {
        this.targetMs = targetMs
        this.mediaId = mediaId
        this.issuedAtMs = nowMs
    }

    /** The player applied a seek (DISCONTINUITY_REASON_SEEK). */
    fun onSeekConfirmed() {
        targetMs = null
        mediaId = null
    }

    /** Playback moved on without a seek (new queue, next/prev, …). */
    fun clear() {
        targetMs = null
        mediaId = null
    }

    /**
     * The position to publish: the held target while a seek on the
     * current item is unconfirmed and fresh, otherwise the player's
     * own position (which also confirms/clears the hold as a side
     * effect when it lands near the target).
     */
    fun resolve(
        playerPositionMs: Long,
        currentMediaId: String?,
        nowMs: Long,
    ): Long {
        val target = targetMs ?: return playerPositionMs
        if (currentMediaId != mediaId || nowMs - issuedAtMs > holdTimeoutMs) {
            clear()
            return playerPositionMs
        }
        if (abs(playerPositionMs - target) <= confirmToleranceMs) {
            clear()
            return playerPositionMs
        }
        return target
    }
}

/**
 * The same user-intent hold for discrete player settings (shuffle,
 * repeat) whose values [PlayerController.sync] republishes from the
 * controller's local snapshot.
 *
 * That snapshot lags a user toggle by a session round-trip; an
 * `onEvents` batch queued before the toggle could make sync publish
 * the pre-toggle value, and with the player paused no further event
 * arrives to correct it — the toggle appeared not to stick (the
 * BUG-2 signature the QA run also saw on repeat/sleep toggles).
 *
 * After [intend], [resolve] returns the intended value until the
 * player reports that very value (confirmed) or [timeoutMs] passes.
 */
class IntentHold<T>(
    private val timeoutMs: Long = 2_000,
) {
    private var intended: T? = null
    private var issuedAtMs: Long = 0

    fun intend(
        value: T,
        nowMs: Long,
    ) {
        intended = value
        issuedAtMs = nowMs
    }

    /** The still-fresh intended value, or null (expiry clears the hold). */
    fun intendedValue(nowMs: Long): T? {
        val value = intended ?: return null
        if (nowMs - issuedAtMs > timeoutMs) {
            intended = null
            return null
        }
        return value
    }

    /** The value to publish given what the player currently reports. */
    fun resolve(
        actual: T,
        nowMs: Long,
    ): T {
        val value = intendedValue(nowMs) ?: return actual
        if (actual == value) {
            intended = null
            return actual
        }
        return value
    }
}
