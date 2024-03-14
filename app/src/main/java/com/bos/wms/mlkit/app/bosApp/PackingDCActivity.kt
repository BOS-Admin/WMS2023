package com.bos.wms.mlkit.app.bosApp

import Model.BosApp.Packing.FillBinDCModel
import Model.BosApp.Packing.FillBinDCModelItem
import Remote.APIClient
import Remote.BasicApi
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.R
import com.bos.wms.mlkit.app.Logger
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import java.io.IOException


class PackingDCActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_packing_dc)
        general = General.getGeneral(applicationContext)
        textUser = findViewById(R.id.textUser)
        textBranch = findViewById(R.id.textBranch)
        val general = General.getGeneral(applicationContext)
        IPAddress = general.ipAddress
        mStorage = Storage(applicationContext)
        IPAddress2 = mStorage.getDataString("IPAddress2", "192.168.51.20")
        var UserId = general.UserID
        locationId = general.mainLocationID
        textBranch.text = general.fullLocation;
        textUser.text = general.userFullName;

        lblError = findViewById(R.id.lblError)
        lblScanError = findViewById(R.id.lblScanError)
        btnNext = findViewById(R.id.btnNext)
        btnDelete = findViewById(R.id.btnDelete)
        btnDone = findViewById(R.id.btnDone)
        btnPrev = findViewById(R.id.btnPrev)
        textBoxNb = findViewById(R.id.textBox)
        textLastItem = findViewById(R.id.textLastItem)
        textItemScanned = findViewById(R.id.textItemScanned)

        PackingTypeId = general.packReasonId
        textBoxNb.text = general.boxNb
        textBranch.isEnabled = false;
        textUser.isEnabled = false;
        textBoxNb.isEnabled = false;
        textItemScanned.requestFocus()
        textLastItem.isEnabled = false

        queuePackingThreshold = General.getGeneral(application).getSetting(applicationContext, "QueueItemPackingValidationThreshold").toInt()

        textItemScanned.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                Log.i("Ah-Log", textItemScanned.text.toString())
                if (updatingText)
                    return;
                updatingText = true;
                lblScanError.text = "";
                var item = textItemScanned.text.toString()
                if (item.length < 5) {
                    Beep()
                    lblScanError.text = "Invalid ItemCode"
                    textItemScanned.setText("")
                    updatingText = false;
                    return;
                }
                if((!(item.startsWith("230"))) &&  isValidUPCA(item))
                    item = convertToIS(item)

                if (items.containsValue(item)) {
                    Beep()
                    lblScanError.text = "$item Scanned Twice !!!"
                    textItemScanned.setText("")
                    updatingText = false;
                    return;
                }
                Log.i("Ah-Log", "3")

                ValidateScan(item)
                textItemScanned.isEnabled = false


            }

            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int, count: Int
            ) {
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

        })

        btnNext.setOnClickListener {
            if (items.size == 0)
                return@setOnClickListener
            index++;
            if (!items.containsKey(index)) {
                index = 0
            }
            textLastItem.text = items[index]
        }

        btnPrev.setOnClickListener {
            if (items.size == 0)
                return@setOnClickListener
            index--;
            if (!items.containsKey(index)) {
                index = items.size - 1
            }
            textLastItem.text = items[index]
        }

        btnDone.setOnClickListener {
            proceed()
        }
        btnDelete.setOnClickListener {
            if (items == null || items.size == 0)
                return@setOnClickListener

            AlertDialog.Builder(this)
                .setTitle("Delete entry")
                .setMessage("Are you sure you want to delete this entry?") // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.yes,
                    DialogInterface.OnClickListener { dialog, which ->
                        removeLastItem()
                    }) // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()

        }

    }

    fun removeLastItem() {
        var itemSerial = items.remove(items.size - 1)

        if(itemsQueueCount.containsKey(itemSerial)){
            itemsQueueCount.remove(itemSerial)
        }

        index = items.size - 1
        if (index < 0) {
            textLastItem.text = ""
            return
        } else textLastItem.text = items[index]
    }

