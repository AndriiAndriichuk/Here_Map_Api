package com.ciuc.andrii.a923digital_test.ui.search.listener

import com.here.android.mpa.search.PlaceLink

interface OnDriveStartedListener {
    fun onDriveStarted(list: List<PlaceLink>)
}