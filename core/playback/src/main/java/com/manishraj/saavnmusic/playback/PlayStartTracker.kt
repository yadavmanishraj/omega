package com.manishraj.saavnmusic.playback

import com.manishraj.saavnmusic.domain.Song

/**
 * Decides which track starts count as "plays" for History /
 * "Jump back in" (BUG-5).
 *
 * Play recording used to live only in the row-tap play path, so
 * tracks reached by auto-advance, Next/Prev, or a queue-sheet tap
 * were never recorded. Recording now happens centrally in
 * [PlayerController]: explicit queue starts go through
 * [onExplicitPlay], and every observed current-item change goes
 * through [onObservedCurrent]. This tracker is the single decider
 * both paths share, so a play is recorded exactly once:
 *
 * - An explicit play (row tap → `playQueue`) always records its
 *   start song. The observer echo when that item becomes current
 *   is suppressed because it is already the last recorded song.
 * - An observed TRANSITION to a different song records it — this
 *   is what catches auto-advance, Next/Prev, and queue-sheet taps.
 *   A stale observation of the outgoing song racing in after an
 *   explicit play is not a transition (it equals the last observed
 *   song) and records nothing, so History ordering stays truthful.
 * - The FIRST item observed in a process never records: it is a
 *   session the service restored, not a new play.
 * - Observing the same song again never records. In particular a
 *   repeat-one restart and a repeat-all wrap of a single-song queue
 *   keep the same mediaId across the boundary, so a track looping
 *   onto itself counts as ONE play, not a new one per loop.
 *
 * Pure and unit-tested; the controller feeds it and emits what it
 * returns on [PlayerController.playStarts].
 */
class PlayStartTracker {
    /** Song the observer most recently saw as current. */
    private var lastObservedId: String? = null

    /** Song most recently handed out for recording (either path). */
    private var lastRecordedId: String? = null
    private var seenAnyItem: Boolean = false

    /**
     * Playback was explicitly (re)started at [song] via playQueue.
     * Returns the song to record (always [song]).
     */
    fun onExplicitPlay(song: Song): Song {
        lastRecordedId = song.id
        seenAnyItem = true
        return song
    }

    /**
     * The player's current item was observed as [current] (null when
     * nothing is current). Returns the song to record, or null when
     * this observation is not a new play.
     */
    fun onObservedCurrent(current: Song?): Song? {
        if (current == null) {
            lastObservedId = null
            return null
        }
        if (!seenAnyItem) {
            // First item seen this process (e.g. a session restored
            // by the service): it was already playing — not a new play.
            seenAnyItem = true
            lastObservedId = current.id
            return null
        }
        if (current.id == lastObservedId) return null
        lastObservedId = current.id
        if (current.id == lastRecordedId) return null
        lastRecordedId = current.id
        return current
    }
}
