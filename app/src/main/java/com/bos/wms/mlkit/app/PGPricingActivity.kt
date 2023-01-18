package com.bos.wms.mlkit.app

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
    private lateinit var adp:CustomListAdapter
    private lateinit var PricingLineCode:String
    private var testingAlwaysValid:Boolean =false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pg_pricing)
        ItemSerials= arrayListOf<String>()
        mStorage = Storage(applicationContext) //sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")
        PricingLineCode = mStorage.getDataString("PricingLineCode", "PL001")


        if(ItemSerials==null)
            ItemSerials=  arrayListOf<String>()
        var items= arrayListOf<String>()
        items.addAll( ItemSerials.reversed())
        adp= CustomListAdapter(applicationContext,ItemSerials)
        recyclerView.setAdapter(adp)
        txtPG.setShowSoftInputOnFocus(false);
        txtSeason.setShowSoftInputOnFocus(false);
        txtItemSerial.setShowSoftInputOnFocus(false);



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
                    Cancel();
                    UpdatingText=false;
                    return
                }


                var UserID: Int=General.getGeneral(applicationContext).UserID
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
    }

    fun Cancel(){
        lblError.text = "Item Scanned Twice !!"
        txtItemSerial.isEnabled=false;
        txtPG.isEnabled=false;
        txtSeason.isEnabled=false;
        recyclerView.setLayoutManager(LinearLayoutManager(this))
        if(ItemSerials==null)
            ItemSerials=  arrayListOf<String>()
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
            // TODO: handle loggedInUser authentication
            //var UserID: Int=General.getGeneral(applicationContext).UserID
//            var FloorID: Int=General.getGeneral(applicationContext).FloorID
//            var model=PutPalleteNextShelfModel(PalleteCode,-1,UserID,FloorID)
            api= APIClient.getInstance(IPAddress ,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.ValidatePGPricing(ItemSerial,PG,Season)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            if(testingAlwaysValid || s.isNullOrEmpty() || s.lowercase().startsWith("success")){
                                ItemSerials.add(ItemSerial)
                                General.playSuccess()
                            }
                            else{
                                General.playError()
                            }
                            txtItemSerial.setText("")
                            txtItemSerial.requestFocus()
                            UpdatingText=false
                            RefreshLabels()
                            lblError.setText(s)

                            //val stringResponse = s.string()
                        },
                        {t:Throwable?->
                            run {
                                UpdatingText=false
                                if(t is HttpException){
                                    var ex:HttpException=t as HttpException
                                    lblError.setText(ex.response().errorBody()!!.string())
                                }
                                else{
                                    lblError.setText(t?.message)
                                }

                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            UpdatingText=false
            lblError.setText(e?.message)
            throw(IOException("UPC Pricing Activity - ValidateScan", e))
        }
        finally {
        }
    }
    fun RefreshLabels(){
        lblError.text = ""
        recyclerView.setLayoutManager(LinearLayoutManager(this))
        if(ItemSerials==null)
            ItemSerials=  arrayListOf<String>()
        var items= arrayListOf<String>()
        items.addAll( ItemSerials.reversed())
        adp = CustomListAdapter(this, items)
        recyclerView.setAdapter(adp)

      if(ItemSerials.size>0){
            btnUPCPricingDone.visibility= View.VISIBLE
          btnUPCPricingDone.setOnClickListener {
              PostFoldingScanItem(ItemSerials.joinToString())
          }
        }
        hideSoftKeyboard(this)
    }
    var ColorGreen = Color.parseColor("#52ac24")
    var ColorRed = Color.parseColor("#ef2112")
    var ColorWhite = Color.parseColor("#ffffff")
    @SuppressLint("ResourceAsColor")
    fun PostFoldingScanItem(StrItemSerials:String) {

        try {
            btnUPCPricingDone.isEnabled = false
            var PGStr:String=txtPG.text.toString()
            var SeasonStr:String=txtSeason.text.toString()
            // TODO: handle loggedInUser authentication
            var UserID: Int=General.getGeneral(applicationContext).UserID
            txtItemSerial.isEnabled=false
            api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.PostPGPricing(UserID,PricingLineCode,StrItemSerials,PGStr,SeasonStr)
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
                                General.playSuccess()
                                lblError.setTextColor(ColorGreen)
                                startActivity(getIntent());
                                finish();
                                overridePendingTransition(0, 0);
                            }
                            if (ErrorMsg.isEmpty()) {
                                General.playSuccess()
                                startActivity(getIntent());
                                finish();
                                overridePendingTransition(0, 0);
                                RefreshLabels()
                                adp.notifyDataSetChanged()
                                //val stringResponse = s.string()
                                lblError.setText("")
                            } else {
                                txtItemSerial.setText("")
                                btnUPCPricingDone.isEnabled = true
                                General.playError()
                                txtItemSerial.requestFocus()
                                lblError.setTextColor(R.color.design_default_color_error)
                                lblError.text = ErrorMsg
                                //(ErrorMsg)
                            }

                            txtItemSerial.isEnabled=true
                            UpdatingText=false
                        },
                        {t:Throwable?->
                            run {
                                lblError.setText(t?.message)
                                txtItemSerial.isEnabled=true
                                UpdatingText=false
                                btnUPCPricingDone.isEnabled = true
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            lblError.setText(e?.message)
            txtItemSerial.isEnabled=true
            btnUPCPricingDone.isEnabled = true
            throw(IOException("UPC Pricing Activity - PostUPCPricing", e))
        }
        finally {
            txtItemSerial.isEnabled=true
        }
    }

}