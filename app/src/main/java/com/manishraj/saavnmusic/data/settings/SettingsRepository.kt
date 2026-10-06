package com.manishraj.saavnmusic.data.settings
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.*

private val Context.ds by preferencesDataStore("settings")

data class AppSettings(
    val baseUrl: String = "https://saavn.dev/api/",
    val streamQuality: String = "320kbps",
    val downloadQuality: String = "320kbps",
    val darkTheme: Boolean = true,
    val dynamicColor: Boolean = true,
)

@Singleton class SettingsRepository
    @Inject
    constructor(
        @ApplicationContext private val ctx: Context,
    ) {
        private object K {
            val BASE = stringPreferencesKey("base_url")
            val SQ = stringPreferencesKey("stream_q")
            val DQ = stringPreferencesKey("download_q")
            val DARK = booleanPreferencesKey("dark")
            val DYN = booleanPreferencesKey("dynamic")
        }

        val settings =
            ctx.ds.data.map {
                AppSettings(
                    it[K.BASE] ?: "https://saavn.dev/api/",
                    it[K.SQ] ?: "320kbps",
                    it[K.DQ] ?: "320kbps",
                    it[K.DARK] ?: true,
                    it[K.DYN] ?: true,
                )
            }

        suspend fun update(
            transform: (AppSettings) -> AppSettings,
            current: AppSettings,
        ) {
            ctx.ds.edit {
                val n = transform(current)
                it[K.BASE] =
                    n.baseUrl
                it[K.SQ] = n.streamQuality
                it[K.DQ] = n.downloadQuality
                it[K.DARK] = n.darkTheme
                it[K.DYN] = n.dynamicColor
            }
        }
    }
