package com.manishraj.saavnmusic.download

import android.content.Context
import android.os.Environment
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.manishraj.saavnmusic.data.repository.MusicRepository
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.downloadFileName
import com.manishraj.saavnmusic.domain.sanitizeFileName
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream

/**
 * Downloads mirroring jiosaavn-dl: pick the requested quality from
 * downloadUrl (fallback: the song's stream URL), save as sanitized
 * "Artist - Title.m4a" into app-specific Music/SaavnMusic (no storage
 * permission needed), then register in Room for offline playback.
 *
 * Row lifecycle (Room is what Library renders, so the worker owns the
 * whole lifecycle):
 * - start: upsert a DOWNLOADING row at progress 0;
 * - during transfer: throttled progress writes (every >=5% of bytes or
 *   >=500 ms), percent + bytes downloaded;
 * - success: [MusicRepository.registerDownload] flips the row to
 *   COMPLETED at 100%;
 * - failure (no URL, HTTP error, IO error, upstream song fetch
 *   failing): the row is marked FAILED — Library offers Retry, which
 *   re-enqueues. Nothing is left to WorkManager's implicit retry;
 * - cancellation (WorkManager stop, or the user deleting the download
 *   from Library, which cancels the unique work): the partial file is
 *   deleted and the row removed, so nothing stays stuck DOWNLOADING.
 *
 * NOTE: full ID3/M4A tag+artwork embedding (mutagen in jiosaavn-dl) is
 * a metadata sidecar JSON here; see STUDY.md.
 */
