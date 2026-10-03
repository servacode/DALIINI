package com.servacode.directory.core.auth

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindings {
    @Binds @Singleton
    abstract fun bindAccessTokenStore(impl: MemoryAccessTokenStore): AccessTokenStore

    @Binds @Singleton
    abstract fun bindRefreshTokenVault(impl: AndroidKeyStoreRefreshTokenVault): RefreshTokenVault
}
