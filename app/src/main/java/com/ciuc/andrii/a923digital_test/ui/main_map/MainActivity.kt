package com.ciuc.andrii.a923digital_test.ui.main_map

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PointF
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import com.ciuc.andrii.a923digital_test.R
import com.ciuc.andrii.a923digital_test.ui.BaseActivity
import com.ciuc.andrii.a923digital_test.ui.search.OnDriveStartedListener
import com.ciuc.andrii.a923digital_test.ui.search.SearchFragment
import com.ciuc.andrii.a923digital_test.ui.search.WayPointInfoFragment
import com.ciuc.andrii.a923digital_test.utils.*
import com.here.android.mpa.common.*
import com.here.android.mpa.guidance.NavigationManager
import com.here.android.mpa.guidance.NavigationManager.NewInstructionEventListener
import com.here.android.mpa.guidance.VoiceCatalog
import com.here.android.mpa.mapping.*
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.routing.*
import com.here.android.mpa.routing.Route.TrafficPenaltyMode
import com.here.android.mpa.search.DiscoveryResult
import com.here.android.mpa.search.PlaceLink
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference


class MainActivity : BaseActivity(),
    PositioningManager.OnPositionChangedListener {

    private val NAVIGATION_SPEED: Long = 50
    private var map: Map? = null
    private var mapFragment: AndroidXMapFragment? = null
    private var currentLocation: GeoCoordinate? = null
    private var listMarkers = arrayListOf<MapMarker>()
    private var mapEngineInitialized: Boolean = false
    private var currentLocationMarker: MapMarker? = null
    private var locationManager: LocationManager? = null
    private var currentRoute: Route? = null
    private var onNavigationMode = false

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProviders.of(this).get(MainViewModel::class.java)
    }

    //todo Adding listener to fix selected wayPoints in Fragment
    private val onDriveStartedListener = object : OnDriveStartedListener {
        override fun onDriveStarted(list: List<PlaceLink>) {
            clearMapExceptLocation()
            list.forEach { wayPoint ->
                addWayPointMarkerToMap(wayPoint)
                createRouteFromCurrentLocation(list)
                btnSimulate.show()
            }
        }
    }

    val listener = object :
        MapGesture.OnGestureListener {
        override fun onLongPressRelease() {}

        override fun onRotateEvent(p0: Float): Boolean {
            return false
        }

        override fun onMultiFingerManipulationStart() {}

        override fun onPinchLocked() {}

        override fun onPinchZoomEvent(p0: Float, p1: PointF): Boolean {
            return false
        }

        override fun onTapEvent(p0: PointF): Boolean {
            return false
        }

        override fun onPanStart() {}

        override fun onMultiFingerManipulationEnd() {}

        override fun onDoubleTapEvent(p0: PointF): Boolean {
            return false
        }

        override fun onPanEnd() {}

        override fun onTiltEvent(p0: Float): Boolean {
            return false
        }

        override fun onMapObjectsSelected(p0: MutableList<ViewObject>): Boolean {
            Log.d("34343", p0.toString())
            val mapObjects = p0.filter { it is MapMarker }
            val mapMarker = mapObjects[0] as MapObject

            if (mapMarker is MapMarker) {
                val fragment =
                    WayPointInfoFragment.newInstance(stopAddress = mapMarker.title.toString())
                if (supportFragmentManager.containsFragment(fragment).not()) {
                    supportFragmentManager.addFragment(fragment)
                } else {
                    supportFragmentManager.removeFragment(fragment)
                }
            }
            return false
        }

        override fun onRotateLocked() {}

        override fun onLongPressEvent(p0: PointF): Boolean {
            return false
        }

        override fun onTwoFingerTapEvent(p0: PointF): Boolean {
            return false
        }

    }

    // declare the listeners
