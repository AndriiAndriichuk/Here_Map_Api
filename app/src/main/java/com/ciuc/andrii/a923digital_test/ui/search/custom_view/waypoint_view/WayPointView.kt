package com.ciuc.andrii.a923digital_test.ui.search.custom_view.waypoint_view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import com.ciuc.andrii.a923digital_test.R
import kotlinx.android.synthetic.main.layout_stop_item.view.*

class WayPointView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var wayPoints = arrayOf<String>()

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_stop_item, this, true)

        initWayPoint()
    }

    fun setAutoCompleteList(list: Array<String>) {
        wayPoints = list
        initWayPoint()
    }

    private fun initWayPoint() {
        val adapter = ArrayAdapter(
            this.context, android.R.layout.simple_dropdown_item_1line, wayPoints
        )

        editWaypoint.setAdapter(adapter)

        editWaypoint.threshold = 3
    }


}
