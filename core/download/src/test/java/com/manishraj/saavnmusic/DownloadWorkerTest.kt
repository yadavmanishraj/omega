package com.manishraj.saavnmusic

import com.manishraj.saavnmusic.download.DownloadWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Pure-JVM coverage for [DownloadWorker]'s reachable logic.
 *
 * What is NOT covered here, and why: the transfer loop
 * (`copyWithProgress` — the >=5% / >=500 ms progress throttling and
 * the percent capping) and the row lifecycle in `doWork` are private
 * instance behavior bound to a WorkManager worker Context, an
 * OkHttp call and a real file system. Exercising them from a JVM
 * unit test would need a production seam (extracting the copy loop
 * into a standalone class) or an instrumented/Robolectric test —
 * neither exists today. The Room-side effects of the lifecycle ARE
 * covered in :core:data (MusicRepositoryLibraryTest).
 *
 * What remains reachable and contractual is the unique-work naming:
 * Library cancels and retries downloads by this name, so it must
 * stay stable and unique per song.
 */
class DownloadWorkerTest {
    @Test
    fun uniqueWorkNameIsDownloadPrefixPlusSongId() {
        assertEquals("download-abc123", DownloadWorker.uniqueWorkName("abc123"))
        assertEquals("download-", DownloadWorker.uniqueWorkName(""))
    }

    @Test
    fun uniqueWorkNamesDifferPerSong() {
        assertNotEquals(
            DownloadWorker.uniqueWorkName("song-a"),
            DownloadWorker.uniqueWorkName("song-b"),
        )
    }
}
