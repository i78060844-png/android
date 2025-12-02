package com.android.swingmusic.home.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.swingmusic.common.presentation.navigator.CommonNavigator
import com.android.swingmusic.core.domain.model.Folder
import com.android.swingmusic.core.domain.model.Track
import com.android.swingmusic.core.domain.model.TrackArtist
import com.android.swingmusic.core.domain.util.QueueSource
import com.android.swingmusic.folder.presentation.viewmodel.FoldersViewModel
import com.android.swingmusic.player.presentation.event.QueueEvent
import com.android.swingmusic.player.presentation.viewmodel.MediaControllerViewModel
import com.android.swingmusic.uicomponent.R as UiComponent
import com.ramcosta.composedestinations.annotation.Destination
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Destination
@Composable
fun Home(
    navigator: CommonNavigator,
    mediaControllerViewModel: MediaControllerViewModel,
    foldersViewModel: FoldersViewModel
) {
    val playerUiState by mediaControllerViewModel.playerUiState.collectAsState()
    val baseUrl by mediaControllerViewModel.baseUrl.collectAsState()
    val foldersState by foldersViewModel.foldersAndTracks

    LaunchedEffect(Unit) {
        foldersViewModel.fetchRootDirectoriesWhenReady()
        mediaControllerViewModel.refreshBaseUrl()
    }

    val libraryTracks = foldersState.foldersAndTracks.tracks

    val continueListening = remember(playerUiState.queue, libraryTracks, playerUiState.nowPlayingTrack) {
        when {
            playerUiState.queue.isNotEmpty() -> playerUiState.queue
            libraryTracks.isNotEmpty() -> libraryTracks
            playerUiState.nowPlayingTrack != null -> listOf(playerUiState.nowPlayingTrack!!)
            else -> emptyList()
        }.distinctBy { it.trackHash }.take(8)
    }

    val albumHighlights = remember(continueListening) {
        continueListening
            .filter { it.albumHash.isNotBlank() }
            .distinctBy { it.albumHash }
            .map {
                AlbumHighlight(
                    albumHash = it.albumHash,
                    title = it.album,
                    artist = it.trackArtists.firstOrNull()?.name ?: "Unknown artist",
                    image = it.image
                )
            }
            .take(6)
    }

    val artistHighlights = remember(continueListening) {
        continueListening
            .flatMap(Track::trackArtists)
            .distinctBy(TrackArtist::artistHash)
            .map { ArtistHighlight(it.artistHash, it.name) }
            .take(10)
    }

    val folderShortcuts = remember(foldersState.foldersAndTracks.folders) {
        foldersState.foldersAndTracks.folders.take(6)
    }

    val greeting = remember { resolveGreeting() }
    val currentTrackTitle = playerUiState.nowPlayingTrack?.title

    val quickActions = remember(navigator) {
        listOf(
            QuickAction(UiComponent.drawable.folder_filled, "Library") { navigator.gotoFolders() },
            QuickAction(UiComponent.drawable.ic_album, "Albums") { navigator.gotoAlbumLibrary() },
            QuickAction(UiComponent.drawable.ic_artist, "Artists") { navigator.gotoArtistLibrary() },
            QuickAction(UiComponent.drawable.ic_search, "Search") { navigator.gotoSearch() }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = currentTrackTitle?.let { "Listening to $it" } ?: "Dive back into your music",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SearchCard(onClick = navigator::gotoSearch)
        }

        item {
            QuickActionGrid(actions = quickActions)
        }

        if (continueListening.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Continue listening",
                    actionLabel = "Queue",
                    onActionClick = navigator::gotoQueueScreen
                )
            }

            item {
                ContinueListeningRow(
                    tracks = continueListening,
                    baseUrl = baseUrl.orEmpty(),
                    nowPlayingHash = playerUiState.nowPlayingTrack?.trackHash,
                    onPlayTrack = { track ->
                        playFromHome(
                            track = track,
                            activeQueue = playerUiState.queue,
                            fallbackQueue = continueListening,
                            controller = mediaControllerViewModel
                        )
                    }
                )
            }
        }

        if (albumHighlights.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Popular albums",
                    actionLabel = "Browse",
                    onActionClick = navigator::gotoAlbumLibrary
                )
            }

            item {
                AlbumHighlightsRow(
                    highlights = albumHighlights,
                    baseUrl = baseUrl.orEmpty(),
                    onAlbumClick = navigator::gotoAlbumWithInfo
                )
            }
        }

        if (folderShortcuts.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Jump back in",
                    actionLabel = "Folders",
                    onActionClick = navigator::gotoFolders
                )
            }

            item {
                FolderRow(
                    folders = folderShortcuts,
                    onFolderClick = { folder ->
                        val folderName = folder.name.ifBlank { "Library" }
                        navigator.gotoSourceFolder(folderName, folder.path)
                    }
                )
            }
        }

        if (artistHighlights.isNotEmpty()) {
            item {
                SectionHeader(title = "Trending artists")
            }

            item {
                ArtistSpotlightRow(
                    artists = artistHighlights,
                    onClick = navigator::gotoArtistInfo
                )
            }
        }
    }
}

