package com.servacode.directory.core.network

import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.inject.Singleton
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

@Singleton
class RealtimeEventDeduplicator @Inject constructor() {
    private val keys = LinkedHashSet<String>()
    private val maxEntries = 128
    private val lock = SynchronizedObject()

    fun shouldDeliver(event: RealtimeEnvelope): Boolean = synchronized(lock) {
        val key = listOf(
            event.version,
            event.name,
            event.scope.type,
            event.scope.id,
            event.resourceId.orEmpty(),
            event.occurredAt,
        ).joinToString("|")
        if (!keys.add(key)) return@synchronized false
        while (keys.size > maxEntries) {
            val first = keys.firstOrNull() ?: break
            keys.remove(first)
        }
        true
    }
}
