package com.android.swingmusic.home.presentation.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.swingmusic.core.domain.model.Album
import com.android.swingmusic.core.domain.model.Artist
import com.android.swingmusic.auth.domain.repository.AuthRepository
import com.android.swingmusic.network.data.api.service.NetworkApiService
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
    
    // Random/popular albums from library
    val popularAlbums: List<Album> = emptyList(),
    
    // Random/popular artists from library
    val popularArtists: List<Artist> = emptyList(),
    
    // Stats
    val totalAlbums: Int = 0,
    val totalArtists: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val networkApiService: NetworkApiService,
    private val authRepository: AuthRepository
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
                val baseUrl = authRepository.getBaseUrl()
                val token = authRepository.getAccessToken()
                
                if (baseUrl == null || token == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Not authenticated") }
                    return@launch
                }
                
                _baseUrl.value = baseUrl
                val bearerToken = "Bearer $token"
                
                // Load data in parallel
                val albumsDeferred = async {
                    try {
                        networkApiService.getAllAlbums(
                            url = "${baseUrl}getall/albums",
                            bearerToken = bearerToken,
                            pageSize = 12,
                            startIndex = (0..50).random(),
                            sortBy = "created_date",
                            sortOrder = 1
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
                            sortBy = "created_date",
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
                
                // Await all results
                val albumsResult = albumsDeferred.await()
                val artistsResult = artistsDeferred.await()
                val albumsCountResult = albumsCountDeferred.await()
                val artistsCountResult = artistsCountDeferred.await()
                
                // Convert albums using DTO field names
                val albums = albumsResult?.albumDto?.mapNotNull { dto ->
                    try {
                        Album(
                            albumHash = dto.albumHash ?: return@mapNotNull null,
                            title = dto.title ?: "",
                            image = dto.image ?: "",
                            albumArtists = dto.albumArtistDto?.map { artistDto ->
                                Artist(
                                    artistHash = artistDto.artisthash ?: "",
                                    colors = artistDto.colors ?: emptyList(),
                                    createdDate = artistDto.createdDate ?: 0.0,
                                    helpText = artistDto.helpText ?: "",
                                    image = artistDto.image ?: "",
                                    name = artistDto.name ?: ""
                                )
                            } ?: emptyList(),
                            colors = dto.colors ?: emptyList(),
                            createdDate = dto.createdDate ?: 0.0,
                            date = dto.date ?: 0,
                            helpText = dto.helpText ?: "",
                            versions = dto.versions ?: emptyList()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }?.shuffled()?.take(8) ?: emptyList()
                
                // Convert artists using DTO field names
                val artists = artistsResult?.artistsDto?.mapNotNull { dto ->
                    try {
                        Artist(
                            artistHash = dto.artisthash ?: return@mapNotNull null,
                            colors = dto.colors ?: emptyList(),
                            createdDate = dto.createdDate ?: 0.0,
                            helpText = dto.helpText ?: "",
                            image = dto.image ?: "",
                            name = dto.name ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }?.shuffled()?.take(10) ?: emptyList()
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        popularAlbums = albums,
                        popularArtists = artists,
                        totalAlbums = albumsCountResult?.total ?: 0,
                        totalArtists = artistsCountResult?.total ?: 0
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
}
