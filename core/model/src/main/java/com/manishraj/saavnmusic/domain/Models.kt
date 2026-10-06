package com.manishraj.saavnmusic.domain

data class Song(
    val id: String,
    val name: String,
    val artist: String,
    val album: String?,
    val imageUrl: String?,
    val durationSec: Long?,
    val streamUrl: String?,
    val downloadUrls: List<Pair<String, String>> = emptyList(),
    val hasLyrics: Boolean = false,
    val year: String? = null,
    val language: String = "",
)

data class Album(
    val id: String,
    val name: String,
    val artist: String,
    val imageUrl: String?,
    val year: String?,
    val songCount: Int?,
    val songs: List<Song> = emptyList(),
    val description: String? = null,
)

data class Playlist(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val songCount: Int?,
    val songs: List<Song> = emptyList(),
    val description: String? = null,
)

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val followers: Long? = null,
    val bio: String? = null,
    val topSongs: List<Song> = emptyList(),
    val topAlbums: List<Album> = emptyList(),
)

/** Home feed sections, assembled from the upstream browse-modules payload (already classified by shape). */
data class HomeContent(
    val trendingSongs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val artists: List<Artist> = emptyList(),
)

/** Global search (autocomplete) results: lightweight items; songs here are NOT playable until resolved by id. */
data class GlobalSearch(
    val topSongs: List<Song> = emptyList(),
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
)

/** A downloaded track as the UI needs it (mirrors the Room row, without exposing the entity). */
data class DownloadInfo(
    val songId: String,
    val name: String,
    val artist: String,
    val album: String?,
    val imageUrl: String?,
    val filePath: String,
    val quality: String,
    val sizeBytes: Long = 0,
    val status: String = "COMPLETED",
    val progress: Int = 100,
)

/** A user-created, on-device playlist (no accounts — Room only). */
data class LocalPlaylist(
    val id: Long,
    val name: String,
    val songCount: Int = 0,
)

fun formatDuration(sec: Long?): String {
    if (sec == null) return ""
    return "%d:%02d".format(sec / 60, sec % 60)
}

/** jiosaavn-dl naming: sanitized "Artist - Title.m4a", albums as "Artist - Album [Year]/NN. Title.m4a" (see STUDY.md). */
fun sanitizeFileName(s: String): String = s.replace(Regex("[\\/:*?\"<>|]"), "").trim().ifBlank { "track" }

fun downloadFileName(
    song: Song,
    position: Int? = null,
    total: Int? = null,
): String =
    if (position != null) {
        "%02d. %s.m4a".format(position, sanitizeFileName(song.name))
    } else {
        "%s - %s.m4a".format(sanitizeFileName(song.artist), sanitizeFileName(song.name))
    }
