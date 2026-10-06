package com.manishraj.saavnmusic.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manishraj.saavnmusic.data.repository.MusicRepository
import com.manishraj.saavnmusic.domain.Album
import com.manishraj.saavnmusic.domain.Artist
import com.manishraj.saavnmusic.domain.Playlist
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val repo: MusicRepository,
    ) : ViewModel() {
        val query = MutableStateFlow("")
        val tab = MutableStateFlow(0)
        val songs = MutableStateFlow<UiState<List<Song>>>(UiState.Success(emptyList()))
        val albums = MutableStateFlow<List<Album>>(emptyList())
        val artists = MutableStateFlow<List<Artist>>(emptyList())
        val playlists = MutableStateFlow<List<Playlist>>(emptyList())
        val recent = repo.recentSearches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fun search(q: String) {
            query.value = q
            if (q.isBlank()) return
            viewModelScope.launch {
                songs.value = UiState.Loading
                repo.addRecentSearch(q)
                try {
                    songs.value = UiState.Success(repo.searchSongs(q))
                    albums.value = repo.searchAlbums(q)
                    artists.value = repo.searchArtists(q)
                    playlists.value = repo.searchPlaylists(q)
                } catch (e: Exception) {
                    songs.value = UiState.Error(e.message ?: "Search failed")
                }
            }
        }

        fun clearRecent() {
            viewModelScope.launch { repo.clearRecentSearches() }
        }
    }
