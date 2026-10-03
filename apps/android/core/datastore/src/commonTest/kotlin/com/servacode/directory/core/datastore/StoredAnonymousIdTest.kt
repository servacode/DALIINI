package com.servacode.directory.core.datastore

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class StoredAnonymousIdTest {
    private val uuid = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")

    @Test
    fun `the id is a random version 4 UUID in lower case`() = runTest {
        val id = StoredAnonymousId(DirectoryDataStore(MemoryDataStore())).current()

        assertTrue(uuid.matches(id), id)
    }

    @Test
    fun `the id is kept once made`() = runTest {
        val stored = StoredAnonymousId(DirectoryDataStore(MemoryDataStore()))

        assertEquals(stored.current(), stored.current())
    }

    @Test
    fun `callers racing on the first launch agree on one id`() = runTest {
        val stored = StoredAnonymousId(DirectoryDataStore(MemoryDataStore()))

        val ids = (1..8).map { async { stored.current() } }.awaitAll()

        assertEquals(1, ids.toSet().size, ids.toString())
    }

    @Test
    fun `another device makes another id`() = runTest {
        val first = StoredAnonymousId(DirectoryDataStore(MemoryDataStore())).current()
        val second = StoredAnonymousId(DirectoryDataStore(MemoryDataStore())).current()

        assertNotEquals(first, second)
    }
}
