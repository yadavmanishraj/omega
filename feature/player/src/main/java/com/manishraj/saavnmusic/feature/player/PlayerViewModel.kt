package com.manishraj.saavnmusic.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manishraj.saavnmusic.data.repository.MusicRepository
import com.manishraj.saavnmusic.data.settings.AppSettings
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        val player: PlayerController,
        private val repo: MusicRepository,
    ) : ViewModel() {
        val state = player.state
        val appSettings: StateFlow<AppSettings> =
            repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

        fun play(
            songs: List<Song>,
            index: Int,
        ) {
            val s = songs.getOrNull(index) ?: return
            viewModelScope.launch { repo.recordPlay(s) }
            player.playQueue(songs, index, appSettings.value.streamQuality)
        }

        fun isFavorite(id: String) = repo.isFavorite(id)

        /**
         * Whether [id] is fully downloaded on this device. Mirrors the
         * Library path (the downloads Room flow on [MusicRepository]);
         * the player uses it for the download button's Downloaded state.
         */
        fun isDownloaded(id: String): Flow<Boolean> =
            repo.downloads.map { list ->
                list.any { it.songId == id && it.status == "COMPLETED" }
            }

        fun toggleFavorite(
            s: Song,
            fav: Boolean,
        ) {
            viewModelScope.launch { repo.toggleFavorite(s, fav) }
        }

        fun lyrics(
            id: String,
            onResult: (String?) -> Unit,
        ) {
            viewModelScope.launch { onResult(repo.lyrics(id)) }
        }

        fun suggestions(id: String) {
            viewModelScope.launch {
                try {
                    val extra = repo.suggestions(id)
                    if (extra.isNotEmpty()) player.playQueue(extra, 0, appSettings.value.streamQuality)
                } catch (_: Exception) {
                    // Suggestions are best-effort only; never disturb playback.
                }
            }
        }
    }
