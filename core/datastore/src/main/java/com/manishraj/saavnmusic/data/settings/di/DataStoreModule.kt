package com.manishraj.saavnmusic.data.settings.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.manishraj.saavnmusic.data.session.PlaybackSessionStore
import com.manishraj.saavnmusic.data.settings.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * DataStore providers owned by :core:datastore (split out of the old god
 * AppModule). The preferences file keeps its monolith name ("settings").
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun dataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.settingsDataStore

    @Provides
    @Singleton
    fun settingsRepository(dataStore: DataStore<Preferences>): SettingsRepository = SettingsRepository(dataStore)

    @Provides
    @Singleton
    fun playbackSessionStore(dataStore: DataStore<Preferences>): PlaybackSessionStore = PlaybackSessionStore(dataStore)
}
