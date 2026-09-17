package com.servacode.directory.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DirectoryDatabase =
        Room.databaseBuilder(context, DirectoryDatabase::class.java, "directory-cache.db").build()

    @Provides
    fun provideCacheDao(database: DirectoryDatabase): CacheDao = database.cacheDao()
}
