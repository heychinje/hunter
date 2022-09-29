package com.jshunter.hunter.client

interface Client {
    fun init()

    fun connect()

    fun disconnect()
}