package com.manishraj.saavnmusic.playback
import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import androidx.media3.common.*
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.manishraj.saavnmusic.data.session.PlaybackSessionStore
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
     * The armed sleep-timer preset in minutes (0 = off) and the live
     * remaining time, both published by the controller (F-05): the
     * timer's truth lives here, so the player UI shows the armed
     * state after collapse/reopen instead of a composition-local
     * memory that forgot it.
     */
    val sleepMinutes: Int = 0,
    val sleepRemainingMs: Long = 0,
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
        /**
         * Session persistence (F-16). Nullable with a default so the
         * JVM unit tests that construct the controller directly (no
         * Hilt graph) keep compiling and simply run without
         * persistence; Hilt always provides the real store.
         */
        private val sessionStore: PlaybackSessionStore? = null,
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

        // Sleep timer (F-05): the tracker owns the armed preset and
        // its deadline; PlayerState publishes both (see SleepTimer.kt).
        private val sleepTimer = SleepTimerTracker()
        private var sleepJob: Job? = null

        // Session restore (F-16): (queue index, position) of a
        // persisted session that has been published as state but
        // whose engine timeline has NOT been built yet. Building it
        // is deferred to the first transport action so a cold start
        // loads nothing (and can surface no spurious playback
        // error); while it is pending, the position poll must not
        // clobber the restored position with the empty player's 0.
        private var pendingRestore: Pair<Int, Long>? = null

        init {
            scope.launch { restorePersistedSession() }
        }

        /**
         * Writes the current queue/index/position as the restorable
         * session (F-16); clears the stored session when the queue
         * is gone. Fire-and-forget on the controller scope — a lost
         * write only ever costs a staler resume point.
         */
        private fun persistSession() {
            val store = sessionStore ?: return
            val st = _state.value
            val index = st.queue.indexOfFirst { it.id == st.current?.id }
            val session = PlaybackSessionCodec.snapshot(st.queue, index, st.positionMs)
            scope.launch {
                if (session == null) {
                    store.clear()
                } else {
                    store.save(PlaybackSessionCodec.encode(session))
                }
            }
        }

        /**
         * Cold-start restore (F-16): if a persisted session exists,
         * publish it PAUSED — queue, current song, saved position —
         * so the mini-player returns exactly where the process died.
         * Never auto-plays, and never overrides a session the user
         * already started in this process.
         */
        private suspend fun restorePersistedSession() {
            val store = sessionStore ?: return
            val restored =
                PlaybackSessionCodec.decode(store.sessionJson.first())?.toRestoredPlayback()
                    ?: return
            if (_state.value.queue.isNotEmpty() || _state.value.current != null) return
            pendingRestore = restored.currentIndex to restored.positionMs
            val current = restored.queue[restored.currentIndex]
            _state.update {
                it.copy(
                    queue = restored.queue,
                    current = current,
                    positionMs = restored.positionMs,
                    durationMs = (current.durationSec ?: 0) * 1000,
                    isPlaying = false,
                )
            }
        }

        /**
         * Builds the engine timeline for a restored session at the
         * restored index/position (prepared, not yet playing — the
         * caller decides). Returns true when it materialized one.
         * The timeline is built from the PUBLISHED queue, so edits
         * made since the restore (Play next) are honored.
         */
        private fun materializePendingRestore(c: MediaController): Boolean {
            val pending = pendingRestore ?: return false
            if (c.mediaItemCount > 0) {
                pendingRestore = null
                return false
            }
            val indexedPairs =
                _state.value.queue.mapIndexedNotNull { queueIndex, song ->
                    mediaItemFor(song, lastQuality)?.let { item -> queueIndex to item }
                }
            if (indexedPairs.isEmpty()) {
                pendingRestore = null
                return false
            }
            val startPos =
                indexedPairs.indexOfLast { it.first <= pending.first }.takeIf { it >= 0 } ?: 0
            c.setMediaItems(indexedPairs.map { it.second }, startPos, pending.second)
            c.prepare()
            pendingRestore = null
            return true
        }

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
                        // at 0:00 with zero feedback. Surface it in
                        // state; the UI presents each failure on
                        // exactly ONE surface — the full player's
                        // inline row + Retry, or a snackbar from the
                        // mini-player when collapsed — arbitrated
                        // by errorSeq (feature:player, Task 4).
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
                var ticks = 0
                while (true) {
                    val p = controller
                    if (p !=
                        null
                    ) {
                        // While a restored session (F-16) awaits its
                        // first transport action the engine timeline
                        // is empty: its 0 position/duration must not
                        // clobber the restored ones.
                        if (pendingRestore == null) {
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
                        // Sleep countdown (F-05): publish the
                        // remaining time from the tracker's deadline
                        // so the chip and the actual stop share one
                        // truth and survive collapse/reopen.
                        if (sleepTimer.isArmed) {
                            val remaining = sleepTimer.remainingMs(SystemClock.elapsedRealtime())
                            _state.update { it.copy(sleepRemainingMs = remaining) }
                        }
                        // Periodic session persist while playing
                        // (F-16): ~every 10 s, so the resume point
                        // stays fresh without hammering DataStore.
                        ticks++
                        if (ticks % 20 == 0 && p.isPlaying && pendingRestore == null) {
                            persistSession()
                        }
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
            val currentChanged = newCurrent?.id != snapshot.current?.id
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
            // A new current track is a persist point for the
            // restorable session (F-16).
            if (currentChanged) persistSession()
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
            // A fresh queue supersedes any restored session (F-16).
            pendingRestore = null
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
            persistSession()
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
                // With a restored session pending (F-16) the engine
                // timeline doesn't exist yet; the insertion lives in
                // state and is honored when the timeline is built
                // from the published queue at materialization.
                if (pendingRestore == null) {
                    mediaItemFor(song, lastQuality)?.let { item -> c.addMediaItem(plan.index, item) }
                }
            }
            _state.update { it.copy(queue = plan.queue) }
            persistSession()
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
            // First transport action after a cold-start restore
            // (F-16): build the timeline at the saved index/position,
            // then play from exactly there.
            if (materializePendingRestore(c)) {
                c.play()
                return
            }
            if (c.isPlaying) {
                c.pause()
                // Pause is a persist point (F-16): the resume point
                // is where the user actually stopped.
                persistSession()
            } else {
                c.play()
            }
        }

        fun next() {
            pendingSeeks.clear()
            val c = controller ?: return
            materializePendingRestore(c)
            c.seekToNextMediaItem()
        }

        fun prev() {
            pendingSeeks.clear()
            val c = controller ?: return
            materializePendingRestore(c)
            c.seekToPreviousMediaItem()
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
            persistSession()
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
            val c = controller ?: return
            materializePendingRestore(c)
            c.seekTo(i, 0)
            c.play()
            _state.update { it.copy(current = it.queue.getOrNull(i)) }
        }

        /**
         * Arms ([minutes] > 0) or clears (0) the sleep timer and
         * publishes the new truth in [PlayerState] (F-05). Expiry
         * pauses playback and clears the published state, so what
         * the UI shows and what the timer does can never disagree.
         */
        fun setSleepTimer(minutes: Int) {
            sleepJob?.cancel()
            val now = SystemClock.elapsedRealtime()
            if (minutes <= 0) {
                sleepTimer.cancel()
                _state.update { it.copy(sleepMinutes = 0, sleepRemainingMs = 0) }
                return
            }
            sleepTimer.arm(minutes, now)
            _state.update {
                it.copy(sleepMinutes = minutes, sleepRemainingMs = sleepTimer.remainingMs(now))
            }
            sleepJob =
                scope.launch {
                    delay(minutes * 60_000L)
                    sleepTimer.cancel()
                    _state.update { it.copy(sleepMinutes = 0, sleepRemainingMs = 0) }
                    controller?.pause()
                    persistSession()
                }
        }
    }
