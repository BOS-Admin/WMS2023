package com.bos.wms.mlkit.app.bosApp


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
import Model.BosApp.PrinterModelItem
import Remote.APIClient
import Remote.BasicApi
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.R
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException


class PrinterSelectionActivity : AppCompatActivity() {
    private lateinit var spinner: Spinner
    private lateinit var lblError1: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printer_selection)
        spinner=findViewById<Spinner>(R.id.spinner)
        lblError1=findViewById<TextView>(R.id.lblError)
        mStorage = Storage(applicationContext) //sp存储
        IPAddress =General.getGeneral(applicationContext).ipAddress
        mStorage= Storage(applicationContext)
        var locationId: Int= General.getGeneral(applicationContext).mainLocationID

        GetPrinters(""+locationId);
        findViewById<Button>(R.id.btnPackingReasonDone).setOnClickListener{
            try {
                General.getGeneral(applicationContext).printerStationCode =
                    spinner.selectedItem.toString()
                General.getGeneral(applicationContext).saveGeneral(applicationContext)
                Log.i("AH-Log-Printer", "spinner " + spinner.selectedItem.toString())
                val intent = Intent(applicationContext, PackingActivity::class.java)
                startActivity(intent)
                finish()
            }
            catch (_:Exception){

            }


        }

    }

    fun GetPrinters(location:String) {

        try {
            lblError1.text = "fetching data..."
            Log.i("PrinterSelectionActivity","location = " +location)
            api= APIClient.getInstance(General.getGeneral(applicationContext).ipAddress ,true).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.GetPrintSections(location)
                    .subscribeOn(Schedulers.io())
                    //  .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            if(s!=null && s.isNotEmpty()){
                                Log.i("AH-Log-Printer","printers size "+s.size)
                                updateUiWithPrinters(s)
                                lblError1.text = ""
                            }
                            else{
                                lblError1.text="List is Empty"
                            }

                        },
                        {t:Throwable?->
                            run {

                                if(t is HttpException){
                                    var ex: HttpException =t as HttpException
                                    showMessage(ex.response().errorBody()!!.string(),Color.RED)
                                }
                                else{
                                    if(t?.message!=null)
                                        showMessage(t.message.toString(), Color.RED)
                                }

                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            lblError1.text=e?.message
        }
        finally {
        }
    }

    private fun updateUiWithPrinters(model: List<PrinterModelItem>) {
        val adapter: ArrayAdapter<*> = object : ArrayAdapter<Any?>(
            applicationContext,
            android.R.layout.simple_list_item_1, android.R.id.text1, model.map { x->x.stationCode }
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView = super.getView(position, convertView, parent) as TextView
                textView.textSize = 14f
                textView.text = model[position].stationCode
                return textView
            }
        }

        spinner.adapter = adapter
        adapter.notifyDataSetChanged()

    }

    var compositeDisposable= CompositeDisposable()
    lateinit var  mStorage:Storage
    lateinit var api: BasicApi
    var IPAddress =""


    private fun showMessage(msg:String,color:Int){
        runOnUiThread{
            lblError1.setTextColor(color)
            lblError1.text = msg
        }
    }


}