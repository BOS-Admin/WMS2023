package com.hc.mixthebluetooth.activity.Rfid

import Model.RfidStationModel
import Remote.APIClient
import com.hc.mixthebluetooth.Remote.Routes.BasicApi
import android.bluetooth.BluetoothDevice
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import com.hc.basiclibrary.titleBasic.DefaultNavigationBar
import com.hc.basiclibrary.viewBasic.BasActivity
import com.hc.mixthebluetooth.Model.RfidBulkTypeUpdateSessionModel
import com.hc.mixthebluetooth.R
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.*
import com.hc.mixthebluetooth.customView.PopWindowMain
import com.hc.mixthebluetooth.customView.StepFailView
import com.hc.mixthebluetooth.customView.StepSuccessView
import com.hc.mixthebluetooth.storage.Storage
import com.rfidread.Models.Tag_Model
import com.util.StationSocket
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors

@RequiresApi(Build.VERSION_CODES.O)
public class RfidBulkTypeUpdateActivity : BasActivity(), RFIDListener {

    private fun getRfidStation() {
        ConveyorStation = mStorage!!.getDataString("LotBondStation", "")
        if(ConveyorStation.trim().isNotEmpty()) {
            ConveyorMode=true
            addTopSuccessStep("Conveyor Mode: ON", "Conveyor Code: $ConveyorStation","")
        }
        else {
            ConveyorMode=false
            addTopSuccessStep("Conveyor Mode: OFF","","")
        }
    }

