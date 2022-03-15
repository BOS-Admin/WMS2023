package com.bos.wms.mlkit.app

import Model.LocationCheckModel
import Model.LocationCheckModelBin
import Model.LocationCheckModelShelf
import Remote.APIClient
import Remote.BasicApi
import android.annotation.SuppressLint
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AlertDialog
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
import kotlinx.android.synthetic.main.content_locationcheck.*
import okhttp3.ResponseBody
import java.io.IOException


class LocationCheckActivity : AppCompatActivity() {
    private lateinit var linearLayoutManager: LinearLayoutManager

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var TextChangeEvent:TextWatcher
    private lateinit var ShelfItems:ArrayList<String>
    private lateinit var BinItems:ArrayList<String>
    private lateinit var adp:CustomListAdapter
    private var  DefaultShelfNbOfBins:Int? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locationcheck)
        BinItems= arrayListOf<String>()
        mStorage = Storage(applicationContext) //sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")
        DefaultShelfNbOfBins=(General.getGeneral(application).getSetting(applicationContext,"DefaultShelfNbOfBins")).toInt()
        InitLocationCheck()
        txtShelfBarcode.setShowSoftInputOnFocus(false);
        txtlocationcheckBinBarcode.setShowSoftInputOnFocus(false);

        txtShelfBarcode.requestFocus()
        txtShelfBarcode.setShowSoftInputOnFocus(false);
        txtlocationcheckBinBarcode.setShowSoftInputOnFocus(false);

        TextChangeEvent=object : TextWatcher {
            var UpdatingText:Boolean=false
            @SuppressLint("ResourceAsColor")
            override fun afterTextChanged(s: Editable) {
                if(UpdatingText)
                    return;
                UpdatingText=true;
                var seqVal:Int=0
                var shelfStr:String=txtShelfBarcode.text.toString()
                var binStr:String=txtlocationcheckBinBarcode.text.toString()
                lblError.setText("")
                if(!locationCheckModel.shelfs.filter { !it.DoneLocationCheck && it.locationBarcode==shelfStr}.any()){
                    seqVal=0;
                }
                else if(binStr.isNullOrEmpty()){
                    seqVal=1;
                }
                else if(binStr.lowercase().startsWith("be")) {
                    seqVal = 2;
                }
                else{
                    seqVal=-1
                }
                if(seqVal<=0){
                    Beep()
                    recyclerView.setBackgroundColor(Color.RED)
                }else{
                    recyclerView.setBackgroundResource(0)
                }
                when (seqVal) {
                    -1 -> {
                        txtlocationcheckBinBarcode.setText("")
                        txtlocationcheckBinBarcode.requestFocus()
                    }
                    0 -> {

                        txtShelfBarcode.setText("")
                        txtlocationcheckBinBarcode.setText("")
                        BinItems.clear()
                        lbllocationcheckListView.setText("---Available Shelfs---")
                        val racks = locationCheckModel.shelfs.filter { !it.DoneLocationCheck }.map { it.locationBarcode}.distinct()
                        ShelfItems= racks.toCollection(ArrayList())
                        adp= CustomListAdapter(applicationContext,ShelfItems)
                        recyclerView.setAdapter(adp)
                        if(racks.isEmpty()) {
                            val builder = AlertDialog.Builder(this@LocationCheckActivity)
                            builder.setTitle("Location Check Done!!")
                            builder.setMessage("Location Check Done, Do you want to start a new list?")
                            builder.setPositiveButton("Yes") { dialogInterface, which ->
                                startActivity(getIntent());
                                finish();
                                overridePendingTransition(0, 0);
                            }
                            builder.setNegativeButton("No") { dialogInterface, which ->
                                finish();
                                //overridePendingTransition(0, 0);
                            }
                            builder.setCancelable(false)


                            builder.show()
                        }
                        txtShelfBarcode.requestFocus()
                        CurrentShelf=null
                    }
                    1 -> {
                        CurrentShelf=locationCheckModel.shelfs.filter { !it.DoneLocationCheck && it.locationBarcode==shelfStr}.firstOrNull()
//                        DefaultShelfNbOfBins=CurrentShelf.
                        lbllocationcheckListView.setText("---Scanned Bins---")
                        adp= CustomListAdapter(applicationContext,BinItems)
                        recyclerView.setAdapter(adp)
                        txtlocationcheckBinBarcode.requestFocus()
                    }
                    2 -> {
                        txtlocationcheckBinBarcode.setText("")
                        if (BinItems.contains(binStr)){
                            lblError.setText("Bin already scanned!!")
                        }else if(BinItems.count()>= DefaultShelfNbOfBins!!)
                        {
                            txtlocationcheckBinBarcode.requestFocus()
                        }
                        else{
                            BinItems.add(0,binStr)
                        }
                        adp= CustomListAdapter(applicationContext,BinItems)
                        recyclerView.setAdapter(adp)
                        txtlocationcheckBinBarcode.requestFocus()
                    }
                }
                if(seqVal==2 && DefaultShelfNbOfBins==BinItems.count()){
                    txtlocationcheckBinBarcode.isEnabled=false
                    txtShelfBarcode.isEnabled=false
                    var bins= arrayListOf<LocationCheckModelBin>()
                    BinItems.forEachIndexed { index, element ->
                        bins.add(LocationCheckModelBin(element,index))
                    }
                    var Model=CurrentShelf
                    Model?.bins=bins
                    PostLocationCheckShelf(Model!!)
                }
                UpdatingText=false;
                RefreshLabels()

                txtlocationcheckBinBarcode.setShowSoftInputOnFocus(false);
                txtShelfBarcode.setShowSoftInputOnFocus(false);


            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {

            }
        }
        hideSoftKeyboard(this)
        txtShelfBarcode.addTextChangedListener(TextChangeEvent);
        txtlocationcheckBinBarcode.addTextChangedListener(TextChangeEvent);
    }
    lateinit var api: BasicApi
    var compositeDisposable= CompositeDisposable()
    var CurrentShelf:LocationCheckModelShelf?=null
    lateinit var locationCheckModel: LocationCheckModel
    lateinit var  mStorage:Storage ;  //sp存储
    var IPAddress =""
    private fun Beep(){
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300)
    }
    fun InitLocationCheck() {

        try {
            // TODO: handle loggedInUser authentication
            var FloorID: Int=General.getGeneral(applicationContext).FloorID
            api= APIClient.getInstance(IPAddress ,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.LocationCheck(General.getGeneral(applicationContext).UserID,FloorID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            locationCheckModel=s as LocationCheckModel

                            ShelfItems= locationCheckModel.shelfs.map { x->x.locationBarcode }.toCollection(ArrayList())
                            linearLayoutManager = LinearLayoutManager(this)
                            recyclerView.layoutManager = linearLayoutManager
                            recyclerView.setLayoutManager(LinearLayoutManager(this))
                            adp= CustomListAdapter(this,ShelfItems)
                            recyclerView.setAdapter(adp)
                            RefreshLabels()
                            lblError.setText("")
                            //val stringResponse = s.string()
                        },
                        {t:Throwable?->
                            run {
                                lblError.setText(t?.message)
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            lblError.setText(e?.message)
            throw(IOException("LocationCheck Activity - GetLocationCheckListItems", e))
        }
        finally {
        }
    }
    fun RefreshLabels(){
        lblError.text = ""
       /* var RackCode:Int=LocationCheckModel.rackCode
        var maxRack:Int=LocationCheckListItems.maxOf { it.rackNb }
        lbllocationcheckRack.setText("Rack: "+RackCode.toString())
        lblLocationCheckWave.setText("Wave:"+LocationCheckModel.waveNb.toString())
        var bins:Int=LocationCheckListItems.distinctBy { it.binCode }.count()
        var remainBins:Int=LocationCheckListItems.filter { !it.DoneLocationCheck }.distinctBy { it.binCode }.count()
        lblLocationCheckReminaingBins.setText("Bins:"+ remainBins.toString() + "/"+ bins.toString())
        var items:Int=LocationCheckListItems.distinctBy { it.itemSerialID }.count()
        var remainItems:Int=LocationCheckListItems.filter { !it.DoneLocationCheck }.distinctBy { it.itemSerialID }.count()
        lblLocationCheckReminaingItems.setText("Items:"+ remainItems.toString() + "/"+ items.toString())
        if(remainItems==0){
            lblLocationCheckListView.setText("---LocationCheck Completed---")
            btnLocationCheckDone.visibility= View.VISIBLE
            btnLocationCheckDone.setOnClickListener { finish() }
        }*/
        hideSoftKeyboard(this)
    }
    @SuppressLint("ResourceAsColor")
    fun PostLocationCheckShelf(Model:LocationCheckModelShelf) {

        try {
            // TODO: handle loggedInUser authentication


            api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.LocationCheck(Model)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            var ErrorMsg = ""
                                try {
                                    ErrorMsg = s.string()
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }

                            if (ErrorMsg.isEmpty()) {
                                locationCheckModel.shelfs?.find { it.locationBarcode==txtShelfBarcode.text.toString() }?.DoneLocationCheck = true
                                RefreshLabels()
                                txtShelfBarcode.setText("")
                                txtShelfBarcode.requestFocus()

                                //val stringResponse = s.string()
                                lblError.setText("")
                            } else if (!ErrorMsg.isEmpty()) {
                                lblError.setTextColor(R.color.design_default_color_error)
                                lblError.text = ErrorMsg
                                //(ErrorMsg)
                            }

                            txtlocationcheckBinBarcode.isEnabled=true
                            txtShelfBarcode.isEnabled=true
                        },
                        {t:Throwable?->
                            run {
                                lblError.setText(t?.message)
                                txtlocationcheckBinBarcode.isEnabled=true
                                txtShelfBarcode.isEnabled=true
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            lblError.setText(e?.message)
            txtlocationcheckBinBarcode.isEnabled=true
            txtShelfBarcode.isEnabled=true
            throw(IOException("LocationCheck Activity - PostLocationCheckShelf", e))
        }
        finally {
        }
    }

}