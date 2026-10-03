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
        Room.databaseBuilder(context, DirectoryDatabase::class.java, DirectoryDatabase.FILE_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideCacheDao(database: DirectoryDatabase): CacheDao = database.cacheDao()

    @Provides @Singleton
    fun providePublicCache(source: PublicCacheDataSource): PublicCache = source

    @Provides
    fun provideLocalStoresDao(database: DirectoryDatabase): LocalStoresDao = database.localStoresDao()

    @Provides @Singleton
    fun provideRecentlyViewedStore(store: RoomRecentlyViewedStore): RecentlyViewedStore = store

    @Provides @Singleton
    fun provideEmergencyNumbersCache(cache: RoomEmergencyNumbersCache): EmergencyNumbersCache = cache
}
