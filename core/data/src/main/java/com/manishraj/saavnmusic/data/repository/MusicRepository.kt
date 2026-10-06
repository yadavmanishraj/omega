package com.manishraj.saavnmusic.data.repository
import com.manishraj.saavnmusic.data.local.*
import com.manishraj.saavnmusic.data.remote.SaavnApi
import com.manishraj.saavnmusic.data.settings.AppSettings
import com.manishraj.saavnmusic.data.settings.SettingsRepository
import com.manishraj.saavnmusic.data.remote.dto.*
import com.manishraj.saavnmusic.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.*

@Singleton class MusicRepository
    @Inject
    constructor(
        private val api: SaavnApi,
        private val dao: LibraryDao,
        private val settingsRepository: SettingsRepository,
    ) {
        // Settings are re-exposed through the repository so features can
        // depend on :core:data alone (Now in Android settings pattern).
        val settings: Flow<AppSettings> = settingsRepository.settings

        suspend fun updateSettings(transform: (AppSettings) -> AppSettings) = settingsRepository.update(transform)

        suspend fun searchAll(q: String) = api.searchAll(q).data ?: GlobalSearchDto()

        suspend fun searchSongs(q: String) =
            api
                .searchSongs(q)
                .data
                ?.results
                .orEmpty()
                .map { it.toDomain() }

        suspend fun searchAlbums(q: String) =
            api
                .searchAlbums(q)
                .data
                ?.results
                .orEmpty()
                .map { it.toDomain() }

        suspend fun searchArtists(q: String) =
            api
                .searchArtists(q)
                .data
                ?.results
                .orEmpty()
                .map { it.toDomain() }

        suspend fun searchPlaylists(q: String) =
            api
                .searchPlaylists(q)
                .data
                ?.results
                .orEmpty()
                .map { it.toDomain() }

        suspend fun song(
            id: String,
            lyrics: Boolean = false,
        ): Song =
            api
                .songById(id, lyrics)
                .data
                ?.firstOrNull()
                ?.toDomain() ?: throw IllegalStateException("Song not found")

        suspend fun songWithLyrics(id: String) = api.songById(id, true).data?.firstOrNull()

        suspend fun suggestions(id: String) =
            api
                .suggestions(id)
                .data
                .orEmpty()
                .map { it.toDomain() }

        suspend fun album(id: String) = api.albumById(id).data?.toDomain() ?: throw IllegalStateException("Album not found")

        suspend fun playlist(id: String) = api.playlistById(id).data?.toDomain() ?: throw IllegalStateException("Playlist not found")

        suspend fun artist(id: String) = api.artistById(id).data?.toDomain() ?: throw IllegalStateException("Artist not found")

        // Local library (no accounts - everything on-device). Flows are
        // exposed as domain models so feature modules never see Room
        // entities (Now in Android rule: repositories speak domain types).
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

        fun playlistSongs(id: Long): Flow<List<Song>> =
            dao.playlistSongs(id).map { list -> list.map { it.toSong() } }

        suspend fun toggleFavorite(
            s: Song,
            fav: Boolean,
        ) {
            if (fav) {
                dao.removeFavorite(
                    s.id,
                )
            } else {
                dao.addFavorite(FavoriteEntity(s.id, s.name, s.artist, s.album, s.imageUrl, s.durationSec, s.streamUrl))
            }
        }

        suspend fun recordPlay(s: Song) = dao.addHistory(HistoryEntity(s.id, s.name, s.artist, s.imageUrl, s.streamUrl))

        suspend fun addRecentSearch(q: String) {
            if (q.isNotBlank()) dao.addRecentSearch(RecentSearchEntity(q.trim()))
        }

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

        suspend fun deleteDownload(id: String) = dao.deleteDownload(id)
    }

fun SongDto.toDomain(): Song =
    Song(
        id,
        name,
        artists.primary
            .joinToString(", ") {
                it.name.orEmpty()
            }.ifBlank {
                artists.all.joinToString(", ") { it.name.orEmpty() }
            },
        album?.name,
        image.bestUrl(),
        duration,
        downloadUrl.urlForQuality("320kbps"),
        downloadUrl.map {
            it.quality to
                it.url
        },
        hasLyrics,
        year,
        language,
    )

fun AlbumDto.toDomain(): Album =
    Album(
        id,
        name,
        artists.primary.joinToString(", ") {
            it.name.orEmpty()
        },
        image.bestUrl(),
        year,
        songCount,
        songs.map { it.toDomain() },
        description,
    )

fun PlaylistDto.toDomain(): Playlist = Playlist(id, name, image.bestUrl(), songCount, songs.map { it.toDomain() }, description)

fun ArtistDetailDto.toDomain(): Artist =
    Artist(
        id,
        name,
        image.bestUrl(),
        followerCount,
        bio.firstOrNull()?.text,
        topSongs.map {
            it.toDomain()
        },
        topAlbums.map { it.toDomain() },
    )

// Entity -> domain mappers. Snapshots stored in Room carry a single
// stream URL (no quality ladder); playback falls back to it.
fun FavoriteEntity.toSong(): Song = Song(songId, name, artist, album, imageUrl, durationSec, streamUrl)

fun HistoryEntity.toSong(): Song = Song(songId, name, artist, null, imageUrl, null, streamUrl)

fun LocalPlaylistSongEntity.toSong(): Song = Song(songId, name, artist, null, imageUrl, null, streamUrl)

fun DownloadEntity.toDownloadInfo(): DownloadInfo =
    DownloadInfo(songId, name, artist, album, imageUrl, filePath, quality, sizeBytes, status, progress)
