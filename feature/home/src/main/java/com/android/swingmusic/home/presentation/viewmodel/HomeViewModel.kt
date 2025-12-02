package com.android.swingmusic.home.presentation.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.swingmusic.core.domain.model.Album
import com.android.swingmusic.core.domain.model.Artist
import com.android.swingmusic.core.domain.model.Folder
import com.android.swingmusic.core.domain.model.Track
import com.android.swingmusic.database.data.dao.QueueDao
import com.android.swingmusic.database.data.entity.QueueEntity
import com.android.swingmusic.network.data.api.service.NetworkApiService
import com.android.swingmusic.auth.data.workmanager.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    
    // Recently played from different sources (not just current queue)
    val recentTracks: List<Track> = emptyList(),
    
    // Random/popular albums from library (NOT from current queue)
    val popularAlbums: List<Album> = emptyList(),
    
    // Random/popular artists from library
    val popularArtists: List<Artist> = emptyList(),
    
    // Folders
    val folders: List<Folder> = emptyList(),
    
    // Stats
    val totalAlbums: Int = 0,
    val totalArtists: Int = 0,
    val totalFolders: Int = 0,
    val queueSize: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val networkApiService: NetworkApiService,
    private val tokenManager: TokenManager,
    private val queueDao: QueueDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val _baseUrl = mutableStateOf<String?>(null)
    val baseUrl: State<String?> = _baseUrl
    
    init {
        loadHomeData()
    }
    
    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val baseUrl = tokenManager.getBaseUrl()
                val token = tokenManager.getAccessToken()
                
                if (baseUrl == null || token == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Not authenticated") }
                    return@launch
                }
                
                _baseUrl.value = baseUrl
                val bearerToken = "Bearer $token"
                
                // Load data in parallel
                val albumsDeferred = async {
                    try {
                        // Get random albums - sort by random or recently added
                        networkApiService.getAllAlbums(
                            url = "${baseUrl}getall/albums",
                            bearerToken = bearerToken,
                            pageSize = 12,
                            startIndex = (0..50).random(), // Random offset for variety
                            sortBy = "created_date", // Recently added
                            sortOrder = 1 // Descending (newest first)
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                val artistsDeferred = async {
                    try {
                        networkApiService.getAllArtists(
                            url = "${baseUrl}getall/artists",
                            bearerToken = bearerToken,
                            pageSize = 10,
                            startIndex = (0..30).random(),
                            sortBy = "playcount", // Most played
                            sortOrder = 1
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                val albumsCountDeferred = async {
                    try {
                        networkApiService.getAlbumsCount(
                            url = "${baseUrl}getall/albums",
                            bearerToken = bearerToken
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                val artistsCountDeferred = async {
                    try {
                        networkApiService.getArtistsCount(
                            url = "${baseUrl}getall/artists",
                            bearerToken = bearerToken
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                val rootDirsDeferred = async {
                    try {
                        networkApiService.getRootDirectories(
                            url = "${baseUrl}getdir",
                            bearerToken = bearerToken
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                val queueDeferred = async {
                    try {
                        queueDao.getSavedQueue()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                
                // Await all results
                val albumsResult = albumsDeferred.await()
                val artistsResult = artistsDeferred.await()
                val albumsCountResult = albumsCountDeferred.await()
                val artistsCountResult = artistsCountDeferred.await()
                val rootDirsResult = rootDirsDeferred.await()
                val queueResult = queueDeferred.await()
                
                // Convert albums
                val albums = albumsResult?.albums?.map { dto ->
                    Album(
                        albumHash = dto.albumHash,
                        title = dto.title,
                        image = dto.image,
                        albumArtists = dto.albumArtists.map { artistDto ->
                            com.android.swingmusic.core.domain.model.AlbumArtist(
                                artistHash = artistDto.artistHash,
                                image = artistDto.image,
                                name = artistDto.name
                            )
                        },
                        createdDate = dto.createdDate,
                        date = dto.date,
                        trackCount = dto.trackCount,
                        duration = dto.duration,
                        isFavorite = dto.isFavorite,
                        color = dto.color,
                        versions = dto.versions?.map { it } ?: emptyList(),
                        genreHashes = dto.genreHashes ?: emptyList(),
                        baseTitle = dto.baseTitle ?: dto.title
                    )
                }?.shuffled()?.take(8) ?: emptyList() // Shuffle for variety
                
                // Convert artists
                val artists = artistsResult?.artists?.map { dto ->
                    Artist(
                        artistHash = dto.artistHash,
                        name = dto.name,
                        image = dto.image,
                        albumCount = dto.albumCount,
                        trackCount = dto.trackCount,
                        duration = dto.duration,
                        isFavorite = dto.isFavorite,
                        color = dto.color
                    )
                }?.shuffled()?.take(10) ?: emptyList()
                
                // Convert folders
                val folders = rootDirsResult?.rootDirs?.map { dto ->
                    Folder(
                        name = dto.name,
                        path = dto.path,
                        trackCount = dto.trackCount,
                        folderCount = dto.folderCount,
                        isSym = dto.isSym
                    )
                }?.take(6) ?: emptyList()
                
                // Get recent tracks from queue (but shuffle to not show same order)
                val recentTracks = queueResult
                    .shuffled()
                    .take(10)
                    .map { it.toTrack() }
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        recentTracks = recentTracks,
                        popularAlbums = albums,
                        popularArtists = artists,
                        folders = folders,
                        totalAlbums = albumsCountResult?.total ?: 0,
                        totalArtists = artistsCountResult?.total ?: 0,
                        totalFolders = folders.size,
                        queueSize = queueResult.size
                    )
                }
                
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load home data"
                    )
                }
            }
        }
    }
    
    fun refresh() {
        loadHomeData()
    }
    
    private fun QueueEntity.toTrack(): Track {
        return Track(
            trackHash = trackHash,
            album = album,
            albumHash = albumHash,
            bitrate = bitrate,
            duration = duration,
            filepath = filepath,
            folder = folder,
            image = image,
            isFavorite = isFavorite,
            title = title,
            albumTrackArtists = albumTrackArtists.map {
                com.android.swingmusic.core.domain.model.TrackArtist(
                    artistHash = it.artistHash,
                    name = it.name,
                    image = it.image
                )
            },
            trackArtists = trackArtists.map {
                com.android.swingmusic.core.domain.model.TrackArtist(
                    artistHash = it.artistHash,
                    name = it.name,
                    image = it.image
                )
            },
            disc = disc,
            trackNumber = trackNumber
        )
    }
}
