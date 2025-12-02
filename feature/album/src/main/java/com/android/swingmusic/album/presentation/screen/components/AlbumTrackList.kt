package com.android.swingmusic.album.presentation.screen.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.swingmusic.core.domain.model.Track
import com.android.swingmusic.core.domain.util.PlaybackState
import com.android.swingmusic.uicomponent.presentation.component.TrackItem

@Composable
fun DiscHeader(discNumber: Int) {
    Text(
        text = "Disc $discNumber",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .75f),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
fun AlbumTrackItem(
    track: Track,
    baseUrl: String,
    isCurrentTrack: Boolean,
    playbackState: PlaybackState,
    onClickTrack: () -> Unit,
    onClickMore: (Track) -> Unit
) {
    TrackItem(
        track = track,
        baseUrl = baseUrl,
        isAlbumTrack = true,
        showMenuIcon = true,
        isCurrentTrack = isCurrentTrack,
        playbackState = playbackState,
        onClickTrackItem = onClickTrack,
        onClickMoreVert = { onClickMore(it) }
    )
}
