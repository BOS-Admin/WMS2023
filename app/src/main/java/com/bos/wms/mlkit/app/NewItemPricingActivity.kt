package com.bos.wms.mlkit.app

import Model.Pricing.PricingStandModel
import Model.Pricing.UPCPricingItemModel
import Remote.APIClient
import Remote.APIClient.getInstanceStatic
import Remote.BasicApi
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
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
import kotlinx.android.synthetic.main.activity_new_item_pricing.*
import kotlinx.android.synthetic.main.content_new_item_pricing.*
import kotlinx.android.synthetic.main.content_new_item_pricing.btnUPCPricingDone
import kotlinx.android.synthetic.main.content_new_item_pricing.lblError
import kotlinx.android.synthetic.main.content_new_item_pricing.recyclerView
import kotlinx.android.synthetic.main.content_new_item_pricing.txtItemSerial
import kotlinx.android.synthetic.main.content_pg_pricing.*
import retrofit2.HttpException
import java.io.IOException


class NewItemPricingActivity : AppCompatActivity() {
    private lateinit var linearLayoutManager: LinearLayoutManager

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var TextChangeEvent:TextWatcher
    private lateinit var ItemSerials:ArrayList<String>
    private lateinit var ItemSerialsList:ArrayList<String>
    private lateinit var adp:CustomListAdapter
    private lateinit var PricingLineCode:String
    private var testingAlwaysValid:Boolean =false
    private var userId:Int =-1
    private var stand: PricingStandModel? =null

