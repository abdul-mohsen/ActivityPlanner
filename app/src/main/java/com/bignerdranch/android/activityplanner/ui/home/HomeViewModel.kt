package com.bignerdranch.android.activityplanner.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.activityplanner.Repo.BusinessRepository
import com.bignerdranch.android.activityplanner.Repo.SearchHistoryRepository
import com.bignerdranch.android.activityplanner.Repo.WeatherRepository
import com.bignerdranch.android.activityplanner.model.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeViewModel : ViewModel() {

    suspend fun getAllBusiness() = BusinessRepository.allBusiness.stateIn(viewModelScope)

    private val _businessList: MutableStateFlow<List<Business>> = MutableStateFlow(emptyList())
    val businessList: StateFlow<List<Business>> = _businessList

    private val _weatherDataState: MutableStateFlow<WeatherDataState> = MutableStateFlow(WeatherDataState.Idle)
    val weatherDataState: StateFlow<WeatherDataState> = _weatherDataState

    private val _autoCompleteFlow: MutableStateFlow<AutoComplete> = MutableStateFlow(AutoComplete())
    val autoCompleteFlow: StateFlow<AutoComplete> = _autoCompleteFlow

    var searchHistoryList: List<String> = emptyList()

    init {
        viewModelScope.launch {
            SearchHistoryRepository.allSearchHistory.collect { list ->
                searchHistoryList = list.map { it.query }
            }
        }
    }

    @FlowPreview
    fun loadNewData(){
        viewModelScope.launch {
            BusinessRepository.deleteAll()
            val tempBusinessList = mutableListOf<Business>()
            BusinessRepository.getBusinesses(
                term = "",
                latitude = 37.786882,
                longitude = -122.399972
            ).collect { list ->
                tempBusinessList.addAll(list)
            }
            Timber.d("A new list have been loaded $tempBusinessList")
            BusinessRepository.insert(tempBusinessList)
            _businessList.emit(tempBusinessList)
        }
    }

    @FlowPreview
    suspend fun loadWeather(list: List<Business>) {
        viewModelScope.launch {
            Timber.d("$list")
            WeatherRepository.getWeather(list).collect{ list ->
                _businessList.value.first()
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
            Timber.d("This is a test stay a way")
            BusinessRepository.autoComplete(
                text = query,
                latitude = 37.786882,
                longitude = -122.399972
            ).collect { autoComplete ->
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
                latitude = 37.786882,
                longitude = -122.399972
            )
        }
    }
}
