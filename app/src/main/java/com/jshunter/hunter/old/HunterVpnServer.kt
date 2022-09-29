package com.jshunter.hunter.old

import com.jshunter.hunter.server.Server
import fi.iki.elonen.NanoHTTPD

class HunterVpnServer(hostname: String, port: Int) : Server {
    companion object {
        private const val HOST_NAME = ""
        private const val PORT = 8080
    }

    private val server: NanoHTTPDServer by lazy { NanoHTTPDServer(HOST_NAME, PORT) }
    override fun init() {
        TODO("Not yet implemented")
    }

    override fun start() = server.start()

    override fun stop() = server.stop()

    private class NanoHTTPDServer(hostname: String, port: Int) : NanoHTTPD(hostname, port) {
        override fun serve(session: IHTTPSession?): Response {
            return super.serve(session)
        }
    }
}