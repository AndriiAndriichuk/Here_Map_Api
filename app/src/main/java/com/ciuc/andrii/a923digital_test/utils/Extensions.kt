package com.ciuc.andrii.a923digital_test.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

//todo Context extensions
fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun Activity.openAppSettings(requestCode: Int) {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    val uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    startActivityForResult(intent, requestCode)
}

//todo FragmentManager extensions
fun FragmentManager.containsFragment(fragment: Fragment) =
    findFragmentByTag(fragment.javaClass.simpleName) == null

fun FragmentManager.addFragment(fragment: Fragment) =
    this.beginTransaction()
        .add(android.R.id.content, fragment, fragment.javaClass.simpleName)
        .commit()

fun FragmentManager.removeFragment(fragment: Fragment) =
    this.beginTransaction()
        .remove(fragment).commit()


//todo View extensions
fun View.show()  { visibility = View.VISIBLE }

fun View.hide() { visibility = View.INVISIBLE }

fun View.gone() { visibility = View.GONE }

//todo Int representation of permission's state extensions
fun Int.permissionGranted(): Boolean = this == PackageManager.PERMISSION_GRANTED
fun Int.permissionDenied(): Boolean = this == PackageManager.PERMISSION_DENIED