package com.hc.mixthebluetooth.activity.rfidLotBond

import Model.RfidStationModel
import Remote.APIClient
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
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
import androidx.core.view.isVisible
import com.hc.basiclibrary.titleBasic.DefaultNavigationBar
import com.hc.basiclibrary.viewBasic.BasActivity
import com.hc.mixthebluetooth.Model.RfidLotBond.RfidLotBondSessionModel
import com.hc.mixthebluetooth.R
import com.hc.mixthebluetooth.Remote.Routes.BasicApi
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.*
import com.hc.mixthebluetooth.customView.PopWindowMain
import com.hc.mixthebluetooth.customView.StepFailView
import com.hc.mixthebluetooth.customView.StepSuccessView
import com.hc.mixthebluetooth.storage.Storage
import com.rfidread.Enumeration.eReadType
import com.rfidread.Models.Tag_Model
import com.util.StationSocket
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("MissingPermission")
open class RfidLotBondEnhancedActivity : BasActivity(), RFIDListener {

    enum class ResponseMessage {
        INVALID_RFID,MULTI_RFID, RFID_BONDED_TO_ANOTHER_ITEMSERIAL, ITEMSERIAL_BONDED_TO_ANOTHER_RFID, RFID_NOT_CLEAN,OTHERS
    }

