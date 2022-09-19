package com.jshunter.hunter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class HunterVpnService : VpnService() {
//    private data class Connection(val t: Thread, val pfd: ParcelFileDescriptor)

    companion object {
        val TAG = HunterVpnService::class.java.simpleName
        const val ACTION_CONNECT = "com.jshunter.hunter.START"
        const val ACTION_DISCONNECT = "com.jshunter.hunter.STOP"
    }

    private val connectingThread = AtomicReference<Thread?>()
//    private val connection = AtomicReference<Connection?>()

    override fun onCreate() {

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) =
        if (intent?.action == ACTION_DISCONNECT) {
            disconnect()
            START_NOT_STICKY
        } else {
            connect()
            START_STICKY
        }

    override fun onDestroy() {
        disconnect()
    }

    private fun connect() {
        // become a foreground service
        updateForegroundNotification(
            Icon.createWithResource(this, R.drawable.ic_vpn),
            getString(R.string.connecting),
            null
        )

        // start connection
        startConnection(HunterVpnConnection())
    }

    private fun disconnect() {

    }

    private fun updateForegroundNotification(
        icon: Icon,
        msg: String,
        pendingIntent: PendingIntent?,
    ) {
        val channelId = "com.jshunter.hunter"
        val channelName = "Hunter"
        val notificationId = 1;
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(channelId, channelName, IMPORTANCE_HIGH)
        )
        startForeground(
            notificationId,
            Notification.Builder(this@HunterVpnService, channelId).apply {
                setSmallIcon(icon)
                setContentText(msg)
                setContentIntent(pendingIntent)
            }.build()
        )
    }

    private fun startConnection(vpnConnection: VpnConnection) {
        thread(name = "Connection-Thread") {
            vpnConnection.run {
                init()
                connect {

                }
            }
        }.also {
            connectingThread.getAndSet(it)?.interrupt()
        }
    }
}