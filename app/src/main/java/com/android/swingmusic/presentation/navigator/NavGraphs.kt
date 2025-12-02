package com.android.swingmusic.presentation.navigator

import android.os.Bundle
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.featurealbum.destinations.AlbumWithInfoScreenDestination
import com.ramcosta.composedestinations.generated.featurealbum.destinations.AllAlbumScreenDestination
import com.ramcosta.composedestinations.generated.featureartist.destinations.AllArtistsScreenDestination
import com.ramcosta.composedestinations.generated.featureartist.destinations.ArtistInfoScreenDestination
import com.ramcosta.composedestinations.generated.featureartist.destinations.ViewAllScreenOnArtistDestination
import com.ramcosta.composedestinations.generated.auth.destinations.LoginWithQrCodeDestination
import com.ramcosta.composedestinations.generated.auth.destinations.LoginWithUsernameScreenDestination
import com.ramcosta.composedestinations.generated.featurefolder.destinations.FoldersAndTracksPaginatedScreenDestination
import com.ramcosta.composedestinations.generated.featurehome.destinations.HomeDestination
import com.ramcosta.composedestinations.generated.featureplayer.destinations.NowPlayingScreenDestination
import com.ramcosta.composedestinations.generated.featureplayer.destinations.QueueScreenDestination
import com.ramcosta.composedestinations.generated.featuresearch.destinations.SearchScreenDestination
import com.ramcosta.composedestinations.generated.featuresearch.destinations.ViewAllSearchResultsDestination
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.spec.NavGraphSpec
import com.ramcosta.composedestinations.spec.TypedNavHostGraphSpec
import com.ramcosta.composedestinations.spec.TypedRoute

/**
 * No animation style for NavHost - all transitions are instant
 */
object NoTransitions : NavHostAnimatedDestinationStyle() {
    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { EnterTransition.None }
    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { ExitTransition.None }
    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { EnterTransition.None }
    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { ExitTransition.None }
}

object NavGraphs {
    fun root(isUserLoggedIn: Boolean): TypedNavHostGraphSpec<Unit> = object : TypedNavHostGraphSpec<Unit> {
        override val route: String = "root"

        override val startRoute: TypedRoute<Unit> =
            if (isUserLoggedIn) HomeDestination else LoginWithQrCodeDestination

        override val destinations: List<DestinationSpec> = listOf(
            // pre-auth
            LoginWithQrCodeDestination,
            LoginWithUsernameScreenDestination,
            // post-auth - bottom nav
            HomeDestination,
            FoldersAndTracksPaginatedScreenDestination,
            AllAlbumScreenDestination,
            AllArtistsScreenDestination,
            SearchScreenDestination,
            // inner destinations
            NowPlayingScreenDestination,
            QueueScreenDestination,
            AlbumWithInfoScreenDestination,
            ViewAllScreenOnArtistDestination,
            ArtistInfoScreenDestination,
            ViewAllSearchResultsDestination,
        )

        override val nestedNavGraphs: List<NavGraphSpec> = emptyList()

        override val defaultTransitions: NavHostAnimatedDestinationStyle = NoTransitions

        override val defaultStartDirection: Direction = if (isUserLoggedIn) HomeDestination() else LoginWithQrCodeDestination

        override fun argsFrom(bundle: Bundle?): Unit = Unit

        override fun argsFrom(savedStateHandle: SavedStateHandle): Unit = Unit
    }
}
