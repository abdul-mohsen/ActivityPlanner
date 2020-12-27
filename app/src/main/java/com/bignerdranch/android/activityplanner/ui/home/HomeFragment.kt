package com.bignerdranch.android.activityplanner.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.opengl.Visibility
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import com.bignerdranch.android.activityplanner.model.WeatherDataState
import com.bignerdranch.android.activityplanner.ui.adapter.BusinessAdapter
import com.bignerdranch.android.activityplanner.ui.adapter.MyArrayAdapter
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import java.lang.Exception
import java.util.*

class HomeFragment : Fragment(), OnMapReadyCallback, PermissionsListener {
    private lateinit var mapView: MapView
    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private lateinit var mapboxMap: MapboxMap
    private val homeViewModel: HomeViewModel by lazy { ViewModelProvider(this).get(HomeViewModel::class.java) }
    private lateinit var binding: FragmentHomeBinding
    private lateinit var arrayAdapter: MyArrayAdapter
    private lateinit var businessAdapter: BusinessAdapter
    private var autoCompeteJob: Job? = null
    private lateinit var x: Bitmap
    private var featureList: List<Feature> = emptyList()
    private var isMapReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(requireContext(), getString(R.string.mapbox_access_token))
        lifecycle.coroutineScope.launch(Dispatchers.IO) {
            x = Picasso.get().load(R.drawable.mapbox_marker_icon_default).get()
        }
    }

    @FlowPreview
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("I have been created")

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
                    Key = DATE_KEY)
            )
        }
        findNavController().currentBackStackEntry?.savedStateHandle?.apply {
            getLiveData<Long>(DATE_KEY).observe(
                viewLifecycleOwner,
                {date ->
                    homeViewModel.updateDate(date = homeViewModel.addHoursToDate(Date(date)))
                }
            )
        }

        binding.slider.addOnChangeListener { _, value, _ ->
            val cal = Calendar.getInstance()
            cal.time = homeViewModel.selectedDate.value
            cal.set(Calendar.HOUR_OF_DAY, value.toInt())
            homeViewModel.updateDate(date = cal.time, targetState = WeatherDataState.NewTemp)
        }
        binding.businessSearch.addTextChangedListener(
            onTextChanged = { _, _, _, _ ->
                autoCompeteJob?.cancel()
                if (binding.businessSearch.text.isBlank())
                    arrayAdapter.submit(homeViewModel.searchHistoryList.toSet().toList())
                else
                    autoCompeteJob = lifecycle.coroutineScope.launch {
                        delay(1000)
                        Timber.d("Testing the delay")
                        homeViewModel.autoComplete(binding.businessSearch.text.toString())
                    }

            }
        )
        arrayAdapter = MyArrayAdapter(requireContext(), R.layout.auto_fill_item)
        binding.businessSearch.setAdapter(arrayAdapter)
        binding.businessSearch.setOnItemClickListener { parent, _, position, _ ->
            Timber.d("Yay you found what you want to search")
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
                if (list.isNotEmpty()) {
                    if (list.first().weather == null)
                        homeViewModel.updateDate(list.toMutableList())

                    featureList = list.map { Feature.fromGeometry(
                        Point.fromLngLat(it.coordinates.longitude, it.coordinates.latitude)
                    ) }
                    if (isMapReady)
                        mapboxMap.getStyle { it.doThings(featureList) }
                }
            }
        }
    }

    @FlowPreview
    private fun observeWeatherDataState() {
        lifecycle.coroutineScope.launchWhenStarted {
            homeViewModel.weatherDataState.collect { state ->
                Timber.d("$state")
                when (state) {
                    WeatherDataState.Idle -> Timber.d("___")
                    WeatherDataState.NewData -> {
                        homeViewModel.updateWeatherDataState(WeatherDataState.Idle)
                        businessAdapter.submitList(homeViewModel.businessList.value)
                    }
                    WeatherDataState.NewTemp -> {
                        homeViewModel.updateWeatherDataState(WeatherDataState.Idle)
                        businessAdapter.submitList(homeViewModel.businessList.value)
                        businessAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    @FlowPreview
    private fun observeAutoCompleteList() {
        lifecycle.coroutineScope.launchWhenStarted {
            homeViewModel.apply {
                autoCompleteFlow.collect { autoComplete ->
                    val autoList = getListOfAutoComplete(autoComplete).toMutableList()
                    Timber.d("my auto list $autoList")
                    arrayAdapter.submit(autoList)
                }
            }
        }
    }

    private fun observeDate() {
        lifecycle.coroutineScope.launchWhenStarted {
            homeViewModel.selectedDate.collect { date ->
                binding.timeText.text = homeViewModel.shortDate
            }
        }
    }

    @FlowPreview
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        isMapReady = true
        mapboxMap.addOnFlingListener {
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
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            // Map is set up and the style has loaded. Now you can add data or make other map adjustments
            enableLocationComponent(it, requireActivity())
            it.doThings(featureList)
        }
        homeViewModel.updateLocation(mapboxMap.cameraPosition.target)
    }

    private fun Style.doThings(symbolLayerIconFeatureList: List<Feature>) {
        this.run {
            removeLayer(LAYER_ID)
            removeSource(SOURCE_ID)
            addImage(ICON_ID, x)
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

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style, activity: Activity) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(activity)) {
            // Create and customize the LocationComponent's options
            val customLocationComponentOptions = LocationComponentOptions.builder(activity)
                .trackingGesturesManagement(true)
                .accuracyColor(ContextCompat.getColor(activity, R.color.mapboxGreen))
                .build()
            val locationComponentActivationOptions = LocationComponentActivationOptions.builder(
                activity,
                loadedMapStyle
            )
                .locationComponentOptions(customLocationComponentOptions)
                .build()
            // Get an instance of the LocationComponent and then adjust its settings
            mapboxMap.locationComponent.apply {
                // Activate the LocationComponent with options
                activateLocationComponent(locationComponentActivationOptions)
                // Enable to make the LocationComponent visible
                isLocationComponentEnabled = true
                // Set the LocationComponent's camera mode
                cameraMode = CameraMode.TRACKING
                // Set the LocationComponent's render mode
                renderMode = RenderMode.COMPASS
            }
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(activity)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(
            requireContext(),
            R.string.user_location_permission_explanation,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent(mapboxMap.style!!, requireActivity())
        } else {
            Toast.makeText(
                activity,
                R.string.user_location_permission_not_granted,
                Toast.LENGTH_LONG
            ).show()
//            finish()
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
