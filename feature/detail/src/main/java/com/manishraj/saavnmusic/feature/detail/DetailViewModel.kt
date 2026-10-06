package com.manishraj.saavnmusic.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manishraj.saavnmusic.data.repository.MusicRepository
import com.manishraj.saavnmusic.domain.Album
import com.manishraj.saavnmusic.domain.Artist
import com.manishraj.saavnmusic.domain.Playlist
import com.manishraj.saavnmusic.domain.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel
    @Inject
    constructor(
        private val repo: MusicRepository,
    ) : ViewModel() {
        val album = MutableStateFlow<UiState<Album>>(UiState.Loading)
        val playlist = MutableStateFlow<UiState<Playlist>>(UiState.Loading)
        val artist = MutableStateFlow<UiState<Artist>>(UiState.Loading)

        fun loadAlbum(id: String) {
            viewModelScope.launch {
                album.value = UiState.Loading
                try {
                    album.value = UiState.Success(repo.album(id))
                } catch (e: Exception) {
                    album.value = UiState.Error("Couldn't load this. Check your connection, then retry.")
                }
            }
        }

        fun loadPlaylist(id: String) {
            viewModelScope.launch {
                playlist.value = UiState.Loading
                try {
                    playlist.value = UiState.Success(repo.playlist(id))
                } catch (e: Exception) {
                    playlist.value = UiState.Error("Couldn't load this. Check your connection, then retry.")
                }
            }
        }

        fun loadArtist(id: String) {
            viewModelScope.launch {
                artist.value = UiState.Loading
                try {
                    artist.value = UiState.Success(repo.artist(id))
                } catch (e: Exception) {
                    artist.value = UiState.Error("Couldn't load this. Check your connection, then retry.")
                }
            }
        }
    }
