package com.android.swingmusic.uicomponent.presentation.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.swingmusic.core.domain.model.Artist
import com.android.swingmusic.uicomponent.R
import com.android.swingmusic.uicomponent.presentation.theme.SwingMusicTheme_Preview

/**
 * Compact artist item with overlay text - for grid views
 */
@Composable
fun ArtistItem(
    modifier: Modifier,
    artist: Artist,
    baseUrl: String,
    onClick: (artistHash: String) -> Unit
) {
    val cardShape = RoundedCornerShape(6.dp)
    
    Box(
        modifier = modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clip(cardShape)
            .background(Color(0xFF151515))
            .clickable { onClick(artist.artistHash) }
    ) {
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = ImageRequest.Builder(LocalContext.current)
                .data("${baseUrl}img/artist/medium/${artist.image}")
                .crossfade(true)
                .build(),
            placeholder = painterResource(R.drawable.artist_fallback),
            fallback = painterResource(R.drawable.artist_fallback),
            error = painterResource(R.drawable.artist_fallback),
            contentDescription = artist.name,
            contentScale = ContentScale.Crop
        )

        // Gradient overlay with text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.5f to Color.Black.copy(alpha = 0.4f),
                        1f to Color.Black.copy(alpha = 0.85f)
                    )
                )
                .padding(8.dp)
        ) {
            Text(
                text = artist.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ArtistItemPreview() {
    fun generateDummyArtist(): Artist {
        val artisthash = "dummy_artist"
        val colors = listOf("#FFFFFF", "#000000", "#FF0000")
        val createdDate = System.currentTimeMillis().toDouble()
        val helpText = "2hrs, 10 minutes"
        val image = "https://example.com/image.jpg"
        val name = "Dummy Artist"

        return Artist(artisthash, colors, createdDate, helpText, image, name)
    }

    SwingMusicTheme_Preview {
        Surface {
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                columns = GridCells.Fixed(3),
                state = rememberLazyGridState(),
            ) {
                items(6) {
                    ArtistItem(
                        modifier = Modifier.fillMaxWidth(),
                        artist = generateDummyArtist(),
                        baseUrl = "",
                        onClick = {}
                    )
                }
            }
        }
    }
}
