package com.hc.mixthebluetooth.activity

import Remote.APIClient
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.hc.mixthebluetooth.Model.PackReasonModelItem
import com.hc.mixthebluetooth.R
import com.hc.mixthebluetooth.Remote.Routes.BasicApi
import com.hc.mixthebluetooth.storage.Storage
import com.util.General
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException


class PackingReasonActivity : AppCompatActivity() {
    private lateinit var spinner: Spinner
    private lateinit var lblError1: TextView
    private lateinit var model :List<PackReasonModelItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_packing_reason)
        mStorage= Storage(applicationContext)
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")
        spinner=findViewById(R.id.spinner)
        lblError1=findViewById(R.id.lblError)
        GetPackReasons()

        findViewById<Button>(R.id.btnPackingReasonDone).setOnClickListener{
            if(spinner.selectedItem==null){
                finish()
                return@setOnClickListener
            }

            val general = General.getGeneral(applicationContext)
            general.currentPackReason=model[spinner.selectedItemPosition]
            general.saveGeneral(applicationContext)
            val intent = Intent (applicationContext, AntennaPackingActivity::class.java)
            startActivity(intent)
            finish()

        }

    }



    fun GetPackReasons() {

        try {
            lblError1.text = "fetching data..."
            Log.i("PackReasonActivity","started api")
            api= APIClient.getInstance(IPAddress ,true).create(
                BasicApi::class.java)
            compositeDisposable.addAll(
                api.GetPackReasons()
                    .subscribeOn(Schedulers.io())
                    //  .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            if(s!==null && s.isNotEmpty()){
                                model=s
                                Log.i("AH-Log-Pack","pack size "+s.size)
                                runOnUiThread{
                                    updateUiWithPackReasons(s)
                                    lblError1.text = ""
                                }

                            }
                            else{
                                runOnUiThread{
                                    lblError1.text="Pack List is Empty"
                                }

                            }

                        },
                        {t:Throwable?->
                            run {

                                if(t is HttpException){
                                    var ex: HttpException =t as HttpException
                                    showMessage("Http Error :ex.response().errorBody()!!.string()",Color.RED)
                                }
                                else{
                                    if(t?.message!=null)
                                        showMessage("Error :"+t.message.toString(), Color.RED)
                                }

                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            runOnUiThread{
                lblError1.text="API Error: "+ e?.message
            }

        }
        finally {
        }
    }

    private fun updateUiWithPackReasons( model:List<PackReasonModelItem>) {

        try{
            val adapter: ArrayAdapter<*> = object : ArrayAdapter<Any?>(
                applicationContext,
                android.R.layout.simple_list_item_1, android.R.id.text1, model.map { x->x.name }
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val textView = super.getView(position, convertView, parent) as TextView
                    textView.textSize = 14f
                    try{
                        textView.text = model[position].name
                    }catch (e :Exception){
                        textView.text = e.message
                    }

                    return textView
                }
            }

            spinner.adapter = adapter
            adapter.notifyDataSetChanged()
        }
        catch (e:Exception){

        }


    }

    var compositeDisposable= CompositeDisposable()
    lateinit var  mStorage: Storage
    var IPAddress =""
    lateinit var api: BasicApi


    private fun showMessage(msg:String,color:Int){
        runOnUiThread{
            lblError1.setTextColor(color)
            lblError1.text = msg
        }
    }



}