package com.manishraj.saavnmusic.playback.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

/**
 * Playback providers owned by :core:playback. The [ExoPlayer] is
 * deliberately NOT a singleton: [com.manishraj.saavnmusic.playback.PlaybackService]
 * releases the player it was injected with when the service is destroyed,
 * exactly as the monolith did — a fresh service gets a fresh player.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlaybackModule {
    @Provides
    fun exoPlayer(
        @ApplicationContext context: Context,
    ): ExoPlayer =
        ExoPlayer
            .Builder(context)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            ).setHandleAudioBecomingNoisy(true)
            .build()
}
