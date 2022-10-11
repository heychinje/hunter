package com.jshunter.hunter.client

import android.net.VpnService
import android.util.Log

class VpnClientImpl : Client {
    private lateinit var vpnService: VpnService

    @Volatile
    private var _isConnected = false

    override val isConnected: Boolean
        get() = _isConnected

    override fun init(vpnService: VpnService) {
        this.vpnService = vpnService
        Log.e("VpnClientImpl", "init: ")
    }

    @Synchronized
    override fun connect() {
        if (_isConnected) return

        Log.e("VpnClientImpl", "connect: ")
        _isConnected = true


    }

    @Synchronized
    override fun disconnect() {
        if (!_isConnected) return

        Log.e("VpnClientImpl", "disconnect: ")
        _isConnected = false
    }

    init {
        Log.e("VpnClientImpl", "...... ")
    }
}