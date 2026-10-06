package com.manishraj.saavnmusic.data.repository

import com.manishraj.saavnmusic.data.local.DownloadEntity
import com.manishraj.saavnmusic.data.local.FavoriteEntity
import com.manishraj.saavnmusic.data.local.HistoryEntity
import com.manishraj.saavnmusic.data.local.LibraryDao
import com.manishraj.saavnmusic.data.local.LocalPlaylistEntity
import com.manishraj.saavnmusic.data.local.LocalPlaylistSongEntity
import com.manishraj.saavnmusic.data.local.RecentSearchEntity
import com.manishraj.saavnmusic.data.remote.JioSaavnClient
import com.manishraj.saavnmusic.data.remote.cleanedLyrics
import com.manishraj.saavnmusic.data.remote.dto.RawAlbumDto
import com.manishraj.saavnmusic.data.remote.dto.RawArtistMapDto
import com.manishraj.saavnmusic.data.remote.dto.RawArtistPageDto
import com.manishraj.saavnmusic.data.remote.dto.RawBrowseModulesDto
import com.manishraj.saavnmusic.data.remote.dto.RawGlobalSearchDto
import com.manishraj.saavnmusic.data.remote.dto.RawLyricsDto
import com.manishraj.saavnmusic.data.remote.dto.RawPagedDto
import com.manishraj.saavnmusic.data.remote.dto.RawPlaylistDto
import com.manishraj.saavnmusic.data.remote.dto.RawSongDetailsDto
import com.manishraj.saavnmusic.data.remote.dto.RawSongDto
import com.manishraj.saavnmusic.data.remote.dto.RawStationCreatedDto
import com.manishraj.saavnmusic.data.remote.dto.RawStationEntryDto
import com.manishraj.saavnmusic.data.remote.toDomain
import com.manishraj.saavnmusic.data.remote.toDomainArtist
import com.manishraj.saavnmusic.data.remote.toHomeContent
import com.manishraj.saavnmusic.data.settings.AppSettings
import com.manishraj.saavnmusic.data.settings.SettingsRepository
import com.manishraj.saavnmusic.domain.Album
import com.manishraj.saavnmusic.domain.Artist
import com.manishraj.saavnmusic.domain.DownloadInfo
import com.manishraj.saavnmusic.domain.GlobalSearch
import com.manishraj.saavnmusic.domain.HomeContent
import com.manishraj.saavnmusic.domain.LocalPlaylist
import com.manishraj.saavnmusic.domain.Playlist
import com.manishraj.saavnmusic.domain.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single source of truth. Remote data comes from JioSaavn's upstream
 * `api.php` directly — the calls are built exactly the way the
 * jiosaavn-api repository builds them (docs/sdlc/UPSTREAM_SPEC.md,
 * validated live in UPSTREAM_VALIDATION.md). There is no hosted wrapper
 * instance anywhere in this path. Local data (favorites, downloads,
 * history, playlists) is Room-only: no accounts, nothing leaves the
 * device.
 */
