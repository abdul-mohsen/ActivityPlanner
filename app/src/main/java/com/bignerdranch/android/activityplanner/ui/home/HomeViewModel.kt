package com.bignerdranch.android.activityplanner.ui.home

import android.text.format.DateFormat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.activityplanner.Repo.BusinessRepository
import com.bignerdranch.android.activityplanner.Repo.SearchHistoryRepository
import com.bignerdranch.android.activityplanner.Repo.WeatherRepository
import com.bignerdranch.android.activityplanner.model.AutoComplete
import com.bignerdranch.android.activityplanner.model.Business
import com.bignerdranch.android.activityplanner.model.SearchHistory
import com.bignerdranch.android.activityplanner.model.WeatherDataState
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import java.util.*

class HomeViewModel : ViewModel() {

    private lateinit var location: LatLng
    private var getData = BusinessRepository.allBusiness
    private var onMoveScreenJob: Job? = null

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
    private suspend fun loadWeather(list: List<Business>) {
        viewModelScope.launch {
            Timber.d("$list")
            WeatherRepository.getWeather(list).collect{ list ->
                try {
                    _businessList.value.first { it.id == list.first().businessId }
                        .weatherTimeMap.putAll(list.map { it.timeEpoch to it }.toMap())
                    Timber.d("new Weather data")
                    _weatherDataState.emit(WeatherDataState.NewData)
                } catch (e: Exception) {
                    Timber.d(e)
                }

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
        onMoveScreenJob?.cancel()
        onMoveScreenJob = viewModelScope.launch(Dispatchers.IO) {
            location = latLng
            delay(500)

            val temp = BusinessRepository.allBusinessByLatLon(latLng.latitude, latLng.longitude)
            Timber.d("$temp  +++_+_+_+_")
            if (temp.isEmpty()) {
                delay(1000)
                Timber.d("Going online")
                loadNewData()
            }
            else _businessList.emit(temp)
        }
    }

    @FlowPreview
    suspend fun updateData(businesses: MutableList<Business>, date: Int = 1608879600) {
        val queryWeatherList = businesses.filter { it.weatherTimeMap[date] == null }
        val ids = queryWeatherList.map { it.id }
        Timber.d("$ids , $date")
        val output = WeatherRepository.allWeatherByBusinessIdAndDate(ids, date)
        Timber.d("Here is the real problem")
        Timber.d("$output")
        output.forEach { weather ->
            queryWeatherList.first { business ->
                business.id == weather.businessId
            }.weatherTimeMap[date] = weather
        }

        queryWeatherList.filter { it.weatherTimeMap[date] != null }.forEach { business ->
            businesses.first { it.id == business.id }.weatherTimeMap[date] = business.weatherTimeMap[date]!!
        }

        _businessList.emit(businesses)

        val myTempFormat = "yyyy-MM-dd HH:00"
        DateFormat.format(myTempFormat, Date(date* 1000L)).also {
            Timber.d("$it")
        }
        Timber.d("some complicated shit is going on here")
        Timber.d("${queryWeatherList.map { it.weatherTimeMap }}")
        _weatherDataState.emit(WeatherDataState.NewData)

        val needInternet = queryWeatherList.filter { it.weatherTimeMap[date] == null }
        if (needInternet.isNotEmpty()) loadWeather(needInternet)
    }

}
