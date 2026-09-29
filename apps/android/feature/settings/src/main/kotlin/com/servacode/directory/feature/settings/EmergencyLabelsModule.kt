package com.servacode.directory.feature.settings

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

/** The built-in emergency lines' names, read once from this module's resources. */
@Module
@InstallIn(SingletonComponent::class)
object EmergencyLabelsModule {
    @Provides
    fun builtInEmergencyLabels(@ApplicationContext context: Context): BuiltInEmergencyLabels =
        BuiltInEmergencyLabels(
            ambulance = context.getString(R.string.emergency_builtin_ambulance),
            police = context.getString(R.string.emergency_builtin_police),
            fire = context.getString(R.string.emergency_builtin_fire),
        )
}
