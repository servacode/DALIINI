package com.servacode.directory.core.database

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CacheFirstTest {
    private val offline = AppException(AppError(AppError.Kind.OFFLINE))

    @Test fun `cached, then fresh, and the fresh value is written`() = runTest {
        val written = mutableListOf<String>()

        val emitted = cacheFirst(read = { "old" }, fetch = { "new" }, write = { written += it }).toList()

        assertEquals(listOf(Loaded.Cached("old"), Loaded.Fresh("new")), emitted)
        assertEquals(listOf("new"), written)
    }

    @Test fun `no cache goes straight to the backend`() = runTest {
        val emitted = cacheFirst<String>(read = { null }, fetch = { "new" }, write = {}).toList()

        assertEquals(listOf(Loaded.Fresh("new")), emitted)
    }

    @Test fun `a failed fetch keeps the cache and says why`() = runTest {
        val emitted = cacheFirst<String>(read = { "old" }, fetch = { throw offline }, write = {}).toList()

        assertEquals(listOf(Loaded.Cached("old"), Loaded.Stale("old", offline.error)), emitted)
    }

    @Test fun `nothing anywhere is a failure`() = runTest {
        val emitted = cacheFirst<String>(read = { null }, fetch = { throw offline }, write = {}).toList()

        assertEquals(listOf(Loaded.Failed(offline.error)), emitted)
    }

    @Test fun `a broken cache neither hides the backend's answer nor fails the screen`() = runTest {
        val emitted = cacheFirst(
            read = { error("corrupt") },
            fetch = { "new" },
            write = { error("disk full") },
        ).toList()

        assertEquals(listOf(Loaded.Fresh("new")), emitted)
    }
}
