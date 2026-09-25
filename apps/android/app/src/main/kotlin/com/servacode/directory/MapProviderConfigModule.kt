package com.servacode.directory

import com.servacode.directory.core.maps.MapProviderConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapProviderConfigModule {
    @Provides
    @Singleton
    fun provideMapProviderConfig(): MapProviderConfig = MapProviderConfig(
        routingBaseUrl = BuildConfig.ROUTING_BASE_URL,
        geocodingBaseUrl = BuildConfig.GEOCODING_BASE_URL,
        geocodingUserAgent = BuildConfig.GEOCODING_USER_AGENT,
        styleUrl = BuildConfig.MAP_STYLE_URL,
    )
}
