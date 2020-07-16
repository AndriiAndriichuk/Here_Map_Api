package com.ciuc.andrii.a923digital_test.ui.main_map

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import com.ciuc.andrii.a923digital_test.R
import com.ciuc.andrii.a923digital_test.ui.search.SearchFragment
import com.ciuc.andrii.a923digital_test.utils.*
import com.here.android.mpa.common.*
import com.here.android.mpa.mapping.AndroidXMapFragment
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.mapping.MapMarker
import com.here.android.mpa.mapping.MapRoute
import com.here.android.mpa.routing.*
import com.here.android.mpa.search.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_no_internet_gps.*
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity(), LocationListener,
    ActivityCompat.OnRequestPermissionsResultCallback {

    private var hasPermissions: Boolean = false
    private var gpsProviderEnabled = false
    private var internetEnabled = false
    private var searchResultList: MutableList<DiscoveryResult>? = null
    private var map: Map? = null
    private var mapFragment: AndroidXMapFragment? = null
    private var currentLocation: GeoCoordinate? = null
    private var listMarkers = arrayListOf<MapMarker>()
    private var mapEngineInitialized: Boolean = false


    private val mainViewModel: MainViewModel by lazy {
        ViewModelProviders.of(this).get(MainViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeMap()
        setUpUI()

        hasPermissions = checkPermissions()

        checkInternetAndGps()

    }

    private fun initializeMap() {
        mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapfragment) as AndroidXMapFragment

        MapSettings.setDiskCacheRootPath(filesDir.absolutePath + File.separator + ".here-maps")

        mapFragment?.let { mapFragment ->
            mapFragment.init { error ->
                if (error == OnEngineInitListener.Error.NONE) {
                    map = mapFragment.map
                    map?.setCenter(
                        currentLocation ?: GeoCoordinate(48.394580, 25.952817),
                        Map.Animation.NONE
                    )
                    map?.zoomLevel = (map?.maxZoomLevel as Double + map?.minZoomLevel!!) / 2
                } else {
                    toast(getString(R.string.cannot_initialize_map))
                }
            }

        }
        MapEngine.getInstance().init(
            ApplicationContext(this@MainActivity)
        ) { error ->
            if (error == OnEngineInitListener.Error.NONE) {
                mapEngineInitialized = true
                currentLocation?.let {
                    val wayPoint = RouteWaypoint(
                        GeoCoordinate(
                            48.394580, 25.952817
                        )
                    )
                    addMarkerToMap(wayPoint.originalPosition)
                }

                //testing search places functionality
                //searchPlaces()
            }
        }
    }

    private fun setUpUI() {
        supportActionBar?.hide()

        val fragment = SearchFragment.newInstance()
        btnSearch.setOnClickListener {
            if (supportFragmentManager.containsFragment(fragment)) {
                supportFragmentManager.addFragment(fragment)
            } else {
                supportFragmentManager.removeFragment(fragment)
            }
        }

    }

    private fun checkPermissions(): Boolean {
        var result: Int
        val listPermissionsNeededToRequest: ArrayList<String> = ArrayList()
        for (permission in PERMISSIONS) {
            result = ContextCompat.checkSelfPermission(this, permission)
            if (result.permissionDenied()) {
                listPermissionsNeededToRequest.add(permission)
            }
        }
        if (listPermissionsNeededToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeededToRequest.toArray(
                    arrayOfNulls<String>(
                        listPermissionsNeededToRequest.size
                    )
                ),
                PERMISSIONS_REQUEST
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST -> {
                if (grantResults.contains(PackageManager.PERMISSION_DENIED)) {
                    toast(getString(R.string.please_grand_permissions))
                    openAppSettings(requestCode)
                }
                return
            }
        }
    }

    private fun checkInternetAndGps() {
        gpsProviderEnabled = gpsProviderEnabled()
        internetEnabled = mainViewModel.hasNetworkConnection(application)

        if (gpsProviderEnabled.not() || internetEnabled.not())
            showNoInternetOrGpsDialog(internetEnabled, gpsProviderEnabled)
    }

    private fun gpsProviderEnabled(): Boolean {
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && packageManager.hasSystemFeature(
            PackageManager.FEATURE_LOCATION_GPS
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PERMISSIONS_REQUEST && resultCode == Activity.RESULT_OK) {
            hasPermissions = true
        }
    }

    private fun showNoInternetOrGpsDialog(
        internetEnabled: Boolean = true,
        gpsEnabled: Boolean = true
    ) {

        val dialog = Dialog(this@MainActivity)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_no_internet_gps)
        dialog.btnRetry.setOnClickListener {
            checkInternetAndGps()
            dialog.cancel()
        }
        if (internetEnabled.not()) dialog.titleInternet.show()
        if (gpsEnabled.not()) dialog.titleGps.show()
        dialog.show()
    }


    override fun onBackPressed() {
        val fragment =
            SearchFragment.newInstance()
        if (supportFragmentManager.findFragmentByTag(fragment.javaClass.simpleName) != null) {
            supportFragmentManager.beginTransaction()
                .remove(fragment).commit()
        } else {
            super.onBackPressed()
        }
    }


    override fun onLocationChanged(location: Location) {
        currentLocation = GeoCoordinate(location.latitude, location.longitude, location.altitude)
        currentLocation?.let { addMarkerToMap(it) }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }

    override fun onProviderEnabled(provider: String?) {

    }

    override fun onProviderDisabled(provider: String?) {

    }

    private fun addMarkerToMap(geoCoordinate: GeoCoordinate, iconID: Int = R.drawable.ic_marker) {
        val markerImage = Image()
        try {
            markerImage.setImageResource(iconID)

            val marker = MapMarker(geoCoordinate, markerImage)

            marker.isDraggable = true
            marker.title = "MapMarker id: ${listMarkers.size + 1}"

            marker.let {
                map?.addMapObject(it)
                listMarkers.add(it)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun createRouteFromCurrentLocation(wayPoint: RouteWaypoint) {
        val router = CoreRouter()

        val routeOptions = RouteOptions()
        routeOptions.transportMode = RouteOptions.TransportMode.CAR
        routeOptions.setHighwaysAllowed(false)
        routeOptions.routeType =
            RouteOptions.Type.FASTEST
        routeOptions.routeCount = 1

        val routePlan = RoutePlan()
        routePlan.addWaypoint(
            RouteWaypoint(
                currentLocation as GeoCoordinate
            )
        )
        routePlan.addWaypoint(
            wayPoint
        )

        // Create the RouteOptions and set its transport mode & routing type

        routePlan.routeOptions = routeOptions


        // Calculate the route
        router.calculateRoute(routePlan, RouteListener())
    }

    inner class RouteListener : CoreRouter.Listener {
        // Method defined in Listener
        override fun onProgress(percentage: Int) {
            // Display a message indicating calculation progress
        }

        // Method defined in Listener
        override fun onCalculateRouteFinished(
            routeResult: List<RouteResult>,
            error: RoutingError
        ) {
            // If the route was calculated successfully
            if (error === RoutingError.NONE) {
                // Render the route on the map
                val mapRoute = MapRoute(routeResult[0].route)
                map?.addMapObject(mapRoute)
                mapRoute.renderType = MapRoute.RenderType.PRIMARY
                val gbb: GeoBoundingBox? = routeResult[0].route.boundingBox
                map?.zoomTo(
                    gbb!!, Map.Animation.NONE,
                    Map.MOVE_PRESERVE_ORIENTATION
                )
            } else {
                Toast.makeText(this@MainActivity, "Can't create route $error", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private val placeResultListener: ResultListener<Place> =
        ResultListener<Place> { place, errorCode ->
            if (errorCode === ErrorCode.NONE) {
                val geoCoordinate = place?.location!!.coordinate
                //  m_placeLocation.setText(geoCoordinate.toString())
                Log.d("fbsdbs", place.name.toString())

            } else {
                Log.d("fbsdbs", place?.name.toString())
                Toast.makeText(
                    applicationContext,
                    "ERROR:Place request returns error: $errorCode", Toast.LENGTH_SHORT
                )
                    .show()
            }
        }

    private fun searchPlaces() {
        val result = searchResultList?.get(0)
        if (result?.resultType == DiscoveryResult.ResultType.PLACE) {
            /* Fire the PlaceRequest */
            val placeLink = result as PlaceLink
            val placeRequest = placeLink.detailsRequest
            placeRequest?.execute(placeResultListener)
        }
    }
}