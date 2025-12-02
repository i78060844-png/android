package com.android.swingmusic.endlesssound.domain.repository

import com.android.swingmusic.endlesssound.domain.model.CacheResult
import com.android.swingmusic.endlesssound.domain.model.CacheStats
import com.android.swingmusic.endlesssound.domain.model.CachedTrack
import com.android.swingmusic.endlesssound.domain.model.EndlessSoundConfig
import kotlinx.coroutines.flow.Flow

/**
 * EndlessSound - Intelligent Audio Caching System
 * 
 * Caches audio tracks locally with adaptive TTL based on replay behavior:
 * - First listen: 1 hour TTL
 * - First replay (valid): 6 hours TTL  
 * - 10+ replays: 3 days TTL
 * - 25+ replays: 14 days TTL (maximum)
 * 
 * A valid replay means starting from beginning (0-6 sec) and listening at least 15 seconds.
 */
interface EndlessSoundRepository {
    
    /**
     * Get a track from cache or download it.
     * Returns file URI to use for playback.
     * 
     * @param trackHash Unique track identifier
     * @param originalPath Original file path on server
     * @param baseUrl Server base URL
     * @return Local file URI if cached, or original URL if not cached
     */
    suspend fun getTrackUri(
        trackHash: String,
        originalPath: String,
        baseUrl: String
    ): String
    
    /**
     * Check if a track is cached and valid
     */
    suspend fun isCached(trackHash: String): Boolean
    
    /**
     * Get cached track info
     */
    suspend fun getCachedTrack(trackHash: String): CachedTrack?
    
    /**
     * Start caching a track in background.
     * Call this when playback starts.
     */
    suspend fun startCaching(
        trackHash: String,
        originalPath: String,
        baseUrl: String
    )
    
    /**
     * Record a replay session.
     * Call when track playback ends or user skips.
     * 
     * @param trackHash Track identifier
     * @param startPositionSec Where playback started (seconds)
     * @param listenedDurationSec How long user listened (seconds)
     */
    suspend fun recordReplaySession(
        trackHash: String,
        startPositionSec: Int,
        listenedDurationSec: Int
    )
    
    /**
     * Get all valid (non-expired) cached tracks
     */
    suspend fun getValidCachedTracks(): List<CachedTrack>
    
    /**
     * Observe cached track hashes for UI updates
     */
    fun observeCachedTrackHashes(): Flow<List<String>>
    
    /**
     * Get cache statistics
     */
    suspend fun getCacheStats(): CacheStats
    
    /**
     * Clean up expired tracks and reclaim space if needed
     */
    suspend fun cleanup()
    
    /**
     * Force evict a specific track from cache
     */
    suspend fun evictTrack(trackHash: String)
    
    /**
     * Clear all cache
     */
    suspend fun clearAll()
    
    /**
     * Update configuration
     */
    fun updateConfig(config: EndlessSoundConfig)
    
    /**
     * Get current configuration
     */
    fun getConfig(): EndlessSoundConfig
}
