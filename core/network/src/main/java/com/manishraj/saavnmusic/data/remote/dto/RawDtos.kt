package com.manishraj.saavnmusic.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Raw DTOs for JioSaavn's upstream `api.php` responses, mirroring the
 * jiosaavn-api repository's Zod models (docs/sdlc/UPSTREAM_SPEC.md §3-§4).
 *
 * Upstream is stringly-typed and unstable: most numbers/booleans arrive
 * as JSON strings, declared fields are sometimes absent, and undeclared
 * fields appear freely. Every field here is therefore nullable/defaulted
 * and the shared Json config uses ignoreUnknownKeys.
 */

/** Accepts a JSON number OR a numeric string (upstream is inconsistent even within one payload family). */
object FlexibleIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.manishraj.saavnmusic.FlexibleInt", PrimitiveKind.INT)

    override fun serialize(
        encoder: Encoder,
        value: Int,
    ) {
        encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return 0
        val primitive = element as? JsonPrimitive ?: return 0
        return primitive.intOrNull ?: primitive.contentOrNull?.toIntOrNull() ?: 0
    }
}

@Serializable
data class RawArtistMapDto(
    val id: String? = null,
    val name: String? = null,
    val role: String? = null,
    val type: String? = null,
    val image: String? = null,
    @SerialName("perma_url") val permaUrl: String? = null,
)

@Serializable
data class RawArtistMapGroupDto(
    @SerialName("primary_artists") val primary: List<RawArtistMapDto> = emptyList(),
    @SerialName("featured_artists") val featured: List<RawArtistMapDto> = emptyList(),
    val artists: List<RawArtistMapDto> = emptyList(),
)

@Serializable
data class RawSongMoreInfoDto(
    @SerialName("release_date") val releaseDate: String? = null,
    val duration: String? = null,
    val label: String? = null,
    @SerialName("has_lyrics") val hasLyrics: String? = null,
    @SerialName("lyrics_id") val lyricsId: String? = null,
    @SerialName("copyright_text") val copyrightText: String? = null,
    @SerialName("album_id") val albumId: String? = null,
    val album: String? = null,
    @SerialName("album_url") val albumUrl: String? = null,
    @SerialName("encrypted_media_url") val encryptedMediaUrl: String? = null,
    @SerialName("320kbps") val is320kbps: String? = null,
    val artistMap: RawArtistMapGroupDto? = null,
)

@Serializable
data class RawSongDto(
    val id: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val type: String? = null,
    @SerialName("perma_url") val permaUrl: String? = null,
    val image: String? = null,
    val language: String? = null,
    val year: String? = null,
    @SerialName("play_count") val playCount: String? = null,
    @SerialName("explicit_content") val explicitContent: String? = null,
    @SerialName("more_info") val moreInfo: RawSongMoreInfoDto? = null,
)

@Serializable
data class RawSongDetailsDto(
    val songs: List<RawSongDto> = emptyList(),
)

@Serializable
data class RawPagedDto<T>(
    @Serializable(with = FlexibleIntSerializer::class) val total: Int = 0,
    @Serializable(with = FlexibleIntSerializer::class) val start: Int = 0,
    val results: List<T> = emptyList(),
)

@Serializable
data class RawAlbumMoreInfoDto(
    val artistMap: RawArtistMapGroupDto? = null,
    @SerialName("song_count") val songCount: String? = null,
    @SerialName("copyright_text") val copyrightText: String? = null,
    val music: String? = null,
)

@Serializable
data class RawAlbumDto(
    val id: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    @SerialName("header_desc") val headerDesc: String? = null,
    val type: String? = null,
    @SerialName("perma_url") val permaUrl: String? = null,
    val image: String? = null,
    val language: String? = null,
    val year: String? = null,
    @SerialName("play_count") val playCount: String? = null,
    @SerialName("explicit_content") val explicitContent: String? = null,
    @SerialName("list_count") val listCount: String? = null,
    val list: List<RawSongDto> = emptyList(),
    @SerialName("more_info") val moreInfo: RawAlbumMoreInfoDto? = null,
)

@Serializable
data class RawPlaylistMoreInfoDto(
    val uid: String? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    @SerialName("follower_count") val followerCount: String? = null,
    @SerialName("song_count") val songCount: String? = null,
    val artists: List<RawArtistMapDto> = emptyList(),
)

@Serializable
data class RawPlaylistDto(
    val id: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    @SerialName("header_desc") val headerDesc: String? = null,
    val description: String? = null,
    val type: String? = null,
    @SerialName("perma_url") val permaUrl: String? = null,
    val image: String? = null,
    val language: String? = null,
    val year: String? = null,
    @SerialName("play_count") val playCount: String? = null,
    @SerialName("explicit_content") val explicitContent: String? = null,
    @SerialName("list_count") val listCount: String? = null,
    val list: List<RawSongDto> = emptyList(),
    @SerialName("more_info") val moreInfo: RawPlaylistMoreInfoDto? = null,
)

