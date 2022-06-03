package com.seif.spotifyclone.exoplayer

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.seif.spotifyclone.utils.Constants.NETWORK_ERROR
import com.seif.spotifyclone.utils.Event
import com.seif.spotifyclone.utils.Resource


class MusicServiceConnection( // communicate with viewModel
    context: Context
) {
    private val _isConnected =
        MutableLiveData<Event<Resource<Boolean>>>() // contains connection between our activity and music service is currently active
    val isConnected: LiveData<Event<Resource<Boolean>>> = _isConnected  // immutable غير متغيرة

    private val _networkError =
        MutableLiveData<Event<Resource<Boolean>>>() // whether there is a network error or not
    val networkError: LiveData<Event<Resource<Boolean>>> = _networkError

    private val _playbackState =
        MutableLiveData<PlaybackStateCompat?>() // where the current player in playing or not
    val playbackState: LiveData<PlaybackStateCompat?> = _playbackState

    private val _currentPlayingSong =
        MutableLiveData<MediaMetadataCompat?>() // contains metadata information about currently playing song
    val currentPlayingSong: LiveData<MediaMetadataCompat?> = _currentPlayingSong

    lateinit var mediaController: MediaControllerCompat

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)

    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(
            context,
            MusicService::class.java
        ),
        mediaBrowserConnectionCallback,
        null
    ).apply { connect() }

    val transportControls: MediaControllerCompat.TransportControls // as pause, skip song, resume
        get() = mediaController.transportControls // return this controller when we use (transportControls) object

    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.unsubscribe(parentId, callback)
    }

    private inner class MediaBrowserConnectionCallback(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() { // once music serviec connection is active it will be called
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }
            _isConnected.postValue(Event(Resource.success(true)))
        }

        override fun onConnectionSuspended() { // when connection is suspended
            _isConnected.postValue(
                Event(
                    Resource.error(
                        "The connection was suspended", false
                    )
                )
            )
        }

        override fun onConnectionFailed() { // when connection is failed
            _isConnected.postValue(
                Event(
                    Resource.error(
                        "Couldn't connect to media browser", false
                    )
                )
            )
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) { // when song paused or resumed
            _playbackState.postValue(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) { // when song is skipped
            _currentPlayingSong.postValue(metadata)
        }

        override fun onSessionEvent(
            event: String?,
            extras: Bundle?
        ) { // used to be notified when there is an error
            super.onSessionEvent(event, extras)
            when (event) {
                NETWORK_ERROR -> _networkError.postValue(
                    Event(
                        Resource.error(
                            "Couldn't connect to the server. Please check your internet connection.",
                            null
                        )
                    )
                )
            }
        }

        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }
}


