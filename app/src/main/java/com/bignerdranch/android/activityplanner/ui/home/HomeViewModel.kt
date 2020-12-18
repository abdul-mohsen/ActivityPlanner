package com.bignerdranch.android.activityplanner.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.activityplanner.Repo.BusinessRepository
import com.bignerdranch.android.activityplanner.Repo.WeatherRepository
import com.bignerdranch.android.activityplanner.model.Business
import com.bignerdranch.android.activityplanner.model.WeatherDataState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeViewModel : ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text


    private val _businessList: MutableStateFlow<List<Business>> = MutableStateFlow(emptyList())
    val businessList: StateFlow<List<Business>> = _businessList

    private val _weatherDataState: MutableStateFlow<WeatherDataState> = MutableStateFlow(WeatherDataState.Idle)
    val weatherDataState: StateFlow<WeatherDataState> = _weatherDataState

    @FlowPreview
    fun loadNewData(){
        viewModelScope.launch {
            val tempBusinessList = mutableListOf<Business>()
            BusinessRepository.getBusinesses(
                term = "",
                latitude = 37.786882,
                longitude = -122.399972
            ).collect { list ->
                tempBusinessList.addAll(list)
            }
            Timber.d("A new list have been loaded $tempBusinessList")
            _businessList.emit(tempBusinessList)
        }
    }

    @FlowPreview
    fun loadWeather(list: List<Business>) {
        viewModelScope.launch {
            Timber.d("$list")
            WeatherRepository.getWeather(list).collect{ mapItem ->
                Timber.d("${mapItem.first} ${mapItem.second}")
                _businessList.value.first { it.id == mapItem.first }
                    .weatherTimeMap.putAll(mapItem.second.map { it.timeEpoch to it }.toMap())
                Timber.d("new Weather data")
                _weatherDataState.emit(WeatherDataState.NewData)
            }
        }
    }

    suspend fun updateWeatherDataState(state: WeatherDataState) {
        _weatherDataState.emit(state)
    }
}
