package com.manishraj.saavnmusic.core.common

import javax.inject.Qualifier

/** Qualifies the IO [kotlinx.coroutines.CoroutineDispatcher] (Now in Android pattern). */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DispatcherIO

/** Qualifies the Default [kotlinx.coroutines.CoroutineDispatcher]. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DispatcherDefault

/** Qualifies the Main [kotlinx.coroutines.CoroutineDispatcher]. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DispatcherMain
