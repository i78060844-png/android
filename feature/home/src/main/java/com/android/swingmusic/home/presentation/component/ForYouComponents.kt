package com.android.swingmusic.home.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.swingmusic.core.domain.model.Recommendation
import com.android.swingmusic.core.domain.model.RecommendationReason
import com.android.swingmusic.core.domain.model.Track
import com.android.swingmusic.core.domain.model.TrackStats
import com.android.swingmusic.core.domain.repository.DataHealth
import com.android.swingmusic.uicomponent.R as UiComponent

// Color constants
private val CardColor = Color(0xFF151515)
private val CardAltColor = Color(0xFF1F1F1F)
private val AccentColor = Color(0xFFF5F5F5)
private val MutedColor = Color(0xFF9C9C9C)
private val GreenAccent = Color(0xFF1DB954)

/**
 * "В моменте" section - shows tracks user typically listens to at this time
 */
@Composable
fun InTheMomentSection(
    recommendations: List<Recommendation>,
    baseUrl: String,
    currentHour: Int,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(GreenAccent)
            )
            Text(
                text = "В моменте",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AccentColor
            )
            Text(
                text = "• ${formatTimeWindow(currentHour)}",
                style = MaterialTheme.typography.bodySmall,
                color = MutedColor
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = "Треки, которые вы обычно слушаете сейчас",
            style = MaterialTheme.typography.bodySmall,
            color = MutedColor
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(recommendations, key = { it.track.trackHash }) { rec ->
                InTheMomentCard(
                    recommendation = rec,
                    baseUrl = baseUrl,
                    onClick = { onTrackClick(rec.track) }
                )
            }
        }
    }
}

@Composable
private fun InTheMomentCard(
    recommendation: Recommendation,
    baseUrl: String,
    onClick: () -> Unit
) {
    val track = recommendation.track
    val cardShape = RoundedCornerShape(6.dp)
    
    Box(
        modifier = Modifier
            .width(150.dp)
            .clip(cardShape)
            .background(CardColor)
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("${baseUrl}img/thumbnail/${track.image}")
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(id = UiComponent.drawable.audio_fallback),
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop
                )
                
                // Streak badge if applicable
                if (recommendation.reason is RecommendationReason.TimeBasedHabit) {
                    val habit = recommendation.reason as RecommendationReason.TimeBasedHabit
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(GreenAccent)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${habit.streakDays} дн.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = AccentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = recommendation.contextMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * "Ваш топ за неделю" section
 */
@Composable
fun TopTracksSection(
    tracks: List<Pair<Track, TrackStats>>,
    baseUrl: String,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = "Ваш топ за неделю",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AccentColor
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(tracks.take(10), key = { it.first.trackHash }) { (track, stats) ->
                TopTrackCard(
                    track = track,
                    stats = stats,
                    rank = tracks.indexOf(track to stats) + 1,
                    baseUrl = baseUrl,
                    onClick = { onTrackClick(track) }
                )
            }
        }
    }
}

@Composable
private fun TopTrackCard(
    track: Track,
    stats: TrackStats,
    rank: Int,
    baseUrl: String,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(6.dp)
    
    Box(
        modifier = Modifier
            .size(130.dp)
            .clip(cardShape)
            .background(CardColor)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = ImageRequest.Builder(LocalContext.current)
                .data("${baseUrl}img/thumbnail/${track.image}")
                .crossfade(true)
                .build(),
            placeholder = painterResource(id = UiComponent.drawable.audio_fallback),
            contentDescription = track.title,
            contentScale = ContentScale.Crop
        )
        
        // Rank badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = AccentColor,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Title overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.6f to Color.Black.copy(alpha = 0.3f),
                        1f to Color.Black.copy(alpha = 0.8f)
                    )
                )
                .padding(8.dp)
        ) {
            Column {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${stats.totalPlays} прослушиваний",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * "Вспомните" section - tracks user loved but hasn't played recently
 */
@Composable
fun RediscoverSection(
    recommendations: List<Recommendation>,
    baseUrl: String,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = "Вспомните",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AccentColor
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = "Треки, которые вы давно не слушали",
            style = MaterialTheme.typography.bodySmall,
            color = MutedColor
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(recommendations, key = { it.track.trackHash }) { rec ->
                RediscoverCard(
                    recommendation = rec,
                    baseUrl = baseUrl,
                    onClick = { onTrackClick(rec.track) }
                )
            }
        }
    }
}

