package com.manishraj.saavnmusic

import com.manishraj.saavnmusic.data.remote.JioSaavnClient
import com.manishraj.saavnmusic.data.repository.MusicRepository
import com.manishraj.saavnmusic.data.repository.toSong
import com.manishraj.saavnmusic.data.settings.SettingsRepository
import com.manishraj.saavnmusic.domain.DownloadInfo
import com.manishraj.saavnmusic.domain.Song
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local-library behavior of [MusicRepository] against an in-memory
 * [FakeLibraryDao]. The upstream client and settings repository are
 * real instances wired to fakes, but no remote function is ever
 * called: everything under test here is the Room-facing half of the
 * repository (favorites, history, playlists, downloads, searches).
 */
class MusicRepositoryLibraryTest {
    private val dao = FakeLibraryDao()
    private val json = Json { ignoreUnknownKeys = true }
    private val repo =
        MusicRepository(
            client = JioSaavnClient(OkHttpClient(), json),
            dao = dao,
            settingsRepository = SettingsRepository(FakeDataStore()),
            json = json,
        )

    private fun song(
        id: String,
        name: String = "Song $id",
    ): Song =
        Song(
            id = id,
            name = name,
            artist = "Artist $id",
            album = "Album $id",
            imageUrl = "https://img.example/$id.jpg",
            durationSec = 200L,
            streamUrl = "https://stream.example/$id",
        )

    // ---- Playlists ----

    @Test
    fun playlistAddsAppendAtNextPosition() =
        runTest {
            val playlistId = repo.createPlaylist("Mix")
            repo.addToPlaylist(playlistId, song("a"))
            repo.addToPlaylist(playlistId, song("b"))
            repo.addToPlaylist(playlistId, song("c"))

            // Regression test for the wave-2 fix: adds used to all land
            // at position 0; they must append in insertion order.
            val rows = dao.playlistSongs(playlistId).first()
            assertEquals(listOf("a", "b", "c"), rows.map { it.songId })
            assertEquals(listOf(0, 1, 2), rows.map { it.position })

            val songs = repo.playlistSongs(playlistId).first()
            assertEquals(listOf("a", "b", "c"), songs.map { it.id })
        }

    @Test
    fun playlistPositionsAreCountedPerPlaylist() =
        runTest {
            val firstId = repo.createPlaylist("One")
            val secondId = repo.createPlaylist("Two")
            repo.addToPlaylist(firstId, song("a"))
            repo.addToPlaylist(firstId, song("b"))
            repo.addToPlaylist(secondId, song("c"))

            val secondRows = dao.playlistSongs(secondId).first()
            assertEquals(listOf("c"), secondRows.map { it.songId })
            assertEquals(listOf(0), secondRows.map { it.position })
        }

    @Test
    fun reAddingPlaylistSongReplacesSnapshotWithoutDuplicating() =
        runTest {
            val playlistId = repo.createPlaylist("Mix")
            repo.addToPlaylist(playlistId, song("a", name = "Old Name"))
            repo.addToPlaylist(playlistId, song("b"))
            repo.addToPlaylist(playlistId, song("a", name = "New Name"))

            val rows = dao.playlistSongs(playlistId).first()
            assertEquals(2, rows.size)
            assertEquals("New Name", rows.first { it.songId == "a" }.name)
            // The replacement carries the append position, so the
            // re-added song moves behind the songs added before it.
            assertEquals(listOf("b", "a"), rows.map { it.songId })
        }

    @Test
    fun localPlaylistsFlowReflectsCreatesCountsAndDelete() =
        runTest {
            val playlistId = repo.createPlaylist("Road Trip")
            var playlists = repo.localPlaylists.first()
            assertEquals(1, playlists.size)
            assertEquals(playlistId, playlists[0].id)
            assertEquals("Road Trip", playlists[0].name)
            assertEquals(0, playlists[0].songCount)

            repo.addToPlaylist(playlistId, song("a"))
            repo.addToPlaylist(playlistId, song("b"))
            playlists = repo.localPlaylists.first()
            assertEquals(2, playlists[0].songCount)

            repo.deletePlaylist(playlistId)
            assertTrue(repo.localPlaylists.first().isEmpty())
        }

