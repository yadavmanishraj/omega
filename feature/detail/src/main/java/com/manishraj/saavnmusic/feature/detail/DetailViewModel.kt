package com.manishraj.saavnmusic.feature.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.manishraj.saavnmusic.data.repository.MusicRepository
import com.manishraj.saavnmusic.domain.Album
import com.manishraj.saavnmusic.domain.Artist
import com.manishraj.saavnmusic.domain.LocalPlaylist
import com.manishraj.saavnmusic.domain.Playlist
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.UiState
import com.manishraj.saavnmusic.download.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel
    @Inject
    constructor(
        private val repo: MusicRepository,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        val album = MutableStateFlow<UiState<Album>>(UiState.Loading)
        val playlist = MutableStateFlow<UiState<Playlist>>(UiState.Loading)
        val artist = MutableStateFlow<UiState<Artist>>(UiState.Loading)
        val playlists: StateFlow<List<LocalPlaylist>> =
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
         * Row-menu Download (the D6 parity fix): the same path the
         * player and Library use — the user's download-quality
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
