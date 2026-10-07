package com.manishraj.saavnmusic.playback

import com.manishraj.saavnmusic.domain.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Session persistence (F-16): the codec round-trip and the restore
 * decision — a persisted queue comes back paused, with stale values
 * clamped instead of crashing or pointing past the end.
 */
class PlaybackSessionTest {
    private fun song(id: String) =
        Song(
            id = id,
            name = "Song $id",
            artist = "Artist $id",
            album = "Album",
            imageUrl = "https://example.com/$id.jpg",
            durationSec = 200,
            streamUrl = "https://example.com/$id.mp3",
            downloadUrls = listOf("96kbps" to "https://example.com/$id-96.mp3", "320kbps" to "https://example.com/$id-320.mp3"),
            hasLyrics = true,
        )

    private val queue = listOf(song("a"), song("b"), song("c"))

    @Test
    fun `round trip preserves queue index and position`() {
        val session = PlaybackSessionCodec.snapshot(queue, currentIndex = 1, positionMs = 42_000)!!
        val restored = PlaybackSessionCodec.decode(PlaybackSessionCodec.encode(session))!!.toRestoredPlayback()!!

        assertEquals(queue, restored.queue)
        assertEquals(1, restored.currentIndex)
        assertEquals(42_000, restored.positionMs)
    }

    @Test
    fun `empty queue snapshots to nothing`() {
        assertNull(PlaybackSessionCodec.snapshot(emptyList(), currentIndex = 0, positionMs = 0))
    }

    @Test
    fun `empty persisted queue restores to nothing`() {
        val session = PlaybackSession(songs = emptyList(), currentIndex = 0, positionMs = 0)
        assertNull(session.toRestoredPlayback())
    }

    @Test
    fun `stale index clamps to the last song`() {
        val session =
            PlaybackSessionCodec
                .snapshot(queue, currentIndex = 2, positionMs = 0)!!
                .copy(currentIndex = 99)
        assertEquals(2, session.toRestoredPlayback()!!.currentIndex)
    }

    @Test
    fun `negative position floors at zero`() {
        val session =
            PlaybackSessionCodec
                .snapshot(queue, currentIndex = 0, positionMs = 5_000)!!
                .copy(positionMs = -1_000)
        assertEquals(0, session.toRestoredPlayback()!!.positionMs)
    }

    @Test
    fun `corrupt payloads decode to nothing`() {
        assertNull(PlaybackSessionCodec.decode(null))
        assertNull(PlaybackSessionCodec.decode(""))
        assertNull(PlaybackSessionCodec.decode("not json at all"))
        assertNull(PlaybackSessionCodec.decode("{\"songs\":42}"))
    }

    @Test
    fun `unknown fields in the payload are tolerated`() {
        val raw = """{"songs":[],"currentIndex":0,"positionMs":0,"futureField":true}"""
        val decoded = PlaybackSessionCodec.decode(raw)
        assertEquals(0, decoded!!.songs.size)
    }
}