    //region initial
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rfid_bulk)
        setTitle()
        mStorage = Storage(this)
        userID = mStorage!!.getDataInt("UserID", -1)
        ipAddress = mStorage!!.getDataString("IPAddress", "192.168.50.20:5000")
        lotBondStation = mStorage!!.getDataString("LotBondStation", "")
        rfidGrp1ReadTime1 = Integer.parseInt(mStorage!!.getDataString("RFIDGrp1ReadTime1", "300"))
        stepsLayout = findViewById(R.id.linearLayoutSteps);
        lblRfidDevice = findViewById(R.id.lblRfidDevice);
        lblConveyor = findViewById(R.id.lblConveyor);
        lblResult = findViewById(R.id.lblResult);
        btnDone = findViewById(R.id.btnDone);
        btnDone.setOnClickListener {
        clearText(txtItemSerial)
        clearText(txtBolNb)
        clearText(txtCartonNo)
        txtCartonNo.isEnabled = false
        txtItemSerial.isEnabled = false
        clearSteps()
        txtBolNb.requestFocus()
        }
        try {
            initAll()
        } catch (e: Exception) {
            Log.e("BLUETOOTH", "Error: " + e.message)
        }

        txtBolNb = findViewById(R.id.txtLotNo)
        txtCartonNo = findViewById(R.id.txtCartonNo)
        txtItemSerial = findViewById(R.id.txtUPC)
        txtBolNb.addTextChangedListener(LotNbTextWatcher())
        txtCartonNo.addTextChangedListener(CartonNbTextWatcher())
        txtItemSerial.addTextChangedListener(ItemSerialTextWatcher())
        txtBolNb.isEnabled = false
        txtCartonNo.isEnabled = false
        txtItemSerial.isEnabled = false
        getRfidStation(lotBondStation)

    }

    override fun initAll() {
        try {
            Thread {
                val rfidMac = mStorage!!.getDataString("RFIDMac", "")
                val rfidName = getBluetoothDeviceNameFromMac(rfidMac)
                var slid = Slid()
                slid.bluetoothAddress = rfidName
                var rs = RFIDDevicesManager.connectSingleSlid(slid)
                if (rs)
                    lblRfidDevice.text = "Rfid Devices: ${slid.connParam}"
                RFIDDevicesManager.setOutput(RFIDOutput(this));
            }.start()
        } catch (e: Exception) {
            lblRfidDevice.text = "Rfid Devices: " + e.message
        }


    }

    inner class LotNbTextWatcher : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            if (updatingText)
                return;
            clearSteps()
            updatingText = true;
            val str = txtBolNb.text.toString()
            if (str.length < 4) {
                playError()
                addFailStep("Invalid LotNo \n NO!!! $str", "")
                clearText(txtBolNb)
                return;
            }
            txtBolNb.isEnabled = false;
            validateBolNo(str)
        }

        override fun onTextChanged(
            s: CharSequence, start: Int, before: Int, count: Int
        ) {
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        }

    }
    inner class CartonNbTextWatcher : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            if (updatingText)
                return;
            clearSteps()
            updatingText = true;
            val str = txtCartonNo.text.toString()
            if (str.length < 10) {
                playError()
                addFailStep("Invalid Carton No $str", "")
                clearText(txtCartonNo)
                return;
            }
            txtCartonNo.isEnabled = false
            validateCartonNo(str, txtBolNb.text.toString())

        }

        override fun onTextChanged(
            s: CharSequence, start: Int, before: Int, count: Int
        ) {
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        }

    }
    inner class ItemSerialTextWatcher : TextWatcher {
     override fun afterTextChanged(s: Editable) {
            if (updatingText)
                return;
            clearSteps()
            updatingText = true;
            val str = txtItemSerial.text.toString()
            if (str.length < 10 || str.length > 14) {
                playError()
                addFailStep("Invalid ItemSerial  $str", "")
                clearText(txtItemSerial)
                return;
            }
            updatingText = false
            txtItemSerial.isEnabled = false;
            addSuccessStep("Valid ItemSerial  $str", "")
            Thread {
                initRfidLotBondSession(str)
            }.start()

        }

        override fun onTextChanged(
            s: CharSequence, start: Int, before: Int, count: Int
        ) {
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        }

    }
    private fun getRfidStation(code: String) {

        try {
            conveyorStation = mStorage!!.getDataString("LotBondStation", "")
            if (conveyorStation.trim().isNotEmpty()) {
                conveyorMode = true
                lblConveyor.text = "Conveyor: $conveyorStation"
            } else {
                conveyorMode = false
                lblConveyor.text = "Conveyor Mode: OFF"
                txtBolNb.isEnabled = true
                txtBolNb.requestFocus()
                return
            }


            api = APIClient.getInstance(ipAddress, false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.GetRfidLotBondStation(code)
                    .subscribeOn(Schedulers.io())
                    // .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            if (s != null && s.station.stationCode == code) {
                                saveStationModel(s)
                                runOnUiThread {
                                    var str = ""
                                    for (x in s.messages)
                                        str += "(" + x.message + "): " + x.hexCode + "\n"
                                    addSuccessStep("Valid Station: $code", str)
                                    txtBolNb.isEnabled = true
                                    txtBolNb.requestFocus()
                                }
                            } else
                                addFailStep(
                                    "Failed To retrieve station info",
                                    "Station is not valid: $code (API Error)"
                                )

                        },
                        { t: Throwable? ->
                            run {

                                if (t is HttpException) {
                                    val ex: HttpException = t
                                    addFailStep(
                                        "Failed To retrieve station info",
                                        ex.response().errorBody()!!.string() + "(API Http Error)"
                                    )

                                } else
                                    addFailStep(
                                        "Failed To retrieve station info",
                                        t?.message + " (API Error)"
                                    )
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            addFailStep("Failed To retrieve station info", "Error:" + e?.message)
        }
    }
    fun saveStationModel(s: RfidStationModel) {
        stationModel = s
        stationMessages = HashMap()
        for (x in s.messages)
            stationMessages[x.message] = x.hexCode
        var stage = "Start"
        try {
            socket = StationSocket()
            socket.startConnection(stationModel?.station?.ipaddress,
                stationModel?.station?.portNb!!
            )
            stage = "done";
            addSuccessStep("Connection to conveyor successful", "Stage=$stage ")
        } catch (e: Exception) {
            var msg = "";
            try {
                msg = "IP= " + stationModel?.station?.ipaddress;
                msg += ";Port= " + stationModel?.station?.portNb;

            } catch (ex: Exception) {

            }
            addFailStep(
                "Failed to start", "Station Connection Param:" + msg + "\nStage=" + stage + "; "
                        + e.message + ":" + e.stackTrace + ";" + e.cause?.message + ":" + e.cause?.stackTrace
            )
        }

    }

    fun validateBolNo(bolNb: String) {

        try {
            txtBolNb.isEnabled = false;
            api = APIClient.getInstance(ipAddress, false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.ValidateBol(bolNb)
                    .subscribeOn(Schedulers.io())
                    // .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->

                            if (s)
                                runOnUiThread {
                                    addSuccessStep("Valid Bol Number $bolNb", "")
                                    txtCartonNo.isEnabled = true
                                    txtCartonNo.requestFocus()
                                    updatingText = false
                                }
                            else {
                                addFailStep("Invalid Bol Number", "$bolNb (API Error)")
                                clearText(txtBolNb)
                            }


                        },
                        { t: Throwable? ->
                            run {

                                if (t is HttpException) {
                                    var ex: HttpException = t
                                    addFailStep(
                                        "Invalid Bol Number",
                                        ex.response().errorBody()!!.string() + " (API Http Error)"
                                    )
                                    clearText(txtBolNb)
                                } else {
                                    addFailStep("Invalid Bol Number", t?.message + " (API Error)")
                                    clearText(txtBolNb)

                                }
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            addFailStep("Invalid Bol Number", "Error:" + e?.message)
            clearText(txtBolNb)
        }
    }

    fun validateCartonNo(cartonNb: String, bolNb: String) {

        try {
            txtCartonNo.isEnabled = false;
            api = APIClient.getInstance(ipAddress, false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.ValidateCarton(cartonNb, bolNb)
                    .subscribeOn(Schedulers.io())
                    // .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->

                            if (s)
                                runOnUiThread {
                                    addSuccessStep("Valid Carton Number $cartonNb", "")
                                    txtItemSerial.isEnabled = true
                                    txtItemSerial.requestFocus()
                                    updatingText = false
                                }
                            else {
                                addFailStep("Invalid Carton Number", "$cartonNb (API Error)")
                                clearText(txtCartonNo)
                            }


                        },
                        { t: Throwable? ->
                            run {

                                if (t is HttpException) {
                                    var ex: HttpException = t
                                    addFailStep(
                                        "Invalid Carton Number",
                                        ex.response().errorBody()!!.string() + " (API Http Error)"
                                    )
                                    clearText(txtCartonNo)
                                } else {
                                    addFailStep(
                                        "Invalid Carton Number",
                                        t?.message + " (API Error)"
                                    )
                                    clearText(txtCartonNo)

                                }
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            addFailStep("Invalid Carton Number", "Error:" + e?.message)
            clearText(txtCartonNo)
        }
    }

    override fun onDestroy() {
        RFIDDevicesManager.stopSingleSlid()
        RFIDDevicesManager.disconnectSingle()
        RFIDDevicesManager.setOutput(null)
        try{
            socket.stopConnection()
        }
        catch (_:Exception){

        }

        super.onDestroy()
    }

    //endregion
    //region start rfid session
    open fun initRfidLotBondSession(itemSerial: String) {
        if(rfidSessionInProgress)
            return
        if (conveyorMode && stationModel == null) {
            addFailStep("Failed to retrieve station info", "Station Model is Empty")
            return
        }
        rfidSessionInProgress=true
        txtItemSerial.isEnabled = false;
        txtItemSerial.clearFocus();
        resetRfidLotBondSession()
        startRfidLotBondSession(itemSerial)

    }
    open fun resetRfidLotBondSession(){
        clearSteps()
        countDownTimer?.cancel()
        RFIDDevicesManager.stopSingleSlid()
        session = RfidLotBondSessionModel()
        session.activity=activityTitle
        session.rfid = null
        session.bol = txtBolNb.text.toString()
        session.cartonNo = txtCartonNo.text.toString()
        session.stationCode = stationModel?.station?.stationCode
        session.userId = userID
        bbbMap.clear()
        rs = -1000;


    }
    open fun startRfidLotBondSession(itemSerial: String){
        if(!allowSession){
            addFailStep("Session Not Allowed", "allow session: $allowSession")
            return
        }
        session.itemSerial = itemSerial
        session.sessionStartDate = dtf.format(LocalDateTime.now())
        startRfidReading(rfidGrp1ReadTime1.toLong())

    }
    //region rfid rReading
    fun startRfidReading(millis: Long) {
        rs = RFIDDevicesManager.readEPCSingleSlid(eReadType.Inventory)
        session.rfidReadStart = dtf.format(LocalDateTime.now())
        addSuccessStep("Rfid Reading for $millis ms Started ", "Read status: $rs", session.rfidReadStart)
        runOnUiThread {
                countDownTimer?.cancel()
                countDownTimer = object : CountDownTimer(millis, 300) {
                    override fun onTick(l: Long) {
                    }

                    override fun onFinish() {
                        runOnUiThread {
                            stopRfidReading("Rfid Reading Stopped after $millis ms")
                        }
                    }
                }.start()
            }
    }

    fun stopRfidReading(message: String) {
        val status = RFIDDevicesManager.stopSingleSlid()
        session.rfidReadStop = dtf.format(LocalDateTime.now())
        addSuccessStep(
            "Rfid Reading Stopped ",
            "$message \nStop status=$status",
            session.rfidReadStop
        )
        var s = ""
        for (x in bbbMap.keys)
            s += x + "\n"
        addSuccessStep("Rfid Read: ${bbbMap.size}", s)
        checkRfidReadings()
        bbbMap.clear()
    }

    override fun notifyListener(device: RFIDDevice, tag_model: Tag_Model) {
        try {
            Log.i("Ah-Log-XXX", "EPC " + tag_model._EPC)
            synchronized(hmListLock) {
                val rfid = tagModelKey(tag_model)
                if (rfid.isEmpty()) {
                    return
                }
                if (rfid.lowercase().startsWith("bbb")) {

                    if (!bbbMap.containsKey(rfid)) {
                        bbbMap[rfid] = tag_model
                    }
                }
            }

        } catch (ex: Exception) {
            Log.d("Debug", "Tags output exceptions:" + ex.message)
        }
    }

    fun checkRfidReadings() {
            when (bbbMap.size) {
                0 -> reject("No Rfids Detected")

                1 -> lotBondAPI()

                else -> reject("Multi Rfids Detected")

            }
    }

    //endregion
    //region lotBond
    fun lotBondAPI() {
        try {
            session.rfid=bbbMap.keys.first()
                session.rfidLotBondStart = dtf.format(LocalDateTime.now())
                api = APIClient.getInstance(ipAddress, false).create(BasicApi::class.java)
                compositeDisposable.addAll(
                    api.RFIDLotBondEnhanced(session)
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                            { s ->
                                val rs = "" + s?.rfidLotBondMessage
                                session = s
                                session.rfidLotBondMessage = rs
                                session.rfidLotBondStop = dtf.format(LocalDateTime.now())
                                if (rs.lowercase().startsWith("success")) {
                                    addSuccessStep(
                                        "Rfid Lot Bond Success",
                                        rs,
                                        session.rfidLotBondStop
                                    )
                                    accept("Lot bond success: $rs")
                                } else {
                                    addFailStep(
                                        "Rfid Lot Bond Failure XXX",
                                        rs,
                                        session.rfidLotBondStop
                                    )
                                    reject(""+rs)
                                }
                            },
                            { t: Throwable? ->
                                session.rfidLotBondStop = dtf.format(LocalDateTime.now())
                                run {
                                    if (t is HttpException) {
                                        var ex: HttpException = t
                                        addFailStep(
                                            "Rfid Lot Bond Failure",
                                            ex.response().errorBody()!!
                                                .string() + "(API Http Error)"
                                        )
                                        session.rfidLotBondMessage =
                                            ex.response().errorBody()!!
                                                .string() + "(API Http Error)"

                                    } else {
                                        addFailStep(
                                            "Rfid Lot Bond Failure",
                                            t?.message + " (API Error)"
                                        )
                                        session.rfidLotBondMessage = t?.message + " (API Error)"
                                    }
                                    updateResult("Lot Bond Fail", Color.RED)
                                    reject(""+session.rfidLotBondMessage)
                                }
                            }
                        )
                )


        } catch (e: Throwable) {
            addFailStep("Rfid Lot Bond Failure", "Error:" + e?.message)
            reject("Lot bond fail: Exception${e?.message}")
        }

    }

    open fun accept(msg: String = "") {
        try {
            Thread{
                updateResult("Lot Bond Success ✓", Color.GREEN)
                addSuccessStep("Lot Bond Success", msg, dtf.format(LocalDateTime.now()))
                session.rfidLotBondMessage = msg
                if (conveyorMode) {
                    if (!stationMessages.containsKey("Success Msg")) {
                        addFailStep("Failed to find Success Msg message", "")
                    } else {
                        session.rfidMsgSentDate = dtf.format(LocalDateTime.now())
                        stationMessages["Success Msg"]?.let {
                            session.rfidMsgSent = it
                            addSuccessStep(
                                "Moving Conveyor Success ",
                                "Success Msg: " + session.rfidMsgSent,
                                session.rfidMsgSentDate
                            )
                            val response = try {
                                socket.sendMessage(it)
                            } catch (e: Exception) {
                                val sw = StringWriter()
                                val pw = PrintWriter(sw)
                                e.printStackTrace(pw)
                                "Error: "+e.message.toString()+"\n"+sw.toString()
                            }
                            session.rfidMsgRec = "" + response
                            session.rfidMsgRecDate = dtf.format(LocalDateTime.now())
                            if (response == stationMessages["Success Resp"])
                                addSuccessStep(
                                    "Conveyor Forwards Move Success \u2713 ",
                                    "RFID Receive Success Resp : $response",
                                    session.rfidMsgRecDate
                                )

                            else {
                                addFailStep(
                                    "Failed To Move Conveyor Forwards",
                                    "Invalid conveyor response \n(${session.rfidMsgRec})",
                                    session.rfidMsgRecDate
                                )
                            showErrorMessage("Invalid conveyor response \n(${session.rfidMsgRec})")
                            }

                        }
                    }
                }
                finalize()
            }.start()

        } catch (e: Exception) {
            addFailStep("Conveyor Failure", "Error " + e.message + ";" + e.stackTrace)

        }

    }

     open fun reject(msg: String = "") {
            try {
                Thread{
                    session.rfidLotBondMessage = msg

                    addFailStep("Rejected", msg, dtf.format(LocalDateTime.now()))
                    if (conveyorMode) {
                        if (!stationMessages.containsKey("Failure Msg")) {
                            addFailStep("Failed to find 'Failure Msg' message", "")
                        } else {
                            session.rfidMsgSentDate = dtf.format(LocalDateTime.now())
                            stationMessages["Failure Msg"]?.let {
                                session.rfidMsgSent = it
                                addSuccessStep(
                                    "Moving Conveyor Backwards",
                                    "'Failure Msg': " + session.rfidMsgSent,
                                    session.rfidMsgSentDate
                                )
                                val response = try {
                                    socket.sendMessage(it)
                                } catch (e: Exception) {
                                    "Error: "+e.message.toString()+"\n"+e.printStackTrace()
                                }
                                session.rfidMsgRec = "" + response
                                session.rfidMsgRecDate = dtf.format(LocalDateTime.now())
                                if (response == stationMessages["Failure Msg"])
                                    addSuccessStep(
                                        "Conveyor Backwards Move Success \u2713 ",
                                        "Received 'Failure Msg' : $response",
                                        session.rfidMsgRecDate
                                    )
                                else {
                                    addFailStep(
                                        "Failed To Move Conveyor Backwards",
                                        "Invalid conveyor response \n(${session.rfidMsgRec})",
                                        session.rfidMsgRecDate
                                    )
                                    showErrorMessage("Invalid conveyor response \n(${session.rfidMsgRec})")
                                }
                            }
                        }
                }

                }.start()

            } catch (e: Exception) {
                addFailStep("Conveyor Failure", "Error " + e.message + ";" + e.stackTrace)
            } finally {

            }

         finalize()
         updateResult("Lot Bond Fail\n$msg", Color.RED)
         if(msg.lowercase().startsWith("no rfids detected"))
             playSound(ResponseMessage.INVALID_RFID)
         else if(msg.lowercase().startsWith("multi rfids detected"))
             playSound(ResponseMessage.MULTI_RFID)
         else if(msg.lowercase().startsWith("rfid bonded to another itemserial"))
             playSound(ResponseMessage.RFID_BONDED_TO_ANOTHER_ITEMSERIAL)
         else if(msg.lowercase().startsWith("itemserial bonded to another rfid"))
             playSound(ResponseMessage.ITEMSERIAL_BONDED_TO_ANOTHER_RFID)
         else if(msg.lowercase().startsWith("rfid not cleaned"))
             playSound(ResponseMessage.RFID_NOT_CLEAN)
         else playSound(ResponseMessage.OTHERS)

    }

     fun finalize() {
         session.activity=activityTitle
        try {
            api = APIClient.getInstance(ipAddress, false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.postRidLotBondSession(session)
                    .subscribeOn(Schedulers.io())
                    // .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            if (s != null && s.string() == "success")
                                addSuccessStep(
                                    "Rfid Lot Bond session is Done",
                                    "Updating session was successful"
                                )
                            else
                                addFailStep(
                                    "Rfid Lot Bond session is Done",
                                    "Updating session was unsuccessful"
                                )

                            runOnUiThread {
                                rfidSessionInProgress=false
                                clearText(txtItemSerial)
                            }

                        },
                        { t: Throwable? ->
                            run {

                                if (t is HttpException) {
                                    var ex: HttpException = t
                                    addFailStep(
                                        "Rfid Lot Bond session is Done",
                                        ex.response().errorBody()!!.string() + "(API Http Error)"
                                    )

                                } else
                                    addFailStep("Rfid Lot Bond session is Done", t?.message + " (API Error)")
                            }

                            runOnUiThread {
                                rfidSessionInProgress=false
                                clearText(txtItemSerial)
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            addFailStep("Rfid Lot Bond session is Done", "Error:" + e?.message)
        }

    }

    //endregion
    //region view
    open fun clearText(txt: EditText) {
        runOnUiThread {
            updatingText = true;
            txt.setText("")
            updatingText = false
            txt.isEnabled = true;
            txt.requestFocus()
        }

    }

    open fun clearSteps() {
        runOnUiThread {
            updateResult("Result", Color.BLUE)
            stepsLayout.removeAllViews()
        }

    }

    fun addSuccessStep(title: String, desc: String, date: String?) {
        runOnUiThread {
            stepsLayout.addView(StepSuccessView(this, "$title: ${getTime("" + date)}", desc))
        }
    }

    fun addFailStep(title: String, desc: String, date: String?) {
        runOnUiThread {
            stepsLayout.addView(StepFailView(this, "$title: ${getTime("" + date)}", desc))
        }
        playError()
    }

    fun addSuccessStep(title: String, desc: String) {
        runOnUiThread {
            stepsLayout.addView(StepSuccessView(this, title, desc))
        }
    }

    fun addFailStep(title: String, desc: String) {
        runOnUiThread {
            stepsLayout.addView(StepFailView(this, title, desc))
        }
        playError()
    }

    fun updateResult(title: String, color: Int) {
        runOnUiThread {
            lblResult.text = title;
            lblResult.setTextColor(color)
        }
    }

    //endregion
    //region basic functions
    fun tagModelKey(model: Tag_Model): String {
        return if ((model._EPC != null && model._TID != null) && model._EPC.isNotEmpty() && model._TID.isNotEmpty()) model._EPC + "-" + model._TID else ""
    }

    open fun setTitle() {
        setTitle("Rfid Lot Bond Enhanced")
    }

    open fun setTitle(msg:String="") {
        activityTitle=msg;
        mTitle = DefaultNavigationBar.Builder(
            this,
            findViewById<View>(R.id.activity_rfid_bulk) as ViewGroup
        )
            .setLeftText(msg)
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

    var mTitle: DefaultNavigationBar? = null
    fun setPopWindow(v: View) {
        PopWindowMain(
            v, this@RfidLotBondEnhancedActivity
        ) { resetEngine ->
            //弹出窗口销毁的回调
            mTitle!!.updateRightImage(false)
            if (resetEngine) { //更换搜索引擎，重新搜索
                // refresh()
            }
        }
    }

    fun getTime(date: String): String {
        if (date == null || !date.contains("T"))
            return "";
        return date.split("T")[1];
    }

    fun getBluetoothDeviceNameFromMac(Mac: String?): String? {
        if (!(Mac != null && !Mac.isEmpty())) {
            return ""
        }
        getBT4DeviceStrList()
        for (d in bluetoothDeviceList) {
            if (d.address != null && d.address.contains(Mac)) return d.name
            //something here
        }
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = bluetoothAdapter.getRemoteDevice(Mac) ?: return ""
        return device.name
    }

    fun getBT4DeviceStrList(): List<String?>? {
        bluetoothDeviceList.clear()
        bluetoothDeviceListStr.clear()
        bluetoothDeviceListMac.clear()
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices = bluetoothAdapter.bondedDevices
        if (pairedDevices.size > 0) {
            val var1: Iterator<*> = pairedDevices.iterator()
            while (var1.hasNext()) {
                val device = var1.next() as BluetoothDevice
                try {
                    bluetoothDeviceList.add(device)
                    bluetoothDeviceListStr.add(device.name)
                    bluetoothDeviceListMac.add(device.address)
                } catch (var4: java.lang.Exception) {
                }
            }
        }
        return bluetoothDeviceListStr
    }

    override fun notifyStartAntenna(ant: Int) {

    }

    override fun notifyStopAntenna(ant: Int) {

    }

    override fun notifyStartDevice(message: String) {

    }

    override fun notifyEndDevice(message: String?) {

    }

    private fun showErrorMessage(msg: String) {
        allowSession=false
        playError()
        playError()
        playError()
        playError()
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle("Conveyor Error")
                .setMessage(msg)
                .setPositiveButton("OK") { _, _ ->
                    val intent = Intent (applicationContext, RfidLotBondMainMenuActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .show()

        }
    }


    fun onCheckboxClicked(view:View) {
        stepsLayout.isVisible = (view as CheckBox).isChecked;
    }
    fun playSound(x:ResponseMessage){

        when(x){
            ResponseMessage.INVALID_RFID-> MediaPlayer.create(this,R.raw.invalid_rfid).start()
            ResponseMessage.MULTI_RFID-> MediaPlayer.create(this,R.raw.multi_rfid).start()
            ResponseMessage.RFID_BONDED_TO_ANOTHER_ITEMSERIAL-> MediaPlayer.create(this,R.raw.rfid_bonded_to_another_itemserial).start()
            ResponseMessage.ITEMSERIAL_BONDED_TO_ANOTHER_RFID-> MediaPlayer.create(this,R.raw.itemserial_bonded_to_another_rfid).start()
            ResponseMessage.RFID_NOT_CLEAN-> MediaPlayer.create(this,R.raw.rfid_not_clean).start()
            ResponseMessage.OTHERS-> MediaPlayer.create(this,R.raw.others).start()
        }
    }



    //endregion
    //region declarations
    var updatingText = false
    lateinit var stepsLayout: LinearLayout
    lateinit var txtBolNb: EditText
    lateinit var txtCartonNo: EditText
    lateinit var txtItemSerial: EditText
    lateinit var lblRfidDevice: TextView
    lateinit var lblConveyor: TextView
    lateinit var lblResult: TextView
    lateinit var btnDone: Button
    lateinit var api: BasicApi
    var mStorage: Storage? = null
    var userID: Int = -1
    var ipAddress = ""
    var compositeDisposable = CompositeDisposable()
    var lotBondStation = ""
    var stationModel: RfidStationModel? = null
    lateinit var session: RfidLotBondSessionModel
    lateinit var stationMessages: HashMap<String, String>
    val bbbMap = HashMap<String, Tag_Model>()
    val hmListLock = Any()
    var countDownTimer: CountDownTimer? = null
    var rfidGrp1ReadTime1 = 0
    var bluetoothDeviceList: ArrayList<BluetoothDevice> = arrayListOf()
    var bluetoothDeviceListStr: ArrayList<String> = arrayListOf()
    var bluetoothDeviceListMac: ArrayList<String> = arrayListOf()
    lateinit var socket: StationSocket
    val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
    var rs = -1000;
    var success = false
    var conveyorMode = false
    var conveyorStation = ""
    var rfidSessionInProgress=false
    var activityTitle:String=""
    //endregion
    var allowSession=true
}