package com.android.swingmusic.home.presentation

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.swingmusic.common.presentation.component.TopSearchBar
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
import com.ramcosta.composedestinations.annotation.RootGraph
import java.util.Calendar

@Destination<RootGraph>
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

    val libraryActions = remember(navigator) {
        listOf(
            LibraryAction(UiComponent.drawable.folder_filled, "Folders") { navigator.gotoFolders() },
            LibraryAction(UiComponent.drawable.ic_album, "Albums") { navigator.gotoAlbumLibrary() },
            LibraryAction(UiComponent.drawable.ic_artist, "Artists") { navigator.gotoArtistLibrary() },
            LibraryAction(UiComponent.drawable.play_list, "Playlists") { navigator.gotoSearch() },
            LibraryAction(UiComponent.drawable.fav_filled, "Favorites") { navigator.gotoSearch() },
            LibraryAction(UiComponent.drawable.play_arrow_fill, "Fav. tracks") { navigator.gotoSearch() },
            LibraryAction(UiComponent.drawable.ic_artist, "Fav. artists") { navigator.gotoSearch() },
            LibraryAction(UiComponent.drawable.ic_album, "Fav. albums") { navigator.gotoSearch() }
        )
    }

    val homeStats = remember(
        playerUiState.queue,
        foldersState.foldersAndTracks.folders,
        continueListening,
        artistHighlights
    ) {
        listOfNotNull(
            playerUiState.queue.takeIf { it.isNotEmpty() }?.let { HomeStat("In queue", it.size.toString()) },
            foldersState.foldersAndTracks.folders.takeIf { it.isNotEmpty() }
                ?.let { HomeStat("Folders", it.size.toString()) },
            artistHighlights.takeIf { it.isNotEmpty() }?.let { HomeStat("Artists", artistHighlights.size.toString()) },
            continueListening.takeIf { it.isNotEmpty() }?.let { HomeStat("Listening", continueListening.size.toString()) }
        ).take(3)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TopSearchBar(
                    onSearchClick = navigator::gotoSearch,
                    onAvatarClick = navigator::gotoSearch
                )

                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = HomeAccentColor
                )
                Text(
                    text = currentTrackTitle?.let { "Listening to $it" } ?: "Dive back into your music",
                    style = MaterialTheme.typography.bodySmall,
                    color = HomeMutedColor
                )
            }
        }

        if (homeStats.isNotEmpty()) {
            item {
                HomeStatsRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    stats = homeStats
                )
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionHeader(title = "Browse Library")
                LibraryActionGrid(actions = libraryActions)
            }
        }

        if (continueListening.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeader(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        title = "Continue listening",
                        actionLabel = "Queue",
                        onActionClick = navigator::gotoQueueScreen
                    )
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
        }

        if (albumHighlights.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeader(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        title = "Popular albums",
                        actionLabel = "Browse",
                        onActionClick = navigator::gotoAlbumLibrary
                    )
                    AlbumHighlightsRow(
                        highlights = albumHighlights,
                        baseUrl = baseUrl.orEmpty(),
                        onAlbumClick = navigator::gotoAlbumWithInfo
                    )
                }
            }
        }

        if (folderShortcuts.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeader(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        title = "Jump back in",
                        actionLabel = "Folders",
                        onActionClick = navigator::gotoFolders
                    )
                    FolderRow(
                        folders = folderShortcuts,
                        onFolderClick = { folder ->
                            val folderName = folder.name.ifBlank { "Library" }
                            navigator.gotoSourceFolder(folderName, folder.path)
                        }
                    )
                }
            }
        }

        if (artistHighlights.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeader(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        title = "Trending artists"
                    )
                    ArtistSpotlightRow(
                        artists = artistHighlights,
                        onClick = navigator::gotoArtistInfo
                    )
                }
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
        color = HomeCardAltColor,
        tonalElevation = 0.dp
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
                tint = HomeMutedColor
            )
            Text(
                text = "Search anything",
                style = MaterialTheme.typography.bodyMedium,
                color = HomeMutedColor
            )
        }
    }
}

