package com.manishraj.saavnmusic.feature.home

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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val repo: MusicRepository,
    ) : ViewModel() {
        val trending = MutableStateFlow<UiState<List<Song>>>(UiState.Loading)
        val albums = MutableStateFlow<List<Album>>(emptyList())
        val playlists = MutableStateFlow<List<Playlist>>(emptyList())
        val artists = MutableStateFlow<List<Artist>>(emptyList())
        val history: StateFlow<List<Song>> =
            repo.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        init {
            load()
        }

        fun load() {
            viewModelScope.launch {
                trending.value = UiState.Loading
                try {
                    // Home = global search carousels for evergreen terms (the API has no public trending/home endpoint in the current controller set - see STUDY.md)
                    trending.value = UiState.Success(repo.searchSongs("trending bollywood"))
                    albums.value = repo.searchAlbums("latest albums")
                    playlists.value = repo.searchPlaylists("top playlists")
                    artists.value = repo.searchArtists("top singers")
                } catch (e: Exception) {
                    trending.value = UiState.Error(e.message ?: "Network error")
                }
            }
        }
    }
