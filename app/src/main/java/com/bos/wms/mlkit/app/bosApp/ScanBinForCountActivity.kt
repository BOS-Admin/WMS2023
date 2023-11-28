package com.bos.wms.mlkit.app.bosApp


import Remote.APIClient
import Remote.BasicApi
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.General.hideSoftKeyboard
import com.bos.wms.mlkit.R
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import java.io.IOException

class ScanBinForCountActivity : AppCompatActivity() {
    private lateinit var txtScanBox: EditText
    private lateinit var txtScanUser: EditText
    private lateinit var txtScanDestination: EditText
    private lateinit var lblScanDestination: TextView
    private lateinit var textUser: TextView
    private lateinit var textBranch: TextView
    private lateinit var lblError1: TextView

    private lateinit var TextChangeEvent:TextWatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_container)
        mStorage = Storage(applicationContext)
        IPAddress= General.getGeneral(applicationContext).ipAddress
        txtScanBox=findViewById(R.id.textBox)
        txtScanUser=findViewById(R.id.textUserScan)
        txtScanDestination=findViewById(R.id.textDestination)
        textUser=findViewById(R.id.textUser)
        textBranch=findViewById(R.id.textBranch)
        lblError1=findViewById(R.id.lblError)
        lblScanDestination=findViewById(R.id.lblDestination)
        val lblDescription:TextView =findViewById(R.id.lblDescription)
        val lblBox:TextView =findViewById(R.id.lblBox)
        if( General.getGeneral(applicationContext).packType=="PaletteBinsCount"){
            title = "Scan Palette";
            lblDescription.text="Please scan your barcode and the palette barcode"
            lblBox.text="Palette"
        }



        txtScanBox.setShowSoftInputOnFocus(false);
        txtScanUser.setShowSoftInputOnFocus(false);
        //txtScanDestination.setShowSoftInputOnFocus(false);
        general= General.getGeneral(applicationContext)
        var userCode: String= general.UserCode
        var branch: String=general.fullLocation
        textUser.text =General.getGeneral(applicationContext).userFullName
        textBranch.text = branch
        txtScanUser.requestFocus()
        txtScanUser.setShowSoftInputOnFocus(false);
        txtScanBox.setShowSoftInputOnFocus(false);
        //txtScanDestination.setShowSoftInputOnFocus(false);
        txtScanDestination.isVisible=false
        lblScanDestination.isVisible=false
        TextChangeEvent=object : TextWatcher {

            @SuppressLint("ResourceAsColor", "SetTextI18n")
            override fun afterTextChanged(s: Editable) {
                if(UpdatingText)
                    return;
                UpdatingText=true;
                var userScanCode:String=txtScanUser.text.toString()
                var boxNb:String=txtScanBox.text.toString()
                //var destination:String=txtScanDestination.text.toString()
                lblError1.text ="";

                if(!General.ValidateUserCode(userScanCode)){
                    txtScanUser.setText("")
                    txtScanUser.requestFocus()
                    UpdatingText=false;
                    return
                }

                if(userCode.toString()!=userScanCode){
                    lblError1.text = "Scanned User: $userScanCode Doesn't Match User Id <$userCode>"
                    lblError1.setTextColor(Color.RED)
                    txtScanUser.setText("")
                    txtScanUser.requestFocus()
                    UpdatingText=false;
                    return
                }

                if( !validateBoxNb(boxNb)){
                    if(boxNb.length>1) {
                        lblError1.setTextColor(Color.RED)
                        lblError1.text = "Invalid Box Number: $boxNb"
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
                proceed(boxNb,userCode);

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
       // txtScanDestination.addTextChangedListener(TextChangeEvent);
    }


    private fun proceed(boxNb:String,destination:String){
        if( General.getGeneral(applicationContext).packType=="PaletteBinsCount")
            ValidatePalette(boxNb,102,General.getGeneral(applicationContext).UserID,General.getGeneral(applicationContext).mainLocationID,destination)
        else
            ValidateBin(boxNb,102,General.getGeneral(applicationContext).UserID,General.getGeneral(applicationContext).mainLocationID,destination)

       // val intent = Intent (applicationContext, CountActivity::class.java)
       // startActivity(intent)
    }




    fun ValidateBin(binBarcode:String,packingTypeId:Int,userId:Int,locationId:Int,destination:String) {

        try {
            lblError1.text = "fetching data..."
            Log.i("ScanBinForCountActivity","started api")
            api= APIClient.getInstance(IPAddress ,true).create(
                BasicApi::class.java)
            compositeDisposable.addAll(
                api.ValidateBinForCount(binBarcode,packingTypeId,userId,locationId)
                    .subscribeOn(Schedulers.io())
                    //.observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            var ErrorMsg = ""
                            try {
                                ErrorMsg = s.string()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                           // Log.i("AH-Log","response  "+s.string())
                           // Log.i("AH-Log","response  "+ErrorMsg.length)
                           // Log.i("AH-Log","response  "+ErrorMsg.isEmpty())

                            if (ErrorMsg.isEmpty() || ErrorMsg=="success" || ErrorMsg.lowercase().startsWith("released") ) {
                                Log.i("AH-Log","response  "+s.string())
                                General.getGeneral(applicationContext).boxNb=binBarcode
                                General.getGeneral(applicationContext).saveGeneral(applicationContext)
                                showMessage("",Color.GREEN)
                                val intent = Intent (applicationContext, CountActivity::class.java)
                                startActivity(intent)
                                finish()

                            } else {
                                showMessage(ErrorMsg,Color.RED)
                              }
                          runOnUiThread{
                              txtScanBox.isEnabled=false
                              UpdatingText=false
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
            showMessage("Exception: "+e?.message,Color.RED)
        }
        finally {
        }
    }

    fun ValidatePalette(binBarcode:String,packingTypeId:Int,userId:Int,locationId:Int,destination:String) {

        try {
            lblError1.text = "fetching data..."
            Log.i("ScanPaletteForCountActivity","started api")
            api= APIClient.getInstance(IPAddress ,true).create(
                BasicApi::class.java)
            compositeDisposable.addAll(
                api.ValidatePaletteForCount(binBarcode,packingTypeId,userId,locationId)
                    .subscribeOn(Schedulers.io())
                    //.observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            var ErrorMsg = ""
                            try {
                                ErrorMsg = s.string()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            // Log.i("AH-Log","response  "+s.string())
                            // Log.i("AH-Log","response  "+ErrorMsg.length)
                            // Log.i("AH-Log","response  "+ErrorMsg.isEmpty())

                            if (ErrorMsg.isEmpty() || ErrorMsg=="success" || ErrorMsg.lowercase().startsWith("released") ) {
                                Log.i("AH-Log","response  "+s.string())
                                General.getGeneral(applicationContext).boxNb=binBarcode
                                General.getGeneral(applicationContext).saveGeneral(applicationContext)
                                showMessage("",Color.GREEN)
                                val intent = Intent (applicationContext, CountActivity::class.java)
                                startActivity(intent)
                                finish()

                            } else {
                                showMessage(ErrorMsg,Color.RED)
                            }
                            runOnUiThread{
                                txtScanBox.isEnabled=false
                                UpdatingText=false
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
            showMessage("Exception: "+e?.message,Color.RED)
        }
        finally {
        }
    }

    private fun showMessage(msg:String,color:Int){
        if(color==Color.RED)
            Beep()
        runOnUiThread{
            lblError1.setTextColor(color)
            lblError1.text = msg
        }
    }

    private fun validateBoxNb(box:String):Boolean{

        if(!General.ValidateBinCode(box))
            return false
        var success=false
        for(s in general.BoxNbDigits.split(","))
            if(box.length==Integer.parseInt(s)){
                success= true
                break
            }
        if(success)
            for(s in general.BoxStartsWith.split(","))
                if(box.startsWith(s))
                    return true
        return false
    }

    private lateinit var general: General

    var compositeDisposable= CompositeDisposable()
    var UpdatingText:Boolean=false
    lateinit var  mStorage:Storage ;
    lateinit var api: BasicApi
    var IPAddress =""

    private fun Beep(){
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300)
    }

}