package com.bignerdranch.android.activityplanner.ui.home

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.coroutineScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.bignerdranch.android.activityplanner.OnSnapPositionChangeListener
import com.bignerdranch.android.activityplanner.R
import com.bignerdranch.android.activityplanner.SnapOnScrollListener
import com.bignerdranch.android.activityplanner.databinding.FragmentHomeBinding
import com.bignerdranch.android.activityplanner.model.DataState
import com.bignerdranch.android.activityplanner.ui.adapter.BusinessAdapter
import com.bignerdranch.android.activityplanner.ui.adapter.MyArrayAdapter
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class HomeFragment : Fragment(), OnMapReadyCallback {
    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private val homeViewModel: HomeViewModel by lazy { ViewModelProvider(this).get(HomeViewModel::class.java) }
    private lateinit var binding: FragmentHomeBinding
    private lateinit var arrayAdapter: MyArrayAdapter
    private lateinit var businessAdapter: BusinessAdapter
    private lateinit var mapStyle: String
    private lateinit var markerImage: Bitmap
    private var featureList: List<Feature> = emptyList()
    private var isMapReady = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(requireContext(), getString(R.string.mapbox_access_token))
        lifecycle.coroutineScope.launch(Dispatchers.IO) {
            markerImage = Picasso.get().load(R.drawable.mapbox_marker_icon_default).get()
        }
        
        mapStyle = when(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> Style.DARK
            else -> Style.MAPBOX_STREETS
        }
    }

    @FlowPreview
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        businessAdapter = BusinessAdapter()

        binding.grid.adapter = businessAdapter
        binding.button.setOnClickListener {
            binding.layoutSlider.visibility = when(binding.layoutSlider.visibility){
                View.GONE -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.timeText.setOnClickListener {
            findNavController().navigate(
                HomeFragmentDirections.actionNavigationHomeToDatePickerFragment(
                    Date = homeViewModel.selectedDate.value.time,
                    Key = DATE_KEY
                )
            )
        }
        findNavController().currentBackStackEntry?.savedStateHandle?.apply {
            getLiveData<Long>(DATE_KEY).observe(
                viewLifecycleOwner,
                {date ->
                    homeViewModel.updateDate(
                        date = homeViewModel.addHoursToDate(Date(date)),
                        targetState = DataState.NewWeatherData
                    )
                }
            )
        }

        binding.slider.addOnChangeListener { _, value, _ ->
            val date = homeViewModel.addHoursToDate(value)
            homeViewModel.updateDate(date = date, targetState = DataState.NewWeatherData)
        }
        binding.businessSearch.addTextChangedListener(
            onTextChanged = { _, _, _, _ ->
                homeViewModel.autoComplete(binding.businessSearch.text.toString())
            }
        )
        arrayAdapter = MyArrayAdapter(requireContext(), R.layout.auto_fill_item)
        binding.businessSearch.setAdapter(arrayAdapter)
        binding.businessSearch.setOnItemClickListener { parent, _, position, _ ->
            homeViewModel.searchAPI(parent.getItemAtPosition(position).toString())
        }

        observeBusinessList()
        observeWeatherDataState()
        observeAutoCompleteList()
        observeDate()

        return binding.root
    }

    @FlowPreview
    private fun observeBusinessList() {
        lifecycle.coroutineScope.launchWhenStarted {
            homeViewModel.businessList.collect { list ->
                Timber.d("I have been called")
                businessAdapter.submitList(homeViewModel.businessList.value)
                featureList = list.map { Feature.fromGeometry(
                    Point.fromLngLat(it.coordinates.longitude, it.coordinates.latitude)
                ) }
                if (isMapReady)
                    mapboxMap.run {
                        getStyle { it.doThings(featureList) }
                        mapboxMap.animateCamera { CameraPosition.Builder()
                            .target(LatLng(list.first().coordinates.latitude,list.first().coordinates.longitude))
                            .zoom(10.0)
                            .tilt(20.0)
                            .build()
                        }
                    }
            }
        }
    }

    private fun observeWeatherDataState() {
        lifecycle.coroutineScope.launchWhenStarted {
            homeViewModel.dataState.collect { state ->
                Timber.d("$state")
                when (state) {
                    DataState.Idle -> {
                        Timber.d("I am Idle")
                        binding.progressBar.visibility = View.GONE
                    }
                    DataState.NewBusinessData -> {
                        homeViewModel.updateDataState(DataState.Idle)
                        businessAdapter.submitList(homeViewModel.businessList.value)
                    }
                    DataState.NewWeatherData -> {
                        homeViewModel.updateDataState(DataState.Idle)
                        businessAdapter.submitList(homeViewModel.businessList.value)
                        businessAdapter.notifyDataSetChanged()
                    }
                    DataState.NoBusinessMatch -> {
                        Toast.makeText(requireContext(), "No business found", Toast.LENGTH_SHORT).show()
                        homeViewModel.updateDataState(DataState.Idle)
                    }
                    DataState.NoWeatherData -> {
                        Toast.makeText(requireContext(), "No weather data found", Toast.LENGTH_SHORT).show()
                        businessAdapter.notifyDataSetChanged()
                        homeViewModel.updateDataState(DataState.Idle)
                    }
                    DataState.Updating -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun observeAutoCompleteList() {
        lifecycle.coroutineScope.launchWhenStarted {
            homeViewModel.apply {
                autoCompleteFlow.collect { autoComplete ->
                    arrayAdapter.submit(autoComplete)
                }
            }
        }
    }

    private fun observeDate() {
        lifecycle.coroutineScope.launchWhenStarted {
            homeViewModel.selectedDate.collect {
                binding.timeText.text = homeViewModel.shortDate
            }
        }
    }

    @FlowPreview
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        isMapReady = true
        mapboxMap.addOnCameraMoveListener {
            mapboxMap.run {
                homeViewModel.updateLocation(cameraPosition.target)
                Timber.d("The camera is moving ")
            }
        }
        val snapHelper = LinearSnapHelper()
        binding.grid.attachSnapHelperWithListener(snapHelper) { position ->
            try {
                val cord = homeViewModel.businessList.value[position].coordinates
                mapboxMap.animateCamera { CameraPosition.Builder()
                    .target(LatLng(cord.latitude,cord.longitude))
                    .zoom(10.0)
                    .tilt(20.0)
                    .build()
                }
            } catch (e: Exception) {
                Timber.d(e)
            }
        }
        Timber.d("I have been called")
        mapboxMap.setStyle(mapStyle) {
            // Map is set up and the style has loaded. Now you can add data or make other map adjustments
            it.doThings(featureList)
        }
        homeViewModel.updateLocation(mapboxMap.cameraPosition.target)
    }

    private fun Style.doThings(symbolLayerIconFeatureList: List<Feature>) {
        this.run {
            removeLayer(LAYER_ID)
            removeSource(SOURCE_ID)
            addImage(ICON_ID, markerImage)
            addSource(
                GeoJsonSource(
                    SOURCE_ID,
                    FeatureCollection.fromFeatures(symbolLayerIconFeatureList)
                )
            )
            addLayer(
                SymbolLayer(LAYER_ID, SOURCE_ID).withProperties(
                    iconImage(ICON_ID),
                    iconAllowOverlap(true)
                )
            )
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    private fun RecyclerView.attachSnapHelperWithListener(
        snapHelper: SnapHelper,
        behavior: SnapOnScrollListener.Behavior = SnapOnScrollListener.Behavior.NOTIFY_ON_SCROLL,
        onSnapPositionChangeListener: OnSnapPositionChangeListener
    ) {
        snapHelper.attachToRecyclerView(this)
        val snapOnScrollListener =
            SnapOnScrollListener(snapHelper, behavior, onSnapPositionChangeListener)
        addOnScrollListener(snapOnScrollListener)
    }

    companion object {
        private const val SOURCE_ID = "SOURCE_ID"
        private const val ICON_ID = "ICON_ID"
        private const val LAYER_ID = "LAYER_ID"
        private const val DATE_KEY = "Date"
    }
}
