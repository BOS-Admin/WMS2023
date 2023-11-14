package com.bos.wms.mlkit.app.bosApp


import Remote.APIClient
import Remote.BasicApi
import android.app.AlertDialog
import android.graphics.Color
import android.media.AudioManager
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
import com.bos.wms.mlkit.app.Logger
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import java.io.IOException

class CountActivity : AppCompatActivity() {

    private lateinit var textUser: TextView
    private lateinit var textBranch: TextView
    private lateinit var lblError: TextView
    private lateinit var textBoxNb: EditText
    private lateinit var textCount: EditText
    private lateinit var btnDone: Button
    private var locationId:Int=-1
    private lateinit var general:General



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_count)

        mStorage = Storage(applicationContext)
        textUser=findViewById(R.id.textUser)
        textBranch=findViewById(R.id.textBranch)
        lblError=findViewById(R.id.lblError)
        btnDone=findViewById(R.id.btnDone)
        textBoxNb=findViewById(R.id.textBoxNb)
        textCount=findViewById(R.id.textCount)
        val lblDescription:TextView=findViewById(R.id.lblDescription)
        val lblBox:TextView=findViewById(R.id.textBox)
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")
        general= General.getGeneral(applicationContext)
        var UserId =general.userFullName
        textBranch.text = general.fullLocation;
        textUser.setText(UserId.toString() + " " + General.getGeneral(applicationContext).UserName);
        locationId= General.getGeneral(applicationContext).mainLocationID
        textBoxNb.setText( General.getGeneral(applicationContext).boxNb)
        textBranch.isEnabled=false;
        textUser.isEnabled=false;
        textBoxNb.isEnabled=false;
        textCount.requestFocus()
        lblDescription.text="Enter the number of bins"
        lblBox.text="Palette";

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

            if( General.getGeneral(applicationContext).packType=="PaletteBinsCount")
                proceedPalette(count)
            else
                proceed(count)


        }

    }

    private fun proceed(count: Int) {

            try {
                btnDone.isEnabled = false
                btnDone.isVisible = false
                textCount.isEnabled=false
                Log.i("2-13-2023","Count Location "+locationId);
                api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
                compositeDisposable.addAll(
                    api.FillBinCount(general.UserID,count,textBoxNb.text.toString(),locationId)
                        .subscribeOn(Schedulers.io())
                      //  .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            {s->
                                var response = try {
                                    s.string()
                                } catch (e: IOException) {
                                    e.message.toString()
                                }
                                if (response!=null && (response.lowercase().startsWith("released") || response.lowercase().startsWith("success"))) {
                                    showMessage(response,Color.GREEN)
                                }
                                else{
                                    showMessage(response,Color.RED)

                                }



                            },
                            {t:Throwable?->
                                run {
                                    showMessage("API ERROR: "+t?.message,Color.GREEN)

                                }
                            }
                        )
                )
            } catch (e: Throwable) {
                lblError.setTextColor(Color.RED)
                lblError.text = e?.message

            }
            finally {
                textCount.isEnabled=false
            }
        }

    private fun proceedPalette(count: Int) {

        try {
            btnDone.isEnabled = false
            btnDone.isVisible = false
            textCount.isEnabled=false

            lblError.text = "Wait ..."
            lblError.setTextColor(Color.BLUE)
            Log.i("2-13-2023","Count Location "+locationId);
            api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.FillPaletteBinsCount(general.UserID,count,textBoxNb.text.toString(),locationId)
                    .subscribeOn(Schedulers.io())
                    //  .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            var response = try {
                                s.string()
                            } catch (e: IOException) {
                                e.message.toString()
                            }
                            if (response!=null && (response.lowercase().startsWith("released") || response.lowercase().startsWith("success"))) {
                                runOnUiThread {
                                    lblError.text = response
                                    lblError.setTextColor(Color.BLUE)
                                }

                                showMessage(response,Color.GREEN)
                            }
                            else{
                                runOnUiThread {
                                    lblError.text = response
                                    lblError.setTextColor(Color.RED )
                                }
                                showMessage(response,Color.RED)

                            }



                        },
                        {t:Throwable?->
                            run {

                                if (t is HttpException) {
                                    var ex: HttpException = t as HttpException

                                    runOnUiThread {
                                        lblError.text = "Error: "+ex.response().errorBody()!!.string() + ""
                                        lblError.setTextColor(Color.RED )
                                    }

                                    showMessage(
                                        "Error: "+ex.response().errorBody()!!.string() + "",
                                        Color.RED
                                    )

                                } else {

                                    showMessage(t?.message + " (API Error)", Color.RED)

                                    val error=t?.message + " (API Error)"

                                    runOnUiThread {
                                        lblError.text = error
                                        lblError.setTextColor(Color.RED )
                                    }

                                    showMessage(
                                        "Error: $error",
                                        Color.RED
                                    )
//                                    val x=executeCommand()
//                                    Logger.Debug("PackingDC.log","API-Timeout" ,"$error \n End Ping ($ItemSerial): => $x => (${System.currentTimeMillis()-start})")
                                }



                            }
                        }
                    )
            )
        } catch (e: Throwable) {

            runOnUiThread {
                lblError.text = e.message
                lblError.setTextColor(Color.RED )
            }

            lblError.setTextColor(Color.RED)
            lblError.text = e?.message

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