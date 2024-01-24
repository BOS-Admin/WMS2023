package com.bos.wms.mlkit.customView;


import android.content.Context
import android.graphics.Paint
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.bos.wms.mlkit.R


class UPCCardDoneView @JvmOverloads constructor(
        context: Context,
        Company:String,
        ItemSerial:String,
        ItemNo:String,
        PrevPrice:String,
        NewPrice:String,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0

) : LinearLayout(context, attrs, defStyle, defStyleRes) {

        init {
                LayoutInflater.from(context)
                        .inflate(R.layout.upc_card_done, this, true)

                orientation = HORIZONTAL
                findViewById<TextView>(R.id.textCompany).text = Company
                findViewById<TextView>(R.id.textPrevLBPPrice).text = PrevPrice
                findViewById<TextView>(R.id.textNewLBPPrice).text = NewPrice
                findViewById<TextView>(R.id.textItemNo).text = ItemNo
                findViewById<TextView>(R.id.textItemSerial).text = ItemSerial


                if(Company=="BRANDS")
                        findViewById<TextView>(R.id.textPrevLBPPrice).paintFlags = Paint.STRIKE_THRU_TEXT_FLAG;


        }

}