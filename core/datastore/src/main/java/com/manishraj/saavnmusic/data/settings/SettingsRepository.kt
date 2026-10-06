package com.manishraj.saavnmusic.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppSettings(
    val baseUrl: String = "https://saavn.dev/api/",
    val streamQuality: String = "320kbps",
    val downloadQuality: String = "320kbps",
    val darkTheme: Boolean = true,
    val dynamicColor: Boolean = true,
)

/**
 * Preferences-backed settings. The [DataStore] itself is provided by
 * `DataStoreModule` in this module's `di` package (the preferences file is
 * still named "settings", so existing installs keep their values).
 */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    private object K {
        val BASE = stringPreferencesKey("base_url")
        val SQ = stringPreferencesKey("stream_q")
        val DQ = stringPreferencesKey("download_q")
        val DARK = booleanPreferencesKey("dark")
        val DYN = booleanPreferencesKey("dynamic")
    }

    private fun Preferences.toAppSettings(): AppSettings =
        AppSettings(
            baseUrl = this[K.BASE] ?: "https://saavn.dev/api/",
            streamQuality = this[K.SQ] ?: "320kbps",
            downloadQuality = this[K.DQ] ?: "320kbps",
            darkTheme = this[K.DARK] ?: true,
            dynamicColor = this[K.DYN] ?: true,
        )

    val settings: Flow<AppSettings> = dataStore.data.map { it.toAppSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val next = transform(prefs.toAppSettings())
            prefs[K.BASE] = next.baseUrl
            prefs[K.SQ] = next.streamQuality
            prefs[K.DQ] = next.downloadQuality
            prefs[K.DARK] = next.darkTheme
            prefs[K.DYN] = next.dynamicColor
        }
    }
}
