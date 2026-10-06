package com.manishraj.saavnmusic.data.remote.dto
import kotlinx.serialization.Serializable

@Serializable data class ApiResponse<T>(
    val success: Boolean = true,
    val data: T? = null,
)

@Serializable data class LinkDto(
    val quality: String = "",
    val url: String = "",
)

@Serializable data class ArtistMapDto(
    val primary: List<ArtistDto> = emptyList(),
    val featured: List<ArtistDto> = emptyList(),
    val all: List<ArtistDto> = emptyList(),
)

@Serializable data class ArtistDto(
    val id: String? = null,
    val name: String? = null,
    val role: String? = null,
    val type: String? = null,
    val image: List<LinkDto> = emptyList(),
    val url: String? = null,
)

@Serializable data class AlbumRefDto(
    val id: String? = null,
    val name: String? = null,
    val url: String? = null,
)

@Serializable data class SongDto(
    val id: String,
    val name: String = "",
    val type: String = "song",
    val year: String? = null,
    val releaseDate: String? = null,
    val duration: Long? = null,
    val label: String? = null,
    val explicitContent: Boolean = false,
    val playCount: Long? = null,
    val language: String = "",
    val hasLyrics: Boolean = false,
    val lyricsId: String? = null,
    val url: String = "",
    val copyright: String? = null,
    val album: AlbumRefDto? = null,
    val artists: ArtistMapDto = ArtistMapDto(),
    val image: List<LinkDto> = emptyList(),
    val downloadUrl: List<LinkDto> = emptyList(),
    val lyrics: LyricsDto? = null,
)

@Serializable data class LyricsDto(
    val lyrics: String = "",
    val copyright: String? = null,
    val snippet: String? = null,
)

@Serializable data class AlbumDto(
    val id: String,
    val name: String = "",
    val description: String? = null,
    val year: String? = null,
    val playCount: Long? = null,
    val songCount: Int? = null,
    val language: String = "",
    val explicitContent: Boolean = false,
    val url: String = "",
    val artists: ArtistMapDto = ArtistMapDto(),
    val image: List<LinkDto> = emptyList(),
    val songs: List<SongDto> = emptyList(),
)

@Serializable data class PlaylistDto(
    val id: String,
    val name: String = "",
    val description: String? = null,
    val year: String? = null,
    val playCount: Long? = null,
    val songCount: Int? = null,
    val language: String = "",
    val explicitContent: Boolean = false,
    val url: String = "",
    val image: List<LinkDto> = emptyList(),
    val artists: List<ArtistDto> = emptyList(),
    val songs: List<SongDto> = emptyList(),
)

@Serializable data class ArtistDetailDto(
    val id: String,
    val name: String = "",
    val url: String = "",
    val type: String = "artist",
    val followerCount: Long? = null,
    val fanCount: Long? = null,
    val isVerified: Boolean = false,
    val dominantLanguage: String? = null,
    val dominantType: String? = null,
    val bio: List<BioDto> = emptyList(),
    val dob: String? = null,
    val image: List<LinkDto> = emptyList(),
    val topSongs: List<SongDto> = emptyList(),
    val topAlbums: List<AlbumDto> = emptyList(),
    val similarArtists: List<ArtistDetailDto> = emptyList(),
)

@Serializable data class BioDto(
    val text: String? = null,
    val title: String? = null,
    val sequence: Int? = null,
)

@Serializable data class SearchResultDto<T>(
    val total: Int = 0,
    val start: Int = 0,
    val results: List<T> = emptyList(),
)

@Serializable data class GlobalSearchDto(
    val topQuery: SearchSectionDto<SongDto>? = null,
    val songs: SearchSectionDto<SongDto>? = null,
    val albums: SearchSectionDto<AlbumDto>? = null,
    val artists: SearchSectionDto<ArtistDetailDto>? = null,
    val playlists: SearchSectionDto<PlaylistDto>? = null,
)

@Serializable data class SearchSectionDto<T>(
    val results: List<T> = emptyList(),
    val position: Int = 0,
)

/** Quality helpers matching the API: downloadUrl/image are quality->url lists (12/48/96/160/320kbps; images 50x50,150x150,500x500). */
fun List<LinkDto>.bestUrl(): String? = lastOrNull { it.url.isNotBlank() }?.url

fun List<LinkDto>.urlForQuality(q: String): String? = firstOrNull { it.quality == q }?.url ?: bestUrl()

fun SongDto.streamUrl(quality: String) = downloadUrl.urlForQuality(quality)

fun SongDto.artwork() = image.bestUrl()
