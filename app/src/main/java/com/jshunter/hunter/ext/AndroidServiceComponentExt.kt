package com.jshunter.hunter.ext

import android.app.*
import android.graphics.drawable.Icon

fun Service.updateNotification(
    iconResId: Int?,
    msgResId: Int?,
    channelId: String,
    channelName: String,
    notificationId: Int,
    pendingIntent: PendingIntent? = null,
) = updateNotification(
    iconResId?.let { Icon.createWithResource(this, it) },
    msgResId?.let { getString(it) },
    channelId,
    channelName,
    notificationId,
    pendingIntent,
)

fun Service.updateNotification(
    icon: Icon?,
    msg: String?,
    channelId: String,
    channelName: String,
    notificationId: Int,
    pendingIntent: PendingIntent? = null,
) {
    getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
    )
    Notification.Builder(this, channelId).apply {
        setSmallIcon(icon)
        setContentText(msg)
        setContentIntent(pendingIntent)
    }.build().let {
        startForeground(notificationId, it)
    }
}
