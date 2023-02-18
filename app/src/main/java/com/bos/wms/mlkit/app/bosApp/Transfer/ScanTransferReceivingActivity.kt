package com.bos.wms.mlkit.app.bosApp.Transfer

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
import java.io.IOException

class ScanTransferReceivingActivity : AppCompatActivity() {

    private lateinit var TextChangeEvent:TextWatcher
    private lateinit var txtScanBox: EditText
    private lateinit var txtScanUser: EditText
    private lateinit var txtScanDestination: EditText
    private lateinit var textUser: TextView
    private lateinit var textBranch: TextView
    private lateinit var lblError1: TextView
    private lateinit var general: General
    private lateinit var lblScanDestination: TextView
    private lateinit var lblBox: TextView
    private lateinit var lblDescription: TextView
    private val fakeLocation=""

    private fun proceed(transferId:String){
        ValidateBin(transferId)

    }




    fun ValidateBin(transferNavNo:String) {

        try {
            lblError1.text = "fetching data..."
            Log.i("ScanTransferReceivingActivity","started api")
            api= APIClient.getInstance(General.getGeneral(applicationContext).ipAddress ,true).create(
                BasicApi::class.java)
            compositeDisposable.addAll(
                api.ValidateReceiving(if(fakeLocation=="") general.mainLocation else fakeLocation,transferNavNo)
                    .subscribeOn(Schedulers.io())
                    // .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            var response = try {
                                s.string()
                            } catch (e: IOException) {
                                e.message.toString()
                            }

                            if (response!=null && response.lowercase().startsWith("receiving allowed")) {
                                runOnUiThread{
                                    lblError1.setTextColor(Color.GREEN)
                                    lblError1.text=response
                                    Log.i("Receiving",response)
                                    general.transferNavNo=transferNavNo
                                    val intent = Intent (applicationContext, TransferReceivingActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                            }
                            else{
                                runOnUiThread{
                                    showMessage(response,Color.RED)
                                    Log.i("Receiving",response)
                                }

                            }


                        },
                        {t:Throwable?->
                            run {
                                if(t is HttpException){
                                    var ex: HttpException =t as HttpException
                                    showMessage( ex.response().errorBody()!!.string()+" (API Http Error)",Color.RED)

                                }
                                else{
                                    showMessage( t?.message+" (API Error)",Color.RED)
                                }


                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            showMessage( "Error: " + e?.message,Color.RED)

            //throw(IOException("UPC Pricing Activity - ValidateScan", e))
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

    private fun Beep(){
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300)
    }



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
        txtScanBox.setShowSoftInputOnFocus(false);
        txtScanUser.setShowSoftInputOnFocus(false);
        txtScanDestination.setShowSoftInputOnFocus(false);
        general=General.getGeneral(applicationContext)
        var userCode: String= general.UserCode
        var branch: String=general.fullLocation
        textUser.text =general.userFullName
        textBranch.text = branch
        txtScanUser.requestFocus()
        txtScanUser.setShowSoftInputOnFocus(false);
        txtScanBox.setShowSoftInputOnFocus(false);
        txtScanDestination.setShowSoftInputOnFocus(false);

        lblScanDestination=findViewById(R.id.lblDestination)
        txtScanDestination.isVisible=false
        lblScanDestination.isVisible=false
        lblDescription=findViewById(R.id.lblDescription)
        lblBox=findViewById(R.id.lblBox)
        lblDescription.text="Please scan your barcode and the transfer barcode"
        lblBox.text = "Transfer Barcode"

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

               if(boxNb==null || boxNb=="") {
                    txtScanBox.setText("")
                    txtScanBox.requestFocus()
                    UpdatingText=false;
                    return
                }
                if(boxNb.length < 2) {
                    lblError1.setTextColor(Color.RED)
                    lblError1.text = "Inavlid Transfer Number: " + boxNb
                    txtScanBox.setText("")
                    txtScanBox.requestFocus()
                    UpdatingText=false;
                    return
                }

                UpdatingText=false
                txtScanUser.setShowSoftInputOnFocus(false);
                txtScanBox.setShowSoftInputOnFocus(false);
                txtScanDestination.setShowSoftInputOnFocus(false);
                proceed(boxNb);

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

    }


}