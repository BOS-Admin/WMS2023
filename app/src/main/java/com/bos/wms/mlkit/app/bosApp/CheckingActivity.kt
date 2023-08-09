package com.bos.wms.mlkit.app.bosApp

import Model.BosApp.Checking.CheckingDCModel
import Model.BosApp.Checking.CheckingDCModelItem
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
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_scan_container.*
import retrofit2.HttpException
import java.io.IOException


class CheckingActivity : AppCompatActivity() {

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
    private lateinit var general: General

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_packing_dc)
        general = General.getGeneral(applicationContext)
        textUser = findViewById(R.id.textUser)
        textBranch = findViewById(R.id.textBranch)
        val general = General.getGeneral(applicationContext)
        IPAddress = general.ipAddress
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
        mStorage = Storage(applicationContext)
        PackingTypeId = general.packReasonId
        textBoxNb.text = general.boxNb
        textBranch.isEnabled = false;
        textUser.isEnabled = false;
        textBoxNb.isEnabled = false;
        textItemScanned.requestFocus()
        textLastItem.isEnabled = false


        textItemScanned.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                Log.i("Ah-Log", textItemScanned.text.toString())
                if (updatingText)
                    return;
                updatingText = true;
                lblScanError.text = "";
                val item = textItemScanned.text.toString()
                if (item.length < 5) {
                    Beep()
                    lblScanError.text = "Invalid ItemSerial"
                    textItemScanned.setText("")
                    updatingText = false;
                    return;
                }

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

        items.remove(items.size - 1)
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
        if (barcode.length != 12 || !barcode.startsWith("22")) {
            return false
        }

        val checksumDigit = Character.getNumericValue(barcode[11])
        val barcodeWithoutChecksum = barcode.substring(0, 11)
        val expectedChecksum = calculateUPCAChecksum(barcodeWithoutChecksum)

        return checksumDigit == expectedChecksum
    }
//endregion


    fun ValidateScan(ItemSerial: String) {

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


            api = APIClient.getInstance(IPAddress, false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.ValidateFillBinCheckingItem(itemCode, general.mainLocation, general.UserID)
                    .subscribeOn(Schedulers.io())
                    // .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            var response = try {
                                s.string()
                            } catch (e: IOException) {
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
                                    Log.i("Checking", response)
                                }
                            } else {
                                showScanMessage(response, Color.RED)
                                runOnUiThread {
                                    updatingText = true
                                    textItemScanned.setText("")
                                    updatingText = false
                                    textItemScanned.isEnabled = true
                                    textItemScanned.requestFocus()


                                    Log.i("Checking", response)
                                }

                            }

                        },
                        { t: Throwable? ->
                            run {
                                runOnUiThread {
                                    btnNext.isEnabled = true
                                    btnPrev.isEnabled = true

                                }
                                Log.i("API Http Error ", "API Http Error " + t.toString())
                                if (t is HttpException) {
                                    var ex: HttpException = t as HttpException
                                    showScanMessage(
                                        ex.response().errorBody()!!.string() + " (API Http Error) ",
                                        Color.RED
                                    )

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
            lblScanError.text = ""
            var UserID: Int = General.getGeneral(applicationContext).UserID
            var itemsStr: String = items.values.joinToString(",")
            Log.i("Ah-log", itemsStr);
            textItemScanned.isEnabled = false

            var modelItems: ArrayList<CheckingDCModelItem> = arrayListOf()
            for (it in items)
                modelItems.add(CheckingDCModelItem(it.value))

            var model =
                CheckingDCModel(
                    textBoxNb.text.toString(),
                    general.UserID,
                    modelItems,
                    PackingTypeId,
                    locationId
                )

            api = APIClient.getInstance(IPAddress, false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.CheckingDC(model)
                    .subscribeOn(Schedulers.io())
                    // .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            var response = try {
                                s.string()
                            } catch (e: IOException) {
                                e.message.toString()
                            }
                            if (response != null && (response.lowercase()
                                    .startsWith("released") || response.lowercase()
                                    .startsWith("success") || response.lowercase()
                                    .startsWith("allowed"))
                            ) {
                                showMessage(response, Color.GREEN)
                            } else {
                                showMessage(response, Color.RED)

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
                                    showMessage(
                                        ex.response().errorBody()!!.string() + " (Http Error)",
                                        Color.RED
                                    )
                                } else {
                                    if (t?.message != null)
                                        showMessage(
                                            t.message.toString() + " (API Error)",
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

    lateinit var api: BasicApi
    var compositeDisposable = CompositeDisposable()
    lateinit var mStorage: Storage;
    private var items: HashMap<Int, String> = HashMap();
    private var index: Int = -1
    var IPAddress = ""
    var updatingText = false;


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