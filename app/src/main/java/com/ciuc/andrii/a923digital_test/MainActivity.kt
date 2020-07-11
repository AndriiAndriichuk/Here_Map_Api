package com.ciuc.andrii.a923digital_test

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.here.android.mpa.common.GeoCoordinate
import com.here.android.mpa.common.Image
import com.here.android.mpa.common.MapSettings
import com.here.android.mpa.common.OnEngineInitListener
import com.here.android.mpa.mapping.AndroidXMapFragment
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.mapping.MapMarker
import java.io.File
import java.io.IOException


class MainActivity : FragmentActivity(), LocationListener {

    private var map: Map? = null
    private var mapFragment: AndroidXMapFragment? = null
    var currentLocation: GeoCoordinate? = null
    var listMarkers = arrayListOf<MapMarker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initialize()
    }

    private fun initialize() {


        // Search for the map fragment to finish setup by calling init().
        mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapfragment) as AndroidXMapFragment

        // Set up disk map cache path for this application
        // Use path under your application folder for storing the disk cache
        MapSettings.setDiskCacheRootPath(filesDir.absolutePath + File.separator + ".here-maps")


        //todo Location
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager


        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
        ) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500,
                1f,
                this@MainActivity
            )
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                500,
                1f,
                this@MainActivity
            )
        } else {
            Toast.makeText(this, "error", Toast.LENGTH_SHORT).show()


        }




        mapFragment!!.init { error ->
            if (error == OnEngineInitListener.Error.NONE) {
                // retrieve a reference of the map from the map fragment
                map = mapFragment!!.map
                // Set the map center to the Vancouver region (no animation)
                map?.setCenter(
                    currentLocation ?: GeoCoordinate(52.53032, 13.37409),
                    Map.Animation.NONE
                )
                // Set the zoom level to the average between min and max
                map?.zoomLevel = (map?.maxZoomLevel as Double + map?.minZoomLevel!!) / 2
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "ERROR: Cannot initialize Map Fragment",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    }

    override fun onLocationChanged(location: Location) {
        currentLocation = GeoCoordinate(location.latitude, location.longitude, location.altitude)
       addMarkerToMap(currentLocation!!)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }

    override fun onProviderEnabled(provider: String?) {

    }

    override fun onProviderDisabled(provider: String?) {

    }

    fun addMarkerToMap(geoCoordinate: GeoCoordinate) {
        val marker_img = Image()
        try {
            marker_img.setImageResource(R.drawable.ic_marker)


            val marker = map?.center?.let { MapMarker(it, marker_img) }

            marker?.isDraggable = true
            marker?.title = "MapMarker id: ${listMarkers.size + 1}"
            // add a MapMarker to current active map.
            // add a MapMarker to current active map.
            marker?.let {
                map?.addMapObject(it as MapMarker)

                listMarkers.add(it as MapMarker)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }


}