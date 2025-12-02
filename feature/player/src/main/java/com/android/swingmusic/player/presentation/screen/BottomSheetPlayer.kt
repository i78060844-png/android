package com.android.swingmusic.player.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.swingmusic.core.domain.model.Track
import com.android.swingmusic.core.domain.util.PlaybackState
import com.android.swingmusic.core.domain.util.RepeatMode
import com.android.swingmusic.core.domain.util.ShuffleMode
import com.android.swingmusic.player.presentation.event.PlayerUiEvent
import com.android.swingmusic.player.presentation.event.QueueEvent
import com.android.swingmusic.player.presentation.util.calculateCurrentOffsetForPage
import com.android.swingmusic.player.presentation.viewmodel.MediaControllerViewModel
import com.android.swingmusic.uicomponent.R
import com.android.swingmusic.uicomponent.presentation.util.BlurTransformation
import com.android.swingmusic.uicomponent.presentation.util.formatDuration
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    mediaControllerViewModel: MediaControllerViewModel,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onClickArtist: (artistHash: String) -> Unit,
    onClickQueueIcon: () -> Unit,
    content: @Composable () -> Unit
) {
    val playerUiState by mediaControllerViewModel.playerUiState.collectAsState()
    val baseUrl by mediaControllerViewModel.baseUrl.collectAsState()
    val scope = rememberCoroutineScope()

    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    
    // Mini player height + bottom padding (for navigation bar)
    val peekHeight = 64.dp + bottomPadding

    val track = playerUiState.nowPlayingTrack

    if (track == null) {
        content()
        return
    }

    BottomSheetScaffold(
        modifier = Modifier.imePadding(),
        scaffoldState = scaffoldState,
        sheetPeekHeight = peekHeight,
        sheetDragHandle = null,
        sheetShape = RoundedCornerShape(0.dp),
        sheetContainerColor = Color.Black,
        sheetContent = {
            // Use target state for smoother transitions
            val targetExpanded = sheetState.targetValue == SheetValue.Expanded
            val currentExpanded = sheetState.currentValue == SheetValue.Expanded
            val isExpanded = targetExpanded || currentExpanded
            
            Box(modifier = Modifier.fillMaxSize()) {
                // Show expanded content when expanding or expanded
                if (isExpanded) {
                    ExpandedPlayerContent(
                    track = track,
                    playingTrackIndex = playerUiState.playingTrackIndex,
                    queue = playerUiState.queue,
                    seekPosition = playerUiState.seekPosition,
                    playbackDuration = playerUiState.playbackDuration,
                    trackDuration = playerUiState.trackDuration,
                    playbackState = playerUiState.playbackState,
                    isBuffering = playerUiState.isBuffering,
                    repeatMode = playerUiState.repeatMode,
                    shuffleMode = playerUiState.shuffleMode,
                    baseUrl = baseUrl ?: "",
                    onPageSelect = { page ->
                        mediaControllerViewModel.onQueueEvent(QueueEvent.SeekToQueueItem(page))
                    },
                    onClickArtist = onClickArtist,
                    onToggleRepeatMode = {
                        mediaControllerViewModel.onPlayerUiEvent(PlayerUiEvent.OnToggleRepeatMode)
                    },
                    onClickPrev = {
                        mediaControllerViewModel.onPlayerUiEvent(PlayerUiEvent.OnPrev)
                    },
                    onTogglePlayerState = {
                        mediaControllerViewModel.onPlayerUiEvent(PlayerUiEvent.OnTogglePlayerState)
                    },
                    onResumePlayBackFromError = {
                        mediaControllerViewModel.onPlayerUiEvent(PlayerUiEvent.OnResumePlaybackFromError)
                    },
                    onClickNext = {
                        mediaControllerViewModel.onPlayerUiEvent(PlayerUiEvent.OnNext)
                    },
                    onToggleShuffleMode = {
                        mediaControllerViewModel.onPlayerUiEvent(
                            PlayerUiEvent.OnToggleShuffleMode(toggleShuffle = true)
                        )
                    },
                    onSeekPlayBack = {
                        mediaControllerViewModel.onPlayerUiEvent(PlayerUiEvent.OnSeekPlayBack(it))
                    },
                    onToggleFavorite = { isFavorite, trackHash ->
                        mediaControllerViewModel.onPlayerUiEvent(
                            PlayerUiEvent.OnToggleFavorite(isFavorite, trackHash)
                        )
                    },
                    onClickQueueIcon = onClickQueueIcon,
                    onCollapse = {
                        scope.launch { sheetState.partialExpand() }
                    }
                )
                }
                
                // Show collapsed content when not expanded
                if (!isExpanded) {
                    CollapsedPlayerContent(
                    track = track,
                    playbackState = playerUiState.playbackState,
                    isBuffering = playerUiState.isBuffering,
                    seekPosition = playerUiState.seekPosition,
                    baseUrl = baseUrl ?: "",
                    bottomPadding = bottomPadding,
                    onTogglePlaybackState = {
                        mediaControllerViewModel.onPlayerUiEvent(PlayerUiEvent.OnTogglePlayerState)
                    },
                    onResumePlayBackFromError = {
                        mediaControllerViewModel.onPlayerUiEvent(PlayerUiEvent.OnResumePlaybackFromError)
                    },
                    onSwipeNext = {
                        mediaControllerViewModel.onPlayerUiEvent(PlayerUiEvent.OnNext)
                    },
                    onSwipePrev = {
                        mediaControllerViewModel.onPlayerUiEvent(PlayerUiEvent.OnPrev)
                    },
                    onExpand = {
                        scope.launch { sheetState.expand() }
                    }
                )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding())
                .fillMaxSize()
        ) {
            content()
        }
    }
}

@Composable
private fun CollapsedPlayerContent(
    track: Track,
    playbackState: PlaybackState,
    isBuffering: Boolean,
    seekPosition: Float,
    baseUrl: String,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onTogglePlaybackState: () -> Unit,
    onResumePlayBackFromError: () -> Unit,
    onSwipeNext: () -> Unit,
    onSwipePrev: () -> Unit,
    onExpand: () -> Unit
) {
    var dragOffset by remember { mutableStateOf(0f) }

    // Spotify-style mini player
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = bottomPadding)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset < -50) {
                            onExpand()
                        }
                        dragOffset = 0f
                    }
                ) { _, dragAmount ->
                    dragOffset += dragAmount
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onExpand() }
            .background(Color.Black)
    ) {
        // Progress bar at top
        androidx.compose.material3.LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            progress = { seekPosition },
            gapSize = 0.dp,
            drawStopIndicator = {},
            strokeCap = StrokeCap.Square,
            color = Color.White,
            trackColor = Color.Transparent
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art - Spotify uses smaller rounded corners
                AsyncImage(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("${baseUrl}img/thumbnail/small/${track.image}")
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(R.drawable.audio_fallback),
                    fallback = painterResource(R.drawable.audio_fallback),
                    error = painterResource(R.drawable.audio_fallback),
                    contentDescription = "Track Image",
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.trackArtists.joinToString(", ") { it.name },
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFFB3B3B3) // Spotify subdued text
                    )
                }
            }

            // Controls - Play/Pause button with animation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        if (playbackState == PlaybackState.ERROR) {
                            onResumePlayBackFromError()
                        } else {
                            onTogglePlaybackState()
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            strokeCap = StrokeCap.Round,
                            color = Color.White
                        )
                    } else {
                        AnimatedContent(
                            targetState = playbackState,
                            transitionSpec = {
                                (scaleIn(
                                    initialScale = 0.8f,
                                    animationSpec = tween(150)
                                ) + fadeIn(animationSpec = tween(150)))
                                    .togetherWith(
                                        scaleOut(
                                            targetScale = 0.8f,
                                            animationSpec = tween(150)
                                        ) + fadeOut(animationSpec = tween(150))
                                    )
                            },
                            label = "MiniPlayPauseAnimation"
                        ) { state ->
                            val icon = when (state) {
                                PlaybackState.PLAYING -> R.drawable.ic_pause_rounded
                                PlaybackState.PAUSED -> R.drawable.ic_play_rounded
                                PlaybackState.ERROR -> R.drawable.error
                            }
                            Icon(
                                painter = painterResource(id = icon),
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(28.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedPlayerContent(
    track: Track,
    playingTrackIndex: Int,
    queue: List<Track>,
    seekPosition: Float,
    playbackDuration: String,
    trackDuration: String,
    playbackState: PlaybackState,
    isBuffering: Boolean,
    repeatMode: RepeatMode,
    shuffleMode: ShuffleMode,
    baseUrl: String,
    onPageSelect: (page: Int) -> Unit,
    onClickArtist: (artistHash: String) -> Unit,
    onToggleRepeatMode: () -> Unit,
    onClickPrev: () -> Unit,
    onTogglePlayerState: () -> Unit,
    onResumePlayBackFromError: () -> Unit,
    onClickNext: () -> Unit,
    onToggleShuffleMode: () -> Unit,
    onSeekPlayBack: (Float) -> Unit,
    onToggleFavorite: (Boolean, String) -> Unit,
    onClickQueueIcon: () -> Unit,
    onCollapse: () -> Unit
) {
    val repeatModeIcon = when (repeatMode) {
        RepeatMode.REPEAT_ONE -> R.drawable.ic_repeat_one_rounded
        else -> R.drawable.ic_repeat_rounded
    }
    val playbackStateIcon = when (playbackState) {
        PlaybackState.PLAYING -> R.drawable.ic_pause_rounded
        PlaybackState.PAUSED -> R.drawable.ic_play_rounded
        PlaybackState.ERROR -> R.drawable.error
    }

    val pagerState = rememberPagerState(
        initialPage = playingTrackIndex,
        pageCount = { if (queue.isEmpty()) 1 else queue.size }
    )

    var isInitialComposition by remember { mutableStateOf(true) }

    LaunchedEffect(
        key1 = playingTrackIndex,
        key2 = pagerState
    ) {
        if (playingTrackIndex in queue.indices) {
            if (playingTrackIndex != pagerState.currentPage) {
                pagerState.animateScrollToPage(playingTrackIndex)
            }
        }

        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (isInitialComposition) {
                isInitialComposition = false
            } else {
                if (playingTrackIndex != page) {
                    onPageSelect(page)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .padding(top = 12.dp, bottom = 16.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 20) {
                            onCollapse()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(Color(0xFF5C5C5C))
            )
        }

        // Album art with pager
        HorizontalPager(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            state = pagerState,
            beyondViewportPageCount = 2,
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            val imageData = if (page == playingTrackIndex) {
                "${baseUrl}img/thumbnail/${queue.getOrNull(playingTrackIndex)?.image ?: track.image}"
            } else {
                "${baseUrl}img/thumbnail/${queue.getOrNull(page)?.image ?: track.image}"
            }
            val pageOffset = pagerState.calculateCurrentOffsetForPage(page)

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .graphicsLayer {
                            val scale = lerp(1f, 0.85f, pageOffset)
                            scaleX = scale
                            scaleY = scale
                        },
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageData)
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(R.drawable.audio_fallback),
                    fallback = painterResource(R.drawable.audio_fallback),
                    error = painterResource(R.drawable.audio_fallback),
                    contentDescription = "Track Image",
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Track title and artist
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.trackArtists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFFAAAAAA),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        track.trackArtists.firstOrNull()?.let { onClickArtist(it.artistHash) }
                    }
                )
            }
            
            IconButton(
                onClick = { onToggleFavorite(track.isFavorite, track.trackHash) }
            ) {
                Icon(
                    painter = painterResource(
                        id = if (track.isFavorite) R.drawable.fav_filled else R.drawable.fav_not_filled
                    ),
                    contentDescription = "Favorite",
                    tint = if (track.isFavorite) Color(0xFFFF2D55) else Color(0xFFAAAAAA)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress bar
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                value = seekPosition,
                onValueChange = { value -> onSeekPlayBack(value) },
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0xFF5C5C5C)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = playbackDuration,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E8E)
                )
                Text(
                    text = if (playbackState == PlaybackState.ERROR)
                        track.duration.formatDuration() else "-$trackDuration",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E8E)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Previous
            IconButton(
                modifier = Modifier.size(64.dp),
                onClick = onClickPrev
            ) {
                Icon(
                    modifier = Modifier.size(44.dp),
                    painter = painterResource(id = R.drawable.ic_skip_previous_rounded),
                    contentDescription = "Previous",
                    tint = Color.White
                )
            }

            // Play/Pause with animation
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (playbackState != PlaybackState.ERROR) {
                                onTogglePlayerState()
                            } else {
                                onResumePlayBackFromError()
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(44.dp),
                        strokeCap = StrokeCap.Round,
                        strokeWidth = 3.dp,
                        color = Color.White
                    )
                } else {
                    AnimatedContent(
                        targetState = playbackState,
                        transitionSpec = {
                            (scaleIn(
                                initialScale = 0.8f,
                                animationSpec = tween(150)
                            ) + fadeIn(animationSpec = tween(150)))
                                .togetherWith(
                                    scaleOut(
                                        targetScale = 0.8f,
                                        animationSpec = tween(150)
                                    ) + fadeOut(animationSpec = tween(150))
                                )
                        },
                        label = "PlayPauseAnimation"
                    ) { state ->
                        val icon = when (state) {
                            PlaybackState.PLAYING -> R.drawable.ic_pause_rounded
                            PlaybackState.PAUSED -> R.drawable.ic_play_rounded
                            PlaybackState.ERROR -> R.drawable.error
                        }
                        Icon(
                            modifier = Modifier.size(72.dp),
                            painter = painterResource(id = icon),
                            contentDescription = "Play/Pause",
                            tint = Color.White
                        )
                    }
                }
            }

            // Next
            IconButton(
                modifier = Modifier.size(64.dp),
                onClick = onClickNext
            ) {
                Icon(
                    modifier = Modifier.size(44.dp),
                    painter = painterResource(id = R.drawable.ic_skip_next_rounded),
                    contentDescription = "Next",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Bottom controls row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onToggleRepeatMode) {
                Icon(
                    painter = painterResource(id = repeatModeIcon),
                    contentDescription = "Repeat",
                    tint = if (repeatMode == RepeatMode.REPEAT_OFF)
                        Color(0xFF8E8E8E) else Color(0xFFFF2D55)
                )
            }

            IconButton(onClick = onToggleShuffleMode) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_shuffle_rounded),
                    contentDescription = "Shuffle",
                    tint = if (shuffleMode == ShuffleMode.SHUFFLE_OFF)
                        Color(0xFF8E8E8E) else Color(0xFFFF2D55)
                )
            }

            IconButton(onClick = onClickQueueIcon) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_queue_music_rounded),
                    contentDescription = "Queue",
                    tint = Color(0xFF8E8E8E)
                )
            }
        }
    }
}
