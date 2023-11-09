package com.hc.mixthebluetooth.activity.rfidLotBond

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.widget.*
import androidx.annotation.RequiresApi
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.*
import java.time.LocalDateTime

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("MissingPermission")
class RfidLotBondBulkEnhancedActivity : RfidLotBondEnhancedActivity() {

    override fun startRfidLotBondSession(itemSerial: String) {
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
            startRfidReading(rfidGrp1ReadTime1.toLong())
            if(conveyorMode)
                 moveClientForward()
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
    override fun accept(msg: String) {
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
                finalize()
            }.start()

        } catch (e: Exception) {
            addFailStep("Conveyor Failure", "Error " + e.message + ";" + e.stackTrace)

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




}