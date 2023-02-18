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




class StockTakeActivity : AppCompatActivity() {

    private lateinit var PricingLineCode:String
    private lateinit var textPrintSection: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_take)
        mStorage = Storage(applicationContext) //sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")
        PricingLineCode = mStorage.getDataString("PricingLineCode", "PL001")

        val textUser=findViewById<EditText>(R.id.textUser)
        val textBranch=findViewById<EditText>(R.id.textBranch)

        mStorage= Storage(applicationContext)
        var FloorID: Int= General.getGeneral(applicationContext).FloorID
        var UserId = General.getGeneral(applicationContext).UserCode
        textBranch.setText(""+General.getGeneral(applicationContext).fullLocation);
        textUser.setText(""+UserId);
        textBranch.isEnabled=false;

        findViewById<Button>(R.id.btnDCBox).setOnClickListener{
            General.getGeneral(applicationContext).stockType=100
            General.getGeneral(applicationContext).saveGeneral(applicationContext)
            val intent = Intent (applicationContext, ScanBoxForStockTakeActivity::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.btnDCStand).setOnClickListener{
            General.getGeneral(applicationContext).stockType=101
            General.getGeneral(applicationContext).saveGeneral(applicationContext)
            val intent = Intent (applicationContext, ScanBoxForStockTakeActivity::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.btnDCRack).setOnClickListener{
            General.getGeneral(applicationContext).stockType=102
            General.getGeneral(applicationContext).transactionType=101
            General.getGeneral(applicationContext).saveGeneral(applicationContext)
            val intent = Intent (applicationContext, ScanRackStockTakeActivity::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.btnCount).setOnClickListener{
            General.getGeneral(applicationContext).stockType=100
            General.getGeneral(applicationContext).transactionType=100
            General.getGeneral(applicationContext).saveGeneral(applicationContext)
            val intent = Intent (applicationContext, ScanForCountStockTakeActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnRackCount).setOnClickListener{
            General.getGeneral(applicationContext).stockType=102
            General.getGeneral(applicationContext).transactionType=100
            General.getGeneral(applicationContext).saveGeneral(applicationContext)
            val intent = Intent (applicationContext, ScanRackStockTakeActivity::class.java)
            startActivity(intent)
        }


    }


    var compositeDisposable= CompositeDisposable()
    lateinit var  mStorage:Storage
    var IPAddress =""


}