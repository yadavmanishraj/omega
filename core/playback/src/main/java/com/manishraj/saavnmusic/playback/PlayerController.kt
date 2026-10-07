package com.manishraj.saavnmusic.playback
import android.content.ComponentName
import android.content.Context
import androidx.media3.common.*
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.manishraj.saavnmusic.domain.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.*

data class PlayerState(
    val current: Song? = null,
    val queue: List<Song> = emptyList(),
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val speed: Float = 1f,
)

@Singleton class PlayerController
    @Inject
    constructor(
        @ApplicationContext private val ctx: Context,
    ) {
        private var controller: MediaController? = null
        private val _state = MutableStateFlow(PlayerState())
        val state: StateFlow<PlayerState> = _state
        private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        fun connect() {
            if (controller !=
                null
            ) {
                return
            }
            val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
            val f = MediaController.Builder(ctx, token).buildAsync()
            f.addListener({
                controller =
                    f.get()
                ; attach()
            }, MoreExecutors.directExecutor())
        }

        private fun attach() {
            val c = controller ?: return
            c.addListener(
                object : Player.Listener {
                    override fun onEvents(
                        p: Player,
                        e: Player.Events,
                    ) {
                        sync(p)
                    }
                },
            )
            scope.launch {
                while (true) {
                    val p = controller
                    if (p !=
                        null
                    ) {
                        _state.update { it.copy(positionMs = p.currentPosition, durationMs = p.duration.coerceAtLeast(0)) }
                    }
                    delay(500)
                }
            }
        }

        private fun sync(p: Player) {
            _state.update {
                // Follow the player's current item so auto-advance (and
                // next/prev) refresh the Now Playing song, not just
                // user-initiated plays. Matched by mediaId (= song id);
                // falls back to the existing song when the queue has
                // no match (e.g. suggestions replaced the queue).
                val mediaId = p.currentMediaItem?.mediaId
                val advanced =
                    mediaId?.let { id -> it.queue.firstOrNull { s -> s.id == id } }
                it.copy(
                    current = advanced ?: it.current,
                    isPlaying = p.isPlaying,
                    isBuffering = p.playbackState == Player.STATE_BUFFERING,
                    shuffle = p.shuffleModeEnabled,
                    repeatMode = p.repeatMode,
                    speed = p.playbackParameters.speed,
                )
            }
        }

        fun playQueue(
            songs: List<Song>,
            start: Int,
            quality: String,
        ) {
            val c =
                controller ?: return
            val items =
                songs.mapNotNull { s ->
                    val url =
                        s.downloadUrls.firstOrNull { it.first == quality }?.second ?: s.streamUrl
                    if (url ==
                        null
                    ) {
                        null
                    } else {
                        MediaItem
                            .Builder()
                            .setUri(
                                url,
                            ).setMediaId(
                                s.id,
                            ).setMediaMetadata(
                                MediaMetadata
                                    .Builder()
                                    .setTitle(
                                        s.name,
                                    ).setArtist(s.artist)
                                    .setAlbumTitle(s.album)
                                    .setArtworkUri(
                                        s.imageUrl?.let {
                                            android.net.Uri.parse(it)
                                        },
                                    ).build(),
                            ).build()
                    }
                }
            _state.update { it.copy(queue = songs, current = songs.getOrNull(start)) }
            c.setMediaItems(items, start.coerceAtLeast(0), 0L)
            c.prepare()
            c.play()
        }

        fun playPause() {
            val c = controller ?: return
            if (c.isPlaying) c.pause() else c.play()
        }

        fun next() {
            controller?.seekToNextMediaItem()
        }

        fun prev() {
            controller?.seekToPreviousMediaItem()
        }

        fun seekTo(ms: Long) {
            controller?.seekTo(ms)
        }

        fun toggleShuffle() {
            val c = controller ?: return
            c.shuffleModeEnabled = !c.shuffleModeEnabled
        }

        fun cycleRepeat() {
            val c =
                controller ?: return
            c.repeatMode =
                when (c.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
        }

        fun setSpeed(v: Float) {
            controller?.setPlaybackSpeed(v)
        }

        fun playIndex(i: Int) {
            controller?.seekTo(i, 0)
            controller?.play()
            _state.update { it.copy(current = it.queue.getOrNull(i)) }
        }

        // Sleep timer: simple coroutine cancelling playback after N minutes.
        private var sleepJob: Job? = null

        fun setSleepTimer(minutes: Int) {
            sleepJob?.cancel()
            if (minutes <=
                0
            ) {
                return
            }
            sleepJob =
                scope.launch {
                    delay(minutes * 60_000L)
                    controller?.pause()
                }
        }
    }