//region UPCA
    fun calculateUPCAChecksum(barcodeWithoutChecksum: String): Int {
        val reversed = barcodeWithoutChecksum.reversed().toCharArray()
        val sum = (0 until reversed.size).sumOf { i ->
            Character.getNumericValue(reversed[i]) * if (i % 2 == 0) 3 else 1
        }
        return (10 - sum % 10) % 10
    }


    fun convertToIS(upca: String): String {
        return "IS00" + upca.substring(2, upca.length - 1)
    }
    fun isValidUPCA(barcode: String): Boolean {
        if(barcode.startsWith("230"))
            return false;

        if ( barcode.length != 12 || ( !barcode.startsWith("22") && !barcode.startsWith("23")) ){
            return false
        }

        val checksumDigit = Character.getNumericValue(barcode[11])
        val barcodeWithoutChecksum = barcode.substring(0, 11)
        val expectedChecksum = calculateUPCAChecksum(barcodeWithoutChecksum)

        return checksumDigit == expectedChecksum
    }
//endregion


    companion object {
        @JvmStatic
        public fun executeCommand(ip:String): String {
            Log.i("AH-Log-Packing", " executeCommand")
            val runtime = Runtime.getRuntime()
            try {
                val mIpAddrProcess = runtime.exec("/system/bin/ping -c 1 $ip")
                val mExitValue = mIpAddrProcess.waitFor()
                Log.i("AH-Log-Packing", " mExitValue $mExitValue")
                if(mExitValue==0)
                    return "Success"
                else
                    return "Failed \nExitValue=$mExitValue "
            } catch (ignore: InterruptedException) {
                ignore.printStackTrace()
                return "Failed\nException:\n$ignore"
            } catch (e: IOException) {
                e.printStackTrace()
                return "Failed\nException:\n$e"
            }
            return "Failed"
        }

    }






    fun ValidateScan(ItemSerial: String) {
//        var start:Long=System.currentTimeMillis();
        try {
            textItemScanned.isEnabled = false
            btnNext.isEnabled = false
            btnPrev.isEnabled = false

            var itemCode = ItemSerial

            if(isValidUPCA(itemCode))
               itemCode = convertToIS(itemCode)
            else
                if (!itemCode.startsWith("IS"))
                    itemCode = "IN$itemCode"



            Log.i("Ah-Log", "Packing Reason Id " + general.packReasonId)
            //Logger.Debug("PackingDC","Ping" ,"Packing Reason Id " + general.packReasonId)
            api = APIClient.getInstance(IPAddress, true).create(BasicApi::class.java)


//
//            Log.i("AH-Log-Packing", "Start Ping")
//            Logger.Debug("PackingDC.log","Ping" ,"Start Ping ($ItemSerial)")
//            start = System.currentTimeMillis()
//            val x=executeCommand()
//            Logger.Debug("PackingDC.log","Ping" ,"End Ping: ($ItemSerial) => $x : => (${System.currentTimeMillis()-start}) ")
//            Log.i("AH-Log-Packing", "End Ping: $x")


            Log.i("Ah-Log", "Packing Reason Id " + general.packReasonId)

            Logger.Debug("PackingDC","API Start ItemSerial: ($ItemSerial) ")
//            start = System.currentTimeMillis()

            api = APIClient.getInstance(IPAddress, false).create(BasicApi::class.java)
            api2 = APIClient.getNewInstanceStatic(IPAddress2).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.ValidateFillBinItem(
                    itemCode,
                    general.mainLocation,
                    general.UserID,
                    general.packReasonId,textBoxNb.text.toString()
                )
                    .subscribeOn(Schedulers.io())
                    // .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            var response = try {
                                s.string()
                            } catch (e: Exception) {
                                e.message.toString()
                            }
                            runOnUiThread {
                                btnNext.isEnabled = true
                                btnPrev.isEnabled = true
                            }

                            if (response != null && response == "success") {
                                Logger.Debug("PackingTest", "OriginalLocation " + general.mainLocation + " Destination: " + general.destination)
                                if(general.mainLocation.equals("W1005")){

                                    runOnUiThread {
                                        btnNext.isEnabled = false
                                        btnPrev.isEnabled = false
                                    }

                                    try {
                                        compositeDisposable.addAll(
                                            api2.ValidateFillBinItemLocal(itemCode, general.destination).subscribeOn(Schedulers.io())
                                            .subscribe(
                                                    { s ->
                                                        var response = try {
                                                            s.string()
                                                        } catch (e: Exception) {
                                                            e.message.toString()
                                                        }
                                                        runOnUiThread {
                                                            btnNext.isEnabled = true
                                                            btnPrev.isEnabled = true
                                                        }

                                                        if (response != null && response.contains("success")) {

                                                            if(IsValidQueueCheckBin(textBoxNb.text.toString())) {
                                                                var splitArgs = response.split('-');
                                                                if (splitArgs.size > 1) {
                                                                    try {
                                                                        var currentQueueID = splitArgs[1].toInt();
                                                                        var currentQueueItemCount = splitArgs[2].toInt();
                                                                        Logger.Debug("PACKING-QID", "Got Item Queue ID: " + itemCode + " Queue: " + currentQueueID + " Count: " + currentQueueItemCount);
                                                                        /*if(!queueItemCount.containsKey(currentQueueID)){
                                                                            queueItemCount.put(currentQueueID, currentQueueItemCount);
                                                                        }*/
                                                                        if (!itemsQueueCount.containsKey(itemCode)) {
                                                                            itemsQueueCount.put(itemCode, currentQueueID);
                                                                        }
                                                                        try {
                                                                            var threshold:Double = (queuePackingThreshold / 100.0);
                                                                            var queueIDSize = 0

                                                                            for ((key, value) in itemsQueueCount){
                                                                                if(value == currentQueueID){
                                                                                    queueIDSize++;
                                                                                }
                                                                            }

                                                                            Logger.Debug("PACKING-QID-COUNT", "Got Total QID: " + currentQueueID + " Item Size: " + queueIDSize);
                                                                            var maxSizeForQueue = currentQueueItemCount * threshold
                                                                            if(queueIDSize > maxSizeForQueue){
                                                                                var errorMessage = "Error Queue ID: " + currentQueueID + " Reached Max Items From Same Stand (" + queueIDSize + ")/" + currentQueueItemCount + ", MAX: " + maxSizeForQueue + "(" + queuePackingThreshold + "%)";
                                                                                //showMessage(errorMessage, Color.RED)
                                                                                /*runOnUiThread {
                                                                                    itemsQueueCount.clear()
                                                                                    queueItemCount.clear()
                                                                                    items.clear()
                                                                                    textLastItem.text = ""
                                                                                    index = -1;
                                                                                    btnDone.isVisible = false
                                                                                    btnDelete.isVisible = false
                                                                                }*/
                                                                                runOnUiThread {
                                                                                    Beep()
                                                                                    lblScanError.setTextColor(Color.RED)
                                                                                    lblScanError.text = errorMessage
                                                                                    updatingText = true
                                                                                    textItemScanned.setText("")
                                                                                    updatingText = false
                                                                                    textItemScanned.isEnabled = true
                                                                                    textItemScanned.requestFocus()
                                                                                    Log.i("DC-Packing", errorMessage)
                                                                                }
                                                                            }else {
                                                                                runOnUiThread {
                                                                                    lblScanError.setTextColor(Color.GREEN)
                                                                                    lblScanError.text = "Success"
                                                                                    index = items.size
                                                                                    items[index] = itemCode
                                                                                    textLastItem.text = itemCode
                                                                                    updatingText = true
                                                                                    textItemScanned.setText("")
                                                                                    updatingText = false
                                                                                    textItemScanned.isEnabled = true
                                                                                    textItemScanned.requestFocus()
                                                                                    Log.i("DC-Packing", response)
                                                                                }
                                                                            }

                                                                        }catch(e: Throwable){

                                                                        }
                                                                    } catch (ex: Throwable) {

                                                                    }
                                                                }else {
                                                                    runOnUiThread {
                                                                        lblScanError.setTextColor(Color.GREEN)
                                                                        lblScanError.text = "Success"
                                                                        index = items.size
                                                                        items[index] = itemCode
                                                                        textLastItem.text = itemCode
                                                                        updatingText = true
                                                                        textItemScanned.setText("")
                                                                        updatingText = false
                                                                        textItemScanned.isEnabled = true
                                                                        textItemScanned.requestFocus()
                                                                        Log.i("DC-Packing", response)
                                                                    }
                                                                }
                                                            }else {
                                                                runOnUiThread {
                                                                    lblScanError.setTextColor(Color.GREEN)
                                                                    lblScanError.text = "Success"
                                                                    index = items.size
                                                                    items[index] = itemCode
                                                                    textLastItem.text = itemCode
                                                                    updatingText = true
                                                                    textItemScanned.setText("")
                                                                    updatingText = false
                                                                    textItemScanned.isEnabled = true
                                                                    textItemScanned.requestFocus()
                                                                    Log.i("DC-Packing", response)
                                                                }
                                                            }

                                                        } else {
                                                            runOnUiThread {
                                                                updatingText = true
                                                                textItemScanned.setText("")
                                                                updatingText = false
                                                                textItemScanned.isEnabled = true
                                                                textItemScanned.requestFocus()
                                                                showScanMessage(response, Color.RED)
                                                                Log.i("DC-Packing", response)
                                                            }
                                                        }

                                                    },
                                                    { t: Throwable? ->
                                                        run {
                                                            runOnUiThread {
                                                                btnNext.isEnabled = true
                                                                btnPrev.isEnabled = true
                                                            }

                                                            if (t is HttpException) {
                                                                var ex: HttpException = t as HttpException
                                                                showScanMessage(
                                                                        ex.response().errorBody()!!.string() + " - " + t?.message,
                                                                        Color.RED
                                                                )

                                                            } else {

                                                                showScanMessage(t?.message + " (API Error)", Color.RED)

                                                                val error=t?.message + " (API Error)"
                                                                var start:Long=System.currentTimeMillis();
                                                                Logger.Debug("PackingDC","API-Timeout $error \n Start Ping ($ItemSerial)")
                                                            }

                                                            Logger.Debug("PACKING-ERROR-2", t?.message + " Local: " + IPAddress2);

                                                            runOnUiThread {
                                                                updatingText = true
                                                                textItemScanned.setText("")
                                                                updatingText = false
                                                                textItemScanned.isEnabled = true
                                                                textItemScanned.requestFocus()
                                                            }
                                                        }
                                                    }
                                            )
                                        )
                                    }
                                    catch (e: java.lang.Exception) {
                                        Logger.Debug("PACKING-ERROR", e?.message);
                                        showScanMessage("Error:" + e?.message, Color.RED)
                                    }

                                }else {
                                    runOnUiThread {
                                        lblScanError.setTextColor(Color.GREEN)
                                        lblScanError.text = response
                                        index = items.size
                                        items[index] = itemCode
                                        textLastItem.text = itemCode
                                        updatingText = true
                                        textItemScanned.setText("")
                                        updatingText = false
                                        textItemScanned.isEnabled = true
                                        textItemScanned.requestFocus()
                                        Log.i("DC-Packing", response)
                                    }
                                }
                            } else {
                                runOnUiThread {
                                    updatingText = true
                                    textItemScanned.setText("")
                                    updatingText = false
                                    textItemScanned.isEnabled = true
                                    textItemScanned.requestFocus()
                                    showScanMessage(response, Color.RED)
                                    Log.i("DC-Packing", response)
                                }
                            }

                        },
                        { t: Throwable? ->
                            run {
                                runOnUiThread {
                                    btnNext.isEnabled = true
                                    btnPrev.isEnabled = true
                                }

                               // Logger.Debug("PackingDC.log","API Done" ,"ItemSerial: ($ItemSerial) => (${System.currentTimeMillis()-start})")

                                if (t is HttpException) {
                                    var ex: HttpException = t as HttpException
                                    showScanMessage(
                                        ex.response().errorBody()!!.string() + " - " + t?.message,
                                        Color.RED
                                    )

                                } else {

                                    showScanMessage(t?.message + " (API Error)", Color.RED)

                                    val error=t?.message + " (API Error)"
                                    var start:Long=System.currentTimeMillis();
                                    Logger.Debug("PackingDC.log","API-Timeout $error \n Start Ping ($ItemSerial)")
//                                    val x=executeCommand()
//                                    Logger.Debug("PackingDC.log","API-Timeout" ,"$error \n End Ping ($ItemSerial): => $x => (${System.currentTimeMillis()-start})")
                                }

                                Logger.Debug("PACKING-ERROR-3", t?.message + " Local: " + IPAddress2);

                                runOnUiThread {
                                    updatingText = true
                                    textItemScanned.setText("")
                                    updatingText = false
                                    textItemScanned.isEnabled = true
                                    textItemScanned.requestFocus()
                                }
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            showScanMessage("Error:" + e?.message, Color.RED)
//            Logger.Debug("PackingDC.log","API Done" ,"ItemSerial: ($ItemSerial) => (${System.currentTimeMillis()-start})")
            //throw(IOException("UPC Pricing Activity - ValidateScan", e))
        } finally {


        }
    }

    private fun proceed() {

        try {
            btnDone.isEnabled = false
            btnDelete.isEnabled = false
            lblScanError.text = ""
            var UserID: Int = General.getGeneral(applicationContext).UserID
            var itemsStr: String = items.values.joinToString(",")
            Log.i("Ah-log", itemsStr);
            textItemScanned.isEnabled = false


            /*if(itemsQueueCount.size > 0){
                try {
                    var threshold:Double = (queuePackingThreshold / 100.0);
                    var queueIDSizes = itemsQueueCount.map { it.value }
                            .groupBy { it }
                            .map { Pair(it.key, it.value.size) }

                    for ((key, value) in queueIDSizes) {
                        Logger.Debug("PACKING-QID-COUNT", "Got Total QID: " + key + " Item Size: " + value);
                        var maxSizeForQueue = 0.0
                        var currentQueueCount = queueItemCount.get(key)
                        if(queueItemCount.containsKey(key)){
                            maxSizeForQueue = currentQueueCount!!.times(threshold)
                        }
                        if(value > maxSizeForQueue){
                            showMessage("Error Queue ID: " + key + " Reached Max Items From Same Stand (" + value + ")/" + currentQueueCount + ", MAX: " + maxSizeForQueue + "(" + queuePackingThreshold + "%)", Color.RED)
                            runOnUiThread {
                                itemsQueueCount.clear()
                                queueItemCount.clear()
                                items.clear()
                                textLastItem.text = ""
                                index = -1;
                                btnDone.isVisible = false
                                btnDelete.isVisible = false
                            }
                            return;
                        }
                    }

                }catch(e: Throwable){

                }
            }*/


            var modelItems: ArrayList<FillBinDCModelItem> = arrayListOf()
            for (it in items)
                modelItems.add(FillBinDCModelItem(it.value))

            var model =
                FillBinDCModel(
                    textBoxNb.text.toString(),
                    general.UserID,
                    modelItems,
                    PackingTypeId,
                    general.destination,
                    locationId
                )


            api = APIClient.getInstance(IPAddress, true).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.FillBinDC(model)
                    .subscribeOn(Schedulers.io())
                    // .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            var response = try {
                                s.string()
                            } catch (e: Exception) {
                                e.message.toString()
                            }
                           // Log.i("Ah-log", response);
                            if (response != null && (response.lowercase()
                                    .startsWith("released") || response.lowercase()
                                    .startsWith("success"))
                            ) {
                                showMessage(response, Color.GREEN)
                            } else {


                                if (response != null && response.trim() == "Status: (103,Released)"){
                                    showMessage("" + response?.toString(), Color.GREEN)
                                }
                                else
                                showMessage("" + response?.toString(), Color.RED)



                            }
                            runOnUiThread {
                                itemsQueueCount.clear()
                                queueItemCount.clear()
                                items.clear()
                                textLastItem.text = ""
                                index = -1;
                                btnDone.isVisible = false
                                btnDelete.isVisible = false
                            }

                        },
                        { t: Throwable? ->
                            run {
                                if (t is HttpException) {
                                    var ex: HttpException = t as HttpException
                                    var errMsg=ex.response().errorBody()!!.string() + ""
                                    if (errMsg != null && errMsg.trim() == "Status :(101,DC)  NextStatus: (Count)"){
                                        showMessage("" + errMsg?.toString(), Color.GREEN)
                                    }

                                    else
                                    showMessage(
                                        errMsg,
                                        Color.RED
                                    )
                                } else {
                                    if (t?.message != null)
                                        showMessage(
                                            t.message.toString() + " (API Error )",
                                            Color.RED
                                        )
                                }
                                runOnUiThread {
                                    btnDone.isVisible = false
                                    btnDelete.isVisible = false

                                }
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            showMessage("Error: " + e?.message, Color.RED)

        } finally {

        }
    }

    private fun Beep() {
        ToneGenerator(
            AudioManager.STREAM_MUSIC,
            ToneGenerator.MAX_VOLUME
        ).startTone(ToneGenerator.TONE_SUP_ERROR, 300)
    }


    private fun showScanMessage(msg: String, color: Int) {
        if (color == Color.RED)
            Beep()
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle("Item Error")
                .setMessage(msg)
                .setPositiveButton("OK") { _, _ ->
//                    val intent = Intent (applicationContext, PackingActivity::class.java)
//                    startActivity(intent)
                    //finish()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                // .setCancelable(false)
                .show()
            lblScanError.setTextColor(color)
            lblScanError.text = msg
        }
    }

    private fun showMessage(msg: String, color: Int) {
        if (color == Color.RED)
            Beep()
        Log.i("Ah-log", msg);

        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle("Result")
                .setMessage(msg)
                .setPositiveButton("OK") { _, _ ->
                    finish()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .show()
            lblError.setTextColor(color)
            lblError.text = msg
        }
    }

    fun IsValidQueueCheckBin(box: String): Boolean {
        val validations = General.getGeneral(application).getSetting(applicationContext, "QueueItemPackingValidationBoxes")
        if (validations == null) {
            Logger.Error("SystemControl", "Failed Finding System Control Value With QueueItemPackingValidationBoxes")
            return false
        }
        val args = validations.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (args.size == 0) return box.startsWith(validations)
        for (arg in args) {
            if (box.startsWith(arg)) return true
        }
        return false
    }

    private var itemsQueueCount: HashMap<String, Int> = HashMap();

    private var queueItemCount: HashMap<Int, Int> = HashMap();

    private lateinit var textUser: TextView
    private lateinit var textBranch: TextView
    private lateinit var lblError: TextView
    private lateinit var lblScanError: TextView
    private lateinit var textBoxNb: TextView
    private lateinit var textItemScanned: EditText
    private lateinit var textLastItem: TextView
    private lateinit var btnNext: Button
    private lateinit var btnDelete: Button
    private lateinit var btnDone: Button
    private lateinit var btnPrev: Button
    private var queuePackingThreshold: Int = 0
    private var PackingTypeId: Int = -1
    private var locationId: Int = -1
    private lateinit var general: General
    lateinit var api: BasicApi
    lateinit var api2: BasicApi
    var compositeDisposable = CompositeDisposable()
    lateinit var mStorage: Storage;
    private var items: HashMap<Int, String> = HashMap();
    private var index: Int = -1
    var IPAddress = ""
    var IPAddress2 = ""
    var updatingText = false;

}


