package com.hc.mixthebluetooth.activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.DisplayOrientedMeteringPointFactory;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.example.VolleyMultipartRequest;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.hc.mixthebluetooth.Extensions;
import com.hc.mixthebluetooth.Logger;
import com.hc.mixthebluetooth.Model.RepairTypeModel;
import com.hc.mixthebluetooth.R;
import com.hc.mixthebluetooth.storage.Storage;
import com.hopeland.androidpc.example.PublicData;
import com.rfidread.Connect.BaseConnect;
import com.rfidread.Enumeration.eReadType;
import com.rfidread.Helper.Helper_ThreadPool;
import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.Models.Tag_Model;
import com.rfidread.RFIDReader;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Remote.APIClient;
import com.hc.mixthebluetooth.Remote.Routes.BasicApi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class RepairCategorizationActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1000;

    List<Button> repairTypesButtons;

    Button usedButton, donationButton, totalLossButton, repairedButton;

    ImageView btnStackedItems;

    ProgressDialog mainProgressDialog;

    private PreviewView cameraPreview;
    private ImageView cameraPreviewImageView;
    private ImageButton captureImageButton;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    public TextView repairHelpText, chooseRepairText, scanItemsHelpText;

    Button scannedItemsCount;
    int totalScannedItems = 0;

    LinearLayout helpLinearLayout;

    public List<RepairTypeModel> allRepairTypes = new ArrayList<>();

    public RepairTypeModel CurrentSelectedRepairModel = null;

    public String LotBondStation = "";

    String IPAddress = "";

    public int CurrentTakenPictures = 0;

    CircularProgressIndicator loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repair_categorization);

        repairTypesButtons = new ArrayList<>();

        repairTypesButtons.add(usedButton = findViewById(R.id.usedButton));
        repairTypesButtons.add(donationButton = findViewById(R.id.donationButton));
        repairTypesButtons.add(totalLossButton = findViewById(R.id.totalLossButton));
        repairTypesButtons.add(repairedButton = findViewById(R.id.repairedButton));

        btnStackedItems = findViewById(R.id.btnStackedItems);

        repairHelpText = findViewById(R.id.repairHelpText);

        helpLinearLayout = findViewById(R.id.centerLayout);
        scannedItemsCount = findViewById(R.id.scannedItemsCount);
        chooseRepairText = findViewById(R.id.chooseRepairText);
        scanItemsHelpText = findViewById(R.id.scanItemsHelpText);

        captureImageButton = findViewById(R.id.captureImageButton);
        captureImageButton.setEnabled(false);

        cameraPreviewImageView = findViewById(R.id.cameraPreviewImageView);
        cameraPreviewImageView.setVisibility(View.INVISIBLE);

        cameraPreview = findViewById(R.id.cameraPreview);

        cameraExecutor = Executors.newSingleThreadExecutor();

        captureImageButton.setOnClickListener((click) -> {
            //pickImage();
            captureImage();
        });

        loadingBar = findViewById(R.id.loadingBar);

        Storage mStorage = new Storage(this.getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");

        LotBondStation = mStorage.getDataString("LotBondStation", "");

        Logger.Debug("StartRepairCategorizationMenu", "Setting Up Menu With CurrentLocation: " + TransferToRepairActivity.CurrentLocation + " RepairLocation: " + TransferToRepairActivity.CurrentRepairLocationCode);
        if(!TransferToRepairActivity.CurrentLocation.isEmpty() && !TransferToRepairActivity.CurrentRepairLocationCode.isEmpty()){

            if(!TransferToRepairActivity.CurrentLocation.equalsIgnoreCase(TransferToRepairActivity.CurrentRepairLocationCode)){
                Logger.Error("StartRepairCategorizationMenu", "StartFromLocation - Couldn't Use Location: " + TransferToRepairActivity.CurrentLocation + " RepairLocation: " + TransferToRepairActivity.CurrentRepairLocationCode);
                OnErrorReceived("Error", "You must be in the repair area to use this menu");
            }else {
                GetRepairTypes();
                AttemptRFIDReaderConnection();

                mainProgressDialog = ProgressDialog.show(this, "",
                        "Connecting To Sled, Please wait...", true);
                mainProgressDialog.show();
            }

        }else {
            Logger.Error("StartRepairCategorizationMenu", "StartFromLocation - Couldn't Use Location: " + TransferToRepairActivity.CurrentLocation + " RepairLocation: " + TransferToRepairActivity.CurrentRepairLocationCode);
            OnErrorReceived("Error", "Couldn't Determine your location");
        }


        btnStackedItems.setOnClickListener(view -> {
            Extensions.setImageDrawableWithAnimation(btnStackedItems, getDrawable(R.drawable.baseline_stacked_repair_type_icon), 200);
            new CountDownTimer(400, 400) {

                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    ResetRepairTypes();
                }
            }.start();
        });

        for(Button btn : repairTypesButtons){
            btn.setOnClickListener(view -> {
                SelectRepairType(btn);
            });
        }

        HideMenuItems();

    }

    /**
     * This function hides all the menu
     */
    public void HideMenuItems(){
        for(Button btn : repairTypesButtons){
            btn.setVisibility(View.GONE);
        }
        btnStackedItems.setVisibility(View.GONE);

        repairHelpText.setVisibility(View.GONE);
        captureImageButton.setVisibility(View.GONE);
        cameraPreviewImageView.setVisibility(View.GONE);
        cameraPreview.setVisibility(View.GONE);

        helpLinearLayout.setVisibility(View.GONE);

    }

    /**
     * This function resets all the menu items
     */
    public void ResetMenuItems(){
        for(Button btn : repairTypesButtons){
            btn.setVisibility(View.VISIBLE);
        }
        btnStackedItems.setVisibility(View.GONE);

        repairHelpText.setVisibility(View.GONE);
        captureImageButton.setVisibility(View.GONE);
        cameraPreviewImageView.setVisibility(View.GONE);
        cameraPreview.setVisibility(View.GONE);

        helpLinearLayout.setVisibility(View.VISIBLE);
        scannedItemsCount.setVisibility(View.GONE);
        chooseRepairText.setVisibility(View.VISIBLE);
        scanItemsHelpText.setVisibility(View.GONE);
        loadingBar.setVisibility(View.GONE);

    }

    /**
     * This function resets the current RFID reading and current repair output type
     */
    public void ResetRepairTypes(){

        for(Button btn : repairTypesButtons){
            btn.setVisibility(View.VISIBLE);
            btn.setClickable(true);
        }
        btnStackedItems.setVisibility(View.GONE);

        repairHelpText.setVisibility(View.GONE);
        captureImageButton.setVisibility(View.GONE);
        cameraPreviewImageView.setVisibility(View.GONE);
        cameraPreview.setVisibility(View.GONE);

        helpLinearLayout.setVisibility(View.VISIBLE);
        scannedItemsCount.setVisibility(View.GONE);
        chooseRepairText.setVisibility(View.VISIBLE);
        scanItemsHelpText.setVisibility(View.GONE);

        loadingBar.setVisibility(View.GONE);

        CurrentSelectedRepairModel = null;
        lastReadRFID = "";
        totalScannedItems = 0;
        CurrentTakenPictures = 0;

        scannedItemsCount.setText("0 Items Scanned");

        RFIDStopRead();
    }

    /**
     * This function resets the current RFID reading and current repair output type
     */
    public void PauseMenuItems(boolean pause){

        for(Button btn : repairTypesButtons){
            btn.setClickable(!pause);
        }
        btnStackedItems.setClickable(!pause);
    }

    /**
     * This function selects a specific button as the current repair output type and starts the rfid reading
     * @param repairTypeButton
     */
    public void SelectRepairType(Button repairTypeButton){
        if(allRepairTypes.size() > 0) {
            for(Button btn : repairTypesButtons){
                if(btn != repairTypeButton){
                    btn.setVisibility(View.GONE);
                }else {
                    btn.setClickable(false);
                    String currentRepairType = "";
                    if(btn == usedButton){//Used
                        currentRepairType = "Used";
                    }else if(btn == donationButton){//Donation
                        currentRepairType = "Donation";
                    }else if(btn == totalLossButton){//Total Loss
                        currentRepairType = "TotalLoss";
                    }else if(btn == repairedButton){//Repaired
                        currentRepairType = "Repaired";
                    }

                    String finalCurrentRepairType = currentRepairType;
                    Optional<RepairTypeModel> model = allRepairTypes.stream().filter(repairTypeModel -> repairTypeModel.getName().equalsIgnoreCase(finalCurrentRepairType)).findFirst();
                    if(model.get() != null){
                        CurrentSelectedRepairModel = model.get();
                        if(model.get().getMinRequiredImages() > 0){
                            repairHelpText.setText("Please Scan An Item To Begin");
                            repairHelpText.setVisibility(View.VISIBLE);
                            captureImageButton.setVisibility(View.VISIBLE);
                            captureImageButton.setEnabled(false);
                            cameraPreviewImageView.setVisibility(View.INVISIBLE);
                            cameraPreview.setVisibility(View.VISIBLE);
                            helpLinearLayout.setVisibility(View.GONE);
                            loadingBar.setVisibility(View.GONE);
                        }else {
                            helpLinearLayout.setVisibility(View.VISIBLE);
                            scannedItemsCount.setVisibility(View.VISIBLE);
                            chooseRepairText.setVisibility(View.GONE);
                            scanItemsHelpText.setVisibility(View.VISIBLE);
                            loadingBar.setVisibility(View.GONE);
                        }
                    }else {
                        CurrentSelectedRepairModel = null;
                    }
                }
            }
            btnStackedItems.setVisibility(View.VISIBLE);
            Extensions.setImageDrawableWithAnimation(btnStackedItems, getDrawable(R.drawable.baseline_stacked_repair_type_icon), 200);
            RFIDStartRead();

        }

    }

    /**
     * This gets all the repair types and their details from the backend
     */
    public void GetRepairTypes(){
        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.GetRepairTypes()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    allRepairTypes.addAll(s);
                                }
                            }, (throwable) -> {
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    String response = ex.response().errorBody().string();
                                    if(response.isEmpty()){
                                        response = throwable.getMessage();
                                    }
                                    Logger.Debug("API", "GetRepairTypes - Error In HTTP Response: " + response);
                                }else {
                                    Logger.Error("API", "GetRepairTypes - Error In Response: " + throwable.getMessage());
                                }
                            }));
        } catch (Throwable e) {
            Logger.Error("API", "GetRepairTypes - Error Connecting: " + e.getMessage());
        }
    }


    /**
     * This function is called when a rfid sled is connected successfully
     */
    public void OnRFIDReaderConnected(){
        ResetMenuItems();

        if(cameraPermissionsGranted()){
            mainProgressDialog.setMessage("Starting The Camera, Please Wait...");
            startCamera(true);
        }else {
            requestCameraPermissions();
        }

    }

    /**
     * This function is called when the rfid sled connection fails
     */
    public void OnRFIDReaderError(String error){
        mainProgressDialog.cancel();
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Connection Error")
                .setMessage(error)
                .setNegativeButton("Close", (dialogInterface, i) ->
                {
                    finish();
                })
                .show();
    }

    /**
     * This function is called when any error occurs
     */
    public void OnErrorReceived(String title, String error){
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(error)
                .setNegativeButton("Close", (dialogInterface, i) ->
                {
                    finish();
                })
                .show();
    }

    /**
     * This function is called when any error occurs
     */
    public void OnItemError(String rfid, String error){
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Item Error")
                .setMessage(error + "\n" + "RFID: " + rfid)
                .setNegativeButton("Close", (dialogInterface, i) ->
                {

                })
                .show();
    }

    /**
     * Bluetooth rfid reader info
     */

    String RFIDMac = "";

    List<BluetoothDevice> BluetoothDeviceList = new ArrayList();
    List<String> BluetoothDevicelistStr = new ArrayList();
    List<String> BluetoothDevicelistMac = new ArrayList();

    Boolean IsRFIDConnected = false;

    String RFIDName = "";

    String lastReadRFID = "";

    @Override
    protected void onDestroy() {
        RFIDStopRead();
        cameraExecutor.shutdown();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        RFIDStopRead();
        super.onPause();
    }

    /**
     * This function returns all the Bluetooth paired devices
     * @return
     */
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

    /**
     * Gets the bluetooth device name from its mac address
     * @param Mac
     * @return
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
            //something here
        }
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(Mac);
        if (device == null)
            return "";
        return device.getName();
    }

    /**
     * This function attempts the rfid reader connection with the corresponding mac address
     */
    public void AttemptRFIDReaderConnection() {
        try {
            GetBT4DeviceStrList();
            Storage mStorage = new Storage(this);//sp存储
            RFIDMac = mStorage.getDataString("RFIDMac", "00:15:83:3A:4A:26");
            RFIDName = GetBluetoothDeviceNameFromMac(RFIDMac);
            new CountDownTimer(200, 200) {
                public void onTick(long l) {

                }

                public void onFinish() {
                    ConnectToRFIDReader();
                }
            }.start();
        } catch (Exception ex) {
            Logger.Error("RFID-READER", "AttemptRFIDReaderConnection - " + ex.getMessage());
            OnRFIDReaderError(ex.getMessage());
        }

    }



    /**
     * This function initializes the connection to the rfid reader
     */
    private void ConnectToRFIDReader() {
        try {
            RFIDReader.CloseAllConnect();
            RFIDReader.GetBT4DeviceStrList();
            if (RFIDName != null && !RFIDName.isEmpty()) {
                if (!RFIDReader.CreateBT4Conn(RFIDName, new IAsynchronousMessage() {
                    @Override
                    public void WriteDebugMsg(String s) {

                    }

                    @Override
                    public void WriteLog(String s) {

                    }

                    @Override
                    public void PortConnecting(String s) {

                    }

                    @Override
                    public void PortClosing(String s) {

                    }

                    @Override
                    public void OutPutTags(Tag_Model model) {
                        if(CurrentSelectedRepairModel != null){
                            String key = TagModelKey(model);
                            if(!key.isEmpty()){

                                if(CurrentSelectedRepairModel.getMinRequiredImages() > 0){
                                    Logger.Debug("RFID-READER", "OutPutTags - Detected Tag: " + key);
                                    if(!lastReadRFID.equals(key)) {
                                        CurrentTakenPictures = 0;
                                        lastReadRFID = key;
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                captureImageButton.setEnabled(true);
                                                loadingBar.setVisibility(View.GONE);
                                                repairHelpText.setText("Please Take " + CurrentSelectedRepairModel.getMinRequiredImages() + " Picture(s) Of The Item");
                                            }
                                        });
                                    }
                                }else {
                                    Logger.Debug("RFID-READER", "OutPutTags - Detected Tag: " + key);
                                    lastReadRFID = key;
                                    UpdateOutputType(lastReadRFID);
                                }
                            }
                        }
                    }

                    @Override
                    public void OutPutTagsOver() {

                    }

                    @Override
                    public void GPIControlMsg(int i, int i1, int i2) {

                    }

                    @Override
                    public void OutPutScanData(byte[] bytes) {

                    }
                })) {
                    Logger.Error("RFID-READER", "ConnectToRFIDReader - Error Connecting to " + RFIDMac);
                } else {
                    Logger.Debug("RFID-READER", "ConnectToRFIDReader - RFID-" + RFIDMac + " Connected Successfully");
                    OnRFIDReaderConnected();
                    RFIDReader._Config.SetReaderAutoSleepParam(RFIDName, false, "");
                }
            }
            int RFIDCount = RFIDReader.HP_CONNECT.size();
            HashMap<String, BaseConnect> lst = RFIDReader.HP_CONNECT;
            if (lst.keySet().stream().count() > 0) {
                IsRFIDConnected = true;
            } else {
                OnRFIDReaderError("Couldn't Connect To Sled");
                Logger.Error("RFID-READER", "ConnectToRFIDReader - Couldn't Connect To Sled");
            }
        } catch (Exception ex) {
            Logger.Error("RFID-READER", "ConnectToRFIDReader - " + ex.getMessage());
            OnRFIDReaderError(ex.getMessage());
        }
    }

    /**
     * This function returns the rfid key of a tag
     * @param model
     * @return
     */
    public String TagModelKey(Tag_Model model) {
        if (model._EPC != null && model._TID != null && !model._EPC.isEmpty() && !model._TID.isEmpty())
            return model._EPC + "-" + model._TID;
        return "";
    }

    /**
     * This function configures the RFID reader to read EPC 6C/6B Tags
     */
    public void RFIDStartRead() {


        Helper_ThreadPool.ThreadPool_StartSingle(new Runnable() {
            @Override
            public void run() {
                try {
                    lastReadRFID = "";
                    if (PublicData._IsCommand6Cor6B.equals("6C")) {// read 6c tags
                        GetEPC_6C();
                    } else {// read 6b tags
                        GetEPC_6B();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * This function stop the configured rfid readings
     */
    public void RFIDStopRead() {
        Helper_ThreadPool.ThreadPool_StartSingle(new Runnable() {
            @Override
            public void run() {
                try {
                    RFIDReader._Config.Stop(RFIDName);
                } catch (Exception e) {
                    Logger.Error("RFID-READER", "RFIDStopRead - Error While Stopping The Read Command: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });

    }

    public void UpdateOutputType(String rfid){
        if(CurrentSelectedRepairModel == null){
            Logger.Error("UpdateOutputType", "Attempting to update the output type of " + rfid + " where the selected repair type is null");
            return;
        }
        try {

            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.UpdateRepairOutputType(rfid, CurrentSelectedRepairModel.getId())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    totalScannedItems++;
                                    if(CurrentSelectedRepairModel.getMinRequiredImages() == 0) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ObjectAnimator animate = ObjectAnimator.ofPropertyValuesHolder(scannedItemsCount,
                                                        PropertyValuesHolder.ofFloat("scaleX", 1.1f),
                                                        PropertyValuesHolder.ofFloat("scaleY", 1.1f));
                                                animate.setDuration(200);
                                                animate.setRepeatCount(ObjectAnimator.RESTART);
                                                animate.setRepeatMode(ObjectAnimator.REVERSE);
                                                animate.addListener(new Animator.AnimatorListener() {
                                                    @Override
                                                    public void onAnimationStart(@NonNull Animator animator) {

                                                    }

                                                    @Override
                                                    public void onAnimationEnd(@NonNull Animator animator) {
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                scannedItemsCount.setText(totalScannedItems + " Items Scanned");
                                                            }
                                                        });
                                                    }

                                                    @Override
                                                    public void onAnimationCancel(@NonNull Animator animator) {

                                                    }

                                                    @Override
                                                    public void onAnimationRepeat(@NonNull Animator animator) {

                                                    }
                                                });
                                                animate.start();
                                            }
                                        });
                                    }else {
                                        loadingBar.setVisibility(View.GONE);
                                        captureImageButton.setVisibility(View.VISIBLE);
                                        captureImageButton.setEnabled(false);
                                        PauseMenuItems(false);
                                        cameraPreviewImageView.setVisibility(View.INVISIBLE);
                                        repairHelpText.setText("Please Scan An Item To Start");
                                    }
                                    Logger.Debug("UpdateOutputType", "Updated output type of " + rfid + " to " + CurrentSelectedRepairModel.getName());
                                }
                            }, (throwable) -> {
                                if(CurrentSelectedRepairModel.getMinRequiredImages() > 0){
                                    loadingBar.setVisibility(View.GONE);
                                    captureImageButton.setVisibility(View.VISIBLE);
                                    captureImageButton.setEnabled(false);
                                    PauseMenuItems(false);
                                    cameraPreviewImageView.setVisibility(View.INVISIBLE);
                                    repairHelpText.setText("Please Scan An Item To Start");
                                }
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    String response = ex.response().errorBody().string();
                                    if(response.isEmpty()){
                                        response = throwable.getMessage();
                                    }
                                    Logger.Debug("API", "GetRepairTypes - Error In HTTP Response: " + response);
                                    OnItemError(rfid, response);
                                }else {
                                    OnItemError(rfid, throwable.getMessage());
                                    Logger.Error("API", "GetRepairTypes - Error In Response: " + throwable.getMessage());
                                }
                            }));
        } catch (Throwable e) {
            if(CurrentSelectedRepairModel.getMinRequiredImages() > 0){
                loadingBar.setVisibility(View.GONE);
                captureImageButton.setVisibility(View.VISIBLE);
                captureImageButton.setEnabled(false);
                PauseMenuItems(false);
                cameraPreviewImageView.setVisibility(View.INVISIBLE);
                repairHelpText.setText("Please Scan An Item To Start");
            }
            Logger.Error("API", "GetRepairTypes - Error Connecting: " + e.getMessage());
        }
    }

    private int GetEPC_6C() {
        String RFID = RFIDName;
        int ret = -1;
        if (!RFIDReader.HP_CONNECT.containsKey(RFID))
            return ret;
        ret = RFIDReader._Tag6C.GetEPC_TID(RFID, 1, eReadType.Inventory);
        return ret;
    }

    private int GetEPC_6B() {
        String RFID = RFIDName;
        int ret = -1;
        if (!RFIDReader.HP_CONNECT.containsKey(RFID))
            return ret;
        ret = RFIDReader._Tag6B.Get6B(RFID, 1, eReadType.Inventory.GetNum(), 0);
        return ret;
    }

    /** Camera Functions **/

    /**
     * Start the back camera, and build the preview for the user to see
     */
    public void startCamera(boolean closeDialog){
        captureImageButton.setEnabled(true);
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                //Create Preview And Bind It
                Preview preview = new Preview.Builder()
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                imageCapture =
                        new ImageCapture.Builder()
                                .build();

                Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageCapture, preview);

                if(closeDialog){
                    mainProgressDialog.cancel();
                }

                /**
                 * This code helps the camera focus on click
                 */
                cameraPreview.setOnTouchListener(new View.OnTouchListener() {
                    @RequiresApi(api = Build.VERSION_CODES.R)
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                        {
                            try {
                                DisplayOrientedMeteringPointFactory factory = new DisplayOrientedMeteringPointFactory(getDisplay(), camera.getCameraInfo(), (float)cameraPreviewImageView.getWidth(), (float)cameraPreviewImageView.getHeight());
                                MeteringPoint autoFocusPoint = factory.createPoint(motionEvent.getX(), motionEvent.getY());

                                camera.getCameraControl().startFocusAndMetering(
                                        new FocusMeteringAction.Builder(
                                                autoFocusPoint,
                                                FocusMeteringAction.FLAG_AF
                                        ).disableAutoCancel().build());
                            } catch (Exception e) {
                                Logger.Error("Camera", "cameraFocus - " + e.getMessage());
                            }
                        }
                        return true;
                    }
                });

            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));

    }

    /**
     * Capture an image and upload it to the cloud
     */
    public void captureImage() {

        captureImageButton.setEnabled(false);

        File FolerPath = new File(
                getOutputDirectory(),
                "/Images/"
        );

        FolerPath.mkdirs();

        String fileNameFormatStr = new SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS", Locale.US
        ).format(System.currentTimeMillis()) + ".jpg";

        File photoFile = new File(
                FolerPath,
                fileNameFormatStr
        );

        cameraPreviewImageView.setImageBitmap(cameraPreview.getBitmap());
        cameraPreviewImageView.setVisibility(View.VISIBLE);

        loadingBar.setVisibility(View.VISIBLE);
        captureImageButton.setVisibility(View.GONE);
        PauseMenuItems(true);

        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(outputFileOptions, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                        Bitmap image = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                        if(CurrentSelectedRepairModel != null){
                            uploadImage(lastReadRFID, image, photoFile, 0);
                        }
                    }
                    @Override
                    public void onError(ImageCaptureException error) {
                        cameraPreviewImageView.setVisibility(View.INVISIBLE);
                        captureImageButton.setEnabled(true);
                        loadingBar.setVisibility(View.GONE);
                        PauseMenuItems(false);
                        captureImageButton.setVisibility(View.VISIBLE);
                        Snackbar.make(findViewById(R.id.repairCategorizationActivityLayout), "Failed Capturing Image", Snackbar.LENGTH_LONG)
                                .setAction("No action", null).show();
                        Logger.Error("CAMERA", "CaptureImage - Image Failed To Saved: " + error.getMessage());
                    }
                }
        );

    }

    /**
     * Check if the camera permissions are granted for the camera preview and capture
     * @return
     */
    public boolean cameraPermissionsGranted(){
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request the camera permissions from the user
     */
    public void requestCameraPermissions(){
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Logger.Debug("CAMERA", "OnRequestPermissionsResult - Camera Started, User Granted Permissions");
                startCamera(false);
            } else {
                Toast.makeText(this, "Can't use camera scan", Toast.LENGTH_LONG).show();
                Logger.Error("CAMERA", "OnRequestPermissionsResult - User Failed To Grant Camera Permissions");
            }
        }
    }

    /**
     * This is used to get the output directory of the application were we can save a temp image of the camera capture
     * @return File path
     */
    private File getOutputDirectory() {
        File[] allMediaDirs = getExternalMediaDirs();
        File mediaDir = allMediaDirs.length > 0 ? allMediaDirs[0] : null;
        if(mediaDir == null) {
            new File(getResources().getString(R.string.app_name)).mkdirs();
        }

        return (mediaDir != null && mediaDir.exists()) ? mediaDir : getFilesDir();
    }

    public void uploadImage(String rfid, Bitmap bitmap, File fileName, int trails){
        if(trails == 0){
            Logger.Debug("IMAGE", "uploadImage - Starting Upload For: " + fileName.getAbsolutePath());
        }
        if(trails>3){
            Logger.Error("IMAGE", "uploadImage - Trails reached 3");
            loadingBar.setVisibility(View.GONE);
            captureImageButton.setVisibility(View.VISIBLE);
            captureImageButton.setEnabled(true);
            PauseMenuItems(false);
            return;
        }

        String ipAddress = "http://" + IPAddress + (IPAddress.endsWith("/") ? "" : "/");

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, ipAddress + "api/Repair/UploadImage", new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(response.toString().contains("Success")){
                    CurrentTakenPictures++;
                    Logger.Debug("IMAGE", "uploadImage - Image Saved And Uploaded");
                    if(CurrentTakenPictures == CurrentSelectedRepairModel.getMinRequiredImages()){
                        Logger.Debug("Upload", "Change output type of " + lastReadRFID + " To " + CurrentSelectedRepairModel.getName());
                        UpdateOutputType(lastReadRFID);
                    }else {
                        loadingBar.setVisibility(View.GONE);
                        captureImageButton.setVisibility(View.VISIBLE);
                        captureImageButton.setEnabled(true);
                        PauseMenuItems(false);
                        cameraPreviewImageView.setVisibility(View.INVISIBLE);
                        repairHelpText.setText("Please Take " + (CurrentSelectedRepairModel.getMinRequiredImages() - CurrentTakenPictures) + " Picture(s) Of The Item");
                    }
                }else {
                    Logger.Error("IMAGE", "uploadImage - Got Image Upload Response: " + response.toString());
                    loadingBar.setVisibility(View.GONE);
                    captureImageButton.setVisibility(View.VISIBLE);
                    captureImageButton.setEnabled(true);
                    PauseMenuItems(false);
                    cameraPreviewImageView.setVisibility(View.INVISIBLE);
                    new AlertDialog.Builder(RepairCategorizationActivity.this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("Image Upload Error")
                            .setMessage(response.toString())
                            .setNegativeButton("Close", (dialogInterface, i) ->
                            {

                            })
                            .show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                String errorMessage = "Unknown error";
                if (networkResponse == null) {
                    if (error.getClass().equals(TimeoutError.class)) {
                        errorMessage = "Request timeout";
                    } else if (error.getClass().equals(NoConnectionError.class)) {
                        errorMessage = "Failed to connect server";
                    }
                    uploadImage(rfid, bitmap, fileName, trails + 1);
                } else {
                    String result = new String(networkResponse.data);
                    try {
                        errorMessage = result;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    loadingBar.setVisibility(View.GONE);
                    captureImageButton.setVisibility(View.VISIBLE);
                    captureImageButton.setEnabled(true);
                    cameraPreviewImageView.setVisibility(View.INVISIBLE);
                    PauseMenuItems(false);
                }
                if(errorMessage.isEmpty()){
                    errorMessage = error.getMessage();
                }
                Logger.Error("IMAGE", "uploadImage - Response Error: " + errorMessage);
                error.printStackTrace();
                new AlertDialog.Builder(RepairCategorizationActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Image Upload Error")
                        .setMessage(errorMessage)
                        .setNegativeButton("Close", (dialogInterface, i) ->
                        {

                        })
                        .show();
            }
        }) {

            @Override
            public Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                params.put("image", new DataPart(fileName.getAbsolutePath() + "*" + rfid, getFileDataFromBitmap(bitmap), null));
                return params;
            }
        };
        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        this.runOnUiThread(new Runnable() {
            public void run() {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Volley.newRequestQueue(getApplicationContext()).add(multipartRequest);
                    }
                }, 1000L * Math.min(trails,100));
            }
        });
    }

    public static byte[] getFileDataFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

}