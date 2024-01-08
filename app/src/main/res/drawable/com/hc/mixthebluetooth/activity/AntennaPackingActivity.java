package com.hc.mixthebluetooth.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.hc.basiclibrary.permission.PermissionUtil;
import com.hc.mixthebluetooth.Logger;
import com.hc.mixthebluetooth.Model.FillBinDCModel;
import com.hc.mixthebluetooth.Model.FillBinDCModelItem;
import com.hc.mixthebluetooth.Model.RFIDPackingInfoReceivedModel;
import com.hc.mixthebluetooth.Model.ValidatePackItemModel;
import com.hc.mixthebluetooth.R;
import com.hc.mixthebluetooth.Remote.Routes.BasicApi;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDDevice;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDDevicesManager;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDListener;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDOutput;
import com.hc.mixthebluetooth.activity.tool.Analysis;
import com.hc.mixthebluetooth.storage.Storage;
import com.rfidread.Connect.BaseConnect;
import com.rfidread.Models.Tag_Model;
import com.rfidread.RFIDReader;
import com.util.General;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import Model.RFIDPackingInfoModel;
import Remote.APIClient;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class AntennaPackingActivity extends AppCompatActivity {


    Button binRFIDBtn, detectedRFIDsCount, startBtn, confirmBtn, packedItemsCount, pendingItemsCount;

    String CurrentBinBarcode = null;

    TextView secondHelpText;

    ArrayList<Integer> AntennaList;

    boolean IsRFIDConnected = false;

    Storage mStorage = null;
    private String IPAddress = "";
    private int UserID = -1;

    private String binRfidPrefix = "BBB020,BBB20";
    private List<String> binPrefixList = new ArrayList<>();

    private ArrayList<String> PendingItems = new ArrayList<>();

    private HashMap<String, String> ValidDetectedItems = new HashMap<>(), UndefinedDetectedItems = new HashMap<>();

    private HashMap<String, ValidatePackItemModel> ValidPackedItems = new HashMap<>();

    private HashMap<String, String> FailedPackedItems = new HashMap<>();

    private ArrayList<String> ProcessedRFIDS = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_antenna_packing);

        mStorage = new Storage(this);

        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
        UserID = mStorage.getDataInt("UserID");

        String binRfidPrefixStr = General.getGeneral(getApplication()).getSetting(getApplicationContext(),"RFIDPackingBoxPrefix");

        if(!binRfidPrefixStr.isEmpty()){
            binRfidPrefix = binRfidPrefixStr;
        }

        Logger.Debug("SystemControl", "Got RFIDPackingBoxPrefix List: " + binRfidPrefix);

        binPrefixList.addAll(Arrays.asList(binRfidPrefix.split(",")));

        binRFIDBtn = findViewById(R.id.binRFIDBtn);
        detectedRFIDsCount = findViewById(R.id.detectedRFIDsCount);
        startBtn = findViewById(R.id.startBtn);
        confirmBtn = findViewById(R.id.confirmBtn);
        packedItemsCount = findViewById(R.id.packedItemsCount);
        pendingItemsCount = findViewById(R.id.pendingItemsCount);

        secondHelpText = findViewById(R.id.secondHelpText);

        startBtn.setOnClickListener(view -> {


            if (startBtn.getText().equals("Start")) {
                startBtn.setText("Pause");
                ReadRFIDS();
            } else {
                startBtn.setText("Start");
                PauseRFID();
            }

        });

        detectedRFIDsCount.setOnClickListener(view -> {
            ViewDetectedRFIDS();
        });


        packedItemsCount.setOnClickListener(view -> {
            ViewPackedRFIDS();
        });

        confirmBtn.setOnClickListener(view -> {
            PackReadRFIDS();
        });

        RequestPermissions();



        GetRFIDAntennaPackingDelay();

    }

    public void StartBackgroundTimer(int timer){

        Logger.Debug("RFIDPacking", "Starting Background Timer For: " + timer);

        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {

                if(CurrentBinBarcode != null){
                    if(PendingItems.size() > 0){
                        ProcessDetectedRFIDItems(PendingItems);
                    }
                }

                h.postDelayed(this, timer);
            }
        }, timer);
    }

    @SuppressLint("ResourceAsColor")
    private void StartRFIDConnection() {
        try {
            String Connection = "";
            RFIDDevicesManager.setOutput(new RFIDOutput(new RFIDListener() {
                @Override
                public void notifyListener(RFIDDevice device, Tag_Model tag_model) {
                    ProcessDetectedRFIDTag(tag_model);
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
            }));
            AntennaList = RFIDDevicesManager.getSingleAntennaReader().getAntennaListInt();
            int RFIDCount = RFIDReader.HP_CONNECT.size();

            secondHelpText.setText(String.valueOf(RFIDCount) + " Connected RFID Devices\n" + Connection);

            HashMap<String, BaseConnect> lst = RFIDReader.HP_CONNECT;
            if (lst.keySet().stream().count() > 0) {
                IsRFIDConnected = true;
            } else {

            }
        } catch (Exception ex) {
            secondHelpText.setText(ex.getMessage());
        }

    }

    public void ReadRFIDS(){
        if (!IsRFIDConnected)
            return;
        if (!IsReading())
            return;


        try {
            AttemptAntennaRead();

        } catch (Exception ex) {
            Logger.Error("PACKING", "ReadRFIDS - Error " + ex.getMessage());
        }
    }

    public void PauseRFID()
    {
        secondHelpText.setText("Antenna Reading Paused");
        if (RFIDDevicesManager.getSingleAntennaReader() != null)
            RFIDDevicesManager.getSingleAntennaReader().stop();
    }


    public void AttemptAntennaRead() {
        secondHelpText.setText("Antenna Currently Reading");
        int rs = RFIDDevicesManager.readEPCSingleAntennaAll();
        Logger.Debug("PACKING", "AttemptAntennaRead - Reading from Antenna result " + rs);
        switch (rs) {
            case 1:
                secondHelpText.setText("Reading Error: Antenna port Parameter err ");
                break;
            case 2:
                secondHelpText.setText("Reading Error: Choosing tag Parameter error ");
                break;
            case 3:
                secondHelpText.setText("Reading Error: TID parameter error ");
                break;
            case 4:
                secondHelpText.setText("Reading Error: user data area parameter error ");
                break;
            case 5:
                secondHelpText.setText("Reading Error: reserved area parameter error ");
                break;
            case 6:
                secondHelpText.setText("Reading Error: Other parameter error ");
                break;
        }


    }

    private Boolean IsReading() {
        return startBtn.getText().equals("Pause");
    }

    public void ProcessDetectedRFIDTag(Tag_Model model){

        if(CurrentBinBarcode == null)
        {
            ProcessDetectedBinBarcode(model);
            return;
        }

        String rfid = TagModelKey(model);

        if(!PendingItems.contains(rfid) && !ProcessedRFIDS.contains(rfid)) {
            PendingItems.add(rfid);
            UpdateUIText(pendingItemsCount, PendingItems.size() + " Pending");
        }

        //ProcessDetectedRFIDItem(rfid);
    }

    public void ProcessDetectedRFIDItems(ArrayList<String> rfids){
        ArrayList<String> ToSendRFIDS = new ArrayList<>();

        for (String pendingRFID : rfids) {
            if(!ProcessedRFIDS.contains(pendingRFID)){
                ProcessedRFIDS.add(pendingRFID);
            }
            ToSendRFIDS.add(pendingRFID);
        }

        PendingItems.clear();
        UpdateUIText(pendingItemsCount, PendingItems.size() + " Pending");

        Logger.Debug("RFIDPacking", "ProcessDetectedRFIDItems - Processing Detected " + ToSendRFIDS.size() + " Items!");

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.GetRFIDPackingInfo(new RFIDPackingInfoModel(CurrentBinBarcode, ToSendRFIDS, General.getGeneral(this).mainLocation, UserID, General.getGeneral(this).currentPackReason.getId()))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {

                                if(s != null){
                                    for(RFIDPackingInfoReceivedModel model : s){
                                        if(model.getStatus() == 1) {
                                            if (!ValidDetectedItems.containsKey(model.getRfid())) {
                                                ValidDetectedItems.put(model.getRfid(), model.getItemSerial());
                                            }

                                            if(!ValidPackedItems.containsKey(model.getRfid())){
                                                try {
                                                    ValidatePackItemModel packModel = new Gson().fromJson(model.getDetails(), ValidatePackItemModel.class);
                                                    ValidPackedItems.put(model.getRfid(), packModel);
                                                }catch(Exception ex){
                                                    FailedPackedItems.put(model.getRfid(), ex.getMessage());
                                                }
                                            }

                                        }
                                        else if(model.getStatus() == 0){
                                            if(!UndefinedDetectedItems.containsKey(model.getRfid())){
                                                UndefinedDetectedItems.put(model.getRfid(), model.getMessage());
                                            }
                                        }else if(model.getStatus() == 3){
                                            if (!ValidDetectedItems.containsKey(model.getRfid())) {
                                                ValidDetectedItems.put(model.getRfid(), model.getItemSerial());
                                            }

                                            if(!FailedPackedItems.containsKey(model.getRfid())){
                                                FailedPackedItems.put(model.getRfid(), model.getMessage());
                                            }
                                        }
                                    }

                                    UpdateUIText(detectedRFIDsCount, "Detected " + (ValidDetectedItems.size() + UndefinedDetectedItems.size()) + " Items");
                                    UpdateUIText(packedItemsCount, "Packed " + ValidPackedItems.size() + " Items");


                                }
                                Logger.Debug("API", "ProcessDetectedRFIDTag - Received Processing Result: " + s);
                            }, (throwable) -> {
                                String response = "";
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    response = ex.response().errorBody().string();
                                    if(response.isEmpty()){
                                        response = throwable.getMessage();
                                    }
                                    Logger.Debug("API", "ProcessDetectedRFIDTag - Returned HTTP Error " + response);
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "ProcessDetectedRFIDTag - Error In API Response: " + response);
                                }

                                ShowAlertDialog("Error", response);

                            }));
        } catch (Throwable e) {
            Logger.Error("API", "ProcessDetectedRFIDTag - Error Connecting: " + e.getMessage());
            ShowSnackBar("Error Connecting To Services!", Snackbar.LENGTH_LONG);
        }
    }

    public void ProcessDetectedBinBarcode(Tag_Model model){

        if(CurrentBinBarcode != null)
        {
            return;
        }

        String rfid = TagModelKey(model);

        if(ProcessedRFIDS.contains(rfid))
            return;

        if(!IsValidBinRFID(rfid)){
            if(!PendingItems.contains(rfid) && !ProcessedRFIDS.contains(rfid)) {
                PendingItems.add(rfid);
                UpdateUIText(pendingItemsCount, PendingItems.size() + " Pending");
            }
            return;
        }

        try {
            String BarcodeFromEPC = EPCToUPC(model._EPC);
            ProcessedRFIDS.add(rfid);
            Handler mainHandler = new Handler(this.getMainLooper());

            Runnable mainThreadRunnable = new Runnable() {
                @Override
                public void run() {
                    ValidateDetectedBinBarcode(rfid, BarcodeFromEPC);
                }
            };
            mainHandler.post(mainThreadRunnable);
        } catch (Throwable e) {
            Logger.Error("API", "ProcessDetectedBinBarcode - Error: " + e.getMessage());
        }
    }

    public void ValidateDetectedBinBarcode(String rfid, String barcode){

        try {

            ProgressDialog dialog = ProgressDialog.show(AntennaPackingActivity.this, "", "Validating Bin " + barcode + " Please Wait...", true);
            dialog.show();


            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.ValidateBin(barcode, rfid, General.getGeneral(this).currentPackReason.getId(), UserID, General.getGeneral(this).mainLocationId)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    dialog.cancel();
                                    CurrentBinBarcode = barcode;
                                    UpdateUIText(binRFIDBtn, "Detected Bin Barcode " + CurrentBinBarcode);
                                }else {
                                    ShowSnackBar("Error Bin Model Is Empty", Snackbar.LENGTH_LONG);
                                    dialog.cancel();
                                }
                            }, (throwable) -> {
                                String response = "";
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    response = ex.response().errorBody().string();
                                    if(response.isEmpty()){
                                        response = throwable.getMessage();
                                    }
                                    Logger.Debug("API", "ValidateDetectedBinBarcode - Returned HTTP Error " + response);
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "ValidateDetectedBinBarcode - Error In API Response: " + throwable.getMessage());
                                }
                                ShowAlertDialog("Error", response);
                                dialog.cancel();

                            }));
        } catch (Throwable e) {
            Logger.Error("API", "ValidateDetectedBinBarcode - Error Connecting: " + e.getMessage());
            ShowSnackBar("Error Connecting To Services!", Snackbar.LENGTH_LONG);
        }
    }

    public void GetRFIDAntennaPackingDelay(){

        String rfidPackingDelay = General.getGeneral(getApplication()).getSetting(getApplicationContext(),"RFIDPackingDelay");

        if(!rfidPackingDelay.isEmpty()){
            int timer = 3000;
            try{
                timer = Integer.parseInt(rfidPackingDelay);
            }catch(Exception ex){}
            StartBackgroundTimer(timer);
        }
        Logger.Debug("SystemControl", "Got RFIDPackingDelay: " + rfidPackingDelay);
    }

    public String EPCToUPC(String EPC) {
        String Barcode = EPC.substring(3, 16).toUpperCase();
        Boolean IsNumeric = true;

        if (Barcode.endsWith("A") ||
                Barcode.endsWith("B") ||
                Barcode.endsWith("C") ||
                Barcode.endsWith("D") ||
                Barcode.endsWith("E") ||
                Barcode.endsWith("F")
        )
            IsNumeric = false;
        if (Barcode.startsWith("0") && IsNumeric && Barcode.length() > 12)
            Barcode = Barcode.substring(1);

        return Barcode;
    }

    public void UpdateUIText(Button btn, String text){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ObjectAnimator animate = ObjectAnimator.ofPropertyValuesHolder(btn,
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
                                btn.setText(text);
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
    }

    public void ViewDetectedRFIDS(){
        String text = "Valid Items: \n\n";
        for(String rfid : ValidDetectedItems.keySet()){
            text += ValidDetectedItems.get(rfid) + " - " + rfid + "\n";
        }

        if(UndefinedDetectedItems.size() > 0) {
            text += "\n\n\n\nUnDefined Items: \n\n";
            for (String rfid : UndefinedDetectedItems.keySet()) {
                text += rfid + " - " + UndefinedDetectedItems.get(rfid) + "\n";
            }
        }

        new AlertDialog.Builder(AntennaPackingActivity.this).setTitle("Detected " + (ValidDetectedItems.size() + UndefinedDetectedItems.size()) + " Items")
                .setMessage(text)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                }).show();
    }

    public void ViewPackedRFIDS(){
        String text = "Valid Items: \n\n";
        for(String rfid : ValidPackedItems.keySet()){
            text += ValidDetectedItems.get(rfid) + " - " + ValidPackedItems.get(rfid).toString() + "\n";
        }
        if(FailedPackedItems.size() > 0) {
            text += "\n\n\n\nInvalid Items: \n\n";
            for (String rfid : FailedPackedItems.keySet()) {
                text += ValidDetectedItems.get(rfid) + " - " + FailedPackedItems.get(rfid) + "\n";
            }
        }
        new AlertDialog.Builder(AntennaPackingActivity.this).setTitle("Detected " + (ValidPackedItems.size() + FailedPackedItems.size()) + " Items")
                .setMessage(text)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                }).show();
    }

    public void ShowPackingFailed(String message, ArrayList<String> items){
        String text = message + "\n\n";
        for(String item : items){
            text += item + "\n";
        }

        new AlertDialog.Builder(AntennaPackingActivity.this).setTitle("Packing Failed")
                .setMessage(text)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                }).show();
    }

    public void PackReadRFIDS(){
        startBtn.setText("Start");
        PauseRFID();

        if(CurrentBinBarcode == null)
            return;

        if(ValidDetectedItems.size() == 0)
            return;

        ProgressDialog dialog = ProgressDialog.show(this, "", "Packing Items Please Wait...", true);
        dialog.show();

        UpdateUIText(binRFIDBtn, "Waiting For Bin RFID");
        UpdateUIText(detectedRFIDsCount, "Detected 0 Items");
        UpdateUIText(packedItemsCount, "Packed 0 Items");
        UpdateUIText(pendingItemsCount, "0 Pending");



        List<FillBinDCModelItem> items = new ArrayList<>();

        ArrayList<String> totalClassAB = new ArrayList<>(), totalClassC = new ArrayList<>(), totalDamaged = new ArrayList<>(), totalUnDamaged = new ArrayList<>(),
                totalRejected = new ArrayList<>(), totalZeroPrice = new ArrayList<>(), totalOffSeason = new ArrayList<>(),
                totalUndefined = new ArrayList<>();

        boolean proceedPacking = true;

        for (String rfid : ValidPackedItems.keySet()) {
            ValidatePackItemModel model = ValidPackedItems.get(rfid);

            String itemNumber = ValidDetectedItems.get(rfid);

            if (model.getRejected())
                totalRejected.add(itemNumber);

            if (model.getDamaged())
                totalDamaged.add(itemNumber);
            else
                totalUnDamaged.add(itemNumber);

            if (model.getZeroPrice())
                totalZeroPrice.add(itemNumber);

            if (model.getOffSeason())
                totalOffSeason.add(itemNumber);

            if (model.getClassLetter().equalsIgnoreCase("A") || model.getClassLetter().equalsIgnoreCase("B"))
                totalClassAB.add(itemNumber);

            if (model.getClassLetter().equalsIgnoreCase("C"))
                totalClassC.add(itemNumber);

            items.add(new FillBinDCModelItem(itemNumber));
        }

        for (String rfid : UndefinedDetectedItems.keySet()) {
            String data = rfid + " - " + UndefinedDetectedItems.get(rfid);
            totalUndefined.add(data);
        }

        int totalItems = items.size() + totalUndefined.size();
        General general = General.getGeneral(this);

        if(general.currentPackReason.getClassAB() != null){
            int threshold = (general.currentPackReason.getClassAB() * totalItems) / 100;
            if(totalClassAB.size() > threshold){
                ShowPackingFailed("Class A/B Items(" + totalClassAB.size() + ") Reached More Than Threshold " + threshold + "/" + totalItems, totalClassAB);
                proceedPacking = false;
            }
        }

        if(general.currentPackReason.getClassC() != null){
            int threshold = (general.currentPackReason.getClassC() * totalItems) / 100;
            if(totalClassC.size() > threshold){
                ShowPackingFailed("Class C Items(" + totalClassC.size() + ") Reached More Than Threshold " + threshold + "/" + totalItems, totalClassC);
                proceedPacking = false;
            }
        }

        if(general.currentPackReason.getDamaged() != null){
            int threshold = (general.currentPackReason.getDamaged() * totalItems) / 100;
            if(totalDamaged.size() > threshold){
                ShowPackingFailed("Damaged Items(" + totalDamaged.size() + ") Reached More Than Threshold " + threshold + "/" + totalItems, totalDamaged);
                proceedPacking = false;
            }
        }

        if(general.currentPackReason.getUnDamaged() != null){
            int threshold = (general.currentPackReason.getUnDamaged() * totalItems) / 100;
            if(totalUnDamaged.size() > threshold){
                ShowPackingFailed("UnDamaged Items(" + totalUnDamaged.size() + ") Reached More Than Threshold " + threshold + "/" + totalItems, totalUnDamaged);
                proceedPacking = false;
            }
        }

        if(general.currentPackReason.getRejected() != null){
            int threshold = (general.currentPackReason.getRejected() * totalItems) / 100;
            if(totalRejected.size() > threshold){
                ShowPackingFailed("Rejected Items(" + totalRejected.size() + ") Reached More Than Threshold " + threshold + "/" + totalItems, totalRejected);
                proceedPacking = false;
            }
        }

        if(general.currentPackReason.getOffSeason() != null){
            int threshold = (general.currentPackReason.getOffSeason() * totalItems) / 100;
            if(totalOffSeason.size() > threshold){
                ShowPackingFailed("Off-Season Items(" + totalOffSeason.size() + ") Reached More Than Threshold " + threshold + "/" + totalItems, totalOffSeason);
                proceedPacking = false;
            }
        }

        if(general.currentPackReason.getZeroPrice() != null){
            int threshold = (general.currentPackReason.getZeroPrice() * totalItems) / 100;
            if(totalZeroPrice.size() > threshold){
                ShowPackingFailed("Zero-Price Items(" + totalZeroPrice.size() + ") Reached More Than Threshold " + threshold + "/" + totalItems, totalZeroPrice);
                proceedPacking = false;
            }
        }

        if(general.currentPackReason.getUnDefined() != null){
            int threshold = (general.currentPackReason.getUnDefined() * totalItems) / 100;
            if(totalUndefined.size() > threshold){
                ShowPackingFailed("UnDefined-Price Items(" + totalUndefined.size() + ") Reached More Than Threshold " + threshold + "/" + totalItems, totalUndefined);
                proceedPacking = false;
            }
        }

        if (proceedPacking) {
            try {
                FillBinDCModel model = new FillBinDCModel(CurrentBinBarcode, UserID, items, general.currentPackReason.getId(),
                        general.mainLocation, general.mainLocationId);

                BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
                CompositeDisposable compositeDisposable = new CompositeDisposable();
                compositeDisposable.addAll(
                        api.FillBinDC(model)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe((s) -> {
                                    if (s != null) {
                                        try {
                                            String result = s.string();
                                            secondHelpText.setText("Result: "+result);
                                            ShowSnackBar(result, Snackbar.LENGTH_LONG);
                                            if (result.contains("Count")) {
                                                VerifyBinCount(model.getBinBarcode(), model.getItemSerials().size(), model.getLocationId(), dialog);
                                            } else {
                                                dialog.cancel();
                                            }
                                            Logger.Debug("API", "PackReadRFIDS - Returned: " + result);
                                        } catch (Exception ex) {
                                            ShowSnackBar("Error, Invalid Response!", Snackbar.LENGTH_LONG);
                                        }
                                    }
                                }, (throwable) -> {
                                    String response = "";
                                    if (throwable instanceof HttpException) {
                                        HttpException ex = (HttpException) throwable;
                                        response = ex.response().errorBody().string();
                                        if (response.isEmpty()) {
                                            response = throwable.getMessage();
                                        }
                                        Logger.Debug("API", "PackReadRFIDS - Returned HTTP Error " + response);
                                    } else {
                                        response = throwable.getMessage();
                                        Logger.Error("API", "PackReadRFIDS - Error In API Response: " + throwable.getMessage());
                                    }
                                    ShowSnackBar(response, Snackbar.LENGTH_LONG);
                                    if (response.contains("Count")) {
                                        VerifyBinCount(model.getBinBarcode(), model.getItemSerials().size(), model.getLocationId(), dialog);
                                    } else {
                                        dialog.cancel();
                                    }

                                }));
            } catch(Throwable e){
                Logger.Error("API", "PackReadRFIDS - Error Connecting: " + e.getMessage());
                dialog.cancel();
            }
        }else {
            dialog.cancel();
        }

        CurrentBinBarcode = null;
        PendingItems.clear();
        ValidDetectedItems.clear();
        UndefinedDetectedItems.clear();
        ProcessedRFIDS.clear();

        ValidPackedItems.clear();
        FailedPackedItems.clear();

    }

    public void VerifyBinCount(String barcode, int count, int location, ProgressDialog dialog){
        try {

            List<FillBinDCModelItem> items = new ArrayList<>();

            for(String rfid : ValidPackedItems.keySet()){
                items.add(new FillBinDCModelItem(ValidDetectedItems.get(rfid)));
            }

            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.FillBinCount(UserID, count, barcode, location)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try {
                                        String result = s.string();
                                        secondHelpText.setText("Result: "+result);
                                        ShowSnackBar(result, Snackbar.LENGTH_LONG);
                                        dialog.cancel();
                                        Logger.Debug("API", "VerifyBinCount - Returned: " + result);
                                    }catch (Exception ex){
                                        ShowSnackBar("Error, Invalid Response!", Snackbar.LENGTH_LONG);
                                    }
                                }
                            }, (throwable) -> {
                                String response = "";
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    response = ex.response().errorBody().string();
                                    if(response.isEmpty()){
                                        response = throwable.getMessage();
                                    }
                                    Logger.Debug("API", "VerifyBinCount - Returned HTTP Error " + response);
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "VerifyBinCount - Error In API Response: " + throwable.getMessage());
                                }
                                ShowSnackBar(response, Snackbar.LENGTH_LONG);
                                dialog.cancel();//Bin ID not found:207000000772

                            }));
        } catch (Throwable e) {
            Logger.Error("API", "VerifyBinCount - Error Connecting: " + e.getMessage());
            dialog.cancel();
        }
    }

    public void ValidateFillBinItem(String rfid, String itemNo){
        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.ValidateFillBinRFIDItem(itemNo, rfid, CurrentBinBarcode, General.getGeneral(this).mainLocation, UserID, General.getGeneral(this).currentPackReason.getId())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    if(!ValidPackedItems.containsKey(rfid)){
                                        ValidPackedItems.put(rfid, s);
                                        UpdateUIText(packedItemsCount, "Packed " + ValidPackedItems.size() + " Items");
                                    }
                                    Logger.Debug("API", "ValidateFillBinItem - Returned: " + s.toString());
                                }
                            }, (throwable) -> {
                                String response = "";
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    response = ex.response().errorBody().string();
                                    if(response.isEmpty()){
                                        response = throwable.getMessage();
                                    }
                                    Logger.Debug("API", "ValidateFillBinItem - Returned HTTP Error " + response);
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "ValidateFillBinItem - Error In API Response: " + throwable.getMessage());
                                }

                                if(!FailedPackedItems.containsKey(rfid)){
                                    FailedPackedItems.put(rfid, response);
                                }

                            }));
        } catch (Throwable e) {
            Logger.Error("API", "ValidateFillBinItem - Error Connecting: " + e.getMessage());
            if(!FailedPackedItems.containsKey(rfid)){
                FailedPackedItems.put(rfid, "Failed To Connect!");
            }
        }
    }

    private void RequestPermissions(){
        PermissionUtil.requestEach(AntennaPackingActivity.this, new PermissionUtil.OnPermissionListener() {


            @Override
            public void onFailed(boolean showAgain) {

            }

            @Override
            public void onSucceed() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        if (Analysis.isOpenGPS(AntennaPackingActivity.this))
                            StartRFIDConnection();
                        else
                            OpenSettings();
                    }
                },1000);

            }
        }, PermissionUtil.LOCATION);
    }

    private void OpenSettings(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        builder.setTitle("Error")
                .setMessage("GPS Location Isn't Enabled!, Please enable the location permission of the phone!")
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, 10);
                    }
                }).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(IsReading()){
            ReadRFIDS();
        }
    }

    @Override
    protected void onPause() {
        PauseRFID();
        super.onPause();
    }


    @Override
    public void onBackPressed() {
        PauseRFID();
        boolean disconnected = RFIDDevicesManager.disconnectSingle();
        Logger.Debug("PACKING", "Antenna Disconnected: " + disconnected);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        PauseRFID();
        boolean disconnected = RFIDDevicesManager.disconnectSingle();
        Logger.Debug("PACKING", "Antenna Disconnected: " + disconnected);
        super.onDestroy();
    }

    public boolean IsValidBinRFID(String rfid) {
        return rfid != null && binPrefixList.stream().anyMatch(i -> rfid.toLowerCase().startsWith(i.toLowerCase()));
    }

    public String TagModelKey(Tag_Model model) {
        if (model._EPC != null && model._TID != null && !model._EPC.isEmpty() && !model._TID.isEmpty())
            return model._EPC + "-" + model._TID;
        return "";
    }

    public void ShowSnackBar(String message, int length){
        Snackbar.make(findViewById(R.id.antennaPackingMainLayout), message, length)
                .setAction("No action", null).show();
    }

    public void ShowAlertDialog(String title, String message){
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

}