@Composable
private fun HomeStatsRow(
    modifier: Modifier = Modifier,
    stats: List<HomeStat>
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val primary = stats.firstOrNull()
        val paddedStats = buildList<HomeStat?> {
            addAll(stats)
            repeat((3 - stats.size).coerceAtLeast(0)) { add(null) }
        }.take(3)

        paddedStats.forEach { stat ->
            if (stat != null) {
                HomeStatCard(
                    modifier = Modifier.weight(1f),
                    stat = stat,
                    highlight = primary != null && stat == primary
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HomeStatCard(
    modifier: Modifier = Modifier,
    stat: HomeStat,
    highlight: Boolean
) {
    Surface(
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (highlight) HomeCardAltColor else HomeCardColor,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stat.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 0.8.sp,
                color = HomeMutedColor
            )
            Text(
                text = stat.value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = HomeAccentColor
            )
        }
    }
}

@Composable
private fun LibraryActionGrid(actions: List<LibraryAction>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { action ->
                    LibraryActionCard(
                        modifier = Modifier.weight(1f),
                        action = action
                    )
                }

                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LibraryActionCard(
    modifier: Modifier = Modifier,
    action: LibraryAction
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clickable(onClick = action.onClick),
        shape = RoundedCornerShape(12.dp),
        color = HomeCardColor,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp),
                color = HomeCardAltColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(id = action.iconRes),
                        contentDescription = action.label,
                        tint = HomeAccentColor
                    )
                }
            }

            Text(
                text = action.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = HomeAccentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(tracks, key = { it.trackHash }) { track ->
            val isActive = track.trackHash == nowPlayingHash
            val cardShape = RoundedCornerShape(6.dp)
            
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(cardShape)
                    .background(HomeCardColor)
                    .clickable { onPlayTrack(track) }
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
                        .padding(horizontal = 8.dp, vertical = 8.dp)
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

                if (isActive) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.White.copy(alpha = 0.08f))
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(highlights, key = { it.albumHash }) { highlight ->
            val cardShape = RoundedCornerShape(6.dp)
            
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(cardShape)
                    .background(HomeCardColor)
                    .clickable { onAlbumClick(highlight.albumHash) }
            ) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("${baseUrl}img/thumbnail/${highlight.image}")
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(id = UiComponent.drawable.audio_fallback),
                    contentDescription = highlight.title,
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
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = highlight.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = highlight.artist,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(folders, key = { it.path }) { folder ->
            Surface(
                modifier = Modifier
                    .size(width = 130.dp, height = 80.dp)
                    .clickable { onFolderClick(folder) },
                shape = RoundedCornerShape(10.dp),
                color = HomeCardColor,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = folder.name.ifBlank { "Unknown" },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = HomeAccentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${folder.trackCount} tracks",
                        style = MaterialTheme.typography.labelSmall,
                        color = HomeMutedColor
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(artists, key = { it.artistHash }) { artist ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = HomeCardAltColor,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .clickable { onClick(artist.artistHash) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(id = UiComponent.drawable.ic_artist),
                        contentDescription = artist.name,
                        tint = HomeAccentColor
                    )
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = HomeAccentColor,
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
    modifier: Modifier = Modifier,
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = HomeAccentColor
        )

        if (actionLabel != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = HomeAccentColor
                )
            ) {
                Text(
                    text = actionLabel.uppercase(),
                    color = HomeMutedColor,
                    letterSpacing = 0.8.sp,
                    style = MaterialTheme.typography.labelSmall
                )
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

private data class LibraryAction(
    val iconRes: Int,
    val label: String,
    val onClick: () -> Unit
)

private data class HomeStat(
    val label: String,
    val value: String
)

private val HomeCardColor = Color(0xFF151515)
private val HomeCardAltColor = Color(0xFF1F1F1F)
private val HomeAccentColor = Color(0xFFF5F5F5)
private val HomeMutedColor = Color(0xFF9C9C9C)

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
