package com.manishraj.saavnmusic.feature.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.manishraj.saavnmusic.data.repository.MusicRepository
import com.manishraj.saavnmusic.data.repository.RemovedPlaylistSong
import com.manishraj.saavnmusic.domain.DownloadInfo
import com.manishraj.saavnmusic.domain.LocalPlaylist
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.download.DownloadWorker
import com.manishraj.saavnmusic.playback.InsertNextResult
import com.manishraj.saavnmusic.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortMode { NEWEST, OLDEST, A_Z }

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val repo: MusicRepository,
        private val player: PlayerController,
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

        /** Row-menu favorite toggle (the shared menu's Add/Remove). */
        fun toggleFavorite(
            song: Song,
            isFavorite: Boolean,
        ) {
            viewModelScope.launch { repo.toggleFavorite(song, isFavorite) }
        }

        /**
         * Row-menu Download (menu parity, spec §3): the same enqueue
         * [retryDownload] uses — settings quality + the shared worker,
         * which registers the Library row itself on start.
         */
        fun download(song: Song) {
            viewModelScope.launch {
                val quality = repo.settings.first().downloadQuality
                DownloadWorker.enqueue(WorkManager.getInstance(context), song, quality)
            }
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

        /**
         * Row-menu Play next (menu parity, spec §3): the shared
         * [PlayerController]'s engine op. Synchronous — the caller
         * picks the snackbar copy from the result (inserted after
         * the current track vs appended to an idle queue).
         */
        fun playNext(song: Song): InsertNextResult = player.insertNext(song)

        /**
         * Removes [song] from local playlist [playlistId] (the
         * LocalPlaylistDetail row menu). [onRemoved] receives the
         * removed membership so the caller can offer Undo;
         * [restoreToPlaylist] puts it back at its original position.
         */
        fun removeFromPlaylist(
            playlistId: Long,
            song: Song,
            onRemoved: (RemovedPlaylistSong) -> Unit,
        ) {
            viewModelScope.launch {
                repo.removeFromPlaylist(playlistId, song)?.let(onRemoved)
            }
        }

        /** Undo for [removeFromPlaylist] — exact-position restore. */
        fun restoreToPlaylist(removed: RemovedPlaylistSong) {
            viewModelScope.launch { repo.restoreToPlaylist(removed) }
        }

        fun playlistSongs(id: Long): Flow<List<Song>> = repo.playlistSongs(id)

        /**
         * One local playlist by id — the data source for the
         * `library/playlist/{id}` NavHost destination (A17 F-01).
         * Emits null when no playlist matches (unknown id, or the
         * playlist was deleted); callers distinguish "not loaded
         * yet" from "gone" by whether the flow has emitted at all.
         */
        fun playlist(id: Long): Flow<LocalPlaylist?> = repo.localPlaylists.map { list -> list.firstOrNull { it.id == id } }

        fun addToPlaylist(
            pid: Long,
            s: Song,
        ) {
            viewModelScope.launch { repo.addToPlaylist(pid, s) }
        }

        fun createPlaylistAndAdd(
            name: String,
            s: Song,
        ) {
            viewModelScope.launch {
                val playlistId = repo.createPlaylist(name)
                repo.addToPlaylist(playlistId, s)
            }
        }
    }
