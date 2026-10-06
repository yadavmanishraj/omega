package com.manishraj.saavnmusic.playback
import androidx.media3.common.AudioAttributes; import androidx.media3.common.C; import androidx.media3.exoplayer.ExoPlayer; import androidx.media3.session.*
/** Media3 MediaSessionService: foreground mediaPlayback service, notification + lock-screen / Bluetooth controls for free. */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    override fun onCreate(){ super.onCreate()
        val player = ExoPlayer.Builder(this).setAudioAttributes(AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(), true).setHandleAudioBecomingNoisy(true).build()
        session = MediaSession.Builder(this, player).build()
    }
    override fun onGetSession(info: MediaSession.ControllerInfo) = session
    override fun onTaskRemoved(rootIntent: android.content.Intent?){ val p=session?.player; if(p==null || !p.playWhenReady || p.mediaItemCount==0) stopSelf(); super.onTaskRemoved(rootIntent) }
    override fun onDestroy(){ session?.run{ player.release(); release() }; session=null; super.onDestroy() }
}
