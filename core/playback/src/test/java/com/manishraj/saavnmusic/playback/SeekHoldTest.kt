package com.manishraj.saavnmusic.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** User-intent holds (BUG-2): seeks and toggles must win over stale player snapshots. */
class SeekHoldTest {
    private val tracker = PendingSeekTracker(holdTimeoutMs = 2_500, confirmToleranceMs = 1_500)

    @Test
    fun `no pending seek publishes the player position`() {
        assertEquals(12_000L, tracker.resolve(12_000L, "a", nowMs = 100))
    }

    @Test
    fun `pending seek holds the target while the player still reports the old position`() {
        tracker.onSeek(targetMs = 90_000, mediaId = "a", nowMs = 1_000)
        assertEquals(90_000L, tracker.resolve(13_000L, "a", nowMs = 1_100))
        assertEquals(90_000L, tracker.resolve(13_400L, "a", nowMs = 1_600))
    }

    @Test
    fun `player position near the target confirms and clears the hold`() {
        tracker.onSeek(targetMs = 90_000, mediaId = "a", nowMs = 1_000)
        // Player landed (and advanced a bit while playing).
        assertEquals(90_800L, tracker.resolve(90_800L, "a", nowMs = 1_300))
        // Hold cleared: subsequent positions pass through untouched.
        assertEquals(95_000L, tracker.resolve(95_000L, "a", nowMs = 1_400))
    }

    @Test
    fun `seek discontinuity confirmation clears the hold`() {
        tracker.onSeek(targetMs = 90_000, mediaId = "a", nowMs = 1_000)
        tracker.onSeekConfirmed()
        assertEquals(13_000L, tracker.resolve(13_000L, "a", nowMs = 1_100))
    }

    @Test
    fun `hold expires so a lost seek falls back to the real position`() {
        tracker.onSeek(targetMs = 90_000, mediaId = "a", nowMs = 1_000)
        assertEquals(20_000L, tracker.resolve(20_000L, "a", nowMs = 3_501))
    }

    @Test
    fun `item change makes the pending seek moot`() {
        tracker.onSeek(targetMs = 90_000, mediaId = "a", nowMs = 1_000)
        assertEquals(0L, tracker.resolve(0L, "b", nowMs = 1_100))
        // And it stays cleared back on the original item.
        assertEquals(5_000L, tracker.resolve(5_000L, "a", nowMs = 1_200))
    }

    @Test
    fun `a newer seek supersedes the pending one`() {
        tracker.onSeek(targetMs = 90_000, mediaId = "a", nowMs = 1_000)
        tracker.onSeek(targetMs = 30_000, mediaId = "a", nowMs = 1_200)
        assertEquals(30_000L, tracker.resolve(13_000L, "a", nowMs = 1_300))
    }

    @Test
    fun `clear drops the hold`() {
        tracker.onSeek(targetMs = 90_000, mediaId = "a", nowMs = 1_000)
        tracker.clear()
        assertEquals(13_000L, tracker.resolve(13_000L, "a", nowMs = 1_100))
    }
}

class IntentHoldTest {
    private val hold = IntentHold<Boolean>(timeoutMs = 2_000)

    @Test
    fun `no intent publishes the actual value`() {
        assertEquals(false, hold.resolve(actual = false, nowMs = 100))
        assertNull(hold.intendedValue(nowMs = 100))
    }

    @Test
    fun `intent wins over a stale actual value`() {
        hold.intend(true, nowMs = 1_000)
        assertEquals(true, hold.resolve(actual = false, nowMs = 1_100))
        assertEquals(true, hold.resolve(actual = false, nowMs = 1_900))
    }

    @Test
    fun `player confirming the intent clears the hold`() {
        hold.intend(true, nowMs = 1_000)
        assertEquals(true, hold.resolve(actual = true, nowMs = 1_200))
        // Cleared: a later genuine change by the player is honored.
        assertEquals(false, hold.resolve(actual = false, nowMs = 1_300))
    }

    @Test
    fun `intent expires after the timeout`() {
        hold.intend(true, nowMs = 1_000)
        assertEquals(false, hold.resolve(actual = false, nowMs = 3_001))
        assertNull(hold.intendedValue(nowMs = 3_001))
    }

    @Test
    fun `intendedValue reports the fresh intent`() {
        hold.intend(false, nowMs = 500)
        assertEquals(false, hold.intendedValue(nowMs = 600))
    }
}
