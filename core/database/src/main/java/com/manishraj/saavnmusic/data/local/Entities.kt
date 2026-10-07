package com.manishraj.saavnmusic.data.local
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: String,
    val name: String,
    val artist: String,
    val album: String?,
    val imageUrl: String?,
    val durationSec: Long?,
    val streamUrl: String?,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val songId: String,
    val name: String,
    val artist: String,
    val album: String?,
    val imageUrl: String?,
    val filePath: String,
    val quality: String,
    val sizeBytes: Long = 0,
    val status: String = "COMPLETED",
    val progress: Int = 100,
    val addedAt: Long = System.currentTimeMillis(),
    // Added in schema v2 (kept last: the 1->2 migration appends the
    // column with ALTER TABLE ... ADD COLUMN, matching this position).
    val errorMessage: String? = null,
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val songId: String,
    val name: String,
    val artist: String,
    val imageUrl: String?,
    val streamUrl: String?,
    val playedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "local_playlists")
data class LocalPlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "local_playlist_songs", primaryKeys = ["playlistId", "songId"])
data class LocalPlaylistSongEntity(
    val playlistId: Long,
    val songId: String,
    val name: String,
    val artist: String,
    val imageUrl: String?,
    val streamUrl: String?,
    val position: Int = 0,
)

/** Projection row for the playlist list (playlist + its song count). */
data class LocalPlaylistRow(
    val id: Long,
    val name: String,
    val songCount: Int,
)

@Dao interface LibraryDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun favorites(): kotlinx.coroutines.flow.Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId=:id)")
    fun isFavorite(id: String): kotlinx.coroutines.flow.Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(e: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId=:id")
    suspend fun removeFavorite(id: String)

    @Query("SELECT * FROM downloads ORDER BY addedAt DESC")
    fun downloads(): kotlinx.coroutines.flow.Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE songId=:id")
    suspend fun download(id: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownload(e: DownloadEntity)

    @Query("UPDATE downloads SET progress = :progress, sizeBytes = :sizeBytes WHERE songId = :id")
    suspend fun updateDownloadProgress(
        id: String,
        progress: Int,
        sizeBytes: Long,
    )

    @Query("UPDATE downloads SET status = :status WHERE songId = :id")
    suspend fun updateDownloadStatus(
        id: String,
        status: String,
    )

    @Query("UPDATE downloads SET status = 'FAILED', errorMessage = :message WHERE songId = :id")
    suspend fun updateDownloadFailed(
        id: String,
        message: String?,
    )

    @Query("DELETE FROM downloads WHERE songId=:id")
    suspend fun deleteDownload(id: String)

    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT 50")
    fun history(): kotlinx.coroutines.flow.Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addHistory(e: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    @Query("SELECT * FROM recent_searches ORDER BY searchedAt DESC LIMIT 10")
    fun recentSearches(): kotlinx.coroutines.flow.Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRecentSearch(e: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE query=:q")
    suspend fun removeRecentSearch(q: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()

    @Query(
        "SELECT p.id AS id, p.name AS name, " +
            "(SELECT COUNT(*) FROM local_playlist_songs s WHERE s.playlistId = p.id) AS songCount " +
            "FROM local_playlists p ORDER BY p.createdAt DESC",
    )
    fun playlists(): kotlinx.coroutines.flow.Flow<List<LocalPlaylistRow>>

    @Insert suspend fun createPlaylist(e: LocalPlaylistEntity): Long

    @Query("DELETE FROM local_playlists WHERE id=:id")
    suspend fun deletePlaylist(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToPlaylist(e: LocalPlaylistSongEntity)

    @Query("SELECT * FROM local_playlist_songs WHERE playlistId=:pid AND songId=:sid")
    suspend fun playlistSong(
        pid: Long,
        sid: String,
    ): LocalPlaylistSongEntity?

    @Query("DELETE FROM local_playlist_songs WHERE playlistId=:pid AND songId=:sid")
    suspend fun removeFromPlaylist(
        pid: Long,
        sid: String,
    )

    @Query("SELECT COUNT(*) FROM local_playlist_songs WHERE playlistId=:id")
    suspend fun playlistSongCount(id: Long): Int

    @Query("SELECT * FROM local_playlist_songs WHERE playlistId=:id ORDER BY position")
    fun playlistSongs(id: Long): kotlinx.coroutines.flow.Flow<List<LocalPlaylistSongEntity>>
}

@Database(
    entities = [
        FavoriteEntity::class,
        DownloadEntity::class,
        HistoryEntity::class,
        RecentSearchEntity::class,
        LocalPlaylistEntity::class,
        LocalPlaylistSongEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}

/**
 * Schema 1 -> 2: downloads gain a nullable `errorMessage` so a FAILED
 * row can say why (HTTP code, no stream, IO error). Purely additive —
 * every v1 row and table survives untouched.
 */
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE downloads ADD COLUMN errorMessage TEXT")
        }
    }
