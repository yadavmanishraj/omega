package com.manishraj.saavnmusic.data.local.di

import android.content.Context
import androidx.room.Room
import com.manishraj.saavnmusic.data.local.AppDatabase
import com.manishraj.saavnmusic.data.local.LibraryDao
import com.manishraj.saavnmusic.data.local.MIGRATION_1_2
import com.manishraj.saavnmusic.data.local.MIGRATION_2_3
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Room providers owned by :core:database (split out of the old god
 * AppModule). The database name is unchanged from the monolith
 * ("saavn-music.db") so existing installs keep their library.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun db(
        @ApplicationContext c: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(c, AppDatabase::class.java, "saavn-music.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun dao(db: AppDatabase): LibraryDao = db.libraryDao()
}
