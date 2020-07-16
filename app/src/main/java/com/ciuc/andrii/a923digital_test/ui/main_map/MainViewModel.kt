package com.ciuc.andrii.a923digital_test.ui.main_map

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    fun hasNetworkConnection(application: Application): Boolean {
        val connectivityManager =
          application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}