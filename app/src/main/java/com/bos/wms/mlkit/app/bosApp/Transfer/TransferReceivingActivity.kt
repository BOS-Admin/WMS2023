package com.bos.wms.mlkit.app.bosApp.Transfer


import Model.BosApp.BinModelItem1
import Model.BosApp.Transfer.BinModelItem
import Remote.APIClient
import Remote.BasicApi
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.R
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import java.io.IOException


class TransferReceivingActivity : AppCompatActivity() {



    private fun EndProcess() {

        try {
            btnDone.isEnabled = false
            btnEnd.isVisible = false
            textCount.isEnabled = false
            //Log.i("Ah-log", "" + count);
            api = APIClient.getInstance(IPAddress, false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.EndTransferReceivingProcess(
                    general.transferNavNo,
                    general.mainLocation
                )
                    .subscribeOn(Schedulers.io())
                    //  .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            var response = try {
                                s.string()
                            } catch (e: IOException) {
                                e.message.toString()
                            }
                            if (response != null && (response.lowercase()
                                    .startsWith("success") || response.lowercase()
                                    .startsWith("released"))
                            ) {
                                showMessage(response, Color.GREEN)
                            } else {
                                showMessage(""+response?.toString(),Color.RED)
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
                                            t.message.toString() + " (API Error )",
                                            Color.RED
                                        )
                                }

                                Beep()
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            lblError.setTextColor(Color.RED)
            lblError.text = e?.message
            Beep()
        } finally {
            textCount.isEnabled = false
        }
    }


    private fun proceedCount(count: Int) {

        val binBar=textBox.text.toString()
        updatingText=true
        textBox.setText("")
        updatingText=false
        textCount.setText("")

        try {
            btnDone.isEnabled = false
            textCount.isEnabled = false
            Log.i("Ah-log", "" + count);
            api = APIClient.getInstance(IPAddress, false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.CheckTransferBinCount(
                    general.UserID,
                    count,
                    binBar,
                    locationId,
                    general.transferNavNo
                )
                    .subscribeOn(Schedulers.io())
                    //  .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            var response = try {
                                s.string()
                            } catch (e: IOException) {
                                e.message.toString()
                            }
                            if (response != null && (response.lowercase()
                                    .startsWith("success") || response.lowercase()
                                    .startsWith("released"))
                            ) {
                                showScanMessage(response, Color.GREEN)
                            } else {
                                showScanMessage(response, Color.RED)
                            }
                            runOnUiThread{
                                GetBins()
                                btnDone.isEnabled = true
                                textCount.isEnabled = true
                            }


                        },
                        { t: Throwable? ->
                            run {
                                if (t is HttpException) {
                                    var ex: HttpException = t as HttpException
                                    showScanMessage(
                                        "Http Error: " + ex.response().errorBody()!!.string(),
                                        Color.RED
                                    )
                                } else {
                                    if (t?.message != null)
                                        showScanMessage("Error: " + t.message.toString(), Color.RED)
                                }

                                runOnUiThread{
                                    GetBins()
                                    btnDone.isEnabled = true
                                    textCount.isEnabled = true
                                }
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            showScanMessage("Error: " + e?.message, Color.RED)
            runOnUiThread{
                GetBins()
                btnDone.isEnabled = true
                textCount.isEnabled = true
            }
        } finally {

        }
    }


    fun GetBin(binBarcode: String) {
        try {
            var isCount = false
            lblScanError.text = "fetching data..."
            Log.i("TransferReceivingActivity", "transfer = $binBarcode")
            api = APIClient.getInstance(general.ipAddress, true).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.GetBinTransferInfo(binBarcode)
                    .subscribeOn(Schedulers.io())
                    //  .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            if (s != null) {
                                Log.i("AH-Log-Printer", "NextStatus " + s.transferNextStatus)
                                runOnUiThread{
                                    lblScanError.text = ""
                                }


                                if (s.transferStatus == 103) {
                                    showScanMessage("Box Already Released !!!", Color.RED)
                                } else if (s.transferNextStatus == null
                                    || s.transferNextStatus == ""
                                    || s.transferNextStatus.lowercase() == "count"
                                    || s.transferNextStatus.lowercase() == "recount"
                                ) {
                                    isCount = true
                                } else {
                                    general.boxNb=s.binBarcode
                                    general.saveGeneral(applicationContext)
                                    val intent = Intent(
                                        applicationContext,
                                        TransferReceivingDCActivity::class.java
                                    )
                                    startActivity(intent)
                                    finish()
                                    return@subscribe
                                }


                            } else {
                                showScanMessage("Bin Model is Empty", Color.RED)
                            }
                            runOnUiThread {
                                textBox.isEnabled = true
                                if(isCount){
                                    updatingText = false
                                    textCount.requestFocus()
                                }
                                else{
                                    updatingText = true
                                    textBox.setText("")
                                    updatingText = false
                                }

                            }

                        },
                        { t: Throwable? ->
                            run {

                                runOnUiThread{
                                    lblScanError.text = ""
                                }

                                if (t is HttpException) {
                                    var ex: HttpException = t as HttpException
                                    showScanMessage(
                                        ex.response().errorBody()!!
                                            .string() + " Error Fetching Bin (API Http Error)",
                                        Color.RED
                                    )
                                } else {
                                    if (t?.message != null)
                                        showScanMessage(
                                            t.message.toString() + " Error Fetching Bin (API Error)",
                                            Color.RED
                                        )
                                }

                                runOnUiThread {
                                    textBox.isEnabled = true
                                    updatingText = true
                                    textBox.setText("")
                                    updatingText = false
                                }


                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            showScanMessage(e?.message + " Error Fetching Bin (Error)", Color.RED)
            runOnUiThread {
                textBox.isEnabled = true
                updatingText = true
                textBox.setText("")
                updatingText = false
            }
        } finally {

        }
    }

    fun GetBins() {

        var msg="API Error"
        listBoxes.clear()
        renderedListBoxes.clear()
        renderedListBoxes.add("Fetching boxes, please wait...")
        arrayAdapter.notifyDataSetChanged()

        try {
            lblError.text = "fetching data..."
            Log.i("TransferReceivingActivity", "transfer = " + general.transferNavNo)
            api = APIClient.getInstance(General.getGeneral(applicationContext).ipAddress, true)
                .create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.GetTransferBins(general.transferNavNo)
                    .subscribeOn(Schedulers.io())
                    //  .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            if (s != null && s.isNotEmpty()) {
                                Log.i("AH-Log-X", "bins size " + s.size)
                                runOnUiThread {
                                    boxesModel=s
                                    updateUiWithBins(s)
                                    lblError.text = "boxes count= " + s.size
                                }


                            } else {
                                showMessage("List is Empty", Color.RED)
                                runOnUiThread{
                                    listBoxes.clear()
                                    renderedListBoxes.clear()
                                    renderedListBoxes.add("No boxes found")
                                    arrayAdapter.notifyDataSetChanged()
                                }

                            }

                        },
                        { t: Throwable? ->
                            run {
                                if (t is HttpException) {
                                    var ex: HttpException = t as HttpException
                                    msg=ex.response().errorBody()!!.string() + " Error Fetching Transfer Bins (API Http Error)"

                                } else {
                                    if (t?.message != null)
                                        msg=t.message.toString() + " Error Fetching Transfer Bins (API Error)"
                                }
                                showMessage(msg,Color.RED)
                                runOnUiThread{
                                    listBoxes.clear()
                                    renderedListBoxes.clear()
                                    renderedListBoxes.add(msg)
                                    arrayAdapter.notifyDataSetChanged()
                                }

                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            msg=e?.message + " Error Fetching Transfer Bins (Error)"
            showMessage(msg, Color.RED)
            runOnUiThread{
                listBoxes.clear()
                renderedListBoxes.clear()
                renderedListBoxes.add(msg)
                arrayAdapter.notifyDataSetChanged()
            }
        } finally {
        }
    }

    private fun updateUiWithBins(model: List<BinModelItem1>) {
        listBoxes.clear()
        renderedListBoxes.clear()
        unreleasedBins = arrayListOf();
        for (x in model) {
            if(x.TransferStatus!=103 && x.Received==0)
                unreleasedBins.add(x.BinBarcode)
            Log.i("Ah-Log-X", "BinId " + x.BinId)
        }
            for (x in model){
            listBoxes.add(x.BinBarcode)
            renderedListBoxes.add(renderBinInList(x))
        }
        arrayAdapter.notifyDataSetChanged()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_receiving)

        listBoxes = arrayListOf()

        renderedListBoxes = arrayListOf()
        listView = findViewById(R.id.listView)
        arrayAdapter = ArrayAdapter<String>(
            this@TransferReceivingActivity,
            R.layout.support_simple_spinner_dropdown_item,
            renderedListBoxes,
        )
        listView.adapter = arrayAdapter

        mStorage = Storage(applicationContext)
        textUser = findViewById(R.id.textUser)
        textBranch = findViewById(R.id.textBranch)
        textBox = findViewById(R.id.textBox)
        lblError = findViewById(R.id.lblError)
        lblScanError = findViewById(R.id.lblScanError)
        btnDone = findViewById(R.id.btnDone)
        btnEnd = findViewById(R.id.btnEnd)
        lblBox = findViewById(R.id.lblBox)
        lblCount = findViewById(R.id.lblCount)
        textCount = findViewById(R.id.textCount)

        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")
        general = General.getGeneral(applicationContext)
        var UserId = general.userFullName
        textBranch.text = general.fullLocation;
        textUser.text = general.userFullName;
        locationId = general.mainLocationID
        textBranch.isEnabled = false;
        textUser.isEnabled = false;
        textBox.requestFocus()

        GetBins()
        textBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                Log.i("Ah-Log", textBox.text.toString())
                if (updatingText)
                    return;
                updatingText = true;
                lblScanError.text = "";
                val item = textBox.text.toString()
                if (item.length < 5) {
                    Beep()
                    lblScanError.text = "Invalid ItemCode"
                    textBox.setText("")
                    updatingText = false;
                    return;
                }

                if(boxesModel==null){
                    showScanMessage("Box List is empty", Color.RED)
                    textBox.setText("")
                    updatingText = false;
                    return;
                }
                var b: BinModelItem1? =null
                for(x in boxesModel)
                    if(x.BinBarcode==item){
                        b=x
                        break
                }

                if (b==null) {
                    showScanMessage("$item  is not included in Transfer", Color.RED)
                    textBox.setText("")
                    updatingText = false;
                    return;
                }


                if (b.Received==1) {
                    showScanMessage("$item  is already received", Color.RED)
                    textBox.setText("")
                    updatingText = false;
                    return;
                }

                Log.i("Ah-Log", "3")
                textBox.isEnabled = false

                GetBin(item)

            }

            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int, count: Int
            ) {
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

        })





        btnDone.setOnClickListener {
            lblScanError.text = ""
            var count: Int = 0
            try {
                count = Integer.parseInt(textCount.text.toString())
                if (count <= 0) throw Exception()
                proceedCount(count)

            } catch (e: Exception) {
                lblScanError.text = "Invalid Count !!!!!"
                Beep()
                return@setOnClickListener
            }

        }

        btnEnd.setOnClickListener {
            lblScanError.text = ""
            if(unreleasedBins.size!=0){
                runOnUiThread {
                    AlertDialog.Builder(this)
                        .setTitle("Result")
                        .setMessage("The Following bins are not released.\n"+
                                unreleasedBins.joinToString("\n"))
                        .setPositiveButton("OK") { _, _ ->
                            EndProcess()
                        }
                        .setNegativeButton("Cancel") { _, _ ->
                            return@setNegativeButton
                        }
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setCancelable(false)
                        .show()

                }
            }
            else
                EndProcess()

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
    var IPAddress = ""


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


    private fun renderBinInList(bin: BinModelItem1): String {

        val x = "                                           ";
        val l = x.length
        var res = "(${statusToString(bin.TransferStatus)}) Next(${bin.TransferNextStatus})"
        val res1=if(bin.Received==0) res else "(Received)"
        return if(bin.BinBarcode!=null)
            bin.BinBarcode + x.substring(bin.BinBarcode.length) + res1
        else
            ""

    }

    private fun statusToString(s: Int): String {
        return when (s) {
            100 -> "Empty"
            101 -> "DC"
            102 -> "Count"
            103 -> "Released"
            104 -> "ReDC"
            105 -> "ReCount"
            106 -> "DC1"
            107 -> "DC2"
            else -> " <$s> Invalid Status"
        }

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




    private lateinit var textUser: TextView
    private lateinit var textBranch: TextView
    private lateinit var lblError: TextView
    private lateinit var lblScanError: TextView
    private lateinit var textBox: EditText
    private lateinit var lblBox: TextView
    private lateinit var lblCount: TextView
    private lateinit var textCount: EditText
    private lateinit var btnDone: Button
    private lateinit var btnEnd: Button
    private var locationId: Int = -1
    private lateinit var general: General
    private lateinit var listView: ListView
    private lateinit var arrayAdapter: ArrayAdapter<String>
    private lateinit var listBoxes: ArrayList<String>
    private lateinit var renderedListBoxes: ArrayList<String>
    var updatingText = false;
    var unreleasedBins = arrayListOf<String>()
    private lateinit var boxesModel:List<BinModelItem1>

}