package com.bignerdranch.android.activityplanner.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.activityplanner.Repo.BusinessRepository
import com.bignerdranch.android.activityplanner.Repo.SearchHistoryRepository
import com.bignerdranch.android.activityplanner.Repo.WeatherRepository
import com.bignerdranch.android.activityplanner.model.*
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import java.util.Date
import java.util.Calendar
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.coroutines.CoroutineContext
import kotlin.math.cos
import kotlin.math.pow

class HomeViewModel(
    private val ioDispatcher: CoroutineContext = Dispatchers.IO,
    private val cpuDispatcher: CoroutineContext = Dispatchers.Default) :
    ViewModel() {

    private var location: LatLng = LatLng(tempLat, tempLon)
    private var onMoveScreenJob: Job? = null
    private var onDateChangeJob: Job? = null
    private var autoCompeteJob: Job? = null

    private val _businessList: MutableStateFlow<List<Business>> = MutableStateFlow(emptyList())
    val businessList: StateFlow<List<Business>> = _businessList

    private val _dataState: MutableStateFlow<DataState> = MutableStateFlow(DataState.Idle)
    val dataState: StateFlow<DataState> = _dataState

    private val _autoCompleteFlow: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val autoCompleteFlow: StateFlow<List<String>> = _autoCompleteFlow

    private val _selectedDate: MutableStateFlow<Date> = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate
    private val fullDate: String
        @RequiresApi(Build.VERSION_CODES.O)
        get() = selectedDate.value.format(FULL_DATE_FORMAT)
    val shortDate: String
        @RequiresApi(Build.VERSION_CODES.O)
        get() = selectedDate.value.format(DATE_FORMAT)

    var searchHistoryList: List<String> = emptyList()

    init {
        viewModelScope.launch(ioDispatcher) {
            SearchHistoryRepository.allSearchHistory.collect { list ->
                searchHistoryList = list.map { it.query }
            }
        }
    }

    @FlowPreview
    fun autoComplete(query: String) {
        autoCompeteJob?.cancel()
        autoCompeteJob = viewModelScope.launch(ioDispatcher) {
            if (query.isBlank())
                _autoCompleteFlow.emit(searchHistoryList.toSet().toList())
            else BusinessRepository.getAutoComplete(
                text = query,
                latitude = location.latitude,
                longitude = location.longitude
            ).collect { autoComplete ->
                _autoCompleteFlow.emit(getListOfAutoComplete(autoComplete))
            }
        }
    }

    private fun getListOfAutoComplete(autoComplete: AutoComplete): List<String> = autoComplete
        .run {  mutableListOf<String>().apply {
            addAll(categories)
            addAll(businesses)
            addAll(terms)
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
                if (list.isEmpty()) {
                    updateDataState(DataState.NoBusinessMatch)
                } else {
                    location = latLng
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
            _selectedDate.emit(date)
            updateDataState(DataState.Updating)
            WeatherRepository.allWeatherByBusinessIdAndDate(businesses = businesses, fullDate, shortDate).collect { list ->
                if (list.isEmpty()) {
                    businesses.forEach { it.weather = null }
                    _businessList.emit(businesses)
                    updateDataState(DataState.NoWeatherData)
                } else {
                    list.forEach { weather ->
                        businesses.first { business ->
                            business.id == weather.businessId
                        }.weather = weather
                    }
                    _businessList.emit(businesses)
                    updateDataState(targetState)
                }
            }
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun Date.format(format: String ): String {
        val localDate = this.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        val formatter = DateTimeFormatter.ofPattern(format)
        return localDate.format(formatter)
    }

    companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd"
        private const val FULL_DATE_FORMAT = "yyyy-MM-dd HH:00"
        private const val tempLat = 0.0
        private const val tempLon = 0.0
    }
}
