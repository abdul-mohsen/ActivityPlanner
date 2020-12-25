package com.bignerdranch.android.activityplanner.ui.dashboard

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.bignerdranch.android.activityplanner.R
import com.bignerdranch.android.activityplanner.databinding.FragmentDashboardBinding
import com.bignerdranch.android.activityplanner.model.Business
import com.bignerdranch.android.activityplanner.ui.adapter.BusinessAdapter
import com.bignerdranch.android.activityplanner.ui.adapter.StringAdapter
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import timber.log.Timber

class DashboardFragment : Fragment()  {

    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var binding: FragmentDashboardBinding
    private lateinit var myAdapter: StringAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentDashboardBinding.inflate(layoutInflater, container, false)
        dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)
        myAdapter = StringAdapter()
        binding.grid.run {
            this.adapter = myAdapter
        }



        binding.button.setOnClickListener {
            myAdapter.submitList(listOf(
                "12314", "1231241", "@#%@#$524" , "3252352"
            ).toList())
        }

        return binding.root
    }

}