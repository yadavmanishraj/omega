package com.manishraj.saavnmusic.playback

import com.manishraj.saavnmusic.domain.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Queue math for removing a queue item ([planRemoveFromQueue]) — pure, no player involved. */
class RemoveFromQueuePlanTest {
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
    fun `removing an item after the current one leaves the current index untouched`() {
        val plan = planRemoveFromQueue(queue, index = 2, currentIndex = 0)!!
        assertEquals(listOf("a", "b"), plan.queue.map { it.id })
        assertEquals(0, plan.currentIndex)
        assertFalse(plan.removedCurrent)
    }

    @Test
    fun `removing an item before the current one shifts the current index down`() {
        val plan = planRemoveFromQueue(queue, index = 0, currentIndex = 2)!!
        assertEquals(listOf("b", "c"), plan.queue.map { it.id })
        assertEquals(1, plan.currentIndex)
        assertFalse(plan.removedCurrent)
    }

    @Test
    fun `removing the current item lands on the item sliding into its slot`() {
        val plan = planRemoveFromQueue(queue, index = 1, currentIndex = 1)!!
        assertEquals(listOf("a", "c"), plan.queue.map { it.id })
        assertEquals(1, plan.currentIndex)
        assertTrue(plan.removedCurrent)
    }

    @Test
    fun `removing the current item when it is last lands on the new last item`() {
        val plan = planRemoveFromQueue(queue, index = 2, currentIndex = 2)!!
        assertEquals(listOf("a", "b"), plan.queue.map { it.id })
        assertEquals(1, plan.currentIndex)
        assertTrue(plan.removedCurrent)
    }

    @Test
    fun `removing the only item leaves an empty queue and nothing current`() {
        val plan = planRemoveFromQueue(listOf(song("a")), index = 0, currentIndex = 0)!!
        assertEquals(emptyList<Song>(), plan.queue)
        assertEquals(-1, plan.currentIndex)
        assertTrue(plan.removedCurrent)
    }

    @Test
    fun `removing with nothing current keeps nothing current`() {
        val plan = planRemoveFromQueue(queue, index = 1, currentIndex = -1)!!
        assertEquals(listOf("a", "c"), plan.queue.map { it.id })
        assertEquals(-1, plan.currentIndex)
        assertFalse(plan.removedCurrent)
    }

    @Test
    fun `out-of-range indices are a no-op`() {
        assertNull(planRemoveFromQueue(queue, index = 3, currentIndex = 0))
        assertNull(planRemoveFromQueue(queue, index = -1, currentIndex = 0))
        assertNull(planRemoveFromQueue(emptyList(), index = 0, currentIndex = -1))
    }

    @Test
    fun `the original queue is not mutated`() {
        planRemoveFromQueue(queue, index = 1, currentIndex = 1)
        assertEquals(listOf("a", "b", "c"), queue.map { it.id })
    }
}