@HiltWorker
class DownloadWorker
    @AssistedInject
    constructor(
        @Assisted ctx: Context,
        @Assisted params: WorkerParameters,
        private val repo: MusicRepository,
        private val http: OkHttpClient,
    ) : CoroutineWorker(ctx, params) {
        override suspend fun doWork(): Result {
            val id = inputData.getString(KEY_ID) ?: return Result.failure()
            val quality = inputData.getString(KEY_QUALITY) ?: DEFAULT_QUALITY
            // Display metadata travels with the work request so a Room
            // row can be written (DOWNLOADING, later FAILED) even when
            // resolving the song upstream fails.
            val fallback = fallbackSong(id)
            var targetFile: File? = null
            return try {
                val song =
                    try {
                        repo.song(id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Without the song there is no URL to download;
                        // still record the failure if we have metadata.
                        if (fallback != null) {
                            val file = File(musicDir(), downloadFileName(fallback))
                            repo.markDownloadStarted(fallback, file.absolutePath, quality)
                            repo.markDownloadFailed(id)
                        }
                        return Result.failure()
                    }
                val dir = musicDir()
                val file = File(dir, downloadFileName(song))
                targetFile = file
                val url =
                    song.downloadUrls.firstOrNull { it.first == quality }?.second ?: song.streamUrl
                repo.markDownloadStarted(song, file.absolutePath, quality)
                if (url == null) {
                    // No stream for this song at any quality: a
                    // first-class FAILED row, not an endless retry.
                    repo.markDownloadFailed(id)
                    return Result.failure()
                }
                val completed =
                    http.newCall(Request.Builder().url(url).build()).execute().use { response ->
                        val body = response.body
                        if (!response.isSuccessful || body == null) {
                            false
                        } else {
                            copyWithProgress(body.contentLength(), body.byteStream(), file, id)
                            true
                        }
                    }
                if (!completed) {
                    runCatching { file.delete() }
                    repo.markDownloadFailed(id)
                    return Result.failure()
                }
                currentCoroutineContext().ensureActive()
                writeSidecar(dir, song)
                repo.registerDownload(song, file.absolutePath, quality, file.length())
                Result.success()
            } catch (e: CancellationException) {
                // Cancelled mid-transfer: remove the partial file and
                // the row so nothing is left stuck in DOWNLOADING.
                targetFile?.let { partial -> runCatching { partial.delete() } }
                runCatching { repo.removeDownloadRow(id) }
                throw e
            } catch (e: Exception) {
                targetFile?.let { partial -> runCatching { partial.delete() } }
                if (targetFile != null) {
                    runCatching { repo.markDownloadFailed(id) }
                }
                Result.failure()
            }
        }

        /**
         * Streams [input] into [file], persisting progress to Room as
         * bytes move — throttled to a write when the percent complete
         * advances by at least [PROGRESS_STEP_PERCENT] or
         * [PROGRESS_MIN_INTERVAL_MS] has elapsed since the last write.
         * Percent stays 0 while the total size is unknown (the byte
         * count still advances); the completion write sets 100.
         */
        private suspend fun copyWithProgress(
            totalBytes: Long,
            input: InputStream,
            file: File,
            songId: String,
        ) {
            var downloaded = 0L
            var lastProgress = 0
            var lastWriteAtMs = 0L
            input.use { stream ->
                file.outputStream().use { out ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = stream.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        downloaded += read
                        val progress =
                            if (totalBytes > 0) {
                                (downloaded * 100 / totalBytes).toInt().coerceIn(0, 99)
                            } else {
                                0
                            }
                        val now = System.currentTimeMillis()
                        val stepped = progress - lastProgress >= PROGRESS_STEP_PERCENT
                        val waited = now - lastWriteAtMs >= PROGRESS_MIN_INTERVAL_MS
                        if (stepped || waited) {
                            repo.updateDownloadProgress(songId, progress, downloaded)
                            lastProgress = progress
                            lastWriteAtMs = now
                        }
                    }
                }
            }
        }

        private fun musicDir(): File =
            File(
                applicationContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "SaavnMusic",
            ).apply { mkdirs() }

        private fun writeSidecar(
            dir: File,
            song: Song,
        ) {
            runCatching {
                File(dir, sanitizeFileName(song.name) + ".json").writeText(
                    """{"title":"${song.name}","artist":"${song.artist}","album":"${song.album.orEmpty()}","image":"${song.imageUrl.orEmpty()}"}""",
                )
            }
        }

        /** The enqueue-time display metadata as a stand-in [Song], or null when absent. */
        private fun fallbackSong(id: String): Song? {
            val name = inputData.getString(KEY_NAME) ?: return null
            return Song(
                id = id,
                name = name,
                artist = inputData.getString(KEY_ARTIST).orEmpty(),
                album = inputData.getString(KEY_ALBUM),
                imageUrl = inputData.getString(KEY_IMAGE),
                durationSec = null,
                streamUrl = null,
            )
        }

        companion object {
            private const val KEY_ID = "id"
            private const val KEY_QUALITY = "quality"
            private const val KEY_NAME = "name"
            private const val KEY_ARTIST = "artist"
            private const val KEY_ALBUM = "album"
            private const val KEY_IMAGE = "image"
            private const val DEFAULT_QUALITY = "320kbps"
            private const val PROGRESS_STEP_PERCENT = 5
            private const val PROGRESS_MIN_INTERVAL_MS = 500L

            /** Unique-work name for a song's download (also used to cancel it from Library). */
            fun uniqueWorkName(songId: String): String = "download-$songId"

            fun enqueue(
                wm: WorkManager,
                song: Song,
                quality: String,
            ) {
                val req =
                    OneTimeWorkRequestBuilder<DownloadWorker>()
                        .setInputData(
                            workDataOf(
                                KEY_ID to song.id,
                                KEY_QUALITY to quality,
                                KEY_NAME to song.name,
                                KEY_ARTIST to song.artist,
                                KEY_ALBUM to song.album,
                                KEY_IMAGE to song.imageUrl,
                            ),
                        ).setConstraints(
                            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                        ).build()
                wm.enqueueUniqueWork(uniqueWorkName(song.id), ExistingWorkPolicy.KEEP, req)
            }
        }
    }
