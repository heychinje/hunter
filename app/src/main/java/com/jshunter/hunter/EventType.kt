package com.jshunter.hunter

import com.jshunter.hunter.eventbus.Type

sealed class EventType : Type {
    object OnConnect : Type
    object OnDisconnect : Type
    object OnUpdateNotification : Type
}
