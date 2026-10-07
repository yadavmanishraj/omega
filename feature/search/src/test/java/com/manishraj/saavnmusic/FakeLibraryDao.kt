package com.manishraj.saavnmusic

import com.manishraj.saavnmusic.data.local.DownloadEntity
import com.manishraj.saavnmusic.data.local.FavoriteEntity
import com.manishraj.saavnmusic.data.local.HistoryEntity
import com.manishraj.saavnmusic.data.local.LibraryDao
import com.manishraj.saavnmusic.data.local.LocalPlaylistEntity
import com.manishraj.saavnmusic.data.local.LocalPlaylistRow
import com.manishraj.saavnmusic.data.local.LocalPlaylistSongEntity
import com.manishraj.saavnmusic.data.local.RecentSearchEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * In-memory [LibraryDao] for JVM unit tests. Mirrors the Room queries'
 * observable semantics:
 * - inserts use REPLACE on the primary key (a re-insert replaces the
 *   row and, like Room's delete+insert, moves it to "most recent");
 * - list queries order most-recent first (addedAt/playedAt/searchedAt/
 *   createdAt DESC), with history capped at 50 and searches at 10;
 * - playlist songs come back ordered by their stored position.
 */
class FakeLibraryDao : LibraryDao {
    private val favoritesState = MutableStateFlow<List<FavoriteEntity>>(emptyList())
    private val downloadsState = MutableStateFlow<List<DownloadEntity>>(emptyList())
    private val historyState = MutableStateFlow<List<HistoryEntity>>(emptyList())
    private val searchesState = MutableStateFlow<List<RecentSearchEntity>>(emptyList())
    private val playlistsState = MutableStateFlow<List<LocalPlaylistEntity>>(emptyList())
    private val playlistSongsState = MutableStateFlow<List<LocalPlaylistSongEntity>>(emptyList())
    private var nextPlaylistId = 1L

    override fun favorites(): Flow<List<FavoriteEntity>> = favoritesState.map { rows -> rows.reversed() }

    override fun isFavorite(id: String): Flow<Boolean> = favoritesState.map { rows -> rows.any { it.songId == id } }

    override suspend fun addFavorite(e: FavoriteEntity) {
        favoritesState.value = favoritesState.value.filterNot { it.songId == e.songId } + e
    }

    override suspend fun removeFavorite(id: String) {
        favoritesState.value = favoritesState.value.filterNot { it.songId == id }
    }

    override fun downloads(): Flow<List<DownloadEntity>> = downloadsState.map { rows -> rows.reversed() }

    override suspend fun download(id: String): DownloadEntity? = downloadsState.value.firstOrNull { it.songId == id }

    override suspend fun upsertDownload(e: DownloadEntity) {
        downloadsState.value = downloadsState.value.filterNot { it.songId == e.songId } + e
    }

    override suspend fun updateDownloadProgress(
        id: String,
        progress: Int,
        sizeBytes: Long,
    ) {
        downloadsState.value =
            downloadsState.value.map { row ->
                if (row.songId == id) row.copy(progress = progress, sizeBytes = sizeBytes) else row
            }
    }

    override suspend fun updateDownloadStatus(
        id: String,
        status: String,
    ) {
        downloadsState.value =
            downloadsState.value.map { row ->
                if (row.songId == id) row.copy(status = status) else row
            }
    }

    override suspend fun updateDownloadFailed(
        id: String,
        message: String?,
    ) {
        downloadsState.value =
            downloadsState.value.map { row ->
                if (row.songId == id) row.copy(status = "FAILED", errorMessage = message) else row
            }
    }

    override suspend fun deleteDownload(id: String) {
        downloadsState.value = downloadsState.value.filterNot { it.songId == id }
    }

    override fun history(): Flow<List<HistoryEntity>> = historyState.map { rows -> rows.reversed().take(50) }

    override suspend fun addHistory(e: HistoryEntity) {
        historyState.value = historyState.value.filterNot { it.songId == e.songId } + e
    }

    override suspend fun clearHistory() {
        historyState.value = emptyList()
    }

    override fun recentSearches(): Flow<List<RecentSearchEntity>> = searchesState.map { rows -> rows.reversed().take(10) }

    override suspend fun addRecentSearch(e: RecentSearchEntity) {
        searchesState.value = searchesState.value.filterNot { it.query == e.query } + e
    }

    override suspend fun removeRecentSearch(q: String) {
        searchesState.value = searchesState.value.filterNot { it.query == q }
    }

    override suspend fun clearRecentSearches() {
        searchesState.value = emptyList()
    }

    override fun playlists(): Flow<List<LocalPlaylistRow>> =
        combine(playlistsState, playlistSongsState) { playlists, songs ->
            playlists.reversed().map { playlist ->
                LocalPlaylistRow(
                    id = playlist.id,
                    name = playlist.name,
                    songCount = songs.count { it.playlistId == playlist.id },
                )
            }
        }

    override suspend fun createPlaylist(e: LocalPlaylistEntity): Long {
        val id = nextPlaylistId++
        playlistsState.value = playlistsState.value + e.copy(id = id)
        return id
    }

    override suspend fun deletePlaylist(id: Long) {
        playlistsState.value = playlistsState.value.filterNot { it.id == id }
    }

    override suspend fun addToPlaylist(e: LocalPlaylistSongEntity) {
        playlistSongsState.value =
            playlistSongsState.value.filterNot {
                it.playlistId == e.playlistId && it.songId == e.songId
            } + e
    }

    override suspend fun playlistSong(
        pid: Long,
        sid: String,
    ): LocalPlaylistSongEntity? = playlistSongsState.value.firstOrNull { it.playlistId == pid && it.songId == sid }

    override suspend fun removeFromPlaylist(
        pid: Long,
        sid: String,
    ) {
        playlistSongsState.value =
            playlistSongsState.value.filterNot { it.playlistId == pid && it.songId == sid }
    }

    override suspend fun playlistSongCount(id: Long): Int = playlistSongsState.value.count { it.playlistId == id }

    override fun playlistSongs(id: Long): Flow<List<LocalPlaylistSongEntity>> =
        playlistSongsState.map { rows ->
            rows.filter { it.playlistId == id }.sortedBy { it.position }
        }
}