@Serializable
data class RawSimilarArtistDto(
    val id: String? = null,
    val name: String? = null,
    @SerialName("perma_url") val permaUrl: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
)

@Serializable
data class RawArtistPageDto(
    val artistId: String? = null,
    val id: String? = null,
    val name: String? = null,
    val subtitle: String? = null,
    val image: String? = null,
    @SerialName("follower_count") val followerCount: String? = null,
    @SerialName("fan_count") val fanCount: String? = null,
    val isVerified: Boolean? = null,
    val dominantLanguage: String? = null,
    val dominantType: String? = null,
    val bio: String? = null,
    val dob: String? = null,
    val topSongs: List<RawSongDto> = emptyList(),
    val topAlbums: List<RawAlbumDto> = emptyList(),
    val singles: List<RawSongDto> = emptyList(),
    val similarArtists: List<RawSimilarArtistDto> = emptyList(),
)

// ---- Global search (autocomplete.get) ----

@Serializable
data class RawSectionDto<T>(
    val data: List<T> = emptyList(),
    @Serializable(with = FlexibleIntSerializer::class) val position: Int = 0,
)

@Serializable
data class RawGlobalSongMoreInfoDto(
    val album: String? = null,
    @SerialName("primary_artists") val primaryArtists: String? = null,
    val singers: String? = null,
    val language: String? = null,
)

@Serializable
data class RawGlobalSongItemDto(
    val id: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val type: String? = null,
    val image: String? = null,
    @SerialName("perma_url") val permaUrl: String? = null,
    val description: String? = null,
    @SerialName("more_info") val moreInfo: RawGlobalSongMoreInfoDto? = null,
)

@Serializable
data class RawGlobalAlbumMoreInfoDto(
    val music: String? = null,
    val year: String? = null,
    @SerialName("song_pids") val songPids: String? = null,
    val language: String? = null,
)

@Serializable
data class RawGlobalAlbumItemDto(
    val id: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val type: String? = null,
    val image: String? = null,
    @SerialName("perma_url") val permaUrl: String? = null,
    val description: String? = null,
    @SerialName("more_info") val moreInfo: RawGlobalAlbumMoreInfoDto? = null,
)

@Serializable
data class RawGlobalArtistItemDto(
    val id: String? = null,
    val title: String? = null,
    val image: String? = null,
    val type: String? = null,
    @Serializable(with = FlexibleIntSerializer::class) val position: Int = 0,
)

@Serializable
data class RawGlobalPlaylistMoreInfoDto(
    val language: String? = null,
)

@Serializable
data class RawGlobalPlaylistItemDto(
    val id: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val type: String? = null,
    val image: String? = null,
    @SerialName("perma_url") val permaUrl: String? = null,
    val description: String? = null,
    @SerialName("more_info") val moreInfo: RawGlobalPlaylistMoreInfoDto? = null,
)

/** Note: upstream also returns `episodes` and `shows` sections; the app ignores them. */
@Serializable
data class RawGlobalSearchDto(
    val topquery: RawSectionDto<RawGlobalSongItemDto>? = null,
    val songs: RawSectionDto<RawGlobalSongItemDto>? = null,
    val albums: RawSectionDto<RawGlobalAlbumItemDto>? = null,
    val artists: RawSectionDto<RawGlobalArtistItemDto>? = null,
    val playlists: RawSectionDto<RawGlobalPlaylistItemDto>? = null,
)

// ---- Lyrics / radio / browse ----

@Serializable
data class RawLyricsDto(
    val lyrics: String? = null,
    @SerialName("lyrics_copyright") val lyricsCopyright: String? = null,
    val snippet: String? = null,
)

@Serializable
data class RawStationCreatedDto(
    val stationid: String? = null,
)

@Serializable
data class RawStationEntryDto(
    val song: RawSongDto? = null,
)

/**
 * Browse-modules payload (Home). Per live validation, the five content
 * sections are DIRECT top-level arrays of raw entities whose `type`
 * field is unreliable, so they are kept as [JsonObject]s and classified
 * by shape in the mapper. `radio`/`top_shows` are wrapper objects and
 * are not used by the app.
 */
@Serializable
data class RawBrowseModulesDto(
    @SerialName("new_trending") val newTrending: List<JsonObject> = emptyList(),
    @SerialName("new_albums") val newAlbums: List<JsonObject> = emptyList(),
    val charts: List<JsonObject> = emptyList(),
    @SerialName("top_playlists") val topPlaylists: List<JsonObject> = emptyList(),
    @SerialName("browse_discover") val browseDiscover: List<JsonObject> = emptyList(),
)
