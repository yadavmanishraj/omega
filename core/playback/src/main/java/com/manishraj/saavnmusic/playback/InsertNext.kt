package com.manishraj.saavnmusic.playback

import com.manishraj.saavnmusic.domain.Song

/** Outcome of a "Play next" request ([PlayerController.insertNext]). */
enum class InsertNextResult {
    /** The song now sits immediately after the currently playing item. */
    INSERTED_NEXT,

    /** No active queue to insert into; the song was appended instead. */
    APPENDED,
}

/** The queue mutation an insert-next request resolves to. */
data class InsertNextPlan(
    /** The queue after the insertion. */
    val queue: List<Song>,
    /** Timeline index the inserted song occupies in [queue]. */
    val index: Int,
    val result: InsertNextResult,
)

/**
 * Queue math for "Play next" — pure, so it is unit-tested without a
 * player (see InsertNextPlanTest).
 *
 * With an active queue the song goes immediately after
 * [currentIndex]. With NO active queue (empty queue, or
 * [currentIndex] outside it because nothing is current) the song is
 * APPENDED and reported as [InsertNextResult.APPENDED]: a menu action
 * must never silently turn into "stop what is playing and play this
 * instead", which is what play-at-0 would do.
 */
fun planInsertNext(
    queue: List<Song>,
    currentIndex: Int,
    song: Song,
): InsertNextPlan {
    if (queue.isEmpty() || currentIndex !in queue.indices) {
        return InsertNextPlan(
            queue = queue + song,
            index = queue.size,
            result = InsertNextResult.APPENDED,
        )
    }
    val at = currentIndex + 1
    return InsertNextPlan(
        queue = queue.toMutableList().apply { add(at, song) },
        index = at,
        result = InsertNextResult.INSERTED_NEXT,
    )
}
