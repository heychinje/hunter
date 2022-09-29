package com.jshunter.hunter.eventbus

interface Publisher {
    fun send(event: Event): Boolean
}