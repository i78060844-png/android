package com.android.swingmusic.database.data.repository

import com.android.swingmusic.core.domain.model.Track
import com.android.swingmusic.core.domain.model.TrackArtist
import com.android.swingmusic.core.domain.repository.TrackCacheRepository
import com.android.swingmusic.database.data.dao.QueueDao
import com.android.swingmusic.database.data.entity.QueueEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TrackCacheRepository that uses queue and in-memory cache
 */
@Singleton
class TrackCacheRepositoryImpl @Inject constructor(
    private val queueDao: QueueDao
) : TrackCacheRepository {
    
    // In-memory cache for quick access
    private val trackCache = mutableMapOf<String, Track>()
    private val cacheMutex = Mutex()
    
    override suspend fun getTrackByHash(trackHash: String): Track? = withContext(Dispatchers.IO) {
        // First check in-memory cache
        cacheMutex.withLock {
            trackCache[trackHash]?.let { return@withContext it }
        }
        
        // Then check queue in database
        val queueEntities = queueDao.getSavedQueue()
        val entityFromQueue = queueEntities.find { it.trackHash == trackHash }
        
        val trackFromQueue = entityFromQueue?.toTrack()
        
        if (trackFromQueue != null) {
            cacheMutex.withLock {
                trackCache[trackHash] = trackFromQueue
            }
        }
        
        trackFromQueue
    }
    
    override suspend fun getTracksByHashes(trackHashes: List<String>): List<Track> = 
        withContext(Dispatchers.IO) {
            val result = mutableListOf<Track>()
            val missingHashes = mutableListOf<String>()
            
            // Check cache first
            cacheMutex.withLock {
                for (hash in trackHashes) {
                    val cached = trackCache[hash]
                    if (cached != null) {
                        result.add(cached)
                    } else {
                        missingHashes.add(hash)
                    }
                }
            }
            
            // Get missing from queue
            if (missingHashes.isNotEmpty()) {
                val queueEntities = queueDao.getSavedQueue()
                val tracksFromQueue = queueEntities
                    .filter { it.trackHash in missingHashes }
                    .map { it.toTrack() }
                
                cacheMutex.withLock {
                    tracksFromQueue.forEach { track ->
                        trackCache[track.trackHash] = track
                    }
                }
                
                result.addAll(tracksFromQueue)
            }
            
            // Return in original order
            trackHashes.mapNotNull { hash -> result.find { it.trackHash == hash } }
        }
    
    override suspend fun cacheTracks(tracks: List<Track>) {
        cacheMutex.withLock {
            tracks.forEach { track ->
                trackCache[track.trackHash] = track
            }
        }
    }
    
    override suspend fun clearCache() {
        cacheMutex.withLock {
            trackCache.clear()
        }
    }
    
    private fun QueueEntity.toTrack(): Track {
        return Track(
            trackHash = trackHash,
            album = album,
            albumHash = albumHash,
            bitrate = bitrate,
            duration = duration,
            filepath = filepath,
            folder = folder,
            image = image,
            isFavorite = isFavorite,
            title = title,
            albumTrackArtists = albumTrackArtists.map { 
                TrackArtist(artistHash = it.artistHash, name = it.name, image = it.image) 
            },
            trackArtists = trackArtists.map { 
                TrackArtist(artistHash = it.artistHash, name = it.name, image = it.image) 
            },
            disc = disc,
            trackNumber = trackNumber
        )
    }
}
