package com.bignerdranch.android.activityplanner.ui.home

import androidx.core.graphics.translationMatrix
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.activityplanner.Repo.BusinessRepository
import com.bignerdranch.android.activityplanner.Repo.SearchHistoryRepository
import com.bignerdranch.android.activityplanner.Repo.WeatherRepository
import com.bignerdranch.android.activityplanner.model.*
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeViewModel : ViewModel() {

    private var location: LatLng = LatLng(37.786942,-122.399643)
    private var getData = BusinessRepository.allBusiness

    private val _businessList: MutableStateFlow<List<Business>> = MutableStateFlow(emptyList())
    val businessList: StateFlow<List<Business>> = _businessList

    private val _weatherDataState: MutableStateFlow<WeatherDataState> = MutableStateFlow(WeatherDataState.Idle)
    val weatherDataState: StateFlow<WeatherDataState> = _weatherDataState

    private val _autoCompleteFlow: MutableStateFlow<AutoComplete> = MutableStateFlow(AutoComplete())
    val autoCompleteFlow: StateFlow<AutoComplete> = _autoCompleteFlow

    var searchHistoryList: List<String> = emptyList()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            SearchHistoryRepository.allSearchHistory.collect { list ->
                searchHistoryList = list.map { it.query }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            getData.collect { list ->
                _businessList.emit(
                    list.map { business ->
                        Timber.d("$business")
                        BusinessRepository.getFullBusinessInfo(
                            BusinessRepository.getById(
                                business.id
                            )
                        )
                    }
                )
            }
        }
    }

    @FlowPreview
    private fun loadNewData(){
        viewModelScope.launch {
            val tempBusinessList = mutableListOf<Business>()
            BusinessRepository.getBusinesses(
                term = "",
                latitude = location.latitude,
                longitude = location.longitude
            ).collect { list ->
                tempBusinessList.addAll(list)
            }
            Timber.d("A new list have been loaded $tempBusinessList")
            BusinessRepository.insert(tempBusinessList)
        }
    }

    @FlowPreview
    suspend fun loadWeather(list: List<Business>) {
        viewModelScope.launch {
            Timber.d("$list")
            WeatherRepository.getWeather(list).collect{ list ->
                _businessList.value.first { it.id == list.first().businessId }
                    .weatherTimeMap.putAll(list.map { it.timeEpoch to it }.toMap())
                Timber.d("new Weather data")
                _weatherDataState.emit(WeatherDataState.NewData)
            }
        }
    }

    @FlowPreview
    fun autoComplete(query: String) {
        viewModelScope.launch {
            BusinessRepository.autoComplete(
                text = query,
                latitude = location.latitude,
                longitude = location.longitude
            ).collect { autoComplete ->
                Timber.d("This is a test stay a way $autoComplete")
                _autoCompleteFlow.emit(autoComplete)
            }
        }
    }

    fun getListOfAutoComplete(autoComplete: AutoComplete): List<String> = autoComplete
        .run {  mutableListOf<String>().apply {
            addAll(businesses)
            addAll(terms)
            addAll(categories)
        } }.toSet().toList().also { Timber.d("$it") }

    suspend fun updateWeatherDataState(state: WeatherDataState) {
        _weatherDataState.emit(state)
    }

    @FlowPreview
    fun searchAPI(query: String) {
        viewModelScope.launch {
            SearchHistoryRepository.insert(SearchHistory(query = query))
            BusinessRepository.getBusinesses(
                term = query,
                latitude = location.latitude,
                longitude = location.longitude
            )
        }
    }

    @FlowPreview
    fun updateLocation(latLng: LatLng) {
        viewModelScope.launch(Dispatchers.IO) {
            location = latLng
            val temp = BusinessRepository.allBusinessByLatLon(latLng.latitude, latLng.longitude)
            Timber.d("$temp  +++_+_+_+_")
            if (temp.isEmpty()) loadNewData()
            else _businessList.emit(temp)
        }
    }
}
