package com.manishraj.saavnmusic.data.repository.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.manishraj.saavnmusic.data.remote.SaavnApi
import com.manishraj.saavnmusic.data.settings.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Bindings owned by :core:data (split out of the old god AppModule). The
 * API client is constructed here — not in :core:network — because its
 * base URL comes from :core:datastore settings, and :core:network must
 * not depend on :core:datastore. :core:data sits above both.
 *
 * Base URL is read once at graph creation; changing it in Settings
 * applies after an app restart (documented in the Settings UI).
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun api(
        client: OkHttpClient,
        json: Json,
        settings: SettingsRepository,
    ): SaavnApi {
        val base = runCatching { runBlocking { settings.settings.first().baseUrl } }.getOrDefault("https://saavn.dev/api/")
        return Retrofit
            .Builder()
            .baseUrl(
                if (base.endsWith("/")) base else "$base/",
            )
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SaavnApi::class.java)
    }
}
