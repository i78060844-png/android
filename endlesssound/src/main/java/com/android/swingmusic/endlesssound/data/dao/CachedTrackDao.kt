package com.android.swingmusic.endlesssound.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.android.swingmusic.endlesssound.data.entity.CacheStatsEntity
import com.android.swingmusic.endlesssound.data.entity.CachedTrackEntity
import com.android.swingmusic.endlesssound.data.entity.ReplaySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedTrackDao {
    
    // ==================== CACHED TRACKS ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedTrack(track: CachedTrackEntity)
    
    @Update
    suspend fun updateCachedTrack(track: CachedTrackEntity)
    
    @Query("SELECT * FROM cached_tracks WHERE trackHash = :trackHash")
    suspend fun getCachedTrack(trackHash: String): CachedTrackEntity?
    
    @Query("SELECT * FROM cached_tracks WHERE trackHash = :trackHash AND expiresAt > :now")
    suspend fun getValidCachedTrack(trackHash: String, now: Long = System.currentTimeMillis()): CachedTrackEntity?
    
    @Query("SELECT * FROM cached_tracks")
    suspend fun getAllCachedTracks(): List<CachedTrackEntity>
    
    @Query("SELECT * FROM cached_tracks WHERE expiresAt > :now")
    suspend fun getValidCachedTracks(now: Long = System.currentTimeMillis()): List<CachedTrackEntity>
    
    @Query("SELECT * FROM cached_tracks WHERE expiresAt <= :now")
    suspend fun getExpiredTracks(now: Long = System.currentTimeMillis()): List<CachedTrackEntity>
    
    @Query("DELETE FROM cached_tracks WHERE trackHash = :trackHash")
    suspend fun deleteCachedTrack(trackHash: String)
    
    @Query("DELETE FROM cached_tracks WHERE expiresAt <= :now")
    suspend fun deleteExpiredTracks(now: Long = System.currentTimeMillis()): Int
    
    @Query("DELETE FROM cached_tracks WHERE trackHash IN (:trackHashes)")
    suspend fun deleteCachedTracks(trackHashes: List<String>)
    
    @Query("SELECT SUM(fileSizeBytes) FROM cached_tracks")
    suspend fun getTotalCacheSize(): Long?
    
    @Query("SELECT COUNT(*) FROM cached_tracks")
    suspend fun getCachedTrackCount(): Int
    
    @Query("SELECT COUNT(*) FROM cached_tracks WHERE tierOrdinal = :tierOrdinal")
    suspend fun getCountByTier(tierOrdinal: Int): Int
    
    @Query("SELECT * FROM cached_tracks ORDER BY lastAccessedAt ASC LIMIT :limit")
    suspend fun getLeastRecentlyUsed(limit: Int): List<CachedTrackEntity>
    
    @Query("UPDATE cached_tracks SET lastAccessedAt = :now WHERE trackHash = :trackHash")
    suspend fun updateLastAccessed(trackHash: String, now: Long = System.currentTimeMillis())
    
    @Query("UPDATE cached_tracks SET replayCount = replayCount + 1, tierOrdinal = :newTierOrdinal, expiresAt = :newExpiresAt WHERE trackHash = :trackHash")
    suspend fun incrementReplayCount(trackHash: String, newTierOrdinal: Int, newExpiresAt: Long)
    
    @Query("SELECT trackHash FROM cached_tracks WHERE expiresAt > :now")
    fun observeValidCachedTrackHashes(now: Long = System.currentTimeMillis()): Flow<List<String>>
    
    // ==================== REPLAY SESSIONS ====================
    
    @Insert
    suspend fun insertReplaySession(session: ReplaySessionEntity)
    
    @Query("SELECT COUNT(*) FROM replay_sessions WHERE trackHash = :trackHash AND wasValidReplay = 1")
    suspend fun getValidReplayCount(trackHash: String): Int
    
    @Query("SELECT * FROM replay_sessions WHERE trackHash = :trackHash ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getReplaySessions(trackHash: String, limit: Int = 10): List<ReplaySessionEntity>
    
    @Query("DELETE FROM replay_sessions WHERE timestamp < :before")
    suspend fun cleanupOldSessions(before: Long)
    
    // ==================== CACHE STATS ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStats(stats: CacheStatsEntity)
    
    @Query("SELECT * FROM cache_stats WHERE date = :date")
    suspend fun getStatsForDate(date: String): CacheStatsEntity?
    
    @Query("SELECT SUM(hits) as hits, SUM(misses) as misses FROM cache_stats")
    suspend fun getTotalHitMissCount(): HitMissCount?
    
    @Query("UPDATE cache_stats SET hits = hits + 1, bytesServed = bytesServed + :bytes WHERE date = :date")
    suspend fun recordHit(date: String, bytes: Long)
    
    @Query("UPDATE cache_stats SET misses = misses + 1 WHERE date = :date")
    suspend fun recordMiss(date: String)
    
    @Query("UPDATE cache_stats SET downloads = downloads + 1, bytesDownloaded = bytesDownloaded + :bytes WHERE date = :date")
    suspend fun recordDownload(date: String, bytes: Long)
}

data class HitMissCount(
    val hits: Int,
    val misses: Int
)