    fun ValidateType(type: String) {

        try {
            txtType.isEnabled = false;
            api = APIClient.getInstance(IPAddress, false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.InitRfidBulkTypeUpdate(UserID,ConveyorStation, type)
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                        { s ->
                            session=s;
                            runOnUiThread {
                                addSuccessStep("Valid Type $type","Type Name: "+s.type+"\nSession Id: "+session.id)
                                updatingText = false
                            }
                            session.rfidReadTime=RFIDGrp1ReadTime1;
                            startRfidReading(RFIDGrp1ReadTime1.toLong());
                        },
                        { t: Throwable? ->
                            run {

                                if (t is HttpException) {
                                    var ex: HttpException = t
                                    addFailStep("Error ",
                                        ex.response().errorBody()!!.string() + " (API Http Error)"
                                    )
                                    clearText(txtType)
                                } else {
                                    addFailStep("Error ",t?.message + " (API Error)")
                                    clearText(txtType)

                                }
                                updateResult("Failed XXX",Color.RED)
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            addFailStep("Error","Error:" + e?.message)
            updateResult("Failed XXX",Color.RED)
            clearText(txtType)
        }

    }



    fun startRfidReading(millis:Long) {
        runOnUiThread{
            val rs=RFIDDevicesManager.readEPCSingleAntennaAll()
            notifyStartDevice(""+rs)
            countDownTimer?.cancel()
            countDownTimer = object : CountDownTimer(millis, 1000) {
                override fun onTick(l: Long) {
                }
                override fun onFinish() {
                    notifyEndDevice(RFIDDevicesManager.stopSingleAntenna())
                }
            }.start()
        }

    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun notifyStartDevice(message: String) {

        val now :LocalDateTime= LocalDateTime.now();
        session.rfidReadStart = dtf.format(now)
        addSuccessStep("Rfid Reading Started ","Read From ${RFIDDevicesManager.getSingleAntennaReader()?.ipAddress} status: $message \n Read Time: "+RFIDGrp1ReadTime1+" ms"  ,session.rfidReadStart)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun notifyEndDevice(message: String?) {

        val now :LocalDateTime= LocalDateTime.now();
        session.rfidReadStop = dtf.format(now)
        addSuccessStep("Rfid Reading Stopped ","Stop status=$message",session.rfidReadStop)
        addSuccessStep("Rfid Read: ${bbbList.size}","")
        proceedRfid()
        bbbList.clear()

    }

    private fun proceedRfid() {
        try {

            val rfid: String =
                bbbList.keys.stream().collect(Collectors.joining(","))
            session.rfids = rfid
            session.rfidCount=bbbList.size;

            //Test
            // session.rfids="BBB0800000000001AC00E2E6-E2801170200015C65E3C0928,30342B1B4021EAD049651A0E-E2801130200030C7597808D6,30340BC4A8486C800670001C-E28011602000644F080609F4"
            //  session.rfids="BBB0000000000000B2003C82-E2801170200012282FED0945,BBB0000000000000B2006E10-E28011702000030730060945,BBB0000000000000B2008D0C-E2801170200011D507F80945"
            // session.rfids="BBB0884547842121AC0269D0-E2003412013CF90000082272,BBB0000000000000B1000237-E2003414012403013751D236,BBB0000000000000B10001F8-E200341401130300BB53D928"
            api = APIClient.getInstance(IPAddress, false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.RfidBulkTypeUpdate(session)
                    .subscribeOn(Schedulers.io())
                    // .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            val rs=""+s?.bulkTypeUpdateMessage

                            if(rs.lowercase().startsWith("success")){
                                addSuccessStep("Bulk Type Update Success",rs)
                                updateResult("Type Update Success ✓", Color.rgb(20,205,20))
                            }

                            else{
                                addFailStep("Bulk Type Update Failure XXX", rs)
                                updateResult("Type Update Fail", Color.RED)
                            }

                            if (ConveyorMode){
                                if(session.stationMsgSent!=null && session.stationMsgSent!!.length>1)
                                    addSuccessStep("Moving Conveyor Forward  ","Msg Send: " +session.stationMsgSent)
                                else {
                                    addFailStep("Moving Conveyor Forward  ","Msg Send: (" +session.stationMsgSent+")")
                                }
                                if(session.stationMsgToReceive!=null && session.stationMsgToReceive!!.length>1)
                                    addSuccessStep("Moving Conveyor Forward  ","Msg To Receive: " +session.stationMsgToReceive)
                                else {
                                    addFailStep("Moving Conveyor Forward  ","Msg To Receive: (" +session.stationMsgToReceive+")")
                                }

                                if(session.stationMsgReceived!=null && session.stationMsgReceived!!.length>1)
                                    addSuccessStep("Moving Conveyor Forward  ","Msg Received: " +session.stationMsgReceived)
                                else {
                                    addFailStep("Moving Conveyor Forward  ","Msg Received: (" +session.stationMsgReceived+")")
                                }

                                if(session.stationMsgToReceive!=null && session.stationMsgReceived==session.stationMsgToReceive) {
                                    addSuccessStep(
                                        "Moving Conveyor Forward Success ✓",
                                        ""
                                    )

                                }
                                else {
                                    addFailStep("Moving Conveyor Forward Failed XXX  ","")

                                }

                            }
                            clearText(txtType);
                        },
                        { t: Throwable? ->
                            run {
                                if (t is HttpException) {
                                    var ex: HttpException = t
                                    addFailStep(
                                        "Bulk Type Update Failure",
                                        ex.response().errorBody()!!.string() + "(API Http Error)"
                                    )
                                    clearText(txtType);

                                } else{
                                    addFailStep("Bulk Type Update Failure",t?.message + " (API Error)")
                                    clearText(txtType);
                                }


                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            addFailStep("Bulk Type Update Failure","Error:" + e?.message)
            clearText(txtType);
        }

    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bulk_type_update)
        setTitle()
        mStorage = Storage(this)
        UserID = mStorage!!.getDataInt("UserID", -1)
        IPAddress = mStorage!!.getDataString("IPAddress", "192.168.50.20:5000")
        RFIDGrp1ReadTime1 = mStorage!!.getDataInt("AntennaReadTime", 10000)

        //Toast.makeText(applicationContext,"AntennaReadTime $RFIDGrp1ReadTime1",Toast.LENGTH_SHORT).show();
        stepslayout = findViewById(R.id.linearLayoutSteps);
        topSteps = findViewById(R.id.topLinearLayoutSteps);
        lblError = findViewById(R.id.lblError);
        lblResult = findViewById(R.id.lblResult);

        antConnected= mStorage!!.getData("AntennaConnected", false)

        //Test
        //antConnected=true


        if(!antConnected){
            lblError.setTextColor(Color.RED);
            lblError.text="Antenna not connected";
            return;
        }

        initAll()

        txtType = findViewById(R.id.txtType)
        txtType.addTextChangedListener(TypeTextWatcher())
        txtType.isEnabled = false
        getRfidStation()
        txtType.isEnabled = true
        txtType.requestFocus()

    }


    override fun initAll() {
        val AntIp = mStorage!!.getDataString("AntennaReaderIPAddress", "")
        RFIDDevicesManager.setOutput(RFIDOutput(this));
        if(!AntIp.isNullOrEmpty()){
            antConnected=true;
            lblError.text = "Antenna Reader: ${AntIp}"

        }
        else{
            antConnected=false;
            lblError.setTextColor(Color.RED);
            lblError.text = "Antenna Reader: None"
        }
    }



    override fun notifyListener(device: RFIDDevice, tag_model: Tag_Model) {
        try {
            Log.i("Ah-Log-XXX","EPC "+tag_model._EPC)
            synchronized(hmList_Lock) {
                if (TagModelKey(tag_model).isEmpty()) {
                    return
                }
                if (TagModelKey(tag_model).lowercase().startsWith("bbb")) {
                    if (!bbbList.containsKey(TagModelKey(tag_model))) {
                        bbbList[TagModelKey(tag_model)] = tag_model
                    }
                }
            }

        } catch (ex: Exception) {
            Log.d("Debug", "Tags output exceptions:" + ex.message)
        }
    }



    inner class TypeTextWatcher : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            if (updatingText)
                return;
            clearSteps()
            updatingText = true;
            val str = txtType.text.toString()
            if (str.length < 4) {
                playError()
                addFailStep("Invalid Type \n !!! $str Minimum Length is 4","")
                updateResult("Failed XXX",Color.RED)
                clearText(txtType)
                return;
            }
            txtType.isEnabled = false;
            ValidateType(str)
        }

        override fun onTextChanged(
            s: CharSequence, start: Int, before: Int, count: Int
        ) {
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        }

    }



    fun clearText(txt: EditText) {
        runOnUiThread {
            updatingText = true;
            txt.setText("")
            updatingText = false
            txt.isEnabled = true;
            txt.requestFocus()
        }

    }

    fun clearSteps() {
        runOnUiThread {
            updateResult("Result",Color.BLUE)
            stepslayout.removeAllViews()
        }

    }

    fun TagModelKey(model: Tag_Model): String {
        return if ((model._EPC != null && model._TID != null) && model._EPC.isNotEmpty() && model._TID.isNotEmpty()) model._EPC + "-" + model._TID else ""
    }

    private fun addSuccessStep(title:String,desc: String,date:String?) {
        runOnUiThread{
            stepslayout.addView(StepSuccessView(this, "$title: ${getTime(""+date)}",desc))
        }
    }

    private fun addFailStep(title:String,desc: String,date:String?) {
        runOnUiThread {
            stepslayout.addView(StepFailView(this, "$title: ${getTime(""+date)}",desc))
        }
        playError()
    }


    private fun addTopSuccessStep(title:String,desc: String,date:String?) {
        runOnUiThread{
            topSteps.addView(StepSuccessView(this, "$title ${getTime(""+date)}",desc))
        }
    }

    private fun addTopFailStep(title:String,desc: String,date:String?) {
        runOnUiThread {
            topSteps.addView(StepFailView(this, "$title: ${getTime(""+date)}",desc))
        }
        playError()
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




    private fun updateResult(title:String,color:Int) {
        runOnUiThread{
            lblResult.text = title;
            lblResult.setTextColor(color)
        }
    }


    private fun setTitle() {
        mTitle = DefaultNavigationBar.Builder(
            this,
            findViewById<View>(R.id.activity_bulk_type_update) as ViewGroup
        )
            .setLeftText("Rfid Bulk Type Update")
            .hideLeftIcon()
            .setRightIcon()
            .setRightClickListener(View.OnClickListener { v ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    toast(
                        "This feature is not supported by the system, please upgrade the phone system",
                        Toast.LENGTH_LONG
                    )
                    return@OnClickListener
                }
                setPopWindow(v)
                mTitle?.updateRightImage(true)
            })
            .builer()
    }

    private var mTitle: DefaultNavigationBar? = null

    private fun setPopWindow(v: View) {
        PopWindowMain(
            v, this@RfidBulkTypeUpdateActivity
        ) { resetEngine ->
            //弹出窗口销毁的回调
            mTitle!!.updateRightImage(false)
            if (resetEngine) { //更换搜索引擎，重新搜索
                // refresh()
            }
        }
    }

    fun getTime(date:String):String{
        if(date==null || !date.contains("T"))
            return "";
        return date.split("T")[1];
    }
    override fun notifyStartAntenna(ant: Int) {
        TODO("Not yet implemented")
    }

    override fun notifyStopAntenna(ant: Int) {
        TODO("Not yet implemented")
    }




    var updatingText = false
    lateinit var stepslayout: LinearLayout
    lateinit var topSteps: LinearLayout
    lateinit var txtType: EditText
    lateinit var txtCartonNo: EditText
    lateinit var txtUPC: EditText
    lateinit var lblError: TextView
    lateinit var lblResult: TextView
    lateinit var btnDone: Button
    lateinit var api: BasicApi
    var mStorage: Storage? = null
    var UserID: Int = -1
    var IPAddress = ""
    var compositeDisposable = CompositeDisposable()
    private var ConveyorStation = ""
    lateinit var stationModel: RfidStationModel
    lateinit var session: RfidBulkTypeUpdateSessionModel
    lateinit var stationMessages: HashMap<String, String>
    private val bbbList = HashMap<String, Tag_Model>()
    private val hmList_Lock = Any()
    var countDownTimer: CountDownTimer? = null
    var RFIDGrp1ReadTime1=0
    var BluetoothDeviceList: ArrayList<BluetoothDevice> = arrayListOf()
    var BluetoothDevicelistStr: ArrayList<String> = arrayListOf()
    var BluetoothDevicelistMac: ArrayList<String> = arrayListOf()
    private lateinit var socket:StationSocket
    val dtf :DateTimeFormatter= DateTimeFormatter.ofPattern( "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private var ConveyorMode=false
    var antConnected=false;

}