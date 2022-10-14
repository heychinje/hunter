package com.jshunter.hunter.client

import android.net.VpnService
import com.jshunter.hunter.VpnConfig

interface Client {
    val isConnected: Boolean?

    fun init(vpnService: VpnService,vpnConfig: VpnConfig)

    fun connect()

    fun disconnect()
}