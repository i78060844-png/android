package com.android.swingmusic.database.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing listening events
 */
@Entity(
    tableName = "listening_events",
    indices = [
        Index(value = ["trackHash"]),
        Index(value = ["timestamp"]),
        Index(value = ["hourOfDay"]),
        Index(value = ["dayOfWeek"]),
        Index(value = ["albumHash"]),
    ]
)
data class ListeningEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackHash: String,
    val albumHash: String,
    val artistHashes: String, // Comma-separated
    val timestamp: Long,
    val dayOfWeek: Int,
    val hourOfDay: Int,
    val listenDurationMs: Long,
    val trackDurationMs: Long,
    val completedPlay: Boolean,
    val skipped: Boolean,
    val source: String
)

/**
 * Aggregated track statistics cache
 */
@Entity(
    tableName = "track_stats_cache",
    indices = [Index(value = ["lastUpdated"])]
)
data class TrackStatsCacheEntity(
    @PrimaryKey
    val trackHash: String,
    val totalPlays: Int,
    val completedPlays: Int,
    val skips: Int,
    val totalListenTimeMs: Long,
    val lastPlayedAt: Long,
    val firstPlayedAt: Long,
    val averageCompletionRate: Float,
    val playsByHourJson: String, // JSON map
    val playsByDayOfWeekJson: String, // JSON map
    val streak: Int,
    val currentStreakStartDate: Long?,
    val lastUpdated: Long
)

/**
 * Aggregated artist statistics cache
 */
@Entity(
    tableName = "artist_stats_cache",
    indices = [Index(value = ["totalPlays"])]
)
data class ArtistStatsCacheEntity(
    @PrimaryKey
    val artistHash: String,
    val totalPlays: Int,
    val uniqueTracksPlayed: Int,
    val totalListenTimeMs: Long,
    val lastPlayedAt: Long,
    val favoriteTimeOfDay: Int?,
    val favoriteDayOfWeek: Int?,
    val lastUpdated: Long
)

/**
 * Aggregated album statistics cache
 */
@Entity(
    tableName = "album_stats_cache"
)
data class AlbumStatsCacheEntity(
    @PrimaryKey
    val albumHash: String,
    val totalPlays: Int,
    val uniqueTracksPlayed: Int,
    val totalListenTimeMs: Long,
    val lastPlayedAt: Long,
    val completionRate: Float,
    val lastUpdated: Long
)

/**
 * Stores detected listening patterns
 */
@Entity(
    tableName = "listening_patterns",
    primaryKeys = ["hourOfDay", "dayOfWeek"]
)
data class ListeningPatternEntity(
    val hourOfDay: Int,
    val dayOfWeek: Int,
    val avgPlaysPerSession: Float,
    val commonTrackHashes: String, // Comma-separated top tracks for this slot
    val totalSessions: Int,
    val lastUpdated: Long
)
