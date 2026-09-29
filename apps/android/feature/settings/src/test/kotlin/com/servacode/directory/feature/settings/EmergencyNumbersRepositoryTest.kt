package com.servacode.directory.feature.settings

import com.servacode.directory.core.model.EmergencyScope
import com.servacode.directory.core.testing.FakeEmergencyNumbersCache
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.ScriptedPublicApi
import com.servacode.directory.core.testing.emergency
import com.servacode.directory.core.testing.missingEndpoint
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyNumbersRepositoryTest {
    private val api = ScriptedPublicApi()
    private val cache = FakeEmergencyNumbersCache()
    private val repository = EmergencyNumbersRepository(api, cache, FakePreferences("raqqa"), LABELS)

    private val ambulance = emergency("الإسعاف", "110")
    private val hospital = emergency("مستشفى الرقة", "022123456", EmergencyScope.PROVINCE, "raqqa")

    @Test fun `the platform's numbers are grouped, shown and kept for the province`() = runTest {
        api.emergencyAnswer = { listOf(ambulance, hospital) }

        val last = repository.load().toList().single() as EmergencyLoad.Fresh

        assertEquals(listOf(ambulance), last.numbers.national)
        assertEquals(listOf(hospital), last.numbers.province)
        assertEquals(listOf("emergency:raqqa"), api.calls)
        assertEquals(listOf(ambulance, hospital), cache.read("raqqa"))
    }

    @Test fun `offline, the kept numbers stay, marked stale`() = runTest {
        cache.write("raqqa", listOf(ambulance, hospital))

        val emitted = repository.load().toList()

        assertTrue(emitted.first() is EmergencyLoad.Cached)
        val stale = emitted.last() as EmergencyLoad.Stale
        assertEquals(listOf(hospital), stale.numbers.province)
    }

    @Test fun `nothing kept and no network gives the built-in numbers, flagged`() = runTest {
        val only = repository.load().toList().single() as EmergencyLoad.BuiltIn

        assertTrue(only.numbers.builtIn)
        assertEquals(listOf("110", "112", "113"), only.numbers.national.map { it.number })
    }

    @Test fun `a backend without the endpoint is treated like no network`() = runTest {
        api.emergencyAnswer = { throw missingEndpoint }

        assertTrue(repository.load().toList().single() is EmergencyLoad.BuiltIn)
    }

    @Test fun `an empty answer does not wipe what was kept`() = runTest {
        cache.write("raqqa", listOf(ambulance))
        api.emergencyAnswer = { emptyList() }

        assertTrue(repository.load().toList().last() is EmergencyLoad.Stale)
        assertEquals(listOf(ambulance), cache.read("raqqa"))
    }
}
