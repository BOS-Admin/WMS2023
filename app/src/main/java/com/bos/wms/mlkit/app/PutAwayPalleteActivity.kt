package com.bos.wms.mlkit.app

import Model.PutPalleteNextShelfModel
import Model.PutPalleteNextShelfModelResponse
import Model.PutPalleteOnShelfModel
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
import kotlinx.android.synthetic.main.content_putawaypallete.*
import okhttp3.ResponseBody
import java.io.IOException


class PutAwayPalleteActivity : AppCompatActivity() {
    private lateinit var linearLayoutManager: LinearLayoutManager

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var TextChangeEvent:TextWatcher
    private lateinit var ShelfItems:ArrayList<String>
    private lateinit var BinItems:ArrayList<String>
    private lateinit var adp:CustomListAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_putawaypallete)
        BinItems= arrayListOf<String>()
        mStorage = Storage(applicationContext) //sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")
        ShelfItems= ArrayList();
        txtputawaypalleteShelfBarcode.setShowSoftInputOnFocus(false);
        txtPutAwayPalleteBarcode.setShowSoftInputOnFocus(false);

        txtPutAwayPalleteBarcode.requestFocus()
        txtputawaypalleteShelfBarcode.setShowSoftInputOnFocus(false);
        txtPutAwayPalleteBarcode.setShowSoftInputOnFocus(false);

        TextChangeEvent=object : TextWatcher {
            var UpdatingText:Boolean=false
            @SuppressLint("ResourceAsColor")
            override fun afterTextChanged(s: Editable) {
                if(UpdatingText)
                    return;
                UpdatingText=true;
                var seqVal:Int=0
                var shelfStr:String=txtputawaypalleteShelfBarcode.text.toString()
                var PalleteStr:String=txtPutAwayPalleteBarcode.text.toString()


                if(!General.ValidatePalleteCode(PalleteStr)){
                    seqVal=0
                }
                else if(General.ValidateShelfCode(shelfStr)){
                    seqVal=2
                }
                else{
                    seqVal=1
                }
               when (seqVal) {
                    0 -> {
                        txtPutAwayPalleteBarcode.setText("")
                        txtputawaypalleteShelfBarcode.setText("")
                        txtPutAwayPalleteBarcode.requestFocus()
                    }
                    1 -> {
                        Initputawaypallete(PalleteStr)
                        txtputawaypalleteShelfBarcode.setText("")
                        txtputawaypalleteShelfBarcode.requestFocus()
                    }
                    2 -> {
                        var UserID: Int=General.getGeneral(applicationContext).UserID
                        var model=PutPalleteOnShelfModel(PalleteStr,shelfStr,UserID)
                        PostputawaypalleteShelf(model)

                    }
               }
                UpdatingText=false;

                txtPutAwayPalleteBarcode.setShowSoftInputOnFocus(false);
                txtputawaypalleteShelfBarcode.setShowSoftInputOnFocus(false);
            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {

            }
        }
        hideSoftKeyboard(this)
        txtputawaypalleteShelfBarcode.addTextChangedListener(TextChangeEvent);
        txtPutAwayPalleteBarcode.addTextChangedListener(TextChangeEvent);
    }
    var LastPallete:String="azsthnj"
    lateinit var api: BasicApi
    var compositeDisposable= CompositeDisposable()
    lateinit var NextShelf:PutPalleteNextShelfModelResponse
    lateinit var  mStorage:Storage ;  //sp存储
    var IPAddress =""
    private fun Beep(){
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300)
    }
    fun Initputawaypallete(PalleteCode:String) {

        try {
            if(PalleteCode.equals(LastPallete))
                return;

            // TODO: handle loggedInUser authentication
            var UserID: Int=General.getGeneral(applicationContext).UserID
            var FloorID: Int=General.getGeneral(applicationContext).FloorID
            var model=PutPalleteNextShelfModel(PalleteCode,-1,UserID,FloorID)
            api= APIClient.getInstance(IPAddress ,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.PutPalleteNextShelf(model)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            NextShelf=s
                            ShelfItems.clear()
                            if(s.rackCellID<0){
                                ShelfItems.add(s.locationBarCode)
                            }
                            else{
                                ShelfItems.add(s.userDefinedBarcode)
                                LastPallete=PalleteCode
                            }


                            linearLayoutManager = LinearLayoutManager(this)
                            recyclerView.layoutManager = linearLayoutManager
                            recyclerView.setLayoutManager(LinearLayoutManager(this))
                            adp= CustomListAdapter(this,ShelfItems)
                            recyclerView.setAdapter(adp)
                            RefreshLabels()
                            if(s.rackCellID<0){
                                lblError.setText(s.locationBarCode)
                                lblError.setTextColor(ColorRed)
                                General.playError()
                                txtPutAwayPalleteBarcode.setText("")
                                txtputawaypalleteShelfBarcode.setText("")
                                txtPutAwayPalleteBarcode.requestFocus()
                            }
                            else{
                                lblError.setText("")
                                lblError.setTextColor(ColorGreen)
                                General.playSuccess()
                            }

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
            throw(IOException("putawaypallete Activity - GetputawaypalleteListItems", e))
        }
        finally {
        }
    }
    var ColorGreen = Color.parseColor("#52ac24")
    var ColorRed = Color.parseColor("#ef2112")
    var ColorWhite = Color.parseColor("#ffffff")
    fun RefreshLabels(){
        lblError.text = ""
      if(0==0){
            lblputawaypalleteListView.setText("---Putaway Pallete---")
            btnputawaypalleteDone.visibility= View.VISIBLE
            btnputawaypalleteDone.setOnClickListener { finish() }
        }
        hideSoftKeyboard(this)
    }
    @SuppressLint("ResourceAsColor")
    fun PostputawaypalleteShelf(Model:PutPalleteOnShelfModel) {

        try {
            // TODO: handle loggedInUser authentication


            api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.PutPalleteOnShelf(Model)
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
                                RefreshLabels()
                                txtPutAwayPalleteBarcode.setText("")
                                txtputawaypalleteShelfBarcode.setText("")
                                txtPutAwayPalleteBarcode.requestFocus()
                                LastPallete="gyuyun"
                                //val stringResponse = s.string()
                                lblError.setText("Success")
                                ShelfItems.clear()
                                lblError.setTextColor(ColorGreen)
                                General.playSuccess()
                            } else if (!ErrorMsg.isEmpty()) {
                                txtputawaypalleteShelfBarcode.setText("")
                                txtputawaypalleteShelfBarcode.requestFocus()
                                lblError.setTextColor(ColorRed)
                                lblError.text = ErrorMsg
                                General.playError()
                                //(ErrorMsg)
                            }

                            txtPutAwayPalleteBarcode.isEnabled=true
                            txtputawaypalleteShelfBarcode.isEnabled=true
                        },
                        {t:Throwable?->
                            run {
                                lblError.setText(t?.message)
                                txtPutAwayPalleteBarcode.isEnabled=true
                                txtputawaypalleteShelfBarcode.isEnabled=true
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            lblError.setText(e?.message)
            txtPutAwayPalleteBarcode.isEnabled=true
            txtputawaypalleteShelfBarcode.isEnabled=true
            throw(IOException("Putawaypallete Activity - Postputawaypallete", e))
        }
        finally {
        }
    }

}