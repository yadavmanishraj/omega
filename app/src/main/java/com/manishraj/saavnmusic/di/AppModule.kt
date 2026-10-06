package com.manishraj.saavnmusic.di
import android.content.Context; import androidx.room.Room; import androidx.work.WorkManager
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.manishraj.saavnmusic.data.local.*; import com.manishraj.saavnmusic.data.remote.SaavnApi; import com.manishraj.saavnmusic.data.settings.SettingsRepository
import dagger.*; import dagger.hilt.*; import dagger.hilt.android.qualifiers.ApplicationContext; import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first; import kotlinx.coroutines.runBlocking; import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType; import okhttp3.OkHttpClient; import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit; import java.util.concurrent.TimeUnit; import javax.inject.Singleton
@Module @InstallIn(SingletonComponent::class) object AppModule {
    @Provides @Singleton fun json(): Json = Json { ignoreUnknownKeys=true; coerceInputValues=true }
    @Provides @Singleton fun okHttp(): OkHttpClient = OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply{level=HttpLoggingInterceptor.Level.BASIC}).connectTimeout(20,TimeUnit.SECONDS).readTimeout(30,TimeUnit.SECONDS).build()
    /** Base URL is user-configurable in Settings (DataStore); read once at graph creation, restart applies changes - documented in README. */
    @Provides @Singleton fun api(client:OkHttpClient, json:Json, settings:SettingsRepository): SaavnApi {
        val base = runCatching { runBlocking { settings.settings.first().baseUrl } }.getOrDefault("https://saavn.dev/api/")
        return Retrofit.Builder().baseUrl(if(base.endsWith("/")) base else "$base/").client(client).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(SaavnApi::class.java)
    }
    @Provides @Singleton fun db(@ApplicationContext c:Context): AppDatabase = Room.databaseBuilder(c,AppDatabase::class.java,"saavn-music.db").build()
    @Provides fun dao(db:AppDatabase)=db.libraryDao()
    @Provides @Singleton fun workManager(@ApplicationContext c:Context)=WorkManager.getInstance(c)
}
