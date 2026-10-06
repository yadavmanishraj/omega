package com.manishraj.saavnmusic.feature.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.manishraj.saavnmusic.data.repository.MusicRepository
import com.manishraj.saavnmusic.domain.DownloadInfo
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.download.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortMode { NEWEST, OLDEST, A_Z }

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val repo: MusicRepository,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        val favorites = repo.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        val downloads = repo.downloads.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        val history = repo.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        val playlists = repo.localPlaylists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        val sortMode = MutableStateFlow(SortMode.NEWEST)

        fun createPlaylist(name: String) {
            viewModelScope.launch { repo.createPlaylist(name) }
        }

        fun deletePlaylist(id: Long) {
            viewModelScope.launch { repo.deletePlaylist(id) }
        }

        fun clearHistory() {
            viewModelScope.launch { repo.clearHistory() }
        }

        fun unfavorite(song: Song) {
            viewModelScope.launch { repo.toggleFavorite(song, fav = true) }
        }

        fun restoreFavorite(song: Song) {
            viewModelScope.launch { repo.addFavorite(song) }
        }

        fun deleteDownload(info: DownloadInfo) {
            // Cancel any in-flight transfer first: the worker's
            // cancellation cleanup removes its partial file and row,
            // and repo.deleteDownload removes row + file + sidecar —
            // without the cancel, a running worker could re-register
            // the download after the delete.
            WorkManager.getInstance(context).cancelUniqueWork(DownloadWorker.uniqueWorkName(info.songId))
            viewModelScope.launch { repo.deleteDownload(info.songId) }
        }

        /** Retry a failed download, and also the Undo path after a delete (the file is gone; re-download). */
        fun retryDownload(info: DownloadInfo) {
            viewModelScope.launch {
                val song = runCatching { repo.song(info.songId) }.getOrNull() ?: return@launch
                val quality = repo.settings.first().downloadQuality
                DownloadWorker.enqueue(WorkManager.getInstance(context), song, quality)
            }
        }

        fun playlistSongs(id: Long): Flow<List<Song>> = repo.playlistSongs(id)

        fun addToPlaylist(
            pid: Long,
            s: Song,
        ) {
            viewModelScope.launch { repo.addToPlaylist(pid, s) }
        }
    }