    fun CreateStand() {
        try {
            Logger.Debug("API", "ItemPricingCreating Stand")
            val api = getInstanceStatic(IPAddress, false).create(
                BasicApi::class.java
            )
            val compositeDisposable = CompositeDisposable()
            compositeDisposable.addAll(
                api.CreatePricingStand(userId, PricingLineCode)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s: PricingStandModel? ->
                            if (s != null) {
                                Logger.Debug(
                                    "API",
                                    "ItemPricingCreate Stand Returned Result: $s Userid: $userId PricingLineCode: $PricingLineCode"
                                )
                                stand = s
                                runOnUiThread {
                                   txtItemSerial.isEnabled=true;

                                    txtItemSerial.requestFocus()
                                    toolbar.title = "(Item Pricing) Stand Id: "+s.id
                                }
                            } else {
                                Logger.Error(
                                    "API",
                                    "ItemPricingCreate Stand - retuned null:  Userid: $userId PricingLineCode: $PricingLineCode"
                                )
                                General.playError()
                                showMessageAndExit(
                                    "Failed To Create Stand",
                                    "Web Service Returned Null",
                                    Color.RED
                                )
                            }
                        }) { throwable: Throwable ->
                        var error: String? = throwable.toString()
                        if (throwable is HttpException) {
                            error = throwable.response().errorBody()!!.string()
                            if (error.isEmpty()) error = throwable.message
                            Logger.Debug(
                                "API",
                                "ItemPricingCreating Stand - Error In HTTP Response: $error"
                            )
                            showMessageAndExit(
                                "Failed To Create Stand",
                                "$error (API Http Error)",
                                Color.RED
                            )
                        } else {
                            Logger.Error(
                                "API",
                                "ItemPricingCreating Stand - Error In API Response: " + throwable.message
                            )
                            showMessageAndExit(
                                "Failed To Create Stand",
                                throwable.message + " (API Error)",
                                Color.RED
                            )
                        }
                    })
        } catch (e: Throwable) {
            Logger.Error("API", "ItemPricingCreating Stand: Error" + e.message)
            showMessageAndExit("Failed To Create Stand", e.message + " (Exception)", Color.RED)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_item_pricing)
        ItemSerials= arrayListOf<String>()
        ItemSerialsList= arrayListOf<String>()
        mStorage = Storage(applicationContext) //sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")
        PricingLineCode = mStorage.getDataString("PricingLineCode", "PL001")


        if(ItemSerials==null)
            ItemSerials=  arrayListOf<String>()
        if(ItemSerialsList==null)
            ItemSerialsList=  arrayListOf<String>()
        var items= arrayListOf<String>()
        items.addAll( ItemSerials.reversed())
        adp= CustomListAdapter(applicationContext,ItemSerialsList)
        recyclerView.setAdapter(adp)

        txtItemSerial.setShowSoftInputOnFocus(false);

        toolbar.setTitle("(Item Pricing) Stand Id:")

        txtItemSerial.requestFocus()
        txtItemSerial.showSoftInputOnFocus = false;
        TextChangeEvent=object : TextWatcher {

            @SuppressLint("ResourceAsColor")
            override fun afterTextChanged(s: Editable) {
                if(UpdatingText)
                    return;
                UpdatingText=true;
                var itemSerialStr:String=txtItemSerial.text.toString()
                lblError.text = ""

                if(!General.ValidateItemSerialCode(itemSerialStr)){
                    txtItemSerial.setText("")
                    txtItemSerial.requestFocus()
                    UpdatingText=false;
                    RefreshLabels()
                    return
                }

                if(ItemSerials!=null && ItemSerials.contains(txtItemSerial.text.toString().trim())) {
                    //Cancel();
                    showMessageAndExit("XXXXXX Failure XXXXXX","XXXXXX ItemSerial Scanned Twice XXXXXXXXX",Color.RED)
                    UpdatingText=false;
                    return
                }

                ValidateScan(itemSerialStr)
                txtItemSerial.setShowSoftInputOnFocus(false);

            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {

            }
        }
        hideSoftKeyboard(this)
        txtItemSerial.addTextChangedListener(TextChangeEvent);


        userId = General.getGeneral(applicationContext).UserID;

        txtItemSerial.isEnabled=false
        CreateStand()
    }

    lateinit var api: BasicApi
    var compositeDisposable= CompositeDisposable()
    var UpdatingText:Boolean=false
    lateinit var  mStorage:Storage ;  //sp存储
    var IPAddress =""
    private fun Beep(){
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300)
    }
    fun ValidateScan(ItemSerial:String) {

        try {
            stand.let{
                api= APIClient.getInstance(IPAddress ,false).create(BasicApi::class.java)
                compositeDisposable.addAll(
                    api.PriceItem(UPCPricingItemModel(userId, ItemSerial,"",stand!!.id ))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            {s->
                                if(s!=null){
                                    ItemSerials.add(ItemSerial)
                                    ItemSerialsList.add("${s.ItemSerial} (${s.USDPrice})")
                                    General.playSuccess()
                                }
                                else{
                                    General.playError()
                                    lblError.text = "Error: Web Service Returned Null"
                                }
                                txtItemSerial.setText("")
                                txtItemSerial.requestFocus()
                                UpdatingText=false
                                RefreshLabels()

                            },
                            {t:Throwable?->
                                run {
                                    txtItemSerial.setText("")
                                    txtItemSerial.requestFocus()
                                    UpdatingText=false
                                    if(t is HttpException){
                                        var ex:HttpException=t as HttpException
                                        showMessage("Scan Error",ex.response().errorBody()!!.string(),Color.RED)
                                    }
                                    else{
                                        showMessage("Scan Error","Error: "+t?.message,Color.RED)
                                    }

                                }
                            }
                        )
                )
            }
            }catch (e: Throwable) {
            txtItemSerial.setText("")
            txtItemSerial.requestFocus()
            UpdatingText=false
            showMessage("Scan Error","Exception"+  e?.message,Color.RED)
        }
        finally {
        }
    
    }
    fun RefreshLabels(){
        lblError.text = ""
        recyclerView.setLayoutManager(LinearLayoutManager(this))
        if(ItemSerials==null)
            ItemSerials=  arrayListOf<String>()
        if(ItemSerialsList==null)
            ItemSerialsList=  arrayListOf<String>()
        var items= arrayListOf<String>()
        items.addAll( ItemSerialsList.reversed())
        adp = CustomListAdapter(this, items)
        recyclerView.setAdapter(adp)

      if(ItemSerials.size>0){
            btnUPCPricingDone.visibility= View.VISIBLE
          btnUPCPricingDone.setOnClickListener {
              PostStand()
          }
        }
        hideSoftKeyboard(this)
    }
    var ColorGreen = Color.parseColor("#52ac24")
    var ColorRed = Color.parseColor("#ef2112")
    var ColorWhite = Color.parseColor("#ffffff")
    @SuppressLint("ResourceAsColor")
    fun PostStand() {

        try {
            btnUPCPricingDone.isEnabled = false
            txtItemSerial.isEnabled=false
            api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.SendPrintingStand(stand!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            var errmsg = ""
                             try {
                                 errmsg = s.string()
                             } catch (e: IOException) {
                                 e.printStackTrace()
                             }

                            if (errmsg.isNotEmpty() && errmsg.lowercase().startsWith("success")) {
                                General.playSuccess()
                                showMessageAndExit("✓✓✓✓✓✓ Success ✓✓✓✓✓", errmsg, Color.GREEN)

                            }
                            else{
                                if (errmsg.isEmpty()) {
                                    General.playError()
                                    showMessageAndExit("XXXXXX Failure XXXXXX", "Web Service Returned Empty Response", Color.RED)
                                }
                                else  {
                                    General.playError()
                                    showMessageAndExit("XXXXXX Failure XXXXXX", "Error $errmsg", Color.RED)
                                }
                            }
                        },
                        {t:Throwable?->
                            run {
                                var error: String? = t.toString()
                                if (t is HttpException) {
                                    error = t.response().errorBody()!!.string()
                                    if (error.isEmpty()) error = t.message

                                    showMessageAndExit(
                                        "XXXXXX Failure XXXXXX",
                                        "$error (API Http Error)",
                                        Color.RED
                                    )
                                } else {

                                    showMessageAndExit(
                                        "XXXXXX Failure XXXXXX",
                                        t?.message + " (API Error)",
                                        Color.RED
                                    )
                                }
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            showMessageAndExit(
                "XXXXXX Failure XXXXXX",
                e.message + " (Exception)",
                Color.RED
            )
        }

    }

    private fun showMessage(title: String, msg: String, color: Int) {
        if (color == Color.RED) Beep()
        lblError.text=msg
        lblError.setTextColor(color)
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(
                    "Ok"
                ) { _: DialogInterface?, _: Int -> }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        }
    }

    private fun showMessageAndExit(title: String, msg: String, color: Int) {
        if (color == Color.RED) Beep()
        lblError.text=msg
        lblError.setTextColor(color)
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(
                    "Ok"
                ) { _: DialogInterface?, _: Int -> finish() }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .show()
        }
    }


}