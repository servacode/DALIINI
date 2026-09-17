package com.servacode.directory.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkErrorMapperTest {
    @Test fun mapsAuthAndServerFailures() {
        assertEquals(NetworkError.Unauthorized, NetworkErrorMapper.fromStatus(401))
        assertEquals(NetworkError.Forbidden, NetworkErrorMapper.fromStatus(403))
        assertEquals(NetworkError.Server(503), NetworkErrorMapper.fromStatus(503))
    }
}
