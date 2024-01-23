package com.bos.wms.mlkit.app.bosApp

import Model.BosApp.FillStockTakeDCModel
import Model.BosApp.FillStockTakeDCModelItem
import Remote.APIClient
import Remote.APIClient.getInstanceStatic
import Remote.BasicApi
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.R
import com.bos.wms.mlkit.app.Logger
import com.bos.wms.mlkit.storage.Storage
import com.google.android.material.snackbar.Snackbar
import com.rfidread.Enumeration.eReadType
import com.rfidread.Helper.Helper_ThreadPool
import com.rfidread.Interface.IAsynchronousMessage
import com.rfidread.Models.Tag_Model
import com.rfidread.RFIDReader
import com.rfidread.Tag6B
import com.rfidread.Tag6C
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_scan_container.*
import retrofit2.HttpException


class StockTakeDCRFIDActivity : AppCompatActivity() {

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
    private var PackingTypeId: Int = -1
    private var locationId: Int = -1
    private var stockTakeType: Int = -1
    private lateinit var lblBox: TextView
    private var lastDetectedRFIDTag = ""
    private var RFIDName = ""
    private var RFIDMac = ""
    private var IsRFIDConnected = false
    var mainProgressDialog: ProgressDialog? = null
    var CurrentBarcode: String? = null
    val BluetoothDeviceList: MutableList<BluetoothDevice> = mutableListOf()
    val BluetoothDevicelistStr: MutableList<String> = mutableListOf()
    val BluetoothDevicelistMac: MutableList<String> = mutableListOf()
    var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    var dialogBuilder: AlertDialog.Builder? = null
    var dialogView: View? = null
    var dialogTitle: TextView? = null
    var btnResult: Button? = null
    var dialogScan: AlertDialog? = null
    var tempDialogScan: AlertDialog? = null
    private var singleProcessLocked = false
    var CanConnectRFID = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_packing_dc)


        textUser = findViewById(R.id.textUser)
        textBranch = findViewById(R.id.textBranch)
        lblError = findViewById(R.id.lblError)
        lblScanError = findViewById(R.id.lblScanError)
        btnNext = findViewById(R.id.btnNext)
        btnDelete = findViewById(R.id.btnDelete)
        btnDone = findViewById(R.id.btnDone)
        btnPrev = findViewById(R.id.btnPrev)
        textBoxNb = findViewById(R.id.textBox)
        textLastItem = findViewById(R.id.textLastItem)
        textItemScanned = findViewById(R.id.textItemScanned)
        lblBox = findViewById(R.id.lblBox)
        mStorage = Storage(applicationContext)
        val general = General.getGeneral(applicationContext)
        IPAddress = general.ipAddress
        var FloorID: Int = general.FloorID
        var UserId = general.UserID
        PackingTypeId = general.packReasonId
        locationId = general.mainLocationID
        stockTakeType = general.stockType
        if (stockTakeType == 101)
            lblBox.text = "Stand"
        else
            lblBox.text = "Box"
        textBranch.text = "" + General.getGeneral(applicationContext).fullLocation;
        textUser.text = UserId.toString() + " " + General.getGeneral(applicationContext).UserName;
        textBoxNb.text = General.getGeneral(applicationContext).boxNb
        textBranch.isEnabled = false;
        textUser.isEnabled = false;
        textBoxNb.isEnabled = false;
        textItemScanned.requestFocus()
        textLastItem.isEnabled = false


        dialogBuilder = AlertDialog.Builder(this@StockTakeDCRFIDActivity)
        dialogView = layoutInflater.inflate(R.layout.dialog_scan_rfid, null)
        // Ensure dialogView is not null before proceeding
        if (dialogView != null) {
            dialogBuilder!!.setView(dialogView)
            dialogScan = dialogBuilder!!.create()
            dialogScan?.setCanceledOnTouchOutside(false)

            // Get references safely
            dialogTitle = dialogView!!.findViewById(R.id.dialogTitle)
            btnResult = dialogView!!.findViewById(R.id.btnResult)

        } else {
            Logger.Debug("StocktakeRFID", "Error in reference")
        }




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

                if (isValidUPCA(item))
                    item = convertToIS(item)

                if (items.containsValue(item)) {
                    Beep()
                    lblScanError.text = "$item Scanned Twice !!!"
                    textItemScanned.setText("")
                    updatingText = false;
                    return;
                }
                Log.i("Ah-Log", "3")
                Logger.Debug("StockTakeTest", "to show dialog ")
                dialogScan?.show();
                if(CanConnectRFID)
                    RFIDStartRead()

                CurrentBarcode = item
                singleProcessLocked = true
                //block threads to complete after approval (callback)


            }

            override fun onTextChanged(
                    s: CharSequence, start: Int, before: Int, count: Int) {
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
        Logger.Debug("StockTakeTest", "will AttemptRFIDReaderConnection ")
        AttemptRFIDReaderConnection()
        mainProgressDialog = ProgressDialog.show(
                this@StockTakeDCRFIDActivity,
                "",
                "Connecting To Sled, Please wait...",
                true
        )
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
        if (barcode.length != 12 || (!barcode.startsWith("22") && !barcode.startsWith("23"))) {
            return false
        }

        val checksumDigit = Character.getNumericValue(barcode[11])
        val barcodeWithoutChecksum = barcode.substring(0, 11)
        val expectedChecksum = calculateUPCAChecksum(barcodeWithoutChecksum)

        return checksumDigit == expectedChecksum
    }
    //endregion


