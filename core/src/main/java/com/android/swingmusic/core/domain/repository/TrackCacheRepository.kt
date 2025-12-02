package com.android.swingmusic.core.domain.repository

import com.android.swingmusic.core.domain.model.Track

/**
 * Interface for fetching track data from cache or local storage
 */
interface TrackCacheRepository {
    /**
     * Get a track by its hash from local cache
     */
    suspend fun getTrackByHash(trackHash: String): Track?
    
    /**
     * Get multiple tracks by their hashes
     */
    suspend fun getTracksByHashes(trackHashes: List<String>): List<Track>
    
    /**
     * Cache tracks for later retrieval
     */
    suspend fun cacheTracks(tracks: List<Track>)
    
    /**
     * Clear track cache
     */
    suspend fun clearCache()
}
