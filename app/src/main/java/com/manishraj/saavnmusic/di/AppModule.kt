package com.manishraj.saavnmusic.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * What is left of the old god AppModule: only the WorkManager singleton,
 * which moves to :core:download with the worker in a later step. Every
 * other binding now lives in the module that owns the implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun workManager(
        @ApplicationContext c: Context,
    ): WorkManager = WorkManager.getInstance(c)
}
