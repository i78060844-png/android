package com.android.swingmusic.album.presentation.screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.swingmusic.core.domain.model.AlbumInfo
import com.android.swingmusic.uicomponent.R
import com.android.swingmusic.uicomponent.presentation.util.BlurTransformation
import com.android.swingmusic.uicomponent.presentation.util.formatDate

@Composable
fun AlbumHeader(
    albumInfo: AlbumInfo,
    baseUrl: String,
    onClickBack: () -> Unit,
    onClickArtist: (artistHash: String) -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val versionContainerColor = if (isDarkTheme) Color(0x261DB954) else Color(0x3D744F00)
    val versionTextColor = if (isDarkTheme) Color(0xFF1DB954) else Color(0xFF744E00)
    val interaction = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Blurred background
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            model = ImageRequest.Builder(LocalContext.current)
                .data("${baseUrl}img/thumbnail/${albumInfo.image}")
                .crossfade(true)
                .transformations(listOf(BlurTransformation(scale = 0.25f, radius = 25)))
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = .2f),
                            MaterialTheme.colorScheme.surface.copy(alpha = .5f),
                            MaterialTheme.colorScheme.surface.copy(alpha = .8f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    modifier = Modifier.clip(CircleShape),
                    onClick = onClickBack
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }

            // Album art
            AsyncImage(
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shadow(elevation = 8.dp)
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .1f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                model = ImageRequest.Builder(LocalContext.current)
                    .data("${baseUrl}img/thumbnail/${albumInfo.image}")
                    .crossfade(true)
                    .build(),
                placeholder = painterResource(R.drawable.audio_fallback),
                fallback = painterResource(R.drawable.audio_fallback),
                error = painterResource(R.drawable.audio_fallback),
                contentDescription = albumInfo.title,
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = albumInfo.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Type and Artists
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item {
                    Text(
                        text = albumInfo.type.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f)
                    )
                    Dot()
                }

                itemsIndexed(albumInfo.albumArtists) { index, artist ->
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = interaction,
                            indication = null
                        ) { onClickArtist(artist.artistHash) },
                        text = artist.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f)
                    )
                    if (index != albumInfo.albumArtists.lastIndex) {
                        Text(text = ", ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Year and versions
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item {
                    Text(
                        text = albumInfo.date.formatDate("yyyy"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                items(albumInfo.versions) { version ->
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(versionContainerColor)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = version.uppercase(),
                            maxLines = 1,
                            fontWeight = FontWeight.SemiBold,
                            style = TextStyle(fontSize = 9.sp, color = versionTextColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Dot() {
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .size(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
    )
}
