package com.ciuc.andrii.a923digital_test.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.transition.Slide
import com.ciuc.andrii.a923digital_test.R
import com.ciuc.andrii.a923digital_test.ui.search.custom_view.waypoint_view.WayPointView
import com.ciuc.andrii.a923digital_test.utils.gone
import com.ciuc.andrii.a923digital_test.utils.removeFragment
import com.ciuc.andrii.a923digital_test.utils.show
import com.ciuc.andrii.a923digital_test.utils.toast
import com.here.android.mpa.common.GeoCoordinate
import com.here.android.mpa.routing.RouteWaypoint
import com.here.android.mpa.search.*
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.android.synthetic.main.layout_stop_item.view.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


private const val ARG_PARAM1 = "stopTitle"
private const val ARG_PARAM2 = "stopAddress"

class WayPointInfoFragment() : Fragment() {
    // TODO: Rename and change types of parameters
    private var stopTitle: String? = null
    private var stopAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            stopTitle = it.getString(ARG_PARAM1)
            stopAddress = it.getString(ARG_PARAM2)
        }

        enterTransition = Slide()
        exitTransition = Slide()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_waypoint_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<LinearLayout>(R.id.wayPointInfo).textStop.setText(if (stopTitle?.isEmpty()!!) getString(R.string.stop) else stopTitle)

        view.findViewById<LinearLayout>(R.id.wayPointInfo).editWaypoint.setText(stopAddress.toString())

        view.findViewById<LinearLayout>(R.id.wayPointInfo).editWaypoint.apply {
            isEnabled = false
        }

        view.findViewById<ConstraintLayout>(R.id.fragment_waypoint_info).setOnClickListener {
            closeThisFragment()
        }
    }

    private fun closeThisFragment() {
        activity?.supportFragmentManager?.let { fragmentManager ->
            if (fragmentManager.findFragmentByTag(this@WayPointInfoFragment.javaClass.simpleName) != null)
                fragmentManager.removeFragment(this@WayPointInfoFragment)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(
            stopTitle: String = "",
            stopAddress: String = ""
        ) =
            WayPointInfoFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, stopTitle)
                    putString(ARG_PARAM2, stopAddress)
                }
            }
    }

}
