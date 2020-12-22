package com.bignerdranch.android.activityplanner.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.get
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.coroutineScope
import com.bignerdranch.android.activityplanner.R
import com.bignerdranch.android.activityplanner.databinding.FragmentHomeBinding
import com.bignerdranch.android.activityplanner.model.WeatherDataState
import com.bignerdranch.android.activityplanner.ui.adapter.MyArrayAdapter
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.internal.notifyAll
import timber.log.Timber

class HomeFragment : Fragment() {
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var binding: FragmentHomeBinding
    private lateinit var arrayAdapter: MyArrayAdapter
    private var autoCompeteJob: Job? = null

    @FlowPreview
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        Timber.d("I have been created")
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.businessSearch.addTextChangedListener(
            onTextChanged = { _, _, _, _ ->
                autoCompeteJob?.cancel()
                if (binding.businessSearch.text.isBlank())
                    arrayAdapter.submit(homeViewModel.searchHistoryList.toSet().toList())
                else {
                    autoCompeteJob = lifecycle.coroutineScope.launch {
                        delay(1000)
                        Timber.d("Testing the delay")
                        homeViewModel.autoComplete(binding.businessSearch.text.toString())
                    }
                }
            }
        )
        arrayAdapter = MyArrayAdapter(
            requireContext(),
            R.layout.auto_fill_item
        )
        binding.businessSearch.setAdapter(arrayAdapter)
        binding.businessSearch.setOnItemClickListener { parent, _, position, _ ->
            Timber.d("Yay you found what you want to search")
            homeViewModel.searchAPI(parent.getItemAtPosition(position).toString())
        }
        observeBusinessList()
        observeWeatherDataState()
        observeAutoCompleteList()
        return binding.root
    }

    @FlowPreview
    private fun observeBusinessList(){
        lifecycle.coroutineScope.launchWhenStarted {
            homeViewModel.businessList.collect { list ->
                Timber.d("test")
                if (list.isNotEmpty()){
                    homeViewModel.loadWeather(list)
                }
            }
        }
    }

    private fun observeWeatherDataState() {
        lifecycle.coroutineScope.launchWhenStarted {
            homeViewModel.weatherDataState.collect { state ->
                Timber.d("$state")
                when (state) {
                    WeatherDataState.Idle -> Timber.d("___")
                    WeatherDataState.NewData -> homeViewModel.updateWeatherDataState(WeatherDataState.Idle)
                }
            }
        }
    }

    private fun observeAutoCompleteList() {
        lifecycle.coroutineScope.launchWhenStarted {
            homeViewModel.apply {
                autoCompleteFlow.collect { autoComplete ->
                    val autoList = getListOfAutoComplete(autoComplete).toMutableList()
                    Timber.d("my auto list $autoList")
                     arrayAdapter.submit(
                         autoList
                     )
                }
            }
        }
    }
}
