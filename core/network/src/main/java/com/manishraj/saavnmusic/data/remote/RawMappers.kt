package com.manishraj.saavnmusic.data.remote

import com.manishraj.saavnmusic.data.remote.dto.RawAlbumDto
import com.manishraj.saavnmusic.data.remote.dto.RawArtistMapDto
import com.manishraj.saavnmusic.data.remote.dto.RawArtistMapGroupDto
import com.manishraj.saavnmusic.data.remote.dto.RawArtistPageDto
import com.manishraj.saavnmusic.data.remote.dto.RawBrowseModulesDto
import com.manishraj.saavnmusic.data.remote.dto.RawGlobalAlbumItemDto
import com.manishraj.saavnmusic.data.remote.dto.RawGlobalArtistItemDto
import com.manishraj.saavnmusic.data.remote.dto.RawGlobalPlaylistItemDto
import com.manishraj.saavnmusic.data.remote.dto.RawGlobalSearchDto
import com.manishraj.saavnmusic.data.remote.dto.RawGlobalSongItemDto
import com.manishraj.saavnmusic.data.remote.dto.RawLyricsDto
import com.manishraj.saavnmusic.data.remote.dto.RawPlaylistDto
import com.manishraj.saavnmusic.data.remote.dto.RawSongDto
import com.manishraj.saavnmusic.domain.Album
import com.manishraj.saavnmusic.domain.Artist
import com.manishraj.saavnmusic.domain.GlobalSearch
import com.manishraj.saavnmusic.domain.HomeContent
import com.manishraj.saavnmusic.domain.Playlist
import com.manishraj.saavnmusic.domain.Song
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Raw DTO -> domain mappers: the in-app port of the jiosaavn-api
 * repository's `create*Payload()` helpers (UPSTREAM_SPEC §8.4), plus the
 * HTML-entity unescaping upstream payloads need (confirmed live:
 * titles arrive with `&quot;` etc.) and the browse-modules shape
 * classification (UPSTREAM_VALIDATION §4: entity `type` is unreliable,
 * branch on fields present).
 */

private val NAMED_ENTITIES =
    mapOf(
        "amp" to "&",
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "apos" to "'",
        "nbsp" to " ",
    )

/** Unescapes HTML entities (named + decimal/hex numeric) for display. */
fun unescapeHtml(value: String?): String {
    if (value.isNullOrEmpty() || !value.contains('&')) return value.orEmpty()
    val regex = Regex("&(#x[0-9a-fA-F]+|#\\d+|[a-zA-Z]+);")
    return regex.replace(value) { match ->
        val entity = match.groupValues[1]
        when {
            entity.startsWith("#x") || entity.startsWith("#X") ->
                entity.drop(2).toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: match.value
            entity.startsWith("#") ->
                entity.drop(1).toIntOrNull()?.let { String(Character.toChars(it)) } ?: match.value
            else -> NAMED_ENTITIES[entity] ?: match.value
        }
    }
}

private fun RawArtistMapGroupDto?.primaryNames(): String =
    this
        ?.primary
        ?.mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } }
        ?.joinToString(", ") { unescapeHtml(it) }
        .orEmpty()

private fun RawArtistMapGroupDto?.allNames(): String =
    this
        ?.artists
        ?.mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } }
        ?.joinToString(", ") { unescapeHtml(it) }
        .orEmpty()

fun RawArtistMapDto.toDomainArtist(): Artist =
    Artist(
        id = id.orEmpty(),
        name = unescapeHtml(name),
        imageUrl = MediaUrlFactory.bestImage(image),
    )

