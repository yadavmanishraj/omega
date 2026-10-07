package com.manishraj.saavnmusic

import android.content.ContextWrapper
import com.manishraj.saavnmusic.data.remote.JioSaavnClient
import com.manishraj.saavnmusic.data.repository.MusicRepository
import com.manishraj.saavnmusic.data.settings.SettingsRepository
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.feature.library.LibraryViewModel
import com.manishraj.saavnmusic.feature.library.SortMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [LibraryViewModel] state behavior: its StateFlows are `stateIn`
 * views of the repository's library flows, and its actions delegate
 * straight to the repository. Tested against the real
 * [MusicRepository] backed by [FakeLibraryDao], with the Main
 * dispatcher replaced by a test dispatcher.
 *
 * The ViewModel's Context is only used to reach WorkManager inside
 * deleteDownload/retryDownload, which these tests never call — so a
 * context that is never invoked is enough.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private class UnusedContext : ContextWrapper(null)

    private val dispatcher = StandardTestDispatcher()
    private val dao = FakeLibraryDao()
    private val json = Json { ignoreUnknownKeys = true }
    private val repo =
        MusicRepository(
            client = JioSaavnClient(OkHttpClient(), json),
            dao = dao,
            settingsRepository = SettingsRepository(FakeDataStore()),
            json = json,
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): LibraryViewModel = LibraryViewModel(repo, UnusedContext())

    private fun song(id: String): Song =
        Song(
            id = id,
            name = "Song $id",
            artist = "Artist $id",
            album = "Album $id",
            imageUrl = "https://img.example/$id.jpg",
            durationSec = 200L,
            streamUrl = "https://stream.example/$id",
        )

    /** WhileSubscribed state flows only run while collected; keep a collector alive per flow. */
    private fun LibraryViewModel.keepStateFlowsAlive(scope: CoroutineScope) {
        scope.launch { favorites.collect {} }
        scope.launch { downloads.collect {} }
        scope.launch { history.collect {} }
        scope.launch { playlists.collect {} }
    }

    @Test
    fun stateStartsEmptyWithNewestSort() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.keepStateFlowsAlive(backgroundScope)
            runCurrent()

            assertEquals(SortMode.NEWEST, vm.sortMode.value)
            assertTrue(vm.favorites.value.isEmpty())
            assertTrue(vm.downloads.value.isEmpty())
            assertTrue(vm.history.value.isEmpty())
            assertTrue(vm.playlists.value.isEmpty())
        }

    @Test
    fun favoritesMirrorRepositoryAndUnfavoriteDelegates() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.keepStateFlowsAlive(backgroundScope)
            runCurrent()

            val s = song("fav")
            repo.addFavorite(s)
            runCurrent()
            assertEquals(listOf("fav"), vm.favorites.value.map { it.id })

            vm.unfavorite(s)
            advanceUntilIdle()
            assertTrue(vm.favorites.value.isEmpty())
        }

    @Test
    fun downloadsMirrorRepository() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.keepStateFlowsAlive(backgroundScope)
            runCurrent()

            repo.markDownloadStarted(song("dl"), "/music/track.m4a", "320kbps")
            runCurrent()
            assertEquals(listOf("dl"), vm.downloads.value.map { it.songId })
            assertEquals("DOWNLOADING", vm.downloads.value[0].status)
        }

    @Test
    fun historyAndPlaylistsMirrorRepository() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.keepStateFlowsAlive(backgroundScope)
            runCurrent()

            repo.recordPlay(song("h"))
            repo.createPlaylist("Chill")
            runCurrent()
            assertEquals(listOf("h"), vm.history.value.map { it.id })
            assertEquals(listOf("Chill"), vm.playlists.value.map { it.name })
        }

    @Test
    fun createPlaylistAndAddWiresThroughRepository() =
        runTest(dispatcher) {
            val vm = viewModel()
            val s = song("p1")
            vm.createPlaylistAndAdd("Mix", s)
            advanceUntilIdle()

            val playlists = repo.localPlaylists.first()
            assertEquals(1, playlists.size)
            assertEquals("Mix", playlists[0].name)
            assertEquals(1, playlists[0].songCount)
            val songs = repo.playlistSongs(playlists[0].id).first()
            assertEquals(listOf("p1"), songs.map { it.id })
        }

    @Test
    fun sortModeCanBeChanged() {
        val vm = viewModel()
        vm.sortMode.value = SortMode.A_Z
        assertEquals(SortMode.A_Z, vm.sortMode.value)
        vm.sortMode.value = SortMode.OLDEST
        assertEquals(SortMode.OLDEST, vm.sortMode.value)
    }
}
