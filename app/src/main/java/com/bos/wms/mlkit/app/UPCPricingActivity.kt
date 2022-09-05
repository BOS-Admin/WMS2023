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
import kotlinx.android.synthetic.main.content_item_pricing.*
import kotlinx.android.synthetic.main.content_upc_pricing.*
import kotlinx.android.synthetic.main.content_upc_pricing.lblError
import kotlinx.android.synthetic.main.content_upc_pricing.recyclerView
import retrofit2.HttpException
import java.io.IOException


class UPCPricingActivity : AppCompatActivity() {
    private lateinit var linearLayoutManager: LinearLayoutManager

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var TextChangeEvent:TextWatcher
    private lateinit var ItemSerials:ArrayList<String>
    private lateinit var ItemUPCs:ArrayList<String>
    private lateinit var adp:CustomListAdapter
    private lateinit var PricingLineCode:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upc_pricing)
        ItemSerials= arrayListOf<String>()
        ItemUPCs= arrayListOf<String>()
        mStorage = Storage(applicationContext) //sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")
        PricingLineCode = mStorage.getDataString("PricingLineCode", "PL001")
        adp= CustomListAdapter(applicationContext,ItemSerials)
        recyclerView.setAdapter(adp)
        txtUPC.setShowSoftInputOnFocus(false);
        txtItemSerial.setShowSoftInputOnFocus(false);

        txtItemSerial.requestFocus()
        txtItemSerial.setShowSoftInputOnFocus(false);
        txtUPC.setShowSoftInputOnFocus(false);

        TextChangeEvent=object : TextWatcher {

            @SuppressLint("ResourceAsColor")
            override fun afterTextChanged(s: Editable) {
                if(UpdatingText)
                    return;
                UpdatingText=true;
                var seqVal:Int=0
                var itemSerialStr:String=txtItemSerial.text.toString()
                var UPCStr:String=txtUPC.text.toString()
                lblError.setText("")
                if(!General.ValidateItemSerialCode(itemSerialStr)){
                    txtItemSerial.setText("")
                    txtItemSerial.requestFocus()
                    UpdatingText=false;
                    RefreshLabels()
                    return
                }

                if(!General.ValidateItemCode(UPCStr)){
                    txtUPC.setText("")
                    txtUPC.requestFocus()
                    UpdatingText=false;
                    RefreshLabels()
                    return
                }
                var UserID: Int=General.getGeneral(applicationContext).UserID
                ValidateScan(itemSerialStr,UPCStr)
                txtItemSerial.setShowSoftInputOnFocus(false);
                txtUPC.setShowSoftInputOnFocus(false);
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
        txtUPC.addTextChangedListener(TextChangeEvent);
    }
    lateinit var api: BasicApi
    var compositeDisposable= CompositeDisposable()
    var UpdatingText:Boolean=false
    lateinit var  mStorage:Storage ;  //sp存储
    var IPAddress =""
    private fun Beep(){
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300)
    }
    fun ValidateScan(ItemSerial:String,ItemUPC:String) {

        try {
            // TODO: handle loggedInUser authentication
            //var UserID: Int=General.getGeneral(applicationContext).UserID
//            var FloorID: Int=General.getGeneral(applicationContext).FloorID
//            var model=PutPalleteNextShelfModel(PalleteCode,-1,UserID,FloorID)
            api= APIClient.getInstance(IPAddress ,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.ValidateUPCPricing(ItemSerial,ItemUPC)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            if(s.isNullOrEmpty() || s.lowercase().startsWith("success")){
                                ItemSerials.add(ItemSerial)
                                ItemUPCs.add(ItemUPC)
                                General.playSuccess()
                                txtUPC.setText("")
                                txtItemSerial.setText("")
                                txtItemSerial.requestFocus()
                            }
                            else{
                                General.playError()
                                if(s.lowercase().contains("upc")){
                                    txtUPC.setText("")
                                    txtUPC.requestFocus()
                                }
                                else{
                                    txtUPC.setText("")
                                    txtItemSerial.setText("")
                                    txtItemSerial.requestFocus()
                                }
                            }
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
        adp = CustomListAdapter(this, ItemSerials)
        recyclerView.setAdapter(adp)

      if(ItemSerials.size>0){
            btnUPCPricingDone.visibility= View.VISIBLE
          btnUPCPricingDone.setOnClickListener {
              PostFoldingScanItem(ItemSerials.joinToString(),ItemUPCs.joinToString())
          }
        }
        hideSoftKeyboard(this)
    }
    var ColorGreen = Color.parseColor("#52ac24")
    var ColorRed = Color.parseColor("#ef2112")
    var ColorWhite = Color.parseColor("#ffffff")
    @SuppressLint("ResourceAsColor")
    fun PostFoldingScanItem(StrItemSerials:String,StrItemUPCs: String) {

        try {
            // TODO: handle loggedInUser authentication
            var UserID: Int=General.getGeneral(applicationContext).UserID
            txtUPC.isEnabled=false
            api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.PostUPCPricing(UserID,StrItemSerials,StrItemUPCs,PricingLineCode)
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
                                txtUPC.setText("")
                                txtUPC.requestFocus()
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
                                txtUPC.setText("")
                                txtUPC.requestFocus()
                                adp.notifyDataSetChanged()
                                //val stringResponse = s.string()
                                lblError.setText("")
                            } else {
                                txtUPC.setText("")
                                General.playError()
                                txtUPC.requestFocus()
                                lblError.setTextColor(R.color.design_default_color_error)
                                lblError.text = ErrorMsg
                                //(ErrorMsg)
                            }

                            txtUPC.isEnabled=true
                            UpdatingText=false
                        },
                        {t:Throwable?->
                            run {
                                lblError.setText(t?.message)
                                txtUPC.isEnabled=true
                                UpdatingText=false
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            lblError.setText(e?.message)
            txtUPC.isEnabled=true
            throw(IOException("UPC Pricing Activity - PostUPCPricing", e))
        }
        finally {
            txtUPC.isEnabled=true
        }
    }

}