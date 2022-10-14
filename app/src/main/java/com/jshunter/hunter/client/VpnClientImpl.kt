package com.jshunter.hunter.client

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.jshunter.hunter.VpnConfig
import com.jshunter.hunter.eventbus.impl.TAG
import com.jshunter.hunter.ext.toText
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class VpnClientImpl : Client, CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private lateinit var vpnService: VpnService

    private lateinit var vpnInterface: ParcelFileDescriptor

    private lateinit var vpnConfig: VpnConfig

    private var connectionJob: Job? = null

    init {
        Log.e("VpnClientImpl", "...... ")
    }

    @Volatile
    private var _isConnected: Boolean? = null
        @Synchronized set
        @Synchronized get

    override val isConnected: Boolean?
        get() = _isConnected

    override fun init(vpnService: VpnService,vpnConfig: VpnConfig) {
        this.vpnService = vpnService
        this.vpnConfig = vpnConfig
        Log.e("VpnClientImpl", "init: ")
    }

    override fun connect() {
        if (_isConnected == true) return
        connectionJob = launch {
            // config builder
            val builder = vpnService.Builder().apply {
                with(vpnConfig) {
                    addAddress(address, addressPrefixLength)
                    addDnsServer(dns)
                    addRoute(route, addressPrefixLength)
                    setMtu(mtu)
                    setSession(session)
                }
            }

            // establish
            vpnInterface =
                builder.establish() ?: throw IllegalStateException("Could not establish VPN")

            _isConnected = true

            val tx = FileInputStream(vpnInterface.fileDescriptor)
            val rx = FileOutputStream(vpnInterface.fileDescriptor)

            withContext(Dispatchers.IO) {
                val packet = ByteBuffer.allocate(Short.MAX_VALUE.toInt())
                while (_isConnected == true && isActive){
                    val length = tx.read(packet.array())

                    Log.e(TAG, "data: ${packet.array().toText()}")

                    rx.write(packet.array(), 0, length)
                }
            }
        }
    }

    override fun disconnect() {
        if (_isConnected == null || _isConnected == false) return
        Log.e("VpnClientImpl", "disconnect: ")
        _isConnected = false

        connectionJob?.cancel()
    }
}