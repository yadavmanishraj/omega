package com.manishraj.saavnmusic.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persistence for the last playback session (F-16): a single opaque
 * JSON string owned by [com.manishraj.saavnmusic.playback.PlayerController]
 * (the codec and restore decisions live there, unit-tested — this
 * store deliberately knows nothing about the payload's shape).
 *
 * It shares the app's one preferences DataStore with settings: two
 * DataStore instances over one file are forbidden, so the session
 * key rides the instance [com.manishraj.saavnmusic.data.settings.di.DataStoreModule]
 * already provides.
 */
class PlaybackSessionStore(
    private val dataStore: DataStore<Preferences>,
) {
    private object K {
        val SESSION = stringPreferencesKey("playback_session_json")
    }

    /** The last written session payload, or null when none was ever saved. */
    val sessionJson: Flow<String?> = dataStore.data.map { it[K.SESSION] }

    suspend fun save(json: String) {
        dataStore.edit { it[K.SESSION] = json }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(K.SESSION) }
    }
}
