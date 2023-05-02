package com.bos.wms.mlkit.app

import Model.Pricing.PricingStandModel
import Model.Pricing.QuatroPricingItemModel
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
import kotlinx.android.synthetic.main.activity_pg_pricing.*
import kotlinx.android.synthetic.main.content_pg_pricing.*
import kotlinx.android.synthetic.main.content_pg_pricing.btnUPCPricingDone
import kotlinx.android.synthetic.main.content_pg_pricing.lblError
import kotlinx.android.synthetic.main.content_pg_pricing.recyclerView
import kotlinx.android.synthetic.main.content_pg_pricing.txtItemSerial
import kotlinx.android.synthetic.main.content_upc_pricing.*
import retrofit2.HttpException
import java.io.IOException


class PGPricingActivity : AppCompatActivity() {
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
            Logger.Debug("API", "QuatroPricingCreating Stand")
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
                                    "QuatroPricingCreate Stand Returned Result: $s Userid: $userId PricingLineCode: $PricingLineCode"
                                )
                                stand = s
                                runOnUiThread {
                                   txtPG.isEnabled=true;
                                   txtSeason.isEnabled=true;
                                   txtItemSerial.isEnabled=true;
                                    txtPG.requestFocus()
                                    toolbar.setTitle("(Quatro Pricing) Stand Id: "+s.id)
                                }
                            } else {
                                Logger.Error(
                                    "API",
                                    "QuatroPricingCreate Stand - retuned null:  Userid: $userId PricingLineCode: $PricingLineCode"
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
                                "QuatroPricingCreating Stand - Error In HTTP Response: $error"
                            )
                            showMessageAndExit(
                                "Failed To Create Stand",
                                "$error (API Http Error)",
                                Color.RED
                            )
                        } else {
                            Logger.Error(
                                "API",
                                "QuatroPricingCreating Stand - Error In API Response: " + throwable.message
                            )
                            showMessageAndExit(
                                "Failed To Create Stand",
                                throwable.message + " (API Error)",
                                Color.RED
                            )
                        }
                    })
        } catch (e: Throwable) {
            Logger.Error("API", "QuatroPricingCreating Stand: Error" + e.message)
            showMessageAndExit("Failed To Create Stand", e.message + " (Exception)", Color.RED)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pg_pricing)
        ItemSerials= arrayListOf<String>()
        ItemSerialsList= arrayListOf<String>()
        mStorage = Storage(applicationContext) //sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")
        PricingLineCode = mStorage.getDataString("PricingLineCode", "PL001")
        userId = General.getGeneral(applicationContext).UserID


        if(ItemSerials==null)
            ItemSerials=  arrayListOf<String>()
        if(ItemSerialsList==null)
            ItemSerialsList=  arrayListOf<String>()
        var items= arrayListOf<String>()
        items.addAll( ItemSerials.reversed())
        adp= CustomListAdapter(applicationContext,ItemSerialsList)
        recyclerView.setAdapter(adp)
        txtPG.setShowSoftInputOnFocus(false);
        txtSeason.setShowSoftInputOnFocus(false);
        txtItemSerial.setShowSoftInputOnFocus(false);

        toolbar.setTitle("(Quatro Pricing) Stand Id:")

        txtPG.requestFocus()
        txtItemSerial.setShowSoftInputOnFocus(false);
        txtPG.setShowSoftInputOnFocus(false);
        txtSeason.setShowSoftInputOnFocus(false);

        TextChangeEvent=object : TextWatcher {

            @SuppressLint("ResourceAsColor")
            override fun afterTextChanged(s: Editable) {
                if(UpdatingText)
                    return;
                UpdatingText=true;
                var seqVal:Int=0
                var itemSerialStr:String=txtItemSerial.text.toString()
                var PGStr:String=txtPG.text.toString()
                var SeasonStr:String=txtSeason.text.toString()
                lblError.setText("")

                if(!General.ValidatePG(PGStr)){
                    txtPG.setText("")
                    txtPG.requestFocus()
                    UpdatingText=false;
                    RefreshLabels()
                    return
                }else{
                    txtPG.isEnabled=false;
                    txtSeason.requestFocus()
                }
                if(!General.ValidateSeason(SeasonStr)){
                    txtSeason.setText("")
                    txtSeason.requestFocus()
                    UpdatingText=false;
                    RefreshLabels()
                    return
                }
                else{
                    txtSeason.isEnabled=false;
                    txtItemSerial.requestFocus()
                }
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

                ValidateScan(itemSerialStr,PGStr,SeasonStr)
                txtItemSerial.setShowSoftInputOnFocus(false);
                txtPG.setShowSoftInputOnFocus(false);
                txtSeason.setShowSoftInputOnFocus(false);

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
        txtPG.addTextChangedListener(TextChangeEvent);
        txtSeason.addTextChangedListener(TextChangeEvent);


        txtPG.isEnabled=false
        txtSeason.isEnabled=false
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
    fun ValidateScan(ItemSerial:String,PG:String,Season:String) {

        try {
            stand.let{
                api= APIClient.getInstance(IPAddress ,false).create(BasicApi::class.java)
                compositeDisposable.addAll(
                    api.PriceQuatroItem(QuatroPricingItemModel(userId, ItemSerial,PG,Season,stand!!.id ))
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
              PostStand(ItemSerials.joinToString())
          }
        }
        hideSoftKeyboard(this)
    }
    var ColorGreen = Color.parseColor("#52ac24")
    var ColorRed = Color.parseColor("#ef2112")
    var ColorWhite = Color.parseColor("#ffffff")
    @SuppressLint("ResourceAsColor")
    fun PostStand(StrItemSerials:String) {

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

    fun Cancel(){
        lblError.text = "Item Scanned Twice !!"
        txtItemSerial.isEnabled=false;
        txtPG.isEnabled=false;
        txtSeason.isEnabled=false;
        recyclerView.setLayoutManager(LinearLayoutManager(this))
        if(ItemSerials==null)
            ItemSerials=  arrayListOf<String>()
        if(ItemSerialsList==null)
            ItemSerialsList=  arrayListOf<String>()
        var items= arrayListOf<String>()
        items.addAll( ItemSerials.reversed())
        adp = CustomListAdapter(this, items)
        recyclerView.setAdapter(adp)

        btnUPCPricingDone.visibility= View.VISIBLE
        btnUPCPricingDone.text="Start Over"
        btnUPCPricingDone.setOnClickListener {
            if(btnUPCPricingDone.text.equals("Start Over")) {
                txtItemSerial.setText("")
                txtSeason.setText("")
                txtPG.setText("")
                txtItemSerial.requestFocus()
                ItemSerials.clear()
                adp = CustomListAdapter(this, ItemSerials)
                recyclerView.setAdapter(adp)
                lblError.text = ""
                btnUPCPricingDone.setText("Done")

                txtItemSerial.isEnabled = true
                startActivity(getIntent());
                finish();
                overridePendingTransition(0, 0);
                RefreshLabels()


                adp.notifyDataSetChanged()
                //val stringResponse = s.string()
                lblError.setText("")
            }

        }

        hideSoftKeyboard(this)
    }

}