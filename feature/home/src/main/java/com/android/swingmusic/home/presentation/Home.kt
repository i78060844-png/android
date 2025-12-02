package com.android.swingmusic.home.presentation

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.swingmusic.common.presentation.navigator.CommonNavigator
import com.android.swingmusic.core.domain.model.Album
import com.android.swingmusic.core.domain.model.Artist
import com.android.swingmusic.home.presentation.viewmodel.HomeViewModel
import com.android.swingmusic.player.presentation.event.QueueEvent
import com.android.swingmusic.player.presentation.viewmodel.MediaControllerViewModel
import com.android.swingmusic.uicomponent.R
import com.android.swingmusic.uicomponent.presentation.theme.SwingMusicTheme
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Destination<RootGraph>(start = true)
@Composable
fun Home(
    commonNavigator: CommonNavigator,
    mediaControllerViewModel: MediaControllerViewModel,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val baseUrl = homeViewModel.baseUrl.value ?: ""
    val homeUiState by homeViewModel.uiState.collectAsState()
    val playerUiState by mediaControllerViewModel.playerUiState.collectAsState()

    SwingMusicTheme(navBarColor = MaterialTheme.colorScheme.surface) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when {
                homeUiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                homeUiState.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = homeUiState.error ?: "Error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Header that scrolls with content
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Home",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { commonNavigator.gotoSearch() }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_search),
                                    contentDescription = "Search"
                                )
                            }
                        }

                        // Browse Library
                        BrowseLibrarySection(
                            totalAlbums = homeUiState.totalAlbums,
                            totalArtists = homeUiState.totalArtists,
                            onAlbumsClick = { commonNavigator.gotoAlbumLibrary() },
                            onArtistsClick = { commonNavigator.gotoArtistLibrary() },
                            onFoldersClick = { commonNavigator.gotoFolders() }
                        )

                        // Continue Listening
                        if (playerUiState.queue.isNotEmpty()) {
                            CompactTracksSection(
                                title = "Continue Listening",
                                subtitle = "Pick up where you left off",
                                tracks = playerUiState.queue.take(10),
                                baseUrl = baseUrl,
                                onTrackClick = { track ->
                                    val index = playerUiState.queue.indexOfFirst {
                                        it.trackHash == track.trackHash
                                    }
                                    if (index != -1) {
                                        mediaControllerViewModel.onQueueEvent(
                                            QueueEvent.SeekToQueueItem(index)
                                        )
                                    }
                                }
                            )
                        }

                        // Albums
                        if (homeUiState.popularAlbums.isNotEmpty()) {
                            CompactAlbumsSection(
                                title = "Recently Added",
                                subtitle = "Fresh additions to your library",
                                albums = homeUiState.popularAlbums,
                                baseUrl = baseUrl,
                                onAlbumClick = { commonNavigator.gotoAlbumWithInfo(it.albumHash) },
                                onSeeAllClick = { commonNavigator.gotoAlbumLibrary() }
                            )
                        }

                        // Artists
                        if (homeUiState.popularArtists.isNotEmpty()) {
                            CompactArtistsSection(
                                title = "Artists",
                                subtitle = "Explore your favorite artists",
                                artists = homeUiState.popularArtists,
                                baseUrl = baseUrl,
                                onArtistClick = { commonNavigator.gotoArtistInfo(it.artistHash) },
                                onSeeAllClick = { commonNavigator.gotoArtistLibrary() }
                            )
                        }

                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BrowseLibrarySection(
    totalAlbums: Int,
    totalArtists: Int,
    onAlbumsClick: () -> Unit,
    onArtistsClick: () -> Unit,
    onFoldersClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Browse Library",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BrowseCard(
                title = "Albums",
                subtitle = if (totalAlbums > 0) "$totalAlbums albums" else null,
                icon = R.drawable.ic_album_rounded,
                color = Color(0xFF6366F1),
                modifier = Modifier.weight(1f),
                onClick = onAlbumsClick
            )
            BrowseCard(
                title = "Artists",
                subtitle = if (totalArtists > 0) "$totalArtists artists" else null,
                icon = R.drawable.ic_artist_rounded,
                color = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f),
                onClick = onArtistsClick
            )
            BrowseCard(
                title = "Folders",
                subtitle = null,
                icon = R.drawable.folder_filled,
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f),
                onClick = onFoldersClick
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun BrowseCard(
    title: String,
    subtitle: String?,
    icon: Int,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CompactTracksSection(
    title: String,
    subtitle: String,
    tracks: List<com.android.swingmusic.core.domain.model.Track>,
    baseUrl: String,
    onTrackClick: (com.android.swingmusic.core.domain.model.Track) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tracks, key = { it.trackHash }) { track ->
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .clickable { onTrackClick(track) }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("$baseUrl/img/thumbnail/${track.image}")
                            .crossfade(true)
                            .build(),
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                    Text(
                        text = track.trackArtists.joinToString { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun CompactAlbumsSection(
    title: String,
    subtitle: String,
    albums: List<Album>,
    baseUrl: String,
    onAlbumClick: (Album) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            Text(
                text = "See all",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onSeeAllClick() }
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(albums, key = { it.albumHash }) { album ->
                Column(
                    modifier = Modifier
                        .width(120.dp)
                        .clickable { onAlbumClick(album) }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("$baseUrl/img/thumbnail/${album.image}")
                            .crossfade(true)
                            .build(),
                        contentDescription = album.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                    Text(
                        text = album.albumArtists.joinToString { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun CompactArtistsSection(
    title: String,
    subtitle: String,
    artists: List<Artist>,
    baseUrl: String,
    onArtistClick: (Artist) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            Text(
                text = "See all",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onSeeAllClick() }
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(artists, key = { it.artistHash }) { artist ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(80.dp)
                        .clickable { onArtistClick(artist) }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("$baseUrl/img/artist/small/${artist.image}")
                            .crossfade(true)
                            .build(),
                        contentDescription = artist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
