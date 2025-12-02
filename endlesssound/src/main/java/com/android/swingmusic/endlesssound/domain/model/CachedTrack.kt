package com.android.swingmusic.endlesssound.domain.model

/**
 * TTL tiers for cached tracks based on replay count
 */
enum class CacheTier(val ttlMs: Long, val minReplays: Int) {
    /** First listen - 1 hour TTL */
    INITIAL(ttlMs = 1 * 60 * 60 * 1000L, minReplays = 0),
    
    /** First replay - 6 hours TTL */
    REPLAYED(ttlMs = 6 * 60 * 60 * 1000L, minReplays = 1),
    
    /** 10+ replays - 3 days TTL */
    FAVORITE(ttlMs = 3 * 24 * 60 * 60 * 1000L, minReplays = 10),
    
    /** 25+ replays - 14 days TTL (maximum) */
    PERMANENT(ttlMs = 14 * 24 * 60 * 60 * 1000L, minReplays = 25);

    companion object {
        fun fromReplayCount(count: Int): CacheTier {
            return when {
                count >= PERMANENT.minReplays -> PERMANENT
                count >= FAVORITE.minReplays -> FAVORITE
                count >= REPLAYED.minReplays -> REPLAYED
                else -> INITIAL
            }
        }
    }
}

/**
 * Represents a cached audio track
 */
data class CachedTrack(
    val trackHash: String,
    val filePath: String,
    val cacheFilePath: String,
    val cachedAt: Long,
    val expiresAt: Long,
    val tier: CacheTier,
    val replayCount: Int,
    val fileSizeBytes: Long,
    val lastAccessedAt: Long
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt
    
    val isValid: Boolean
        get() = !isExpired
}

/**
 * Criteria for a valid replay:
 * - Start from beginning (0-6 seconds)
 * - Listen at least 15 seconds
 */
data class ReplaySession(
    val trackHash: String,
    val startPositionSec: Int,
    val listenedDurationSec: Int
) {
    val isValidReplay: Boolean
        get() = startPositionSec <= 6 && listenedDurationSec >= 15
}
