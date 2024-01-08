package com.hc.mixthebluetooth.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.hc.mixthebluetooth.Logger;
import com.hc.mixthebluetooth.Model.ItemSerialModel;
import com.hc.mixthebluetooth.Model.RepairTypeModel;
import com.hc.mixthebluetooth.Model.TransferRepairModel;
import com.hc.mixthebluetooth.Model.ValidateRFIdsModel;
import com.hc.mixthebluetooth.R;
import com.hc.mixthebluetooth.storage.Storage;
import com.hopeland.androidpc.example.PublicData;
import com.rfidread.Connect.BaseConnect;
import com.rfidread.Enumeration.eReadType;
import com.rfidread.Helper.Helper_ThreadPool;
import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.Models.Tag_Model;
import com.rfidread.RFIDReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import Remote.APIClient;
import com.hc.mixthebluetooth.Remote.Routes.BasicApi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class TransferToRepairActivity extends AppCompatActivity {

    String IPAddress = "";

    ChipGroup repairTypeChips;

    Chip stitchChip, washChip, usedChip, donationChip, totalLossChip, repairedChip;

    TextInputLayout itemCountTextInput;

    MaterialButton confirmButton;

    List<String> allRFIDTags = new ArrayList<>();

    /**
     * These are gotten upon login and saved
     */
    public static String CurrentLocation = "";
    public static String CurrentRepairLocationCode = "";

    public List<RepairTypeModel> allRepairTypes = new ArrayList<>();

    public RepairTypeModel SelectedRepairModel = null;

    public String LotBondStation = "";

    int UserID = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_to_repair);

        repairTypeChips = findViewById(R.id.repairTypeChips);

        stitchChip = findViewById(R.id.stitchChip);
        washChip = findViewById(R.id.washChip);
        usedChip = findViewById(R.id.usedChip);
        donationChip = findViewById(R.id.donationChip);
        totalLossChip = findViewById(R.id.totalLossChip);
        repairedChip = findViewById(R.id.repairedChip);

        itemCountTextInput = findViewById(R.id.itemCount);

        confirmButton = findViewById(R.id.confirmButton);

        Storage mStorage = new Storage(this.getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");

        UserID = mStorage.getDataInt("UserID");

        LotBondStation = mStorage.getDataString("LotBondStation", "");

        HideAllItems();

        GetRepairTypes();

        StartFromLocation();


        itemCountTextInput.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(!itemCountTextInput.getEditText().getText().toString().isEmpty()) {
                    confirmButton.setEnabled(true);
                }else if(itemCountTextInput.getVisibility() == View.VISIBLE){
                    confirmButton.setEnabled(false);
                }
            }
        });

        repairTypeChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            confirmButton.setEnabled(false);
            boolean ResetCountText = true;
            if(checkedIds.size() > 0){
                String currentRepairType = "";
                if(checkedIds.get(0) == stitchChip.getId()){//Stitch
                    currentRepairType = "Stitch";
                }else if(checkedIds.get(0) == washChip.getId()){//Wash
                    currentRepairType = "Wash";
                }else if(checkedIds.get(0) == usedChip.getId()){//Used
                    currentRepairType = "Used";
                }else if(checkedIds.get(0) == donationChip.getId()){//Donation
                    currentRepairType = "Donation";
                }else if(checkedIds.get(0) == totalLossChip.getId()){//Total Loss
                    currentRepairType = "TotalLoss";
                }else if(checkedIds.get(0) == repairedChip.getId()){//Repaired
                    currentRepairType = "Repaired";
                }

                String finalCurrentRepairType = currentRepairType;
                Optional<RepairTypeModel> model = allRepairTypes.stream().filter(repairTypeModel -> repairTypeModel.getName().equalsIgnoreCase(finalCurrentRepairType)).findFirst();
                if(model.get() != null){
                    SelectedRepairModel = model.get();
                    if(model.get().getCheckCount()){
                        ResetCountText = false;
                        if(itemCountTextInput.getVisibility() == View.VISIBLE){
                            itemCountTextInput.getEditText().setText("");
                        }else {
                            TranslateAnimation animate = new TranslateAnimation(
                                    0,                 // fromXDelta
                                    0,                 // toXDelta
                                    itemCountTextInput.getHeight(),  // fromYDelta
                                    0);                // toYDelta
                            animate.setDuration(200);
                            animate.setFillAfter(true);
                            animate.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {
                                    itemCountTextInput.setVisibility(View.VISIBLE);
                                }

                                @Override
                                public void onAnimationEnd(Animation animation) {

                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {

                                }
                            });
                            itemCountTextInput.startAnimation(animate);
                        }
                    }else {
                        confirmButton.setEnabled(true);
                    }
                }else {
                    SelectedRepairModel = null;
                }

            }
            if(ResetCountText) ResetCountText();
        });

        itemCountTextInput.setVisibility(View.GONE);
        AttemptRFIDReaderConnection();

        confirmButton.setOnClickListener(view -> {
            if(SelectedRepairModel == null) return;
            ProgressDialog dialog = ProgressDialog.show(this, "",
                    "Reading RFIDs, Please wait...", true);
            dialog.show();
            RFIDStartRead();
            new CountDownTimer(5000, 5000) {
                public void onTick(long l) {

                }
/*

I/System.out: 接收到：EPC：BBB0200500007747AC00EB35
D/RFID-READER: OutPutTags - Detected Tag: BBB0200500007747AC00EB35-E28011702000146D34DF0945
V/FA: Inactivity, disconnecting from the service
I/System.out: 接收到：EPC：BBB0800000000001AC00E2E6
D/RFID-READER: OutPutTags - Detected Tag: BBB0800000000001AC00E2E6-E2801170200015C65E3C0928

 */
                public void onFinish() {
                    RFIDStopRead();
                    //allRFIDTags.add("BBB0200500007747AC00EB35-E28011702000146D34DF0945");
                    //allRFIDTags.add("BBB0800000000001AC00E2E6-E2801170200015C65E3C0928");
                    Logger.Debug("RFID-READER", "Done Reading RFIDS, Total: " + allRFIDTags.size());
                    if(allRFIDTags.size() > 0){

                        ValidateRFIdsModel model = new ValidateRFIdsModel(LotBondStation, SelectedRepairModel.getId(), allRFIDTags);
                        try {
                            //First Validate The RFIDS
                            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
                            CompositeDisposable compositeDisposable = new CompositeDisposable();
                            compositeDisposable.addAll(
                                    api.ValidateRFIds(model)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe((s) -> {
                                                if(s != null){
                                                    if(s.getSuccess()){
                                                        Logger.Debug("API", "ValidateRFIDS - RFID Validation Successful, Response Dump Below.");
                                                        try{
                                                            Gson gson = new Gson();
                                                            Logger.Debug("JSON", "ValidateRFIDS - Successful Items: " +  gson.toJson(s.getItems()));
                                                        }catch (Exception e){
                                                            Logger.Debug("JSON", "ValidateRFIDS - Failed Creating Dump: " + e.getMessage());
                                                        }
                                                        try{
                                                            //Now Start The Transfer Process After The RFIDS are validates
                                                            dialog.setMessage("Uploading RFIDS, Please Wait...");
                                                            String currentStation = "RFS004";
                                                            String toLocation = CurrentLocation.equalsIgnoreCase(CurrentRepairLocationCode) ? "W1005" : CurrentRepairLocationCode;
                                                            int count = SelectedRepairModel.getCheckCount() ? Integer.parseInt(itemCountTextInput.getEditText().getText().toString()) : -1;

                                                            compositeDisposable.addAll(
                                                                    api.RepairTransfer(new TransferRepairModel(currentStation, CurrentLocation, toLocation, UserID, count, SelectedRepairModel.getId(), allRFIDTags))
                                                                            .subscribeOn(Schedulers.io())
                                                                            .observeOn(AndroidSchedulers.mainThread())
                                                                            .subscribe((navResult) -> {
                                                                                if(navResult != null){
                                                                                    if(navResult.getSuccess() && !navResult.getNavNo().contains("Error")){
                                                                                        try{
                                                                                            //Now Receive The Transfer After the bin is at the end of the conveyor
                                                                                            dialog.setMessage("Finishing Transfer, Please Wait...");
                                                                                            String locationCode = toLocation;
                                                                                            String navNo = navResult.getNavNo();
                                                                                            Logger.Debug("NavResponse", "NavNo:" + navNo + " UserId:" + UserID + " LocationCode:" + locationCode);

                                                                                            compositeDisposable.addAll(
                                                                                                    api.ReceiveInRepair(navNo, UserID, locationCode)
                                                                                                            .subscribeOn(Schedulers.io())
                                                                                                            .observeOn(AndroidSchedulers.mainThread())
                                                                                                            .subscribe((result) -> {
                                                                                                                dialog.cancel();
                                                                                                                Logger.Debug("API", "Transfer Received Successfully");
                                                                                                                AlertDialog.Builder errorMessage = new AlertDialog.Builder(TransferToRepairActivity.this)
                                                                                                                        .setIcon(android.R.drawable.ic_dialog_info)
                                                                                                                        .setTitle("Success")
                                                                                                                        .setMessage("Transfer Received Successfully")
                                                                                                                        .setPositiveButton("Close",
                                                                                                                                new DialogInterface.OnClickListener() {
                                                                                                                                    @Override
                                                                                                                                    public void onClick(DialogInterface dialogInterface, int i) {

                                                                                                                                    }
                                                                                                                                });
                                                                                                                errorMessage.show();
                                                                                                            }, (throwable) -> {
                                                                                                                dialog.cancel();
                                                                                                                String response = "";
                                                                                                                if(throwable instanceof HttpException){
                                                                                                                    HttpException ex = (HttpException) throwable;
                                                                                                                    response = ex.response().errorBody().string();
                                                                                                                    if(response.isEmpty()){
                                                                                                                        response = throwable.getMessage();
                                                                                                                    }
                                                                                                                    Logger.Debug("API", "ValidateRFIDS(TransferReceive) - Error In HTTP Response: " + response);
                                                                                                                }else {
                                                                                                                    response = throwable.getMessage();
                                                                                                                    Logger.Error("API", "ValidateRFIDS(TransferReceive) - Error In Response: " + throwable.getMessage());
                                                                                                                }

                                                                                                                AlertDialog.Builder errorMessage = new AlertDialog.Builder(TransferToRepairActivity.this)
                                                                                                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                                                                                                        .setTitle("Error")
                                                                                                                        .setMessage(response)
                                                                                                                        .setPositiveButton("Close",
                                                                                                                                new DialogInterface.OnClickListener() {
                                                                                                                                    @Override
                                                                                                                                    public void onClick(DialogInterface dialogInterface, int i) {

                                                                                                                                    }
                                                                                                                                });
                                                                                                                errorMessage.show();

                                                                                                            }));
                                                                                        }catch(Exception ex){
                                                                                            dialog.cancel();
                                                                                            Logger.Error("API", "ValidateRFIDS - Error Trying To Start Transfer: " + ex.getMessage());
                                                                                        }
                                                                                    }else {
                                                                                        AlertDialog.Builder errorMessage = new AlertDialog.Builder(TransferToRepairActivity.this)
                                                                                                .setIcon(android.R.drawable.ic_dialog_alert)
                                                                                                .setTitle("Error")
                                                                                                .setMessage(navResult.getNavNo())
                                                                                                .setPositiveButton("Close",
                                                                                                        new DialogInterface.OnClickListener() {
                                                                                                            @Override
                                                                                                            public void onClick(DialogInterface dialogInterface, int i) {

                                                                                                            }
                                                                                                        });
                                                                                        errorMessage.show();
                                                                                        Logger.Error("API", "ValidateRFIDS(Transfer) - Error In Nav Response: " + navResult.getNavNo());
                                                                                    }
                                                                                }
                                                                            }, (throwable) -> {
                                                                                dialog.cancel();
                                                                                String response = "";
                                                                                if(throwable instanceof HttpException){
                                                                                    HttpException ex = (HttpException) throwable;
                                                                                    response = ex.response().errorBody().string();
                                                                                    if(response.isEmpty()){
                                                                                        response = throwable.getMessage();
                                                                                    }
                                                                                    Logger.Debug("API", "ValidateRFIDS(Transfer) - Error In HTTP Response: " + response);
                                                                                }else {
                                                                                    response = throwable.getMessage();
                                                                                    Logger.Error("API", "ValidateRFIDS(Transfer) - Error In Response: " + throwable.getMessage());
                                                                                }

                                                                                AlertDialog.Builder errorMessage = new AlertDialog.Builder(TransferToRepairActivity.this)
                                                                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                                                                        .setTitle("Error")
                                                                                        .setMessage(response)
                                                                                        .setPositiveButton("Close",
                                                                                                new DialogInterface.OnClickListener() {
                                                                                                    @Override
                                                                                                    public void onClick(DialogInterface dialogInterface, int i) {

                                                                                                    }
                                                                                                });
                                                                                errorMessage.show();

                                                                            }));
                                                        }catch(Exception ex){
                                                            dialog.cancel();
                                                            Logger.Error("API", "ValidateRFIDS - Error Trying To Start Transfer: " + ex.getMessage());
                                                        }
                                                    }else {
                                                        dialog.cancel();
                                                        Logger.Debug("API", "ValidateRFIDS - RFID Validation Failed, Response Dump Below.");
                                                        try{
                                                            Gson gson = new Gson();
                                                            Logger.Debug("JSON", "ValidateRFIDS - Failed Items: " +  gson.toJson(s.getItems()));
                                                            String allFailedItems = "";
                                                            for(ItemSerialModel itemModel : s.getItems()){
                                                                if(itemModel.getId() == 0){
                                                                    allFailedItems += itemModel.getRfId() + "\n";
                                                                }
                                                            }
                                                            AlertDialog.Builder errorMessage = new AlertDialog.Builder(TransferToRepairActivity.this)
                                                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                                                    .setTitle("Error")
                                                                    .setMessage("Some RFIDs Are Not Valid: \n" + allFailedItems)
                                                                    .setPositiveButton("Close",
                                                                            new DialogInterface.OnClickListener() {
                                                                                @Override
                                                                                public void onClick(DialogInterface dialogInterface, int i) {

                                                                                }
                                                                            });
                                                            errorMessage.show();
                                                        }catch (Exception e){
                                                            Logger.Debug("JSON", "ValidateRFIDS - Failed Creating Dump: " + e.getMessage());
                                                        }
                                                    }
                                                }
                                            }, (throwable) -> {
                                                dialog.cancel();
                                                String response = "";
                                                if(throwable instanceof HttpException){
                                                    HttpException ex = (HttpException) throwable;
                                                    response = ex.response().errorBody().string();
                                                    if(response.isEmpty()){
                                                        response = throwable.getMessage();
                                                    }
                                                    Logger.Debug("API", "ValidateRFIDS - Error In HTTP Response: " + response);
                                                }else {
                                                    response = throwable.getMessage();
                                                    Logger.Error("API", "ValidateRFIDS - Error In Response: " + throwable.getMessage());
                                                }
                                                AlertDialog.Builder errorMessage = new AlertDialog.Builder(TransferToRepairActivity.this)
                                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                                        .setTitle("Error")
                                                        .setMessage(response)
                                                        .setPositiveButton("Close",
                                                                new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialogInterface, int i) {

                                                                    }
                                                                });
                                                errorMessage.show();
                                            }));
                        } catch (Throwable e) {
                            dialog.cancel();
                            Logger.Error("API", "ValidateRFIDS - Error Connecting: " + e.getMessage());
                        }
                    }else {
                        dialog.cancel();
                        AlertDialog.Builder errorMessage = new AlertDialog.Builder(TransferToRepairActivity.this)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle("Error")
                                .setMessage("No RFIDS Were Detected In The Box")
                                .setPositiveButton("Close",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {

                                            }
                                        });
                        errorMessage.show();
                    }
                }
            }.start();
        });

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
                                        response = "API Error Occurred";
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
     * This function hides all the menu items and resets it
     */
    public void HideAllItems(){
        stitchChip.setVisibility(View.GONE);
        washChip.setVisibility(View.GONE);
        usedChip.setVisibility(View.GONE);
        donationChip.setVisibility(View.GONE);
        totalLossChip.setVisibility(View.GONE);
        repairedChip.setVisibility(View.GONE);
        ResetCountText();
        confirmButton.setEnabled(false);
    }

    /**
     * Resets the count text on the page
     */
    public void ResetCountText(){
        TranslateAnimation animate = new TranslateAnimation(
                0,                 // fromXDelta
                0,                 // toXDelta
                0,  // fromYDelta
                itemCountTextInput.getHeight());                // toYDelta
        animate.setDuration(200);
        animate.setFillAfter(true);
        animate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                itemCountTextInput.setVisibility(View.GONE);
                itemCountTextInput.getEditText().setText("");
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        itemCountTextInput.startAnimation(animate);
    }

    /**
     * This function starts the ui from the given location, either inside repair or outside
     * The location code of the repair is gotten from the SystemControl table under the RepairLocationCode field
     */
    public void StartFromLocation(){
        Logger.Debug("StartRepairMenu", "Setting Up Menu With CurrentLocation: " + CurrentLocation + " RepairLocation: " + CurrentRepairLocationCode);
        if(!CurrentLocation.isEmpty() && !CurrentRepairLocationCode.isEmpty()){

            if(CurrentLocation.equalsIgnoreCase(CurrentRepairLocationCode)){
                usedChip.setVisibility(View.VISIBLE);
                donationChip.setVisibility(View.VISIBLE);
                totalLossChip.setVisibility(View.VISIBLE);
                repairedChip.setVisibility(View.VISIBLE);
            }else {
                stitchChip.setVisibility(View.VISIBLE);
                washChip.setVisibility(View.VISIBLE);
            }

        }else {
            Logger.Error("Location", "StartFromLocation - Couldn't Use Location: " + CurrentLocation + " RepairLocation: " + CurrentRepairLocationCode);
            OnErrorReceived("Error", "Couldn't Determine your location");
        }
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
     * Bluetooth rfid reader info
     */

    String RFIDMac = "";

    List<BluetoothDevice> BluetoothDeviceList = new ArrayList();
    List<String> BluetoothDevicelistStr = new ArrayList();
    List<String> BluetoothDevicelistMac = new ArrayList();

    Boolean IsRFIDConnected = false;

    String RFIDName = "";

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
                        String key = TagModelKey(model);
                        if(!key.isEmpty()){
                            if(!allRFIDTags.contains(key)){
                                allRFIDTags.add(key);
                                Logger.Debug("RFID-READER", "OutPutTags - Detected Tag: " + key);
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
                    RFIDReader._Config.SetReaderAutoSleepParam(RFIDName, false, "");
                }
            }
            int RFIDCount = RFIDReader.HP_CONNECT.size();
            HashMap<String, BaseConnect> lst = RFIDReader.HP_CONNECT;
            if (lst.keySet().stream().count() > 0) {
                IsRFIDConnected = true;
            } else {
                Logger.Error("RFID-READER", "ConnectToRFIDReader - Couldn't Connect To Sled");
            }
        } catch (Exception ex) {
            Logger.Error("RFID-READER", "ConnectToRFIDReader - " + ex.getMessage());
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
                    allRFIDTags.clear();
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
        try {
            RFIDReader._Config.Stop(RFIDName);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Logger.Error("RFID-READER", "RFIDStopRead - Error While Stopping The Read Command: " + e.getMessage());
            e.printStackTrace();
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

}