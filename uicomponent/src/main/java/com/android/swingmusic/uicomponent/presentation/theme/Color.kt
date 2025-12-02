package com.android.swingmusic.uicomponent.presentation.theme

import androidx.compose.ui.graphics.Color

// ============================================
// AMOLED Spotify Dark Theme Colors
// True black for OLED displays + Spotify green
// ============================================

// Spotify Green accent (official Spotify colors)
val SpotifyGreen = Color(0xFF1DB954)
val SpotifyGreenLight = Color(0xFF1ED760)
val SpotifyGreenDark = Color(0xFF169C46)
val SpotifyGreenMuted = Color(0xFF1A7C3D)

// AMOLED Pure Black backgrounds (true black for OLED power saving)
val AmoledBlack = Color(0xFF000000)
val AmoledDarkGray = Color(0xFF0A0A0A)
val AmoledMediumGray = Color(0xFF141414)
val AmoledLightGray = Color(0xFF1F1F1F)
val AmoledElevated = Color(0xFF242424)
val AmoledCard = Color(0xFF181818)

// Legacy aliases for compatibility
val SpotifyBlack = AmoledBlack
val SpotifyDarkGray = AmoledDarkGray
val SpotifyMediumGray = AmoledMediumGray
val SpotifyLightGray = AmoledLightGray

// Text colors - high contrast for AMOLED
val SpotifyWhite = Color(0xFFFFFFFF)
val SpotifyLightText = Color(0xFFB3B3B3)
val SpotifySubdued = Color(0xFF6A6A6A)
val SpotifyDimText = Color(0xFF535353)

// Surface tones for cards and elevated elements
val SurfaceElevated1 = Color(0xFF121212)
val SurfaceElevated2 = Color(0xFF1E1E1E)
val SurfaceElevated3 = Color(0xFF2A2A2A)

// Accent colors for badges and highlights
val AccentGold = Color(0xFFFFD700)
val AccentCyan = Color(0xFF00D4FF)
val AccentPurple = Color(0xFFB388FF)
val AccentPink = Color(0xFFFF4081)

// ============================================
// AMOLED Spotify Dark Theme Color Scheme
// ============================================

val dark_primary = SpotifyGreen
val dark_onPrimary = Color(0xFF002E12)
val dark_primaryContainer = Color(0xFF0D3F1D)
val dark_onPrimaryContainer = SpotifyGreenLight
val dark_secondary = SpotifyLightText
val dark_onSecondary = AmoledBlack
val dark_secondaryContainer = AmoledMediumGray
val dark_onSecondaryContainer = SpotifyWhite
val dark_tertiary = AccentCyan
val dark_onTertiary = AmoledBlack
val dark_tertiaryContainer = Color(0xFF004D61)
val dark_onTertiaryContainer = Color(0xFFBFE9F9)
val dark_error = Color(0xFFFF6B6B)
val dark_onError = Color(0xFF3D0000)
val dark_errorContainer = Color(0xFF5C1010)
val dark_onErrorContainer = Color(0xFFFFDAD6)
val dark_outline = SpotifySubdued
val dark_background = AmoledBlack
val dark_onBackground = SpotifyWhite
val dark_surface = AmoledBlack
val dark_onSurface = SpotifyWhite
val dark_surfaceVariant = AmoledMediumGray
val dark_onSurfaceVariant = SpotifyLightText
val dark_inverseSurface = SpotifyWhite
val dark_inverseOnSurface = AmoledDarkGray
val dark_inversePrimary = SpotifyGreenDark
val dark_shadow = AmoledBlack
val dark_surfaceTint = SpotifyGreen
val dark_outlineVariant = AmoledLightGray
val dark_scrim = AmoledBlack

// Additional surface colors for Material3
val dark_surfaceContainer = AmoledDarkGray
val dark_surfaceContainerLow = AmoledBlack
val dark_surfaceContainerHigh = AmoledMediumGray
val dark_surfaceContainerHighest = AmoledLightGray
val dark_surfaceBright = AmoledElevated
val dark_surfaceDim = AmoledBlack

val seed = SpotifyGreen