// add application specific logic in each of the callbacks.

    // declare the listeners
    // add application specific logic in each of the callbacks.
    private val instructListener: NewInstructionEventListener =
        object : NewInstructionEventListener() {
            override fun onNewInstructionEvent() {
                // Interpret and present the Maneuver object as it contains
                // turn by turn navigation instructions for the user.
                navigationManager!!.nextManeuver
            }
        }

    private val positionListener: NavigationManager.PositionListener =
        object : NavigationManager.PositionListener() {
            override fun onPositionUpdated(loc: GeoPosition) {
                // the position we get in this callback can be used
                // to reposition the map and change orientation.
                loc.coordinate
                loc.heading
                loc.speed

                // also remaining time and distance can be
                // fetched from navigation manager
                navigationManager!!.getTta(TrafficPenaltyMode.DISABLED, true)
                navigationManager!!.destinationDistance
            }
        }

    private var searchFragment = SearchFragment.newInstance(listener = onDriveStartedListener)

    // Declare the navigationManager member variable
    private var navigationManager: NavigationManager? = null
    // Get the NavigationManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeMap()
        //initializeNavigation()
        setUpUI()

        hasPermissions = checkPermissions()

        checkInternetAndGps()

        locale = getCurrentLocale()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        hasPermissions = (requestCode == PERMISSIONS_REQUEST && resultCode == Activity.RESULT_OK)
    }


    override fun onBackPressed() {
        searchFragment.let { fragment ->
            if (supportFragmentManager.containsFragment(fragment).not()) {
                supportFragmentManager.removeFragment(fragment)
            } else {
                super.onBackPressed()
            }
        }
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

                    mapFragment.mapGesture?.addOnGestureListener(listener, 0, false)
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
                initializeNavigation()
                //createRouteFromCurrentLocation(wayPoint)
                //testing search places functionality
                //searchPlaces()
            }
        }
    }

    private fun initializeNavigation() {
        navigationManager = NavigationManager.getInstance()

        // start listening for navigation events
        navigationManager?.addNewInstructionEventListener(
            WeakReference(instructListener)
        )

        // start listening for position events
        navigationManager?.addPositionListener(
            WeakReference(positionListener)
        )

        val voiceCatalog = VoiceCatalog.getInstance()
        voiceCatalog.downloadCatalog { errorCode -> }

        // Get the list of voice packages from the voice catalog list
        val voicePackages =
            VoiceCatalog.getInstance().catalogList

        var id: Long = -1

        // select
        for (vPackage in voicePackages) {
            if (vPackage.marcCode.compareTo("eng", ignoreCase = true) == 0) {
                if (vPackage.isTts) {
                    id = vPackage.id
                    break
                }
            }
        }


        if (!voiceCatalog.isLocalVoiceSkin(id)) {
            voiceCatalog.downloadVoice(id) { }
        }

        // obtain VoiceGuidanceOptions object
        val voiceGuidanceOptions = navigationManager?.voiceGuidanceOptions

        // set the voice skin for use by navigation manager
        voiceCatalog.getLocalVoiceSkin(id)?.let { voiceGuidanceOptions?.setVoiceSkin(it) }

    }

    private fun startNavigation(
        route: Route,
        speedMph: Long = NAVIGATION_SPEED
    ) {
        // if user wants to start simulation,
        // submit calculated route and a simulation speed in meters per second
        navigationManager?.simulate(route, speedMph)

    }


    private fun setUpUI() {
        //hiding ActionBar
        supportActionBar?.hide()

        btnSimulate.gone()

        hideKeyboardIfUserClicksNotOnEditText(window.decorView.rootView)

        btnSearch.setOnClickListener {
            searchFragment.let { fragment ->
                if (supportFragmentManager.containsFragment(fragment).not()) {
                    btnSimulate.gone()
                    supportFragmentManager.addFragment(fragment)
                } else {
                    supportFragmentManager.removeFragment(fragment)
                    searchFragment = SearchFragment.newInstance(listener = onDriveStartedListener)
                }
            }
        }

        btnSimulate.setOnClickListener {
            btnSimulate.gone()
            btnEndSimulation.show()
            btnPauseSimulation.show()
            currentRoute?.let {
                addCurrentLocationMarkerToMap(currentLocation!!, R.drawable.ic_compass)
                onNavigationMode = true
                startNavigation(currentRoute!!)
            }
        }

        btnEndSimulation.setOnClickListener {
            btnEndSimulation.gone()
            btnPauseSimulation.gone()
            clearMapExceptLocation()
            // abort navigation
            navigationManager?.stop()
            onNavigationMode = false
        }

        btnPauseSimulation.setOnClickListener {
            if (btnPauseSimulation.text == resources.getString(R.string.pause)) {
                btnPauseSimulation.text = resources.getString(R.string.resume)
                navigationManager?.pause()
            } else {
                btnPauseSimulation.text = resources.getString(R.string.pause)
                navigationManager?.resume()
            }
        }
    }

    override fun checkInternetAndGps() {
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
                Log.d("4333", "Position updates started successfully.")
            }
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
            if (onNavigationMode)
                addCurrentLocationMarkerToMap(it, R.drawable.ic_compass)
            else
                addCurrentLocationMarkerToMap(it)
        }
    }

    private fun addCurrentLocationMarkerToMap(
        geoCoordinate: GeoCoordinate,
        imageResource: Int = R.drawable.ic_marker
    ) {
        val markerImage = Image()
        try {
            markerImage.setImageResource(imageResource)
            if (currentLocationMarker != null) {
                map?.removeMapObject(currentLocationMarker as MapMarker)
                listMarkers.remove(currentLocationMarker as MapMarker)
            }

            currentLocationMarker = MapMarker(geoCoordinate, markerImage)
            currentLocationMarker!!.title = getString(R.string.my_location)
            map?.addMapObject(currentLocationMarker as MapMarker)
            listMarkers.add(currentLocationMarker as MapMarker)

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun addWayPointMarkerToMap(placeLink: PlaceLink) {
        val markerImage = Image()
        try {
            markerImage.setImageResource(R.drawable.ic_circle)
            val marker = MapMarker(placeLink.position!!, markerImage)
            marker.title = placeLink.title

            map?.addMapObject(marker)
            listMarkers.add(marker)

        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun clearMapExceptLocation() {
        try {
            map?.removeAllMapObjects()
            currentLocation?.let {
                addCurrentLocationMarkerToMap(it)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun createRouteFromCurrentLocation(list: List<PlaceLink>) {
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
            routePlan.addWaypoint(it.toRouteWayPoint())
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
                currentRoute = mapRoute.route
            } else {
                Toast.makeText(this@MainActivity, "Can't create route $error", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

}