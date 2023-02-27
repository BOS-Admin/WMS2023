package com.bos.wms.mlkit.app.bosApp.Transfer

import Model.BosApp.Transfer.TransferShipmentItemModel
import Model.BosApp.Transfer.TransferShipmentModel
import Remote.APIClient
import Remote.BasicApi
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.R
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import java.io.IOException


class ShipmentActivity : AppCompatActivity() {

    fun ValidateScan(box:String) {

        try {

            if(toLocation==null || toLocation.trim()=="" || boxes==null )
            {
                updatingText=false
                updatingText1=false
                textLocationScanned.requestFocus()
                return
            }

            textBoxScanned.isEnabled=false
            textLocationScanned.isEnabled=false
            btnDone.isEnabled=false
            btnDelete.isEnabled=false

            api= APIClient.getInstance(IPAddress ,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.ValidateBinForTransfer(box,general.mainLocation,toLocation)
                    .subscribeOn(Schedulers.io())
                    // .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            var response = try {
                                s.string()
                            } catch (e: IOException) {
                                e.message.toString()
                            }

                            if (response!=null && response=="success") {
                                runOnUiThread{
                                    lblScanError.setTextColor(Color.GREEN)
                                    lblScanError.text =response
                                    index=boxes.size
                                    boxes[index] = box
                                    textLastBox.text=box
                                    textLastLocation.text=toLocation
                                    Log.i("Shipment",response)
                                }
                            }
                            else{
                                runOnUiThread{

                                    showScanMessage(response,Color.RED)
                                    Log.i("Shipment",response)
                                }

                            }
                            runOnUiThread{
                                updatingText=true
                                textBoxScanned.setText("")
                                updatingText=false
                                textBoxScanned.isEnabled=true
                                textLocationScanned.isEnabled=true
                                textBoxScanned.requestFocus()
                                btnDone.isEnabled=true
                                btnDelete.isEnabled=true
                            }

                        },
                        {t:Throwable?->
                            run {
                                if(t is HttpException){
                                    var ex: HttpException =t as HttpException
                                    showScanMessage( ex.response().errorBody()!!.string()+" (API Http Error)",Color.RED)

                                }
                                else{
                                    showScanMessage( t?.message+" (API Error)",Color.RED)
                                }

                                runOnUiThread{
                                    updatingText=true
                                    textBoxScanned.setText("")
                                    updatingText=false
                                    textBoxScanned.isEnabled=true
                                    textLocationScanned.isEnabled=true
                                    textBoxScanned.requestFocus()
                                    btnDone.isEnabled=true
                                    btnDelete.isEnabled=true
                                }

                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            showScanMessage( "Error: " + e?.message,Color.RED)
            runOnUiThread{
                updatingText=true
                textBoxScanned.setText("")
                updatingText=false
                textBoxScanned.isEnabled=true
                textLocationScanned.isEnabled=true
                textBoxScanned.requestFocus()
                btnDone.isEnabled=true
                btnDelete.isEnabled=true
            }
            //throw(IOException("UPC Pricing Activity - ValidateScan", e))
        }
        finally {


        }
    }



    fun removeLastItem(){
        boxes.remove(boxes.size-1)
        index=boxes.size-1
        if(index < 0){
            textLastBox.text=""
            textLastLocation.text=""
            return
        }

        else {
            textLastBox.text = boxes[index]
            textLastLocation.text = toLocation
        }
    }


    private fun proceed() {

        try {

            if(toLocation==null || toLocation.trim()=="" || boxes==null || boxes.size==0)
            {
                showScanMessage("Location or Bins is empty",Color.RED)
                updatingText=false
                updatingText1=false
                textBoxScanned.requestFocus()
                textLocationScanned.requestFocus()
                return
            }


            btnDone.isEnabled = false
            btnDelete.isEnabled=false
            lblScanError.text=""
            var bins:String=boxes.values.joinToString(",")
            //Log.i("Ah-log",itemsStr);
            textBoxScanned.isEnabled=false

            var modelItems:ArrayList<TransferShipmentItemModel>  = arrayListOf()
            for(it in boxes)
                modelItems.add(TransferShipmentItemModel(it.value))

            var model=
                TransferShipmentModel (general.mainLocation,toLocation,general.UserID,modelItems)


            api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.TransferShipment(model)
                    .subscribeOn(Schedulers.io())
                    // .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->

                            var response = try {
                                s.string()
                            } catch (e: IOException) {
                                e.message.toString()
                            }

                            if(response==null || response=="")
                                showMessage("Empty Response",Color.RED)
                             else {

                                    showMessage("Success \n$response",Color.GREEN)
                                }



                            runOnUiThread{
                                boxes.clear()
                               // textLastItem.text = ""
                                index=-1;
                                btnDone.isVisible=false
                                btnDelete.isVisible=false
                            }

                        },
                        {t:Throwable?->
                            run {
                                if(t is HttpException){
                                    var ex: HttpException =t as HttpException
                                    showMessage( ex.response().errorBody()!!.string()+" (APi Http Error)",Color.RED)
                                }
                                else{
                                    if(t?.message!=null)
                                        showMessage(t.message.toString()+ " (API Error )" ,Color.RED)
                                }
                                runOnUiThread{
                                    btnDone.isVisible=false
                                    btnDelete.isVisible=false

                                }
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
          showMessage("Error: "+e?.message,Color.RED)

        }
        finally {

        }
    }





    private fun showScanMessage(msg:String,color:Int){
        if(color==Color.RED)
            Beep()
        runOnUiThread{
            AlertDialog.Builder(this)
                .setTitle("Box Error")
                .setMessage(msg)
                .setPositiveButton("OK") { _, _ ->
//                    val intent = Intent (applicationContext, PackingActivity::class.java)
//                    startActivity(intent)
                    //finish()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
               // .setCancelable(false)
                .show()
            lblScanError.setTextColor(color)
            lblScanError.text = msg
        }
    }

    private fun showMessage(msg:String,color:Int){
        if(color==Color.RED)
            Beep()
        runOnUiThread{
            AlertDialog.Builder(this)
                .setTitle("Result")
                .setMessage(msg)
                .setPositiveButton("OK") { _, _ ->
                    finish()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .show()
            lblError.setTextColor(color)
            lblError.text = msg
        }
    }


    private fun Beep(){
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shipment)
        general =General.getGeneral(applicationContext)
        IPAddress =general.ipAddress
        locationId=general.FloorID

        textUser=findViewById(R.id.textUser)
        textBranch=findViewById(R.id.textBranch)
        lblScanError=findViewById(R.id.lblScanError)
        textBoxScanned=findViewById(R.id.textBoxScanned)
        textLocationScanned=findViewById(R.id.textLocationScanned)
        textLastBox=findViewById(R.id.textLastBox)
        textLastLocation=findViewById(R.id.textLastLocation)
        btnDone=findViewById(R.id.btnDone)
        btnDelete=findViewById(R.id.btnDelete)
        lblError=findViewById(R.id.lblError)

        textBranch.text =general.fullLocation;
        textUser.text = general.userFullName;
        textBranch.isEnabled=false;
        textUser.isEnabled=false;
        textLastBox.isEnabled=false
        textLastLocation.isEnabled=false

        textBoxScanned.isEnabled=false
        textLocationScanned.requestFocus()



        textBoxScanned.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                Log.i("Ah-Log",textBoxScanned.text.toString())
                if(updatingText)
                    return;
                updatingText=true;
                lblScanError.text="";
                val item=textBoxScanned.text.toString()
                if(item.length<5){
                    Beep()
                    showScanMessage("Invalid Box Barcode",Color.RED)
                    textBoxScanned.setText("")
                    updatingText=false;
                    return;
                }

                if(boxes.containsValue(item)  ){
                    Beep()
                    showScanMessage("$item Scanned Twice !!!",Color.RED)
                    textBoxScanned.setText("")
                    updatingText=false;
                    return;
                }
                Log.i("Ah-Log","3")

                ValidateScan(item)
                textBoxScanned.isEnabled=false

            }

            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

        })


        textLocationScanned.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                Log.i("Ah-Log",textLocationScanned.text.toString())
                if(updatingText1)
                    return;
                updatingText1=true;
                lblScanError.text="";
                val location=textLocationScanned.text.toString()
                if(location.length<2){
                    Beep()
                    showScanMessage("Invalid Location :$location",Color.RED)
                    textLocationScanned.setText("")
                    updatingText1=false;
                    return;
                }
                if(location ==general.mainLocation ){
                    Beep()
                    showScanMessage("Can't transfer to your location !!! Your Location(${general.mainLocation}), Location Scanned ($location)",Color.RED)
                    textLocationScanned.setText("")
                    updatingText1=false;
                    return;
                }

                if(toLocation!=null && toLocation!="" && toLocation!= location  ){
                    Beep()
                    showScanMessage("Finish before changing the location !!! Current Location ($toLocation), Location Scanned ($location)",Color.RED)
                    textLocationScanned.setText("")
                    updatingText1=false;
                    return;
                }


                toLocation=location
                //textLocationScanned.isEnabled=false
                updatingText1=false
                textBoxScanned.isEnabled=true
                textBoxScanned.requestFocus()

            }

            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

        })





        btnDone.setOnClickListener {
            proceed()
        }
        btnDelete.setOnClickListener {
            if(boxes==null || boxes.size ==0)
                return@setOnClickListener

            AlertDialog.Builder(this)
                .setTitle("Delete entry")
                .setMessage("Are you sure you want to delete this entry?") // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.yes,
                    DialogInterface.OnClickListener { dialog, which ->
                        removeLastItem()
                    }) // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()

        }

    }

    private var boxes : HashMap<Int,String> = HashMap();
    private var index:Int=-1
    private var toLocation:String=""
    lateinit var api: BasicApi
    var compositeDisposable= CompositeDisposable()
    var IPAddress =""
    var updatingText=false;
    var updatingText1=false;
    private lateinit var textUser: TextView
    private lateinit var textBranch: TextView
    private lateinit var lblError: TextView
    private lateinit var lblScanError: TextView
    private lateinit var textBoxScanned: EditText
    private lateinit var textLocationScanned: EditText
    private lateinit var textLastBox: TextView
    private lateinit var textLastLocation: TextView
    private lateinit var btnDelete: Button
    private lateinit var btnDone: Button
    private var locationId: Int=-1
    private lateinit var general:General



}