package com.hc.mixthebluetooth.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hc.basiclibrary.ioc.ViewById;
import com.hc.basiclibrary.titleBasic.DefaultNavigationBar;
import com.hc.basiclibrary.viewBasic.BasActivity;
import com.hc.mixthebluetooth.Logger;
import com.hc.mixthebluetooth.R;
import com.hc.mixthebluetooth.Remote.Routes.BasicApi;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDDevice;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDDevicesManager;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDListener;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDOutput;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.Slid;
import com.hc.mixthebluetooth.ZebraPrinter;
import com.hc.mixthebluetooth.customView.PopWindowMain;
import com.hc.mixthebluetooth.customView.StepFailView;
import com.hc.mixthebluetooth.customView.StepSuccessView;
import com.hc.mixthebluetooth.storage.Storage;
import com.rfidread.Enumeration.eReadType;
import com.rfidread.Models.Tag_Model;
import com.util.StationSocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import Model.Message;
import Model.RfidStationModel;
import Remote.APIClient;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class LabelPrintFromRFID extends BasActivity implements RFIDListener {


    //region Attributes

    private int mStartDebug = 1;
    private DefaultNavigationBar mTitle;
    private String StationCode = "";
    private String RFID = "";
    private Storage mStorage = null;
    private List<BluetoothDevice> BluetoothDeviceList = new ArrayList();
    private List<String> BluetoothDevicelistStr = new ArrayList();
    private List<String> BluetoothDevicelistMac = new ArrayList();
    private CountDownTimer countDownTimer = null;
    private Object hmListLock = new Object();
    private List<String> TagList = new ArrayList<>();
    private HashMap<String, Integer> TagMap = new HashMap<String, Integer>();
    private String IPAddress = "";
    private int UserID = -1;
    private int rfidGrp1ReadTime1 = 500;
    private String PrinterName = "";
    private String ItemSerial = "";
    private int nbReads;
    private int nbTries = 0;

    private String LastDetectedRFID = "";
    int ColorGreen = Color.parseColor("#52ac24");
    int ColorRed = Color.parseColor("#ef2112");
    int ColorBlack = Color.parseColor("#000000");
    private boolean IsMac;
    private boolean IsReading = true;
    private ZebraPrinter printer;

    private TextView lblRfidDevice;
    private TextView txtScanState;
    private Button btnRFID;


    private LinearLayout stepsLayout;
    private ScrollView stepsScrollView;

    //endregion

    //region onCreate
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label_print_from_rfid);
        mStorage = new Storage(this.getApplicationContext());

        //establish Bluetooth connection
        try {
            initAll();
        } catch (Exception e) {
            Logger.Error("BLUETOOTH", "Error:" + e.getMessage());
        }

        setTitle();



        //time of Slid reading
        rfidGrp1ReadTime1 = Integer.parseInt(mStorage.getDataString("RFIDGrp1ReadTime1", "300"));
        StationCode = mStorage.getDataString("StationCode", "");
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
        UserID = mStorage.getDataInt("UserID");
        nbReads = Integer.parseInt(mStorage.getDataString("Reads", "1"));


        btnRFID = (Button) findViewById(R.id.btnRFID);
        lblRfidDevice = findViewById(R.id.lblRfidDevice);
        stepsLayout = findViewById(R.id.linearLayoutSteps);
        stepsScrollView = findViewById(R.id.stepsScrollView);
        txtScanState = findViewById(R.id.txtScanState);

        //start scanning listener
        setRfidButtonClickListener(btnRFID);

        //checking printer,  if Portable, Assigning Mac Address to Printer Name attribute
        CheckPrinterName();

    }
    //endregion

    /**
     * Listener for starting-stopping scanning process
     */
    private void setRfidButtonClickListener(Button button) {
        // Set up the listener
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String buttonText = btnRFID.getText().toString();
                if (buttonText.equals("Start")) {

                    btnRFID.setText("Stop");
                    stepsLayout.removeAllViews();

                    startRfidReading(rfidGrp1ReadTime1);

                } else if (buttonText.equals("Stop")) {
                    countDownTimer.cancel();
                    LastDetectedRFID = "";
                    stopRfidReading();
                    btnRFID.setText("Start");
                }
            }
        });
    }

    //region RFID STUFF

    /**
     * This method will start RFID scanning for a given time and handle Result by handleRfidScanResult()
     * when finished
     *
     * @param millis
     */
    public void startRfidReading(long millis) {


        TagMap.clear();
        TagList.clear();
        RFIDDevicesManager.readEPCSingleSlid(eReadType.Inventory);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(millis, 100) {
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                handleRfidScanResult();
            }
        }.start();
    }


    /**
     * This method handles the result of the scan and classify whether 0,1 or more RFIDs are
     * detected.
     * case of exactly ONE RFID  is accepted , else rejected
     */
    private void handleRfidScanResult() {
        new Thread(() -> {
            if (TagList.size() == 1) {
                RFID = TagList.get(0);
                runOnUiThread(() -> {
                    txtScanState.setTextColor(ColorGreen);
                });
                if (!RFID.equals(LastDetectedRFID)) {
                    LastDetectedRFID = RFID;
                    ProceedResult();
                } else {
                    addFailStep(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + " Same RFID read", "Scan Another Item");
                    restartCountDownTimer();
                }
            } else {

                if (TagList.size() == 0) {
                    runOnUiThread(() -> {
                        txtScanState.setTextColor(ColorRed);
                    });
                    restartCountDownTimer();
                } else if (TagList.size() > 1) {
                    runOnUiThread(() -> {
                                txtScanState.setTextColor(ColorRed);
                            }
                    );
                    addFailStep(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + " ERROR", "Multi-RFIDs have been read");
                    restartCountDownTimer();
                }
            }
        }).start();

    }

    /**
     * This method clear Map and List and restart the Timer
     */
    private void restartCountDownTimer() {
        TagMap.clear();
        TagList.clear();
        countDownTimer.start();
        runOnUiThread(() -> {
            txtScanState.setText("Scan State");
        });

    }


    /**
     * Stops Slid from Reading
     **/
    public void stopRfidReading() {
        txtScanState.setText("Scan Stopped!");
        txtScanState.setTextColor(ColorBlack);
        RFIDDevicesManager.stopSingleSlid();
    }


    /**
     * This method check ,depending on Printer type:[Portable - Stationary], The scanned RFID if it's valid to print information
     * else a Fail step will appear "Invalid"
     */
    private void ProceedResult() {

        if (IsMac) {
            //Printer is Portable
            //Get actual Zpl data
            GetZPLData(RFID);


        } else {
            //Printer is stationary, Print Information in backend
            PrintZplData(RFID);
            RFID = "";

            restartCountDownTimer();

        }


    }

    /**
     * Plays Error sound and pop up Alert Dialog For Error
     */
    private void showErrorMessage(String msg) {

        playError();
        playError();
        playError();
        playError();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(LabelPrintFromRFID.this)
                        .setTitle("Connection Error")
                        .setMessage(msg)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(getApplicationContext(), MainMenuActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setCancelable(false)
                        .show();
            }
        });
    }

    /**
     * Setting Top Title Bar of the activity including settings and configuration button
     */
    private void setTitle() {
        mTitle = new DefaultNavigationBar
                .Builder(this, (ViewGroup) findViewById(R.id.activity_label_print))
                .setLeftText("Label Print from RFID")
                .hideLeftIcon()
                .setRightIcon()
                .setLeftClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mStartDebug % 4 == 0) {
                            startActivity(DebugActivity.class);
                        }
                        mStartDebug++;
                    }
                })
                .setRightClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                            toast("This feature is not supported by the system, please upgrade the phone system", Toast.LENGTH_LONG);
                            return;
                        }
                        setPopWindow(v);
                        mTitle.updateRightImage(true);
                    }
                })
                .builer();
    }

    public void setPopWindow(View v) {
        PopWindowMain popWindowMain = new PopWindowMain(
                v, this, resetEngine -> {
            //弹出窗口销毁的回调
            mTitle.updateRightImage(false);
            if (resetEngine) { //更换搜索引擎，重新搜索
                // refresh()
            }
        });
    }

    /**
     * Show a success step in Scroll Layout
     */
    public void addSuccessStep(String title, String desc) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stepsLayout.addView(new StepSuccessView(LabelPrintFromRFID.this, title, desc));
                stepsScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
        playSuccess();
        playSuccess();
    }

    /**
     * Show a fail step in Scroll Layout
     */
    public void addFailStep(String title, String desc) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stepsLayout.addView(new StepFailView(LabelPrintFromRFID.this, title, desc));
                stepsScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
        playError();
    }

    /**
     * Listens for every Tag model got from scanning RFID process and add to the list new RFIDs
     * depending on nbReads, RFID is checked once or more before adding it to the List
     */
    @Override
    public void notifyListener(RFIDDevice device, Tag_Model tag_model) {
        try {
            Log.i("Ah-Log-XXX", "EPC " + tag_model._EPC);
            synchronized (hmListLock) {
                String rfid = tagModelKey(tag_model);
                if (rfid.isEmpty()) {
                    return;
                }

                if (!TagMap.containsKey(rfid)) {
                    TagMap.put(rfid, 1);
                    if (nbReads == 1) {
                        TagList.add(rfid);
                    }
                } else {
                    TagMap.put(rfid, TagMap.get(rfid) + 1);
                    if (TagMap.get(rfid) == nbReads) {
                        TagList.add(rfid);
                    }
                }
//                if (TagMap.size() > 1) {
//                    addFailStep(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + " ERROR", "Multi RFIDs Detcted");
//                }

                Logger.Error("Final Map size", "" + TagMap.size());
                for (HashMap.Entry<String, Integer> entry : TagMap.entrySet()) {
                    String key = entry.getKey();
                    Integer value = entry.getValue();
                    Logger.Error("Final Map contain", "RFID" + key + " count: " + value);
                }
                Logger.Error("Final List size", "" + TagList.size());
            }
        } catch (Exception ex) {
            Log.d("Debug", "Tags output exceptions:" + ex.getMessage());
        }
    }

    /**
     * Transforms from TagModel to String
     */
    public String tagModelKey(Tag_Model model) {
        if (model._EPC != null && model._TID != null && !model._EPC.isEmpty() && !model._TID.isEmpty()) {
            return model._EPC + "-" + model._TID;
        } else {
            return "";
        }
    }

    @Override
    public void notifyStartAntenna(int ant) {
    }

    @Override
    public void notifyStopAntenna(int ant) {
    }

    @Override
    public void notifyStartDevice(String message) {
    }

    @Override
    public void notifyEndDevice(String message) {
    }
    //endregion

    @Override
    protected void onDestroy() {
        RFIDDevicesManager.stopSingleSlid();
        RFIDDevicesManager.disconnectSingle();
        RFIDDevicesManager.setOutput(null);
        super.onDestroy();
    }

    //region Configuring Bluetooth Connection

    /**
     * Initiates bluetooth connection with slid
     */
    @Override
    public void initAll() {
        try {
            new Thread(() -> {

                String rfidMac = mStorage.getDataString("RFIDMac", "");
                String rfidName = GetBluetoothDeviceNameFromMac(rfidMac);
                Slid slid = new Slid();
                slid.setBluetoothAddress(rfidName);
                boolean rs = RFIDDevicesManager.connectSingleSlid(slid);
                if (rs) {
                    lblRfidDevice.setText("" + slid.getConnParam());
                } else {
                    showErrorMessage("No bluetooth connection");
                }
                RFIDDevicesManager.setOutput(new RFIDOutput(this));
            }).start();
        } catch (Exception e) {
            lblRfidDevice.setText("" + e.getMessage());
        }
    }

    /**
     * This method will get Bluetooth Device from Mac Address
     */
    @SuppressLint("MissingPermission")
    private String GetBluetoothDeviceNameFromMac(String Mac) {
        if (!(Mac != null && !Mac.isEmpty())) {
            return "";
        }
        GetBT4DeviceStrList();
        for (BluetoothDevice d : BluetoothDeviceList) {
            if (d.getAddress() != null && d.getAddress().contains(Mac))
                return d.getName();
        }
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(Mac);
        if (device == null)
            return "";
        return device.getName();
    }

    @SuppressLint("MissingPermission")
    public List<String> GetBT4DeviceStrList() {

        BluetoothDeviceList.clear();
        BluetoothDevicelistStr.clear();
        BluetoothDevicelistMac.clear();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            Iterator var1 = pairedDevices.iterator();

            while (var1.hasNext()) {
                BluetoothDevice device = (BluetoothDevice) var1.next();
                try {
                    BluetoothDeviceList.add(device);
                    BluetoothDevicelistStr.add(device.getName());
                    BluetoothDevicelistMac.add(device.getAddress());
                } catch (Exception var4) {
                }
            }
        }

        return BluetoothDevicelistStr;
    }

    //endregion

    //region API STUFF

    /**
     * API that check using Station Code, the  printer if it is portable or Stationary , and assigning Mac address to Printer Name attribute
     */
    public void CheckPrinterName() {
        printer = new ZebraPrinter(PrinterName);
        printer.setOnPrinterConnectionFailListener(success -> {
            addFailStep(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + " ERROR", "Printer Connection Failed");

        });
        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.CheckPrinterName(StationCode)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {
                                    //PrinterName could be MacAddress or Actual Printer Name
                                    PrinterName = s.getPrinterName();
                                    IsMac = s.isMac();
                                    if(IsMac){
                                        printer = new ZebraPrinter(PrinterName);
                                        printer.setOnPrinterConnectionFailListener(success -> {
                                            addFailStep(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + " ERROR", "Printer Connection Failed");
                                        });
                                    }

                                }
                            }, (throwable) -> {
                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    String response = ex.response().errorBody().string();
                                    if (response.isEmpty()) {
                                        response = throwable.getMessage();
                                    }
                                    Logger.Debug("API", "CheckPrinterName - Error In HTTP Response: " + response);
                                    addFailStep(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + " ERROR", response);
                                //    Toast.makeText(this, response, Toast.LENGTH_LONG).show();
                                } else {
                                    Logger.Error("API", "CheckPrinterName - Error In Response: " + throwable.getMessage());
                                }
                            }));
        } catch (Throwable e) {
            Logger.Error("API", "CheckPrinterName - Error Connecting: " + e.getMessage());
        }
    }

    /**
     * This API print the detected item's information via Stationary Printer
     */
    public void PrintZplData(String RFID) {
        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.PrintZplData(RFID, StationCode, UserID)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {
                                    JsonObject json = new JsonParser().parse(s.string()).getAsJsonObject();
                                    String msg = json.get("message").getAsString();
                                    ItemSerial = json.get("itemSerial").getAsString();
                                    //should get "Success" in msg
                                    addSuccessStep(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + " " + msg + "", "Label Printed for IS: " + ItemSerial);
                                    Logger.Error("Printed", "RFID: " + RFID + " IS : " + ItemSerial);

                                }
                            }, (throwable) -> {
                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    String response = ex.response().errorBody().string();
                                    if (response.isEmpty()) {
                                        response = throwable.getMessage();
                                    }
                                    Logger.Debug("API", "CheckPrinterName - Error In HTTP Response: " + response);

                                    addFailStep(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) +" Failed!", response);
                                } else {
                                    Logger.Error("API", "CheckPrinterName - Error In Response: " + throwable.getMessage());
                                }
                            }));
        } catch (Throwable e) {
            Logger.Error("API", "CheckPrinterName - Error Connecting: " + e.getMessage());
        }
    }


    /**
     * API that get the detected by RFID , item's ZPL Data , pass it to portable printer and print
     */
    public void GetZPLData(String RFID) {
        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.GetZPLData(RFID, StationCode, UserID)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {
                                    JsonObject json = new JsonParser().parse(s.string()).getAsJsonObject();
                                    String messsage = json.get("message").getAsString();
                                    String zpl = json.get("zpl").getAsString();
                                    ItemSerial = json.get("itemSerial").getAsString();

                                    if (zpl != null) {
                                        runOnUiThread(() -> {
                                            printer.PortablePrintData(zpl);
                                        });

                                        addSuccessStep(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + " "+messsage, "Label Printed for IS: " + ItemSerial);
                                        Logger.Debug("Printed", "RFID: " + this.RFID + " IS : " + ItemSerial);
                                    }else{
                                        addFailStep(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + " ERROR", "Error in ZPL");
                                    }
                                    restartCountDownTimer();
                                    this.RFID = "";

                                }
                            }, (throwable) -> {
                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    String response = ex.response().errorBody().string();
                                    if (response.isEmpty()) {
                                        response = throwable.getMessage();
                                    }
                                    Logger.Debug("API", "CheckPrinterName - Error In HTTP Response: " + response);
                                    addFailStep(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + "ERROR", "Error in ZPL");
                                //    Toast.makeText(this, response, Toast.LENGTH_LONG).show();
                                } else {
                                    Logger.Error("API", "CheckPrinterName - Error In Response: " + throwable.getMessage());
                                }
                                restartCountDownTimer();
                            }));
        } catch (Throwable e) {
            Logger.Error("API", "CheckPrinterName - Error Connecting: " + e.getMessage());
        }
    }


    //endregion
}