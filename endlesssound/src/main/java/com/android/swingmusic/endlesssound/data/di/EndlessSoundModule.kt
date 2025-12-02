package com.android.swingmusic.endlesssound.data.di

import android.content.Context
import androidx.room.Room
import com.android.swingmusic.endlesssound.data.dao.CachedTrackDao
import com.android.swingmusic.endlesssound.data.database.EndlessSoundDatabase
import com.android.swingmusic.endlesssound.data.repository.EndlessSoundRepositoryImpl
import com.android.swingmusic.endlesssound.domain.repository.EndlessSoundRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EndlessSoundDatabaseModule {
    
    @Provides
    @Singleton
    fun provideEndlessSoundDatabase(
        @ApplicationContext context: Context
    ): EndlessSoundDatabase {
        return Room.databaseBuilder(
            context,
            EndlessSoundDatabase::class.java,
            "endless_sound_db"
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideCachedTrackDao(database: EndlessSoundDatabase): CachedTrackDao {
        return database.cachedTrackDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class EndlessSoundRepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindEndlessSoundRepository(
        impl: EndlessSoundRepositoryImpl
    ): EndlessSoundRepository
}
