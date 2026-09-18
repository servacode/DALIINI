package com.servacode.directory

import android.os.Build
import com.servacode.directory.core.network.api.ApiEnvironment
import com.servacode.directory.core.network.api.ClientIdentity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The backend address and cleartext permission of the flavor this build was made for. */
@Module
@InstallIn(SingletonComponent::class)
object ApiConfigModule {
    @Provides
    @Singleton
    fun provideApiEnvironment(): ApiEnvironment = ApiEnvironment(
        baseUrl = BuildConfig.API_BASE_URL,
        allowCleartext = BuildConfig.ALLOW_CLEARTEXT,
    )

    /** Shown to the user in their list of signed-in devices. */
    @Provides
    @Singleton
    fun provideClientIdentity(): ClientIdentity =
        ClientIdentity(deviceName = listOf(Build.MANUFACTURER, Build.MODEL).joinToString(" ").take(120))
}
