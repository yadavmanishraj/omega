package com.manishraj.saavnmusic.playback

import com.manishraj.saavnmusic.domain.Song
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The persisted playback session (F-16): the last queue, where in it
 * playback stopped, and how far into that track it got. Android
 * kills background processes routinely (aggressively on MIUI), and a
 * music app that forgets what was playing fails its core loop — so
 * [PlayerController] writes this on track change / pause /
 * periodically while playing, and restores it PAUSED on cold start.
 * It never auto-plays.
 *
 * The wire shape is a dedicated DTO (not [Song] itself): the domain
 * model stays free of serialization concerns, and the persisted
 * fields are exactly the minimal metadata needed to rebuild a queue
 * whose items can play (stream/download URLs included — a restored
 * queue of unplayable shells would be worse than none).
 *
 * All encode/decode/restore decisions are pure functions here so
 * they are unit-testable without DataStore or a player; the store
 * in :core:datastore only ever sees the opaque JSON string.
 */
@Serializable
data class SessionUrl(
    val quality: String,
    val url: String,
)

@Serializable
data class SessionSong(
    val id: String,
    val name: String,
    val artist: String,
    val album: String? = null,
    val imageUrl: String? = null,
    val durationSec: Long? = null,
    val streamUrl: String? = null,
    val downloadUrls: List<SessionUrl> = emptyList(),
    val hasLyrics: Boolean = false,
)

@Serializable
data class PlaybackSession(
    val songs: List<SessionSong>,
    val currentIndex: Int,
    val positionMs: Long,
)

/** A session that survived validation, ready to publish as state. */
data class RestoredPlayback(
    val queue: List<Song>,
    val currentIndex: Int,
    val positionMs: Long,
)

fun Song.toSessionSong(): SessionSong =
    SessionSong(
        id = id,
        name = name,
        artist = artist,
        album = album,
        imageUrl = imageUrl,
        durationSec = durationSec,
        streamUrl = streamUrl,
        downloadUrls = downloadUrls.map { (quality, url) -> SessionUrl(quality, url) },
        hasLyrics = hasLyrics,
    )

fun SessionSong.toSong(): Song =
    Song(
        id = id,
        name = name,
        artist = artist,
        album = album,
        imageUrl = imageUrl,
        durationSec = durationSec,
        streamUrl = streamUrl,
        downloadUrls = downloadUrls.map { it.quality to it.url },
        hasLyrics = hasLyrics,
    )

object PlaybackSessionCodec {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    /** The persistable snapshot of a live queue; null when there is nothing worth restoring. */
    fun snapshot(
        queue: List<Song>,
        currentIndex: Int,
        positionMs: Long,
    ): PlaybackSession? {
        if (queue.isEmpty()) return null
        return PlaybackSession(
            songs = queue.map { it.toSessionSong() },
            currentIndex = currentIndex.coerceIn(queue.indices),
            positionMs = positionMs.coerceAtLeast(0),
        )
    }

    fun encode(session: PlaybackSession): String = json.encodeToString(PlaybackSession.serializer(), session)

    /** Decodes a stored payload; null for blank/corrupt input (a bad payload must never crash a cold start). */
    fun decode(raw: String?): PlaybackSession? {
        if (raw.isNullOrBlank()) return null
        return try {
            json.decodeFromString(PlaybackSession.serializer(), raw)
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * The restore decision: a persisted session becomes a paused queue
 * only when it actually has songs; a stale index (queue shrank
 * since the write) clamps to the last song rather than pointing
 * past the end, and a negative position floors at 0.
 */
fun PlaybackSession.toRestoredPlayback(): RestoredPlayback? {
    if (songs.isEmpty()) return null
    return RestoredPlayback(
        queue = songs.map { it.toSong() },
        currentIndex = currentIndex.coerceIn(songs.indices),
        positionMs = positionMs.coerceAtLeast(0),
    )
}
