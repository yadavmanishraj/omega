package com.manishraj.saavnmusic

import android.content.ContextWrapper
import com.manishraj.saavnmusic.data.remote.JioSaavnClient
import com.manishraj.saavnmusic.data.repository.ConnectivityObserver
import com.manishraj.saavnmusic.data.repository.MusicRepository
import com.manishraj.saavnmusic.data.settings.SettingsRepository
import com.manishraj.saavnmusic.domain.UiState
import com.manishraj.saavnmusic.feature.search.SearchViewModel
import com.manishraj.saavnmusic.playback.PlayerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.Executors

/**
 * [SearchViewModel] search-state behavior against the real
 * [MusicRepository]: transport is faked at the OkHttp interceptor
 * (canned upstream payloads per `__call`, or an [IOException] when the
 * fake is "down"), storage by [FakeLibraryDao] / [FakeDataStore].
 *
 * runBlocking + a real single-thread Main dispatcher, not runTest:
 * the upstream client hops to Dispatchers.IO per call, which escapes
 * the virtual-time scheduler — so tests poll state with real delays
 * instead (the fake transport answers instantly, keeping them fast).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    /** ConnectivityObserver only needs getSystemService to not blow
     * up; a null service leaves it in the offline default, which the
     * search paths never consult. The VM's own context is only used
     * for WorkManager inside download(), never called here. */
    private class NullServiceContext : ContextWrapper(null) {
        override fun getSystemService(name: String): Any? = null
    }

    private class FakeUpstream : Interceptor {
        @Volatile var failing = false

        /** Artificial per-call latency, to hold a search in flight. */
        @Volatile var delayMs = 0L

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            if (delayMs > 0) Thread.sleep(delayMs)
            if (failing) throw IOException("Fake upstream is down")
            val body =
                when (request.url.queryParameter("__call")) {
                    "autocomplete.get" -> GLOBAL_JSON
                    "search.getResults" -> SONGS_JSON
                    "search.getAlbumResults" -> ALBUMS_JSON
                    "search.getArtistResults" -> ARTISTS_JSON
                    "search.getPlaylistResults" -> PLAYLISTS_JSON
                    else -> "{}"
                }
            return Response
                .Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }
    }

    private val mainDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val dao = FakeLibraryDao()
    private val upstream = FakeUpstream()
    private val json = Json { ignoreUnknownKeys = true }
    private val repo =
        MusicRepository(
            client =
                JioSaavnClient(
                    OkHttpClient.Builder().addInterceptor(upstream).build(),
                    json,
                ),
            dao = dao,
            settingsRepository = SettingsRepository(FakeDataStore()),
            json = json,
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        mainDispatcher.close()
    }

    private fun viewModel(): SearchViewModel {
        val context = NullServiceContext()
        return SearchViewModel(repo, PlayerController(context), ConnectivityObserver(context), context)
    }

    private suspend fun awaitCondition(
        description: String,
        condition: suspend () -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) fail("Timed out waiting for: $description")
            delay(25)
        }
    }

    /** `songs` STARTS as Success(emptyList()), so "is Success" is no
     * completion signal — a search has landed when its last write
     * (the playlists holder, set just before the Recents write) shows
     * the fake upstream's playlist. */
    private suspend fun SearchViewModel.awaitSearchLanded() =
        awaitCondition("search landed") { playlists.value.map { it.id } == listOf("pl1") }

    @Test
    fun submitSearchPopulatesEveryTabAndRecordsRecent() =
        runBlocking {
            val vm = viewModel()
            vm.search("arijit")
            vm.awaitSearchLanded()

            val songs = (vm.songs.value as UiState.Success).data
            assertEquals(listOf("s1"), songs.map { it.id })
            assertEquals(listOf("t1"), vm.topResults.value.map { it.id })
            assertEquals(listOf("al1"), vm.albums.value.map { it.id })
            assertEquals(listOf("art1"), vm.artists.value.map { it.id })
            assertEquals(listOf("pl1"), vm.playlists.value.map { it.id })
            assertEquals("arijit", vm.searchedQuery.value)
            awaitCondition("recent recorded") { repo.recentSearches.first() == listOf("arijit") }
        }

    @Test
    fun debouncedSearchSuccessAlsoRecordsRecent() =
        runBlocking {
            // Regression (exhaustive phone QA, 2026-10-07): a successful
            // search reached by EDITING — the debounce path — never
            // landed in Recent searches; only explicit submits did.
            val vm = viewModel()
            vm.onQueryChange("lofi")
            vm.awaitSearchLanded()
            assertEquals("lofi", vm.searchedQuery.value)
            awaitCondition("recent recorded") { repo.recentSearches.first() == listOf("lofi") }
        }

    @Test
    fun failedSearchReplacesPriorResultsWithErrorAndSkipsRecent() =
        runBlocking {
            // Regression (exhaustive phone QA MAJOR, 2026-10-07): a
            // failed search while prior results existed silently kept
            // the OLD results on the category tabs under the new
            // query. The failure must own every result holder.
            val vm = viewModel()
            vm.search("good")
            vm.awaitSearchLanded()
            awaitCondition("recent recorded") { repo.recentSearches.first() == listOf("good") }
            assertTrue(vm.albums.value.isNotEmpty())
            assertTrue(vm.topResults.value.isNotEmpty())

            upstream.failing = true
            vm.search("bad")
            awaitCondition("error state") { vm.songs.value is UiState.Error }

            assertEquals("bad", vm.searchedQuery.value)
            assertTrue(vm.topResults.value.isEmpty())
            assertTrue(vm.albums.value.isEmpty())
            assertTrue(vm.artists.value.isEmpty())
            assertTrue(vm.playlists.value.isEmpty())
            // The failed query earns no Recents slot; the earlier
            // success is untouched.
            assertEquals(listOf("good"), repo.recentSearches.first())
        }

    @Test
    fun retryAfterFailureRecoversAndRecordsRecent() =
        runBlocking {
            val vm = viewModel()
            upstream.failing = true
            vm.search("bad")
            awaitCondition("error state") { vm.songs.value is UiState.Error }

            upstream.failing = false
            // Exactly what the screen's Retry affordance does.
            vm.search(vm.searchedQuery.value)
            awaitCondition("recovered") { vm.songs.value is UiState.Success }
            val songs = (vm.songs.value as UiState.Success).data
            assertEquals(listOf("s1"), songs.map { it.id })
            awaitCondition("recent recorded") { repo.recentSearches.first() == listOf("bad") }
        }

    @Test
    fun clearingQueryAfterSearchRestoresIdleAndClearsResults() =
        runBlocking {
            // Regression (exhaustive emulator QA BUG-8 / C14,
            // 2026-10-07): tapping the field's × cleared the text
            // but the previous results stayed on screen forever —
            // `searchedQuery` was only ever set by a search, never
            // reset, and the screen derives idle from it.
            val vm = viewModel()
            vm.search("arijit")
            vm.awaitSearchLanded()
            awaitCondition("recent recorded") { repo.recentSearches.first() == listOf("arijit") }
            assertTrue(vm.albums.value.isNotEmpty())
            assertTrue(vm.topResults.value.isNotEmpty())

            vm.onQueryChange("")

            assertEquals("", vm.query.value)
            assertEquals("", vm.searchedQuery.value)
            assertTrue(vm.topResults.value.isEmpty())
            assertTrue(vm.albums.value.isEmpty())
            assertTrue(vm.artists.value.isEmpty())
            assertTrue(vm.playlists.value.isEmpty())
            val songs = vm.songs.value
            assertTrue(songs is UiState.Success && songs.data.isEmpty())
            // Clearing the box is not clearing history: the recent
            // the search earned stays for the idle view to show.
            assertEquals(listOf("arijit"), repo.recentSearches.first())
        }

    @Test
    fun whitespaceOnlyQueryAlsoReturnsToIdle() =
        runBlocking {
            // BUG-8's other face (C11): spaces in an emptied field
            // kept the previous query's state — here, its results.
            val vm = viewModel()
            vm.search("arijit")
            vm.awaitSearchLanded()
            assertTrue(vm.albums.value.isNotEmpty())

            vm.onQueryChange("   ")

            assertEquals("", vm.searchedQuery.value)
            assertTrue(vm.albums.value.isEmpty())
            assertTrue(vm.topResults.value.isEmpty())
            assertTrue(vm.playlists.value.isEmpty())
        }

    @Test
    fun clearingDuringInFlightSearchCancelsItAndLateResponseStaysOut() =
        runBlocking {
            // The reset CANCELS the in-flight job: without that, the
            // slow query would land after the clear and paint its
            // results under the empty field.
            val vm = viewModel()
            // Five sequential upstream calls per search — at 400 ms
            // each the search stays in flight for ~2 s.
            upstream.delayMs = 400
            vm.search("slow")
            awaitCondition("search in flight") { vm.songs.value is UiState.Loading }

            vm.onQueryChange("")
            assertEquals("", vm.searchedQuery.value)
            assertTrue(vm.songs.value is UiState.Success)

            // Outlast the whole in-flight search, then prove its
            // late completion never repopulated anything and earned
            // no Recents slot.
            delay(3_000)
            upstream.delayMs = 0
            assertEquals("", vm.searchedQuery.value)
            assertTrue(vm.topResults.value.isEmpty())
            assertTrue(vm.albums.value.isEmpty())
            assertTrue(vm.artists.value.isEmpty())
            assertTrue(vm.playlists.value.isEmpty())
            val songs = vm.songs.value
            assertTrue(songs is UiState.Success && songs.data.isEmpty())
            assertTrue(repo.recentSearches.first().isEmpty())
        }

    @Test
    fun searchAfterClearWorksAndRecordsRecent() =
        runBlocking {
            // The clear must not wedge the debounce path: typing a
            // fresh query afterwards searches (and records) as usual.
            val vm = viewModel()
            vm.search("one")
            awaitCondition("first recent") { repo.recentSearches.first() == listOf("one") }

            vm.onQueryChange("")
            assertEquals("", vm.searchedQuery.value)

            vm.onQueryChange("two")
            vm.awaitSearchLanded()
            assertEquals("two", vm.searchedQuery.value)
            awaitCondition("recent recorded") { repo.recentSearches.first() == listOf("two", "one") }
        }

    @Test
    fun repeatedSearchMovesRecentToFrontWithoutDuplicating() =
        runBlocking {
            val vm = viewModel()
            vm.search("one")
            awaitCondition("first recent") { repo.recentSearches.first() == listOf("one") }
            vm.search("two")
            awaitCondition("second recent") { repo.recentSearches.first() == listOf("two", "one") }
            vm.search("one")
            awaitCondition("re-search reorders") { repo.recentSearches.first() == listOf("one", "two") }
        }

    private companion object {
        const val TIMEOUT_MS = 10_000L

        // Canned upstream payloads in the real wire shapes (trimmed
        // from core:network's UpstreamParsingTest fixtures); the
        // album/playlist items carry the `"list": ""` shape behind
        // the 84760b9 regression.
        const val GLOBAL_JSON =
            """{"topquery":{"data":[{"id":"t1","title":"Tum Hi Ho",""" +
                """"image":"https://c.saavncdn.com/x-150x150.jpg",""" +
                """"more_info":{"album":"Aashiqui 2","primary_artists":"Arijit Singh","language":"hindi"}}],"position":0},""" +
                """"songs":{"data":[],"position":1},"albums":{"data":[],"position":2},""" +
                """"artists":{"data":[],"position":3},"playlists":{"data":[],"position":4}}"""

        const val SONGS_JSON =
            """{"total":"1","start":0,"results":[""" +
                """{"id":"s1","title":"Song One","type":"song",""" +
                """"image":"https://c.saavncdn.com/1-150x150.jpg",""" +
                """"more_info":{"duration":"200","has_lyrics":"true","album":"Album One",""" +
                """"artistMap":{"primary_artists":[{"id":"art1","name":"Artist One"}],"featured_artists":[],"artists":[]}}}]}"""

        const val ALBUMS_JSON =
            """{"total":1,"start":0,"results":[""" +
                """{"id":"al1","title":"Album One","type":"album","list_count":"0","list":""}]}"""

        const val ARTISTS_JSON =
            """{"total":1,"start":0,"results":[""" +
                """{"id":"art1","name":"Artist One","image":"https://c.saavncdn.com/a-150x150.jpg"}]}"""

        const val PLAYLISTS_JSON =
            """{"total":1,"start":0,"results":[""" +
                """{"id":"pl1","title":"Playlist One","type":"playlist","list_count":"37","list":""}]}"""
    }
}
