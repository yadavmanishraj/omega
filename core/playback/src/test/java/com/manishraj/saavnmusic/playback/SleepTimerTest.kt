package com.manishraj.saavnmusic.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sleep-timer truth (F-05): the preset set the menu offers, the
 * direct-selection semantics it arms with, and the deadline math
 * the published countdown is derived from.
 */
class SleepTimerTest {
    @Test
    fun `preset set offered by the menu is 15-30-60`() {
        assertEquals(listOf(15, 30, 60), SLEEP_TIMER_PRESETS)
    }

    @Test
    fun `direct selection arms exactly the chosen preset from any state`() {
        val tracker = SleepTimerTracker()
        // From off, choosing 30 arms 30 — not a step toward it.
        tracker.arm(30, nowMs = 0)
        assertEquals(30, tracker.armedMinutes)
        // While 30 is armed, choosing 15 lands on 15: selection is
        // absolute, never "next in a cycle".
        tracker.arm(15, nowMs = 0)
        assertEquals(15, tracker.armedMinutes)
        tracker.arm(60, nowMs = 0)
        assertEquals(60, tracker.armedMinutes)
        // Every preset the menu offers arms exactly.
        SLEEP_TIMER_PRESETS.forEach { preset ->
            tracker.arm(preset, nowMs = 0)
            assertEquals(preset, tracker.armedMinutes)
        }
    }

    @Test
    fun `armed tracker counts down from the deadline`() {
        val tracker = SleepTimerTracker()
        tracker.arm(15, nowMs = 1_000)
        assertTrue(tracker.isArmed)
        assertEquals(15, tracker.armedMinutes)
        assertEquals(15 * 60_000L, tracker.remainingMs(1_000))
        assertEquals(10 * 60_000L, tracker.remainingMs(1_000 + 5 * 60_000L))
        assertFalse(tracker.isExpired(1_000 + 14 * 60_000L))
    }

    @Test
    fun `expiry floors remaining at zero`() {
        val tracker = SleepTimerTracker()
        tracker.arm(15, nowMs = 0)
        assertEquals(0, tracker.remainingMs(16 * 60_000L))
        assertTrue(tracker.isExpired(15 * 60_000L))
    }

    @Test
    fun `cancel disarms completely`() {
        val tracker = SleepTimerTracker()
        tracker.arm(30, nowMs = 0)
        tracker.cancel()
        assertFalse(tracker.isArmed)
        assertEquals(0, tracker.armedMinutes)
        assertEquals(0, tracker.remainingMs(0))
        assertFalse(tracker.isExpired(Long.MAX_VALUE))
    }

    @Test
    fun `re-arm replaces the deadline`() {
        val tracker = SleepTimerTracker()
        tracker.arm(15, nowMs = 0)
        tracker.arm(60, nowMs = 10 * 60_000L)
        assertEquals(60, tracker.armedMinutes)
        assertEquals(60 * 60_000L, tracker.remainingMs(10 * 60_000L))
    }
}
