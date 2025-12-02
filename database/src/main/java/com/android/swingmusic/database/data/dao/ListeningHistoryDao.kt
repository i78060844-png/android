package com.android.swingmusic.database.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.swingmusic.database.data.entity.AlbumStatsCacheEntity
import com.android.swingmusic.database.data.entity.ArtistStatsCacheEntity
import com.android.swingmusic.database.data.entity.ListeningEventEntity
import com.android.swingmusic.database.data.entity.ListeningPatternEntity
import com.android.swingmusic.database.data.entity.TrackStatsCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListeningHistoryDao {
    
    // ==================== LISTENING EVENTS ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListeningEvent(event: ListeningEventEntity): Long
    
    @Query("SELECT * FROM listening_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int = 100): List<ListeningEventEntity>
    
    @Query("SELECT * FROM listening_events WHERE trackHash = :trackHash ORDER BY timestamp DESC")
    suspend fun getEventsForTrack(trackHash: String): List<ListeningEventEntity>
    
    @Query("SELECT * FROM listening_events WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getEventsSince(since: Long): List<ListeningEventEntity>
    
    @Query("SELECT * FROM listening_events WHERE hourOfDay BETWEEN :startHour AND :endHour ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getEventsInTimeWindow(startHour: Int, endHour: Int, limit: Int = 50): List<ListeningEventEntity>
    
    @Query("SELECT * FROM listening_events WHERE dayOfWeek = :dayOfWeek ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getEventsForDayOfWeek(dayOfWeek: Int, limit: Int = 100): List<ListeningEventEntity>
    
    @Query("""
        SELECT trackHash, COUNT(*) as playCount 
        FROM listening_events 
        WHERE timestamp >= :since 
        GROUP BY trackHash 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    suspend fun getTopTracksSince(since: Long, limit: Int = 20): List<TrackPlayCount>
    
    @Query("""
        SELECT trackHash, COUNT(*) as playCount 
        FROM listening_events 
        WHERE hourOfDay BETWEEN :startHour AND :endHour 
        GROUP BY trackHash 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    suspend fun getTopTracksForTimeWindow(startHour: Int, endHour: Int, limit: Int = 10): List<TrackPlayCount>
    
    @Query("""
        SELECT DISTINCT trackHash 
        FROM listening_events 
        WHERE timestamp >= :dayStart AND timestamp < :dayEnd
    """)
    suspend fun getTracksPlayedOnDay(dayStart: Long, dayEnd: Long): List<String>
    
    @Query("""
        SELECT artistHashes 
        FROM listening_events 
        WHERE timestamp >= :since 
        GROUP BY artistHashes 
        ORDER BY COUNT(*) DESC
    """)
    suspend fun getTopArtistHashesSince(since: Long): List<String>
    
    @Query("DELETE FROM listening_events WHERE timestamp < :before")
    suspend fun deleteEventsBefore(before: Long)
    
    @Query("SELECT COUNT(*) FROM listening_events")
    suspend fun getTotalEventCount(): Int
    
    @Query("SELECT COUNT(*) FROM listening_events WHERE completedPlay = 1")
    suspend fun getCompletedPlaysCount(): Int
    
    // ==================== TRACK STATS CACHE ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrackStats(stats: TrackStatsCacheEntity)
    
    @Query("SELECT * FROM track_stats_cache WHERE trackHash = :trackHash")
    suspend fun getTrackStats(trackHash: String): TrackStatsCacheEntity?
    
    @Query("SELECT * FROM track_stats_cache ORDER BY totalPlays DESC LIMIT :limit")
    suspend fun getTopPlayedTracks(limit: Int = 50): List<TrackStatsCacheEntity>
    
    @Query("SELECT * FROM track_stats_cache WHERE streak > 0 ORDER BY streak DESC LIMIT :limit")
    suspend fun getTracksWithStreaks(limit: Int = 20): List<TrackStatsCacheEntity>
    
    @Query("SELECT * FROM track_stats_cache ORDER BY lastPlayedAt DESC LIMIT :limit")
    suspend fun getRecentlyPlayedTrackStats(limit: Int = 20): List<TrackStatsCacheEntity>
    
    @Query("""
        SELECT * FROM track_stats_cache 
        WHERE lastPlayedAt < :threshold AND totalPlays >= :minPlays
        ORDER BY totalPlays DESC 
        LIMIT :limit
    """)
    suspend fun getForgottenFavorites(threshold: Long, minPlays: Int = 5, limit: Int = 10): List<TrackStatsCacheEntity>
    
    @Query("SELECT * FROM track_stats_cache ORDER BY lastUpdated ASC LIMIT :limit")
    suspend fun getStaleTrackStats(limit: Int = 50): List<TrackStatsCacheEntity>
    
    // ==================== ARTIST STATS CACHE ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtistStats(stats: ArtistStatsCacheEntity)
    
    @Query("SELECT * FROM artist_stats_cache WHERE artistHash = :artistHash")
    suspend fun getArtistStats(artistHash: String): ArtistStatsCacheEntity?
    
    @Query("SELECT * FROM artist_stats_cache ORDER BY totalPlays DESC LIMIT :limit")
    suspend fun getTopArtists(limit: Int = 20): List<ArtistStatsCacheEntity>
    
    @Query("SELECT * FROM artist_stats_cache ORDER BY lastPlayedAt DESC LIMIT :limit")
    suspend fun getRecentlyPlayedArtists(limit: Int = 10): List<ArtistStatsCacheEntity>
    
    // ==================== ALBUM STATS CACHE ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlbumStats(stats: AlbumStatsCacheEntity)
    
    @Query("SELECT * FROM album_stats_cache WHERE albumHash = :albumHash")
    suspend fun getAlbumStats(albumHash: String): AlbumStatsCacheEntity?
    
    @Query("SELECT * FROM album_stats_cache ORDER BY totalPlays DESC LIMIT :limit")
    suspend fun getTopAlbums(limit: Int = 20): List<AlbumStatsCacheEntity>
    
    // ==================== LISTENING PATTERNS ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertListeningPattern(pattern: ListeningPatternEntity)
    
    @Query("SELECT * FROM listening_patterns WHERE hourOfDay = :hour AND dayOfWeek = :dayOfWeek")
    suspend fun getPatternForSlot(hour: Int, dayOfWeek: Int): ListeningPatternEntity?
    
    @Query("SELECT * FROM listening_patterns WHERE hourOfDay BETWEEN :startHour AND :endHour")
    suspend fun getPatternsForTimeWindow(startHour: Int, endHour: Int): List<ListeningPatternEntity>
    
    @Query("SELECT * FROM listening_patterns ORDER BY avgPlaysPerSession DESC")
    suspend fun getAllPatterns(): List<ListeningPatternEntity>
    
    // ==================== FLOWS FOR REACTIVE UI ====================
    
    @Query("SELECT * FROM track_stats_cache ORDER BY totalPlays DESC LIMIT :limit")
    fun observeTopTracks(limit: Int = 20): Flow<List<TrackStatsCacheEntity>>
    
    @Query("SELECT * FROM artist_stats_cache ORDER BY totalPlays DESC LIMIT :limit")
    fun observeTopArtists(limit: Int = 10): Flow<List<ArtistStatsCacheEntity>>
    
    @Query("SELECT COUNT(*) FROM listening_events")
    fun observeTotalPlays(): Flow<Int>
}

data class TrackPlayCount(
    val trackHash: String,
    val playCount: Int
)
