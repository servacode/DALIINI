package com.servacode.directory

import com.servacode.directory.core.network.PushAvailability
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Features ask whether push is available; only the app knows how it was configured. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PushModule {
    @Binds
    abstract fun bindPushAvailability(setup: PushSetup): PushAvailability
}
