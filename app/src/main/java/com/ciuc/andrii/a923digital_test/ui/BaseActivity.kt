package com.ciuc.andrii.a923digital_test.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ciuc.andrii.a923digital_test.R
import com.ciuc.andrii.a923digital_test.utils.*
import kotlinx.android.synthetic.main.dialog_no_internet_gps.*
import java.security.AccessController

open class BaseActivity : AppCompatActivity(),  ActivityCompat.OnRequestPermissionsResultCallback {
    protected var hasPermissions: Boolean = false
    protected var gpsProviderEnabled = false
    protected var internetEnabled = false


    protected fun checkPermissions(permissions: Array<String> = PERMISSIONS): Boolean {
        var result: Int
        val listPermissionsNeededToRequest: ArrayList<String> = ArrayList()
        for (permission in permissions) {
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

    open fun checkInternetAndGps() {}

    protected fun gpsProviderEnabled(): Boolean {
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && packageManager.hasSystemFeature(
            PackageManager.FEATURE_LOCATION_GPS
        )
    }
    
    protected fun showNoInternetOrGpsDialog(
        internetEnabled: Boolean = true,
        gpsEnabled: Boolean = true
    ) {

        val dialog = Dialog(this@BaseActivity)
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

    //todo The function allows us to hide keyboard if user clicks on all views except ExitText
    protected fun hideKeyboardIfUserClicksNotOnEditText(view: View) {
        // Set up touch listener for non-text box views to hide keyboard.
        if (view !is EditText) {
            view.setOnTouchListener { v, _ ->
                if (AccessController.getContext() != null)
                    hideKeyboard(this)
                v.performClick()
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
}