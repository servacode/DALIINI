package com.servacode.directory.feature.settings

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The built-in emergency lines' names, read from this module's shared words when they are needed. */
@Module
@InstallIn(SingletonComponent::class)
object EmergencyLabelsModule {
    @Provides
    fun emergencyLabels(): EmergencyLabelSource = ResourceEmergencyLabels
}