fun RawSongDto.toDomain(): Song {
    val info = moreInfo
    val ladder = MediaUrlFactory.downloadLadder(info?.encryptedMediaUrl, info?.is320kbps == "true")
    val artistNames = info?.artistMap.primaryNames().ifBlank { info?.artistMap.allNames() }
    return Song(
        id = id.orEmpty(),
        name = unescapeHtml(title),
        artist = artistNames.orEmpty(),
        album = info?.album?.let { unescapeHtml(it) },
        imageUrl = MediaUrlFactory.bestImage(image),
        durationSec = info?.duration?.toLongOrNull(),
        streamUrl =
            ladder.firstOrNull { it.first == "160kbps" }?.second
                ?: ladder.firstOrNull { it.first == "96kbps" }?.second
                ?: ladder.lastOrNull()?.second,
        downloadUrls = ladder,
        // has_lyrics is accurate in search/browse payloads. song.getDetails
        // reports "false" even for songs WITH lyrics (UPSTREAM_VALIDATION
        // §3) - the lyrics call itself is the only reliable test there.
        hasLyrics = info?.hasLyrics == "true",
        year = year,
        language = language.orEmpty(),
    )
}

fun RawAlbumDto.toDomain(): Album =
    Album(
        id = id.orEmpty(),
        name = unescapeHtml(title),
        artist =
            moreInfo?.artistMap
                .primaryNames()
                .ifBlank { moreInfo?.artistMap.allNames() }
                .ifBlank { unescapeHtml(moreInfo?.music) },
        imageUrl = MediaUrlFactory.bestImage(image),
        year = year,
        songCount = (listCount ?: moreInfo?.songCount)?.toIntOrNull(),
        songs = list.map { it.toDomain() },
        description = headerDesc?.let { unescapeHtml(it) },
    )

fun RawPlaylistDto.toDomain(): Playlist =
    Playlist(
        id = id.orEmpty(),
        name = unescapeHtml(title),
        imageUrl = MediaUrlFactory.bestImage(image),
        // list_count (String) is the TRUE total; `list` is only the
        // requested page (UPSTREAM_VALIDATION §5).
        songCount = (listCount ?: moreInfo?.songCount)?.toIntOrNull(),
        songs = list.map { it.toDomain() },
        description = (headerDesc ?: description)?.let { unescapeHtml(it) },
    )

/** Artist bio arrives as a JSON-encoded string of [{text, title, sequence}]; decode defensively. */
private fun parseBio(
    raw: String?,
    json: Json,
): String? {
    if (raw.isNullOrBlank()) return null
    val trimmed = raw.trim()
    if (!trimmed.startsWith("[")) return unescapeHtml(trimmed)
    return try {
        val segments = json.decodeFromString(ListSerializer(BioSegmentDto.serializer()), trimmed)
        segments
            .mapNotNull { it.text?.takeIf { t -> t.isNotBlank() } }
            .joinToString("\n\n") { unescapeHtml(it) }
            .ifBlank { null }
    } catch (e: Exception) {
        // A malformed bio must not kill the artist page (spec §4.8).
        null
    }
}

@kotlinx.serialization.Serializable
private data class BioSegmentDto(
    val text: String? = null,
    val title: String? = null,
    val sequence: Int? = null,
)

fun RawArtistPageDto.toDomain(json: Json): Artist =
    Artist(
        id = artistId ?: id.orEmpty(),
        name = unescapeHtml(name),
        imageUrl = MediaUrlFactory.bestImage(image),
        followers = followerCount?.toLongOrNull(),
        bio = parseBio(bio, json),
        topSongs = topSongs.map { it.toDomain() },
        topAlbums = topAlbums.map { it.toDomain() },
    )

// ---- Global search items: lightweight, not playable ----

fun RawGlobalSongItemDto.toDomain(): Song =
    Song(
        id = id.orEmpty(),
        name = unescapeHtml(title),
        artist = unescapeHtml(moreInfo?.primaryArtists ?: moreInfo?.singers),
        album = moreInfo?.album?.let { unescapeHtml(it) },
        imageUrl = MediaUrlFactory.bestImage(image),
        durationSec = null,
        streamUrl = null,
    )

fun RawGlobalAlbumItemDto.toDomain(): Album =
    Album(
        id = id.orEmpty(),
        name = unescapeHtml(title),
        artist = unescapeHtml(moreInfo?.music),
        imageUrl = MediaUrlFactory.bestImage(image),
        year = moreInfo?.year,
        songCount = null,
        description = description?.let { unescapeHtml(it) },
    )

