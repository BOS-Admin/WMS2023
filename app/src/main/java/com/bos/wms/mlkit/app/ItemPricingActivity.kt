package com.bos.wms.mlkit.app

import Model.BosApp.Transfer.ItemPricingModel
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
import android.util.Log
import android.util.Log.DEBUG
import android.util.Log.ERROR
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
import kotlinx.android.synthetic.main.content_item_pricing.*
import kotlinx.android.synthetic.main.content_item_pricing.btnSubmit
import kotlinx.android.synthetic.main.content_item_pricing.lblError
import kotlinx.android.synthetic.main.content_item_pricing.recyclerView
import java.io.IOException
import java.lang.Exception
import java.util.logging.Logger


class ItemPricingActivity : AppCompatActivity() {
    private lateinit var linearLayoutManager: LinearLayoutManager

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var TextChangeEvent:TextWatcher
    private lateinit var PricingLineCode:String
    private lateinit var Items:ArrayList<String>
    private lateinit var ValidatedItems:ArrayList<String>
    private lateinit var PostPricingItems:ArrayList<String>

    private var  NbOfItems:Int = 0
    private lateinit var adp:CustomListAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_pricing)
        Items= arrayListOf()
        ValidatedItems= arrayListOf()

        PostPricingItems= arrayListOf()
        mStorage = Storage(applicationContext) //sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")
        PricingLineCode = mStorage.getDataString("PricingLineCode", "PL001")


        txtItemCode.setShowSoftInputOnFocus(false);

        txtItemCode.requestFocus()
        txtItemCode.setShowSoftInputOnFocus(false);

        TextChangeEvent=object : TextWatcher {
            var UpdatingText:Boolean=false
            @SuppressLint("ResourceAsColor")
            override fun afterTextChanged(s: Editable) {
                if(UpdatingText)
                    return;
                UpdatingText=true;
                var ItemStr:String=txtItemCode.text.toString()

                lblError.setText("")

                if(Items.contains(ItemStr)){
                    lblError.setText("Item Already Scanned!!")
                    txtItemCode.setText("")
                    txtItemCode.requestFocus()
                }
                else if(General.ValidateItemSerialCode(ItemStr)){

                    ValidateItemSerial(ItemStr)
                    adp= CustomListAdapter(applicationContext,Items)
                    recyclerView.setAdapter(adp)
                    txtItemCode.requestFocus()
                }
                UpdatingText=false;
                RefreshLabels()
                txtItemCode.setShowSoftInputOnFocus(false);
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
        hideSoftKeyboard(this)
        txtItemCode.addTextChangedListener(TextChangeEvent);
    }
    lateinit var api: BasicApi
    var compositeDisposable= CompositeDisposable()

    lateinit var  mStorage:Storage ;  //sp存储
    var IPAddress =""

    private fun Beep(){
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300)
    }
    fun ValidateItemSerial(ItemStr:String) {
        try {
            // TODO: handle loggedInUser authentication
            var UserID: Int=General.getGeneral(applicationContext).UserID
            api= APIClient.getInstance(IPAddress ,false).create(BasicApi::class.java)
            txtItemCode.setText("")
            txtItemCode.requestFocus()
            compositeDisposable.addAll(
                api.InitItemPricing(ItemStr)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            var resp=s
                            if(s.isNullOrEmpty() || s.lowercase().startsWith("success")){

                            Items.add(0,ItemStr)
                            ValidatedItems.add(0,ItemStr)
                            PostPricingItems.add(0, ItemStr)
                            NbOfItems=NbOfItems+1
                            lblError.setText("Item Added")
                            }
                            else{
                                lblError.setText(s)
                            }
                            RefreshLabels();
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

        recyclerView.setLayoutManager(LinearLayoutManager(this))
        adp = CustomListAdapter(this, Items)
        recyclerView.setAdapter(adp)
        var UserID: Int = General.getGeneral(applicationContext).UserID
        Log.e("CurrentUserID", "UserID: " + UserID)


        btnSubmit.visibility = View.VISIBLE
        btnSubmit.setOnClickListener {
            try {
                PostItemPricing(UserID, PricingLineCode, Items)
            } catch (ex: Exception) {
                lblError.setText(ex.message)

            }
            hideSoftKeyboard(this)
        }
    }
    @SuppressLint("ResourceAsColor")
    fun PostItemPricing(UserID:Int,PricingLineCode:String,Items:ArrayList<String>) {
        try {
            Log.e("CurrentUserID2", "UserID: " + UserID)
            api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            var itemPricingModel = ItemPricingModel(UserID, PricingLineCode, Items)
            compositeDisposable.addAll(
                api.PostItemPricing(itemPricingModel)
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
    }
}