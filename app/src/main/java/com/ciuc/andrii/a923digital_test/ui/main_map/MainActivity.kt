package com.ciuc.andrii.a923digital_test.ui.main_map

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import com.ciuc.andrii.a923digital_test.R
import com.ciuc.andrii.a923digital_test.ui.search.OnDriveStartedListener
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
import java.lang.ref.WeakReference
import java.security.AccessController


class MainActivity : AppCompatActivity(),
    ActivityCompat.OnRequestPermissionsResultCallback,
    PositioningManager.OnPositionChangedListener {

    private var hasPermissions: Boolean = false
    private var gpsProviderEnabled = false
    private var internetEnabled = false
    private var searchResultList: MutableList<DiscoveryResult>? = null
    private var map: Map? = null
    private var mapFragment: AndroidXMapFragment? = null
    private var currentLocation: GeoCoordinate? = null
    private var listMarkers = arrayListOf<MapMarker>()
    private var mapEngineInitialized: Boolean = false
    private var currentLocationMarker: MapMarker? = null
    private var locationManager: LocationManager? = null


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

        //todo Location
        locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

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
                startPositionManager()

                //createRouteFromCurrentLocation(wayPoint)
                //testing search places functionality
                //searchPlaces()
            }
        }
    }

    private fun setUpUI() {
        //hiding ActionBar
        supportActionBar?.hide()

        hideKeyboardIfUserClicksNotOnEditText(window.decorView.rootView)

        //todo Adding listener to fix selected wayPoints in Fragment
        val fragment =
            SearchFragment.newInstance(
                listener = object : OnDriveStartedListener {
                    override fun onDriveStarted(list: List<RouteWaypoint>) {
                        toast(list.toString())
                        list.forEach { wayPoint ->
                            addWayPointMarkerToMap(wayPoint.originalPosition)
                            createRouteFromCurrentLocation(list)
                        }
                    }
                })


        btnSearch.setOnClickListener {
            if (supportFragmentManager.containsFragment(fragment)) {
                supportFragmentManager.addFragment(fragment)
            } else {
                supportFragmentManager.removeFragment(fragment)
            }
        }

    }

    //todo The function allows us to hide keyboard if user clicks on all views except ExitText
    private fun hideKeyboardIfUserClicksNotOnEditText(view: View) {
        // Set up touch listener for non-text box views to hide keyboard.
        if (view !is EditText) {
            view.setOnTouchListener { _, _ ->
                if (AccessController.getContext() != null)
                    hideKeyboard(this)
                false
            }
        }
        //If a layout container, iterate over children and seed recursion.
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val innerView = view.getChildAt(i)
                hideKeyboardIfUserClicksNotOnEditText(innerView)
            }
        }
    }

    //todo Hiding system's keyboard
    private fun hideKeyboard(context: Context) {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if ((context as Activity).currentFocus != null) {
            try {
                inputManager.hideSoftInputFromWindow(
                    (context as AppCompatActivity).currentFocus!!.windowToken,
                    0
                )
            } catch (ex: NullPointerException) {
                ex.printStackTrace()
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


    private fun startPositionManager() {
        //todo Using Google's GPS services, because they works faster
        val googleLocationDataSource = LocationDataSourceGoogleServices.getInstance()
        if (googleLocationDataSource != null) {
            val pm = PositioningManager.getInstance()
            pm.dataSource = googleLocationDataSource
            pm.addListener(WeakReference(this@MainActivity))
            if (pm.start(PositioningManager.LocationMethod.GPS_NETWORK)) {
                // Position updates started successfully.
                toast("Position updates started successfully.")
            }
        }
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
            supportFragmentManager.findFragmentByTag(SearchFragment.TAG)
        if (fragment != null) {
            supportFragmentManager.removeFragment(fragment)
        } else {
            super.onBackPressed()
        }
    }

    override fun onPositionFixChanged(
        p0: PositioningManager.LocationMethod?,
        p1: PositioningManager.LocationStatus?
    ) {
        // positioning method changed

    }

    override fun onPositionUpdated(
        p0: PositioningManager.LocationMethod?,
        geoPosition: GeoPosition?,
        p2: Boolean
    ) {
        // new position update received
        geoPosition?.coordinate?.let {
            currentLocation = GeoCoordinate(it)
            addCurrentLocationMarkerToMap(it)
        }
    }

    private fun addCurrentLocationMarkerToMap(geoCoordinate: GeoCoordinate) {
        val markerImage = Image()
        try {
            markerImage.setImageResource(R.drawable.ic_marker)
            if (currentLocationMarker != null) {
                map?.removeMapObject(currentLocationMarker as MapMarker)
                listMarkers.remove(currentLocationMarker as MapMarker)
            }

            currentLocationMarker = MapMarker(geoCoordinate, markerImage)
            map?.addMapObject(currentLocationMarker as MapMarker)
            listMarkers.add(currentLocationMarker as MapMarker)

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun addWayPointMarkerToMap(geoCoordinate: GeoCoordinate) {
        val markerImage = Image()
        try {
            markerImage.setImageResource(R.drawable.ic_circle)
            val marker = MapMarker(geoCoordinate, markerImage)
            map?.addMapObject(marker)
            listMarkers.add(marker)

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

    private fun createRouteFromCurrentLocation(list: List<RouteWaypoint>) {
        val router = CoreRouter()

        val routeOptions = RouteOptions()
        routeOptions.transportMode = RouteOptions.TransportMode.CAR
        routeOptions.setHighwaysAllowed(false)
        routeOptions.routeType =
            RouteOptions.Type.FASTEST
        routeOptions.routeCount = 1

        val routePlan = RoutePlan()
        currentLocation?.let {
            routePlan.addWaypoint(
                RouteWaypoint(
                    currentLocation as GeoCoordinate
                )
            )
        }

        list.forEach {
            routePlan.addWaypoint(it)
        }

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