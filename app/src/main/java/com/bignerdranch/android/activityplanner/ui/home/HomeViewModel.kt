package com.bignerdranch.android.activityplanner.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.activityplanner.Repository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeViewModel : ViewModel() {
    private val repository = Repository

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text


    private val _businessList: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val businessList: StateFlow<List<String>> = _businessList

    @FlowPreview
    fun loadNewData(){
        viewModelScope.launch {
            val tempBusinessList = mutableListOf<String>()
            repository.getBusinesses(
                term = "delis",
                latitude = 37.786882,
                longitude = -122.399972
            ).collect { list ->
                tempBusinessList.addAll(list)
            }
            Timber.d("A new list have been loaded")
            _businessList.emit(tempBusinessList)
        }
    }
}