package com.servacode.directory.core.datastore

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/* The same file as before the preferences were shared: files/datastore/directory_preferences. */
private val Context.directoryDataStore by preferencesDataStore(name = DirectoryDataStore.NAME)

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideDirectoryDataStore(@ApplicationContext context: Context): DirectoryDataStore =
        DirectoryDataStore(context.directoryDataStore)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesBindings {
    @Binds
    @Singleton
    abstract fun bindPreferencesStore(impl: PreferencesRepository): DirectoryPreferencesStore
}
