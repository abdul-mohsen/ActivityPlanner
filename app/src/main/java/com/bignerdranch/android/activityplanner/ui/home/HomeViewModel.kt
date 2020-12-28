package com.bignerdranch.android.activityplanner.ui.home

import android.text.format.DateFormat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.activityplanner.Repo.BusinessRepository
import com.bignerdranch.android.activityplanner.Repo.SearchHistoryRepository
import com.bignerdranch.android.activityplanner.Repo.WeatherRepository
import com.bignerdranch.android.activityplanner.model.*
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import java.util.*
import kotlin.math.cos
import kotlin.math.pow

class HomeViewModel : ViewModel() {
    var ioDispatcher = Dispatchers.IO
    var cpuDispatcher = Dispatchers.Default

    private var location: LatLng = LatLng(tempLat, tempLon)
    private var onMoveScreenJob: Job? = null
    private var onDateChangeJob: Job? = null
    private var weatherJob: Job? = null
    private var autoCompeteJob: Job? = null

    private val _businessList: MutableStateFlow<List<Business>> = MutableStateFlow(emptyList())
    val businessList: StateFlow<List<Business>> = _businessList

    private val _dataState: MutableStateFlow<DataState> = MutableStateFlow(DataState.Idle)
    val dataState: StateFlow<DataState> = _dataState

    private val _autoCompleteFlow: MutableStateFlow<List<String>> = MutableStateFlow(listOf())
    val autoCompleteFlow: StateFlow<List<String>> = _autoCompleteFlow

    private val _selectedDate: MutableStateFlow<Date> = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate
    private val fullDate: String
        get() = DateFormat.format(FULL_DATE_FORMAT ,selectedDate.value).toString()
    val shortDate: String
        get() = DateFormat.format(DATE_FORMAT ,selectedDate.value).toString()

    var searchHistoryList: List<String> = emptyList()

    init {
        viewModelScope.launch(ioDispatcher) {
            SearchHistoryRepository.allSearchHistory.collect { list ->
                searchHistoryList = list.map { it.query }
            }
        }
    }

    @FlowPreview
    private fun loadNewData(term: String = "", latLom: LatLng){
        viewModelScope.launch(ioDispatcher) {
            val tempBusinessList = mutableListOf<Business>()
            BusinessRepository.getBusinesses(
                term = term,
                latitude = latLom.latitude,
                longitude = latLom.longitude,
                pageCount = 4
            ).collect { list ->
                tempBusinessList.addAll(list)
            }
            if (tempBusinessList.isEmpty() ) {
                updateDataState(DataState.NoBusinessMatch)
            } else {
                Timber.d("A new list have been loaded $tempBusinessList")
                BusinessRepository.insert(tempBusinessList)
            }
        }
    }

    @FlowPreview
    private suspend fun loadWeather(list: List<Business>) {
        weatherJob?.cancel()
        var counter = 3
        weatherJob = viewModelScope.launch(ioDispatcher) {
            Timber.d("$list")
            WeatherRepository.getWeather(list, shortDate).collect{ list ->
                try {
                    Timber.d("  ${list.first().businessId }  ${ _businessList.value.map {it.id}}")
                    _businessList.value.first { it.id == list.first().businessId }
                        .weather = list.first { it.time == fullDate }
                    Timber.d("new Weather data")
                    if (counter > 0 ){
                        counter--
                        _dataState.emit(DataState.NewWeatherData)
                    } else
                        _dataState.emit(DataState.NewBusinessData)
                } catch (e: Exception) {
                    Timber.d(e)
                }
            }
            if (counter == 3) updateDataState(DataState.NoWeatherData)
            else updateDataState(DataState.NewWeatherData)
        }
    }

    @FlowPreview
    fun autoComplete(query: String) {
        autoCompeteJob = viewModelScope.launch(ioDispatcher) {
            if (query.isBlank())
                _autoCompleteFlow.emit(searchHistoryList.toSet().toList())
            else BusinessRepository.autoComplete(
                text = query,
                latitude = location.latitude,
                longitude = location.longitude
            ).collect { autoComplete ->
                Timber.d("This is a test stay a way $autoComplete")
                _autoCompleteFlow.emit(getListOfAutoComplete(autoComplete))
            }
        }
    }

