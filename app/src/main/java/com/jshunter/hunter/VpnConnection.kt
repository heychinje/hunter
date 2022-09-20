package com.jshunter.hunter

import android.app.PendingIntent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.net.DatagramSocket

interface VpnConnection {
    val connectionId: String?

    fun init(params: Params)

    fun setStatusListener(statusListener: StatusListener)

    fun connect(
        serverHostName: String,
        serverHostPort: Int,
        vpnServiceBuilder: VpnService.Builder,
        protectSocket: (datagramSocket: DatagramSocket) -> Boolean,
        establishConnection: VpnService.Builder.() -> ParcelFileDescriptor
    )

    interface StatusListener {
        fun onInitialized()
        fun onError(connectionId: String?, t: Throwable)
        fun onConnecting()
        fun onConnected(connectionId: String?)
    }

    data class Params(
        val serverName: String,
        val secret: ByteArray,
        val packagesMap: Map<String, Boolean> = mutableMapOf(),
        val proxyHostName: String? = null,
        val proxyHostPort: Int? = null,
        val defaultConfigureIntent: PendingIntent? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Params

            if (serverName != other.serverName) return false
            if (proxyHostName != other.proxyHostName) return false
            if (proxyHostPort != other.proxyHostPort) return false
            if (!secret.contentEquals(other.secret)) return false
            if (packagesMap != other.packagesMap) return false
            if (defaultConfigureIntent != other.defaultConfigureIntent) return false

            return true
        }

        override fun hashCode(): Int {
            var result = serverName.hashCode()
            result = 31 * result + (proxyHostName?.hashCode() ?: 0)
            result = 31 * result + (proxyHostPort ?: 0)
            result = 31 * result + secret.contentHashCode()
            result = 31 * result + packagesMap.hashCode()
            result = 31 * result + (defaultConfigureIntent?.hashCode() ?: 0)
            return result
        }
    }
}