package com.servacode.directory.core.network

import com.servacode.directory.core.auth.AccessTokenStore
import com.servacode.directory.core.auth.RefreshTokenVault
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.network.api.ApiEnvironment
import com.servacode.directory.core.network.api.ClientIdentity
import com.servacode.directory.core.network.api.GeneratedAuthApi
import com.servacode.directory.core.network.api.GeneratedClient
import com.servacode.directory.core.network.api.GeneratedMaintenanceProbe
import com.servacode.directory.core.network.api.GeneratedOwnerApi
import com.servacode.directory.core.network.api.GeneratedPublicApi
import com.servacode.directory.core.network.api.GeneratedPushRegistration
import com.servacode.directory.core.network.api.GeneratedRefreshGateway
import com.servacode.directory.core.observability.NoOpObservability
import com.servacode.directory.core.observability.Observability
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** Requests that carry no user token: discovery, sign-in, registration, recovery, refresh. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AnonymousApi

/** Requests made as the signed-in user, with refresh on 401. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthorizedApi

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideObservability(): Observability = NoOpObservability

    /** The shared base: timeouts and the connection pool. Used as-is by the WebSocket. */
    @Provides @Singleton
    fun provideBaseHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        // Uploads of up to 10 MiB over a slow mobile link.
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Its own dispatcher, deliberately. The refresh authenticator blocks the thread of a
     * request that got a 401 until the refresh call returns. If that call queued behind the
     * same dispatcher's per-host limit — five requests, all waiting in the authenticator —
     * nothing would ever run again.
     */
    @Provides @Singleton @AnonymousApi
    fun provideAnonymousHttpClient(
        base: OkHttpClient,
        requestIds: RequestIdInterceptor,
        maintenance: MaintenanceInterceptor,
    ): OkHttpClient =
        base.newBuilder()
            .dispatcher(Dispatcher())
            .addInterceptor(requestIds)
            .addInterceptor(maintenance)
            .build()

    @Provides @Singleton @AnonymousApi
    fun provideAnonymousClient(environment: ApiEnvironment, @AnonymousApi http: OkHttpClient): GeneratedClient =
        GeneratedClient(environment, http)

    @Provides @Singleton
    fun provideSessionCoordinator(
        accessTokens: AccessTokenStore,
        refreshTokens: RefreshTokenVault,
        @AnonymousApi client: GeneratedClient,
    ): SessionCoordinator = SessionCoordinator(accessTokens, refreshTokens, GeneratedRefreshGateway(client))

    @Provides @Singleton @AuthorizedApi
    fun provideAuthorizedHttpClient(
        base: OkHttpClient,
        requestIds: RequestIdInterceptor,
        accessTokens: AccessTokenInterceptor,
        session: SessionCoordinator,
        maintenance: MaintenanceInterceptor,
    ): OkHttpClient = base.newBuilder()
        .dispatcher(Dispatcher())
        .addInterceptor(requestIds)
        .addInterceptor(maintenance)
        .addInterceptor(accessTokens)
        .authenticator(RefreshAuthenticator(session))
        .build()

    @Provides @Singleton @AuthorizedApi
    fun provideAuthorizedClient(environment: ApiEnvironment, @AuthorizedApi http: OkHttpClient): GeneratedClient =
        GeneratedClient(environment, http)

    @Provides @Singleton
    fun providePublicApiBoundary(
        @AnonymousApi anonymous: GeneratedClient,
        @AuthorizedApi authorized: GeneratedClient,
    ): PublicApiBoundary = GeneratedPublicApi(anonymous, authorized)

    @Provides @Singleton
    fun provideOwnerApiBoundary(@AuthorizedApi authorized: GeneratedClient): OwnerApiBoundary =
        GeneratedOwnerApi(authorized)

    @Provides @Singleton
    fun provideAuthApiBoundary(
        @AnonymousApi anonymous: GeneratedClient,
        @AuthorizedApi authorized: GeneratedClient,
        identity: ClientIdentity,
    ): AuthApiBoundary = GeneratedAuthApi(anonymous, authorized, identity.deviceName)

    @Provides @Singleton
    fun providePushRegistrationBoundary(@AuthorizedApi authorized: GeneratedClient): PushRegistrationBoundary =
        GeneratedPushRegistration(authorized)

    /** Maintenance ends when this answers normally; see [MaintenanceCoordinator]. */
    @Provides @Singleton
    fun provideMaintenanceProbe(
        environment: ApiEnvironment,
        @AnonymousApi http: OkHttpClient,
        @AnonymousApi anonymous: GeneratedClient,
        state: MaintenanceState,
    ): MaintenanceProbe = GeneratedMaintenanceProbe(environment, http, anonymous, state)
}
