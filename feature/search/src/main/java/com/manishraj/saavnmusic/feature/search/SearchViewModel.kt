package com.manishraj.saavnmusic.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manishraj.saavnmusic.data.repository.ConnectivityObserver
import com.manishraj.saavnmusic.data.repository.MusicRepository
import com.manishraj.saavnmusic.domain.Album
import com.manishraj.saavnmusic.domain.Artist
import com.manishraj.saavnmusic.domain.Playlist
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val repo: MusicRepository,
        connectivity: ConnectivityObserver,
    ) : ViewModel() {
        val online: StateFlow<Boolean> = connectivity.online
        val query = MutableStateFlow("")
        val tab = MutableStateFlow(0)
        val songs = MutableStateFlow<UiState<List<Song>>>(UiState.Success(emptyList()))
        val albums = MutableStateFlow<List<Album>>(emptyList())
        val artists = MutableStateFlow<List<Artist>>(emptyList())
        val playlists = MutableStateFlow<List<Playlist>>(emptyList())
        val topResults = MutableStateFlow<List<Song>>(emptyList())

        /** The last submitted/searched query; blank means the idle state. */
        val searchedQuery = MutableStateFlow("")
        val playlists =
            repo.localPlaylists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fun addToPlaylist(
            playlistId: Long,
            song: Song,
        ) {
            viewModelScope.launch { repo.addToPlaylist(playlistId, song) }
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

        init {
            // Typing auto-searches after ~300ms of quiet (spec §5
            // debounce rule); explicit submit/chip taps search at once.
            viewModelScope.launch {
                pendingQuery
                    .debounce(300)
                    .distinctUntilChanged()
                    .collect { q -> if (q.isNotBlank()) runSearch(q, recordRecent = false) }
            }
        }

        fun onQueryChange(q: String) {
            query.value = q
            pendingQuery.value = q.trim()
        }

        fun search(q: String) {
            query.value = q
            pendingQuery.value = ""
            runSearch(q.trim(), recordRecent = true)
        }

        private fun runSearch(
            q: String,
            recordRecent: Boolean,
        ) {
            if (q.isBlank()) return
            viewModelScope.launch {
                songs.value = UiState.Loading
                searchedQuery.value = q
                if (recordRecent) repo.addRecentSearch(q)
                try {
                    val global = repo.searchAll(q)
                    topResults.value = global.topSongs + global.songs
                    songs.value = UiState.Success(repo.searchSongs(q))
                    albums.value = repo.searchAlbums(q)
                    artists.value = repo.searchArtists(q)
                    playlists.value = repo.searchPlaylists(q)
                } catch (e: Exception) {
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
