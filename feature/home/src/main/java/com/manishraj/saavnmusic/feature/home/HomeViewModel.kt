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
                    // Home is fed by the upstream browse-modules payload
                    // (content.getBrowseModules), classified by shape in
                    // the network mappers - see UPSTREAM_VALIDATION §4.
                    val home = repo.home()
                    trending.value = UiState.Success(home.trendingSongs)
                    albums.value = home.albums
                    playlists.value = home.playlists
                    artists.value = home.artists
                } catch (e: Exception) {
                    trending.value = UiState.Error(e.message ?: "Network error")
                }
            }
        }
    }
