package com.ciuc.andrii.a923digital_test.ui.search

import com.here.android.mpa.routing.RouteWaypoint
import com.nokia.maps.restrouting.Waypoint

interface OnDriveStartedListener {
    fun onDriveStarted(list: List<RouteWaypoint>)
}