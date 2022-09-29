package com.jshunter.hunter.eventbus

interface Subscriber {
    fun onEvent(event: Event)
}