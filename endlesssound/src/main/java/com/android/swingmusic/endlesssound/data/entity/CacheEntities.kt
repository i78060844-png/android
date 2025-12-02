package com.android.swingmusic.endlesssound.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.android.swingmusic.endlesssound.domain.model.CacheTier
import com.android.swingmusic.endlesssound.domain.model.CachedTrack

@Entity(tableName = "cached_tracks")
data class CachedTrackEntity(
    @PrimaryKey
    val trackHash: String,
    val originalFilePath: String,
    val cacheFilePath: String,
    val cachedAt: Long,
    val expiresAt: Long,
    val tierOrdinal: Int,
    val replayCount: Int,
    val fileSizeBytes: Long,
    val lastAccessedAt: Long
) {
    fun toDomain(): CachedTrack = CachedTrack(
        trackHash = trackHash,
        filePath = originalFilePath,
        cacheFilePath = cacheFilePath,
        cachedAt = cachedAt,
        expiresAt = expiresAt,
        tier = CacheTier.entries[tierOrdinal],
        replayCount = replayCount,
        fileSizeBytes = fileSizeBytes,
        lastAccessedAt = lastAccessedAt
    )

    companion object {
        fun fromDomain(track: CachedTrack) = CachedTrackEntity(
            trackHash = track.trackHash,
            originalFilePath = track.filePath,
            cacheFilePath = track.cacheFilePath,
            cachedAt = track.cachedAt,
            expiresAt = track.expiresAt,
            tierOrdinal = track.tier.ordinal,
            replayCount = track.replayCount,
            fileSizeBytes = track.fileSizeBytes,
            lastAccessedAt = track.lastAccessedAt
        )
    }
}

/**
 * Tracks replay sessions to determine valid replays
 */
@Entity(tableName = "replay_sessions")
data class ReplaySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackHash: String,
    val startPositionSec: Int,
    val listenedDurationSec: Int,
    val timestamp: Long,
    val wasValidReplay: Boolean
)

/**
 * Cache hit/miss statistics
 */
@Entity(tableName = "cache_stats")
data class CacheStatsEntity(
    @PrimaryKey
    val date: String, // YYYY-MM-DD format
    val hits: Int = 0,
    val misses: Int = 0,
    val downloads: Int = 0,
    val bytesServed: Long = 0,
    val bytesDownloaded: Long = 0
)