    private fun getListOfAutoComplete(autoComplete: AutoComplete): List<String> = autoComplete
        .run {  mutableListOf<String>().apply {
            addAll(businesses)
            addAll(terms)
            addAll(categories)
        } }.toSet().toList().also { Timber.d("$it") }

    suspend fun updateDataState(state: DataState) {
        _dataState.emit(state)
    }

    @FlowPreview
    fun searchAPI(query: String) {
        viewModelScope.launch(ioDispatcher) {
            updateDataState(DataState.Updating)
            SearchHistoryRepository.insert(SearchHistory(query = query))
            val businesses = mutableListOf<Business>()
            BusinessRepository.getBusinesses(
                term = query,
                latitude = location.latitude,
                longitude = location.longitude
            ).collect { list ->
                businesses.addAll(list)
            }
            if (businesses.isNotEmpty()) updateDate(businesses = businesses)
            else _dataState.emit(DataState.NoBusinessMatch)
        }
    }

    @FlowPreview
    fun updateLocation(latLng: LatLng) {
        if (isDiffBig(latLng, 0.255)) return

        onMoveScreenJob?.cancel()
        onMoveScreenJob = viewModelScope.launch(ioDispatcher) {
            updateDataState(DataState.Updating)
            delay(500)
            BusinessRepository.allBusinessByLatLon(latLng.latitude, latLng.longitude).collect { list ->
                Timber.d("New call for data")
                if (list.isEmpty()) {
                    Timber.d("Going to the internet")
                    loadNewData(latLom = latLng)
                } else {
                    location = latLng
                    val listBusinessWithCategories = BusinessRepository.getFullBusinessInfo(list.map { it.id })
                    listBusinessWithCategories.forEach { businessWithCategories ->
                        list.first { it.id == businessWithCategories.business.id }
                            .categories = businessWithCategories.categories
                    }
                    updateDate(list.toMutableList())
                }
            }
        }
    }

    private fun isDiffBig(latLng: LatLng, threshold: Double) =
        (latLng.altitude - location.altitude).pow(2) + (latLng.longitude - location.longitude).pow(2) * cos(
            Math.toRadians(latLng.altitude)
        ).pow(2) < threshold

    @FlowPreview
    fun updateDate(
        businesses: MutableList<Business> = businessList.value.toMutableList(),
        date: Date = _selectedDate.value,
        targetState: DataState = DataState.NewBusinessData
    ) {
        if (businesses.size == 0) return
        onDateChangeJob?.cancel()
        onDateChangeJob = viewModelScope.launch(cpuDispatcher) {
            updateDataState(DataState.Updating)
            _selectedDate.emit(date)
            val fullDate = DateFormat.format(FULL_DATE_FORMAT ,date).toString()
            val oldWeatherList = businesses.filter { it.weather == null || it.weather!!.time != fullDate }
            val ids = oldWeatherList.map { it.id }
            Timber.d("$ids , $date")
            val sqlWeatherList = WeatherRepository.allWeatherByBusinessIdAndDate(ids, fullDate)
            Timber.d("$sqlWeatherList")
            sqlWeatherList.forEach { weather ->
                businesses.first { business ->
                    business.id == weather.businessId
                }.weather = weather
            }
            if (sqlWeatherList.isNotEmpty()) updateDataState(targetState)
            _businessList.emit(businesses)

            val needInternet = businesses.filter { it.weather == null || it.weather!!.time != fullDate }
            if (needInternet.isNotEmpty()) loadWeather(needInternet)
        }
    }

    fun addHoursToDate(date: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        val cal2 = Calendar.getInstance()
        cal2.time = selectedDate.value
        cal.set(Calendar.HOUR_OF_DAY, cal2.get(Calendar.HOUR_OF_DAY))
        return cal.time
    }

    fun addHoursToDate(value: Float): Date {
        val cal = Calendar.getInstance()
        cal.time = selectedDate.value
        cal.set(Calendar.HOUR_OF_DAY, value.toInt())
        return cal.time
    }

    companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd"
        private const val FULL_DATE_FORMAT = "yyyy-MM-dd HH:00"
        private const val tempLat = 0.0
        private const val tempLon = 0.0
    }
}
