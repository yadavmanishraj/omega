package com.manishraj.saavnmusic.core.common.di

import com.manishraj.saavnmusic.core.common.DispatcherDefault
import com.manishraj.saavnmusic.core.common.DispatcherIO
import com.manishraj.saavnmusic.core.common.DispatcherMain
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Provides the three standard dispatchers behind the qualifiers in `:core:common`. */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @DispatcherIO
    fun providesIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DispatcherDefault
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @DispatcherMain
    fun providesMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
