package com.manishraj.saavnmusic.feature.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.manishraj.saavnmusic.data.repository.ConnectivityObserver
import com.manishraj.saavnmusic.data.repository.MusicRepository
import com.manishraj.saavnmusic.domain.Album
import com.manishraj.saavnmusic.domain.Artist
import com.manishraj.saavnmusic.domain.Playlist
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.TopResult
import com.manishraj.saavnmusic.domain.UiState
import com.manishraj.saavnmusic.download.DownloadWorker
import com.manishraj.saavnmusic.playback.InsertNextResult
import com.manishraj.saavnmusic.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val repo: MusicRepository,
        private val player: PlayerController,
        connectivity: ConnectivityObserver,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        val online: StateFlow<Boolean> = connectivity.online
        val query = MutableStateFlow("")
        val tab = MutableStateFlow(0)
        val songs = MutableStateFlow<UiState<List<Song>>>(UiState.Success(emptyList()))
        val albums = MutableStateFlow<List<Album>>(emptyList())
        val artists = MutableStateFlow<List<Artist>>(emptyList())
        val playlists = MutableStateFlow<List<Playlist>>(emptyList())

        /** Mixed-type top results (F-02): entity-typed, never Songs —
         * the screen dispatches each variant to its own navigation,
         * exactly like the category tabs. */
        val topResults = MutableStateFlow<List<TopResult>>(emptyList())

        /** The last submitted/searched query; blank means the idle state. */
        val searchedQuery = MutableStateFlow("")
        val userPlaylists =
            repo.localPlaylists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        /** Favorites membership drives the row menu's favorite item
         * (the shared menu shows Add/Remove from this, spec §3). */
        val favorites: StateFlow<List<Song>> =
            repo.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fun addToPlaylist(
            playlistId: Long,
            song: Song,
        ) {
            viewModelScope.launch { repo.addToPlaylist(playlistId, song) }
        }

        fun toggleFavorite(
            song: Song,
            isFavorite: Boolean,
        ) {
            viewModelScope.launch { repo.toggleFavorite(song, isFavorite) }
        }

        /**
         * Row-menu Play next (menu parity, spec §3): the shared
         * [PlayerController]'s engine op. Synchronous — the caller
         * picks the snackbar copy from the result (inserted after
         * the current track vs appended to an idle queue).
         */
        fun playNext(song: Song): InsertNextResult = player.insertNext(song)

        /**
         * Row-menu Download (menu parity, spec §3): the same path
         * the player and Library use — the user's download-quality
         * setting + [DownloadWorker.enqueue], which registers the
         * Library row itself when the transfer starts.
         */
        fun download(song: Song) {
            viewModelScope.launch {
                val quality = repo.settings.first().downloadQuality
                DownloadWorker.enqueue(WorkManager.getInstance(context), song, quality)
            }
        }

        fun createPlaylistAndAdd(
            name: String,
            song: Song,
        ) {
            viewModelScope.launch {
                val playlistId = repo.createPlaylist(name)
                repo.addToPlaylist(playlistId, song)
            }
        }

        val recent = repo.recentSearches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        private val pendingQuery = MutableStateFlow("")
        private var searchJob: Job? = null

        init {
            // Typing auto-searches after ~300ms of quiet (spec §5
            // debounce rule); explicit submit/chip taps search at once.
            viewModelScope.launch {
                pendingQuery
                    .debounce(300)
                    .distinctUntilChanged()
                    .collect { q -> if (q.isNotBlank()) runSearch(q) }
            }
        }

        fun onQueryChange(q: String) {
            query.value = q
            val trimmed = q.trim()
            pendingQuery.value = trimmed
            // An emptied field means the idle state (BUG-8): the
            // screen derives idle from `searchedQuery`, which only
            // runSearch ever set — so without this reset, clearing
            // the box left the last query's results (or its
            // no-results label) frozen under the empty field for
            // the rest of the process lifetime.
            if (trimmed.isBlank()) resetToIdle()
        }

        fun search(q: String) {
            query.value = q
            pendingQuery.value = ""
            val trimmed = q.trim()
            if (trimmed.isBlank()) {
                resetToIdle()
            } else {
                runSearch(trimmed)
            }
        }

        /**
         * Back to the idle state: no active query, every result
         * holder cleared, and any in-flight search cancelled FIRST —
         * a late response for the cleared query must never
         * repopulate the holders this reset just emptied.
         */
        private fun resetToIdle() {
            searchJob?.cancel()
            searchJob = null
            searchedQuery.value = ""
            topResults.value = emptyList()
            albums.value = emptyList()
            artists.value = emptyList()
            playlists.value = emptyList()
            songs.value = UiState.Success(emptyList())
        }

        private fun runSearch(q: String) {
            if (q.isBlank()) return
            // A newer search supersedes an in-flight one: without
            // this, a slow earlier query could finish last and paint
            // its stale results — or its failure — over the current
            // query's state.
            searchJob?.cancel()
            searchJob =
                viewModelScope.launch {
                    songs.value = UiState.Loading
                    searchedQuery.value = q
                    // Invariant: while a search runs, every result
                    // holder belongs to it — nothing from a previous
                    // query may show through (phone QA major).
                    topResults.value = emptyList()
                    albums.value = emptyList()
                    artists.value = emptyList()
                    playlists.value = emptyList()
                    try {
                        val global = repo.searchAll(q)
                        topResults.value =
                            global.topResults + global.songs.map { TopResult.SongResult(it) }
                        songs.value = UiState.Success(repo.searchSongs(q))
                        albums.value = repo.searchAlbums(q)
                        artists.value = repo.searchArtists(q)
                        playlists.value = repo.searchPlaylists(q)
                        // Only a search that COMPLETED earns a Recents
                        // slot, and it earns it on both entry paths —
                        // submit and debounce alike. (Failures were
                        // once committed before the attempt — Phase B;
                        // debounce successes were once never recorded —
                        // exhaustive phone QA, 2026-10-07.) The DAO
                        // insert is REPLACE on the query key, so a
                        // repeat just moves the query to the front.
                        repo.addRecentSearch(q)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // A failed load REPLACES the previous results:
                        // partial writes from this attempt are cleared
                        // and `songs` carries the error, which the
                        // screen renders as the shared ErrorState on
                        // every tab — stale results never sit silently
                        // under a new query.
                        topResults.value = emptyList()
                        albums.value = emptyList()
                        artists.value = emptyList()
                        playlists.value = emptyList()
                        songs.value =
                            UiState.Error("Couldn't search right now. Check your connection, then retry.")
                    }
                }
        }

        /** Global-search items are lightweight; resolve to a playable song before handing to the player. */
        fun resolveAndPlay(
            item: Song,
            onReady: (List<Song>, Int) -> Unit,
        ) {
            viewModelScope.launch {
                try {
                    onReady(listOf(repo.song(item.id)), 0)
                } catch (e: Exception) {
                    // Silent: a failed resolve just doesn't start playback.
                }
            }
        }

        fun removeRecent(q: String) {
            viewModelScope.launch { repo.removeRecentSearch(q) }
        }

        fun clearRecent() {
            viewModelScope.launch { repo.clearRecentSearches() }
        }
    }
