package com.manishraj.saavnmusic.playback

import com.manishraj.saavnmusic.domain.Song
import org.junit.Assert.assertEquals
import org.junit.Test

/** Queue math for "Play next" ([planInsertNext]) — pure, no player involved. */
class InsertNextPlanTest {
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

    private val queue = listOf(song("a"), song("b"), song("c"))

    @Test
    fun `inserts immediately after the current item`() {
        val plan = planInsertNext(queue, currentIndex = 0, song = song("x"))
        assertEquals(InsertNextResult.INSERTED_NEXT, plan.result)
        assertEquals(1, plan.index)
        assertEquals(listOf("a", "x", "b", "c"), plan.queue.map { it.id })
    }

    @Test
    fun `inserting after the last item is still play-next, at the end`() {
        val plan = planInsertNext(queue, currentIndex = 2, song = song("x"))
        assertEquals(InsertNextResult.INSERTED_NEXT, plan.result)
        assertEquals(3, plan.index)
        assertEquals(listOf("a", "b", "c", "x"), plan.queue.map { it.id })
    }

    @Test
    fun `empty queue appends and reports appended`() {
        val plan = planInsertNext(emptyList(), currentIndex = -1, song = song("x"))
        assertEquals(InsertNextResult.APPENDED, plan.result)
        assertEquals(0, plan.index)
        assertEquals(listOf("x"), plan.queue.map { it.id })
    }

    @Test
    fun `no current item appends instead of playing immediately`() {
        val plan = planInsertNext(queue, currentIndex = -1, song = song("x"))
        assertEquals(InsertNextResult.APPENDED, plan.result)
        assertEquals(3, plan.index)
        assertEquals(listOf("a", "b", "c", "x"), plan.queue.map { it.id })
    }

    @Test
    fun `out-of-range current index appends`() {
        val plan = planInsertNext(queue, currentIndex = 99, song = song("x"))
        assertEquals(InsertNextResult.APPENDED, plan.result)
        assertEquals(listOf("a", "b", "c", "x"), plan.queue.map { it.id })
    }

    @Test
    fun `the original queue is not mutated`() {
        planInsertNext(queue, currentIndex = 1, song = song("x"))
        assertEquals(listOf("a", "b", "c"), queue.map { it.id })
    }
}
