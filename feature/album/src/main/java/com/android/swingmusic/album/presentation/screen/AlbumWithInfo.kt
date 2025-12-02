package com.android.swingmusic.album.presentation.screen

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.swingmusic.album.presentation.event.AlbumWithInfoUiEvent
import com.android.swingmusic.album.presentation.screen.components.AlbumFooter
import com.android.swingmusic.album.presentation.screen.components.AlbumHeader
import com.android.swingmusic.album.presentation.screen.components.AlbumLoadingShimmer
import com.android.swingmusic.album.presentation.screen.components.AlbumTrackItem
import com.android.swingmusic.album.presentation.screen.components.DiscHeader
import com.android.swingmusic.album.presentation.viewmodel.AlbumWithInfoViewModel
import com.android.swingmusic.common.presentation.navigator.CommonNavigator
import com.android.swingmusic.core.data.util.Resource
import com.android.swingmusic.core.domain.model.AlbumInfo
import com.android.swingmusic.core.domain.model.BottomSheetItemModel
import com.android.swingmusic.core.domain.model.Track
import com.android.swingmusic.core.domain.util.BottomSheetAction
import com.android.swingmusic.core.domain.util.PlaybackState
import com.android.swingmusic.core.domain.util.QueueSource
import com.android.swingmusic.player.presentation.event.PlayerUiEvent
import com.android.swingmusic.player.presentation.event.QueueEvent
import com.android.swingmusic.player.presentation.viewmodel.MediaControllerViewModel
import com.android.swingmusic.uicomponent.R
import com.android.swingmusic.uicomponent.presentation.component.CustomTrackBottomSheet
import com.android.swingmusic.uicomponent.presentation.theme.SwingMusicTheme
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Destination<RootGraph>
@Composable
fun AlbumWithInfoScreen(
    albumWithInfoViewModel: AlbumWithInfoViewModel = hiltViewModel(),
    mediaControllerViewModel: MediaControllerViewModel,
    navigator: CommonNavigator,
    albumHash: String,
) {
    val albumWithInfoState by albumWithInfoViewModel.albumWithInfoState.collectAsState()
    val playerUiState by mediaControllerViewModel.playerUiState.collectAsState()
    val baseUrl by mediaControllerViewModel.baseUrl.collectAsState()

    var showOnRefreshIndicator by remember { mutableStateOf(false) }
    val refreshState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(albumWithInfoState.albumHash, albumWithInfoState.reloadRequired) {
        if (albumWithInfoState.reloadRequired) {
            albumWithInfoViewModel.onAlbumWithInfoUiEvent(
                AlbumWithInfoUiEvent.OnLoadAlbumWithInfo(albumHash)
            )
        }
    }

    SwingMusicTheme {
        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 100.dp)
                )
            }
        ) {
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                isRefreshing = showOnRefreshIndicator,
                state = refreshState,
                onRefresh = {
                    showOnRefreshIndicator = true
                    albumWithInfoViewModel.onAlbumWithInfoUiEvent(
                        AlbumWithInfoUiEvent.OnRefreshAlbumInfo
                    )
                },
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        modifier = Modifier
                            .padding(top = 48.dp)
                            .align(Alignment.TopCenter),
                        isRefreshing = showOnRefreshIndicator,
                        state = refreshState
                    )
                }
            ) {
                when (albumWithInfoState.infoResource) {
                    is Resource.Loading -> {
                        if (!showOnRefreshIndicator) {
                            AlbumLoadingShimmer()
                        }
                    }

                    is Resource.Error -> {
                        showOnRefreshIndicator = false
                        ErrorState(
                            message = albumWithInfoState.infoResource.message ?: "Failed to load",
                            onRetry = {
                                albumWithInfoViewModel.onAlbumWithInfoUiEvent(
                                    AlbumWithInfoUiEvent.OnRefreshAlbumInfo
                                )
                            }
                        )
                    }

                    is Resource.Success -> {
                        showOnRefreshIndicator = false
                        val data = albumWithInfoState.infoResource.data!!
                        val albumInfo = data.albumInfo ?: return@PullToRefreshBox
                        val copyright = data.copyright ?: ""

                        AlbumContent(
                            albumInfo = albumInfo,
                            copyright = copyright,
                            sortedTracks = albumWithInfoState.orderedTracks,
                            groupedTracks = data.groupedTracks,
                            currentTrack = playerUiState.nowPlayingTrack,
                            playbackState = playerUiState.playbackState,
                            baseUrl = baseUrl ?: "",
                            onClickBack = { navigator.navigateBack() },
                            onClickArtist = { navigator.gotoArtistInfo(it) },
                            onPlay = { queue ->
                                if (queue.isNotEmpty()) {
                                    mediaControllerViewModel.onQueueEvent(
                                        QueueEvent.RecreateQueue(
                                            source = QueueSource.ALBUM(albumHash, albumInfo.title),
                                            clickedTrackIndex = 0,
                                            queue = queue
                                        )
                                    )
                                }
                            },
                            onClickTrack = { index, queue ->
                                mediaControllerViewModel.onQueueEvent(
                                    QueueEvent.RecreateQueue(
                                        source = QueueSource.ALBUM(albumHash, albumInfo.title),
                                        clickedTrackIndex = index,
                                        queue = queue
                                    )
                                )
                            },
                            onToggleTrackFavorite = { trackHash, isFavorite ->
                                albumWithInfoViewModel.onAlbumWithInfoUiEvent(
                                    AlbumWithInfoUiEvent.OnToggleAlbumTrackFavorite(trackHash, isFavorite)
                                )
                            },
                            onSheetAction = { track, action ->
                                handleSheetAction(
                                    track = track,
                                    action = action,
                                    albumHash = albumHash,
                                    albumTitle = albumInfo.title,
                                    navigator = navigator,
                                    mediaControllerViewModel = mediaControllerViewModel,
                                    snackbarHostState = snackbarHostState,
                                    scope = scope
                                )
                            },
                            onGotoArtist = { navigator.gotoArtistInfo(it) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumContent(
    albumInfo: AlbumInfo,
    copyright: String,
    sortedTracks: List<Track>,
    groupedTracks: Map<Int, List<Track>>,
    currentTrack: Track?,
    playbackState: PlaybackState,
    baseUrl: String,
    onClickBack: () -> Unit,
    onClickArtist: (String) -> Unit,
    onPlay: (List<Track>) -> Unit,
    onClickTrack: (Int, List<Track>) -> Unit,
    onToggleTrackFavorite: (String, Boolean) -> Unit,
    onSheetAction: (Track, BottomSheetAction) -> Unit,
    onGotoArtist: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showTrackBottomSheet by remember { mutableStateOf(false) }
    var clickedTrack: Track? by remember { mutableStateOf(null) }

    // FAB visibility based on scroll direction
    var previousScrollOffset by remember { mutableIntStateOf(0) }
    var previousFirstVisibleItem by remember { mutableIntStateOf(0) }
    val isFabVisible by remember {
        derivedStateOf {
            val currentFirstVisibleItem = listState.firstVisibleItemIndex
            val currentScrollOffset = listState.firstVisibleItemScrollOffset

            val isScrollingUp = if (currentFirstVisibleItem != previousFirstVisibleItem) {
                currentFirstVisibleItem < previousFirstVisibleItem
            } else {
                currentScrollOffset < previousScrollOffset
            }

            previousFirstVisibleItem = currentFirstVisibleItem
            previousScrollOffset = currentScrollOffset

            // Show FAB when at top or scrolling up
            currentFirstVisibleItem == 0 || isScrollingUp
        }
    }

    // Update clicked track when tracks change
    LaunchedEffect(groupedTracks) {
        groupedTracks.forEach { (_, tracks) ->
            clickedTrack = tracks.find { it.trackHash == clickedTrack?.trackHash } ?: clickedTrack
        }
    }

    if (showTrackBottomSheet) {
        clickedTrack?.let { track ->
            CustomTrackBottomSheet(
                scope = scope,
                sheetState = sheetState,
                isFavorite = track.isFavorite,
                clickedTrack = track,
                baseUrl = baseUrl,
                bottomSheetItems = listOf(
                    BottomSheetItemModel(
                        label = "Go to Artist",
                        painterId = R.drawable.ic_artist,
                        track = track,
                        sheetAction = BottomSheetAction.OpenArtistsDialog(track.trackArtists)
                    ),
                    BottomSheetItemModel(
                        label = "Go to Folder",
                        painterId = R.drawable.folder_outlined_open,
                        track = track,
                        sheetAction = BottomSheetAction.GotoFolder(
                            name = track.folder.substringAfterLast('/'),
                            path = track.folder
                        )
                    ),
                    BottomSheetItemModel(
                        label = "Play Next",
                        painterId = R.drawable.play_next,
                        track = track,
                        sheetAction = BottomSheetAction.PlayNext
                    ),
                    BottomSheetItemModel(
                        label = "Add to Queue",
                        painterId = R.drawable.add_to_queue,
                        track = track,
                        sheetAction = BottomSheetAction.AddToQueue
                    )
                ),
                onHideBottomSheet = { showTrackBottomSheet = it },
                onClickSheetItem = { sheetTrack, action -> onSheetAction(sheetTrack, action) },
                onChooseArtist = { onGotoArtist(it) },
                onToggleTrackFavorite = onToggleTrackFavorite
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            state = listState
        ) {
            item {
                AlbumHeader(
                    albumInfo = albumInfo,
                    baseUrl = baseUrl,
                    onClickBack = onClickBack,
                    onClickArtist = onClickArtist
                )
            }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        groupedTracks.forEach { (discNumber, tracks) ->
            if (groupedTracks.size > 1 || discNumber > 1) {
                item { DiscHeader(discNumber) }
            }

            items(tracks, key = { it.filepath }) { track ->
                AlbumTrackItem(
                    track = track,
                    baseUrl = baseUrl,
                    isCurrentTrack = track.trackHash == currentTrack?.trackHash,
                    playbackState = playbackState,
                    onClickTrack = {
                        val index = sortedTracks.indexOf(track)
                        if (index != -1) onClickTrack(index, sortedTracks)
                    },
                    onClickMore = {
                        clickedTrack = it
                        showTrackBottomSheet = true
                    }
                )
            }
        }

            item {
                AlbumFooter(albumInfo = albumInfo, copyright = copyright)
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Play Button
        AnimatedVisibility(
            visible = isFabVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Surface(
                onClick = { onPlay(sortedTracks) },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.play_arrow_fill),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PLAY NOW",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message)
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Text(text = "RETRY")
        }
    }
}

private fun handleSheetAction(
    track: Track,
    action: BottomSheetAction,
    albumHash: String,
    albumTitle: String,
    navigator: CommonNavigator,
    mediaControllerViewModel: MediaControllerViewModel,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    when (action) {
        is BottomSheetAction.GotoFolder -> {
            navigator.gotoSourceFolder(action.name, action.path)
        }
        is BottomSheetAction.PlayNext -> {
            mediaControllerViewModel.onQueueEvent(
                QueueEvent.PlayNext(track, QueueSource.ALBUM(albumHash, albumTitle))
            )
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Track added to play next",
                    actionLabel = "View Queue",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    navigator.gotoQueueScreen()
                }
            }
        }
        is BottomSheetAction.AddToQueue -> {
            mediaControllerViewModel.onQueueEvent(
                QueueEvent.AddToQueue(track, QueueSource.ALBUM(albumHash, albumTitle))
            )
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Track added to queue",
                    actionLabel = "View Queue",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    navigator.gotoQueueScreen()
                }
            }
        }
        else -> {}
    }
}
