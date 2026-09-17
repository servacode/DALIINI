package com.servacode.directory.core.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeInvalidationBus @Inject constructor() {
    private val mutable = MutableSharedFlow<RealtimeEnvelope>(extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeEnvelope> = mutable.asSharedFlow()

    fun publish(event: RealtimeEnvelope) {
        mutable.tryEmit(event)
    }
}
