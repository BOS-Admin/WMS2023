package com.bos.wms.mlkit.app.bosApp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.R
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_packing_reason.*


class PackingReasonActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_packing_reason)
        mStorage = Storage(applicationContext) //sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")


        mStorage= Storage(applicationContext)
        var FloorID: Int= General.getGeneral(applicationContext).FloorID
        var UserId = General.getGeneral(applicationContext).UserID

        btnPackingReasonDone.setOnClickListener{
            val intent = Intent (applicationContext, ScanContainerActivity::class.java)
            startActivity(intent)


        }


    }


    var compositeDisposable= CompositeDisposable()
    lateinit var  mStorage:Storage
    var IPAddress =""


}