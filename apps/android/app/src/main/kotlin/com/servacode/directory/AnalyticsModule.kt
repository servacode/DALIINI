package com.servacode.directory

import com.servacode.directory.core.analytics.AnalyticsTracker
import com.servacode.directory.core.analytics.AnalyticsTransport
import com.servacode.directory.core.analytics.AnonymousId
import com.servacode.directory.core.analytics.NoOpAnalyticsTracker
import com.servacode.directory.core.analytics.QueuedAnalyticsTracker
import com.servacode.directory.core.datastore.StoredAnonymousId
import com.servacode.directory.core.network.AnonymousApi
import com.servacode.directory.core.network.api.GeneratedAnalyticsTransport
import com.servacode.directory.core.network.api.GeneratedClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Where product measurement is assembled, and the one place that decides whether it happens.
 *
 * The app is wired to the platform's own endpoint rather than to a third-party SDK. That is a
 * deliberate choice and it buys three things: no extra dependency in the app, no data about the
 * people using it leaving our own servers, and a registry on the server that refuses an event it
 * does not recognise — so a measurement that drifts out of step fails loudly instead of filling a
 * table with the wrong thing.
 *
 * Builds with no configured API address get the tracker that does nothing, so a screen's call
 * site is identical either way and a debug build is not quietly reporting anybody's behaviour.
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideTransport(@AnonymousApi anonymous: GeneratedClient): AnalyticsTransport =
        GeneratedAnalyticsTransport(anonymous)

    @Provides
    @Singleton
    fun provideAnonymousId(stored: StoredAnonymousId): AnonymousId = stored

    /**
     * A scope of its own, on IO, that outlives every screen.
     *
     * `SupervisorJob` so one failed send cannot cancel the collector and silence everything after
     * it. It is never cancelled: it lives exactly as long as the process, which is the lifetime
     * of the thing being measured.
     */
    @Provides
    @Singleton
    @AnalyticsScope
    fun provideScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("analytics"))

    @Provides
    @Singleton
    fun provideTracker(
        transport: AnalyticsTransport,
        anonymousId: AnonymousId,
        @AnalyticsScope scope: CoroutineScope,
    ): AnalyticsTracker =
        if (BuildConfig.API_BASE_URL.isBlank()) {
            NoOpAnalyticsTracker
        } else {
            QueuedAnalyticsTracker(transport, anonymousId, scope)
        }
}

/** Distinguishes the analytics scope from any other `CoroutineScope` the graph may hold. */
@javax.inject.Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AnalyticsScope
