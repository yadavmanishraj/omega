package com.manishraj.saavnmusic

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [DataStore] for JVM unit tests: holds one [Preferences]
 * snapshot in a state flow and applies `updateData` transforms to it.
 * Enough for the real [com.manishraj.saavnmusic.data.settings.SettingsRepository]
 * to run against, with no files and no Android runtime.
 */
class FakeDataStore(
    initial: Preferences = emptyPreferences(),
) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
