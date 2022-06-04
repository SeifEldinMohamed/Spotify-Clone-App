package com.seif.spotifyclone.exoplayer

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.seif.spotifyclone.R
import com.seif.spotifyclone.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.seif.spotifyclone.utils.Constants.NOTIFICATION_ID


class MusicNotificationManager(
    private val context: Context,
    sessionToken: MediaSessionCompat.Token,
    notificationListener: PlayerNotificationManager.NotificationListener, // listeners that contains functions that will be called when our notification are created and swiped away by the user bec then we want to stop our foreground serviec
    private val newSongCallback: () -> Unit // we can detect when a new song starts playing as we will need it to update our current duration of the song
) {

    private val notificationManager: PlayerNotificationManager

    init {
        val mediaController = MediaControllerCompat(context, sessionToken) // use to control media
        notificationManager = PlayerNotificationManager.Builder(
            context,
            NOTIFICATION_ID, NOTIFICATION_CHANNEL_ID)
            .setChannelNameResourceId(R.string.notification_channel_name)
            .setChannelDescriptionResourceId(R.string.notification_channel_description)
            .setMediaDescriptionAdapter(DescriptionAdapter(mediaController)) // as recycler view adapter but much simpler we need to tell our notification manager here from exoPlayer what the current title, duration and image of currently playing song is
            .setNotificationListener(notificationListener)
            .build()
        notificationManager.apply {
            setSmallIcon(R.drawable.ic_music)
            setMediaSessionToken(sessionToken) // to give our notification manager access to our current media session in our music service to see changes happened in our music service
        }
    }


    fun showNotification(player: Player) { // we will call this function in our music service class
        notificationManager.setPlayer(player)
    }

    private inner class DescriptionAdapter(
        private val mediaController: MediaControllerCompat
    ) : PlayerNotificationManager.MediaDescriptionAdapter {

        override fun getCurrentContentTitle(player: Player): CharSequence { // return title of currently playing song
            return mediaController.metadata.description.title.toString()
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? { // return the pending intent that leads to our activity
            return mediaController.sessionActivity
        }

        override fun getCurrentContentText(player: Player): CharSequence?    { // return current subtitle
            return mediaController.metadata.description.subtitle.toString()
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            Glide.with(context).asBitmap() // load that object as a bitmap
                .load(mediaController.metadata.description.iconUri)
                .into(object : CustomTarget<Bitmap>() { // this 2 functions called when the image is finally loaded
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        callback.onBitmap(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) = Unit
                })
            return null
        }
    }
}