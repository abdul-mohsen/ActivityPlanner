package com.bignerdranch.android.activityplanner.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.activityplanner.Repo.BusinessRepository
import com.bignerdranch.android.activityplanner.model.Business
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeViewModel : ViewModel() {
    private val repository = BusinessRepository

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text


    private val _businessList: MutableStateFlow<List<Business>> = MutableStateFlow(emptyList())
    val businessList: StateFlow<List<Business>> = _businessList

    @FlowPreview
    fun loadNewData(){
        viewModelScope.launch {
            val tempBusinessList = mutableListOf<Business>()
            repository.getBusinesses(
                term = "delis",
                latitude = 37.786882,
                longitude = -122.399972
            ).collect { list ->
                tempBusinessList.addAll(list)
            }
            Timber.d("A new list have been loaded $tempBusinessList")
            _businessList.emit(tempBusinessList)
        }
    }
}