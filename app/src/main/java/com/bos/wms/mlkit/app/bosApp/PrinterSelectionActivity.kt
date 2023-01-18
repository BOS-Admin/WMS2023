package com.bos.wms.mlkit.app.bosApp

import Model.BosApp.PrintersModel
import Remote.APIClient
import Remote.BasicApi
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.R
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_printer_selection.*
import retrofit2.HttpException


class PrinterSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printer_selection)
        mStorage = Storage(applicationContext) //sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")
        mStorage= Storage(applicationContext)
        var FloorID: Int= General.getGeneral(applicationContext).FloorID
        GetPrinters(""+FloorID);

    }

    fun GetPrinters(location:String) {

        try {
            lblError.text = "fetching data..."
            Log.i("PrinterSelectionActivity","location = " +location)
            api= APIClient.getInstance("https://bosapp.free.beeceptor.com/" ,true,true).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.GetPrintSections()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            if(s.message==null || s.message.isNullOrEmpty()|| s.message == ""){
                                Log.i("AH-Log-Printer","printers size "+s.printers.size)
                                updateUiWithPrinters(s)
                                lblError.text = ""
                            }
                            else{
                              lblError.text=s.message
                            }

                        },
                        {t:Throwable?->
                            run {

                                if(t is HttpException){
                                    var ex: HttpException =t as HttpException
                                    lblError.text=ex.response().errorBody()!!.string()
                                }
                                else{
                                    if(t?.message!=null)
                                        lblError.text=t.message.toString()
                                }

                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            lblError.text=e?.message
        }
        finally {
        }
    }

    private fun updateUiWithPrinters(model: PrintersModel) {
        val adapter: ArrayAdapter<*> = object : ArrayAdapter<Any?>(
            applicationContext,
            android.R.layout.simple_list_item_1, android.R.id.text1, model.printers.map { x->x.Stand }
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView = super.getView(position, convertView, parent) as TextView
                textView.textSize = 14f
                textView.setText(model.printers[position].Stand)
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


}