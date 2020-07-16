package com.ciuc.andrii.a923digital_test.ui.search

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.ViewCompat
import androidx.transition.Slide
import com.ciuc.andrii.a923digital_test.R
import com.ciuc.andrii.a923digital_test.ui.search.custom_view.waypoint_view.WayPointView
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.android.synthetic.main.layout_stop_item.view.*


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SearchFragment : Fragment() {
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
        view.findViewById<LinearLayout>(R.id.layoutCloseSearchFragment).setOnClickListener {
            closeThisFragment()
        }

        view.findViewById<Button>(R.id.btnAddStop).setOnClickListener {
            val inflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val rowView = WayPointView(context!!)
            rowView.textStop.text = "Stop ${linearSearchLabels.childCount}"
            val lp = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            rowView.id = ViewCompat.generateViewId()

            linearSearchLabels.addView(rowView, lp)
        }

        view.findViewById<Button>(R.id.btnDrive).setOnClickListener {
            closeThisFragment()
        }

        view.findViewById<WayPointView>(R.id.wayPointStopOne).textStop.setOnClickListener {

            view.findViewById<WayPointView>(R.id.wayPointStopOne).setAutoCompleteList(
                arrayOf(
                    "Мурзик",
                    "Рыжик",
                    "Барсик",
                    "Борис",
                    "Мурзилка",
                    "Мурка"
                )
            )

        }

    }

    private fun closeThisFragment() {
        activity?.supportFragmentManager?.let { fragmentManager ->
            if (fragmentManager.findFragmentByTag(this@SearchFragment.javaClass.simpleName) != null)
                fragmentManager.beginTransaction().remove(this@SearchFragment)
                    .commit()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String = "", param2: String = "") =
            SearchFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }


}
