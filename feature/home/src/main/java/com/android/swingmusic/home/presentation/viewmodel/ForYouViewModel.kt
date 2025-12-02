package com.android.swingmusic.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.swingmusic.core.domain.model.HomeSection
import com.android.swingmusic.core.domain.model.Recommendation
import com.android.swingmusic.core.domain.model.StatsPeriod
import com.android.swingmusic.core.domain.model.Track
import com.android.swingmusic.core.domain.model.TrackStats
import com.android.swingmusic.core.domain.repository.DataHealth
import com.android.swingmusic.core.domain.repository.MusicForYouRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForYouState(
    val isLoading: Boolean = true,
    val hasEnoughData: Boolean = false,
    val dataHealth: DataHealth? = null,
    val sections: List<HomeSection> = emptyList(),
    val inTheMomentTracks: List<Recommendation> = emptyList(),
    val topTracksThisWeek: List<Pair<Track, TrackStats>> = emptyList(),
    val rediscoverTracks: List<Recommendation> = emptyList(),
    val timeOfDayMix: List<Track> = emptyList(),
    val timeOfDayName: String = "",
    val totalPlays: Int = 0,
    val error: String? = null
)

@HiltViewModel
class ForYouViewModel @Inject constructor(
    private val musicForYouRepository: MusicForYouRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(ForYouState())
    val state: StateFlow<ForYouState> = _state.asStateFlow()
    
    init {
        loadForYouData()
        observeStats()
    }
    
    fun loadForYouData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                val hasEnoughData = musicForYouRepository.hasEnoughDataForPersonalization()
                val dataHealth = musicForYouRepository.getDataHealth()
                
                if (hasEnoughData) {
                    val sections = musicForYouRepository.getPersonalizedHomeSections()
                    val inTheMoment = musicForYouRepository.getInTheMomentRecommendations()
                    val topTracks = musicForYouRepository.getTopTracks(StatsPeriod.THIS_WEEK, 10)
                    val rediscover = musicForYouRepository.getRediscoveryRecommendations(8)
                    val timeOfDayMix = musicForYouRepository.getTimeOfDayMix(12)
                    
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            hasEnoughData = true,
                            dataHealth = dataHealth,
                            sections = sections,
                            inTheMomentTracks = inTheMoment,
                            topTracksThisWeek = topTracks,
                            rediscoverTracks = rediscover,
                            timeOfDayMix = timeOfDayMix,
                            timeOfDayName = getTimeOfDayName()
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            hasEnoughData = false,
                            dataHealth = dataHealth
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load recommendations"
                    )
                }
            }
        }
    }
    
    fun refresh() {
        loadForYouData()
    }
    
    private fun observeStats() {
        viewModelScope.launch {
            musicForYouRepository.observeTotalPlays().collect { count ->
                _state.update { it.copy(totalPlays = count) }
            }
        }
    }
    
    private fun getTimeOfDayName(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Утренний микс"
            in 12..16 -> "Дневной микс"
            in 17..21 -> "Вечерний микс"
            else -> "Ночной микс"
        }
    }
}
