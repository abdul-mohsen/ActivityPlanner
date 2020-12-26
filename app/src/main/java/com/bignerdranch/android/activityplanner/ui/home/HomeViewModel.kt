package com.bignerdranch.android.activityplanner.ui.home

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

class HomeViewModel : ViewModel() {

    private lateinit var location: LatLng
    var selectedDate: String = "2020-12-26 00:00"
    private var onMoveScreenJob: Job? = null
    private var onDateChangeJob: Job? = null
    private var weatherJob: Job? = null

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
    }

    @FlowPreview
    private fun loadNewData(term: String = ""){
        viewModelScope.launch(Dispatchers.IO) {
            val tempBusinessList = mutableListOf<Business>()
            BusinessRepository.getBusinesses(
                term = term,
                latitude = location.latitude,
                longitude = location.longitude,
                pageCount = 4
            ).collect { list ->
                tempBusinessList.addAll(list)
            }
            Timber.d("A new list have been loaded $tempBusinessList")
            BusinessRepository.insert(tempBusinessList)
        }
    }

    @FlowPreview
    private suspend fun loadWeather(list: List<Business>) {
        weatherJob?.cancel()
        var counter = 3
        weatherJob = viewModelScope.launch {
            Timber.d("$list")
            WeatherRepository.getWeather(list).collect{ list ->
                try {
                    Timber.d("  ${list.first().businessId }  ${ _businessList.value.map {it.id}}")
                    _businessList.value.first { it.id == list.first().businessId }
                        .weather = list.first { it.time == selectedDate }
                    Timber.d("new Weather data")
                    if (counter > 0 ){
                        counter--
                        _weatherDataState.emit(WeatherDataState.NewTemp)
                    } else
                        _weatherDataState.emit(WeatherDataState.NewData)
                } catch (e: Exception) {
                    Timber.d(e)
                }
            }
            _weatherDataState.emit(WeatherDataState.NewTemp)
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
            BusinessRepository.allBusinessByLatLon(latLng.latitude, latLng.longitude).collect { list ->
                Timber.d("New call for data")
                if (list.isEmpty()) {
                    delay(1000)
                    Timber.d("Going to the internet")
                    loadNewData()
                }
                else updateDate(list.toMutableList() )
//                else updateDate(list.map { business ->
//                Timber.d("$business")
//                BusinessRepository.getFullBusinessInfo(BusinessRepository.getById(business.id))
//            }.toMutableList() )
            }
        }
    }

    @FlowPreview
    fun updateDate(
        businesses: MutableList<Business> = businessList.value.toMutableList(),
        date: String = selectedDate,
        targetState: WeatherDataState = WeatherDataState.NewData
    ) {
        onDateChangeJob?.cancel()
        selectedDate = date
        onDateChangeJob = viewModelScope.launch(Dispatchers.Default) {
            val oldWeatherList = businesses.filter { it.weather == null || it.weather!!.time != date }
            val ids = oldWeatherList.map { it.id }
            Timber.d("$ids , $date")
            val sqlWeatherList = WeatherRepository.allWeatherByBusinessIdAndDate(ids, date)
            Timber.d("Here is the real problem")
            Timber.d("$sqlWeatherList")
            sqlWeatherList.forEach { weather ->
                businesses.first { business ->
                    business.id == weather.businessId
                }.weather = weather
            }

            _businessList.emit(businesses)
            Timber.d("some complicated shit is going on here")
            Timber.d("${businesses.map { it.weather }}")
            _weatherDataState.emit(targetState)

            val needInternet = businesses.filter { it.weather == null || it.weather!!.time != date }
            if (needInternet.isNotEmpty()) loadWeather(needInternet)
        }
    }
}
