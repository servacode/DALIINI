package com.servacode.directory.feature.bootstrap

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BootstrapModule {
    @Binds abstract fun bindBootstrapRepository(
        impl: DefaultBootstrapRepository,
    ): BootstrapRepository
}
