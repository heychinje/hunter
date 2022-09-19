package com.jshunter.hunter

import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.TimeUnit

class HunterVpnConnection : VpnConnection {
    companion object {
        private const val RETRY_TIMES = 10
        private const val MAX_PACKET_SIZE = Short.MAX_VALUE
        private const val CONTROL_MESSAGE = Byte.MIN_VALUE
        private val IDLE_INTERVAL = TimeUnit.MICROSECONDS.toMillis(100)
        private val SEND_ALIVE_INTERVAL = TimeUnit.SECONDS.toMillis(15)
        private val RECEIVE_TIMEOUT = TimeUnit.SECONDS.toMillis(20)
    }

    private var secret: ByteArray = ByteArray(16) { 0 }

    private var statusListener: VpnConnection.StatusListener? = null

    override fun init(secret: ByteArray, statusListener: VpnConnection.StatusListener?) {
        this.secret = secret
        this.statusListener = statusListener
    }

    override fun connect(
        serverHostname: String,
        serverHostPort: Int,
        onProtectSocket: (datagramSocket: DatagramSocket) -> Boolean,
    ) {
        retry(RETRY_TIMES) {
            internalConnect(
                InetSocketAddress(serverHostname, serverHostPort), onProtectSocket
            )
        }
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }

    private fun internalConnect(
        serverSocketAddress: SocketAddress,
        onProtectSocket: (datagramSocket: DatagramSocket) -> Boolean,
    ) = kotlin.runCatching {
        DatagramChannel.open().use { channel ->
            // protect the vpn socket, avoid dead cycle
            if (!onProtectSocket(channel.socket())) throw IllegalStateException("Cannot protect the tunnel")

            // connect server and configure the channel
            channel.connect(serverSocketAddress).configureBlocking(false)

            // data packet buffer
            val packet = ByteBuffer.allocate(MAX_PACKET_SIZE.toInt())

            // create in/out stream for the channel
            val (inStream, outStream) = handShake(channel).use {
                FileInputStream(it.fileDescriptor) to FileOutputStream(it.fileDescriptor)
            }

            var lastSendTime = System.currentTimeMillis()
            var lastReceiveTime = System.currentTimeMillis()

            // keep forwarding the data till something goes wrong
            while (true) {
                var isIdle = true
                // read the outgoing packet from the input stream
                channel.run {
                    packet.clear()
                    val length = inStream.read(packet.array())
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
                        if (packet[0] == CONTROL_MESSAGE) {
                            // ignore CONTROL MESSAGE
                        } else {
                            outStream.write(packet.array(), 0, length)
                        }

                        isIdle = false
                        lastSendTime = System.currentTimeMillis()
                    }
                }

                // handle idle state
                if (isIdle) {
                    Thread.sleep(IDLE_INTERVAL)
                    val currentTime = System.currentTimeMillis()

                    if (currentTime - lastSendTime >= SEND_ALIVE_INTERVAL) {
                        // no data to send for a while, so send 3 time CONTROL MESSAGE
                        packet.put(CONTROL_MESSAGE).limit(1)
                        for (i in 0 until 3) {
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
    }.isSuccess

    private fun handShake(channel: DatagramChannel): ParcelFileDescriptor {
        val packet = ByteBuffer.allocate(1024)
        packet.put(CONTROL_MESSAGE).put(secret)
        repeat(3){
            packet.flip()
            channel.write(packet)
        }
        packet.clear()

        
    }

    private fun retry(times: Int, action: (index: Int) -> Boolean) {
        var result: Boolean
        var index = 0
        do {
            result = action(index)
            index++
        } while (!result && index < times)
    }
}