@Singleton
class MusicRepository
    @Inject
    constructor(
        private val client: JioSaavnClient,
        private val dao: LibraryDao,
        private val settingsRepository: SettingsRepository,
        private val json: Json,
    ) {
        // Settings are re-exposed through the repository so features can
        // depend on :core:data alone (Now in Android settings pattern).
        val settings: Flow<AppSettings> = settingsRepository.settings

        suspend fun updateSettings(transform: (AppSettings) -> AppSettings) = settingsRepository.update(transform)

        private suspend fun endpoint(): String = settingsRepository.settings.first().apiEndpoint

        // ---- Search ----

        suspend fun searchAll(q: String): GlobalSearch {
            val obj = client.callObject(endpoint(), "autocomplete.get", mapOf("query" to q))
            return json.decodeFromJsonElement<RawGlobalSearchDto>(obj).toDomain()
        }

        suspend fun searchSongs(
            q: String,
            page: Int = 0,
            limit: Int = 20,
        ): List<Song> {
            val obj =
                client.callObject(
                    endpoint(),
                    "search.getResults",
                    mapOf("q" to q, "p" to page.toString(), "n" to limit.toString()),
                )
            // Paging is driven by `total` + accumulated counts only; the
            // upstream `start` field is unreliable (observed -4 at p=0).
            return json.decodeFromJsonElement<RawPagedDto<RawSongDto>>(obj).results.map { it.toDomain() }
        }

        suspend fun searchAlbums(
            q: String,
            page: Int = 0,
            limit: Int = 20,
        ): List<Album> {
            val obj =
                client.callObject(
                    endpoint(),
                    "search.getAlbumResults",
                    mapOf("q" to q, "p" to page.toString(), "n" to limit.toString()),
                )
            return json.decodeFromJsonElement<RawPagedDto<RawAlbumDto>>(obj).results.map { it.toDomain() }
        }

        suspend fun searchArtists(
            q: String,
            page: Int = 0,
            limit: Int = 20,
        ): List<Artist> {
            val obj =
                client.callObject(
                    endpoint(),
                    "search.getArtistResults",
                    mapOf("q" to q, "p" to page.toString(), "n" to limit.toString()),
                )
            return json
                .decodeFromJsonElement<RawPagedDto<RawArtistMapDto>>(obj)
                .results
                .map { it.toDomainArtist() }
        }

        suspend fun searchPlaylists(
            q: String,
            page: Int = 0,
            limit: Int = 20,
        ): List<Playlist> {
            val obj =
                client.callObject(
                    endpoint(),
                    "search.getPlaylistResults",
                    mapOf("q" to q, "p" to page.toString(), "n" to limit.toString()),
                )
            return json.decodeFromJsonElement<RawPagedDto<RawPlaylistDto>>(obj).results.map { it.toDomain() }
        }

        // ---- Details ----

        suspend fun song(id: String): Song = songs(listOf(id)).firstOrNull() ?: throw IllegalStateException("Song not found")

        /** Batch song resolution in one call (`pids` is comma-separated upstream). */
        suspend fun songs(ids: List<String>): List<Song> {
            if (ids.isEmpty()) return emptyList()
            val obj = client.callObject(endpoint(), "song.getDetails", mapOf("pids" to ids.joinToString(",")))
            return json.decodeFromJsonElement<RawSongDetailsDto>(obj).songs.map { it.toDomain() }
        }

        suspend fun album(id: String): Album {
            val obj = client.callObject(endpoint(), "content.getAlbumDetails", mapOf("albumid" to id))
            return json.decodeFromJsonElement<RawAlbumDto>(obj).toDomain()
        }

        suspend fun playlist(id: String): Playlist {
            // Page until the accumulated list reaches list_count (the true
            // total) or a short page returns - never trust a single page.
            val pageSize = 100
            var page = 0
            var first: Playlist? = null
            val all = mutableListOf<Song>()
            while (true) {
                val obj =
                    client.callObject(
                        endpoint(),
                        "playlist.getDetails",
                        mapOf("listid" to id, "n" to pageSize.toString(), "p" to page.toString()),
                    )
                val dto = json.decodeFromJsonElement<RawPlaylistDto>(obj)
                if (first == null) first = dto.toDomain()
                all += dto.list.map { it.toDomain() }
                val total = dto.listCount?.toIntOrNull() ?: all.size
                if (all.size >= total || dto.list.size < pageSize) break
                page++
            }
            val base = first ?: throw IllegalStateException("Playlist not found")
            return base.copy(songs = all, songCount = base.songCount ?: all.size)
        }

        suspend fun artist(id: String): Artist {
            val obj =
                client.callObject(
                    endpoint(),
                    "artist.getArtistPageDetails",
                    mapOf(
                        "artistId" to id,
                        "n_song" to "20",
                        "n_album" to "10",
                        "page" to "0",
                        "sort_order" to "desc",
                        "category" to "popularity",
                    ),
                )
            return json.decodeFromJsonElement<RawArtistPageDto>(obj).toDomain(json)
        }

        // ---- Lyrics ----

        /**
         * Lyrics for a song, or null when the song has none.
         *
         * VALIDATION-CORRECTED RULE (UPSTREAM_VALIDATION §3): the
         * `lyrics_id` parameter is the SONG'S OWN ID — never
         * `more_info.lyrics_id` (empty in search payloads, absent in
         * details payloads). The call is attempted on demand; any body
         * without a `lyrics` key (upstream answers errors as HTTP 200
         * bodies) means "no lyrics".
         */
        suspend fun lyrics(songId: String): String? =
            try {
                val obj = client.callObject(endpoint(), "lyrics.getLyrics", mapOf("lyrics_id" to songId))
                json.decodeFromJsonElement<RawLyricsDto>(obj).cleanedLyrics()
            } catch (e: Exception) {
                null
            }

        // ---- Home ----

        private var homeCache: HomeContent? = null
        private var homeCacheAtMs: Long = 0

        suspend fun home(): HomeContent {
            val cached = homeCache
            if (cached != null && System.currentTimeMillis() - homeCacheAtMs < HOME_CACHE_TTL_MS) {
                return cached
            }
            val obj = client.callObject(endpoint(), "content.getBrowseModules")
            val content = json.decodeFromJsonElement<RawBrowseModulesDto>(obj).toHomeContent(json)
            homeCache = content
            homeCacheAtMs = System.currentTimeMillis()
            return content
        }

        // ---- Suggestions (best-effort; upstream step 2 is currently broken) ----

        private val stationIds = ConcurrentHashMap<String, String>()

        /**
         * Radio-style suggestions seeded by a song. KNOWN UPSTREAM STATE
         * (UPSTREAM_VALIDATION §6): `webradio.createEntityStation` works,
         * but `webradio.getSong` currently answers an error body for every
         * station, so this returns an empty list in practice. It is a
         * silent no-op by design — playback must never depend on it.
         */
        suspend fun suggestions(
            songId: String,
            limit: Int = 10,
        ): List<Song> {
            return try {
                val ep = endpoint()
                var stationId = stationIds[songId]
                if (stationId == null) {
                    val created =
                        client.callObject(
                            ep,
                            "webradio.createEntityStation",
                            mapOf(
                                "entity_id" to "[\"$songId\"]",
                                "entity_type" to "queue",
                            ),
                            ctx = JioSaavnClient.ANDROID_CTX,
                        )
                    stationId =
                        json.decodeFromJsonElement<RawStationCreatedDto>(created).stationid
                            ?: return emptyList()
                    stationIds[songId] = stationId
                }
                val obj =
                    client.callObject(
                        ep,
                        "webradio.getSong",
                        mapOf("stationid" to stationId, "k" to limit.toString()),
                        ctx = JioSaavnClient.ANDROID_CTX,
                    )
                // Success shape: {"stationid": ..., "0": {"song": {...}}, ...}
                // Current upstream shape: {"stationid": ..., "error": "..."} -> no numeric keys -> empty.
                obj.entries
                    .filter { (key, _) -> key.toIntOrNull() != null }
                    .sortedBy { (key, _) -> key.toInt() }
                    .mapNotNull { (_, value) ->
                        runCatching {
                            json.decodeFromJsonElement<RawStationEntryDto>(value).song?.toDomain()
                        }.getOrNull()
                    }
                    .take(limit)
            } catch (e: Exception) {
                emptyList()
            }
        }

        // ---- Local library (no accounts - everything on-device) ----

        // Flows are exposed as domain models so feature modules never see
        // Room entities (Now in Android rule: repositories speak domain).
        val favorites: Flow<List<Song>> =
            dao.favorites().map { list -> list.map { it.toSong() } }
        val downloads: Flow<List<DownloadInfo>> =
            dao.downloads().map { list -> list.map { it.toDownloadInfo() } }
        val history: Flow<List<Song>> =
            dao.history().map { list -> list.map { it.toSong() } }
        val recentSearches: Flow<List<String>> =
            dao.recentSearches().map { list -> list.map { it.query } }
        val localPlaylists: Flow<List<LocalPlaylist>> =
            dao.playlists().map { list -> list.map { LocalPlaylist(it.id, it.name) } }

        fun isFavorite(id: String) = dao.isFavorite(id)

        fun playlistSongs(id: Long): Flow<List<Song>> = dao.playlistSongs(id).map { list -> list.map { it.toSong() } }

        suspend fun toggleFavorite(
            s: Song,
            fav: Boolean,
        ) {
            if (fav) {
                dao.removeFavorite(s.id)
            } else {
                dao.addFavorite(FavoriteEntity(s.id, s.name, s.artist, s.album, s.imageUrl, s.durationSec, s.streamUrl))
            }
        }

        /** Restores a favorite snapshot (used by the Library Undo action). */
        suspend fun addFavorite(s: Song) {
            dao.addFavorite(FavoriteEntity(s.id, s.name, s.artist, s.album, s.imageUrl, s.durationSec, s.streamUrl))
        }

        suspend fun recordPlay(s: Song) = dao.addHistory(HistoryEntity(s.id, s.name, s.artist, s.imageUrl, s.streamUrl))

        suspend fun addRecentSearch(q: String) {
            if (q.isNotBlank()) dao.addRecentSearch(RecentSearchEntity(q.trim()))
        }

        suspend fun removeRecentSearch(q: String) = dao.removeRecentSearch(q)

        suspend fun clearHistory() = dao.clearHistory()

        suspend fun clearRecentSearches() = dao.clearRecentSearches()

        suspend fun createPlaylist(name: String) = dao.createPlaylist(LocalPlaylistEntity(name = name))

        suspend fun deletePlaylist(id: Long) = dao.deletePlaylist(id)

        suspend fun addToPlaylist(
            pid: Long,
            s: Song,
            pos: Int = 0,
        ) = dao.addToPlaylist(LocalPlaylistSongEntity(pid, s.id, s.name, s.artist, s.imageUrl, s.streamUrl, pos))

        suspend fun registerDownload(
            s: Song,
            path: String,
            quality: String,
            size: Long,
        ) = dao.upsertDownload(DownloadEntity(s.id, s.name, s.artist, s.album, s.imageUrl, path, quality, size))

        /** Re-registers a download row (used by the Library Undo action after a delete). */
        suspend fun registerDownload(info: DownloadInfo) =
            dao.upsertDownload(
                DownloadEntity(info.songId, info.name, info.artist, info.album, info.imageUrl, info.filePath, info.quality, info.sizeBytes, info.status, info.progress),
            )

        suspend fun deleteDownload(id: String) {
            // Deleting a download removes the file too, not just the row.
            val row = dao.download(id)
            dao.deleteDownload(id)
            if (row != null) {
                runCatching { java.io.File(row.filePath).delete() }
            }
        }

        private companion object {
            const val HOME_CACHE_TTL_MS = 5 * 60 * 1000L
        }
    }

// Entity -> domain mappers. Snapshots stored in Room carry a single
// stream URL (no quality ladder); playback falls back to it.
fun FavoriteEntity.toSong(): Song = Song(songId, name, artist, album, imageUrl, durationSec, streamUrl)

fun HistoryEntity.toSong(): Song = Song(songId, name, artist, null, imageUrl, null, streamUrl)

fun LocalPlaylistSongEntity.toSong(): Song = Song(songId, name, artist, null, imageUrl, null, streamUrl)

fun DownloadEntity.toDownloadInfo(): DownloadInfo =
    DownloadInfo(songId, name, artist, album, imageUrl, filePath, quality, sizeBytes, status, progress)
