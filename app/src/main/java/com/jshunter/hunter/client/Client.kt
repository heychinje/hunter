package com.jshunter.hunter.client

import android.net.VpnService

interface Client {
    val isConnected: Boolean

    fun init(vpnService: VpnService)

    fun connect()

    fun disconnect()
}