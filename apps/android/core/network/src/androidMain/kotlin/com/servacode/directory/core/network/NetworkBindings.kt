package com.servacode.directory.core.network

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindings {
    @Binds
    @Singleton
    abstract fun bindRealtimeStream(impl: OkHttpRealtimeStream): RealtimeStream

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: AndroidNetworkMonitor): NetworkMonitor

    @Binds
    @Singleton
    abstract fun bindLocationNameResolver(impl: BackendLocationNameResolver): LocationNameResolver
}
