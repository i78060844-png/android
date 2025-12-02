package com.android.swingmusic.database.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.android.swingmusic.database.data.dao.BaseUrlDao
import com.android.swingmusic.database.data.dao.LastPlayedTrackDao
import com.android.swingmusic.database.data.dao.ListeningHistoryDao
import com.android.swingmusic.database.data.dao.QueueDao
import com.android.swingmusic.database.data.dao.UserDao
import com.android.swingmusic.database.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create listening_events table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS listening_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    trackHash TEXT NOT NULL,
                    albumHash TEXT NOT NULL,
                    artistHashes TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    dayOfWeek INTEGER NOT NULL,
                    hourOfDay INTEGER NOT NULL,
                    listenDurationMs INTEGER NOT NULL,
                    trackDurationMs INTEGER NOT NULL,
                    completedPlay INTEGER NOT NULL,
                    skipped INTEGER NOT NULL,
                    source TEXT NOT NULL
                )
            """.trimIndent())
            
            db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_events_trackHash ON listening_events(trackHash)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_events_timestamp ON listening_events(timestamp)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_events_hourOfDay ON listening_events(hourOfDay)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_events_dayOfWeek ON listening_events(dayOfWeek)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_events_albumHash ON listening_events(albumHash)")
            
            // Create track_stats_cache table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS track_stats_cache (
                    trackHash TEXT PRIMARY KEY NOT NULL,
                    totalPlays INTEGER NOT NULL,
                    completedPlays INTEGER NOT NULL,
                    skips INTEGER NOT NULL,
                    totalListenTimeMs INTEGER NOT NULL,
                    lastPlayedAt INTEGER NOT NULL,
                    firstPlayedAt INTEGER NOT NULL,
                    averageCompletionRate REAL NOT NULL,
                    playsByHourJson TEXT NOT NULL,
                    playsByDayOfWeekJson TEXT NOT NULL,
                    streak INTEGER NOT NULL,
                    currentStreakStartDate INTEGER,
                    lastUpdated INTEGER NOT NULL
                )
            """.trimIndent())
            
            db.execSQL("CREATE INDEX IF NOT EXISTS index_track_stats_cache_lastUpdated ON track_stats_cache(lastUpdated)")
            
            // Create artist_stats_cache table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS artist_stats_cache (
                    artistHash TEXT PRIMARY KEY NOT NULL,
                    totalPlays INTEGER NOT NULL,
                    uniqueTracksPlayed INTEGER NOT NULL,
                    totalListenTimeMs INTEGER NOT NULL,
                    lastPlayedAt INTEGER NOT NULL,
                    favoriteTimeOfDay INTEGER,
                    favoriteDayOfWeek INTEGER,
                    lastUpdated INTEGER NOT NULL
                )
            """.trimIndent())
            
            db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_stats_cache_totalPlays ON artist_stats_cache(totalPlays)")
            
            // Create album_stats_cache table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS album_stats_cache (
                    albumHash TEXT PRIMARY KEY NOT NULL,
                    totalPlays INTEGER NOT NULL,
                    uniqueTracksPlayed INTEGER NOT NULL,
                    totalListenTimeMs INTEGER NOT NULL,
                    lastPlayedAt INTEGER NOT NULL,
                    completionRate REAL NOT NULL,
                    lastUpdated INTEGER NOT NULL
                )
            """.trimIndent())
            
            // Create listening_patterns table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS listening_patterns (
                    hourOfDay INTEGER NOT NULL,
                    dayOfWeek INTEGER NOT NULL,
                    avgPlaysPerSession REAL NOT NULL,
                    commonTrackHashes TEXT NOT NULL,
                    totalSessions INTEGER NOT NULL,
                    lastUpdated INTEGER NOT NULL,
                    PRIMARY KEY(hourOfDay, dayOfWeek)
                )
            """.trimIndent())
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "app_database"
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideBaseUrlDao(database: AppDatabase): BaseUrlDao {
        return database.baseUrlDao()
    }

    @Provides
    fun provideTrackDao(database: AppDatabase): QueueDao {
        return database.queueDao()
    }

    @Provides
    fun provideLastPlayedTrackDao(database: AppDatabase): LastPlayedTrackDao {
        return database.lastPlayedTrackDao()
    }
    
    @Provides
    fun provideListeningHistoryDao(database: AppDatabase): ListeningHistoryDao {
        return database.listeningHistoryDao()
    }
}
