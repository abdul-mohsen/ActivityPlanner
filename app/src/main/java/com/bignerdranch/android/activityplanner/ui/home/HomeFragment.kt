package com.bignerdranch.android.activityplanner.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.coroutineScope
import com.bignerdranch.android.activityplanner.databinding.FragmentHomeBinding
import com.bignerdranch.android.activityplanner.model.WeatherDataState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var binding: FragmentHomeBinding
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
                autoCompeteJob = lifecycle.coroutineScope.launch {
                    delay(1000)
                    Timber.d("Testing the delay")
                    homeViewModel.autoComplete(binding.businessSearch.text.toString())
                }
            }
        )

        binding.businessSearch.setOnItemClickListener { _, _, _, _ ->
            Timber.d("Yay you found what you want to search")
        }
//        observeBusinessList()
//        observeWeatherDataState()
        homeViewModel.loadNewData()
        observeAutoCompleteList()
        observeAllBusinesses()

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
                    binding.businessSearch.setAdapter(ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        getListOfAutoComplete(autoComplete)
                    ))
                }
            }
        }
    }

    private fun observeAllBusinesses() {
        lifecycle.coroutineScope.launchWhenStarted {
            homeViewModel.getAllBusiness().collect { list ->
                list.forEach {
                    Timber.d("Cat = ${it.categories}")
                }
                Timber.d("Data base list  = $list")
            }
        }
    }
}