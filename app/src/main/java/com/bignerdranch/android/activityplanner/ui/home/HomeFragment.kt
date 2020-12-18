package com.bignerdranch.android.activityplanner.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.coroutineScope
import com.bignerdranch.android.activityplanner.databinding.FragmentHomeBinding
import com.bignerdranch.android.activityplanner.model.WeatherDataState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import timber.log.Timber

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var binding: FragmentHomeBinding

    @FlowPreview
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        homeViewModel.text.observe(viewLifecycleOwner, {
            binding.textHome.text = it
        })

        observeBusinessList()
        observeWeatherDataState()
        homeViewModel.loadNewData()

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
                    WeatherDataState.NewData -> WeatherDataState.Idle
                }
            }
        }
    }
}