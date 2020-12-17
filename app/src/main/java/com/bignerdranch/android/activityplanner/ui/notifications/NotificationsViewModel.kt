package com.bignerdranch.android.activityplanner.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.activityplanner.Repo.WeatherRepository
import com.bignerdranch.android.activityplanner.model.Business
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

class NotificationsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is notifications Fragment"
    }
    val text: LiveData<String> = _text
    private val _busList: MutableStateFlow<List<Business>> = MutableStateFlow(emptyList())
    val busList:StateFlow<List<Business>> = _busList


    @FlowPreview
    fun loadWeather(){
        viewModelScope.launch {
            WeatherRepository.getWeather(
                listOf(48.8567 to 2.3508),
                id = "1"
            ).collect{ mapItem ->
                Timber.d("${mapItem.first} ${mapItem.second}")
                }
        }
    }


}