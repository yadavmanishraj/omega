package com.manishraj.saavnmusic.playback

import com.manishraj.saavnmusic.domain.Song

/** The queue mutation a remove request resolves to. */
data class RemoveFromQueuePlan(
    /** The queue after the removal. */
    val queue: List<Song>,
    /**
     * Index of the current item in [queue] after the removal;
     * -1 when nothing is current (empty queue, or nothing was
     * current to begin with).
     */
    val currentIndex: Int,
    /** True when the removed item was the current one. */
    val removedCurrent: Boolean,
)

/**
 * Queue math for removing the item at [index] — pure, so it is
 * unit-tested without a player (see RemoveFromQueuePlanTest).
 *
 * Returns null when [index] does not address an item (the caller
 * treats that as a no-op). Current-item semantics mirror what the
 * engine does when its current item is removed: the item that
 * slides into the removed slot becomes current, or the new last
 * item when the removed one was last; removing the only item
 * leaves nothing current. Removing an item BEFORE the current one
 * shifts the current index down by one; removing one after it
 * leaves the current index untouched. A [currentIndex] outside
 * the queue (e.g. -1, nothing current) is treated as "no current"
 * and simply stays -1.
 */
fun planRemoveFromQueue(
    queue: List<Song>,
    index: Int,
    currentIndex: Int,
): RemoveFromQueuePlan? {
    if (index !in queue.indices) return null
    val cur = if (currentIndex in queue.indices) currentIndex else -1
    val newQueue = queue.toMutableList().apply { removeAt(index) }
    val removedCurrent = index == cur
    val newCurrentIndex =
        when {
            newQueue.isEmpty() -> -1
            index < cur -> cur - 1
            removedCurrent -> minOf(index, newQueue.lastIndex)
            else -> cur
        }
    return RemoveFromQueuePlan(
        queue = newQueue,
        currentIndex = newCurrentIndex,
        removedCurrent = removedCurrent,
    )
}
