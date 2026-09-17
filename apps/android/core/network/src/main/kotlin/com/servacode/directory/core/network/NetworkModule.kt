package com.servacode.directory.core.network

import com.servacode.directory.core.observability.NoOpObservability
import com.servacode.directory.core.observability.Observability
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideObservability(): Observability = NoOpObservability

    @Provides @Singleton
    fun providePublicApiBoundary(): PublicApiBoundary = UnboundGeneratedPublicApi

    @Provides @Singleton
    fun provideBaseHttpClient(
        accessTokenInterceptor: AccessTokenInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(accessTokenInterceptor)
        .build()
}
