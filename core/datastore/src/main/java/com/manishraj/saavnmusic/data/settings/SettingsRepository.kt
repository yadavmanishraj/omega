package com.manishraj.saavnmusic.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.manishraj.saavnmusic.core.common.JIOSAAVN_API_ENDPOINT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppSettings(
    /**
     * The upstream endpoint the app calls — JioSaavn's own `api.php` by
     * default. This is an override for the upstream endpoint itself,
     * NOT a hosted wrapper instance (the app builds the repository's
     * calls directly; see docs/sdlc/UPSTREAM_SPEC.md).
     */
    val apiEndpoint: String = JIOSAAVN_API_ENDPOINT,
    val streamQuality: String = "320kbps",
    val downloadQuality: String = "320kbps",
    val darkTheme: Boolean = true,
    val dynamicColor: Boolean = false,
    /** Theme selection: "SYSTEM", "DARK" or "LIGHT" (redesign spec §7). */
    val themeMode: String = "DARK",
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
        // Stored key names are unchanged from v1 so existing installs
        // migrate silently; "base_url" now holds the api.php endpoint.
        val BASE = stringPreferencesKey("base_url")
        val SQ = stringPreferencesKey("stream_q")
        val DQ = stringPreferencesKey("download_q")
        val DARK = booleanPreferencesKey("dark")
        val DYN = booleanPreferencesKey("dynamic")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    private fun Preferences.toAppSettings(): AppSettings {
        val storedEndpoint = this[K.BASE]?.trim().orEmpty()
        val endpoint =
            when {
                storedEndpoint.isBlank() -> JIOSAAVN_API_ENDPOINT
                // v1 installs stored the (now defunct) hosted wrapper URL
                // as the default; migrate those to the upstream endpoint.
                storedEndpoint.contains("saavn.dev") -> JIOSAAVN_API_ENDPOINT
                else -> storedEndpoint
            }
        return AppSettings(
            apiEndpoint = endpoint,
            streamQuality = this[K.SQ] ?: "320kbps",
            downloadQuality = this[K.DQ] ?: "320kbps",
            darkTheme = this[K.DARK] ?: true,
            dynamicColor = this[K.DYN] ?: false,
            themeMode = this[K.THEME_MODE] ?: if (this[K.DARK] == false) "LIGHT" else "DARK",
        )
    }

    val settings: Flow<AppSettings> = dataStore.data.map { it.toAppSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val next = transform(prefs.toAppSettings())
            prefs[K.BASE] = next.apiEndpoint
            prefs[K.SQ] = next.streamQuality
            prefs[K.DQ] = next.downloadQuality
            prefs[K.DARK] = next.darkTheme
            prefs[K.DYN] = next.dynamicColor
            prefs[K.THEME_MODE] = next.themeMode
        }
    }
}
