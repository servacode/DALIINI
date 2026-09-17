package com.servacode.directory.core.network

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeEventDeduplicator @Inject constructor() {
    private val keys = LinkedHashSet<String>()
    private val maxEntries = 128

    @Synchronized
    fun shouldDeliver(event: RealtimeEnvelope): Boolean {
        val key = listOf(
            event.version,
            event.name,
            event.scope.type,
            event.scope.id,
            event.resourceId.orEmpty(),
            event.occurredAt,
        ).joinToString("|")
        if (!keys.add(key)) return false
        while (keys.size > maxEntries) {
            val first = keys.firstOrNull() ?: break
            keys.remove(first)
        }
        return true
    }
}
