package com.jshunter.hunter

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.jshunter.hunter.client.Client
import com.jshunter.hunter.client.VpnClientImpl
import com.jshunter.hunter.eventbus.Event
import com.jshunter.hunter.eventbus.Subscriber
import com.jshunter.hunter.eventbus.impl.EventBus
import com.jshunter.hunter.ext.updateNotification

@RequiresApi(Build.VERSION_CODES.S)
class HunterVpnService : VpnService(), Subscriber {
    companion object {
        private const val TAG = "HunterVpnService"
        private const val CHANNEL_ID = "com.jshunter.hunter"
        private const val CHANNEL_NAME = "Hunter"
        private const val NOTIFICATION_ID = 1;
    }

    private val mainActivityIntent by lazy { Intent(this, MainActivity::class.java) }

    private val pendingIntent by lazy { PendingIntent.getActivity(this, 0, mainActivityIntent, FLAG_IMMUTABLE) }

    private val client: Client by lazy { VpnClientImpl() }

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "onCreate: ")
        updateNotification(
            R.drawable.ic_vpn, R.string.connecting, CHANNEL_ID, CHANNEL_NAME, NOTIFICATION_ID,pendingIntent
        )
        client.init(this)
        EventBus.register(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "onStartCommand: intent: $intent, flags: $flags, startId: $startId")
        connectRemoteServer()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectRemoteServer()
        EventBus.unregister(this)
    }

    override fun onEvent(event: Event) {
        Log.e(TAG, "onEvent: $event")
        when (event.type) {
            EventType.OnUpdateNotification -> updateNotification(event)
            else -> {}
        }
    }

    private fun connectRemoteServer() {
        client.connect()
    }

    private fun disconnectRemoteServer() {
        client.disconnect()
    }

    private fun updateNotification(event: Event) = with(event) {
        updateNotification(
            bundle?.getInt(BundleKey.icon),
            bundle?.getInt(BundleKey.msg),
            CHANNEL_ID,
            CHANNEL_NAME,
            NOTIFICATION_ID,
            bundle?.getParcelable(BundleKey.pendingIntent)
        )
    }
}