fun RawGlobalArtistItemDto.toDomain(): Artist =
    Artist(
        id = id.orEmpty(),
        name = unescapeHtml(title),
        imageUrl = MediaUrlFactory.bestImage(image),
    )

fun RawGlobalPlaylistItemDto.toDomain(): Playlist =
    Playlist(
        id = id.orEmpty(),
        name = unescapeHtml(title),
        imageUrl = MediaUrlFactory.bestImage(image),
        songCount = null,
        description = description?.let { unescapeHtml(it) },
    )

fun RawGlobalSearchDto.toDomain(): GlobalSearch =
    GlobalSearch(
        topSongs = topquery?.data.orEmpty().map { it.toDomain() },
        songs = songs?.data.orEmpty().map { it.toDomain() },
        albums = albums?.data.orEmpty().map { it.toDomain() },
        artists = artists?.data.orEmpty().map { it.toDomain() },
        playlists = playlists?.data.orEmpty().map { it.toDomain() },
    )

/** Lyrics cleaning per jiosaavn-dl + UPSTREAM_VALIDATION §3: only `<br>` separators are used. */
fun RawLyricsDto.cleanedLyrics(): String? = lyrics?.replace("<br>", "\n")?.trim()?.ifBlank { null }

// ---- Browse modules -> Home ----

private enum class BrowseShape { SONG, ALBUM, PLAYLIST }

/**
 * Shape classification (UPSTREAM_VALIDATION §4): a playable stream URL
 * makes it a song; an artist map + song count makes it an album;
 * everything else in these sections is playlist-shaped.
 */
private fun classifyBrowseItem(obj: JsonObject): BrowseShape {
    val moreInfo = obj["more_info"] as? JsonObject
    val encrypted = (moreInfo?.get("encrypted_media_url") as? JsonPrimitive)?.contentOrNull
    if (!encrypted.isNullOrBlank()) return BrowseShape.SONG
    val hasArtistMap = moreInfo?.containsKey("artistMap") == true
    val hasSongCount = (moreInfo?.get("song_count") as? JsonPrimitive)?.contentOrNull != null
    return if (hasArtistMap && hasSongCount) BrowseShape.ALBUM else BrowseShape.PLAYLIST
}

fun RawBrowseModulesDto.toHomeContent(json: Json): HomeContent {
    val mixed = newTrending + newAlbums

    fun <T> decode(
        serializer: kotlinx.serialization.KSerializer<T>,
        obj: JsonObject,
    ): T? = runCatching { json.decodeFromJsonElement(serializer, obj) }.getOrNull()

    val rawSongs =
        mixed
            .filter { classifyBrowseItem(it) == BrowseShape.SONG }
            .mapNotNull { decode(RawSongDto.serializer(), it) }
            .distinctBy { it.id }
    val albums =
        mixed
            .filter { classifyBrowseItem(it) == BrowseShape.ALBUM }
            .mapNotNull { decode(RawAlbumDto.serializer(), it)?.toDomain() }
            .distinctBy { it.id }
    val playlists =
        (
            topPlaylists + charts +
                mixed.filter { classifyBrowseItem(it) == BrowseShape.PLAYLIST }
        ).mapNotNull { decode(RawPlaylistDto.serializer(), it)?.toDomain() }
            .distinctBy { it.id }
    // The artists rail is derived from the trending songs' artist maps -
    // browse modules carry no dedicated artist section.
    val artists =
        rawSongs
            .flatMap { it.moreInfo?.artistMap?.primary.orEmpty() }
            .filter { !it.id.isNullOrBlank() }
            .distinctBy { it.id }
            .take(15)
            .map { it.toDomainArtist() }
    return HomeContent(
        trendingSongs = rawSongs.map { it.toDomain() },
        albums = albums,
        playlists = playlists,
        artists = artists,
    )
}
