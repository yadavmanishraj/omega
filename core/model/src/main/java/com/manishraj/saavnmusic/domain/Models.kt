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
