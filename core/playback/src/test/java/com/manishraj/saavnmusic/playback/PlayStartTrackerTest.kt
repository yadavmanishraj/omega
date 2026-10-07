package com.manishraj.saavnmusic.playback

import com.manishraj.saavnmusic.domain.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * History recording decisions (BUG-5): exactly one record per play,
 * from whichever path started the track.
 */
class PlayStartTrackerTest {
    private fun song(id: String) =
        Song(
            id = id,
            name = "Song $id",
            artist = "Artist",
            album = null,
            imageUrl = null,
            durationSec = 200,
            streamUrl = "https://example.com/$id.mp3",
        )

    private val a = song("a")
    private val b = song("b")
    private val c = song("c")
    private val tracker = PlayStartTracker()

    @Test
    fun `explicit play records and its observer echo is suppressed`() {
        assertEquals(a, tracker.onExplicitPlay(a))
        assertNull(tracker.onObservedCurrent(a))
        assertNull(tracker.onObservedCurrent(a))
    }

    @Test
    fun `auto-advance to a different song records it`() {
        tracker.onExplicitPlay(a)
        tracker.onObservedCurrent(a)
        assertEquals(b, tracker.onObservedCurrent(b))
        assertNull(tracker.onObservedCurrent(b))
    }

    @Test
    fun `next and prev transitions each record`() {
        tracker.onExplicitPlay(a)
        tracker.onObservedCurrent(a)
        assertEquals(b, tracker.onObservedCurrent(b))
        assertEquals(c, tracker.onObservedCurrent(c))
        // Back to b is a fresh play of b.
        assertEquals(b, tracker.onObservedCurrent(b))
    }

    @Test
    fun `first observed item never records (restored session)`() {
        assertNull(tracker.onObservedCurrent(a))
        assertNull(tracker.onObservedCurrent(a))
        // But a genuine transition after the restore does record.
        assertEquals(b, tracker.onObservedCurrent(b))
    }

    @Test
    fun `stale observation of the outgoing song after an explicit play records nothing`() {
        // A is playing and fully observed; the user taps B in the
        // same list. An events batch still reporting A must not
        // re-record A (it would jump above B in History ordering).
        tracker.onExplicitPlay(a)
        tracker.onObservedCurrent(a)
        tracker.onExplicitPlay(b)
        assertNull(tracker.onObservedCurrent(a))
        // The switch completing to B is the suppressed echo.
        assertNull(tracker.onObservedCurrent(b))
        // And advancing past B records normally.
        assertEquals(c, tracker.onObservedCurrent(c))
    }

    @Test
    fun `repeat-one loop of the same song records only the explicit play`() {
        tracker.onExplicitPlay(a)
        // The loop boundary: same mediaId observed again and again.
        repeat(5) { assertNull(tracker.onObservedCurrent(a)) }
    }

    @Test
    fun `null observations record nothing and do not disarm transitions`() {
        tracker.onExplicitPlay(a)
        tracker.onObservedCurrent(a)
        assertNull(tracker.onObservedCurrent(null))
        assertEquals(b, tracker.onObservedCurrent(b))
    }
}
