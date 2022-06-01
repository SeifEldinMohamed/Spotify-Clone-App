package com.seif.spotifyclone.exoplayer.callbacks

import android.app.Notification
import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.seif.spotifyclone.exoplayer.MusicService
import com.seif.spotifyclone.utils.Constants.NOTIFICATION_ID


class MusicPlayerNotificationListener(
    private val musicService: MusicService
) : PlayerNotificationManager.NotificationListener {
    // Called after the notification has been cancelled. (user swiped it away)
    override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
        super.onNotificationCancelled(notificationId, dismissedByUser)
        musicService.apply {
            stopForeground(true)
            isForegroundService = false
            stopSelf() // to stop the service
        }
    }

    // Called each time after the notification has been posted.
    // For a service, the ongoing flag can be used as an indicator
    // as to whether it should be in the foreground.
    override fun onNotificationPosted(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean
    ) {
        super.onNotificationPosted(notificationId, notification, ongoing)
        musicService.apply {
            if (ongoing && !isForegroundService) {
                // now we need to start our foreground service
                ContextCompat.startForegroundService(
                    this,
                    Intent(applicationContext, this::class.java) // this refers to musicService
                )
                startForeground(NOTIFICATION_ID, notification)
                isForegroundService = true
            }
        }
    }
}
