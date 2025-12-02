package com.android.swingmusic.database.data.di

import com.android.swingmusic.core.domain.repository.MusicForYouRepository
import com.android.swingmusic.core.domain.repository.TrackCacheRepository
import com.android.swingmusic.database.data.repository.MusicForYouRepositoryImpl
import com.android.swingmusic.database.data.repository.TrackCacheRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MusicForYouModule {
    
    @Binds
    @Singleton
    abstract fun bindMusicForYouRepository(
        impl: MusicForYouRepositoryImpl
    ): MusicForYouRepository
    
    @Binds
    @Singleton
    abstract fun bindTrackCacheRepository(
        impl: TrackCacheRepositoryImpl
    ): TrackCacheRepository
}
