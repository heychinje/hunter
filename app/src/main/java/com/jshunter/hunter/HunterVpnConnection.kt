package com.jshunter.hunter

import android.app.PendingIntent
import android.net.ProxyInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit

class HunterVpnConnection : VpnConnection {
    companion object {
        const val PARAM_LINE_SEPARATOR = ' '
        const val PARAM_CONTENT_SEPARATOR = '='
        private const val RETRY_TIMES = 10
        private const val CONTROL_MESSAGE_HEAD = Byte.MIN_VALUE
        private const val MAX_HANDSHAKE_ATTEMPTS = 50
        private val IDLE_INTERVAL = TimeUnit.MICROSECONDS.toMillis(100)
        private val KEEP_ALIVE_INTERVAL = TimeUnit.SECONDS.toMillis(15)
        private val RECEIVE_TIMEOUT = TimeUnit.SECONDS.toMillis(20)
        private val TAG = HunterVpnConnection::class.java.simpleName
    }

    private var _connectionId: String? = null

    private var secret: ByteArray = ByteArray(16) { 0 }

    private val packages = mutableMapOf<String, Boolean>()

    private var serverName: String? = null

    private var proxyHostName: String? = null

    private var proxyHostPort: Int? = null

    private var _statusListener: VpnConnection.StatusListener? = null

    private var configureIntent: PendingIntent? = null

    override val connectionId: String?
        get() = _connectionId

    override fun init(params: VpnConnection.Params) {
        packages.putAll(params.packagesMap.filter { it.key.isNotEmpty() })
        secret = params.secret
        serverName = params.serverName
        proxyHostName = params.proxyHostName
        proxyHostPort = params.proxyHostPort
        configureIntent = params.defaultConfigureIntent
        _statusListener?.onInitialized()
    }

    override fun setStatusListener(statusListener: VpnConnection.StatusListener) {
        this._statusListener = statusListener
    }

    override fun connect(
        serverHostName: String,
        serverHostPort: Int,
        vpnServiceBuilder: VpnService.Builder,
        protectSocket: (datagramSocket: DatagramSocket) -> Boolean,
        establishConnection: VpnService.Builder.() -> ParcelFileDescriptor
    ) {
        kotlin.runCatching {
            val connectionAction = {
                internalConnect(
                    serverSocketAddress = InetSocketAddress(serverHostName, serverHostPort),
                    vpnServiceBuilder = vpnServiceBuilder,
                    onProtectSocket = protectSocket,
                    onCreateId = { UUID.randomUUID().toString() },
                    onEstablish = establishConnection,
                    onConnecting = { _statusListener?.onConnecting() },
                    onConnected = { connectionId -> _statusListener?.onConnected(connectionId) },
                    onError = { connectionId, t -> _statusListener?.onError(connectionId, t) }
                )
            }
            repeatUntilFailure(
                maxAttemptTimes = RETRY_TIMES,
                action = connectionAction,
                onInterval = { _connectionId = null; Thread.sleep(3000) }
            )
        }.onFailure {
            _statusListener?.onError(_connectionId, it)
        }
    }

