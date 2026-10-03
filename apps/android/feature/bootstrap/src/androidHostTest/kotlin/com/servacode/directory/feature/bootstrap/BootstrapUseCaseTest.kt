package com.servacode.directory.feature.bootstrap

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BootstrapUseCaseTest {
    @Test fun delegatesToRepository() = runTest {
        val expected = BootstrapResult.Ready("province-1", StartDestination.HOME)
        val repository = object : BootstrapRepository {
            override suspend fun initialize(): BootstrapResult = expected
        }
        assertEquals(expected, BootstrapUseCase(repository)())
    }
}
