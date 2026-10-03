package com.servacode.directory.ios

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room.Room
import androidx.sqlite.driver.NativeSQLiteDriver
import com.servacode.directory.core.auth.RefreshTokenVault
import com.servacode.directory.core.database.DirectoryDatabase
import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.datastore.DirectoryDataStore
import com.servacode.directory.core.location.LocationFix
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.location.LocationResult
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.network.NetworkMonitor
import com.servacode.directory.core.network.api.ApiEnvironment
import com.servacode.directory.feature.home.HomeLoad
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private const val DAMASCUS = "11111111-1111-4111-8111-111111111111"

/**
 * The iPhone's graph as the app wires it, with the network, the files and the position in memory:
 * what the shell's screens read reaches the backend through the shared transport and is kept in
 * the shared cache.
 */
class ShellGraphTest {
    private val requests = mutableListOf<String>()
    private val engine = MockEngine { request ->
        requests += request.url.encodedPath
        when (request.url.encodedPath) {
            "/api/v1/public/provinces/" -> respond(
                """{"items":[{"id":"$DAMASCUS","code":"damascus","nameAr":"دمشق","nameEn":"Damascus",
                "mapCenter":null}]}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
            else -> respond("", HttpStatusCode.ServiceUnavailable)
        }
    }
    private val database = Room.inMemoryDatabaseBuilder<DirectoryDatabase>()
        .setDriver(NativeSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
    private val graph = ShellGraph(
        environment = ApiEnvironment("https://api.example.test/"),
        engine = engine,
        vault = EmptyVault,
        file = DirectoryDataStore(MemoryPreferences()),
        database = database,
        location = NoLocation,
        network = Offline,
    )

    @AfterTest
    fun close() = database.close()

    @Test
    fun `the provinces come from the backend through the shared transport and are cached`() = runTest {
        val states = graph.provinces.provinces().toList()

        val fresh = assertIs<Loaded.Fresh<List<Province>>>(states.last())
        assertEquals(listOf("دمشق"), fresh.value.map { it.nameAr })
        assertEquals(listOf("/api/v1/public/provinces/"), requests)
        assertEquals(listOf(DAMASCUS), graph.cache.provinces().map { it.id })
    }

    @Test
    fun `choosing a province is kept in the preferences`() = runTest {
        graph.provinces.select(DAMASCUS)

        assertEquals(DAMASCUS, graph.preferences.values.first().selectedProvinceId)
    }

    @Test
    fun `without a province the home asks for one`() = runTest {
        assertEquals(HomeLoad.ProvinceRequired, graph.home().first())
    }
}

private object EmptyVault : RefreshTokenVault {
    override fun read(): String? = null
    override fun write(value: String) = Unit
    override fun clear() = Unit
}

private object NoLocation : LocationProvider {
    override suspend fun current(timeoutMillis: Long): LocationResult = LocationResult.PermissionDenied
    override fun lastKnown(): LocationFix? = null
    override fun updates(minTimeMillis: Long): Flow<LocationResult> = emptyFlow()
}

private object Offline : NetworkMonitor {
    override val online: Flow<Boolean> = flowOf(false)
    override val unmetered: Flow<Boolean> = flowOf(false)
}

private class MemoryPreferences : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    private val lock = Mutex()
    override val data: Flow<Preferences> = state
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        lock.withLock { transform(state.value).also { state.value = it } }
}
