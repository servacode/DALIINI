package com.servacode.directory.core.network

import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class RealtimeInvalidationBus @Inject constructor() {
    private val mutable = MutableSharedFlow<RealtimeEnvelope>(extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeEnvelope> = mutable.asSharedFlow()

    fun publish(event: RealtimeEnvelope) {
        mutable.tryEmit(event)
    }
}
