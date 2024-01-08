package com.hc.mixthebluetooth.customView

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.hc.mixthebluetooth.R

class StepFailView @JvmOverloads constructor(
    context: Context,
    title:String,
    description :String,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes) {

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.step_fail, this, true)

        orientation = HORIZONTAL
        findViewById<TextView>(R.id.stepTitle).text = title
        findViewById<TextView>(R.id.stepDescription).text = description

    }
}