package com.bos.wms.mlkit.app.bosApp.Transfer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.R
import com.bos.wms.mlkit.app.bosApp.PrinterSelectionActivity
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.disposables.CompositeDisposable




class TransferActivity : AppCompatActivity() {

    private lateinit var PricingLineCode:String
    private lateinit var textPrintSection: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer)
        val textUser=findViewById<EditText>(R.id.textUser)
        val textBranch=findViewById<EditText>(R.id.textBranch)
        textBranch.setText(General.getGeneral(applicationContext).fullLocation);
        textUser.setText(General.getGeneral(applicationContext).userFullName);
        textBranch.isEnabled=false;
        textPrintSection=findViewById(R.id.textPrintSection)
        textPrintSection.text="Print Section:"+General.getGeneral(applicationContext).printerStationCode
        findViewById<Button>(R.id.btnShipment).setOnClickListener{
            val intent = Intent (applicationContext, ShipmentActivity::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.btnReceiving).setOnClickListener{
            val intent = Intent (applicationContext, ScanTransferReceivingActivity::class.java)
            startActivity(intent)
        }





        findViewById<Button>(R.id.btnChange).setOnClickListener {
            val intent = Intent (applicationContext, PrinterSelectionActivity::class.java)
            startActivity(intent)
        }

    }


    var compositeDisposable= CompositeDisposable()
    lateinit var  mStorage:Storage
    var IPAddress =""


}