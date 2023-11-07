package com.bos.wmsapp.CustomViews

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.hc.mixthebluetooth.R

class UPCCardDoneView @JvmOverloads constructor(
    context: Context,
    title:String,
    qty:String,
    items:List<String>,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0

) : LinearLayout(context, attrs, defStyle, defStyleRes) {

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.upc_card_done, this, true)

        orientation = HORIZONTAL
        findViewById<TextView>(R.id.title).text = title
        findViewById<TextView>(R.id.qty).text = qty

        this.setOnClickListener{
            showMessage(title,items.joinToString(separator = "\n"))
        }

    }

    private fun showMessage(title:String ,msg: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton(
                "OK"
            ) { dialog: DialogInterface?, which: Int -> }
            .setIcon(android.R.drawable.ic_dialog_alert) // .setCancelable(false)
            .show()
    }

}