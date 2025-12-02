package com.android.swingmusic.endlesssound.domain.model

/**
 * Cache statistics for monitoring
 */
data class CacheStats(
    val totalCachedTracks: Int,
    val totalSizeBytes: Long,
    val tierBreakdown: Map<CacheTier, Int>,
    val expiredCount: Int,
    val hitRate: Float // 0.0 - 1.0
)

/**
 * Result of cache operation
 */
sealed class CacheResult {
    data class Hit(val cacheFilePath: String) : CacheResult()
    data class Miss(val reason: String) : CacheResult()
    data class Downloading(val progress: Float) : CacheResult()
    data class Error(val message: String) : CacheResult()
}

/**
 * Configuration for EndlessSound cache
 */
data class EndlessSoundConfig(
    val maxCacheSizeBytes: Long = 2L * 1024 * 1024 * 1024, // 2GB default
    val cleanupThresholdPercent: Float = 0.9f, // Start cleanup at 90% capacity
    val cleanupTargetPercent: Float = 0.7f, // Clean down to 70%
    val enableAutoCleanup: Boolean = true
)
