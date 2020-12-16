package com.bignerdranch.android.activityplanner.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.coroutineScope
import com.bignerdranch.android.activityplanner.R
import com.bignerdranch.android.activityplanner.databinding.ActivityMainBinding
import com.bignerdranch.android.activityplanner.databinding.FragmentHomeBinding
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
        homeViewModel.text.observe(viewLifecycleOwner, Observer {
            binding.textHome.text = it
        })

        observeBusinessList()
        homeViewModel.loadNewData()
        Timber.d("Should run")

        return binding.root
    }

    private fun observeBusinessList(){
        lifecycle.coroutineScope.launchWhenStarted {
            homeViewModel.businessList.collect {
                Timber.d("Some change has been observe")
            }
        }
    }
}