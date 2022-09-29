package com.jshunter.hunter.eventbus

interface Bus {
    fun register(subscriber: Subscriber): Boolean
    fun unregister(subscriber: Subscriber): Boolean
}