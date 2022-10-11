package com.jshunter.hunter.eventbus.impl

import android.util.Log
import com.jshunter.hunter.eventbus.Event
import com.jshunter.hunter.eventbus.Bus
import com.jshunter.hunter.eventbus.Publisher
import com.jshunter.hunter.eventbus.Subscriber
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import java.util.*

object EventBus : Bus, Publisher,
    CoroutineScope by CoroutineScope(Dispatchers.Default + SupervisorJob() + EventBusCoroutineExceptionHandler) {

    private val eventFlow = MutableSharedFlow<Event>(replay = 1)

    private val subscribers by lazy { Collections.synchronizedCollection(mutableListOf<Subscriber>()) }

    init {
        launch {
            eventFlow.collect { event ->
                Log.e(TAG, "event: $event ")
                subscribers.filter { event.targetFilter(it) }.forEach {
                    val specifiedDispatcher = event.targetDispatcher
                    if (specifiedDispatcher != null) launch(specifiedDispatcher) { it.onEvent(event) }
                    else it.onEvent(event)
                }
            }
        }
    }

    override fun register(subscriber: Subscriber) = subscribers.add(subscriber)

    override fun unregister(subscriber: Subscriber) = subscribers.remove(subscriber)

    override fun send(event: Event) = eventFlow.tryEmit(event)
}