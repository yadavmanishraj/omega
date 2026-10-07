package com.manishraj.saavnmusic

import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.UiState
import com.manishraj.saavnmusic.feature.home.dedupeTrending
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Polish item 21 / R-P3: Home's Trending must not repeat a song that
 * Jump back in already shows. The rule lives in the pure
 * [dedupeTrending] at the HomeViewModel seam; these tests pin the
 * rule without spinning up the ViewModel or its repository.
 */
class TrendingDedupeTest {
    private fun song(id: String) = Song(id, "Song $id", "Artist", null, null, null, null)

    @Test
    fun overlapIsRemovedAndOrderPreserved() {
        val trending = listOf(song("a"), song("b"), song("c"), song("d"))
        val history = listOf(song("c"), song("a"))

        val result = dedupeTrending(UiState.Success(trending), history)

        assertEquals(UiState.Success(listOf(song("b"), song("d"))), result)
    }

    @Test
    fun emptyHistoryLeavesTrendingUntouched() {
        val state = UiState.Success(listOf(song("a"), song("b")))

        val result = dedupeTrending(state, emptyList())

        assertSame(state, result)
    }

    @Test
    fun fullOverlapEmptiesTheSection() {
        val trending = listOf(song("a"), song("b"))
        val history = listOf(song("b"), song("a"), song("z"))

        val result = dedupeTrending(UiState.Success(trending), history)

        // Success(emptyList()): HomeScreen omits an empty Success
        // section (header included), same as a natively empty one.
        assertEquals(UiState.Success(emptyList<Song>()), result)
    }

    @Test
    fun duplicatesInsideTrendingAreNotTouched() {
        // The rule arbitrates BETWEEN sections only: a song trending
        // twice (upstream's quirk) stays twice unless it was played.
        val trending = listOf(song("a"), song("a"), song("b"))
        val history = listOf(song("b"))

        val result = dedupeTrending(UiState.Success(trending), history)

        assertEquals(UiState.Success(listOf(song("a"), song("a"))), result)
    }

    @Test
    fun loadingAndErrorPassThrough() {
        val history = listOf(song("a"))

        assertSame(UiState.Loading, dedupeTrending(UiState.Loading, history))
        val error = UiState.Error("nope")
        assertSame(error, dedupeTrending(error, history))
    }
}
