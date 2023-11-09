package com.hc.mixthebluetooth.activity.rfidLotBond

import Model.RfidStationModel
import Remote.APIClient
import com.hc.mixthebluetooth.Remote.Routes.BasicApi
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.annotation.RequiresApi
import com.hc.basiclibrary.viewBasic.BasActivity
import com.hc.mixthebluetooth.R
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.*
import com.hc.mixthebluetooth.customView.StepFailView
import com.hc.mixthebluetooth.customView.StepSuccessView
import com.hc.mixthebluetooth.storage.Storage
import com.util.StationSocket
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException

@RequiresApi(Build.VERSION_CODES.O)
class RfidBulkTestActivity : BasActivity() {

    fun GetRfidStation(code: String) {

        try {
            api = APIClient.getInstance(IPAddress, false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.GetRfidLotBondStation(code)
                    .subscribeOn(Schedulers.io())
                    // .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            if (s != null && s.station.stationCode == code)
                                runOnUiThread {
                                    saveStationModel(s)
                                    var str=""
                                    for (x in s.messages)
                                        str+="("+x.message+"): "+ x.hexCode+"\n"
                                    addSuccessStep("Valid Station: $code",str)

                                }
                            else
                                addFailStep("Failed To retrieve station info","Station is not valid: $code (API Error)")

                        },
                        { t: Throwable? ->
                            run {

                                if (t is HttpException) {
                                    var ex: HttpException = t
                                    addFailStep("Failed To retrieve station info",
                                        ex.response().errorBody()!!.string() + "(API Http Error)"
                                    )

                                } else
                                    addFailStep("Failed To retrieve station info",t?.message + " (API Error)")
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            addFailStep("Failed To retrieve station info","Error:" + e?.message)
        }
    }

    fun saveStationModel(s: RfidStationModel) {
        stationModel = s;
        stationMessages = HashMap();
        for (x in s.messages)
            stationMessages[x.message] = x.hexCode
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rfid_bulk)
        mStorage = Storage(this)
        IPAddress = mStorage!!.getDataString("IPAddress", "192.168.50.20:5000")
        LotBondStation = mStorage!!.getDataString("LotBondStation", "")
        RFIDGrp1ReadTime1 = Integer.parseInt(mStorage!!.getDataString("RFIDGrp1ReadTime1", "300"))
        stepslayout = findViewById(R.id.linearLayoutSteps);
        btnDone = findViewById(R.id.btnDone);
        btnDone.setOnClickListener{
            val socket=StationSocket()
            Thread{
                socket.startConnection("192.168.16.8",502)
                if(!socket.isConnected){
                    addFailStep("Socket Connection",""+socket.ErrorMsg)
                }
                if(socket.isConnected){
//                    var msg=stationMessages["X0 Request State"]
//                    addSuccessStep("Sending X0 Request State",""+msg)
//                   val resp:String = socket.sendMessageV3(msg)
//                    addSuccessStep("Received ",""+resp)

                    var msg=stationMessages["Tatex Send"]
                    addSuccessStep("Tatex Send",""+msg)
                    var resp:String = socket.sendMessage(msg)
                    addSuccessStep("Received ",""+resp)

                    Thread.sleep(1000)

                     msg=stationMessages["RFID Send"]
                    addSuccessStep("RFID Send",""+msg)
                     resp= socket.sendMessage(msg)
                    addSuccessStep("Received ",""+resp)
                }
                socket.stopConnection()

            }.start()

        }
        initAll()
        GetRfidStation(LotBondStation)
    }

    override fun initAll() {

    }


    private fun addSuccessStep(title:String,desc: String) {
        runOnUiThread{
            stepslayout.addView(StepSuccessView(this, title,desc))
        }
    }

    private fun addFailStep(title:String,desc: String) {
        runOnUiThread {
            stepslayout.addView(StepFailView(this, title,desc))
        }
        playError()
    }


    lateinit var stepslayout: LinearLayout
    lateinit var btnDone: Button
    lateinit var api: BasicApi
    var mStorage: Storage? = null
    var IPAddress = ""
    var compositeDisposable = CompositeDisposable()
    private var LotBondStation = ""
    lateinit var stationModel: RfidStationModel
    lateinit var stationMessages: HashMap<String, String>

    var RFIDGrp1ReadTime1=0


}