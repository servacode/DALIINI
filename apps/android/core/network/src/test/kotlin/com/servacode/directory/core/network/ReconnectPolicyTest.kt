package com.servacode.directory.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconnectPolicyTest {
    @Test
    fun `backoff is exponential and capped`() {
        val policy = ReconnectPolicy(baseMillis = 1_000L, maxMillis = 30_000L, jitterFraction = 0.0)
        assertEquals(1_000L, policy.delayMillis(0, 0.0))
        assertEquals(2_000L, policy.delayMillis(1, 0.0))
        assertEquals(4_000L, policy.delayMillis(2, 0.0))
        assertEquals(30_000L, policy.delayMillis(20, 0.0))
    }

    @Test
    fun `jitter remains inside cap`() {
        val policy = ReconnectPolicy()
        assertTrue(policy.delayMillis(10, 1.0) <= 30_000L)
        assertTrue(policy.delayMillis(10, -1.0) >= 0L)
    }
}
