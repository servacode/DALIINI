package com.servacode.directory

import com.servacode.directory.core.network.RealtimeConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RealtimeConfigModule {
    @Provides
    @Singleton
    fun provideRealtimeConfig(): RealtimeConfig = RealtimeConfig(
        webSocketUrl = BuildConfig.REALTIME_WS_URL,
        allowCleartext = BuildConfig.ALLOW_CLEARTEXT,
    )
}
