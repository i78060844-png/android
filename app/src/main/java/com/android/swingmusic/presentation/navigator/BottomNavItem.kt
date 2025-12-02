package com.android.swingmusic.presentation.navigator

import androidx.annotation.DrawableRes
import com.ramcosta.composedestinations.generated.featurealbum.destinations.AllAlbumScreenDestination
import com.ramcosta.composedestinations.generated.featureartist.destinations.AllArtistsScreenDestination
import com.ramcosta.composedestinations.generated.featurefolder.destinations.FoldersAndTracksPaginatedScreenDestination
import com.ramcosta.composedestinations.generated.featurehome.destinations.HomeDestination
import com.ramcosta.composedestinations.generated.featuresearch.destinations.SearchScreenDestination
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.android.swingmusic.uicomponent.R as UiComponent

sealed class BottomNavItem(
    var title: String,
    @DrawableRes var icon: Int,
    var destination: DestinationSpec
) {
    data object Home : BottomNavItem(
        title = "Home",
        icon = UiComponent.drawable.ic_home_rounded,
        destination = HomeDestination
    )

    data object Folder : BottomNavItem(
        title = "Folders",
        icon = UiComponent.drawable.folder_filled,
        destination = FoldersAndTracksPaginatedScreenDestination
    )

    data object Album : BottomNavItem(
        title = "Albums",
        icon = UiComponent.drawable.ic_album_rounded,
        destination = AllAlbumScreenDestination
    )

    data object Artist : BottomNavItem(
        title = "Artists",
        icon = UiComponent.drawable.ic_artist_rounded,
        destination = AllArtistsScreenDestination
    )

    data object Search : BottomNavItem(
        title = "Search",
        icon = UiComponent.drawable.ic_search_rounded,
        destination = SearchScreenDestination
    )
}
