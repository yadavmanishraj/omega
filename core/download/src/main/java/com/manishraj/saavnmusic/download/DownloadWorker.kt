package com.manishraj.saavnmusic.download
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.manishraj.saavnmusic.data.repository.MusicRepository
import com.manishraj.saavnmusic.domain.*
import dagger.assisted.*
import okhttp3.*
import java.io.File

/**
 * Downloads mirroring jiosaavn-dl: pick the requested quality from downloadUrl (fallback highest),
 * save as sanitized "Artist - Title.m4a" (album downloads: "NN. Title.m4a" inside an album folder),
 * into app-specific Music/SaavnMusic (no storage permission needed), then register in Room for offline playback.
 * NOTE: full ID3/M4A tag+artwork embedding (mutagen in jiosaavn-dl) is done via Media3/metadata sidecar JSON here; see STUDY.md.
 */
@HiltWorker class DownloadWorker
    @AssistedInject
    constructor(
        @Assisted ctx: Context,
        @Assisted params: WorkerParameters,
        private val repo: MusicRepository,
    ) : CoroutineWorker(ctx, params) {
        override suspend fun doWork(): Result {
            val id = inputData.getString("id") ?: return Result.failure()
            val quality = inputData.getString("quality") ?: "320kbps"
            return try {
                val song = repo.song(id)
                val url =
                    song.downloadUrls.firstOrNull { it.first == quality }?.second ?: song.streamUrl ?: return Result.failure()
                val dir =
                    File(
                        applicationContext.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC),
                        "SaavnMusic",
                    ).apply { mkdirs() }
                val file = File(dir, downloadFileName(song))
                OkHttpClient().newCall(Request.Builder().url(url).build()).execute().use { r ->
                    if (!r.isSuccessful) return Result.retry()
                    val body =
                        r.body ?: return Result.failure()
                    file.outputStream().use { body.byteStream().copyTo(it) }
                }
                File(
                    dir,
                    sanitizeFileName(song.name) + ".json",
                ).writeText(
                    """{"title":"${song.name}","artist":"${song.artist}","album":"${song.album.orEmpty()}","image":"${song.imageUrl.orEmpty()}"}""",
                )
                repo.registerDownload(song, file.absolutePath, quality, file.length())
                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }

        companion object {
            fun enqueue(
                wm: WorkManager,
                songId: String,
                quality: String,
            ) {
                val req =
                    OneTimeWorkRequestBuilder<DownloadWorker>()
                        .setInputData(
                            workDataOf(
                                "id" to songId,
                                "quality" to quality,
                            ),
                        ).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                        .build()
                ; wm.enqueueUniqueWork("download-$songId", ExistingWorkPolicy.KEEP, req)
            }
        }
    }
