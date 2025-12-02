package com.android.swingmusic.endlesssound.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.android.swingmusic.endlesssound.data.dao.CachedTrackDao
import com.android.swingmusic.endlesssound.data.entity.CacheStatsEntity
import com.android.swingmusic.endlesssound.data.entity.CachedTrackEntity
import com.android.swingmusic.endlesssound.data.entity.ReplaySessionEntity

@Database(
    entities = [
        CachedTrackEntity::class,
        ReplaySessionEntity::class,
        CacheStatsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EndlessSoundDatabase : RoomDatabase() {
    abstract fun cachedTrackDao(): CachedTrackDao
}
