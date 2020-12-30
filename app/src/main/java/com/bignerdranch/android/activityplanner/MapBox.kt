package com.bignerdranch.android.activityplanner

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import kotlinx.coroutines.FlowPreview
import timber.log.Timber

class MapBox: OnMapReadyCallback, PermissionsListener {
    private lateinit var mapboxMap: MapboxMap
    lateinit var context: Context
    lateinit var activity: Activity
    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    var isMapReady = false

    @FlowPreview
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        isMapReady = true
        Timber.d("I have been called")
        mapboxMap.setStyle(Style.MAPBOX_STREETS)
    }

//    private fun Style.doThings(symbolLayerIconFeatureList: List<Feature>) {
//        this.run {
//            removeLayer(HomeFragment.LAYER_ID)
//            removeSource(HomeFragment.SOURCE_ID)
//            addImage(HomeFragment.ICON_ID, x)
//            addSource(
//                GeoJsonSource(
//                    HomeFragment.SOURCE_ID,
//                    FeatureCollection.fromFeatures(symbolLayerIconFeatureList)
//                )
//            )
//            addLayer(
//                SymbolLayer(HomeFragment.LAYER_ID, HomeFragment.SOURCE_ID).withProperties(
//                    PropertyFactory.iconImage(HomeFragment.ICON_ID),
//                    PropertyFactory.iconAllowOverlap(true)
//                )
//            )
//        }
//    }

    @SuppressLint("MissingPermission")
    fun enableLocationComponent(loadedMapStyle: Style, activity: Activity) {
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

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(
            context,
            R.string.user_location_permission_explanation,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent(mapboxMap.style!!, activity)
        } else {
            Toast.makeText(
                context,
                R.string.user_location_permission_not_granted,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}