@Composable
private fun SearchCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(id = UiComponent.drawable.ic_search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Search anything",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun QuickActionGrid(actions: List<QuickAction>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actions.forEach { action ->
            Surface(
                shape = MaterialTheme.shapes.small,
                tonalElevation = 1.dp,
                modifier = Modifier.clickable(onClick = action.onClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = action.iconRes),
                        contentDescription = action.label,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueListeningRow(
    tracks: List<Track>,
    baseUrl: String,
    nowPlayingHash: String?,
    onPlayTrack: (Track) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(tracks, key = { it.trackHash }) { track ->
            val isCurrent = nowPlayingHash == track.trackHash
            Surface(
                modifier = Modifier
                    .size(width = 160.dp, height = 220.dp)
                    .clickable { onPlayTrack(track) },
                shape = MaterialTheme.shapes.medium,
                tonalElevation = if (isCurrent) 6.dp else 2.dp,
                border = if (isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .4f)) else null
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(MaterialTheme.shapes.small),
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("${baseUrl}img/thumbnail/${track.image}")
                            .crossfade(true)
                            .build(),
                        placeholder = painterResource(id = UiComponent.drawable.audio_fallback),
                        contentDescription = track.title
                    )
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.trackArtists.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumHighlightsRow(
    highlights: List<AlbumHighlight>,
    baseUrl: String,
    onAlbumClick: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(highlights, key = { it.albumHash }) { highlight ->
            Surface(
                modifier = Modifier
                    .size(width = 150.dp, height = 200.dp)
                    .clickable { onAlbumClick(highlight.albumHash) },
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(MaterialTheme.shapes.small),
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("${baseUrl}img/thumbnail/${highlight.image}")
                            .crossfade(true)
                            .build(),
                        placeholder = painterResource(id = UiComponent.drawable.audio_fallback),
                        contentDescription = highlight.title
                    )
                    Text(
                        text = highlight.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = highlight.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderRow(
    folders: List<Folder>,
    onFolderClick: (Folder) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(folders, key = { it.path }) { folder ->
            Surface(
                modifier = Modifier
                    .size(width = 160.dp, height = 120.dp)
                    .clickable { onFolderClick(folder) },
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = folder.name.ifBlank { "Unknown" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${folder.trackCount} tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistSpotlightRow(
    artists: List<ArtistHighlight>,
    onClick: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(artists, key = { it.artistHash }) { artist ->
            Surface(
                shape = MaterialTheme.shapes.small,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .clickable { onClick(artist.artistHash) }
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(id = UiComponent.drawable.ic_artist),
                        contentDescription = artist.name,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (actionLabel != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text = actionLabel)
            }
        }
    }
}

private data class AlbumHighlight(
    val albumHash: String,
    val title: String,
    val artist: String,
    val image: String
)

private data class ArtistHighlight(
    val artistHash: String,
    val name: String
)

private data class QuickAction(
    val iconRes: Int,
    val label: String,
    val onClick: () -> Unit
)

private fun playFromHome(
    track: Track,
    activeQueue: List<Track>,
    fallbackQueue: List<Track>,
    controller: MediaControllerViewModel
) {
    if (activeQueue.isNotEmpty()) {
        val existingIndex = activeQueue.indexOfFirst { it.trackHash == track.trackHash }
        if (existingIndex >= 0) {
            controller.onQueueEvent(QueueEvent.SeekToQueueItem(existingIndex))
            return
        }
    }

    if (fallbackQueue.isEmpty()) return

    val fallbackIndex = fallbackQueue.indexOfFirst { it.trackHash == track.trackHash }.let { index ->
        if (index >= 0) index else 0
    }

    controller.onQueueEvent(
        QueueEvent.RecreateQueue(
            source = QueueSource.UNKNOWN,
            queue = fallbackQueue,
            clickedTrackIndex = fallbackIndex
        )
    )
}

private fun resolveGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
}
