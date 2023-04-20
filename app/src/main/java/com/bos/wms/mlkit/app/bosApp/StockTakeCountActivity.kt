package com.bos.wms.mlkit.app.bosApp


import Remote.APIClient
import Remote.BasicApi
import android.app.AlertDialog
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaDescription
import android.media.ToneGenerator
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.R
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import java.io.IOException

class StockTakeCountActivity : AppCompatActivity() {

    private lateinit var textUser: TextView
    private lateinit var textBranch: TextView
    private lateinit var lblError: TextView
    private lateinit var textBox: TextView
    private lateinit var lblDescription: TextView
    private lateinit var textBoxNb: EditText
    private lateinit var textCount: EditText
    private lateinit var btnDone: Button
    private var locationId:Int=-1
    private lateinit var general:General
    private var stockTakeType:Int=-1



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_count)

        mStorage = Storage(applicationContext)
        textUser=findViewById(R.id.textUser)
        textBranch=findViewById(R.id.textBranch)
        textBox=findViewById(R.id.textBox)
        lblError=findViewById(R.id.lblError)
        btnDone=findViewById(R.id.btnDone)
        textBoxNb=findViewById(R.id.textBoxNb)
        textCount=findViewById(R.id.textCount)
        lblDescription=findViewById(R.id.lblDescription)
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")
        general= General.getGeneral(applicationContext)
        var UserId =general.userFullName
        textBranch.text = general.fullLocation;
        textUser.text = UserId.toString() + " " + general.UserName;
        locationId= general.mainLocationID
        textBoxNb.setText( general.boxNb)
        stockTakeType=general.stockType

        when (stockTakeType) {
            100 -> {textBox.text = "Box"; lblDescription.text = "Please enter the Box Count "}
            101 -> {textBox.text = "Stand"; lblDescription.text = "Please enter the Stand Count "}
            102 -> {textBox.text = "Rack"; lblDescription.text = "Please enter the Rack Count "}
            else ->{textBox.text = "Invalid"; lblDescription.text = "Invalid Type!!! ";btnDone.isEnabled=false}
        }


        textBranch.isEnabled=false;
        textUser.isEnabled=false;
        textBoxNb.isEnabled=false;
        textCount.requestFocus()

        btnDone.setOnClickListener {
            lblError.text = ""
            var count:Int=0
            try{
                count=Integer.parseInt(textCount.text.toString())
                if(count <=0) throw Exception()
            }
            catch (e:Exception){
                lblError.text = "Invalid Count !!!!!"
                Beep()
                return@setOnClickListener
            }
            if(stockTakeType==102)
                proceedRack(count)
            if(stockTakeType==101 || stockTakeType==100)
                proceed(count)
        }

    }

    private fun proceed(count: Int) {

            try {
                btnDone.isEnabled = false
                btnDone.isVisible = false
                textCount.isEnabled=false
                Log.i("Ah-log",""+count);
                api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
                compositeDisposable.addAll(
                    api.ValidatBinCountStockTake(general.UserID,count,textBoxNb.text.toString(),locationId)
                        .subscribeOn(Schedulers.io())
                      //  .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            {s->
                                var response = try {
                                    s.string()
                                } catch (e: Exception) {
                                    e.message.toString()
                                }
                                if (response!=null && (response.lowercase().startsWith("success") || response.lowercase().startsWith("released"))) {
                                    showMessage(response,Color.GREEN)
                                }
                                else{
                                    showMessage(""+response?.toString(),Color.RED)
                                }



                            },
                            {t:Throwable?->
                                run {
                                    if(t is HttpException){
                                        var ex: HttpException =t as HttpException
                                        showMessage(""+ ex.response().errorBody()!!.string(),Color.RED)
                                    }
                                    else{
                                        if(t?.message!=null)
                                            showMessage("Error: " +t.message.toString(),Color.RED)
                                    }

                                    Beep()
                                }
                            }
                        )
                )
            } catch (e: Throwable) {
                lblError.setTextColor(Color.RED)
                lblError.text = e?.message
                Beep()
            }
            finally {
                textCount.isEnabled=false
            }
        }

    private fun proceedRack(count: Int) {

        try {
            btnDone.isEnabled = false
            btnDone.isVisible = false
            textCount.isEnabled=false
            Log.i("Ah-log",""+count);
            api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.ValidateRackCountStockTake(general.UserID,textBoxNb.text.toString(),locationId,count)
                    .subscribeOn(Schedulers.io())
                    //  .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            var response = try {
                                s.string()
                            } catch (e: Exception) {
                                e.message.toString()
                            }
                            if (response!=null && (response.lowercase().startsWith("success") || response.lowercase().startsWith("released"))) {
                                showMessage(response,Color.GREEN)
                            }
                            else{
                                showMessage(response,Color.RED)

                            }



                        },
                        {t:Throwable?->
                            run {
                                if(t is HttpException){
                                    var ex: HttpException =t as HttpException
                                    showMessage( ex.response().errorBody()!!.string()+" (Http Error)",Color.RED)
                                }
                                else{
                                    if(t?.message!=null)
                                        showMessage(t.message.toString()+ " (API Error )" ,Color.RED)
                                }

                                Beep()
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            lblError.setTextColor(Color.RED)
            lblError.text = e?.message
            Beep()
        }
        finally {
            textCount.isEnabled=false
        }
    }
    private fun Beep(){
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300)
    }
    lateinit var api: BasicApi
    var compositeDisposable= CompositeDisposable()
    lateinit var  mStorage:Storage ;
    var IPAddress =""



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


}