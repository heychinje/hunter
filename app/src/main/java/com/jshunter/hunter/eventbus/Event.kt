package com.jshunter.hunter.eventbus

import android.os.Bundle
import kotlinx.coroutines.CoroutineDispatcher

data class Event(
    val type: Type,
    val bundle: Bundle? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val targetFilter: ((Subscriber) -> Boolean) = { true },
    val targetDispatcher: CoroutineDispatcher? = null
)