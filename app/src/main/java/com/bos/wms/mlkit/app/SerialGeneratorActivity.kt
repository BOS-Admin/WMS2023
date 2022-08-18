package com.bos.wms.mlkit.app

import Model.FoldingItem
import Model.FoldingItemModel
import Remote.APIClient
import Remote.BasicApi
import android.annotation.SuppressLint
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import kotlinx.android.synthetic.main.content_serial_generator.*
import retrofit2.HttpException
import java.io.IOException


class SerialGeneratorActivity : AppCompatActivity() {
    private lateinit var linearLayoutManager: LinearLayoutManager

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var TextChangeEvent:TextWatcher
    private lateinit var DoneItems:ArrayList<String>
    private lateinit var adp:CustomListAdapter
    private var  DefaultShelfNbOfBins:Int? = null
    private var foldingItem:FoldingItem?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_serial_generator)
        DoneItems= arrayListOf<String>()
        mStorage = Storage(applicationContext) //sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")
        adp= CustomListAdapter(applicationContext,DoneItems)
        recyclerView.setAdapter(adp)
        txtFoldingScanStation.setShowSoftInputOnFocus(false);

        txtFoldingScanStation.requestFocus()
        txtFoldingScanStation.setShowSoftInputOnFocus(false);

        TextChangeEvent=object : TextWatcher {

            @SuppressLint("ResourceAsColor")
            override fun afterTextChanged(s: Editable) {
                if(UpdatingText)
                    return;
                UpdatingText=true;
                var seqVal:Int=0
                var stationStr:String=txtFoldingScanStation.text.toString()
                var itemStr:String=nbFoldingScanItem.text.toString()
                lblError.setText("")
                if(!General.ValidateFoldingStationCode(stationStr)){
                    txtFoldingScanStation.setText("")
                    txtFoldingScanStation.requestFocus()
                    UpdatingText=false;
                    RefreshLabels()
                    btnSubmit.setEnabled(false)
                    return
                }

                if(foldingItem==null){
                    InitFoldingScan(stationStr)
                }
                if(foldingItem==null){
                    txtFoldingScanStation.setText("")
                    txtFoldingScanStation.requestFocus()
                    UpdatingText=false;
                    RefreshLabels()
                    return;
                }

                if(!General.ValidateNbOfSerialsGenerate(itemStr)){
                    nbFoldingScanItem.setText("")
                    nbFoldingScanItem.requestFocus()
                    UpdatingText=false;
                    RefreshLabels()
                    return
                }

                RefreshLabels()
                UpdatingText=false;
                txtFoldingScanStation.setShowSoftInputOnFocus(false);
            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {

            }
        }
        hideSoftKeyboard(this)
        txtFoldingScanStation.addTextChangedListener(TextChangeEvent);
        nbFoldingScanItem.addTextChangedListener(TextChangeEvent);
    }
    lateinit var api: BasicApi
    var compositeDisposable= CompositeDisposable()
    var UpdatingText:Boolean=false
    lateinit var  mStorage:Storage ;  //sp存储
    var IPAddress =""
    private fun Beep(){
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300)
    }
    fun InitFoldingScan(FoldingStationCode:String) {

        try {
            // TODO: handle loggedInUser authentication
            //var UserID: Int=General.getGeneral(applicationContext).UserID
//            var FloorID: Int=General.getGeneral(applicationContext).FloorID
//            var model=PutPalleteNextShelfModel(PalleteCode,-1,UserID,FloorID)
            api= APIClient.getInstance(IPAddress ,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.InitGenerateSerials(FoldingStationCode)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            foldingItem=s
                            txtFoldingScanStation.setText(s.stationCode)
                            txtFoldingScanStation.isEnabled=false
                            UpdatingText=false
                            RefreshLabels()
                            lblError.setText("")
                            //val stringResponse = s.string()
                        },
                        {t:Throwable?->
                            run {
                                if(t is HttpException){
                                    var ex:HttpException=t as HttpException
                                    lblError.setText(ex.response().errorBody()!!.string())
                                }
                                else{
                                    lblError.setText(t?.message)
                                }
                                UpdatingText=false
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            UpdatingText=false
            lblError.setText(e?.message)
            throw(IOException("FoldingScan Activity - GetFoldingScanListItems", e))
        }
        finally {
            UpdatingText=false
        }
    }
    fun RefreshLabels(){
        lblError.text = ""
      if(General.ValidateFoldingStationCode(txtFoldingScanStation.text.toString())
                  && General.ValidateNbOfSerialsGenerate(nbFoldingScanItem.text.toString())){
          btnSubmit.setEnabled(true)
          btnSubmit.setOnClickListener {
                var UserID: Int=General.getGeneral(applicationContext).UserID
                var model=FoldingItemModel(UserID,foldingItem!!.location,foldingItem!!.locationId,nbFoldingScanItem.text.toString(),foldingItem!!.id)
              btnSubmit.setEnabled(false)
                PostFoldingScanItem(model)

            }
        }
        else{
          btnSubmit.setEnabled(false)
      }
        hideSoftKeyboard(this)
    }
    var ColorGreen = Color.parseColor("#52ac24")
    var ColorRed = Color.parseColor("#ef2112")
    var ColorWhite = Color.parseColor("#ffffff")
    @SuppressLint("ResourceAsColor")
    fun PostFoldingScanItem(Model:FoldingItemModel) {

        try {
            // TODO: handle loggedInUser authentication

            nbFoldingScanItem.isEnabled=false
            api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.PostGenerateSerials(Model)
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
                                lblError.text = "Success:"+Model.barcode
                                lblError.setTextColor(ColorGreen)
                                nbFoldingScanItem.setText("")
                                nbFoldingScanItem.requestFocus()
                                DoneItems.add(0,"Success:"+Model.barcode)
                            }
                            if (ErrorMsg.isEmpty()) {

                                RefreshLabels()
                                nbFoldingScanItem.setText("")
                                nbFoldingScanItem.requestFocus()
                                DoneItems.add(0,Model.barcode)
                                adp.notifyDataSetChanged()
                                //val stringResponse = s.string()
                                lblError.setText("")
                            } else {
                                nbFoldingScanItem.setText("")
                                nbFoldingScanItem.requestFocus()
                                lblError.setTextColor(R.color.design_default_color_error)
                                lblError.text = ErrorMsg
                                //(ErrorMsg)
                            }

                            nbFoldingScanItem.isEnabled=true
                            UpdatingText=false
                        },
                        {t:Throwable?->
                            run {
                                lblError.setText(t?.message)
                                nbFoldingScanItem.isEnabled=true
                                UpdatingText=false
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            lblError.setText(e?.message)
            nbFoldingScanItem.isEnabled=true
            throw(IOException("FoldingScan Activity - PostFoldingScanItem", e))
        }
        finally {
            nbFoldingScanItem.isEnabled=true
        }
    }

}