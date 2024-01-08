package com.hc.mixthebluetooth.activity.rfidLotBond

import Model.RfidStationModel
import Remote.APIClient
import android.annotation.SuppressLint
import android.app.AlertDialog
import com.hc.mixthebluetooth.Remote.Routes.BasicApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
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
import com.hc.mixthebluetooth.Model.RfidLotBond.RfidLotBondSessionModel
import com.hc.mixthebluetooth.Model.RfidSessionModel
import com.hc.mixthebluetooth.R
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
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors
@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.O)
class RfidLotBondBulkActivity : BasActivity(), RFIDListener {


    override fun onDestroy() {
        // Toast.makeText(applicationContext,"Im Destroyed !!!",Toast.LENGTH_LONG).show();
        RFIDDevicesManager.stopSingleSlid();
        RFIDDevicesManager.disconnectSingle()
        try{
            socket.stopConnection()
        }
        catch (_:Exception){

        }
        super.onDestroy()
    }

    fun GetRfidStation(code: String) {
        try {
            api = APIClient.getInstance(IPAddress, false).create(BasicApi::class.java)
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
                                    txtLotNo.isEnabled = true
                                    txtLotNo.requestFocus()
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
                                    var ex: HttpException = t
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
        stationModel = s;
        stationMessages = HashMap();
        for (x in s.messages)
            stationMessages[x.message] = x.hexCode
        var stage = "Start"
        try {
            socket = StationSocket()
            stage = "Start Connection"
            socket.startConnection(stationModel.station.ipaddress, stationModel.station.portNb)
            stage = "done";
            addSuccessStep("Connection to conveyor successful", "Stage=$stage ")
        } catch (e: Exception) {
            var msg = "";
            try {
                msg = "IP= " + stationModel.station.ipaddress;
                msg += ";Port= " + stationModel.station.portNb;

            } catch (ex: Exception) {

            }
            addFailStep(
                "Failed to start", "Station Connection Param:" + msg + "\nStage=" + stage + "; "
                        + e.message + ":" + e.stackTrace + ";" + e.cause?.message + ":" + e.cause?.stackTrace
            )
        }

    }

    fun ValidateLotNo(lotNb: String) {

        try {
            txtLotNo.isEnabled = false;
            api = APIClient.getInstance(IPAddress, false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.InitRFIDLotBonding1(UserID, lotNb)
                    .subscribeOn(Schedulers.io())
                    // .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            var response = try {
                                s.string()
                            } catch (e: IOException) {
                                e.message.toString()
                            }

                            if (response != null && response.lowercase().startsWith("success") ||
                                response.lowercase().startsWith("valid")
                            )
                                runOnUiThread {
                                    addSuccessStep("Valid Lot Number $lotNb", "")
                                    txtCartonNo.isEnabled = true
                                    txtCartonNo.requestFocus()
                                    updatingText = false
                                }
                            else {
                                addFailStep("Invalid Lot Number", "$response (API Error)")
                                clearText(txtLotNo)
                            }


                        },
                        { t: Throwable? ->
                            run {

                                if (t is HttpException) {
                                    var ex: HttpException = t
                                    addFailStep(
                                        "Invalid Lot Number",
                                        ex.response().errorBody()!!.string() + " (API Http Error)"
                                    )
                                    clearText(txtLotNo)
                                } else {
                                    addFailStep("Invalid Lot Number", t?.message + " (API Error)")
                                    clearText(txtLotNo)

                                }
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            addFailStep("Invalid Lot Number", "Error:" + e?.message)
            clearText(txtLotNo)
        }
    }

    fun startRfidLotBondSession(UPC: String) {
        if (stationModel == null) {
            addFailStep("Failed to retrieve station info", "Station Model is Empty")
            return
        }
        if(!allowSession){
            addFailStep("Session Not Allowed", "allow session: $allowSession")
            return
        }
        bbbList.clear()
        session = RfidLotBondSessionModel()
        session.activity="Rfid Lot Bond Bulk"
        session.itemSerial = UPC
        session.bol = txtLotNo.text.toString()
        session.cartonNo = txtCartonNo.text.toString()
        session.stationCode = stationModel.station.stationCode
        session.userId = UserID
        CheckIfItemOnConveyor(UPC)


    }

    fun CheckIfItemOnConveyor(UPC: String): Boolean {
        var success = false
        try {
            if (!stationMessages.containsKey("X0 Request State")) {
                addFailStep("Failed to find X0 Request State ", "")
                return false
            }
            session.checkItemOnConveyorSentDate = dtf.format(LocalDateTime.now())
            addSuccessStep(
                "Checking If Item On Conveyor",
                "X0 Request State: ${stationMessages["X0 Request State"]}",
                session.checkItemOnConveyorSentDate
            )

            stationMessages["X0 Request State"]?.let {
                session.checkItemOnConveyorMsgSent = it


                val response =
                    try {
                        socket.sendMessage(it)
                    } catch (e: Exception) {
                        "Error " + e.message.toString() + ";" + e.stackTrace.toString()
                    }
                Log.i("Ah-Log-XXX", "Response $response")
                session.checkItemOnConveyorMsgRec = "" + response
                session.checkItemOnConveyorRecDate = dtf.format(LocalDateTime.now())
                when (response) {
                    stationMessages["X0 ON"] -> {
                        addSuccessStep(
                            "Item On Conveyor \u2713 ",
                            "X0 ON: $response",
                            session.checkItemOnConveyorRecDate
                        )
                        success = true
                        MoveClientForward(UPC)
                    }
                    stationMessages["X0 OFF"] -> {
                        addFailStep(
                            "Item Not On Conveyor XXX !!!!",
                            "X0 OFF: $response", session.checkItemOnConveyorRecDate
                        )
                        addFailStep("Please place the item on the conveyor before scanning", "")
                        clearText(txtUPC)
                    }
                    else -> {
                        addFailStep(
                            "Failed to check if item is on conveyor",
                            "Invalid conveyor response ($response)",
                            session.checkItemOnConveyorRecDate
                        )
                        clearText(txtUPC)
                        showErrorMessage("Invalid conveyor response ($response)")
                    }
                }
                //Testing
//               if (!success)
//                   MoveClientForward(UPC)
            }


        } catch (e: Throwable) {
            addFailStep("Failed to check if item is on conveyor", "Error:" + e?.message)
            return false
        }
        return true
    }

    fun MoveClientForward(UPC: String) {

        try {

            if (!stationMessages.containsKey("Tatex Send")) {
                addFailStep("Failed to find Tatex Send message", "")
            }
            session.tatexMsgDate = dtf.format(LocalDateTime.now())
            addSuccessStep(
                "Moving Conveyor Forward M0",
                "Tatex Send: ${stationMessages["Tatex Send"]}",
                session.tatexMsgDate
            )
            stationMessages["Tatex Send"]?.let {
                session.tatexMsgSent = it
                val response = try {
                    socket.sendMessage(it)
                } catch (e: Exception) {
                    e.message.toString()
                }
                session.tatexMsgRec = "" + response
                session.tatexMsgRecDate = dtf.format(LocalDateTime.now())
                when (response) {
                    stationMessages["Tatex Receive M0 ON"] -> {
                        rs = RFIDDevicesManager.readEPCSingleSlid(eReadType.Inventory)
                        val now: LocalDateTime = LocalDateTime.now();
                        session.rfidReadStart = dtf.format(now)
                        addSuccessStep(
                            "Moving Forward Success M0 \u2713 ",
                            "Tatex Receive M0 ON : " + session.tatexMsgRec, session.tatexMsgRecDate
                        )

                        startRfidReading(RFIDGrp1ReadTime1.toLong())

                    }
                    stationMessages["Tatex Receive M0 OFF"] -> {
                        addFailStep(
                            "Moving Forward M0: Failure XXX !!!!",
                            "Tatex Receive M0 OFF : " + session.tatexMsgRec, session.tatexMsgRecDate
                        )
                        //addFailStep("Please place the item on the conveyor before sacnning ","")
                    }
                    else -> {
                        addFailStep(
                            "Failed to move conveyor forward M0",
                            "Invalid conveyor response (${session.tatexMsgRec})",
                            session.tatexMsgRecDate
                        )
                        showErrorMessage("Invalid conveyor response ($response)")
                    }

                }
            }


        } catch (e: Throwable) {
            addFailStep("Failed to move conveyor forward M0", "Error:" + e?.message)

        }
    }

    fun startRfidReading(millis: Long) {
        runOnUiThread {
            notifyStartDevice("" + rs)
            countDownTimer?.cancel()
            countDownTimer = object : CountDownTimer(millis, 500) {
                override fun onTick(l: Long) {
                }

                override fun onFinish() {
                    runOnUiThread {
                        notifyEndDevice(RFIDDevicesManager.stopSingleSlid())
                    }
                }
            }.start()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun notifyStartDevice(message: String) {
        addSuccessStep("Rfid Reading Started ", "Read status: $message", session.rfidReadStart)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun notifyEndDevice(message: String?) {

        val now: LocalDateTime = LocalDateTime.now();
        session.rfidReadStop = dtf.format(now)
        proceedRfid()
        addSuccessStep("Rfid Reading Stopped ", "Stop status=$message", session.rfidReadStop)
        var s = ""
        for (x in bbbList)
            s += x.key + "\n"
        addSuccessStep("Rfid Read: ${bbbList.size}", s)
        bbbList.clear()

    }

    private fun proceedRfid() {
        try {
            val bbbRfid: String =
                bbbList.keys.stream().collect(Collectors.joining(","))
            var rfid = bbbRfid
            if (bbbList.keys.size > 1) {
                rfid = "multi bbb $bbbRfid"
            }
            session.rfid = rfid
            Log.i("lotbol",""+session.bol)
            if (!stationMessages.containsKey("RFID Send")) {
                addFailStep("Failed to find RFID Send message", "")
                return
            }
            //session.rfidMsgSent=stationMessages["RFID Send"]
            session.rfidLotBondStart = dtf.format(LocalDateTime.now())
            api = APIClient.getInstance(IPAddress, false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.RFIDLotBondingAutoBulkV3(session)
                    .subscribeOn(Schedulers.io())
                    // .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            var success = false;
                            val rs = "" + s?.rfidLotBondMessage
                            session.rfidLotBondMessage = rs
                            session.rfidLotBondStop = dtf.format(LocalDateTime.now())
                            if (rs.lowercase().startsWith("success")) {
                                addSuccessStep("Rfid Lot Bond Success", rs, session.rfidLotBondStop)
                                updateResult("Lot Bond Success ✓", Color.GREEN)
                                success = true;
                            } else {
                                addFailStep(
                                    "Rfid Lot Bond Failure XXX",
                                    rs,
                                    session.rfidLotBondStop
                                )
                                updateResult("Lot Bond Fail", Color.RED)
                            }

                            if (success) {
                                session.rfidMsgSentDate = dtf.format(LocalDateTime.now())
                                stationMessages["RFID Send"]?.let {
                                    session.rfidMsgSent = it
                                    addSuccessStep(
                                        "Moving Conveyor Forward M1 ",
                                        "RFID Send: " + session.rfidMsgSent,
                                        session.rfidMsgSentDate
                                    )
                                    val response = try {
                                        socket.sendMessage(it)
                                    } catch (e: Exception) {
                                        e.message.toString()
                                    }
                                    session.rfidMsgRec = "" + response
                                    session.rfidMsgRecDate = dtf.format(LocalDateTime.now())
                                    when (response) {
                                        stationMessages["RFID Receive M1 ON"] -> {
                                            addSuccessStep(
                                                "Conveyor Forwards Move M1 \u2713 ",
                                                "RFID Receive M1 ON : $response",
                                                session.rfidMsgRecDate
                                            )

                                        }
                                        stationMessages["RFID Receive M1 OFF"] -> {
                                            addFailStep(
                                                "Conveyor Forwards Move: Sensor M1 XXX !!!!",
                                                "Sensor M1 was off for RFID Send message\nRFID Receive M1 OFF : $response",
                                                session.rfidMsgRecDate
                                            )


                                        }
                                        else ->{
                                            addFailStep(
                                                "Failed To Move Conveyor Forwards M1",
                                                "Invalid conveyor response (${session.rfidMsgRec})",
                                                session.rfidMsgRecDate
                                            )
                                            showErrorMessage("Invalid conveyor response (${session.rfidMsgRec})")

                                        }

                                    }
                                }
                            }
                            finalize()

                        },
                        { t: Throwable? ->
                            session.rfidLotBondStop = dtf.format(LocalDateTime.now())
                            run {
                                if (t is HttpException) {
                                    var ex: HttpException = t
                                    addFailStep(
                                        "Rfid Lot Bond Failure",
                                        ex.response().errorBody()!!.string() + "(API Http Error)"
                                    )
                                    session.rfidLotBondMessage =
                                        ex.response().errorBody()!!.string() + "(API Http Error)"

                                } else {
                                    addFailStep(
                                        "Rfid Lot Bond Failure",
                                        t?.message + " (API Error)"
                                    )
                                    session.rfidLotBondMessage = t?.message + " (API Error)"
                                }

                                finalize()
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            addFailStep("Rfid Lot Bond Failure", "Error:" + e?.message)
        }

    }

    fun finalize() {

        try {
            api = APIClient.getInstance(IPAddress, false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.postRidLotBondSession(session)
                    .subscribeOn(Schedulers.io())
                    // .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            if (s != null && s.string() == "success")
                                addSuccessStep(
                                    "Rfid session is Done",
                                    "Updating session was successful"
                                )
                            else
                                addFailStep(
                                    "Rfid session is Done",
                                    "Updating session was unsuccessful"
                                )
                            runOnUiThread {
                                clearText(txtUPC)
                            }

                        },
                        { t: Throwable? ->
                            run {

                                if (t is HttpException) {
                                    var ex: HttpException = t
                                    addFailStep(
                                        "Rfid session is Done",
                                        ex.response().errorBody()!!.string() + "(API Http Error)"
                                    )

                                } else
                                    addFailStep("Rfid session is Done", t?.message + " (API Error)")
                            }
                            runOnUiThread {
                                clearText(txtUPC)
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            addFailStep("Rfid session is Done", "Error:" + e?.message)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rfid_bulk)
        setTitle()
        mStorage = Storage(this)
        UserID = mStorage!!.getDataInt("UserID", -1)
        IPAddress = mStorage!!.getDataString("IPAddress", "192.168.50.20:5000")
        LotBondStation = mStorage!!.getDataString("LotBondStation", "")
        RFIDGrp1ReadTime1 = Integer.parseInt(mStorage!!.getDataString("RFIDGrp1ReadTime1", "300"))
        stepslayout = findViewById(R.id.linearLayoutSteps);
        lblRfidDevice = findViewById(R.id.lblRfidDevice);
        lblConveyor = findViewById(R.id.lblConveyor);
        lblResult = findViewById(R.id.lblResult);
        btnDone = findViewById(R.id.btnDone);
        btnDone.setOnClickListener {
            clearText(txtUPC)
            clearText(txtLotNo)
            clearText(txtCartonNo)
            clearSteps()
            txtLotNo.requestFocus()
        }
        try {
            initAll()
        }catch(e: Exception){
            Log.e("BLUETOOTH", "Error: " + e.message)
        }

        txtLotNo = findViewById(R.id.txtLotNo)
        txtCartonNo = findViewById(R.id.txtCartonNo)
        txtUPC = findViewById(R.id.txtUPC)
        txtLotNo.addTextChangedListener(LotNbTextWatcher())
        txtCartonNo.addTextChangedListener(CartoNbTextWatcher())
        txtUPC.addTextChangedListener(UPCTextWatcher())
        txtLotNo.isEnabled = false
        txtCartonNo.isEnabled = false
        txtUPC.isEnabled = false

        if (LotBondStation.trim().isNotEmpty()) {
            lblConveyor.text = "Conveyor: $LotBondStation"
            GetRfidStation(LotBondStation)
        } else {
            lblConveyor.text = "Conveyor Mode: OFF"
            return
        }

    }


    override fun initAll() {
        try {
            Thread {
                val RFIDMac = mStorage!!.getDataString("RFIDMac", "")
                //RFIDName=GetBluetoothDeviceNameFromMac(RFIDMac);
                val RFIDName = GetBluetoothDeviceNameFromMac(RFIDMac)
                var slid = Slid()
                //slid.bluetoothAddress = "BTR-80022070010"
                slid.bluetoothAddress = RFIDName
                var rs = RFIDDevicesManager.connectSingleSlid(slid)
                // addSuccessStep("Connection to ${slid.bluetoothAddress}: $rs","")

                if (rs)
                    lblRfidDevice.text = " Rfid Devices: ${slid.connParam}"
                RFIDDevicesManager.setOutput(RFIDOutput(this));
            }.start()


        } catch (e: Exception) {
            lblRfidDevice.text = " Rfid Devices: " + e.message
        }


    }

    @SuppressLint("MissingPermission")
    private fun GetBluetoothDeviceNameFromMac(Mac: String?): String? {
        if (!(Mac != null && !Mac.isEmpty())) {
            return ""
        }
        GetBT4DeviceStrList()
        for (d in BluetoothDeviceList) {
            if (d.address != null && d.address.contains(Mac)) return d.name
            //something here
        }
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = bluetoothAdapter.getRemoteDevice(Mac) ?: return ""
        return device.name
    }

    fun GetBT4DeviceStrList(): List<String?>? {
        BluetoothDeviceList.clear()
        BluetoothDevicelistStr.clear()
        BluetoothDevicelistMac.clear()
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices = bluetoothAdapter.bondedDevices
        if (pairedDevices.size > 0) {
            val var1: Iterator<*> = pairedDevices.iterator()
            while (var1.hasNext()) {
                val device = var1.next() as BluetoothDevice
                try {
                    BluetoothDeviceList.add(device)
                    BluetoothDevicelistStr.add(device.name)
                    BluetoothDevicelistMac.add(device.address)
                } catch (var4: java.lang.Exception) {
                }
            }
        }
        return BluetoothDevicelistStr
    }

    override fun notifyListener(device: RFIDDevice, tag_model: Tag_Model) {
        try {
            Log.i("Ah-Log-XXX", "EPC " + tag_model._EPC)
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


    inner class LotNbTextWatcher : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            if (updatingText)
                return;
            clearSteps()
            updatingText = true;
            val str = txtLotNo.text.toString()
            if (str.length < 4) {
                playError()
                addFailStep("Invalid LotNo \n NO!!! $str", "")
                clearText(txtLotNo)
                return;
            }
            txtLotNo.isEnabled = false;
            ValidateLotNo(str)
        }

        override fun onTextChanged(
            s: CharSequence, start: Int, before: Int, count: Int
        ) {
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        }

    }

    inner class CartoNbTextWatcher : TextWatcher {
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
            updatingText = false
            txtCartonNo.isEnabled = false;
            addSuccessStep("Valid  Carton No $str", "")
            txtUPC.isEnabled = true;
            txtUPC.requestFocus()

        }

        override fun onTextChanged(
            s: CharSequence, start: Int, before: Int, count: Int
        ) {
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        }

    }

    inner class UPCTextWatcher : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            if (updatingText)
                return;
            clearSteps()
            updatingText = true;
            val str = txtUPC.text.toString()
            if (str.length < 10 || str.length > 14) {
                playError()
                addFailStep("Invalid UPC  $str", "")
                clearText(txtUPC)
                return;
            }
            updatingText = false
            txtUPC.isEnabled = false;
            addSuccessStep("Valid UPC  $str", "")
            Thread {
                startRfidLotBondSession(str)
            }.start()

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
            updateResult("Result", Color.BLUE)
            stepslayout.removeAllViews()
        }

    }

    fun TagModelKey(model: Tag_Model): String {
        return if ((model._EPC != null && model._TID != null) && model._EPC.isNotEmpty() && model._TID.isNotEmpty()) model._EPC + "-" + model._TID else ""
    }

    private fun addSuccessStep(title: String, desc: String, date: String?) {
        runOnUiThread {
            stepslayout.addView(StepSuccessView(this, "$title: ${getTime("" + date)}", desc))
        }
    }

    private fun addFailStep(title: String, desc: String, date: String?) {
        runOnUiThread {
            stepslayout.addView(StepFailView(this, "$title: ${getTime("" + date)}", desc))
        }
        playError()
    }

    private fun addSuccessStep(title: String, desc: String) {
        runOnUiThread {
            stepslayout.addView(StepSuccessView(this, title, desc))
        }
    }

    private fun addFailStep(title: String, desc: String) {
        runOnUiThread {
            stepslayout.addView(StepFailView(this, title, desc))
        }
        playError()
    }


    private fun updateResult(title: String, color: Int) {
        runOnUiThread {
            lblResult.text = title;
            lblResult.setTextColor(color)
        }
    }


    private fun setTitle() {
        mTitle = DefaultNavigationBar.Builder(
            this,
            findViewById<View>(R.id.activity_rfid_bulk) as ViewGroup
        )
            .setLeftText("Rfid Lot Bond Bulk")
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
            v, this@RfidLotBondBulkActivity
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

    override fun notifyStartAntenna(ant: Int) {
        TODO("Not yet implemented")
    }

    override fun notifyStopAntenna(ant: Int) {
        TODO("Not yet implemented")
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
                   val intent = Intent (applicationContext, RfidLotBondBulkMainMenuActivity::class.java)
                   startActivity(intent)
                    finish()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .show()

        }
    }


    var updatingText = false
    lateinit var stepslayout: LinearLayout
    lateinit var txtLotNo: EditText
    lateinit var txtCartonNo: EditText
    lateinit var txtUPC: EditText
    lateinit var lblRfidDevice: TextView
    lateinit var lblResult: TextView
    lateinit var btnDone: Button
    lateinit var api: BasicApi
    var mStorage: Storage? = null
    var UserID: Int = -1
    var IPAddress = ""
    var compositeDisposable = CompositeDisposable()
    private var LotBondStation = ""
    lateinit var stationModel: RfidStationModel
    lateinit var session: RfidLotBondSessionModel
    lateinit var stationMessages: HashMap<String, String>
    private val bbbList = HashMap<String, Tag_Model>()
    private val hmList_Lock = Any()
    var countDownTimer: CountDownTimer? = null
    var RFIDGrp1ReadTime1 = 0
    var BluetoothDeviceList: ArrayList<BluetoothDevice> = arrayListOf()
    var BluetoothDevicelistStr: ArrayList<String> = arrayListOf()
    var BluetoothDevicelistMac: ArrayList<String> = arrayListOf()
    private lateinit var socket: StationSocket
    val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
    var rs = -1000;
    lateinit var lblConveyor: TextView
    var allowSession=true
    fun onCheckboxClicked(view: View) {}

}