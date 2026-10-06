package com.manishraj.saavnmusic
import android.app.Application; import androidx.hilt.work.HiltWorkerFactory; import androidx.work.Configuration; import dagger.hilt.android.HiltAndroidApp; import javax.inject.Inject
@HiltAndroidApp class SaavnApplication: Application(), Configuration.Provider { @Inject lateinit var workerFactory: HiltWorkerFactory; override val workManagerConfiguration get()=Configuration.Builder().setWorkerFactory(workerFactory).build() }