    @Test
    fun removeFromPlaylistDeletesOnlyThatMembership() =
        runTest {
            val firstId = repo.createPlaylist("One")
            val secondId = repo.createPlaylist("Two")
            repo.addToPlaylist(firstId, song("a"))
            repo.addToPlaylist(firstId, song("b"))
            repo.addToPlaylist(firstId, song("c"))
            repo.addToPlaylist(secondId, song("b"))

            val removed = repo.removeFromPlaylist(firstId, song("b"))
            assertEquals(1, removed?.position)

            // The other rows keep their positions; the same song in
            // the OTHER playlist is a different membership and stays.
            val firstRows = dao.playlistSongs(firstId).first()
            assertEquals(listOf("a", "c"), firstRows.map { it.songId })
            assertEquals(listOf(0, 2), firstRows.map { it.position })
            assertEquals(listOf("b"), dao.playlistSongs(secondId).first().map { it.songId })
            assertEquals(
                2,
                repo.localPlaylists
                    .first()
                    .first { it.id == firstId }
                    .songCount,
            )
        }

    @Test
    fun restoreToPlaylistReInsertsAtOriginalPosition() =
        runTest {
            val playlistId = repo.createPlaylist("Mix")
            repo.addToPlaylist(playlistId, song("a"))
            repo.addToPlaylist(playlistId, song("b"))
            repo.addToPlaylist(playlistId, song("c"))

            val removed = repo.removeFromPlaylist(playlistId, song("b"))
            assertEquals("b", removed?.song?.id)
            repo.restoreToPlaylist(removed!!)

            // Full pre-removal state: same songs, same order, same
            // stored positions (Undo must not append at the end).
            val rows = dao.playlistSongs(playlistId).first()
            assertEquals(listOf("a", "b", "c"), rows.map { it.songId })
            assertEquals(listOf(0, 1, 2), rows.map { it.position })
            assertEquals("Song b", rows[1].name)
        }

    @Test
    fun removeFromPlaylistForMissingSongReturnsNull() =
        runTest {
            val playlistId = repo.createPlaylist("Mix")
            repo.addToPlaylist(playlistId, song("a"))

            assertNull(repo.removeFromPlaylist(playlistId, song("zzz")))
            assertEquals(listOf("a"), repo.playlistSongs(playlistId).first().map { it.id })
        }

    // ---- Favorites ----

    @Test
    fun toggleFavoriteAddsThenRemoves() =
        runTest {
            val s = song("fav1")
            assertFalse(repo.isFavorite("fav1").first())

            repo.toggleFavorite(s, fav = false)
            assertTrue(repo.isFavorite("fav1").first())
            val favorites = repo.favorites.first()
            assertEquals(1, favorites.size)
            assertEquals(s.id, favorites[0].id)
            assertEquals(s.name, favorites[0].name)
            assertEquals(s.artist, favorites[0].artist)
            assertEquals(s.album, favorites[0].album)
            assertEquals(s.durationSec, favorites[0].durationSec)
            assertEquals(s.streamUrl, favorites[0].streamUrl)

            repo.toggleFavorite(s, fav = true)
            assertFalse(repo.isFavorite("fav1").first())
            assertTrue(repo.favorites.first().isEmpty())
        }

    @Test
    fun addFavoriteRestoresSnapshot() =
        runTest {
            val s = song("fav2")
            repo.toggleFavorite(s, fav = false)
            repo.toggleFavorite(s, fav = true)
            assertTrue(repo.favorites.first().isEmpty())

            // The Library Undo path re-adds the removed snapshot.
            repo.addFavorite(s)
            assertEquals(listOf("fav2"), repo.favorites.first().map { it.id })
        }

    // ---- History ----

    @Test
    fun recordPlayWritesHistoryNewestFirst() =
        runTest {
            repo.recordPlay(song("h1"))
            repo.recordPlay(song("h2"))

            val history = repo.history.first()
            assertEquals(listOf("h2", "h1"), history.map { it.id })
            // History rows carry no album/duration snapshot.
            assertNull(history[0].album)
            assertNull(history[0].durationSec)
            assertEquals("https://stream.example/h2", history[0].streamUrl)

            // Replaying a song refreshes its row instead of duplicating it.
            repo.recordPlay(song("h1"))
            val refreshed = repo.history.first()
            assertEquals(listOf("h1", "h2"), refreshed.map { it.id })
        }

    @Test
    fun clearHistoryEmptiesTheFlow() =
        runTest {
            repo.recordPlay(song("h1"))
            repo.clearHistory()
            assertTrue(repo.history.first().isEmpty())
        }

    // ---- Recent searches ----

