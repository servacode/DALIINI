package com.servacode.directory.core.network

import com.servacode.directory.core.maps.GeocodingProvider
import com.servacode.directory.core.maps.RoutingProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapProviderNetworkModule {
    @Provides
    @Singleton
    @MapProviderHttpClient
    fun provideMapProviderHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    /**
     * Valhalla is the engine, and [OsrmRoutingProvider] stays where it is as the one we came
     * from. OSRM compiles one profile into its graph, so walking and riding could only ever be
     * driving under another name; Valhalla answers all three from the same tiles.
     */
    @Provides
    @Singleton
    fun provideRoutingProvider(impl: ValhallaRoutingProvider): RoutingProvider = impl

    @Provides
    @Singleton
    fun provideGeocodingProvider(impl: NominatimGeocodingProvider): GeocodingProvider = impl
}