@Composable
private fun RediscoverCard(
    recommendation: Recommendation,
    baseUrl: String,
    onClick: () -> Unit
) {
    val track = recommendation.track
    val cardShape = RoundedCornerShape(6.dp)
    
    Box(
        modifier = Modifier
            .size(130.dp)
            .clip(cardShape)
            .background(CardColor)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = ImageRequest.Builder(LocalContext.current)
                .data("${baseUrl}img/thumbnail/${track.image}")
                .crossfade(true)
                .build(),
            placeholder = painterResource(id = UiComponent.drawable.audio_fallback),
            contentDescription = track.title,
            contentScale = ContentScale.Crop
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.6f to Color.Black.copy(alpha = 0.3f),
                        1f to Color.Black.copy(alpha = 0.7f)
                    )
                )
                .padding(8.dp)
        ) {
            Column {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (recommendation.reason is RecommendationReason.Rediscovery) {
                    val rediscovery = recommendation.reason as RecommendationReason.Rediscovery
                    Text(
                        text = "${rediscovery.daysSinceLastPlay} дн. назад",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Time of day mix section
 */
@Composable
fun TimeOfDayMixSection(
    timeOfDayName: String,
    tracks: List<Track>,
    baseUrl: String,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(id = UiComponent.drawable.play_arrow_fill),
                contentDescription = null,
                tint = GreenAccent
            )
            Text(
                text = timeOfDayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AccentColor
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = "Подборка на основе ваших предпочтений",
            style = MaterialTheme.typography.bodySmall,
            color = MutedColor
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(tracks, key = { it.trackHash }) { track ->
                SimpleTrackCard(
                    track = track,
                    baseUrl = baseUrl,
                    onClick = { onTrackClick(track) }
                )
            }
        }
    }
}

@Composable
private fun SimpleTrackCard(
    track: Track,
    baseUrl: String,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(6.dp)
    
    Box(
        modifier = Modifier
            .size(130.dp)
            .clip(cardShape)
            .background(CardColor)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = ImageRequest.Builder(LocalContext.current)
                .data("${baseUrl}img/thumbnail/${track.image}")
                .crossfade(true)
                .build(),
            placeholder = painterResource(id = UiComponent.drawable.audio_fallback),
            contentDescription = track.title,
            contentScale = ContentScale.Crop
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.6f to Color.Black.copy(alpha = 0.3f),
                        1f to Color.Black.copy(alpha = 0.7f)
                    )
                )
                .padding(8.dp)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Data collection progress card - shown when not enough data
 */
@Composable
fun DataCollectionCard(
    dataHealth: DataHealth,
    minEvents: Int,
    modifier: Modifier = Modifier
) {
    val progress = (dataHealth.totalEvents.toFloat() / minEvents).coerceIn(0f, 1f)
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = CardAltColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(id = UiComponent.drawable.swing_music_logo_rounded),
                    contentDescription = null,
                    tint = GreenAccent
                )
                Text(
                    text = "Персонализация",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentColor
                )
            }
            
            Text(
                text = "Мы изучаем ваши музыкальные вкусы. Чем больше вы слушаете, тем точнее будут рекомендации.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedColor
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Прогресс",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedColor
                    )
                    Text(
                        text = "${dataHealth.totalEvents}/$minEvents треков",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentColor
                    )
                }
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = GreenAccent,
                    trackColor = CardColor
                )
            }
            
            if (dataHealth.uniqueTracksPlayed > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "Треков",
                        value = dataHealth.uniqueTracksPlayed.toString()
                    )
                    StatItem(
                        label = "Артистов",
                        value = dataHealth.uniqueArtistsPlayed.toString()
                    )
                    StatItem(
                        label = "За 7 дней",
                        value = dataHealth.eventsLast7Days.toString()
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AccentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MutedColor
        )
    }
}

private fun formatTimeWindow(currentHour: Int): String {
    val startHour = (currentHour - 1).coerceAtLeast(0)
    val endHour = (currentHour + 1).coerceAtMost(23)
    return "$startHour:00 - $endHour:59"
}
