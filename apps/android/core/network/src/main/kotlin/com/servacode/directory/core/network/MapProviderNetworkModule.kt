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

    @Provides
    @Singleton
    fun provideRoutingProvider(impl: OsrmRoutingProvider): RoutingProvider = impl

    @Provides
    @Singleton
    fun provideGeocodingProvider(impl: NominatimGeocodingProvider): GeocodingProvider = impl
}