//region RFID reader
    /**
     * This function gets the Item Serial From The RFID Tag
     * @param rfid
     */
    fun CheckDetectedRFIDTag(rfid: String) {
        Logger.Debug("API", "CheckBarcodePreviousOCR - Detected RFID: $rfid Checking IS Now")
        // RFIDStopRead()
        try {
            Logger.Debug("StockTakeTest", "Start API call")
            val api = getInstanceStatic(IPAddress, false).create(BasicApi::class.java)
            val compositeDisposable = CompositeDisposable()
            compositeDisposable.addAll(
                    api.GetRFIDItemNumber(rfid)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ s: String? ->
                                if (s != null) {
                                    // our logic
                                    CurrentBarcode?.let {
                                        CheckRFIDScanResult(it, s
                                        ) { isSuccess ->
                                            if (isSuccess) {
                                                ValidateScan(CurrentBarcode!!)
                                                textItemScanned.isEnabled = false

                                            } else {
                                                runOnUiThread {
                                                    updatingText = true
                                                    textItemScanned.setText("")
                                                    updatingText = false
                                                    textItemScanned.isEnabled = true
                                                    textItemScanned.requestFocus()
                                                    Logger.Debug("StockTakeTest", "calling show scan Message")
                                                    showScanMessage("Mismatch RFID for item: $CurrentBarcode", Color.RED)
                                                    Log.i("DC-Packing", "Mismatch RFID for item: $CurrentBarcode")
                                                }
                                            }
                                        }

                                    }

                                    Logger.Debug("StockTakeTest", s)

                                }
                            }) { throwable: Throwable ->
                                RFIDStartRead()
                                Logger.Error("API", "CheckDetectedRFIDTag - Error In Response: " + throwable.message)
                                ShowSnackBar("Server Error: " + throwable.message, Snackbar.LENGTH_LONG)
                            })
        } catch (e: Throwable) {
            Logger.Error("API", "CheckDetectedRFIDTag - Error Connecting: " + e.message)
            CurrentBarcode = null
            ShowSnackBar("Connection Error Occurred", Snackbar.LENGTH_LONG)
            RFIDStartRead()
        }
        singleProcessLocked = false
        Logger.Debug("StockTakeTest","unlocked process")
    }


    fun CheckRFIDScanResult(
            scannedBarcode: String,
            RFIDBarcode: String,
            callback: (Boolean) -> Unit
    ) {
        Logger.Debug("StockTakeTest", "scannedBarcode: " + scannedBarcode + " RFIDBarcode: " + RFIDBarcode)
        val res: Boolean = scannedBarcode == RFIDBarcode

        if (res) {
            Logger.Debug("StockTakeTest", "Matched")
            runOnUiThread {
                dialogScan!!.dismiss()
                RFIDStopRead()
            }
        } else {
            runOnUiThread {
                dialogScan!!.dismiss()
                RFIDStopRead()
            }
        }
        return callback(res)
    }

    //region RFID configurations
    private fun ConnectToRFIDReader() {
        try {

            RFIDReader.CloseAllConnect()
            Logger.Debug("StockTakeTest", "RFIDReader.CloseAllConnect()")
            RFIDReader.GetBT4DeviceStrList()
            Logger.Debug("StockTakeTest", "RFIDReader.GetBT4DeviceStrList()")
            if (RFIDName != null && !RFIDName.isEmpty()) {
                if (!RFIDReader.CreateBT4Conn(RFIDName, object : IAsynchronousMessage {

                            override fun WriteDebugMsg(s: String) {}
                            override fun WriteLog(s: String) {}
                            override fun PortConnecting(s: String) {}
                            override fun PortClosing(s: String) {}
                            override fun OutPutTags(model: Tag_Model) {
                                val key: String = TagModelKey(model)
                                if(key.startsWith("303"))
                                    return

                                if (!key.isEmpty()) {
                                    Logger.Debug("StockTakeTest","checking Lock, current lock status is: $singleProcessLocked")
                                    if (!singleProcessLocked)
                                        return
                                    singleProcessLocked = false
                                    Logger.Debug("StockTakeTest","Process Locked")
                                    if (lastDetectedRFIDTag !== key) {
                                        lastDetectedRFIDTag = key
                                        Logger.Debug("ConnectToRFIDReader", "will check detected rfid tag")
                                        Logger.Debug("StockTakeTest", "CheckDetectedRFIDTag( " + key + ")")
                                        CheckDetectedRFIDTag(key)
                                    }
                                }
                            }

                            override fun OutPutTagsOver() {}
                            override fun GPIControlMsg(i: Int, i1: Int, i2: Int) {}
                            override fun OutPutScanData(bytes: ByteArray) {}
                        })) {
                    Logger.Error("RFID-READER", "ConnectToRFIDReader - Error Connecting to $RFIDMac")
                } else {
                    Logger.Debug("RFID-READER", "ConnectToRFIDReader - RFID-$RFIDMac Connected Successfully")
                    OnRFIDReaderConnected()
                    RFIDReader._Config.SetReaderAutoSleepParam(RFIDName, false, "")
                }
            }
            val RFIDCount = RFIDReader.HP_CONNECT.size
            val lst = RFIDReader.HP_CONNECT
            if (lst.keys.stream().count() > 0) {
                IsRFIDConnected = true
            } else {
                OnRFIDReaderError("Couldn't Connect To Sled With Mac $RFIDMac")
                Logger.Error("RFID-READER", "ConnectToRFIDReader - Couldn't Connect To Sled")
            }
        } catch (ex: java.lang.Exception) {
            OnRFIDReaderError(ex.message)
            Logger.Error("RFID-READER", "ConnectToRFIDReader - " + ex.message)
        }
    }


    /**
     * This function returns the rfid key of a tag
     * @param model
     * @return
     */
    fun TagModelKey(model: Tag_Model): String {
        return if (model._EPC != null && model._TID != null && !model._EPC.isEmpty() && !model._TID.isEmpty()) model._EPC + "-" + model._TID else ""
    }

    /**
     * This function is called when a rfid sled is connected successfully
     */
    fun OnRFIDReaderConnected() {
        mainProgressDialog?.cancel()
        Logger.Debug("StockTakeTest", "will start read")
        //RFIDStartRead()
        CanConnectRFID = true
    }

    /**
     * This function is called when the rfid sled connection fails
     */
    fun OnRFIDReaderError(error: String?) {
        mainProgressDialog?.cancel()
        AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Connection Error")
                .setMessage(error)
                .setNegativeButton("Close") { dialogInterface: DialogInterface?, i: Int -> finish() }
                .show()
    }


    /**
     * This function configures the RFID reader to read EPC 6C/6B Tags
     */
    fun RFIDStartRead() {
        Logger.Debug("StockTakeTest", "Start Reading")
        Helper_ThreadPool.ThreadPool_StartSingle {
            try {
                if (General._IsCommand6Cor6B == "6C") { // read 6c tags
                    GetEPC_6C()
                } else { // read 6b tags
                    GetEPC_6B()
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * This function stop the configured rfid readings
     */
    fun RFIDStopRead() {
        Logger.Debug("StockTakeTest", "Stop Reading function call")
        try {
            Logger.Debug("StockTakeTest", "before stop")
            RFIDReader._Config.Stop(RFIDName)
            Logger.Debug("StockTakeTest", "after stop")
        } catch (e: java.lang.Exception) {
            // TODO Auto-generated catch block
            Logger.Error("RFID-READER", "RFIDStopRead - Error While Stopping The Read Command: " + e.message)
            e.printStackTrace()
        }
        Logger.Debug("StockTakeTest", "end of function")
    }

    private fun GetEPC_6C(): Int {
        val RFID = RFIDName
        var ret = -1
        if (!RFIDReader.HP_CONNECT.containsKey(RFID)) return ret
        ret = Tag6C.GetEPC_TID(RFID, 1, eReadType.Inventory)
        return ret
    }

    private fun GetEPC_6B(): Int {
        val RFID = RFIDName
        var ret = -1
        if (!RFIDReader.HP_CONNECT.containsKey(RFID)) return ret
        ret = Tag6B.Get6B(RFID, 1, eReadType.Inventory.GetNum(), 0)
        return ret
    }

    fun ShowSnackBar(message: String?, length: Int) {
        Snackbar.make(findViewById(R.id.pasBrandOCRActiviyLayout), message!!, length)
                .setAction("No action", null).show()
    }

    fun AttemptRFIDReaderConnection() {
        try {
            GetBT4DeviceStrList()
            val mStorage = Storage(this) //sp存储
            RFIDMac = mStorage.getDataString("RFIDMac", "00:15:83:3A:4A:26")
            RFIDName = GetBluetoothDeviceNameFromMac(RFIDMac)
            object : CountDownTimer(200, 200) {
                override fun onTick(l: Long) {}
                override fun onFinish() {
                    Logger.Debug("StockTakeTest", "will connect to RFID reader")
                    ConnectToRFIDReader()
                }
            }.start()
        } catch (ex: java.lang.Exception) {
            Logger.Error("RFID-READER", "AttemptRFIDReaderConnection - " + ex.message)
        }
    }

    /**
     * This function returns all the Bluetooth paired devices
     * @return
     */
    @SuppressLint("MissingPermission")
    fun GetBT4DeviceStrList(): List<String> {
        BluetoothDeviceList.clear()
        BluetoothDevicelistStr.clear()
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        if (pairedDevices.size > 0) {
            val iterator = pairedDevices.iterator()
            while (iterator.hasNext()) {
                val device: BluetoothDevice = iterator.next()
                try {
                    BluetoothDeviceList.add(device)
                    BluetoothDevicelistStr.add(device.name)
                } catch (var4: Exception) {
                    // Handle exception if needed
                }
            }
        }
        return BluetoothDevicelistStr
    }


    /**
     * Gets the bluetooth device name from its mac address
     * @param Mac
     * @return
     */
    @SuppressLint("MissingPermission")
    private fun GetBluetoothDeviceNameFromMac(Mac: String?): String {
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
    //endregion
//endregion


    fun removeLastItem() {

        items.remove(items.size - 1)
        index = items.size - 1
        if (index < 0) {
            textLastItem.text = ""
            return
        } else textLastItem.text = items[index]
    }

    fun ValidateScan(ItemSerial: String) {

        try {

            textItemScanned.isEnabled = false
            btnNext.isEnabled = false
            btnPrev.isEnabled = false

            var itemCode = ItemSerial
            if (!itemCode.startsWith("IS"))
                itemCode = "IN$itemCode"
            //Log.i("Ah-Log","4")

            api = APIClient.getInstance(IPAddress, false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                    api.ValidateStockTakeItem(itemCode)
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
                                                showScanMessage(ex.response().errorBody()!!.string() + "", Color.RED)

                                            } else {
                                                showScanMessage(t?.message + " (API Error)", Color.RED)
                                            }
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

        } finally {


        }
    }


    private fun proceed() {

        try {
            btnDone.isEnabled = false
            btnDelete.isEnabled = false
            var UserID: Int = General.getGeneral(applicationContext).UserID
            var itemsStr: String = items.values.joinToString(",")
            Log.i("Ah-log", itemsStr);
            textItemScanned.isEnabled = false


            var modelItems: ArrayList<FillStockTakeDCModelItem> = arrayListOf()
            for (it in items)
                modelItems.add(FillStockTakeDCModelItem(it.value))

            var model =
                    FillStockTakeDCModel(textBoxNb.text.toString(), General.getGeneral(this).UserID, modelItems, PackingTypeId, locationId)


            api = APIClient.getInstance(IPAddress, false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                    api.FillBinStockTake1(model)
                            .subscribeOn(Schedulers.io())
                            // .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    { s ->
                                        var response = try {
                                            s.string()
                                        } catch (e: Exception) {
                                            e.message.toString()
                                        }
                                        if (response != null && (response.lowercase().startsWith("released") || response.lowercase().startsWith("success"))) {
                                            showMessage(response, Color.GREEN)
                                        } else {
                                            showMessage("" + response?.toString(), Color.RED)
                                        }
                                        runOnUiThread {
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
                                                showMessage(ex.response().errorBody()!!.string() + "", Color.RED)
                                            } else {
                                                if (t?.message != null)
                                                    showMessage(t.message.toString() + " (API Error )", Color.RED)
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
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300)
    }

    lateinit var api: BasicApi
    var compositeDisposable = CompositeDisposable()
    lateinit var mStorage: Storage;
    private var items: HashMap<Int, String> = HashMap();
    private var index: Int = -1
    var IPAddress = ""
    var updatingText = false;


    private fun showScanMessage(msg: String, color: Int) {
        Logger.Debug("StockTakeTest", "show Scan Message Call")
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


}