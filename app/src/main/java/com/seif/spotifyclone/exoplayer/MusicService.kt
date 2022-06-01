package com.seif.spotifyclone.exoplayer

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.seif.spotifyclone.exoplayer.callbacks.MusicPlaybackPreparer
import com.seif.spotifyclone.exoplayer.callbacks.MusicPlayerEventListener
import com.seif.spotifyclone.exoplayer.callbacks.MusicPlayerNotificationListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import javax.inject.Inject


private const val SERVICE_TAG = "MusicService"

@AndroidEntryPoint
class MusicService @Inject constructor(
    private val dataSourceFactory: DefaultDataSource.Factory,
    private val exoPlayer: ExoPlayer,
    private val firebaseMusicSource: FirebaseMusicSource
) : MediaBrowserServiceCompat() { // it's called MediaBrowserServiceCompat bec of loadChildren function

    private lateinit var musicNotificationManager: MusicNotificationManager

    // coroutine scope will be limit to the life time of our service
    private val serviceJob = Job()
    private val serviceScope =
        CoroutineScope(Dispatchers.Main + serviceJob)   // the scope in which we launch the coroutine will deal with the cancellation of the coroutines (life time)

    // current session of playing music
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector // class will use to connect to the above media session

    var isForegroundService = false

    private var currentPlayingSong: MediaMetadataCompat? = null

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onCreate() {
        super.onCreate()
        // we have to get the activity intent for our notification
        // so when we click on the notification then we want to open our activity
        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, 0)
        }
        mediaSession = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }

        sessionToken = mediaSession.sessionToken

        musicNotificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(this)
        ) { // will be called when current song switched
            // update current song duration so we can observe on that in our fragments
        }

        // will called every time user choose a new song
        val musicPlaybackPreparer = MusicPlaybackPreparer(firebaseMusicSource){
            currentPlayingSong = it
            preparePlayer(
                firebaseMusicSource.songs,
                it,
                true
            )
        }
        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(musicPlaybackPreparer)
        mediaSessionConnector.setPlayer(exoPlayer)

        exoPlayer.addListener(MusicPlayerEventListener(this))
        musicNotificationManager.showNotification(exoPlayer)
    }

    private fun preparePlayer(
        songs: List<MediaMetadataCompat>, // playlist
        itemToPlay: MediaMetadataCompat?, // current song that we want to play
        playNow: Boolean // directly play it when it's prepared or not
    ) {
        val curSongIndex = if(currentPlayingSong == null) 0 else songs.indexOf(itemToPlay)
        exoPlayer.prepare(firebaseMusicSource.asMediaSource(dataSourceFactory))
        // exoPlayer.setMediaSource(firebaseMusicSource.asMediaSource(dataSourceFactory))
        exoPlayer.seekTo(curSongIndex, 0L) // start from the beginning
        exoPlayer.playWhenReady = playNow // first time will be false but when the user click on song to play it we convert it to false
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // make sure that all the coroutines launched in the service scope are cancelled when the service dies
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        TODO()
    }

    override fun onLoadChildren(
        parentId: String, // id we can call to get a list of songs
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        TODO("Not yet implemented")
    }
}