package com.hc.mixthebluetooth.activity.rfidLotBond

import Remote.APIClient
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import android.widget.*
import androidx.annotation.RequiresApi
import com.hc.mixthebluetooth.Logger
import com.hc.mixthebluetooth.Model.RfidLotBond.RfidLotBondSessionModel
import com.hc.mixthebluetooth.Remote.Routes.BasicApi
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.*
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import java.time.LocalDateTime

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("MissingPermission")
class ItemSerialLotBondBulkEnhancedActivity : ItemSerialLotBondEnhancedActivity() {



    override fun StartItemSerialReadSession(itemSerial: String) {
        if(!allowSession){
            addFailStep("Session Not Allowed", "allow session: $allowSession")
            return
        }
        session.itemSerial = itemSerial
        session.sessionStartDate = dtf.format(LocalDateTime.now())
        var x= if(conveyorMode)  checkIfItemOnConveyor() else true

        //test remove later
        // x=true
        if(x){
            if(conveyorMode){
                moveClientForward()
            }

            var time : Long = 800;//Milliseconds

            try {
                time = rfidGrp1ReadTime1.toLong()
                Logger.Debug("ISLOTBOND", "Lot Bond Wait Time Set To: " + time);
            }catch (ex: Exception){

            }

            runOnUiThread {
                countDownTimer?.cancel()
                countDownTimer = object : CountDownTimer( time, 300) {
                    override fun onTick(l: Long) {
                    }

                    override fun onFinish() {
                        runOnUiThread {
                            attemptLotBondForItem(itemSerial)
                        }
                    }
                }.start()
            }
        }
    }
    private fun checkIfItemOnConveyor(): Boolean {
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
                        return true
                    }
                    stationMessages["X0 OFF"] -> {
                        addFailStep(
                                "Item Not On Conveyor XXX !!!!",
                                "X0 OFF: $response", session.checkItemOnConveyorRecDate
                        )
                        addFailStep("Please place the item on the conveyor before scanning", "")
                        return false
                    }
                    else -> {
                        addFailStep(
                                "Failed to check if item is on conveyor",
                                "Invalid conveyor response ($response)",
                                session.checkItemOnConveyorRecDate
                        )
                        showErrorMessage("Invalid conveyor response ($response)")

                        return false
                    }

                }
                //Testing
                //  if (!success)
                //   MoveClientForward(UPC)
            }
            return false

        } catch (e: Throwable) {
            addFailStep("Failed to check if item is on conveyor", "Error:" + e?.message)
            return false
        }

    }
    private fun moveClientForward() {

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
                        addSuccessStep(
                                "Moving Forward Success M0 \u2713 ",
                                "Tatex Receive M0 ON : " + session.tatexMsgRec, session.tatexMsgRecDate
                        )
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
                        showErrorMessage("Invalid conveyor response (${session.tatexMsgRec})")
                    }



                }
            }


        } catch (e: Throwable) {
            addFailStep("Failed to move conveyor forward M0", "Error:" + e?.message)

        }
    }
    override fun setTitle() {
        setTitle("Rfid Lot Bond Bulk Enhanced")
    }

    fun attemptLotBondForItem(itemSerial: String) {
        try {
            session.rfid="ItemSerialLotBond"
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
                                                    "Item Serial Lot Bond Success",
                                                    rs,
                                                    session.rfidLotBondStop
                                            )
                                            accept("Lot bond success: $rs")
                                        } else {
                                            addFailStep(
                                                    "Item Serial Lot Bond Failure XXX",
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
                                                        "Item Serial Lot Bond Failure",
                                                        ex.response().errorBody()!!
                                                                .string() + "(API Http Error)"
                                                )
                                                session.rfidLotBondMessage =
                                                        ex.response().errorBody()!!
                                                                .string() + "(API Http Error)"

                                            } else {
                                                addFailStep(
                                                        "Item Serial Lot Bond Failure",
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
            addFailStep("Item Serial Lot Bond Failure", "Error:" + e?.message)
            reject("Lot bond fail: Exception${e?.message}")
        }

    }

    override fun accept(msg: String) {
        Logger.Debug("ISLOTBOND", "Accept Message Received For Bulk: " + msg);
        try {
            Thread{
                updateResult("Lot Bond Success ✓", Color.GREEN)
                addSuccessStep("Lot Bond Success", msg, dtf.format(LocalDateTime.now()))
                session.rfidLotBondMessage = msg
                if (conveyorMode) {
                    if (!stationMessages.containsKey("RFID Send")) {
                        addFailStep("Failed to find 'RFID Send' message", "")
                    } else {
                        session.rfidMsgSentDate = dtf.format(LocalDateTime.now())
                        stationMessages["RFID Send"]?.let {
                            session.rfidMsgSent = it
                            addSuccessStep(
                                    "Moving Conveyor Forward M1 ",
                                    "'RFID Send': " + session.rfidMsgSent,
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
                                else -> {
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
                }
                finalizeLotBond()
            }.start()

        } catch (e: Exception) {
            addFailStep("Conveyor Failure", "Error " + e.message + ";" + e.stackTrace)

        }

    }

    fun finalizeLotBond() {
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
                                                    "Item Serial Lot Bond session is Done",
                                                    "Updating session was successful"
                                            )
                                        else
                                            addFailStep(
                                                    "Item Serial Lot Bond session is Done",
                                                    "Updating session was unsuccessful"
                                            )

                                        runOnUiThread {
                                            sessionInProgress=false
                                            clearText(txtItemSerial)
                                        }

                                    },
                                    { t: Throwable? ->
                                        run {

                                            if (t is HttpException) {
                                                var ex: HttpException = t
                                                addFailStep(
                                                        "Item Serial Lot Bond session is Done",
                                                        ex.response().errorBody()!!.string() + "(API Http Error)"
                                                )

                                            } else
                                                addFailStep("Item Serial Lot Bond session is Done", t?.message + " (API Error)")
                                        }

                                        runOnUiThread {
                                            sessionInProgress=false
                                            clearText(txtItemSerial)
                                        }
                                    }
                            )
            )
        } catch (e: Throwable) {
            addFailStep("Item Serial Lot Bond session is Done", "Error:" + e?.message)
        }

    }

    override fun reject(msg: String ) {


        try {

            session.rfidLotBondMessage = msg
            addFailStep("Rejected", msg, dtf.format(LocalDateTime.now()))
        } catch (e: Exception) {
            addFailStep( "Failure", "Error " + e.message + ";" + e.stackTrace)
        } finally {

        }

        finalize()
        updateResult("Lot Bond Fail\n$msg", Color.RED)
        playError()


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

    override fun resetItemSerialLotBonSession(){
        super.resetItemSerialLotBonSession()
        countDownTimer?.cancel()
    }

    var countDownTimer: CountDownTimer? = null


}