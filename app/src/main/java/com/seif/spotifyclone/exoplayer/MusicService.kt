package com.seif.spotifyclone.exoplayer

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.seif.spotifyclone.exoplayer.callbacks.MusicPlaybackPreparer
import com.seif.spotifyclone.exoplayer.callbacks.MusicPlayerEventListener
import com.seif.spotifyclone.exoplayer.callbacks.MusicPlayerNotificationListener
import com.seif.spotifyclone.utils.Constants.MEDIA_ROOT_ID
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
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
    private var isPlayerInitialized = false
    private lateinit var musicPlayerEventListener: MusicPlayerEventListener

    companion object {
        var currenctSongDuration = 0L
            private set // we can only change the value from within the service but we can read it from outside the service
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onCreate() {
        super.onCreate()

        serviceScope.launch {
            firebaseMusicSource.fetchMetaData()
        }

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
            currenctSongDuration = exoPlayer.duration
        }

        // will called every time user choose a new song
        val musicPlaybackPreparer = MusicPlaybackPreparer(firebaseMusicSource) {
            currentPlayingSong = it
            preparePlayer(
                firebaseMusicSource.songs,
                it,
                true
            )
        }
        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(musicPlaybackPreparer)
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        mediaSessionConnector.setPlayer(exoPlayer)

        musicPlayerEventListener = MusicPlayerEventListener(this)
        exoPlayer.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(exoPlayer)
    }

    private inner class MusicQueueNavigator :
        TimelineQueueNavigator(mediaSession) { // used to propagate the information about a specific song , the metadata to our notification
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            // will be called once our service needs a new description from a media item
            return firebaseMusicSource.songs[windowIndex].description
        }
    }

    private fun preparePlayer(
        songs: List<MediaMetadataCompat>, // playlist
        itemToPlay: MediaMetadataCompat?, // current song that we want to play
        playNow: Boolean // directly play it when it's prepared or not
    ) {
        val curSongIndex = if (currentPlayingSong == null) 0 else songs.indexOf(itemToPlay)
        exoPlayer.prepare(firebaseMusicSource.asMediaSource(dataSourceFactory))
        // exoPlayer.setMediaSource(firebaseMusicSource.asMediaSource(dataSourceFactory))
        exoPlayer.seekTo(curSongIndex, 0L) // start from the beginning
        exoPlayer.playWhenReady =
            playNow // first time will be false but when the user click on song to play it we convert it to false
    }

    override fun onTaskRemoved(rootIntent: Intent?) { // when intent removed
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // make sure that all the coroutines launched in the service scope are cancelled when the service dies
        exoPlayer.removeListener(musicPlayerEventListener) // to prevent any memory leaks
        exoPlayer.release()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot { // if we want to have some verification logic for the clients
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }
    // if we have playlist and albums : then each of those playlists in albums has it's own id and then clients can subscribe to those ids

    override fun onLoadChildren(
        parentId: String, // id we can call to get a list of songs
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        when(parentId) {
            MEDIA_ROOT_ID -> {
                val resultsSent = firebaseMusicSource.whenReady { isInitialized ->
                    if(isInitialized) {
                        result.sendResult(firebaseMusicSource.asMediaItems())
                        if(!isPlayerInitialized && firebaseMusicSource.songs.isNotEmpty()) { // when we call our player for the first time we want to prepare our player
                            preparePlayer(firebaseMusicSource.songs, firebaseMusicSource.songs[0], false)
                            isPlayerInitialized = true
                        }
                    } else { // when it's ready but not initialized
                        result.sendResult(null)
                    }
                }
                if(!resultsSent) {
                    result.detach()
                }
            }
        }
    }
}