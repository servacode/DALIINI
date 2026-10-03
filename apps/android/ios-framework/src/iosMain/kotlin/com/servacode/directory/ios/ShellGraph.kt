package com.servacode.directory.ios

import com.servacode.directory.core.auth.AccessTokenStore
import com.servacode.directory.core.auth.KeychainRefreshTokenVault
import com.servacode.directory.core.auth.MemoryAccessTokenStore
import com.servacode.directory.core.auth.RefreshTokenVault
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.database.DirectoryDatabase
import com.servacode.directory.core.database.PublicCacheDataSource
import com.servacode.directory.core.database.iosDirectoryDatabase
import com.servacode.directory.core.datastore.DirectoryDataStore
import com.servacode.directory.core.datastore.PreferencesRepository
import com.servacode.directory.core.datastore.iosDirectoryDataStore
import com.servacode.directory.core.location.IosLocationProvider
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.network.BackendLocationNameResolver
import com.servacode.directory.core.network.IosNetworkMonitor
import com.servacode.directory.core.network.MaintenanceState
import com.servacode.directory.core.network.NetworkMonitor
import com.servacode.directory.core.network.api.ApiEnvironment
import com.servacode.directory.core.transport.ClientPlatform
import com.servacode.directory.core.transport.KtorPublicApi
import com.servacode.directory.core.transport.KtorRefreshGateway
import com.servacode.directory.core.transport.TransportClients
import com.servacode.directory.core.transport.darwinEngine
import com.servacode.directory.feature.home.HomeRepository
import com.servacode.directory.feature.home.HomeUseCase
import com.servacode.directory.feature.province.ProvinceRepository
import com.servacode.directory.feature.province.ProvinceUseCase
import io.ktor.client.engine.HttpClientEngine

/**
 * The iPhone app's graph, wired by hand: what Hilt builds on Android, built here once per
 * process. Every class in it is the shared one; only the platform parts are the iPhone's
 * (DECISIONS 092 and 093).
 */
internal class ShellGraph(
    environment: ApiEnvironment,
    engine: HttpClientEngine,
    vault: RefreshTokenVault,
    file: DirectoryDataStore,
    database: DirectoryDatabase,
    val location: LocationProvider,
    val network: NetworkMonitor,
) {
    val maintenance = MaintenanceState()
    val accessTokens: AccessTokenStore = MemoryAccessTokenStore()

    // The clients ask the coordinator to refresh, and the coordinator refreshes through them.
    private lateinit var sessionCoordinator: SessionCoordinator
    val clients = TransportClients(
        environment,
        engine,
        ClientPlatform.IOS,
        accessTokens,
        maintenance,
    ) { sessionCoordinator }
    val session: SessionCoordinator

    init {
        sessionCoordinator = SessionCoordinator(accessTokens, vault, KtorRefreshGateway(clients))
        session = sessionCoordinator
    }

    val publicApi = KtorPublicApi(clients)
    val preferences = PreferencesRepository(file)
    val cache = PublicCacheDataSource(database.cacheDao())

    val provinces = ProvinceUseCase(ProvinceRepository(cache, publicApi, preferences))
    val home = HomeUseCase(
        HomeRepository(cache, publicApi, preferences, location, BackendLocationNameResolver(publicApi)),
    )

    companion object {
        /** The app's graph on the phone's own Keychain, files, position and network. */
        fun onDevice(configuration: ShellConfiguration): ShellGraph = ShellGraph(
            environment = ApiEnvironment(configuration.apiBaseUrl, configuration.allowCleartext),
            engine = darwinEngine(),
            vault = KeychainRefreshTokenVault(),
            file = iosDirectoryDataStore,
            database = iosDirectoryDatabase,
            location = IosLocationProvider(),
            network = IosNetworkMonitor(),
        )
    }
}
