package com.bos.wms.mlkit.app

import Model.NextReceivingStatusModel
import Remote.APIClient
import Remote.BasicApi
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.R
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_next_receiving_status.*
import java.io.IOException
import java.util.*


class NextReceivingStatusActivity  : AppCompatActivity(), TextToSpeech.OnInitListener {

    lateinit var  mStorage:Storage ;  //sp存储
    var IPAddress =""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_next_receiving_status)
        mStorage = Storage(applicationContext) //sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")

        txtNextReceivingItemBarcode.setShowSoftInputOnFocus(false);
        txtNextReceivingItemBarcode.requestFocus()
        txtNextReceivingItemBarcode.setShowSoftInputOnFocus(false);
        tts = TextToSpeech(
            this
        ) { status -> // TODO Auto-generated method stub
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.e("error", "This Language is not supported")
                } else {

                }
            } else Log.e("error", "Initilization Failed!")
        }
        txtNextReceivingItemBarcode.doAfterTextChanged {
            if (txtNextReceivingItemBarcode.text.isBlank()) {
                return@doAfterTextChanged;
            }
            if (txtNextReceivingItemBarcode.length() != 12 && txtNextReceivingItemBarcode.length() != 13) {
                txtNextReceivingItemBarcode.setText("");
                return@doAfterTextChanged;
            }
            val Model = NextReceivingStatusModel(
                txtNextReceivingItemBarcode.text.toString(),
                General.getGeneral(this).UserID
            )
            NextReceivingStatus(Model)

        }
    }
        lateinit var api: BasicApi
        var compositeDisposable= CompositeDisposable()
        fun NextReceivingStatus(Model: NextReceivingStatusModel) {
            try {
                api = APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
                compositeDisposable.addAll(
                    api.NextReceivingStatus(Model)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { s ->
                                txtNextReceivingItemBarcode.setText("")
                                var Status:String
                                var Sound:String
                                when (s) {
                                    0 -> Status="Item not found"
                                    1 ->  Status="Next Station"
                                    2 -> Status="Studio Station"
                                    3 -> Status="Service Station"
                                    else -> { // Note the block
                                        Status="Unkown Status"
                                    }
                                }
                                Sound=Status.replace("Station","")
                                txtNextReceivingStatus.setText(Status)

//                                tts.setPitch(2f)
//                                tts.setSpeechRate(5f)
//                                var param:Bundle= Bundle()
//                                param.putFloat("rate",5000f)
//                                param.putString("language","eng")
//                                param.putString("country","USA")

                                tts!!.speak(Sound, TextToSpeech.QUEUE_FLUSH, null,"")



                            },
                            { t: Throwable? ->
                                run {
                                    txtNextReceivingStatus.setText(t?.message)
                                }
                            }
                        )
                )
            } catch (e: Throwable) {
                txtNextReceivingStatus.setText(e?.message)
                throw(IOException("NextReceivingStatus Activity - NextReceivingStatus", e))
            } finally {
            }
        }
    lateinit var tts:TextToSpeech;
    override fun onInit(p0: Int) {

    }

}
