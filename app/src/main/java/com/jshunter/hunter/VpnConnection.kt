package com.jshunter.hunter

import java.net.DatagramSocket

interface VpnConnection {
    fun init(secret: ByteArray, statusListener: StatusListener? = null)
    fun connect(
        serverHostname: String,
        serverHostPort: Int,
        onProtectSocket: (datagramSocket: DatagramSocket) -> Boolean,
    )

    fun disconnect()

    interface StatusListener {
        fun onError()
        fun onConnected()
        fun onDisconnected()
    }
}