package com.bignerdranch.android.activityplanner.ui.notifications

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
import com.bignerdranch.android.activityplanner.databinding.FragmentNotificationsBinding
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import timber.log.Timber

class NotificationsFragment : Fragment() {

    private lateinit var notificationsViewModel: NotificationsViewModel
    private lateinit var binding: FragmentNotificationsBinding

    @FlowPreview
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        notificationsViewModel =
                ViewModelProvider(this).get(NotificationsViewModel::class.java)
        binding = FragmentNotificationsBinding.inflate(layoutInflater, container, false)
        notificationsViewModel.text.observe(viewLifecycleOwner, Observer {
            binding.textNotifications.text = it
        })
        notificationsViewModel.loadWeather()
        Timber.d("We are here")
        return binding.root
    }

    fun observeWeather(){
        lifecycle.coroutineScope.launchWhenStarted {
            notificationsViewModel.busList.collect { list ->
                Timber.d("Some change")
            }
        }
    }
}