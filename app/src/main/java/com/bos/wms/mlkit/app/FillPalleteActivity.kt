package com.bos.wms.mlkit.app

import Model.FillPalleteModel
import Model.FillPalleteModelBin
import Model.FillPalleteModelBinValidation
import Model.FillPalleteModelBinValidationResponse
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
import kotlinx.android.synthetic.main.content_fillpallete.*
import java.io.IOException
import java.lang.Exception
import java.util.logging.Logger


class FillPalleteActivity : AppCompatActivity() {
    private lateinit var linearLayoutManager: LinearLayoutManager

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var TextChangeEvent:TextWatcher
    private lateinit var PalleteCode:String
    private lateinit var Bins:ArrayList<String>
    private lateinit var ValidatedBins:ArrayList<FillPalleteModelBinValidationResponse>
    private lateinit var PostPalleteBins:ArrayList<FillPalleteModelBin>
    private var  BinTypeID:Int? = null
    private var  MaxPalleteNbOfBins:Int? = null
    private var  MinPalleteNbOfBins:Int? = null

    private lateinit var adp:CustomListAdapter
    private var  NbOfBins:Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fillpallete)
        Bins= arrayListOf()
        ValidatedBins= arrayListOf()
        Bins= arrayListOf()
        PostPalleteBins= arrayListOf()
        mStorage = Storage(applicationContext) //sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")


        txtPalleteBarcode.setShowSoftInputOnFocus(false);
        txtfillpalleteBinBarcode.setShowSoftInputOnFocus(false);

        txtPalleteBarcode.requestFocus()
        txtPalleteBarcode.setShowSoftInputOnFocus(false);
        txtfillpalleteBinBarcode.setShowSoftInputOnFocus(false);

        TextChangeEvent=object : TextWatcher {
            var UpdatingText:Boolean=false
            @SuppressLint("ResourceAsColor")
            override fun afterTextChanged(s: Editable) {
                if(UpdatingText)
                    return;
                UpdatingText=true;
                var PalleteStr:String=txtPalleteBarcode.text.toString()
                var binStr:String=txtfillpalleteBinBarcode.text.toString()
                lblError.setText("")

                if(Bins.contains(binStr)){
                    lblError.setText("Bin Already Added to Pallete")
                    txtfillpalleteBinBarcode.setText("")
                    txtfillpalleteBinBarcode.requestFocus()
                }
                else if(General.ValidatePalleteCode(PalleteStr) && General.ValidateBinCode(binStr)){

                    ValidateFillPalleteBin(PalleteStr,binStr)
                    PalleteCode=PalleteStr
                    adp= CustomListAdapter(applicationContext,Bins)
                    recyclerView.setAdapter(adp)
                    txtfillpalleteBinBarcode.requestFocus()
                }else{
                    if(!General.ValidateBinCode(binStr)){
                        txtfillpalleteBinBarcode.setText("")
                        txtfillpalleteBinBarcode.requestFocus()
                    }
                    if(!General.ValidatePalleteCode(PalleteStr)){
                        txtPalleteBarcode.setText("")
                        txtPalleteBarcode.requestFocus()
                    }
                }
                UpdatingText=false;
                RefreshLabels()
                txtfillpalleteBinBarcode.setShowSoftInputOnFocus(false);
                txtPalleteBarcode.setShowSoftInputOnFocus(false);
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
        txtPalleteBarcode.addTextChangedListener(TextChangeEvent);
        txtfillpalleteBinBarcode.addTextChangedListener(TextChangeEvent);
    }
    lateinit var api: BasicApi
    var compositeDisposable= CompositeDisposable()

    lateinit var  mStorage:Storage ;  //sp存储
    var IPAddress =""

    private fun Beep(){
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300)
    }
    fun ValidateFillPalleteBin(PalleteCode:String, BinCode:String) {
        try {
            // TODO: handle loggedInUser authentication
            var FloorID: Int=General.getGeneral(applicationContext).FloorID
            var UserID: Int=General.getGeneral(applicationContext).UserID
            var model=FillPalleteModelBinValidation(PalleteCode,BinCode,UserID,BinTypeID?: -1,NbOfBins)
            api= APIClient.getInstance(IPAddress ,false).create(BasicApi::class.java)
            txtfillpalleteBinBarcode.setText("")
            txtfillpalleteBinBarcode.requestFocus()
            compositeDisposable.addAll(
                api.FillPalleteBinValidation(model)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            var resp=s
                            if(resp.statusID>0){
                            BinTypeID=BinTypeID?:resp.binTypeID
                            MaxPalleteNbOfBins=MaxPalleteNbOfBins?:resp.maxPalleteBins
                            MinPalleteNbOfBins=MinPalleteNbOfBins?:resp.minPalleteBins

                            Bins.add(0,resp.binCode)
                            ValidatedBins.add(0,resp)
                            PostPalleteBins.add(0, FillPalleteModelBin(resp.binCode))
                            NbOfBins=NbOfBins+1
                            lblError.setText("Bin Added")
                            }
                            else{
                                lblError.setText(resp.status)
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
            throw(IOException("fillpallete Activity - GetfillpalleteListItems", e))
        }
        finally {
        }
    }
    fun RefreshLabels(){

        recyclerView.setLayoutManager(LinearLayoutManager(this))
        adp= CustomListAdapter(this,Bins)
        recyclerView.setAdapter(adp)
        var UserID: Int=General.getGeneral(applicationContext).UserID

        if(NbOfBins>=MaxPalleteNbOfBins?:50){
            txtfillpalleteBinBarcode.isEnabled=false
        }
        if(NbOfBins>=MinPalleteNbOfBins?:1000 && NbOfBins<=MaxPalleteNbOfBins?:0){
            btnfillpalleteDone.visibility= View.VISIBLE
            btnfillpalleteDone.setOnClickListener {
                try {
                    var BinsStr=Bins.joinToString()

                    var model=FillPalleteModel(PalleteCode,BinsStr,UserID,BinTypeID!!,PostPalleteBins)
                    Postfillpallete(model)
                }
                catch (ex:Exception){
                    lblError.setText(ex.message)
                }

            }
        }
       /* var RackCode:Int=fillpalleteModel.rackCode
        var maxRack:Int=fillpalleteListItems.maxOf { it.rackNb }
        lblfillpalleteRack.setText("Rack: "+RackCode.toString())
        lblfillpalleteWave.setText("Wave:"+fillpalleteModel.waveNb.toString())
        var bins:Int=fillpalleteListItems.distinctBy { it.binCode }.count()
        var remainBins:Int=fillpalleteListItems.filter { !it.Donefillpallete }.distinctBy { it.binCode }.count()
        lblfillpalleteReminaingBins.setText("Bins:"+ remainBins.toString() + "/"+ bins.toString())
        var items:Int=fillpallet4eListItems.distinctBy { it.itemSerialID }.count()
        var remainItems:Int=fillpalleteListItems.filter { !it.Donefillpallete }.distinctBy { it.itemSerialID }.count()
        lblfillpalleteReminaingItems.setText("Items:"+ remainItems.toString() + "/"+ items.toString())
        if(remainItems==0){
            lblfillpalleteListView.setText("---fillpallete Completed---")
            btnfillpalleteDone.visibility= View.VISIBLE
            btnfillpalleteDone.setOnClickListener { finish() }
        }*/
        hideSoftKeyboard(this)
    }
    @SuppressLint("ResourceAsColor")
    fun Postfillpallete(Model:FillPalleteModel) {
        try {
            api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.FillPallete(Model)
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
                                builder.setTitle("Fill Pallete Done!!")
                                builder.setMessage("Fill Pallete Done, Do you want to start a new list?")
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
                            txtfillpalleteBinBarcode.isEnabled=true
                            txtPalleteBarcode.isEnabled=true
                        },
                        {t:Throwable?->
                            run {
                                lblError.setText(t?.message)
                                txtfillpalleteBinBarcode.isEnabled=true
                                txtPalleteBarcode.isEnabled=true
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            lblError.setText(e?.message)
            txtfillpalleteBinBarcode.isEnabled=true
            txtPalleteBarcode.isEnabled=true
            throw(IOException("fillpallete Activity - PostfillpalleteShelf", e))
        }
        finally {
        }
    }
}