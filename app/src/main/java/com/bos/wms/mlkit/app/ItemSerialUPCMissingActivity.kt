package com.bos.wms.mlkit.app

import Remote.APIClient
import Remote.BasicApi
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
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
import kotlinx.android.synthetic.main.content_itemserial_upc_missing.*
import java.io.IOException


class ItemSerialUPCMissingActivity : AppCompatActivity() {
    private lateinit var linearLayoutManager: LinearLayoutManager

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var TextChangeEvent:TextWatcher
    private lateinit var TextUPCChangeEvent:TextWatcher
    private lateinit var MissingLetter:String
    private lateinit var Items:ArrayList<String>
    private lateinit var ValidatedItems:ArrayList<String>
    private lateinit var PostMissingItems:ArrayList<String>

    private var  NbOfItems:Int = 0
    private lateinit var adp:CustomListAdapter
    var UpdatingText:Boolean=false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itemserial_upc_missing)
        Items= arrayListOf()
        ValidatedItems= arrayListOf()

        PostMissingItems= arrayListOf()
        mStorage = Storage(applicationContext) //sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")
        MissingLetter = mStorage.getDataString("MissingLetter", "PL001")

        txtItemCode.setShowSoftInputOnFocus(false);

        txtItemCode.requestFocus()
        txtItemCode.setShowSoftInputOnFocus(false);

        TextChangeEvent=object : TextWatcher {

            @SuppressLint("ResourceAsColor")
            override fun afterTextChanged(s: Editable) {
                if(UpdatingText)
                    return;
                UpdatingText=true;
                var ItemStr:String=txtItemCode.text.toString()

                lblError.setText("")

                if(General.ValidateItemSerialCode(ItemStr)){

                    //PostItemSerial(ItemStr)
                    adp= CustomListAdapter(applicationContext,Items)
                    recyclerView.setAdapter(adp)
                    InitItemSerial(ItemStr)
                }
                else{
                    txtItemCode.setText("")
                    UpdatingText=false;
                }
                RefreshLabels()
                txtItemCode.setShowSoftInputOnFocus(false);
            }

          /*  override fun afterTextChanged(p0: Editable?) {
                TODO("Not yet implemented")
            }*/
            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {

            }


        }

        TextUPCChangeEvent=object : TextWatcher {

            @SuppressLint("ResourceAsColor")
            override fun afterTextChanged(s: Editable) {
                if(UpdatingText)
                    return;
                UpdatingText=true;
                var ItemUPC:String=txtUPC.text.toString()

                lblError.setText("")

                if(!General.ValidateItemCode(ItemUPC)){
                    txtUPC.setText("")

                }
                UpdatingText=false;
                RefreshLabels()
                txtItemCode.setShowSoftInputOnFocus(false);
            }

            /*  override fun afterTextChanged(p0: Editable?) {
                  TODO("Not yet implemented")
              }*/
            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {

            }


        }


        btn17.setOnClickListener { PostItemSerial(0.17f) }
        btn20.setOnClickListener { PostItemSerial(0.2f) }
        btn25.setOnClickListener { PostItemSerial(0.25f) }
        btn30.setOnClickListener { PostItemSerial(0.30f) }
        btn35.setOnClickListener { PostItemSerial(0.35f) }
        btn40.setOnClickListener { PostItemSerial(0.4f) }
        btn50.setOnClickListener { PostItemSerial(0.5f) }
        btn60.setOnClickListener { PostItemSerial(0.6f) }
        btn65.setOnClickListener { PostItemSerial(0.65f) }
        btn70.setOnClickListener { PostItemSerial(0.7f) }
        btn75.setOnClickListener { PostItemSerial(0.75f) }
        btn80.setOnClickListener { PostItemSerial(0.8f) }
        hideSoftKeyboard(this)
        txtItemCode.addTextChangedListener(TextChangeEvent);
        txtUPC.addTextChangedListener(TextUPCChangeEvent);

    }
    lateinit var api: BasicApi
    var compositeDisposable= CompositeDisposable()

    lateinit var  mStorage:Storage ;  //sp存储
    var IPAddress =""

    private fun Beep(){
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300)
    }

    fun InitItemSerial(ItemStr:String) {
        try {
            // TODO: handle loggedInUser authentication
            var UserID: Int=General.getGeneral(applicationContext).UserID
            api= APIClient.getInstance(IPAddress ,false).create(BasicApi::class.java)

            compositeDisposable.addAll(
                api.InitItemSerialMissing(UserID, ItemStr)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            var resp = ""
                            try {
                                resp = s
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                            if(resp.isNullOrEmpty()){
                                lblError.setText("Valid Item!!")
                                txtUPC.requestFocus()
                            }
                            else if (resp.lowercase().startsWith("success")){
                                lblError.setText(resp)
                                txtUPC.requestFocus()
                            }
                            else{
                                txtItemCode.setText("")
                                txtItemCode.requestFocus()
                            lblError.setText(resp)
                        }
                            RefreshLabels();
                            UpdatingText=false;
                            //val stringResponse = s.string()
                        },
                        {t:Throwable?->
                            run {
                                lblError.setText(t?.message)
                                RefreshLabels();
                                UpdatingText=false;
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            lblError.setText(e?.message)
            throw(IOException("ItemPricing Activity - ValidateItemSerial", e))
            UpdatingText=false;
        }
        finally {
        }
    }
    fun PostItemSerial(Perc:Float) {
        try {
            var ItemStr:String="";
            var UPCStr:String="";
            try {
                ItemStr=txtItemCode.text.toString()
            }catch (ex:Exception){
                lblError.setText("Invalid ItemSerial!!")
                txtUPC.setText("")
                txtItemCode.requestFocus()
                UpdatingText=false
                return
            }
            try {
                UPCStr=txtUPC.text.toString()
                if(!General.ValidateItemCode(UPCStr))
                    UPCStr=""
            }catch (ex:Exception){}
            // TODO: handle loggedInUser authentication
            var UserID: Int=General.getGeneral(applicationContext).UserID
            api= APIClient.getInstance(IPAddress ,false).create(BasicApi::class.java)
            txtItemCode.setText("")
            txtUPC.setText("")
            txtItemCode.requestFocus()


            compositeDisposable.addAll(
                api.PostItemSerialMissing(UserID,UPCStr, ItemStr,Perc)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            var resp = ""
                            try {
                                resp = s
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                            if(resp.isNullOrEmpty()|| resp.equals("Success")){
                                Items.add(0,ItemStr+ ":"+UPCStr+Perc.toString())
                                ValidatedItems.add(0,ItemStr)
                                PostMissingItems.add(0, ItemStr)
                                NbOfItems=NbOfItems+1
                                lblError.setText("Item Added")
                                General.playSuccess()
                            }
                            else{
                                General.playError()
                                lblError.setText(resp)
                            }

                            RefreshLabels();
                            txtItemCode.requestFocus()
                            //val stringResponse = s.string()
                        },
                        {t:Throwable?->
                            run {
                                lblError.setText(t?.message)
                                RefreshLabels();
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            lblError.setText(e?.message)
            throw(IOException("ItemPricing Activity - ValidateItemSerial", e))
        }
        finally {
        }
    }
    fun RefreshLabels() {

        lblItemsCnt.setText("Nb Items:"+Items.size)
        var ItemStr:String=txtItemCode.text.toString()
        if(ItemStr.isNullOrEmpty())
            txtItemCode.requestFocus()
        else
            txtUPC.requestFocus()
        recyclerView.setLayoutManager(LinearLayoutManager(this))
        adp = CustomListAdapter(this, Items)
        recyclerView.setAdapter(adp)
    }
  //  @SuppressLint("ResourceAsColor")
   /* fun PostItemPricing(UserID:Int,MissingLetter:String,ItemsStr:String) {
        try {
            api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.PostItemPricing(UserID,MissingLetter,ItemsStr)
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
                                val builder = AlertDialog.Builder(this)
                                builder.setTitle("Item Pricing Done!!")
                                builder.setMessage("Item Pricing Done, Do you want to scan a new list?")
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

                                val alert: AlertDialog =builder.create()
                                builder.show()

                                object : CountDownTimer(3000, 1000) {
                                    override fun onTick(p0: Long) {}
                                    override fun onFinish() {
                                        try {
                                            alert.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
                                        }
                                        catch (ex:Exception){

                                        }
                                    }
                                }.start()

                            } else if (!ErrorMsg.isEmpty()) {
                                lblError.setTextColor(R.color.design_default_color_error)
                                lblError.text = ErrorMsg
                            }
                            txtItemCode.isEnabled=true
                        },
                        {t:Throwable?->
                            run {
                                lblError.setText(t?.message)
                                txtItemCode.isEnabled=true
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            lblError.setText(e?.message)
            txtItemCode.isEnabled=true
            throw(IOException("Item Pricing Activity - PostItemPricing", e))
        }
        finally {
        }
    }*/
}