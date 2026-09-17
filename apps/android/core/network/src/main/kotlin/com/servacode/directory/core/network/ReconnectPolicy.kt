package com.servacode.directory.core.network

import kotlin.math.min

class ReconnectPolicy(
    private val baseMillis: Long = 1_000L,
    private val maxMillis: Long = 30_000L,
    private val jitterFraction: Double = 0.20,
) {
    fun delayMillis(attempt: Int, jitterUnit: Double): Long {
        require(attempt >= 0)
        require(jitterUnit in -1.0..1.0)
        val exponent = attempt.coerceAtMost(20)
        val raw = min(maxMillis, baseMillis * (1L shl exponent))
        val jitter = raw * jitterFraction * jitterUnit
        return (raw + jitter).toLong().coerceIn(0L, maxMillis)
    }
}
