package com.servacode.directory.ios

import com.servacode.directory.core.analytics.AnalyticsTracker
import com.servacode.directory.core.analytics.NoOpAnalyticsTracker
import com.servacode.directory.core.auth.AccessTokenStore
import com.servacode.directory.core.auth.KeychainRefreshTokenVault
import com.servacode.directory.core.auth.MemoryAccessTokenStore
import com.servacode.directory.core.auth.RefreshTokenVault
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.database.DirectoryDatabase
import com.servacode.directory.core.database.PublicCacheDataSource
import com.servacode.directory.core.database.RoomRecentlyViewedStore
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
import com.servacode.directory.core.network.RealtimeInvalidationBus
import com.servacode.directory.core.network.SignOut
import com.servacode.directory.core.network.api.ApiEnvironment
import com.servacode.directory.core.transport.ClientPlatform
import com.servacode.directory.core.transport.KtorAuthApi
import com.servacode.directory.core.transport.KtorOwnerApi
import com.servacode.directory.core.transport.KtorPublicApi
import com.servacode.directory.core.transport.KtorRefreshGateway
import com.servacode.directory.core.transport.TransportClients
import com.servacode.directory.core.transport.darwinEngine
import com.servacode.directory.feature.account.AccountRepository
import com.servacode.directory.feature.account.AccountUseCase
import com.servacode.directory.feature.account.DeleteAccountUseCase
import com.servacode.directory.feature.account.SavedRepository
import com.servacode.directory.feature.auth.AuthRepository
import com.servacode.directory.feature.duty.DutyRepository
import com.servacode.directory.feature.facility.FacilityRepository
import com.servacode.directory.feature.facility.FacilityUseCase
import com.servacode.directory.feature.facility.RecordVisitUseCase
import com.servacode.directory.feature.facility.ReportFacilityUseCase
import com.servacode.directory.feature.home.HomeAdsRepository
import com.servacode.directory.feature.home.HomeAdsUseCase
import com.servacode.directory.feature.home.HomeRepository
import com.servacode.directory.feature.home.HomeUseCase
import com.servacode.directory.feature.province.ProvinceRepository
import com.servacode.directory.feature.province.ProvinceUseCase
import com.servacode.directory.feature.ratings.RatingsRepository
import com.servacode.directory.feature.ratings.RatingsUseCase
import com.servacode.directory.feature.search.SearchRepository
import com.servacode.directory.feature.search.SearchUseCase
import io.ktor.client.engine.HttpClientEngine
import platform.UIKit.UIDevice

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
    /** The site's host, for a facility's shared link; blank in a build that was not given one. */
    val appLinkHost: String = "",
    /** What the backend lists this phone as among the account's sessions. */
    deviceName: String = "iPhone",
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

    /** Nothing is measured from the iPhone yet: the analytics destination is Android's. */
    val analytics: AnalyticsTracker = NoOpAnalyticsTracker
    /** The realtime connection is Android's for now; the bus is here so the home can listen. */
    val invalidations = RealtimeInvalidationBus()
    val recentlyViewed = RoomRecentlyViewedStore(database.localStoresDao())

    val provinces = ProvinceUseCase(ProvinceRepository(cache, publicApi, preferences))
    val home = HomeUseCase(
        HomeRepository(cache, publicApi, preferences, location, BackendLocationNameResolver(publicApi)),
    )
    val homeAds = HomeAdsUseCase(HomeAdsRepository(cache, publicApi))
    val search = SearchUseCase(SearchRepository(publicApi, preferences, location))
    private val facilities = FacilityRepository(cache, publicApi, preferences)
    val facility = FacilityUseCase(facilities)
    val reportFacility = ReportFacilityUseCase(facilities)
    val recordVisit = RecordVisitUseCase(recentlyViewed)

    private val authApi = KtorAuthApi(clients, deviceName)
    private val ownerApi = KtorOwnerApi(clients)
    val auth = AuthRepository(authApi, session)
    private val accounts = AccountRepository(publicApi, ownerApi, session, preferences, SignOut(authApi, session))
    val account = AccountUseCase(accounts)
    val deleteAccount = DeleteAccountUseCase(accounts)
    val saved = SavedRepository(publicApi)
    val ratings = RatingsUseCase(RatingsRepository(publicApi))
    val duty = DutyRepository(ownerApi)

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
            appLinkHost = configuration.appLinkHost,
            deviceName = UIDevice.currentDevice.model,
        )
    }
}
