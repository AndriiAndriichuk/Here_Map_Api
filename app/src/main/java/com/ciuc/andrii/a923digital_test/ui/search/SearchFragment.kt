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
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.android.synthetic.main.layout_stop_item.view.*


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SearchFragment(var onDriveStartedListener: OnDriveStartedListener) : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        enterTransition = Slide()
        exitTransition = Slide()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addWayPointViewToLayout()

        view.findViewById<LinearLayout>(R.id.layoutCloseSearchFragment).setOnClickListener {
            closeThisFragment()
        }

        view.findViewById<Button>(R.id.btnAddStop).setOnClickListener {
            addWayPointViewToLayout()
        }

        view.findViewById<Button>(R.id.btnDrive).setOnClickListener {
            val notEmptyWayPoints =
                linearSearchLabels.children.filter { it.editWaypoint.text.isNotEmpty() }.toList()
            if (notEmptyWayPoints.isNotEmpty()) {
                onDriveStartedListener.onDriveStarted(
                    listOf(
                        RouteWaypoint(
                            GeoCoordinate(
                                48.394580, 25.952817
                            )
                        ), RouteWaypoint(GeoCoordinate(48.347266, 25.960217))
                    )
                )

                closeThisFragment()
            } else {
                context?.toast("Please, input at least one waypoint.")
            }
        }


    }

    private fun addWayPointViewToLayout() {
        val lastWayPoint = linearSearchLabels.getChildAt(linearSearchLabels.childCount - 1)
        if (linearSearchLabels.childCount == 0 || (linearSearchLabels.childCount >= 1 && lastWayPoint.editWaypoint.text.isNotEmpty())) {
            val rowView = WayPointView(context!!)
            rowView.textStop.text = "Stop ${linearSearchLabels.childCount + 1}"

            rowView.setAutoCompleteList(
                arrayOf(
                    "Мурзик",
                    "Рыжик",
                    "Барсик",
                    "Борис",
                    "Мурзилка",
                    "Мурка"
                )
            )

            rowView.editWaypoint.addTextChangedListener(
                object :
                    TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        if (s?.isNotEmpty()!!) {
                            rowView.btnClearWaypoint.show()
                        } else {
                            rowView.btnClearWaypoint.gone()
                        }
                    }

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                        Log.d("32324", "1 $start $after $count")
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        Log.d("32324", "$start $before $count")
                    }
                })

            rowView.btnClearWaypoint.setOnClickListener {
                rowView.editWaypoint.setText("")
                it.gone()
            }
            val lp = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            rowView.id = ViewCompat.generateViewId()

            linearSearchLabels.addView(rowView, lp)
        } else {
            context?.toast("Input previous waypoint to add more.")
        }
    }

    private fun closeThisFragment() {
        activity?.supportFragmentManager?.let { fragmentManager ->
            if (fragmentManager.findFragmentByTag(this@SearchFragment.javaClass.simpleName) != null)
                fragmentManager.removeFragment(this@SearchFragment)
        }
    }

    companion object {
        @JvmStatic
        val TAG = this.javaClass.simpleName
        fun newInstance(
            param1: String = "",
            param2: String = "",
            listener: OnDriveStartedListener
        ) =
            SearchFragment(listener).apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

}
