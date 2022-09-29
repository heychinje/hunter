package com.jshunter.hunter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.VpnService
import com.jshunter.hunter.client.Client
import com.jshunter.hunter.client.VpnClientImpl
import com.jshunter.hunter.eventbus.Event
import com.jshunter.hunter.eventbus.Subscriber
import com.jshunter.hunter.eventbus.impl.EventBus

class HunterVpnService : VpnService(), Subscriber {
    companion object {
        private const val CHANNEL_ID = "com.jshunter.hunter"
        private const val CHANNEL_NAME = "Hunter"
        private const val NOTIFICATION_ID = 1;
    }

    private val client: Client by lazy { VpnClientImpl() }

    override fun onCreate() {
        super.onCreate()
        EventBus.register(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        client.init()
        updateNotification(
            Icon.createWithResource(this, R.drawable.ic_vpn), getString(R.string.connecting), null
        )
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        client.disconnect()
        EventBus.unregister(this)
    }

    override fun onEvent(event: Event) {
        when (event.type) {
            EventType.OnConnect -> client.connect()
            EventType.OnDisconnect -> client.disconnect()
            EventType.OnUpdateNotification -> updateNotification(event)
            else -> {}
        }
    }


    private fun updateNotification(
        icon: Icon,
        msg: String,
        pendingIntent: PendingIntent?,
        channelId: String = CHANNEL_ID,
        channelName: String = CHANNEL_NAME,
        notificationId: Int = NOTIFICATION_ID,
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

    private fun updateNotification(event: Event) {
        val (icon, msg, pendingIntent) = with(event) {
            val icon = bundle?.getParcelable<Icon>(BundleKey.icon)
            val msg = bundle?.getString(BundleKey.msg)
            val pendingIntent = bundle?.getParcelable<PendingIntent>(BundleKey.pendingIntent)
            Triple(icon, msg, pendingIntent)
        }
        updateNotification(icon ?: return, msg ?: return, pendingIntent)
    }
}