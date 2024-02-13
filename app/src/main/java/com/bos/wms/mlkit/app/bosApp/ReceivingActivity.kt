package com.bos.wms.mlkit.app.bosApp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.R
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.disposables.CompositeDisposable




class ReceivingActivity : AppCompatActivity() {

    private lateinit var PricingLineCode:String
    private lateinit var textPrintSection: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receiving)
        mStorage = Storage(applicationContext) //sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")
        PricingLineCode = mStorage.getDataString("PricingLineCode", "PL001")

        val textUser=findViewById<EditText>(R.id.textUser)
        val textBranch=findViewById<EditText>(R.id.textBranch)
        mStorage= Storage(applicationContext)
        textBranch.setText(General.getGeneral(applicationContext).fullLocation);
        textUser.setText(General.getGeneral(applicationContext).userFullName);
        textBranch.isEnabled=false;




        findViewById<Button>(R.id.btnPaletteBinReceiving).setOnClickListener{
            General.getGeneral(applicationContext).packType="PaletteBin"
            General.getGeneral(applicationContext).isReceiving=true;
            General.getGeneral(applicationContext).saveGeneral(applicationContext)
            val intent = Intent (applicationContext, ScanBinForCheckingActivity::class.java)
            startActivity(intent)
        }


    }


    var compositeDisposable= CompositeDisposable()
    lateinit var  mStorage:Storage
    var IPAddress =""


}