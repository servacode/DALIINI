package com.servacode.directory.core.observability

interface Observability {
    fun recordRequest(requestId: String, route: String, statusCode: Int, durationMs: Long)
    fun recordError(kind: String, requestId: String? = null)
}

object NoOpObservability : Observability {
    override fun recordRequest(requestId: String, route: String, statusCode: Int, durationMs: Long) = Unit
    override fun recordError(kind: String, requestId: String?) = Unit
}
