package com.android.swingmusic.core.domain.repository

import com.android.swingmusic.core.domain.model.AlbumStats
import com.android.swingmusic.core.domain.model.ArtistStats
import com.android.swingmusic.core.domain.model.HomeSection
import com.android.swingmusic.core.domain.model.ListeningEvent
import com.android.swingmusic.core.domain.model.ListeningPattern
import com.android.swingmusic.core.domain.model.Recommendation
import com.android.swingmusic.core.domain.model.StatsPeriod
import com.android.swingmusic.core.domain.model.Track
import com.android.swingmusic.core.domain.model.TrackStats
import kotlinx.coroutines.flow.Flow

/**
 * MusicForYouSDK Repository Interface
 * 
 * Provides intelligent music recommendations based on listening behavior.
 */
interface MusicForYouRepository {
    
    // ==================== EVENT TRACKING ====================
    
    /**
     * Record a new listening event. Call when:
     * - Track starts playing (with listenDuration = 0)
     * - Track ends or is skipped (with actual duration)
     */
    suspend fun recordListeningEvent(event: ListeningEvent)
    
    /**
     * Record when a track starts playing
     */
    suspend fun onTrackStarted(
        track: Track,
        source: com.android.swingmusic.core.domain.model.ListeningSource
    )
    
    /**
     * Record when a track ends or is interrupted
     * @param listenDurationMs How long the user actually listened
     * @param skipped True if track was skipped within first 30 seconds
     */
    suspend fun onTrackEnded(
        track: Track,
        listenDurationMs: Long,
        skipped: Boolean
    )
    
    // ==================== STATISTICS ====================
    
    /**
     * Get stats for a specific track
     */
    suspend fun getTrackStats(trackHash: String): TrackStats?
    
    /**
     * Get stats for a specific artist
     */
    suspend fun getArtistStats(artistHash: String): ArtistStats?
    
    /**
     * Get stats for a specific album
     */
    suspend fun getAlbumStats(albumHash: String): AlbumStats?
    
    /**
     * Get user's top tracks for a period
     */
    suspend fun getTopTracks(period: StatsPeriod, limit: Int = 20): List<Pair<Track, TrackStats>>
    
    /**
     * Get user's top artists for a period
     */
    suspend fun getTopArtists(period: StatsPeriod, limit: Int = 10): List<ArtistStats>
    
    /**
     * Get user's top albums for a period
     */
    suspend fun getTopAlbums(period: StatsPeriod, limit: Int = 10): List<AlbumStats>
    
    /**
     * Get total listening time for a period
     */
    suspend fun getTotalListeningTime(period: StatsPeriod): Long
    
    // ==================== PATTERNS ====================
    
    /**
     * Get detected listening patterns
     */
    suspend fun getListeningPatterns(): List<ListeningPattern>
    
    /**
     * Get the pattern for current time slot
     */
    suspend fun getCurrentTimeSlotPattern(): ListeningPattern?
    
    /**
     * Check if user has a listening habit at specific time
     */
    suspend fun hasHabitAt(hourOfDay: Int, dayOfWeek: Int): Boolean
    
    // ==================== RECOMMENDATIONS ====================
    
    /**
     * Get "In The Moment" recommendations based on current time and patterns
     */
    suspend fun getInTheMomentRecommendations(): List<Recommendation>
    
    /**
     * Get rediscovery recommendations (tracks user loved but hasn't played recently)
     */
    suspend fun getRediscoveryRecommendations(limit: Int = 10): List<Recommendation>
    
    /**
     * Get recommendations based on a specific artist
     */
    suspend fun getArtistBasedRecommendations(artistHash: String, limit: Int = 10): List<Recommendation>
    
    /**
     * Get time-of-day mix (tracks that match user's taste for current time)
     */
    suspend fun getTimeOfDayMix(limit: Int = 15): List<Track>
    
    /**
     * Get all home sections personalized for the user
     */
    suspend fun getPersonalizedHomeSections(): List<HomeSection>
    
    /**
     * Check if we have enough data to show personalized content
     */
    suspend fun hasEnoughDataForPersonalization(): Boolean
    
    /**
     * Get minimum events needed before showing personalized content
     */
    fun getMinEventsForPersonalization(): Int
    
    // ==================== FLOWS ====================
    
    /**
     * Observe top tracks in real-time
     */
    fun observeTopTracks(limit: Int = 20): Flow<List<TrackStats>>
    
    /**
     * Observe top artists in real-time
     */
    fun observeTopArtists(limit: Int = 10): Flow<List<ArtistStats>>
    
    /**
     * Observe total play count
     */
    fun observeTotalPlays(): Flow<Int>
    
    // ==================== MAINTENANCE ====================
    
    /**
     * Refresh all stats caches
     */
    suspend fun refreshStatsCache()
    
    /**
     * Clean up old events (keep last N days)
     */
    suspend fun cleanupOldEvents(keepDays: Int = 90)
    
    /**
     * Get data health metrics
     */
    suspend fun getDataHealth(): DataHealth
}

data class DataHealth(
    val totalEvents: Int,
    val eventsLast7Days: Int,
    val eventsLast30Days: Int,
    val uniqueTracksPlayed: Int,
    val uniqueArtistsPlayed: Int,
    val oldestEventDate: Long?,
    val hasEnoughForBasicRecs: Boolean,
    val hasEnoughForAdvancedRecs: Boolean,
    val hasEnoughForTimePatterns: Boolean
)
