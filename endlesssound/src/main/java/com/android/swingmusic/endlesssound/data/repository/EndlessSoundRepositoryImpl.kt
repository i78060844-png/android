package com.android.swingmusic.endlesssound.data.repository

import android.content.Context
import android.net.Uri
import com.android.swingmusic.auth.data.tokenholder.AuthTokenHolder
import com.android.swingmusic.endlesssound.data.dao.CachedTrackDao
import com.android.swingmusic.endlesssound.data.entity.CacheStatsEntity
import com.android.swingmusic.endlesssound.data.entity.CachedTrackEntity
import com.android.swingmusic.endlesssound.data.entity.ReplaySessionEntity
import com.android.swingmusic.endlesssound.domain.model.CacheStats
import com.android.swingmusic.endlesssound.domain.model.CacheTier
import com.android.swingmusic.endlesssound.domain.model.CachedTrack
import com.android.swingmusic.endlesssound.domain.model.EndlessSoundConfig
import com.android.swingmusic.endlesssound.domain.repository.EndlessSoundRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EndlessSoundRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cachedTrackDao: CachedTrackDao,
    private val okHttpClient: OkHttpClient
) : EndlessSoundRepository {

    private var config = EndlessSoundConfig()
    private val downloadMutex = Mutex()
    private val activeDownloads = mutableSetOf<String>()
    
    private val cacheDir: File by lazy {
        File(context.cacheDir, "endless_sound").apply {
            if (!exists()) mkdirs()
        }
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    companion object {
        // Replay validation criteria
        private const val VALID_REPLAY_MAX_START_SEC = 6
        private const val VALID_REPLAY_MIN_DURATION_SEC = 15
        
        // Cleanup thresholds
        private const val HIGH_TIER_PROTECTION_THRESHOLD = 0.5f // Protect high-tier tracks unless freeing >50% space
    }
    
    override suspend fun getTrackUri(
        trackHash: String,
        originalPath: String,
        baseUrl: String
    ): String = withContext(Dispatchers.IO) {
        // Check cache first
        val cached = cachedTrackDao.getValidCachedTrack(trackHash)
        if (cached != null) {
            val cacheFile = File(cached.cacheFilePath)
            // Double check file exists and has content
            if (cacheFile.exists() && cacheFile.length() > 0) {
                Timber.tag("EndlessSound").d("Cache HIT: $trackHash (${cacheFile.length() / 1024}KB, tier=${cached.tierOrdinal})")
                cachedTrackDao.updateLastAccessed(trackHash)
                recordCacheHit(cacheFile.length())
                return@withContext Uri.fromFile(cacheFile).toString()
            } else {
                // Cache entry exists but file is missing/empty, clean up
                Timber.tag("EndlessSound").w("Cache INVALID: $trackHash - file missing or empty")
                cachedTrackDao.deleteCachedTrack(trackHash)
            }
        }
        
        Timber.tag("EndlessSound").d("Cache MISS: $trackHash - returning streaming URL")
        recordCacheMiss()
        
        // Return original URL - track will be cached during playback
        buildTrackUrl(baseUrl, trackHash, originalPath)
    }
    
    override suspend fun isCached(trackHash: String): Boolean = withContext(Dispatchers.IO) {
        val cached = cachedTrackDao.getValidCachedTrack(trackHash)
        val result = cached != null && File(cached.cacheFilePath).let { it.exists() && it.length() > 0 }
        if (result) {
            Timber.tag("EndlessSound").v("Track $trackHash is cached")
        }
        result
    }
    
    override suspend fun getCachedTrack(trackHash: String): CachedTrack? = withContext(Dispatchers.IO) {
        cachedTrackDao.getCachedTrack(trackHash)?.toDomain()
    }
    
    override suspend fun startCaching(
        trackHash: String,
        originalPath: String,
        baseUrl: String
    ) = withContext(Dispatchers.IO) {
        // Skip if already cached or downloading
        if (isCached(trackHash)) {
            Timber.tag("EndlessSound").d("Skip download: $trackHash already cached")
            return@withContext
        }
        
        // Check if already downloading
        val alreadyDownloading = downloadMutex.withLock {
            if (activeDownloads.contains(trackHash)) {
                true
            } else {
                activeDownloads.add(trackHash)
                false
            }
        }
        
        if (alreadyDownloading) {
            Timber.tag("EndlessSound").d("Skip download: $trackHash already downloading")
            return@withContext
        }
        
        try {
            // Check if we need cleanup before downloading
            if (config.enableAutoCleanup) {
                val currentSize = cachedTrackDao.getTotalCacheSize() ?: 0
                if (currentSize > config.maxCacheSizeBytes * config.cleanupThresholdPercent) {
                    Timber.tag("EndlessSound").d("Running cleanup before download: ${currentSize / 1024 / 1024}MB > ${(config.maxCacheSizeBytes * config.cleanupThresholdPercent) / 1024 / 1024}MB")
                    cleanup()
                }
            }
            
            val url = buildTrackUrl(baseUrl, trackHash, originalPath)
            val cacheFile = File(cacheDir, "$trackHash.audio")
            
            Timber.tag("EndlessSound").d("Starting download: $trackHash from $url")
            
            val requestBuilder = Request.Builder().url(url)
            
            // Add authorization header if token available
            AuthTokenHolder.accessToken?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            
            if (response.isSuccessful) {
                response.body?.let { body ->
                    cacheFile.outputStream().use { output ->
                        body.byteStream().copyTo(output)
                    }
                    
                    // Verify file was written successfully
                    if (!cacheFile.exists() || cacheFile.length() == 0L) {
                        Timber.tag("EndlessSound").e("Download failed: file not created or empty for $trackHash")
                        cacheFile.delete()
                        return@withContext
                    }
                    
                    val now = System.currentTimeMillis()
                    val tier = CacheTier.INITIAL
                    
                    val entity = CachedTrackEntity(
                        trackHash = trackHash,
                        originalFilePath = originalPath,
                        cacheFilePath = cacheFile.absolutePath,
                        cachedAt = now,
                        expiresAt = now + tier.ttlMs,
                        tierOrdinal = tier.ordinal,
                        replayCount = 0,
                        fileSizeBytes = cacheFile.length(),
                        lastAccessedAt = now
                    )
                    
                    cachedTrackDao.insertCachedTrack(entity)
                    recordDownload(cacheFile.length())
                    
                    Timber.tag("EndlessSound").d("Cached successfully: $trackHash (${cacheFile.length() / 1024}KB, tier=${tier.name})")
                }
            } else {
                Timber.tag("EndlessSound").e("Download failed: ${response.code} for $trackHash")
            }
        } catch (e: Exception) {
            Timber.tag("EndlessSound").e("Cache error for $trackHash: ${e.message}")
        } finally {
            downloadMutex.withLock {
                activeDownloads.remove(trackHash)
            }
        }
    }
    
    override suspend fun recordReplaySession(
        trackHash: String,
        startPositionSec: Int,
        listenedDurationSec: Int
    ) = withContext(Dispatchers.IO) {
        val isValidReplay = startPositionSec <= VALID_REPLAY_MAX_START_SEC && 
                           listenedDurationSec >= VALID_REPLAY_MIN_DURATION_SEC
        
        Timber.tag("EndlessSound").d(
            "Replay session: $trackHash (start=${startPositionSec}s, listened=${listenedDurationSec}s, valid=$isValidReplay)"
        )
        
        val session = ReplaySessionEntity(
            trackHash = trackHash,
            startPositionSec = startPositionSec,
            listenedDurationSec = listenedDurationSec,
            timestamp = System.currentTimeMillis(),
            wasValidReplay = isValidReplay
        )
        
        cachedTrackDao.insertReplaySession(session)
        
        if (isValidReplay) {
            // Update TTL based on new replay count
            val cached = cachedTrackDao.getCachedTrack(trackHash)
            if (cached == null) {
                Timber.tag("EndlessSound").d("Cannot upgrade TTL: $trackHash not cached")
                return@withContext
            }
            
            val newReplayCount = cached.replayCount + 1
            val newTier = CacheTier.fromReplayCount(newReplayCount)
            val currentTier = CacheTier.entries[cached.tierOrdinal]
            
            if (newTier.ordinal > cached.tierOrdinal) {
                val newExpiresAt = System.currentTimeMillis() + newTier.ttlMs
                cachedTrackDao.incrementReplayCount(trackHash, newTier.ordinal, newExpiresAt)
                
                Timber.tag("EndlessSound").d(
                    "TTL upgraded: $trackHash ${currentTier.name} -> ${newTier.name} (replays: ${cached.replayCount} -> $newReplayCount, ttl: ${newTier.ttlMs / 3600000}h)"
                )
            } else {
                // Just increment count, extend current TTL
                val newExpiresAt = System.currentTimeMillis() + currentTier.ttlMs
                cachedTrackDao.incrementReplayCount(trackHash, cached.tierOrdinal, newExpiresAt)
                
                Timber.tag("EndlessSound").d(
                    "TTL extended: $trackHash ${currentTier.name} (replays: ${cached.replayCount} -> $newReplayCount)"
                )
            }
        }
    }
    
    override suspend fun getValidCachedTracks(): List<CachedTrack> = withContext(Dispatchers.IO) {
        cachedTrackDao.getValidCachedTracks().map { it.toDomain() }
    }
    
    override fun observeCachedTrackHashes(): Flow<List<String>> {
        return cachedTrackDao.observeValidCachedTrackHashes()
    }
    
    override suspend fun getCacheStats(): CacheStats = withContext(Dispatchers.IO) {
        val totalCount = cachedTrackDao.getCachedTrackCount()
        val totalSize = cachedTrackDao.getTotalCacheSize() ?: 0
        val expiredCount = cachedTrackDao.getExpiredTracks().size
        
        val tierBreakdown = CacheTier.entries.associateWith { tier ->
            cachedTrackDao.getCountByTier(tier.ordinal)
        }
        
        val hitMiss = cachedTrackDao.getTotalHitMissCount()
        val hitRate = if (hitMiss != null && (hitMiss.hits + hitMiss.misses) > 0) {
            hitMiss.hits.toFloat() / (hitMiss.hits + hitMiss.misses)
        } else 0f
        
        CacheStats(
            totalCachedTracks = totalCount,
            totalSizeBytes = totalSize,
            tierBreakdown = tierBreakdown,
            expiredCount = expiredCount,
            hitRate = hitRate
        )
    }
    
    override suspend fun cleanup() = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Timber.tag("EndlessSound").d("Starting cleanup...")
        
        // Delete expired tracks
        val expired = cachedTrackDao.getExpiredTracks()
        var freedSpace = 0L
        expired.forEach { entity ->
            val file = File(entity.cacheFilePath)
            if (file.exists()) {
                freedSpace += file.length()
                file.delete()
            }
        }
        cachedTrackDao.deleteExpiredTracks()
        
        if (expired.isNotEmpty()) {
            Timber.tag("EndlessSound").d("Deleted ${expired.size} expired tracks, freed ${freedSpace / 1024 / 1024}MB")
        }
        
        // Check if we still need to free space
        val currentSize = cachedTrackDao.getTotalCacheSize() ?: 0
        val targetSize = (config.maxCacheSizeBytes * config.cleanupTargetPercent).toLong()
        
        if (currentSize > targetSize) {
            Timber.tag("EndlessSound").d("Need to free space: ${currentSize / 1024 / 1024}MB > ${targetSize / 1024 / 1024}MB")
            
            // Evict LRU tracks until we're under target
            var sizeToFree = currentSize - targetSize
            val lruTracks = cachedTrackDao.getLeastRecentlyUsed(100)
            var evicted = 0
            
            for (track in lruTracks) {
                if (sizeToFree <= 0) break
                
                // Don't evict high-tier tracks unless necessary
                if (track.tierOrdinal >= CacheTier.FAVORITE.ordinal && sizeToFree < currentSize * HIGH_TIER_PROTECTION_THRESHOLD) {
                    Timber.tag("EndlessSound").v("Skipping high-tier track: ${track.trackHash} (tier=${track.tierOrdinal})")
                    continue
                }
                
                val file = File(track.cacheFilePath)
                if (file.exists()) {
                    file.delete()
                }
                cachedTrackDao.deleteCachedTrack(track.trackHash)
                sizeToFree -= track.fileSizeBytes
                evicted++
                
                Timber.tag("EndlessSound").v("Evicted LRU: ${track.trackHash} (${track.fileSizeBytes / 1024}KB)")
            }
            
            if (evicted > 0) {
                Timber.tag("EndlessSound").d("Evicted $evicted LRU tracks")
            }
        }
        
        // Cleanup old replay sessions (keep last 30 days)
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        cachedTrackDao.cleanupOldSessions(thirtyDaysAgo)
        
        val duration = System.currentTimeMillis() - startTime
        val finalSize = cachedTrackDao.getTotalCacheSize() ?: 0
        Timber.tag("EndlessSound").d("Cleanup complete in ${duration}ms, cache size: ${finalSize / 1024 / 1024}MB")
    }
    
    override suspend fun evictTrack(trackHash: String) {
        withContext(Dispatchers.IO) {
            val cached = cachedTrackDao.getCachedTrack(trackHash)
            cached?.let {
                File(it.cacheFilePath).delete()
                cachedTrackDao.deleteCachedTrack(trackHash)
            }
        }
    }
    
    override suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            cacheDir.listFiles()?.forEach { it.delete() }
            cachedTrackDao.getAllCachedTracks().forEach {
                cachedTrackDao.deleteCachedTrack(it.trackHash)
            }
            Timber.tag("EndlessSound").d("Cache cleared")
        }
    }
    
    override fun updateConfig(config: EndlessSoundConfig) {
        this.config = config
    }
    
    override fun getConfig(): EndlessSoundConfig = config
    
    private fun buildTrackUrl(baseUrl: String, trackHash: String, filePath: String): String {
        val encodedPath = Uri.encode(filePath)
        return "${baseUrl}file/${trackHash}/legacy?filepath=$encodedPath"
    }
    
    private suspend fun recordCacheHit(bytes: Long) {
        val date = dateFormat.format(Date())
        ensureStatsExist(date)
        cachedTrackDao.recordHit(date, bytes)
    }
    
    private suspend fun recordCacheMiss() {
        val date = dateFormat.format(Date())
        ensureStatsExist(date)
        cachedTrackDao.recordMiss(date)
    }
    
    private suspend fun recordDownload(bytes: Long) {
        val date = dateFormat.format(Date())
        ensureStatsExist(date)
        cachedTrackDao.recordDownload(date, bytes)
    }
    
    private suspend fun ensureStatsExist(date: String) {
        if (cachedTrackDao.getStatsForDate(date) == null) {
            cachedTrackDao.insertOrUpdateStats(CacheStatsEntity(date = date))
        }
    }
}
