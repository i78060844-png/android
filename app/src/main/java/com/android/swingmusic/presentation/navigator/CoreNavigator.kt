package com.android.swingmusic.presentation.navigator

import androidx.navigation.NavController
import com.ramcosta.composedestinations.generated.featurealbum.destinations.AlbumWithInfoScreenDestination
import com.ramcosta.composedestinations.generated.featurealbum.destinations.AllAlbumScreenDestination
import com.ramcosta.composedestinations.generated.featureartist.destinations.ArtistInfoScreenDestination
import com.ramcosta.composedestinations.generated.featureartist.destinations.AllArtistsScreenDestination
import com.ramcosta.composedestinations.generated.featureartist.destinations.ViewAllScreenOnArtistDestination
import com.ramcosta.composedestinations.generated.auth.destinations.LoginWithQrCodeDestination
import com.ramcosta.composedestinations.generated.auth.destinations.LoginWithUsernameScreenDestination
import com.android.swingmusic.common.presentation.navigator.CommonNavigator
import com.ramcosta.composedestinations.generated.featurefolder.destinations.FoldersAndTracksPaginatedScreenDestination
import com.ramcosta.composedestinations.generated.featurehome.destinations.HomeDestination
import com.ramcosta.composedestinations.generated.featureplayer.destinations.QueueScreenDestination
import com.ramcosta.composedestinations.generated.featuresearch.destinations.SearchScreenDestination
import com.ramcosta.composedestinations.generated.featuresearch.destinations.ViewAllSearchResultsDestination
import com.ramcosta.composedestinations.spec.RouteOrDirection
import com.ramcosta.composedestinations.utils.toDestinationsNavigator

class CoreNavigator(
    private val navController: NavController
) : CommonNavigator {

    private val navigator get() = navController.toDestinationsNavigator()

    // Get the start destination as a RouteOrDirection for popUpTo calls
    private val startDestination: RouteOrDirection
        get() = HomeDestination

    /**----------------------------------- Auth Navigator ----------------------------------------*/
    override fun gotoLoginWithUsername() {
        navigator.navigate(LoginWithUsernameScreenDestination) {
            launchSingleTop = true
            restoreState = false

            popUpTo(startDestination) {
                inclusive = true
                saveState = false
            }
        }
    }

    override fun gotoLoginWithQrCode() {
        navigator.navigate(LoginWithQrCodeDestination) {
            launchSingleTop = true
            restoreState = false

            popUpTo(startDestination) {
                inclusive = true
                saveState = false
            }
        }
    }

    override fun gotoHome() {
        navigator.navigate(HomeDestination()) {
            launchSingleTop = true
            restoreState = false

            popUpTo(startDestination) {
                inclusive = true
                saveState = false
            }
        }
    }

    // Todo: Remove this after adding home content
    override fun gotoFolders() {
        navigator.navigate(FoldersAndTracksPaginatedScreenDestination()) {
            launchSingleTop = true
            restoreState = false

            popUpTo(startDestination) {
                inclusive = true
                saveState = false
            }
        }
    }

    override fun gotoAlbumLibrary() {
        navigator.navigate(AllAlbumScreenDestination) {
            launchSingleTop = true
            restoreState = false

            popUpTo(startDestination) {
                inclusive = false
                saveState = false
            }
        }
    }

    override fun gotoArtistLibrary() {
        navigator.navigate(AllArtistsScreenDestination) {
            launchSingleTop = true
            restoreState = false

            popUpTo(startDestination) {
                inclusive = false
                saveState = false
            }
        }
    }

    override fun gotoSearch() {
        navigator.navigate(SearchScreenDestination) {
            launchSingleTop = true
        }
    }

    /**----------------------------------- Album Navigator --------------------------------------*/
    override fun gotoAlbumWithInfo(albumHash: String) {
        navigator.navigate(AlbumWithInfoScreenDestination(albumHash)) {
            launchSingleTop = true
        }
    }

    override fun navigateBack() {
        navController.navigateUp()
    }

    /**----------------------------------- Player Navigator -------------------------------------*/
    override fun gotoQueueScreen() {
        navigator.navigate(QueueScreenDestination) {
            launchSingleTop = true
        }
    }

    override fun gotoArtistInfo(artistHash: String) {
        navigator.navigate(
            ArtistInfoScreenDestination(
                artistHash = artistHash,
                loadNewArtist = true
            )
        ) {
            launchSingleTop = true
        }
    }

    override fun gotoViewAllOnArtistScreen(
        viewAllType: String,
        artistName: String,
        baseUrl: String
    ) {
        navigator.navigate(
            ViewAllScreenOnArtistDestination(
                viewAllType = viewAllType,
                artistName = artistName,
                baseUrl = baseUrl
            )
        ) {
            launchSingleTop = true
        }
    }

    override fun gotoViewAllSearchResultsScreen(
        viewAllType: String,
        searchParams: String
    ) {
        navigator.navigate(
            ViewAllSearchResultsDestination(
                searchParams = searchParams,
                viewAllType = viewAllType
            )
        ) {
            launchSingleTop = true
        }
    }

    override fun gotoSourceFolder(name: String, path: String) {
        navigator.navigate(FoldersAndTracksPaginatedScreenDestination(name, path)) {
            launchSingleTop = true
        }
    }
}