    private fun internalConnect(
        serverSocketAddress: SocketAddress,
        vpnServiceBuilder: VpnService.Builder,
        onProtectSocket: (datagramSocket: DatagramSocket) -> Boolean,
        onCreateId: () -> String,
        onEstablish: VpnService.Builder.() -> ParcelFileDescriptor,
        onConnecting: () -> Unit,
        onConnected: (connectionId: String?) -> Unit,
        onError: (connectionId: String?, t: Throwable) -> Unit,
    ) = kotlin.run {
        onConnecting()
        var isConnected = false
        try {
            DatagramChannel.open().use { channel ->
                // protect the vpn socket, avoid dead cycle
                if (!onProtectSocket(channel.socket())) throw IllegalStateException("Cannot protect the tunnel")

                // connect server and configure the channel
                channel.connect(serverSocketAddress).configureBlocking(false)

                // data packet buffer
                val packet = ByteBuffer.allocate(Short.MAX_VALUE.toInt())

                // create in/out stream for the channel

                val (i, o) = handShake(vpnServiceBuilder, channel, onEstablish, onError).use {
                    FileInputStream(it.fileDescriptor) to FileOutputStream(it.fileDescriptor)
                }

                // mark as connected and generate a connection id
                isConnected = true
                _connectionId = onCreateId()
                onConnected(_connectionId)

                // keep forwarding the data till something goes wrong
                var lastSendTime = System.currentTimeMillis()
                var lastReceiveTime = System.currentTimeMillis()
                while (true) {
                    var isIdle = true
                    // read the outgoing packet from the input stream
                    channel.run {
                        packet.clear()
                        val length = i.read(packet.array())
                        if (length > 0) {
                            packet.flip()
                            write(packet)

                            isIdle = false
                            lastReceiveTime = System.currentTimeMillis()
                        }
                    }

                    // read the incoming packet from the channel
                    channel.run {
                        packet.clear()
                        val length = read(packet)
                        if (length > 0) {
                            if (packet[0] == CONTROL_MESSAGE_HEAD) {
                                // ignore CONTROL MESSAGE
                            } else {
                                o.write(packet.array(), 0, length)
                            }

                            isIdle = false
                            lastSendTime = System.currentTimeMillis()
                        }
                    }

                    // handle idle state
                    if (isIdle) {
                        Thread.sleep(IDLE_INTERVAL)
                        val currentTime = System.currentTimeMillis()

                        if (currentTime - lastSendTime >= KEEP_ALIVE_INTERVAL) {
                            // no data to send for a while, so send 3 time CONTROL MESSAGE
                            packet.put(CONTROL_MESSAGE_HEAD).limit(1)
                            for (time in 0 until 3) {
                                packet.flip()
                                channel.write(packet)
                            }
                            packet.clear()
                            lastSendTime = currentTime
                        } else if (currentTime - lastReceiveTime >= RECEIVE_TIMEOUT) {
                            // timeout for receive data
                            throw IllegalStateException("Receive timeout")
                        }
                    }
                }
            }
        } catch (e: SocketException) {
            Log.e(TAG, "socket exception occurred: $e")
            onError(connectionId, e)
        }
        isConnected
    }

    private fun handShake(
        vpnServiceBuilder: VpnService.Builder,
        channel: DatagramChannel,
        onEstablish: VpnService.Builder.() -> ParcelFileDescriptor,
        onError: (connectionId: String?, t: Throwable) -> Unit,
    ): ParcelFileDescriptor {
        val packet = ByteBuffer.allocate(1024)
        packet.put(CONTROL_MESSAGE_HEAD).put(secret)
        repeat(3) {
            packet.flip()
            channel.write(packet)
        }
        packet.clear()

        for (i in 0 until MAX_HANDSHAKE_ATTEMPTS) {
            Thread.sleep(IDLE_INTERVAL)

            val length = channel.read(packet)
            if (length > 0 && packet[0] == CONTROL_MESSAGE_HEAD) {
                return vpnServiceBuilder.configure(
                    String(packet.array(), 1, length - 1, StandardCharsets.UTF_8), onError
                ).onEstablish()
            }
        }
        throw IOException("Timed out")
    }

    private fun VpnService.Builder.configure(
        controlMessages: String,
        onError: (connectionId: String?, t: Throwable) -> Unit,
    ) = apply {
        // parse all paramLines and apply to the builder
        val paramLines = controlMessages.split(PARAM_LINE_SEPARATOR)
        for (line in paramLines) {
            line.split(PARAM_CONTENT_SEPARATOR).run {
                // line e.g: paramKey=paramValue
                if (size == 2) {
                    val (paramKey, paramValue) = get(0) to get(1)
                    kotlin.runCatching {
                        Log.i(TAG, "configure: paramKey=$paramKey, paramValue=$paramValue")
                    }.onFailure {
                        onError(_connectionId, it)
                        Log.e(TAG, "configure: parse control messages failed: line=$line", it)
                    }
                }
            }
        }

        // apply the package white list to builder
        packages.entries.forEach { pair ->
            kotlin.runCatching {
                if (pair.value) addAllowedApplication(pair.key)
                else addDisallowedApplication(pair.key)
            }.onFailure {
                onError(_connectionId, it)
                Log.e(TAG, "configure: failed to allow or disable package: package=$pair", it)
            }
        }

        serverName?.let { setSession(it) }
        configureIntent?.let { setConfigureIntent(it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            takeIf {
                proxyHostName?.isNotEmpty() == true && (proxyHostPort ?: 0) > 0
            }?.let {
                setHttpProxy(
                    ProxyInfo.buildDirectProxy(proxyHostName, proxyHostPort ?: return@let)
                )
            }
        }
    }
}