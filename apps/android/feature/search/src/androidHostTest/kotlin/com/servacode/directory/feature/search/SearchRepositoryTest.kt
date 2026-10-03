package com.servacode.directory.feature.search

import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.Page
import com.servacode.directory.core.testing.FakeLocation
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.ScriptedPublicApi
import com.servacode.directory.core.testing.facility
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRepositoryTest {
    private val api = ScriptedPublicApi()
    private val repository = SearchRepository(api, FakePreferences("raqqa"), FakeLocation())

    @Test fun `an empty query sends nothing`() = runTest {
        assertNull(repository.first("   "))
        assertTrue(api.calls.isEmpty())
    }

    @Test fun `the backend searches and pages, and the app passes its cursor back untouched`() = runTest {
        api.searchAnswer = { _, cursor ->
            if (cursor == null) Page(listOf(facility("a")), "Y3Vyc29y%3D=", true)
            else Page(listOf(facility("b")), null, false)
        }

        val (context, first) = repository.first("  صيدلية   الشفاء ")!!
        val second = repository.next(context, first.nextCursor)

        assertEquals("صيدلية الشفاء", context.query)
        assertEquals(listOf("a"), first.items.map { it.id })
        assertEquals(listOf("b"), second.items.map { it.id })
        assertEquals(listOf("search:صيدلية الشفاء:null", "search:صيدلية الشفاء:Y3Vyc29y%3D="), api.calls)
    }

    @Test fun `a failure surfaces as an app error`() = runTest {
        val failure = runCatching { repository.first("x") }.exceptionOrNull()

        assertTrue(failure is AppException)
    }
}
