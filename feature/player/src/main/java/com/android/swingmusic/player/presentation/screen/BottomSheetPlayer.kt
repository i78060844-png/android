package com.android.swingmusic.player.presentation.screen

import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
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
import com.android.swingmusic.uicomponent.presentation.component.slider.WaveAnimationSpecs
import com.android.swingmusic.uicomponent.presentation.component.slider.WaveDirection
import com.android.swingmusic.uicomponent.presentation.component.slider.WavySlider
import com.android.swingmusic.uicomponent.presentation.util.BlurTransformation
import com.android.swingmusic.uicomponent.presentation.util.formatDuration
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    mediaControllerViewModel: MediaControllerViewModel,
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
    
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val peekHeight = 64.dp + navigationBarHeight

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
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContent = {
            val isExpanded = sheetState.currentValue == SheetValue.Expanded

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
            } else {
                CollapsedPlayerContent(
                    track = track,
                    playbackState = playerUiState.playbackState,
                    isBuffering = playerUiState.isBuffering,
                    seekPosition = playerUiState.seekPosition,
                    baseUrl = baseUrl ?: "",
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
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
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
            .navigationBarsPadding()
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
            .background(Color(0xFF181818)) // Spotify dark card color
    ) {
        // Progress bar at top - Spotify style
        androidx.compose.material3.LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            progress = { seekPosition },
            gapSize = 0.dp,
            drawStopIndicator = {},
            strokeCap = StrokeCap.Square,
            color = Color(0xFF1DB954), // Spotify green
            trackColor = Color(0xFF404040)
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

            // Controls - Spotify style: just favorite and play button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Play/Pause button - Spotify uses a white filled circle
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
                        Icon(
                            painter = painterResource(
                                id = if (playbackState == PlaybackState.PLAYING)
                                    R.drawable.pause_icon else R.drawable.play_arrow
                            ),
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
    val fileType by remember {
        derivedStateOf {
            track.filepath.substringAfterLast(".").uppercase(Locale.ROOT)
        }
    }

    val inverseOnSurface = MaterialTheme.colorScheme.inverseOnSurface
    val onSurface = MaterialTheme.colorScheme.onSurface
    // AMOLED Spotify-style file type badge colors
    val fileTypeBadgeColor = when (track.bitrate) {
        in 321..1023 -> Color(0xFF0D2629)   // Dark teal for high quality
        in 1024..Int.MAX_VALUE -> Color(0XFF2D2818)  // Dark gold for lossless
        else -> inverseOnSurface
    }
    val fileTypeTextColor = when (track.bitrate) {
        in 321..1023 -> Color(0XFF00D4FF)   // Cyan accent for high quality
        in 1024..Int.MAX_VALUE -> Color(0XFFFFD700)  // Gold accent for lossless
        else -> onSurface
    }

    val animateWave = playbackState == PlaybackState.PLAYING && isBuffering.not()
    val repeatModeIcon = when (repeatMode) {
        RepeatMode.REPEAT_ONE -> R.drawable.repeat_one
        else -> R.drawable.repeat_all
    }
    val playbackStateIcon = when (playbackState) {
        PlaybackState.PLAYING -> R.drawable.pause_icon
        PlaybackState.PAUSED -> R.drawable.play_arrow
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            model = ImageRequest.Builder(LocalContext.current)
                .data("${baseUrl}img/thumbnail/${track.image}")
                .crossfade(true)
                .transformations(
                    listOf(
                        BlurTransformation(
                            scale = 0.25f,
                            radius = 25
                        )
                    )
                )
                .build(),
            contentDescription = "Track Image",
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = .75F),
                            MaterialTheme.colorScheme.surface.copy(alpha = 1F),
                            MaterialTheme.colorScheme.surface.copy(alpha = 1F)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
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
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HorizontalPager(
                    modifier = Modifier.fillMaxWidth(),
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

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AsyncImage(
                            modifier = Modifier
                                .size(320.dp)
                                .clip(RoundedCornerShape(7))
                                .graphicsLayer {
                                    val scale = lerp(1f, 1.25f, pageOffset)
                                    scaleX = scale
                                    scaleY = scale
                                    clip = true
                                    shape = RoundedCornerShape(7)
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

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.fillMaxWidth(.78F)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(modifier = Modifier.fillMaxWidth()) {
                            track.trackArtists.forEachIndexed { index, trackArtist ->
                                item {
                                    Text(
                                        modifier = Modifier
                                            .clickable(
                                                onClick = { onClickArtist(trackArtist.artistHash) },
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() }
                                            ),
                                        text = trackArtist.name,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .84F),
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (index != track.trackArtists.lastIndex) {
                                        Text(
                                            text = ", ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .84F),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    IconButton(
                        modifier = Modifier.clip(CircleShape),
                        onClick = { onToggleFavorite(track.isFavorite, track.trackHash) }
                    ) {
                        val icon = if (track.isFavorite) R.drawable.fav_filled else R.drawable.fav_not_filled
                        Icon(
                            painter = painterResource(id = icon),
                            contentDescription = "Favorite"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    WavySlider(
                        modifier = Modifier.height(12.dp),
                        value = seekPosition,
                        onValueChange = { value -> onSeekPlayBack(value) },
                        waveLength = 32.dp,
                        waveHeight = 8.dp,
                        waveVelocity = (if (animateWave) 16.dp else 0.dp) to WaveDirection.HEAD,
                        waveThickness = 4.dp,
                        trackThickness = 4.dp,
                        incremental = false,
                        animationSpecs = WaveAnimationSpecs(
                            waveHeightAnimationSpec = tween(
                                durationMillis = 300,
                                easing = FastOutSlowInEasing
                            ),
                            waveVelocityAnimationSpec = tween(
                                durationMillis = 2000,
                                easing = LinearOutSlowInEasing
                            ),
                            waveStartSpreadAnimationSpec = tween(
                                durationMillis = 0,
                                easing = EaseOutQuad
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = playbackDuration,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .84F)
                        )
                        Text(
                            text = if (playbackState == PlaybackState.ERROR)
                                track.duration.formatDuration() else trackDuration,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .84F)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Prev button - Spotify style (no background)
                    IconButton(
                        modifier = Modifier.size(48.dp),
                        onClick = onClickPrev
                    ) {
                        Icon(
                            modifier = Modifier.size(32.dp),
                            painter = painterResource(id = R.drawable.prev),
                            contentDescription = "Prev",
                            tint = Color.White
                        )
                    }

                    // Play/Pause button - Spotify green circle
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1DB954)) // Spotify green
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
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
                                modifier = Modifier.size(32.dp),
                                strokeCap = StrokeCap.Round,
                                strokeWidth = 2.dp,
                                color = Color.Black
                            )
                        } else if (playbackState == PlaybackState.ERROR) {
                            Icon(
                                modifier = Modifier.size(32.dp),
                                painter = painterResource(id = R.drawable.error),
                                tint = Color.Black,
                                contentDescription = "Error state"
                            )
                        } else {
                            Icon(
                                modifier = Modifier.size(32.dp),
                                tint = Color.Black,
                                painter = painterResource(id = playbackStateIcon),
                                contentDescription = "Play/Pause"
                            )
                        }
                    }

                    // Next button - Spotify style (no background)
                    IconButton(
                        modifier = Modifier.size(48.dp),
                        onClick = onClickNext
                    ) {
                        Icon(
                            modifier = Modifier.size(32.dp),
                            painter = painterResource(id = R.drawable.next),
                            tint = Color.White,
                            contentDescription = "Next"
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24))
                    .background(fileTypeTextColor.copy(alpha = .075F))
                    .wrapContentSize()
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = fileType,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = fileTypeTextColor
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = fileTypeTextColor
                    )
                    Text(
                        text = "${track.bitrate} Kbps",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = fileTypeTextColor
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF181818)) // Spotify dark card
                    .navigationBarsPadding()
                    .padding(vertical = 12.dp, horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onToggleRepeatMode) {
                    Icon(
                        painter = painterResource(id = repeatModeIcon),
                        tint = if (repeatMode == RepeatMode.REPEAT_OFF)
                            Color(0xFF6A6A6A)  // Subdued
                        else Color(0xFF1DB954), // Spotify green when active
                        contentDescription = "Repeat"
                    )
                }

                IconButton(onClick = onClickQueueIcon) {
                    Icon(
                        painter = painterResource(id = R.drawable.play_list),
                        tint = Color.White,
                        contentDescription = "Queue"
                    )
                }

                IconButton(onClick = onToggleShuffleMode) {
                    Icon(
                        painter = painterResource(id = R.drawable.shuffle),
                        tint = if (shuffleMode == ShuffleMode.SHUFFLE_OFF)
                            Color(0xFF6A6A6A)  // Subdued
                        else Color(0xFF1DB954), // Spotify green when active
                        contentDescription = "Shuffle"
                    )
                }
            }
        }
    }
}
