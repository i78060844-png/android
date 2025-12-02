package com.android.swingmusic.database.data.repository

import com.android.swingmusic.core.domain.model.AlbumStats
import com.android.swingmusic.core.domain.model.ArtistStats
import com.android.swingmusic.core.domain.model.HomeSection
import com.android.swingmusic.core.domain.model.ListeningEvent
import com.android.swingmusic.core.domain.model.ListeningPattern
import com.android.swingmusic.core.domain.model.ListeningSource
import com.android.swingmusic.core.domain.model.Recommendation
import com.android.swingmusic.core.domain.model.RecommendationReason
import com.android.swingmusic.core.domain.model.StatsPeriod
import com.android.swingmusic.core.domain.model.Track
import com.android.swingmusic.core.domain.model.TrackStats
import com.android.swingmusic.core.domain.repository.DataHealth
import com.android.swingmusic.core.domain.repository.MusicForYouRepository
import com.android.swingmusic.core.domain.repository.TrackCacheRepository
import com.android.swingmusic.database.data.dao.ListeningHistoryDao
import com.android.swingmusic.database.data.entity.ArtistStatsCacheEntity
import com.android.swingmusic.database.data.entity.ListeningEventEntity
import com.android.swingmusic.database.data.entity.ListeningPatternEntity
import com.android.swingmusic.database.data.entity.TrackStatsCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicForYouRepositoryImpl @Inject constructor(
    private val listeningHistoryDao: ListeningHistoryDao,
    private val trackCacheRepository: TrackCacheRepository
) : MusicForYouRepository {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        const val MIN_EVENTS_FOR_BASIC_RECS = 20
        const val MIN_EVENTS_FOR_ADVANCED_RECS = 100
        const val MIN_EVENTS_FOR_TIME_PATTERNS = 50
        const val COMPLETED_PLAY_THRESHOLD = 0.8f // 80% of track
        const val SKIP_THRESHOLD_MS = 30_000L // 30 seconds
        const val REDISCOVERY_DAYS_THRESHOLD = 14 // 2 weeks
        const val STREAK_DAYS_FOR_HABIT = 3
    }
    
    // ==================== EVENT TRACKING ====================
    
    override suspend fun recordListeningEvent(event: ListeningEvent) = withContext(Dispatchers.IO) {
        val entity = event.toEntity()
        listeningHistoryDao.insertListeningEvent(entity)
        
        // Update stats caches asynchronously
        updateTrackStatsCache(event.trackHash)
        event.artistHashes.forEach { updateArtistStatsCache(it) }
        if (event.albumHash.isNotBlank()) {
            updateAlbumStatsCache(event.albumHash)
        }
        updateListeningPattern(event.hourOfDay, event.dayOfWeek)
    }
    
    override suspend fun onTrackStarted(track: Track, source: ListeningSource) {
        // We record start events for tracking what was initiated
        // but the real event is recorded on end
    }
    
    override suspend fun onTrackEnded(track: Track, listenDurationMs: Long, skipped: Boolean) {
        val calendar = Calendar.getInstance()
        val completedPlay = listenDurationMs >= (track.duration * 1000 * COMPLETED_PLAY_THRESHOLD)
        
        val event = ListeningEvent(
            trackHash = track.trackHash,
            albumHash = track.albumHash,
            artistHashes = track.trackArtists.map { it.artistHash },
            timestamp = System.currentTimeMillis(),
            dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
            hourOfDay = calendar.get(Calendar.HOUR_OF_DAY),
            listenDurationMs = listenDurationMs,
            trackDurationMs = track.duration * 1000L,
            completedPlay = completedPlay,
            skipped = skipped || listenDurationMs < SKIP_THRESHOLD_MS,
            source = ListeningSource.UNKNOWN
        )
        
        recordListeningEvent(event)
    }
    
    // ==================== STATISTICS ====================
    
    override suspend fun getTrackStats(trackHash: String): TrackStats? = withContext(Dispatchers.IO) {
        listeningHistoryDao.getTrackStats(trackHash)?.toDomain()
    }
    
    override suspend fun getArtistStats(artistHash: String): ArtistStats? = withContext(Dispatchers.IO) {
        listeningHistoryDao.getArtistStats(artistHash)?.toDomain()
    }
    
    override suspend fun getAlbumStats(albumHash: String): AlbumStats? = withContext(Dispatchers.IO) {
        listeningHistoryDao.getAlbumStats(albumHash)?.let { entity ->
            AlbumStats(
                albumHash = entity.albumHash,
                totalPlays = entity.totalPlays,
                uniqueTracksPlayed = entity.uniqueTracksPlayed,
                totalListenTimeMs = entity.totalListenTimeMs,
                lastPlayedAt = entity.lastPlayedAt,
                completionRate = entity.completionRate
            )
        }
    }
    
    override suspend fun getTopTracks(period: StatsPeriod, limit: Int): List<Pair<Track, TrackStats>> = 
        withContext(Dispatchers.IO) {
            val since = getPeriodStartTime(period)
            val topTrackHashes = listeningHistoryDao.getTopTracksSince(since, limit)
            
            topTrackHashes.mapNotNull { playCount ->
                val track = trackCacheRepository.getTrackByHash(playCount.trackHash)
                val stats = getTrackStats(playCount.trackHash)
                if (track != null && stats != null) {
                    track to stats
                } else null
            }
        }
    
    override suspend fun getTopArtists(period: StatsPeriod, limit: Int): List<ArtistStats> =
        withContext(Dispatchers.IO) {
            listeningHistoryDao.getTopArtists(limit).map { it.toDomain() }
        }
    
    override suspend fun getTopAlbums(period: StatsPeriod, limit: Int): List<AlbumStats> =
        withContext(Dispatchers.IO) {
            listeningHistoryDao.getTopAlbums(limit).map { entity ->
                AlbumStats(
                    albumHash = entity.albumHash,
                    totalPlays = entity.totalPlays,
                    uniqueTracksPlayed = entity.uniqueTracksPlayed,
                    totalListenTimeMs = entity.totalListenTimeMs,
                    lastPlayedAt = entity.lastPlayedAt,
                    completionRate = entity.completionRate
                )
            }
        }
    
    override suspend fun getTotalListeningTime(period: StatsPeriod): Long = withContext(Dispatchers.IO) {
        val since = getPeriodStartTime(period)
        listeningHistoryDao.getEventsSince(since).sumOf { it.listenDurationMs }
    }
    
    // ==================== PATTERNS ====================
    
    override suspend fun getListeningPatterns(): List<ListeningPattern> = withContext(Dispatchers.IO) {
        listeningHistoryDao.getAllPatterns().map { it.toDomain() }
    }
    
    override suspend fun getCurrentTimeSlotPattern(): ListeningPattern? = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        listeningHistoryDao.getPatternForSlot(hour, dayOfWeek)?.toDomain()
    }
    
    override suspend fun hasHabitAt(hourOfDay: Int, dayOfWeek: Int): Boolean = withContext(Dispatchers.IO) {
        val pattern = listeningHistoryDao.getPatternForSlot(hourOfDay, dayOfWeek)
        pattern != null && pattern.totalSessions >= STREAK_DAYS_FOR_HABIT
    }
    
    // ==================== RECOMMENDATIONS ====================
    
    override suspend fun getInTheMomentRecommendations(): List<Recommendation> = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        // Get tracks with streaks that typically play around this time
        val tracksWithStreaks = listeningHistoryDao.getTracksWithStreaks(20)
        
        val recommendations = mutableListOf<Recommendation>()
        
        for (statsEntity in tracksWithStreaks) {
            val playsByHour = parsePlaysByHour(statsEntity.playsByHourJson)
            
            // Check if this track is commonly played within ±1 hour of current time
            val nearbyHours = listOf(currentHour - 1, currentHour, currentHour + 1)
                .map { if (it < 0) it + 24 else if (it > 23) it - 24 else it }
            
            val playsNearThisTime = nearbyHours.sumOf { playsByHour[it] ?: 0 }
            
            if (playsNearThisTime >= 3 && statsEntity.streak >= STREAK_DAYS_FOR_HABIT) {
                val track = trackCacheRepository.getTrackByHash(statsEntity.trackHash)
                if (track != null) {
                    recommendations.add(
                        Recommendation(
                            track = track,
                            reason = RecommendationReason.TimeBasedHabit(
                                usualHour = currentHour,
                                streakDays = statsEntity.streak
                            ),
                            confidence = calculateHabitConfidence(statsEntity.streak, playsNearThisTime),
                            contextMessage = buildHabitMessage(statsEntity.streak, currentHour)
                        )
                    )
                }
            }
        }
        
        // Also add frequently played tracks for this time window
        val topForTime = listeningHistoryDao.getTopTracksForTimeWindow(
            startHour = (currentHour - 1).coerceAtLeast(0),
            endHour = (currentHour + 1).coerceAtMost(23),
            limit = 5
        )
        
        for (playCount in topForTime) {
            if (recommendations.none { it.track.trackHash == playCount.trackHash }) {
                val track = trackCacheRepository.getTrackByHash(playCount.trackHash)
                if (track != null) {
                    recommendations.add(
                        Recommendation(
                            track = track,
                            reason = RecommendationReason.FrequentlyPlayed(
                                playCount = playCount.playCount,
                                lastPlayed = System.currentTimeMillis()
                            ),
                            confidence = 0.7f,
                            contextMessage = "Часто слушаете в это время"
                        )
                    )
                }
            }
        }
        
        recommendations.sortedByDescending { it.confidence }.take(8)
    }
    
    override suspend fun getRediscoveryRecommendations(limit: Int): List<Recommendation> = 
        withContext(Dispatchers.IO) {
            val threshold = System.currentTimeMillis() - (REDISCOVERY_DAYS_THRESHOLD * 24 * 60 * 60 * 1000L)
            val forgottenFavorites = listeningHistoryDao.getForgottenFavorites(threshold, minPlays = 5, limit = limit)
            
            forgottenFavorites.mapNotNull { statsEntity ->
                val track = trackCacheRepository.getTrackByHash(statsEntity.trackHash)
                if (track != null) {
                    val daysSince = ((System.currentTimeMillis() - statsEntity.lastPlayedAt) / (24 * 60 * 60 * 1000)).toInt()
                    Recommendation(
                        track = track,
                        reason = RecommendationReason.Rediscovery(
                            daysSinceLastPlay = daysSince,
                            previousPlayCount = statsEntity.totalPlays
                        ),
                        confidence = calculateRediscoveryConfidence(statsEntity.totalPlays, daysSince),
                        contextMessage = "Вы слушали ${statsEntity.totalPlays} раз, но не включали уже $daysSince дней"
                    )
                } else null
            }
        }
    
    override suspend fun getArtistBasedRecommendations(artistHash: String, limit: Int): List<Recommendation> =
        withContext(Dispatchers.IO) {
            val artistStats = listeningHistoryDao.getArtistStats(artistHash)
            if (artistStats == null) return@withContext emptyList()
            
            // Get tracks by this artist that user hasn't played much
            // This would require more complex queries - simplified here
            emptyList()
        }
    
    override suspend fun getTimeOfDayMix(limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        
        // Expand time window based on time of day
        val (startHour, endHour) = when (currentHour) {
            in 5..11 -> 5 to 11 // Morning
            in 12..16 -> 12 to 16 // Afternoon  
            in 17..21 -> 17 to 21 // Evening
            else -> 22 to 4 // Night
        }
        
        val topTracks = if (startHour <= endHour) {
            listeningHistoryDao.getTopTracksForTimeWindow(startHour, endHour, limit)
        } else {
            // Handle overnight window
            val eveningTracks = listeningHistoryDao.getTopTracksForTimeWindow(startHour, 23, limit / 2)
            val nightTracks = listeningHistoryDao.getTopTracksForTimeWindow(0, endHour, limit / 2)
            (eveningTracks + nightTracks).sortedByDescending { it.playCount }.take(limit)
        }
        
        topTracks.mapNotNull { trackCacheRepository.getTrackByHash(it.trackHash) }
    }
    
    override suspend fun getPersonalizedHomeSections(): List<HomeSection> = withContext(Dispatchers.IO) {
        val sections = mutableListOf<HomeSection>()
        val hasEnoughData = hasEnoughDataForPersonalization()
        
        if (!hasEnoughData) {
            // Return minimal sections for new users
            return@withContext sections
        }
        
        // 1. In The Moment (if we detect a pattern for current time)
        val inTheMomentRecs = getInTheMomentRecommendations()
        if (inTheMomentRecs.isNotEmpty()) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            sections.add(
                HomeSection.InTheMoment(
                    recommendations = inTheMomentRecs,
                    timeWindow = (hour - 1).coerceAtLeast(0) to (hour + 1).coerceAtMost(23)
                )
            )
        }
        
        // 2. Your Top Tracks (this week)
        val topTracks = getTopTracks(StatsPeriod.THIS_WEEK, 10)
        if (topTracks.isNotEmpty()) {
            sections.add(
                HomeSection.YourTopTracks(
                    tracks = topTracks.map { it.first },
                    period = StatsPeriod.THIS_WEEK
                )
            )
        }
        
        // 3. Rediscover
        val rediscoveryRecs = getRediscoveryRecommendations(8)
        if (rediscoveryRecs.isNotEmpty()) {
            sections.add(HomeSection.Rediscover(rediscoveryRecs))
        }
        
        // 4. Time of Day Mix
        val timeOfDayMix = getTimeOfDayMix(12)
        if (timeOfDayMix.isNotEmpty()) {
            val timeOfDay = getTimeOfDayName()
            sections.add(
                HomeSection.TimeOfDayMix(
                    timeOfDay = timeOfDay,
                    tracks = timeOfDayMix
                )
            )
        }
        
        // 5. Recently Played
        val recentStats = listeningHistoryDao.getRecentlyPlayedTrackStats(15)
        val recentTracks = recentStats.mapNotNull { trackCacheRepository.getTrackByHash(it.trackHash) }
        if (recentTracks.isNotEmpty()) {
            sections.add(HomeSection.RecentlyPlayed(recentTracks))
        }
        
        sections
    }
    
    override suspend fun hasEnoughDataForPersonalization(): Boolean = withContext(Dispatchers.IO) {
        listeningHistoryDao.getTotalEventCount() >= MIN_EVENTS_FOR_BASIC_RECS
    }
    
    override fun getMinEventsForPersonalization(): Int = MIN_EVENTS_FOR_BASIC_RECS
    
    // ==================== FLOWS ====================
    
    override fun observeTopTracks(limit: Int): Flow<List<TrackStats>> {
        return listeningHistoryDao.observeTopTracks(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun observeTopArtists(limit: Int): Flow<List<ArtistStats>> {
        return listeningHistoryDao.observeTopArtists(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun observeTotalPlays(): Flow<Int> {
        return listeningHistoryDao.observeTotalPlays()
    }
    
    // ==================== MAINTENANCE ====================
    
    override suspend fun refreshStatsCache() = withContext(Dispatchers.IO) {
        // Recalculate stats for all tracks with recent activity
        val staleStats = listeningHistoryDao.getStaleTrackStats(100)
        staleStats.forEach { updateTrackStatsCache(it.trackHash) }
    }
    
    override suspend fun cleanupOldEvents(keepDays: Int) = withContext(Dispatchers.IO) {
        val threshold = System.currentTimeMillis() - (keepDays * 24 * 60 * 60 * 1000L)
        listeningHistoryDao.deleteEventsBefore(threshold)
    }
    
    override suspend fun getDataHealth(): DataHealth = withContext(Dispatchers.IO) {
        val totalEvents = listeningHistoryDao.getTotalEventCount()
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        
        val eventsLast7Days = listeningHistoryDao.getEventsSince(sevenDaysAgo).size
        val eventsLast30Days = listeningHistoryDao.getEventsSince(thirtyDaysAgo).size
        
        val recentEvents = listeningHistoryDao.getRecentEvents(1000)
        val uniqueTracks = recentEvents.map { it.trackHash }.distinct().size
        val uniqueArtists = recentEvents.flatMap { it.artistHashes.split(",") }.distinct().size
        
        DataHealth(
            totalEvents = totalEvents,
            eventsLast7Days = eventsLast7Days,
            eventsLast30Days = eventsLast30Days,
            uniqueTracksPlayed = uniqueTracks,
            uniqueArtistsPlayed = uniqueArtists,
            oldestEventDate = recentEvents.lastOrNull()?.timestamp,
            hasEnoughForBasicRecs = totalEvents >= MIN_EVENTS_FOR_BASIC_RECS,
            hasEnoughForAdvancedRecs = totalEvents >= MIN_EVENTS_FOR_ADVANCED_RECS,
            hasEnoughForTimePatterns = totalEvents >= MIN_EVENTS_FOR_TIME_PATTERNS
        )
    }
    
    // ==================== PRIVATE HELPERS ====================
    
    private suspend fun updateTrackStatsCache(trackHash: String) {
        val events = listeningHistoryDao.getEventsForTrack(trackHash)
        if (events.isEmpty()) return
        
        val totalPlays = events.size
        val completedPlays = events.count { it.completedPlay }
        val skips = events.count { it.skipped }
        val totalListenTime = events.sumOf { it.listenDurationMs }
        val lastPlayedAt = events.maxOf { it.timestamp }
        val firstPlayedAt = events.minOf { it.timestamp }
        
        val avgCompletion = if (events.isNotEmpty()) {
            events.map { it.listenDurationMs.toFloat() / it.trackDurationMs }.average().toFloat()
        } else 0f
        
        val playsByHour = events.groupingBy { it.hourOfDay }.eachCount()
        val playsByDay = events.groupingBy { it.dayOfWeek }.eachCount()
        
        val streak = calculateStreak(events)
        val streakStart = if (streak > 0) {
            events.sortedByDescending { it.timestamp }
                .take(streak)
                .minOfOrNull { it.timestamp }
        } else null
        
        val statsEntity = TrackStatsCacheEntity(
            trackHash = trackHash,
            totalPlays = totalPlays,
            completedPlays = completedPlays,
            skips = skips,
            totalListenTimeMs = totalListenTime,
            lastPlayedAt = lastPlayedAt,
            firstPlayedAt = firstPlayedAt,
            averageCompletionRate = avgCompletion,
            playsByHourJson = json.encodeToString(playsByHour),
            playsByDayOfWeekJson = json.encodeToString(playsByDay),
            streak = streak,
            currentStreakStartDate = streakStart,
            lastUpdated = System.currentTimeMillis()
        )
        
        listeningHistoryDao.upsertTrackStats(statsEntity)
    }
    
    private suspend fun updateArtistStatsCache(artistHash: String) {
        val artistHashPattern = "%$artistHash%"
        // Simplified - would need proper query
    }
    
    private suspend fun updateAlbumStatsCache(albumHash: String) {
        // Simplified - would need proper implementation
    }
    
    private suspend fun updateListeningPattern(hour: Int, dayOfWeek: Int) {
        val existing = listeningHistoryDao.getPatternForSlot(hour, dayOfWeek)
        val newSessions = (existing?.totalSessions ?: 0) + 1
        
        // Get top tracks for this slot
        val topTracks = listeningHistoryDao.getTopTracksForTimeWindow(hour, hour, 5)
        
        val pattern = ListeningPatternEntity(
            hourOfDay = hour,
            dayOfWeek = dayOfWeek,
            avgPlaysPerSession = (existing?.avgPlaysPerSession ?: 1f),
            commonTrackHashes = topTracks.joinToString(",") { it.trackHash },
            totalSessions = newSessions,
            lastUpdated = System.currentTimeMillis()
        )
        
        listeningHistoryDao.upsertListeningPattern(pattern)
    }
    
    private fun calculateStreak(events: List<ListeningEventEntity>): Int {
        if (events.isEmpty()) return 0
        
        val calendar = Calendar.getInstance()
        val today = calendar.apply { 
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        var streak = 0
        var checkDate = today
        
        while (true) {
            val dayStart = checkDate
            val dayEnd = checkDate + 24 * 60 * 60 * 1000
            
            val hasPlayOnDay = events.any { it.timestamp in dayStart until dayEnd }
            if (hasPlayOnDay) {
                streak++
                checkDate -= 24 * 60 * 60 * 1000 // Go back one day
            } else {
                break
            }
        }
        
        return streak
    }
    
    private fun getPeriodStartTime(period: StatsPeriod): Long {
        val calendar = Calendar.getInstance()
        return when (period) {
            StatsPeriod.TODAY -> {
                calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
            }
            StatsPeriod.THIS_WEEK -> {
                calendar.apply {
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
            }
            StatsPeriod.THIS_MONTH -> {
                calendar.apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
            }
            StatsPeriod.ALL_TIME -> 0L
        }
    }
    
    private fun parsePlaysByHour(jsonStr: String): Map<Int, Int> {
        return try {
            json.decodeFromString<Map<Int, Int>>(jsonStr)
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    private fun calculateHabitConfidence(streak: Int, playsNearTime: Int): Float {
        val streakFactor = (streak.coerceAtMost(7) / 7f) * 0.5f
        val playsFactor = (playsNearTime.coerceAtMost(10) / 10f) * 0.5f
        return (streakFactor + playsFactor).coerceIn(0f, 1f)
    }
    
    private fun calculateRediscoveryConfidence(totalPlays: Int, daysSince: Int): Float {
        val playsFactor = (totalPlays.coerceAtMost(20) / 20f) * 0.6f
        val timeFactor = (daysSince.coerceIn(14, 60) - 14) / 46f * 0.4f
        return (playsFactor + timeFactor).coerceIn(0f, 1f)
    }
    
    private fun buildHabitMessage(streak: Int, hour: Int): String {
        val timeStr = when (hour) {
            in 5..11 -> "по утрам"
            in 12..16 -> "днём"
            in 17..21 -> "вечером"
            else -> "ночью"
        }
        return "Вы слушаете этот трек $timeStr уже $streak дней подряд"
    }
    
    private fun getTimeOfDayName(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Утренний"
            in 12..16 -> "Дневной"
            in 17..21 -> "Вечерний"
            else -> "Ночной"
        }
    }
    
    // ==================== MAPPERS ====================
    
    private fun ListeningEvent.toEntity() = ListeningEventEntity(
        id = id,
        trackHash = trackHash,
        albumHash = albumHash,
        artistHashes = artistHashes.joinToString(","),
        timestamp = timestamp,
        dayOfWeek = dayOfWeek,
        hourOfDay = hourOfDay,
        listenDurationMs = listenDurationMs,
        trackDurationMs = trackDurationMs,
        completedPlay = completedPlay,
        skipped = skipped,
        source = source.name
    )
    
    private fun TrackStatsCacheEntity.toDomain() = TrackStats(
        trackHash = trackHash,
        totalPlays = totalPlays,
        completedPlays = completedPlays,
        skips = skips,
        totalListenTimeMs = totalListenTimeMs,
        lastPlayedAt = lastPlayedAt,
        firstPlayedAt = firstPlayedAt,
        averageCompletionRate = averageCompletionRate,
        playsByHour = parsePlaysByHour(playsByHourJson),
        playsByDayOfWeek = try { json.decodeFromString(playsByDayOfWeekJson) } catch (e: Exception) { emptyMap() },
        streak = streak,
        currentStreakStartDate = currentStreakStartDate
    )
    
    private fun ArtistStatsCacheEntity.toDomain() = ArtistStats(
        artistHash = artistHash,
        totalPlays = totalPlays,
        uniqueTracksPlayed = uniqueTracksPlayed,
        totalListenTimeMs = totalListenTimeMs,
        lastPlayedAt = lastPlayedAt,
        favoriteTimeOfDay = favoriteTimeOfDay,
        favoriteDayOfWeek = favoriteDayOfWeek
    )
    
    private fun ListeningPatternEntity.toDomain() = ListeningPattern(
        hourOfDay = hourOfDay,
        dayOfWeek = dayOfWeek,
        avgPlaysPerSession = avgPlaysPerSession,
        commonGenres = emptyList(), // Would need genre data
        commonMoods = emptyList(),
        preferredTempo = null
    )
}
