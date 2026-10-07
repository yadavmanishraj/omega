package com.manishraj.saavnmusic.playback
import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
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
    /**
     * The last playback failure, surfaced honestly to the UI (player
     * error line + snackbar) instead of the old silent 0:00 player.
     * Cleared on the next successful prepare (STATE_READY), on a new
     * queue, and by [PlayerController.retry].
     */
    val errorMessage: String? = null,
    /** Monotonic count of failures, so the UI reacts once per error. */
    val errorSeq: Int = 0,
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

        // User-intent holds (BUG-2): a seek/shuffle/repeat the user
        // just issued stays authoritative in the published state
        // until the player confirms it — see SeekHold.kt.
        private val pendingSeeks = PendingSeekTracker()
        private val shuffleHold = IntentHold<Boolean>()
        private val repeatHold = IntentHold<Int>()

        // History recording (BUG-5): the tracker decides which track
        // starts are plays; consumers collect them here and persist.
        private val playStartTracker = PlayStartTracker()
        private val _playStarts = MutableSharedFlow<Song>(extraBufferCapacity = 32)
        val playStarts: SharedFlow<Song> = _playStarts.asSharedFlow()

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

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int,
                    ) {
                        // The engine applied a seek: positions are
                        // truth again, the BUG-2 hold can end. (The
                        // proximity check in the poll covers any
                        // batch where sync runs before this fires.)
                        if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                            pendingSeeks.onSeekConfirmed()
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        // A failed stream (offline tap on a song that
                        // was never downloaded, dead URL) used to sit
                        // at 0:00 with zero feedback. Surface it: the
                        // player UI shows an error line + Retry and
                        // fires a snackbar (Wave 2a, phone-QA minor).
                        _state.update {
                            it.copy(
                                errorMessage = error.message ?: "Playback error",
                                errorSeq = it.errorSeq + 1,
                                isBuffering = false,
                            )
                        }
                    }
                },
            )
            scope.launch {
                while (true) {
                    val p = controller
                    if (p !=
                        null
                    ) {
                        // While a user seek is unconfirmed, the
                        // player's position snapshot is the PRE-seek
                        // value — publishing it reverted the UI to
                        // the exact pre-gesture position (BUG-2).
                        val positionMs =
                            pendingSeeks.resolve(
                                p.currentPosition,
                                p.currentMediaItem?.mediaId,
                                SystemClock.elapsedRealtime(),
                            )
                        _state.update { it.copy(positionMs = positionMs, durationMs = p.duration.coerceAtLeast(0)) }
                    }
                    delay(500)
                }
            }
        }

        private fun sync(p: Player) {
            val now = SystemClock.elapsedRealtime()
            // Follow the player's current item so auto-advance (and
            // next/prev) refresh the Now Playing song, not just
            // user-initiated plays. Matched by mediaId (= song id);
            // falls back to the existing song when the queue has
            // no match (e.g. suggestions replaced the queue).
            val mediaId = p.currentMediaItem?.mediaId
            val snapshot = _state.value
            val advanced =
                mediaId?.let { id -> snapshot.queue.firstOrNull { s -> s.id == id } }
            val newCurrent = advanced ?: snapshot.current
            // Record the play centrally (BUG-5): this observer sees
            // EVERY track start — auto-advance, next/prev, queue
            // taps — not just the row-tap path that used to record.
            playStartTracker.onObservedCurrent(newCurrent)?.let { _playStarts.tryEmit(it) }
            _state.update {
                it.copy(
                    current = newCurrent,
                    isPlaying = p.isPlaying,
                    isBuffering = p.playbackState == Player.STATE_BUFFERING,
                    // The player's snapshot lags a just-issued toggle
                    // by a session round-trip; an events batch queued
                    // before the toggle would publish the pre-toggle
                    // value here and the toggle looked dropped
                    // (BUG-2). The user's intent wins until the
                    // player confirms it or the hold expires.
                    shuffle = shuffleHold.resolve(p.shuffleModeEnabled, now),
                    repeatMode = repeatHold.resolve(p.repeatMode, now),
                    speed = p.playbackParameters.speed,
                    // A successful prepare heals the surfaced error
                    // (auto-advance recovered, Retry worked, ...).
                    errorMessage =
                        if (p.playbackState == Player.STATE_READY) null else it.errorMessage,
                )
            }
        }

        /** Stream quality of the most recent [playQueue]; reused by [insertNext]. */
        private var lastQuality: String = "320kbps"

        /** Builds the timeline item for [song] at [quality], or null when the song has no playable URL. */
        private fun mediaItemFor(
            song: Song,
            quality: String,
        ): MediaItem? {
            val url =
                song.downloadUrls.firstOrNull { it.first == quality }?.second ?: song.streamUrl
                    ?: return null
            return MediaItem
                .Builder()
                .setUri(url)
                .setMediaId(song.id)
                .setMediaMetadata(
                    MediaMetadata
                        .Builder()
                        .setTitle(song.name)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(
                            song.imageUrl?.let {
                                android.net.Uri.parse(it)
                            },
                        ).build(),
                ).build()
        }

        fun playQueue(
            songs: List<Song>,
            start: Int,
            quality: String,
        ) {
            val c =
                controller ?: return
            lastQuality = quality
            pendingSeeks.clear()
            // Keep the song beside its item: songs without a playable
            // URL drop out of the timeline, so the engine's start
            // index addresses the FILTERED list. Recording history
            // (BUG-5) must name the song the engine will actually
            // start at — the pair at that index — not songs[start],
            // which may never play.
            val pairs = songs.mapNotNull { s -> mediaItemFor(s, quality)?.let { item -> s to item } }
            _state.update {
                it.copy(queue = songs, current = songs.getOrNull(start), errorMessage = null)
            }
            pairs
                .getOrNull(start.coerceAtLeast(0))
                ?.first
                ?.let { startSong -> _playStarts.tryEmit(playStartTracker.onExplicitPlay(startSong)) }
            c.setMediaItems(pairs.map { it.second }, start.coerceAtLeast(0), 0L)
            c.prepare()
            c.play()
        }

        /**
         * "Play next": inserts [song] into the timeline immediately
         * after the current item and mirrors the insertion in
         * [PlayerState.queue]. With no active queue the song is
         * appended (to timeline and state) WITHOUT starting playback
         * and reported as [InsertNextResult.APPENDED] — see
         * [planInsertNext] for the full semantics. Menu wiring lives
         * in the feature modules (a later mini-wave); this is only
         * the engine op.
         */
        fun insertNext(song: Song): InsertNextResult {
            val st = _state.value
            val timelineIndex = controller?.currentMediaItemIndex ?: -1
            val currentIndex =
                if (timelineIndex >= 0) {
                    timelineIndex
                } else {
                    st.queue.indexOfFirst { it.id == st.current?.id }
                }
            val plan = planInsertNext(st.queue, currentIndex, song)
            controller?.let { c ->
                mediaItemFor(song, lastQuality)?.let { item -> c.addMediaItem(plan.index, item) }
            }
            _state.update { it.copy(queue = plan.queue) }
            return plan.result
        }

        /**
         * Retries after a playback error: re-prepares the current
         * item so ExoPlayer re-runs its load, then plays. The
         * surfaced error is cleared up front; if the retry fails too,
         * [Player.Listener.onPlayerError] surfaces a fresh one.
         */
        fun retry() {
            val c = controller ?: return
            _state.update { it.copy(errorMessage = null) }
            c.prepare()
            c.play()
        }

        fun playPause() {
            val c = controller ?: return
            if (c.isPlaying) c.pause() else c.play()
        }

        fun next() {
            pendingSeeks.clear()
            controller?.seekToNextMediaItem()
        }

        fun prev() {
            pendingSeeks.clear()
            controller?.seekToPreviousMediaItem()
        }

        fun seekTo(ms: Long) {
            val c = controller ?: return
            // The seek is authoritative from this instant (BUG-2):
            // publish the target now and hold it against the
            // position poll until the player confirms — otherwise
            // the poll republishes the pre-seek snapshot and the UI
            // reverts to the exact pre-gesture position.
            pendingSeeks.onSeek(ms, c.currentMediaItem?.mediaId, SystemClock.elapsedRealtime())
            _state.update { it.copy(positionMs = ms) }
            c.seekTo(ms)
        }

        fun toggleShuffle() {
            val c = controller ?: return
            val now = SystemClock.elapsedRealtime()
            // Base the toggle on the last user intent when one is
            // still in flight: the controller's snapshot may not
            // have caught up with the previous tap yet.
            val target = !(shuffleHold.intendedValue(now) ?: c.shuffleModeEnabled)
            shuffleHold.intend(target, now)
            c.shuffleModeEnabled = target
        }

        fun cycleRepeat() {
            val c =
                controller ?: return
            val now = SystemClock.elapsedRealtime()
            // Cycle OFF → ALL → ONE → OFF (see RepeatMode.kt);
            // based on the in-flight intent for the same reason as
            // toggleShuffle, and held against stale sync publishes.
            val next = nextEngineRepeatMode(repeatHold.intendedValue(now) ?: c.repeatMode)
            repeatHold.intend(next, now)
            c.repeatMode = next
        }

        fun setSpeed(v: Float) {
            controller?.setPlaybackSpeed(v)
        }

        fun playIndex(i: Int) {
            pendingSeeks.clear()
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
