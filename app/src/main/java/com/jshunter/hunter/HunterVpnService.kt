package com.jshunter.hunter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.VpnService
import android.util.Log
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class HunterVpnService : VpnService() {
//    private data class Connection(val t: Thread, val pfd: ParcelFileDescriptor)

    companion object {
        val TAG: String = HunterVpnService::class.java.simpleName
        const val ACTION_CONNECT = "com.jshunter.hunter.START"
        const val ACTION_DISCONNECT = "com.jshunter.hunter.STOP"
    }

    private val connectingThread = AtomicReference<Thread?>()
//    private val connection = AtomicReference<Connection?>()

    private val connectionStatusListener = object : VpnConnection.StatusListener {
        override fun onInitialized() {
            Log.e(TAG, "onInitialized")
        }

        override fun onError(connectionId: String?, t: Throwable) {
            Log.e(TAG, "onError： ", t)
        }

        override fun onConnecting() {
            Log.e(TAG, "onConnecting")
        }

        override fun onConnected(connectionId: String?) {
            Log.e(TAG, "onConnected: connectionId=$connectionId")
        }
    }

    override fun onCreate() {
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) =
        if (intent?.action == ACTION_DISCONNECT) {
            disconnectServer()
            START_NOT_STICKY
        } else {
            connectServer()
            START_STICKY
        }

    override fun onDestroy() {
        disconnectServer()
    }

    private fun connectServer() {
        // become a foreground service
        updateForegroundNotification(
            Icon.createWithResource(this, R.drawable.ic_vpn), getString(R.string.connecting), null
        )

        // start connection
        startConnection(
            HunterVpnConnection().apply { setStatusListener(connectionStatusListener) },
            VpnConnection.Params("serverName", "secret".toByteArray(StandardCharsets.UTF_8))
        )
    }

    private fun disconnectServer() {

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

    private fun startConnection(vpnConnection: VpnConnection, params: VpnConnection.Params) {
        thread(name = "Connection-Thread") {
            vpnConnection.run {
                init(params)
                connect(
                    serverHostName = "",
                    serverHostPort = 5555,
                    vpnServiceBuilder = Builder(),
                    protectSocket = { datagramSocket -> protect(datagramSocket) },
                    establishConnection = {
                        establish() ?: throw IllegalStateException("Failed to establish connection")
                    }
                )
            }
        }.also {
            connectingThread.getAndSet(it)?.interrupt()
        }
    }
}