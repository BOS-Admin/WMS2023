package com.bos.wms.mlkit.app

import Model.PickItemSerialModel
import Model.PickingListItemModel
import Model.PickingListItemModelItem
import Remote.APIClient
import Remote.BasicApi
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
import kotlinx.android.synthetic.main.content_picking.*
import java.io.IOException


class PickingActivity : AppCompatActivity() {
    private lateinit var linearLayoutManager: LinearLayoutManager

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var TextChangeEvent:TextWatcher
    private lateinit var RecyclerViewItems:ArrayList<String>
    private lateinit var adp:CustomListAdapter
    lateinit var  mStorage:Storage ;  //sp存储
    var IPAddress =""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picking)
        mStorage= Storage(applicationContext) //sp存储
         IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")

        InitPicking()
        txtPickingBinBarcode.setShowSoftInputOnFocus(false);
        txtPickingItemSerialBarcode.setShowSoftInputOnFocus(false);
        txtPickingItemBarcode.setShowSoftInputOnFocus(false);
        txtPickingItemBarcode.requestFocus()
        txtPickingBinBarcode.setShowSoftInputOnFocus(false);
        txtPickingItemSerialBarcode.setShowSoftInputOnFocus(false);
        txtPickingItemBarcode.setShowSoftInputOnFocus(false);
        val DefaultShelfNbOfBins:String= General.getGeneral(application).getSetting(applicationContext,"DefaultShelfNbOfBins")
        TextChangeEvent=object : TextWatcher {
            var UpdatingText:Boolean=false
            override fun afterTextChanged(s: Editable) {
                if(UpdatingText)
                    return;
                UpdatingText=true;
                var seqVal:Int=0
                var rackStr:String=txtPickingItemBarcode.text.toString()
                var binStr:String=txtPickingBinBarcode.text.toString()
                var itemStr:String=txtPickingItemSerialBarcode.text.toString()

                if(!PickingListItems.filter { !it.DonePicking && it.rackCode==rackStr}.any()){
                    txtPickingItemBarcode.setText("")
                    txtPickingBinBarcode.setText("")
                    txtPickingItemSerialBarcode.setText("")
                }else seqVal++
                if(!PickingListItems.filter { !it.DonePicking && it.binCode==binStr}.any()){
                    txtPickingBinBarcode.setText("")
                    txtPickingItemSerialBarcode.setText("")
                }else seqVal++
                if(!PickingListItems.filter { !it.DonePicking && it.itemSerial==itemStr}.any()){
                    txtPickingItemSerialBarcode.setText("")
                }else seqVal++

                when (seqVal) {
                    0 -> {
                        lblPickingListView.setText("---Available Racks---")
                        val racks = PickingListItems.filter { !it.DonePicking }.map { it.rackCode}.distinct()
                        RecyclerViewItems= racks.toCollection(ArrayList())
                        adp= CustomListAdapter(applicationContext,RecyclerViewItems)
                        recyclerView.setAdapter(adp)
                        txtPickingItemBarcode.requestFocus()

                    }
                    1 -> {
                        lblPickingListView.setText("---Available Bins---")
                        val bins = PickingListItems.filter { !it.DonePicking }.filter {it.rackCode.equals(rackStr)}.map { it.binCode}.distinct()
                        RecyclerViewItems= bins.toCollection(ArrayList())
                        adp= CustomListAdapter(applicationContext,RecyclerViewItems)
                        recyclerView.setAdapter(adp)
                        txtPickingBinBarcode.requestFocus()
                    }
                    2 -> {
                        lblPickingListView.setText("---Available Items---")

                        val items = PickingListItems.filter { !it.DonePicking }.filter {it.binCode.equals(binStr)}.map { it.itemSerial}.distinct()
                        RecyclerViewItems= items.toCollection(ArrayList())
                        adp= CustomListAdapter(applicationContext,RecyclerViewItems)
                        recyclerView.setAdapter(adp)
                        txtPickingItemSerialBarcode.requestFocus()
                    }
                    3 -> { // Note the block

                        val item: PickingListItemModelItem? =PickingListItems.find { it.itemSerial==itemStr }
                        val Model= PickItemSerialModel(PickingModel.pickingWaveID,item?.itemSerialID,PickingModel.userID,item?.binID)

                        PickItemSerial(Model)
                        //Pick Item from webservice
                    }
                }
                txtPickingBinBarcode.setShowSoftInputOnFocus(false);
                txtPickingItemSerialBarcode.setShowSoftInputOnFocus(false);
                txtPickingItemBarcode.setShowSoftInputOnFocus(false);
                UpdatingText=false;
                RefreshLabels()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {

            }
        }
        hideSoftKeyboard(this)
        txtPickingItemBarcode.addTextChangedListener(TextChangeEvent);
        txtPickingBinBarcode.addTextChangedListener(TextChangeEvent);
        txtPickingItemSerialBarcode.addTextChangedListener(TextChangeEvent);
    }
    lateinit var api: BasicApi
    var compositeDisposable= CompositeDisposable()
    lateinit var PickingListItems:ArrayList<PickingListItemModelItem>
    lateinit var PickingModel: PickingListItemModel
    fun InitPicking() {

        try {
            // TODO: handle loggedInUser authentication


            api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.GetPickingListItems(General.getGeneral(applicationContext).UserID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            PickingModel=s as PickingListItemModel
                            PickingListItems= PickingModel.items.toCollection(ArrayList())
                            val racks = PickingListItems.filter { !it.DonePicking }.map { it.rackCode}.distinct()
                            RecyclerViewItems= racks.toCollection(ArrayList())
                            linearLayoutManager = LinearLayoutManager(this)
                            recyclerView.layoutManager = linearLayoutManager
                            recyclerView.setLayoutManager(LinearLayoutManager(this))
                            adp= CustomListAdapter(this,RecyclerViewItems)
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
            throw(IOException("Picking Activity - GetPickingListItems", e))
        }
        finally {
        }
    }
    fun RefreshLabels(){
        lblError.text = ""
        var minRack:Int=PickingListItems.minOf { it.rackNb }
        var maxRack:Int=PickingListItems.maxOf { it.rackNb }
        lblPickingRackRange.setText("Racks:"+minRack.toString() +"-->"+ maxRack.toString())
        lblPickingWave.setText("Wave:"+PickingModel.waveNb.toString())
        var bins:Int=PickingListItems.distinctBy { it.binCode }.count()
        var remainBins:Int=PickingListItems.filter { !it.DonePicking }.distinctBy { it.binCode }.count()
        lblPickingReminaingBins.setText("Bins:"+ remainBins.toString() + "/"+ bins.toString())
        var items:Int=PickingListItems.distinctBy { it.itemSerialID }.count()
        var remainItems:Int=PickingListItems.filter { !it.DonePicking }.distinctBy { it.itemSerialID }.count()
        lblPickingReminaingItems.setText("Items:"+ remainItems.toString() + "/"+ items.toString())
        if(remainItems==0){
            lblPickingListView.setText("---Picking Completed---")
            btnPickingDone.visibility= View.VISIBLE
            btnPickingDone.setOnClickListener { finish() }
        }
        hideSoftKeyboard(this)
    }
    fun PickItemSerial(Model:PickItemSerialModel) {

        try {
            // TODO: handle loggedInUser authentication


            api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.PickItemSerial(Model)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            PickingListItems?.find { it.itemSerialID==Model.ItemSerialID }?.DonePicking = true

                            txtPickingItemSerialBarcode.setText("")
                            //val stringResponse = s.string()
                            lblError.setText("")

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
            throw(IOException("Picking Activity - PickItemSerial", e))
        }
        finally {
        }
    }

}