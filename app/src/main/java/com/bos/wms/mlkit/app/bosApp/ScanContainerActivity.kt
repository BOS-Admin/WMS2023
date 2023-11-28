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

class ScanContainerActivity : AppCompatActivity() {

    private lateinit var TextChangeEvent:TextWatcher
    private lateinit var txtScanBox: EditText
    private lateinit var txtScanUser: EditText
    private lateinit var txtScanDestination: EditText
    private lateinit var textUser: TextView
    private lateinit var textBranch: TextView
    private lateinit var lblError1: TextView
    private lateinit var general: General


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_container)
        mStorage = Storage(applicationContext)
        txtScanBox=findViewById(R.id.textBox)
        txtScanUser=findViewById(R.id.textUserScan)
        txtScanDestination=findViewById(R.id.textDestination)
        textUser=findViewById(R.id.textUser)
        textBranch=findViewById(R.id.textBranch)
        val lblDescription:TextView =findViewById(R.id.lblDescription)
        val lblBox:TextView =findViewById(R.id.lblBox)
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


        if( General.getGeneral(applicationContext).packType=="PaletteBinsDC"){
            title = "Scan Palette";
            lblDescription.text="Please scan your barcode and the palette barcode"
            lblBox.text="Palette"

        }

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
                        lblError1.text = "Inavlid Box Number: " + boxNb
                    }
                    txtScanBox.setText("")
                    txtScanBox.requestFocus()
                    UpdatingText=false;
                    return
                }

                if( !General.ValidateDestination(destination)){
                    if(destination.length>1){
                        lblError1.setTextColor(Color.RED)
                        lblError1.text = "Inavlid Box destination: "+destination
                    }

                    txtScanDestination.setText("")
                    txtScanDestination.requestFocus()
                    UpdatingText=false;
                    return
                }

                UpdatingText=false
                txtScanUser.setShowSoftInputOnFocus(false);
                txtScanBox.setShowSoftInputOnFocus(false);
                txtScanDestination.setShowSoftInputOnFocus(false);
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

       if( General.getGeneral(applicationContext).packType=="PaletteBinsDC")
           ValidatePalette(boxNb,102,General.getGeneral(applicationContext).UserID,General.getGeneral(applicationContext).mainLocationID,destination)
        else
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
                api.ValidateBin(binBarcode,packingTypeId,userId,locationId)
                    .subscribeOn(Schedulers.io())
                    // .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            if(s!==null ){
                                Log.i("AH-Log-Pack","size "+s.binBarcode)
                                General.getGeneral(applicationContext).boxNb=binBarcode
                                General.getGeneral(applicationContext).destination=destination
                                General.getGeneral(applicationContext).saveGeneral(applicationContext)
                                lblError1.setTextColor(Color.GREEN)
                                lblError1.text = "Success"
                                val intent = Intent (applicationContext, PackingReasonActivity::class.java)
                                startActivity(intent)
                                finish()

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

    fun ValidatePalette(binBarcode:String,packingTypeId:Int,userId:Int,locationId:Int,destination:String) {

        try {
            lblError1.text = "fetching data..."
            Log.i("ScanPaletteActivity","started api")
            api= APIClient.getInstance(General.getGeneral(applicationContext).ipAddress ,true).create(
                BasicApi::class.java)
            compositeDisposable.addAll(
                api.ValidatePalette(binBarcode,packingTypeId,userId,locationId)
                    .subscribeOn(Schedulers.io())
                    // .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            if(s!==null ){
                                Log.i("AH-Log-Pack","size "+s.binBarcode)
                                General.getGeneral(applicationContext).boxNb=binBarcode
                                General.getGeneral(applicationContext).destination=destination
                                General.getGeneral(applicationContext).saveGeneral(applicationContext)
                                lblError1.setTextColor(Color.GREEN)
                                lblError1.text = "Success"
                                val intent = Intent (applicationContext, PaletteBinsDCActivity::class.java)
                                startActivity(intent)
                                finish()

                            }
                            else{
                                showMessage("API Error: Palette Model is Empty",Color.RED)
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
        var success=false
        Log.i("Ah-Log", general.BoxNbDigits)
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

    private fun Beep(){
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300)
    }


}