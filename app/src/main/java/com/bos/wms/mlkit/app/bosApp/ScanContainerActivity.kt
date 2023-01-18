package com.bos.wms.mlkit.app.bosApp

import Remote.APIClient
import Remote.BasicApi
import android.annotation.SuppressLint
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bos.wms.mlkit.CustomListAdapter
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.General.hideSoftKeyboard
import com.bos.wms.mlkit.R
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_packing.*
import kotlinx.android.synthetic.main.content_item_pricing.*
import kotlinx.android.synthetic.main.content_upc_pricing.*
import kotlinx.android.synthetic.main.content_upc_pricing.lblError
import kotlinx.android.synthetic.main.content_upc_pricing.recyclerView
import retrofit2.HttpException
import java.io.IOException


class ScanContainerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_container)

        mStorage = Storage(applicationContext) //sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")

        var FloorID: Int= General.getGeneral(applicationContext).FloorID
        var UserId = General.getGeneral(applicationContext).UserID
        textBranch.setText(General.getGeneral(applicationContext).LocationString);
        textUser.setText(""+ General.getGeneral(applicationContext).UserID + " " + General.getGeneral(applicationContext).UserName);
        textBranch.isEnabled=false;
        textBranch.isEnabled=false;




    }



    lateinit var api: BasicApi
    var compositeDisposable= CompositeDisposable()
    lateinit var  mStorage:Storage ;  //sp存储
    var IPAddress =""


    var ColorGreen = Color.parseColor("#52ac24")
    var ColorRed = Color.parseColor("#ef2112")
    var ColorWhite = Color.parseColor("#ffffff")

}