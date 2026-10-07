package com.manishraj.saavnmusic.playback

/*
 * Sleep-timer truth (F-05): the armed preset and the deadline live
 * HERE, in the playback layer — never in a composition-local
 * `remember`. The FullPlayer used to keep the displayed minutes in
 * its own `remember` while the actual timer lived in
 * [PlayerController]'s coroutine, so collapsing and reopening the
 * player showed "off" for a timer that was still armed and would
 * still stop playback (and the next tap re-armed instead of
 * cancelling). The controller now publishes the armed preset and
 * the live remaining time in [PlayerState]; the UI renders state
 * only.
 *
 * Pure and clock-injected so it is unit-testable without a player,
 * following the [PendingSeekTracker] pattern.
 */

/**
 * The sleep-timer preset set, in minutes — the choices the
 * player's sleep menu offers (besides Off). Selection is DIRECT:
 * choosing a preset arms exactly it via
 * [PlayerController.setSleepTimer]; there is no cycle. The old
 * tap-to-cycle moon hid the choices from the user entirely —
 * every exploratory tap changed the commitment instead of
 * revealing the options (impeccable critique, Task 4).
 */
val SLEEP_TIMER_PRESETS: List<Int> = listOf(15, 30, 60)

/**
 * Tracks one armed timer: the preset the user chose and the
 * wall-clock deadline it fires at. Remaining time is always derived
 * from the deadline, so the published countdown and the actual stop
 * can never drift apart.
 */
class SleepTimerTracker {
    /** The armed preset in minutes; 0 = no timer. */
    var armedMinutes: Int = 0
        private set

    private var deadlineMs: Long = 0

    /** Arms (or re-arms) the timer for [minutes] from [nowMs]. */
    fun arm(
        minutes: Int,
        nowMs: Long,
    ) {
        armedMinutes = minutes
        deadlineMs = nowMs + minutes * 60_000L
    }

    /** Disarms: no preset, no deadline. */
    fun cancel() {
        armedMinutes = 0
        deadlineMs = 0
    }

    val isArmed: Boolean get() = armedMinutes > 0

    /** Milliseconds until the timer fires; 0 when disarmed or due. */
    fun remainingMs(nowMs: Long): Long = if (!isArmed) 0 else (deadlineMs - nowMs).coerceAtLeast(0)

    /** True when an armed timer's deadline has been reached. */
    fun isExpired(nowMs: Long): Boolean = isArmed && nowMs >= deadlineMs
}
