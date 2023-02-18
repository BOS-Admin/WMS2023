package com.bos.wms.mlkit.app.bosApp

import Remote.APIClient
import Remote.BasicApi
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.General.hideSoftKeyboard
import com.bos.wms.mlkit.R
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isVisible

class ScanBoxForStockTakeActivity : AppCompatActivity() {

    private lateinit var TextChangeEvent:TextWatcher
    private lateinit var txtScanBox: EditText
    private lateinit var txtScanUser: EditText
    private lateinit var txtScanDestination: EditText
    private lateinit var textUser: TextView
    private lateinit var textBranch: TextView
    private lateinit var lblError1: TextView
    private lateinit var lblBox: TextView
    private lateinit var general: General
    private var stockTakeType:Int=-1
    private lateinit var lblScanDestination: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_container)
        mStorage = Storage(applicationContext)
        txtScanBox=findViewById(R.id.textBox)
        txtScanUser=findViewById(R.id.textUserScan)
        txtScanDestination=findViewById(R.id.textDestination)
        textUser=findViewById(R.id.textUser)
        textBranch=findViewById(R.id.textBranch)
        lblError1=findViewById(R.id.lblError)
        lblBox=findViewById(R.id.lblBox)
        txtScanBox.setShowSoftInputOnFocus(false);
        txtScanUser.setShowSoftInputOnFocus(false);
        //txtScanDestination.setShowSoftInputOnFocus(false);
        general=General.getGeneral(applicationContext)
        var userCode: String= general.UserCode
        var branch: String=general.fullLocation
        stockTakeType=general.stockType
        if(stockTakeType==101)
            lblBox.text = "Stand"
        else
            lblBox.text = "Box"
        textUser.text = userCode.toString() + ", " + General.getGeneral(applicationContext).UserName
        textBranch.text = branch
        txtScanUser.requestFocus()
        txtScanUser.setShowSoftInputOnFocus(false);
        txtScanBox.setShowSoftInputOnFocus(false);
       // txtScanDestination.setShowSoftInputOnFocus(false);

        lblScanDestination=findViewById(R.id.lblDestination)
        txtScanDestination.isVisible=false
        lblScanDestination.isVisible=false

        TextChangeEvent=object : TextWatcher {

            @SuppressLint("ResourceAsColor")
            override fun afterTextChanged(s: Editable) {
                if(UpdatingText)
                    return;
                UpdatingText=true;
                var userScanCode:String=txtScanUser.text.toString()
                var boxNb:String=txtScanBox.text.toString()
                var destination:String=txtScanDestination.text.toString()
                lblError1.text ="";

                if(!General.ValidateUserCode(userScanCode)){
                    txtScanUser.setText("")
                    txtScanUser.requestFocus()
                    UpdatingText=false;
                    return
                }

                if(userCode.toString()!=userScanCode){
                    lblError1.text = "Scanned User: "+userScanCode+" Doesn't Match User Id <"+userCode+">"
                    lblError1.setTextColor(Color.RED)
                    txtScanUser.setText("")
                    txtScanUser.requestFocus()
                    UpdatingText=false;
                    return
                }

                if(!validateBoxNb(boxNb)){
                    if(boxNb.length>1) {
                        lblError1.setTextColor(Color.RED)
                        lblError1.text = "Invalid Scan: " + boxNb
                    }
                    txtScanBox.setText("")
                    txtScanBox.requestFocus()
                    UpdatingText=false;
                    return
                }

                UpdatingText=false
                txtScanUser.setShowSoftInputOnFocus(false);
                txtScanBox.setShowSoftInputOnFocus(false);
               // txtScanDestination.setShowSoftInputOnFocus(false);
                proceed(boxNb , destination);

            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {

            }
        }
        hideSoftKeyboard(this)
        txtScanUser.addTextChangedListener(TextChangeEvent);
        txtScanBox.addTextChangedListener(TextChangeEvent);
        txtScanDestination.addTextChangedListener(TextChangeEvent);
    }

    private fun proceed(boxNb:String,destination:String){
        ValidateBin(boxNb,102,General.getGeneral(applicationContext).UserID,General.getGeneral(applicationContext).mainLocationID,destination)
        // val intent = Intent (applicationContext, PackingReasonActivity::class.java)
        //  startActivity(intent)
    }




    fun ValidateBin(binBarcode:String,packingTypeId:Int,userId:Int,locationId:Int,destination:String) {

        try {
            lblError1.text = "fetching data..."
            Log.i("ScanBinActivity","started api")
            api= APIClient.getInstance(General.getGeneral(applicationContext).ipAddress ,true).create(
                BasicApi::class.java)
            compositeDisposable.addAll(
                api.ValidateBinForStockTake(binBarcode,101,userId,locationId)
                    .subscribeOn(Schedulers.io())
                    // .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            if(s!==null ){
                                Log.i("AH-Log-Pack","type "+s.binTypeId)
                                General.getGeneral(applicationContext).boxNb=binBarcode


                                if(s.binTypeId==100|| s.binTypeId==101){


                                    if(general.stockType==100 && s.binTypeId!=100 ){
                                        showMessage("Invalid: $binBarcode, Type IS Not Box , (Input Type=${s.binTypeId})",Color.RED)
                                    }
                                    else if(general.stockType==101 && s.binTypeId!=101 ){
                                        showMessage("Invalid: $binBarcode, Type IS Not Stand , (Input Type=${s.binTypeId})",Color.RED)
                                    }
                                    else{

                                        lblError1.setTextColor(Color.GREEN)
                                        lblError1.text = "Success"
                                        general.stockType=s.binTypeId
                                        General.getGeneral(applicationContext).saveGeneral(applicationContext)
                                        val intent = Intent (applicationContext, StockTakeDCActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }



                                }
                               else{

                                    showMessage("Invalid: $binBarcode, Type IS Not Box/Stand , (Input Type=${s.binTypeId})",Color.RED)


                                }


                            }
                            else{
                                showMessage("API Error: Bin Model is Empty",Color.RED)

                            }

                        },
                        {t:Throwable?->
                            run {

                                if(t is HttpException){
                                    var ex: HttpException =t as HttpException
                                    showMessage( ex.response().errorBody()!!.string()+ " (Http Error) ",Color.RED)
                                }
                                else{
                                    if(t?.message!=null)
                                        showMessage(t.message.toString()+ " (API Error) ",Color.RED)
                                }

                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            showMessage(""+e?.message+"(Exception)" ,Color.RED)
        }
        finally {
        }
    }


    var compositeDisposable= CompositeDisposable()
    var UpdatingText:Boolean=false
    lateinit var  mStorage:Storage ;
    lateinit var api: BasicApi

    private fun showMessage(msg:String,color:Int){
        if(color == Color.RED)
            Beep()
        runOnUiThread{
            lblError1.setTextColor(color)
            lblError1.text = msg
        }
    }


    private fun validateBoxNb(box:String):Boolean{

        if(!General.ValidateBinCode(box))
            return false
        return true
    }

    private fun Beep(){
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300)
    }


}