    @Test
    fun recentSearchesAreTrimmedAndBlankIsIgnored() =
        runTest {
            repo.addRecentSearch("  arijit singh  ")
            repo.addRecentSearch("   ")
            repo.addRecentSearch("")
            assertEquals(listOf("arijit singh"), repo.recentSearches.first())

            repo.removeRecentSearch("arijit singh")
            assertTrue(repo.recentSearches.first().isEmpty())

            repo.addRecentSearch("one")
            repo.clearRecentSearches()
            assertTrue(repo.recentSearches.first().isEmpty())
        }

    // ---- Downloads ----

    @Test
    fun downloadLifecycleStatesArePersisted() =
        runTest {
            val s = song("d1")
            val path = "/music/SaavnMusic/Artist d1 - Song d1.m4a"

            repo.markDownloadStarted(s, path, "320kbps")
            var info = repo.downloads.first().single()
            assertEquals("DOWNLOADING", info.status)
            assertEquals(0, info.progress)
            assertEquals(0L, info.sizeBytes)
            assertEquals(path, info.filePath)
            assertEquals("320kbps", info.quality)

            repo.updateDownloadProgress("d1", 40, 4096L)
            info = repo.downloads.first().single()
            assertEquals("DOWNLOADING", info.status)
            assertEquals(40, info.progress)
            assertEquals(4096L, info.sizeBytes)

            repo.registerDownload(s, path, "320kbps", 10_000L)
            info = repo.downloads.first().single()
            assertEquals("COMPLETED", info.status)
            assertEquals(100, info.progress)
            assertEquals(10_000L, info.sizeBytes)
            assertNull(info.errorMessage)
        }

    @Test
    fun failedDownloadPersistsItsReason() =
        runTest {
            val s = song("d2")
            repo.markDownloadStarted(s, "/music/track.m4a", "160kbps")
            repo.markDownloadFailed("d2", "HTTP 404")

            val info = repo.downloads.first().single()
            assertEquals("FAILED", info.status)
            assertEquals("HTTP 404", info.errorMessage)
        }

    @Test
    fun removeAndDeleteDownloadDropTheRow() =
        runTest {
            val s = song("d3")
            repo.markDownloadStarted(s, "/nonexistent-dir/track.m4a", "320kbps")
            repo.removeDownloadRow("d3")
            assertTrue(repo.downloads.first().isEmpty())

            repo.registerDownload(s, "/nonexistent-dir/track.m4a", "320kbps", 123L)
            assertEquals(1, repo.downloads.first().size)
            // deleteDownload also tries to remove the file; a path that
            // does not exist must not throw.
            repo.deleteDownload("d3")
            assertTrue(repo.downloads.first().isEmpty())
        }

    @Test
    fun registerDownloadInfoRoundTripsRowState() =
        runTest {
            val info =
                DownloadInfo(
                    songId = "d4",
                    name = "Song d4",
                    artist = "Artist d4",
                    album = "Album d4",
                    imageUrl = "https://img.example/d4.jpg",
                    filePath = "/music/track.m4a",
                    quality = "96kbps",
                    sizeBytes = 777L,
                    status = "FAILED",
                    progress = 37,
                    errorMessage = "boom",
                )
            repo.registerDownload(info)

            val stored = repo.downloads.first().single()
            assertEquals(info.songId, stored.songId)
            assertEquals(info.filePath, stored.filePath)
            assertEquals(info.quality, stored.quality)
            assertEquals(info.sizeBytes, stored.sizeBytes)
            assertEquals(info.status, stored.status)
            assertEquals(info.progress, stored.progress)
            // registerDownload(DownloadInfo) carries errorMessage into the
            // entity, so an undo-restored FAILED row keeps its reason.
            assertEquals(info.errorMessage, stored.errorMessage)
        }

    @Test
    fun completedDownloadMapsToPlayableSong() =
        runTest {
            val info =
                DownloadInfo(
                    songId = "d5",
                    name = "Song d5",
                    artist = "Artist d5",
                    album = "Album d5",
                    imageUrl = "https://img.example/d5.jpg",
                    filePath = "/music/local-file.m4a",
                    quality = "320kbps",
                    sizeBytes = 10L,
                    status = "COMPLETED",
                    progress = 100,
                )
            val playable = info.toSong()
            assertEquals("d5", playable.id)
            // The local file path rides in streamUrl for offline playback.
            assertEquals("/music/local-file.m4a", playable.streamUrl)
        }
}
