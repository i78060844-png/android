package com.android.swingmusic.uicomponent.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Spotify-style dark color scheme
private val spotifyDarkColorScheme = darkColorScheme(
    primary = dark_primary,
    onPrimary = dark_onPrimary,
    primaryContainer = dark_primaryContainer,
    onPrimaryContainer = dark_onPrimaryContainer,
    secondary = dark_secondary,
    onSecondary = dark_onSecondary,
    secondaryContainer = dark_secondaryContainer,
    onSecondaryContainer = dark_onSecondaryContainer,
    tertiary = dark_tertiary,
    onTertiary = dark_onTertiary,
    tertiaryContainer = dark_tertiaryContainer,
    onTertiaryContainer = dark_onTertiaryContainer,
    error = dark_error,
    errorContainer = dark_errorContainer,
    onError = dark_onError,
    onErrorContainer = dark_onErrorContainer,
    background = dark_background,
    onBackground = dark_onBackground,
    surface = dark_surface,
    onSurface = dark_onSurface,
    surfaceVariant = dark_surfaceVariant,
    onSurfaceVariant = dark_onSurfaceVariant,
    outline = dark_outline,
    inverseOnSurface = dark_inverseOnSurface,
    inverseSurface = dark_inverseSurface,
    inversePrimary = dark_inversePrimary,
    surfaceTint = dark_surfaceTint,
    outlineVariant = dark_outlineVariant,
    scrim = dark_scrim,
)

/**
 * SwingMusicTheme - Applies Spotify-style dark theme to the app
 * 
 * Note: Parameters are kept for backward compatibility but are ignored.
 * The theme always uses the Spotify dark color scheme.
 * 
 * @param navBarColor Ignored - kept for API compatibility
 */

@Composable
fun SwingMusicTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    @Suppress("UNUSED_PARAMETER") navBarColor: Color? = null,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = spotifyDarkColorScheme,
        typography = Typography,
        content = content
    )
}


/**
 * SwingMusicTheme_Preview - Simplified theme function for Compose previews
 * 
 * This function avoids context dependencies that can break previews in
 * certain Compose versions. It uses the same Spotify dark color scheme
 * as the main theme function.
 */

@Composable
fun SwingMusicTheme_Preview(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = spotifyDarkColorScheme,
        typography = Typography,
    ) {
        content()
    }
}
