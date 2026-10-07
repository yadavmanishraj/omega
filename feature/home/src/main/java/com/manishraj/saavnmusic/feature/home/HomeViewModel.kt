package com.manishraj.saavnmusic.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.manishraj.saavnmusic.data.repository.ConnectivityObserver
import com.manishraj.saavnmusic.data.repository.MusicRepository
import com.manishraj.saavnmusic.domain.Album
import com.manishraj.saavnmusic.domain.Artist
import com.manishraj.saavnmusic.domain.DownloadInfo
import com.manishraj.saavnmusic.domain.LocalPlaylist
import com.manishraj.saavnmusic.domain.Playlist
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.UiState
import com.manishraj.saavnmusic.download.DownloadWorker
import com.manishraj.saavnmusic.playback.InsertNextResult
import com.manishraj.saavnmusic.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Home state (REDESIGN_SPEC §4): each remote section carries its own
 * [UiState] so sections render/omit/fail independently, and the local
 * sections (history, downloads, favorites) keep working offline.
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val repo: MusicRepository,
        private val player: PlayerController,
        connectivity: ConnectivityObserver,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        val online: StateFlow<Boolean> = connectivity.online

        private val _trending = MutableStateFlow<UiState<List<Song>>>(UiState.Loading)
        val trending: StateFlow<UiState<List<Song>>> = _trending.asStateFlow()
        private val _albums = MutableStateFlow<UiState<List<Album>>>(UiState.Loading)
        val albums: StateFlow<UiState<List<Album>>> = _albums.asStateFlow()
        private val _playlists = MutableStateFlow<UiState<List<Playlist>>>(UiState.Loading)
        val playlists: StateFlow<UiState<List<Playlist>>> = _playlists.asStateFlow()
        private val _artists = MutableStateFlow<UiState<List<Artist>>>(UiState.Loading)
        val artists: StateFlow<UiState<List<Artist>>> = _artists.asStateFlow()

        val history: StateFlow<List<Song>> =
            repo.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        val downloads: StateFlow<List<DownloadInfo>> =
            repo.downloads.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        val favorites: StateFlow<List<Song>> =
            repo.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        val userPlaylists: StateFlow<List<LocalPlaylist>> =
            repo.localPlaylists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        init {
            load()
        }

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

        fun load() {
            viewModelScope.launch {
                _trending.value = UiState.Loading
                _albums.value = UiState.Loading
                _playlists.value = UiState.Loading
                _artists.value = UiState.Loading
                try {
                    // Home is fed by the upstream browse-modules payload
                    // (content.getBrowseModules), classified by shape in
                    // the network mappers - see UPSTREAM_VALIDATION §4.
                    // One call feeds all four sections, so a failure is
                    // reported per-section in the UI from the same error.
                    val home = repo.home()
                    _trending.value = UiState.Success(home.trendingSongs)
                    _albums.value = UiState.Success(home.albums)
                    _playlists.value = UiState.Success(home.playlists)
                    _artists.value = UiState.Success(home.artists)
                } catch (e: Exception) {
                    // Friendly cause+fix copy only - never a raw exception
                    // string (REDESIGN_SPEC §4 error-clarity rule).
                    val message = "Couldn't reach the music service. Check your connection, then retry."
                    _trending.value = UiState.Error(message)
                    _albums.value = UiState.Error(message)
                    _playlists.value = UiState.Error(message)
                    _artists.value = UiState.Error(message)
                }
            }
        }
    }
