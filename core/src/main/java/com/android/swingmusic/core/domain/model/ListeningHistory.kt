package com.android.swingmusic.core.domain.model

/**
 * Represents a single listening event
 */
data class ListeningEvent(
    val id: Long = 0,
    val trackHash: String,
    val albumHash: String,
    val artistHashes: List<String>,
    val timestamp: Long, // Unix timestamp in millis
    val dayOfWeek: Int, // 1 = Monday, 7 = Sunday
    val hourOfDay: Int, // 0-23
    val listenDurationMs: Long, // How long the track was played
    val trackDurationMs: Long, // Total track duration
    val completedPlay: Boolean, // Did user listen to >80% of track
    val skipped: Boolean, // Was track skipped within first 30 seconds
    val source: ListeningSource
)

enum class ListeningSource {
    QUEUE,
    ALBUM,
    ARTIST,
    PLAYLIST,
    SEARCH,
    RECOMMENDATION,
    FOLDER,
    UNKNOWN
}

/**
 * Aggregated statistics for a track
 */
data class TrackStats(
    val trackHash: String,
    val totalPlays: Int,
    val completedPlays: Int,
    val skips: Int,
    val totalListenTimeMs: Long,
    val lastPlayedAt: Long,
    val firstPlayedAt: Long,
    val averageCompletionRate: Float, // 0.0 - 1.0
    val playsByHour: Map<Int, Int>, // hour -> play count
    val playsByDayOfWeek: Map<Int, Int>, // dayOfWeek -> play count
    val streak: Int, // Days in a row this track was played
    val currentStreakStartDate: Long?
)

/**
 * Aggregated statistics for an artist
 */
data class ArtistStats(
    val artistHash: String,
    val totalPlays: Int,
    val uniqueTracksPlayed: Int,
    val totalListenTimeMs: Long,
    val lastPlayedAt: Long,
    val favoriteTimeOfDay: Int?, // Most common hour
    val favoriteDayOfWeek: Int? // Most common day
)

/**
 * Aggregated statistics for an album
 */
data class AlbumStats(
    val albumHash: String,
    val totalPlays: Int,
    val uniqueTracksPlayed: Int,
    val totalListenTimeMs: Long,
    val lastPlayedAt: Long,
    val completionRate: Float // How much of album tracks have been played
)

/**
 * Time-based listening pattern
 */
data class ListeningPattern(
    val hourOfDay: Int,
    val dayOfWeek: Int,
    val avgPlaysPerSession: Float,
    val commonGenres: List<String>,
    val commonMoods: List<String>, // If available from metadata
    val preferredTempo: String? // "slow", "medium", "fast"
)

/**
 * A recommendation with context
 */
data class Recommendation(
    val track: Track,
    val reason: RecommendationReason,
    val confidence: Float, // 0.0 - 1.0
    val contextMessage: String // Human-readable reason
)

sealed class RecommendationReason {
    data class TimeBasedHabit(
        val usualHour: Int,
        val streakDays: Int
    ) : RecommendationReason()
    
    data class FrequentlyPlayed(
        val playCount: Int,
        val lastPlayed: Long
    ) : RecommendationReason()
    
    data class SimilarToRecent(
        val similarTrackHash: String,
        val similarity: Float
    ) : RecommendationReason()
    
    data class ArtistFavorite(
        val artistHash: String,
        val artistPlays: Int
    ) : RecommendationReason()
    
    data class Rediscovery(
        val daysSinceLastPlay: Int,
        val previousPlayCount: Int
    ) : RecommendationReason()
    
    data class DayOfWeekPattern(
        val dayOfWeek: Int,
        val usualPlayCount: Int
    ) : RecommendationReason()
    
    object NewRelease : RecommendationReason()
    
    object Trending : RecommendationReason()
}

/**
 * Section types for home screen
 */
sealed class HomeSection {
    data class InTheMoment(
        val recommendations: List<Recommendation>,
        val timeWindow: Pair<Int, Int> // start hour, end hour
    ) : HomeSection()
    
    data class YourTopTracks(
        val tracks: List<Track>,
        val period: StatsPeriod
    ) : HomeSection()
    
    data class RecentlyPlayed(
        val tracks: List<Track>
    ) : HomeSection()
    
    data class BecauseYouListen(
        val artistName: String,
        val tracks: List<Track>
    ) : HomeSection()
    
    data class Rediscover(
        val tracks: List<Recommendation>
    ) : HomeSection()
    
    data class YourTopArtists(
        val artists: List<Pair<Artist, ArtistStats>>
    ) : HomeSection()
    
    data class MixedForYou(
        val tracks: List<Recommendation>
    ) : HomeSection()
    
    data class TimeOfDayMix(
        val timeOfDay: String, // "Morning", "Afternoon", "Evening", "Night"
        val tracks: List<Track>
    ) : HomeSection()
}

enum class StatsPeriod {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    ALL_TIME
}
