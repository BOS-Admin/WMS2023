package com.hc.mixthebluetooth.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentManager;

import com.hc.basiclibrary.ioc.ViewById;
import com.hc.basiclibrary.permission.PermissionUtil;
import com.hc.basiclibrary.viewBasic.BasActivity;
import com.hc.bluetoothlibrary.DeviceModule;
import com.hc.mixthebluetooth.Logger;
import com.hc.mixthebluetooth.R;
import com.hc.mixthebluetooth.Remote.Routes.BasicApi;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDDevice;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDDevicesManager;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDListener;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDOutput;
import com.hc.mixthebluetooth.activity.tool.Analysis;
import com.hc.mixthebluetooth.storage.Storage;
import com.rfidread.Connect.BaseConnect;
import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.Models.Tag_Model;
import com.rfidread.RFIDReader;
import com.util.General1;
import com.util.Logging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import Model.AuditRFIDModel;
import Model.AuditRFIDModelPost;
import Model.AuditRFIDModelPostItems;
import Model.RfidItems;
import Model.UpcItems;
import Remote.APIClient;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;

public class ReplenishmentAuditActivity extends BasActivity implements IAsynchronousMessage, RFIDListener {

    private HashMap<String, Tag_Model> boxMap = new HashMap<String, Tag_Model>();
    private Object boxMap_Lock = new Object();
    private Object boxListLock = new Object();
    private List<String> nonValidRfids = new ArrayList<>();
    private boolean checkingBoxNumber = false;
    private Object apiDone_Lock = new Object();
    private boolean apiDone;
    ArrayList<Integer> antList;
    String validatedBoxNumber1 = "";

    private String itemSerialRfidPrefix = "BBB";
    private String binRfidPrefix = "BBB020,BBB20";
    private List<String> itemPrefixList = new ArrayList<>();
    private List<String> binPrefixList = new ArrayList<>();

    private HashMap<String, String> detectedLotBondedItems = new HashMap<>();

    private boolean DiscardItemSerial = false;

    public boolean validItemRfid(String s) {
        if (s == null)
            return false;
        if (DiscardItemSerial) {
            return true;
        } else {
            return itemPrefixList.stream().anyMatch(i -> s.toLowerCase().startsWith(i.toLowerCase()));
        }
    }

    public boolean validBinRfid(String s) {
        return s != null && binPrefixList.stream().anyMatch(i -> s.toLowerCase().startsWith(i.toLowerCase()));
    }

    private String AntennaLog = "";

    public void read() {
        new Thread(()->{
            AntennaLog = "Trying To Read";
            int rs = RFIDDevicesManager.readEPCSingleAntennaAll();
            // Log.i("AH-Log-time", "Reading from Antenna " +RFIDDevicesManager.getSingleAntennaReader().getAntennaNo());
            //Log.i("AH-Log-time", "Reading from Antenna result " + rs);
            switch (rs) {
                case 0:
                    AntennaLog = "Reading Success";
                    break;
                case 1:
                    AntennaLog = "Reading Error (1): Antenna port Parameter err ";
                    break;
                case 2:
                    AntennaLog = "Reading Error (2): Choosing tag Parameter error ";
                    break;
                case 3:
                    AntennaLog = "Reading Error (3): TID parameter error ";
                    break;
                case 4:
                    AntennaLog = "Reading Error (4): user data area parameter error ";
                    break;
                case 5:
                    AntennaLog = "Reading Error (5): reserved area parameter error ";
                    break;
                default:
                    AntennaLog = "Reading Error (" + rs + "): Other parameter error ";
                    break;
            }

        }).start();



    }


    public void validateBoxNumber() {
        new Thread(() -> {
            List<Tag_Model> boxRfids;
            HashMap<String, String> boxMapResults = new HashMap<>();
            updateBoxBarcode("CHECKING FOR BOX", "Start Searching For Box Barcodes", 0);
            updateTextBoxList(boxMapResults);
            while (checkingBoxNumber) {
                boxRfids = new ArrayList<>();
                synchronized (boxMap_Lock) {
                    boxRfids.addAll(boxMap.values());
                }

                for (Tag_Model model : boxRfids) {
                    if (!nonValidRfids.contains(model._EPC) && ValidatedBoxNumber == null || ValidatedBoxNumber.isEmpty()) {
                        String EPC = model._EPC.startsWith("30396062C39ADD4000231D87") ? "BBB0200700003917AC011038" : model._EPC;
                        String BarcodeFromEPC = EPCToUPC(EPC);
                        if (validBinRfid(EPC)) {
                            synchronized (boxListLock) {
                                if (!boxMapResults.containsKey(EPC))
                                    boxMapResults.put(EPC, ": Checking...");
                                updateTextBoxList(boxMapResults);
                            }
                            InitRFIDApi(BarcodeFromEPC, EPC);
                            while (true) {
                                synchronized (apiDone_Lock) {
                                    if (apiDone)
                                        break;
                                }

                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (ValidatedBoxNumber == null || ValidatedBoxNumber == "" || ValidatedBoxNumber.isEmpty() || ValidatedBoxNumber.length() < 5) {
                                nonValidRfids.add(model._EPC);
                                boxMapResults.put(EPC, ": Not Valid ");
                                updateTextBoxList(boxMapResults);
                            } else {
                                checkingBoxNumber = false;
                                updateBoxBarcode(ValidatedBoxNumber, "ValidatedBoxNumber : " + ValidatedBoxNumber, 1);
                                validatedBoxNumber1 = ValidatedBoxNumber;
                                boxMapResults.put(EPC, ": Valid ");
                                updateTextBoxList(boxMapResults);
                                return;
                            }
                        }
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }).start();

    }

    private void error(String message) {
        error = "E2 "+message;
    }

    private String boxBarcodeFullMessage = "";

    private void updateBoxBarcode(String message, String fullMessage, int status) {
        runOnUiThread(() -> {
            boxBarcodeFullMessage = fullMessage;
            btnBoxBarcode.setText(truncate(message, 30));
            switch (status) {
                case 1:
                    btnBoxBarcode.setBackgroundResource(R.drawable.rounded_corner_green);
                    break;
                case -1:
                    btnBoxBarcode.setBackgroundResource(R.drawable.rounded_corner_red);
                    break;
                default:
                    btnBoxBarcode.setBackgroundResource(R.drawable.rounded_corner1);
            }

        });

    }

    private String truncate(String text, int length) {
        if (text == null)
            return "";
        if (text.length() <= length) {
            return text;
        } else {
            return text.substring(0, length);
        }
    }

    String boxList = "";

    private void updateTextBoxList(HashMap<String, String> map) {
        String message = "";
        if (map.size() > 1) {
            message += map.size() + " box numbers were found !!\n";
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            message += entry.getKey() + " " + entry.getValue() + "\n";
        }
        boxList = message;
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void OutPutTags(Tag_Model model) {
        try {
            String tmodel;
            synchronized (hmList_Lock) {
                tmodel = TagModelKey(model);
                if (tmodel.isEmpty())
                    return;



                if (!hmList.containsKey(tmodel)) {
                    LogDebug("EPC Read11" + tmodel);
                    if (validBinRfid(tmodel)) {
                        synchronized (boxMap_Lock) {
                            if (!boxMap.containsKey(tmodel))
                                boxMap.put(tmodel, model);
                        }

                    } else
                        ProcessDetectedRFIDTag(model);

                }

            }


        } catch (Exception ex) {
            Log.d("Debug", "Tags output exceptions:" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void ProcessDetectedRFIDTag(Tag_Model model) {
        String rfid = TagModelKey(model);
        if (!DiscardItemSerial) {
            hmList.put(rfid, model);
        }
        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.GetRFIDItemSerial(rfid)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {
                                    if (DiscardItemSerial) {
                                        hmList.put(rfid, model);
                                    }
                                    if (!detectedLotBondedItems.containsKey(rfid)) {
                                        detectedLotBondedItems.put(rfid, s);
                                    }
                                    Logger.Debug("API", "ProcessDetectedRFIDTag - RFID Tag: " + rfid + " Has ItemSerial: " + s);
                                }
                            }, (throwable) -> {
                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    String response = ex.response().errorBody().string();
                                    if (response.isEmpty()) {
                                        response = throwable.getMessage();
                                    }
                                    Logger.Debug("API", "ProcessDetectedRFIDTag - Returned HTTP Error " + response);
                                } else {
                                    Logger.Error("API", "ProcessDetectedRFIDTag - Error In API Response: " + throwable.getMessage());
                                }
                                if (DiscardItemSerial && (rfid.toLowerCase().contains("bbb") || rfid.toLowerCase().startsWith("ddd"))) {
                                    Logger.Debug("API", "ProcessDetectedRFIDTag - RFID " + rfid + " Not Bonded And Contains BBB/DDD We Need To Fail The Audit");
                                    hmList.put(rfid, model);
                                }
                            }));
        } catch (Throwable e) {
            Logger.Error("API", "ProcessDetectedRFIDTag - Error Connecting: " + e.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.replenishment_audit);
        //设置头部
        //setTitle();
        setContext(this);

        btnPopUp.setOnClickListener(e -> {
            showMessage("Info",String.join("\n",getPopUpMessages()));
        });

        btnBoxBarcode.setOnClickListener(e -> {
            showMessage("Box Barcode",String.join("\n",getBoxBarcodeMessages()));
        });

        btnAuditResult.setOnClickListener(e -> {
            showMessage("Audit Result",String.join("\n",getAuditMessages()));
        });

        btnLocation.setOnClickListener(e -> {
            if(removeUPcs){
                Intent myIntent = new Intent(getApplicationContext(),AuditRemoveRemoveUPCActivity.class);
                AuditRemoveRemoveUPCActivity.BoxBarcode=validatedBoxNumber1;
                startActivity(myIntent);
            }
            else
                showMessage("Location",String.join("\n",getLocationMessages()));
        });


        new CountDownTimer(2000, 2000) {
            public void onTick(long l) {

            }

            public void onFinish() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    RefreshView();
                }
                start();
            }
        }.start();

        try {
            DiscardItemSerial = getIntent().getBooleanExtra("DiscardItemSerial", false);
        } catch (Exception ex) {

        }


    }


    @Override
    public void notifyListener(RFIDDevice device, Tag_Model tag_model) {
        OutPutTags(tag_model);

    }

    @Override
    public void notifyStartAntenna(int ant) {
        AntennaLog = "Reading From Antenna: " + ant;
    }

    @Override
    public void notifyStopAntenna(int ant) {
        AntennaLog = "Stpped Reading From Antenna: " + ant;

    }

    @Override
    public void notifyStartDevice(String message) {

    }

    @Override
    public void notifyEndDevice(String message) {

    }


    String lblRfidHeader="";


    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("ResourceAsColor")
    @Override
    public void initAll() {
        try {




            mStorage = new Storage(this);//sp存储
            IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
            UserID = mStorage.getDataInt("UserID", -1);

            initPermission();
            itemSerialRfidPrefix = mStorage.getDataString("ItemSerialRfidPrefix", "BBB");
            binRfidPrefix = mStorage.getDataString("BinRfidPrefix", "BBB020,BBB20");
            lblRfidHeader="Rfid (" + itemSerialRfidPrefix + ") Bin (" + binRfidPrefix + ")";
            itemPrefixList.addAll(Arrays.asList(itemSerialRfidPrefix.split(",")));
            binPrefixList.addAll(Arrays.asList(binRfidPrefix.split(",")));


            new CountDownTimer(500, 500) {
                public void onTick(long l) {

                }

                public void onFinish() {
                    initRFID();
                }
            }.start();




            btnStart.setOnClickListener(view -> {
                error = "";
              resetAudit();
              resetLocation();
              removeUPcs=false;

                if (btnStart.getText().equals(START)) {


                    btnStart.setText(PAUSE);
                    if (!checkingBoxNumber && (ValidatedBoxNumber == null || ValidatedBoxNumber.isEmpty())) {
                        checkingBoxNumber = true;
                        validateBoxNumber();
                    }
                    ReadMultiRFIDV2();
                } else {

                    btnStart.setText(START);
                    checkingBoxNumber = false;
                    PauseRFID();
                }

            });


            btnFoundedRFID.setOnClickListener(view -> {
                if (auditRFIDModel == null)
                    return;
                if (btnStart.getText().equals(START)) {
                    showMessage("RFID List", GetRFIDList(true));

                } else {
                    return;
                }
            });

            btnExpectedRFID.setOnClickListener(view -> {
                if (auditRFIDModel == null)
                    return;
                if (btnStart.getText().equals(START)) {

                    showMessage("Expected", GetExpected());
                } else {
                    return;
                }
            });
            btnAddedRFID.setOnClickListener(view -> {
                if (auditRFIDModel == null)
                    return;
                if (btnStart.getText().equals(START)) {
                    GetUPCByRFID(GetAdded(), this.getSupportFragmentManager());
                } else {
                    return;
                }
            });
            btnMatchedRFID.setOnClickListener(view -> {
                if (auditRFIDModel == null)
                    return;
                if (btnStart.getText().equals(START)) {

                    showMessage("Matched", GetMatched());
                } else {
                    return;
                }
            });
            btnMissingRFID.setOnClickListener(view -> {
                if (auditRFIDModel == null)
                    return;
                if (btnStart.getText().equals(START)) {
                    showMessage("Missing", GetMissing(true));
                } else {
                    return;
                }
            });

            btnDone.setOnClickListener(view -> {

                if (btnStart.getText().equals(PAUSE)) {

                    showMessage("Info","Stop reading first");
                    return;
                }
                checkingBoxNumber = false;
                btnStart.setText(START);
                PauseRFID();

                if (auditRFIDModel == null){
                    failAudit("Failed", "Box Not Loaded Yet");
                    return;
                }



                ArrayList<AuditRFIDModelPostItems> Items = new ArrayList<>();

                ArrayList<String> Missing = GetMissing();
                ArrayList<String> RFIDItems = GetRFIDList();
                ArrayList<String> Added = GetAdded();
                ArrayList<String> Expected = GetExpected();
                ArrayList<String> Matched = GetMatched();
                List<RfidItems> rfidItems = new ArrayList<RfidItems>();

                if (auditRFIDModel == null) {
                    failAudit("Failed", "Button Done : API Call: RFID Audit Model is Null");
                    return;
                }

                if (auditRFIDModel.getRfidItems() == null) {
                    failAudit("Failed", "Button Done : API Call : RFID Items Is Null ");
                    return;
                }

                if (auditRFIDModel.getUpcItems() == null) {
                    failAudit("Failed", "Button Done : API Call : UPC Items Is Null ");
                    return;
                }
                if (auditRFIDModel.getAuditID() <= 0) {
                    failAudit("Failed", "Button Done : API Call : Audit Id = 0 ");
                    return;
                }

                if (auditRFIDModel.getBoxRFID() == null) {
                    failAudit("Failed", "Button Done : API Call : BoxRFID is NULL ");
                    return;
                }
                if (auditRFIDModel.getBoxBarcode() == null) {
                    failAudit("Failed", "Button Done : API Call : Audit BoxBarcode = 0 ");
                    return;
                }
                try {
                    rfidItems = auditRFIDModel.getRfidItems();
                } catch (Exception e) {
                    failAudit("Failed", e.getMessage());
                    log(e.getMessage());
                }


                for (String str : Matched) {
                    RfidItems Item = rfidItems.stream().filter(r -> r.getRfid().equals(str.split(":")[1])).findFirst().get();
                    Items.add(new AuditRFIDModelPostItems(Item.getId(), Item.getLotBondId(), Item.getBarcode(), Item.getRfid(), 2, "Matched"));
                }

                for (String rfid : Added) {
                    Items.add(new AuditRFIDModelPostItems(-1, -1, "", rfid, 3, "Added"));
                }

                for (String UPC : Missing) {
                    Items.add(new AuditRFIDModelPostItems(-1, -1, UPC, "", 4, "Missed"));
                }
                for (String rfid : RFIDItems) {
                    Items.add(new AuditRFIDModelPostItems(-1, -1, "", rfid, 5, "RFIDList"));
                }
                for (String UPC : Expected) {
                    Items.add(new AuditRFIDModelPostItems(-1, -1, UPC, "", 6, "Expected"));
                }

                PostRFID(Items);
                RestartScreen();

            });
        } catch (Exception ex) {
            failAudit("Failed", ex.getMessage());
        }

    }





    String auditFullMessage = "";
    String locationFullMessage = "";

    private ArrayList<String> getPopUpMessages() {
        ArrayList<String> messages = new ArrayList<>();
        messages.add("Device");
        messages.add("----------");
        messages.add(connectedDevice);
        messages.add(AntennaLog);
        messages.add("\nRfids");
        messages.add("----------");
        messages.add("Rfid Bin Prefix"+binPrefixList);
        messages.add("Item Prefix"+itemSerialRfidPrefix);
        if (hmList != null ) {
            messages.add("Rfid Count "+hmList.size());
        }

        if (error != null && error != "") {
            messages.add("\nError");
            messages.add("----------");
            messages.add(error);
        }

        return messages;
    }

    private ArrayList<String> getBoxBarcodeMessages() {
        ArrayList<String> messages = new ArrayList<>();
        messages.add("BoxBracodes");
        messages.add("------------");
        messages.add(boxList);
        messages.add("\n\n");
        messages.add(boxBarcodeFullMessage);
        return messages;
    }

    private ArrayList<String> getAuditMessages() {
        ArrayList<String> messages = new ArrayList<>();
        messages.add(auditFullMessage);
        return messages;
    }

    private ArrayList<String> getLocationMessages() {
        ArrayList<String> messages = new ArrayList<>();
        messages.add(locationFullMessage);
        return messages;
    }

        private void failAudit (String message, String fullMessage){
            error = auditFullMessage = fullMessage;
            runOnUiThread(() -> {
                btnAuditResult.setText(truncate(message, 40));
                btnAuditResult.setBackgroundResource(R.drawable.rounded_corner_red);
            });
        }

        private void successAudit (String message, String fullMessage){
            auditFullMessage = fullMessage;
            runOnUiThread(() -> {
                btnAuditResult.setText(truncate(message, 40));
                btnAuditResult.setBackgroundResource(R.drawable.rounded_corner_green);
            });
        }

    private void resetBoxBarcode () {
        boxBarcodeFullMessage = "";
        runOnUiThread(() -> {
            btnBoxBarcode.setText("BoxBarcode");
            btnBoxBarcode.setBackgroundResource(R.drawable.rounded_corner1);
        });
    }

        private void resetAudit () {
            auditFullMessage = "";
            runOnUiThread(() -> {
                btnAuditResult.setText("AUDIT RESULT");
                btnAuditResult.setBackgroundResource(R.drawable.rounded_corner1);
            });
        }


        private void resetLocation () {
            locationFullMessage = "";
            runOnUiThread(() -> {
                btnLocation.setText("Location");
                btnLocation.setBackgroundResource(R.drawable.rounded_corner1);
            });
        }

        private void failLocation (String message, String fullMessage){
            locationFullMessage = fullMessage;
            error += "\n" + locationFullMessage;
            runOnUiThread(() -> {
                btnLocation.setText(truncate(message, 40));
                btnLocation.setBackgroundResource(R.drawable.rounded_corner_red);
            });
        }



    private void successLocation (String message, String fullMessage){
            locationFullMessage = fullMessage;
            runOnUiThread(() -> {
                btnLocation.setText(truncate(message, 40));
                btnLocation.setBackgroundResource(R.drawable.rounded_corner_green);
            });
        }


        @SuppressLint("ResourceAsColor")
        @RequiresApi(api = Build.VERSION_CODES.M)
        private void InitRFIDApi (String BoxBarcode, String BoxRFID){
            try {
                synchronized (apiDone_Lock) {
                    apiDone = false;
                }

                int UserID = mStorage.getDataInt("UserID");
                BasicApi api = APIClient.INSTANCE.getInstance(IPAddress, false).create(BasicApi.class);
                Call<AuditRFIDModel> call = api.InitNonBBBAudit(UserID, BoxBarcode, BoxRFID);
                call.enqueue(new Callback<AuditRFIDModel>() {
                    @Override
                    public void onResponse(Call<AuditRFIDModel> call, Response<AuditRFIDModel> response) {

                        synchronized (apiDone_Lock) {
                            apiDone = true;
                        }
                        try {
                            auditRFIDModel = response.body();

                            if (auditRFIDModel == null) {
                                return;
                            }

                            if (auditRFIDModel.getRfidItems() == null) {
                                return;
                            }

                            if (auditRFIDModel.getUpcItems() == null) {
                                return;
                            }
                            if (auditRFIDModel.getAuditID() <= 0) {
                                return;
                            }

                            if (auditRFIDModel.getBoxRFID() == null) {
                                return;
                            }
                            if (auditRFIDModel.getBoxBarcode() == null) {
                                return;
                            }

                            if (auditRFIDModel != null && auditRFIDModel.getRfidItems() != null && auditRFIDModel.getUpcItems() != null) {
                                if (auditRFIDModel != null && auditRFIDModel.getAuditID() != -1) {
                                    checkingBoxNumber = false;
                                    ProceedSuccessScan(auditRFIDModel.getAuditID(), auditRFIDModel.getBoxBarcode(), auditRFIDModel.getBoxRFID());

                                } else {
                                    String msg = auditRFIDModel == null ? "Invalid Box Barcode" : auditRFIDModel.getBoxBarcode();
                                    updateBoxBarcode("Invalid Box Barcode", msg, -1);
                                    log(msg);
                                }
                            } else {

                                String msg = "API Returned Null For BoxBarcode: " + BoxBarcode + " BoxRFID: " + BoxRFID;
                                updateBoxBarcode("Invalid Box Barcode", msg, -1);
                                log(msg);
                            }

                        } catch (Exception ex) {
                            updateBoxBarcode("Invalid Box Barcode", ex.getMessage(), -1);
                            log(ex.getMessage());
                        }


                    }

                    @Override
                    public void onFailure(Call<AuditRFIDModel> call, Throwable t) {

                        try {
                            updateBoxBarcode("Invalid Box Barcode", t.getMessage(), -1);
                            log(t.getMessage());
                        } catch (Exception ex) {
                            log(ex.getMessage());
                            updateBoxBarcode("Invalid Box Barcode", t.getMessage(), -1);
                        }
                    }
                });


            } catch (Exception ex) {
                updateBoxBarcode("Invalid Box Barcode", ex.getMessage(), -1);
                log(ex.getMessage());


            } finally {


            }
        }


        @SuppressLint("ResourceAsColor")
        @RequiresApi(api = Build.VERSION_CODES.M)
        private void GetUPCByRFID (ArrayList < String > Items, FragmentManager manager){
            try {
                ArrayList<String> data = new ArrayList<>();
                for (String rfid : Items) {
                    if (detectedLotBondedItems.containsKey(rfid)) {
                        data.add(">> "+detectedLotBondedItems.get(rfid) + "\n" + rfid);
                    } else {
                        data.add(">> "+"No Serial \n" + rfid);
                    }
                }
               showMessage("Added Items", data);
            } catch (Exception ex) {
                Log.i("AH-Log-Added", "Failed Exception");
                Log.i("AH-Log-Added", ex.getMessage());
                error(ex.getMessage());
                log(ex.getMessage());
                // throw(ex);
            } finally {

            }
        }

        public ArrayList<String> GetAdded () {
            ArrayList<String> Data = new ArrayList<>();
            try {
                List<RfidItems> rfidItems = auditRFIDModel.getRfidItems();
                List<String> Keys = hmList.keySet().stream().filter(x -> validItemRfid(x)).collect(Collectors.toList());
                for (String rfid : Keys) {
                    if (rfid.equals(ValidatedBoxRFID))
                        continue;
                    if (rfidItems.stream().anyMatch(r -> r.getRfid().equals(rfid))) {
                    } else {
                        Data.add(rfid);
                    }
                }

            } catch (Exception e) {
                log(e.getMessage());
                error(e.getMessage());
                return null;
            }

            return Data;
        }

        public ArrayList<String> GetExpected () {
            ArrayList<String> Data = new ArrayList<>();
            try {
                List<UpcItems> upcItems = auditRFIDModel.getUpcItems();
                for (UpcItems item : upcItems) {
                    Data.add((item.getBarcode()));
                }
            } catch (Exception e) {
                log(e.getMessage());
                error(e.getMessage());
                return null;
            }
            return Data;
        }

        public ArrayList<String> GetMatched () {

            ArrayList<String> Data = new ArrayList<>();
            try {
                List<RfidItems> rfidItems = auditRFIDModel.getRfidItems();
                List<String> Keys = hmList.keySet().stream().filter(x -> validItemRfid(x)).collect(Collectors.toList());

                for (String rfid : Keys) {
                    if (rfid.equals(ValidatedBoxRFID))
                        continue;
                    if (rfidItems.stream().anyMatch(r -> r.getRfid().equals(rfid))) {
                        RfidItems Item = rfidItems.stream().filter(r -> r.getRfid().equals(rfid)).findFirst().get();
                        Data.add(Item.getBarcode() + ":" + Item.getRfid());
                    }

                }


            } catch (Exception e) {
                log(e.getMessage());
                error(e.getMessage());
                return null;
            }

            return Data;
        }

        public ArrayList<String> GetMissing () {
            ArrayList<String> Data = new ArrayList<>();
            try {
                List<UpcItems> upcItems = auditRFIDModel.getUpcItems();
                List<RfidItems> rfidItems = auditRFIDModel.getRfidItems();
                List<String> Keys = hmList.keySet().stream().filter(x -> validItemRfid(x)).collect(Collectors.toList());

                ArrayList<String> ExpectedUPC = new ArrayList<>();
                ArrayList<String> MatchedUPC = new ArrayList<>();
                for (UpcItems item : upcItems) {
                    ExpectedUPC.add((item.getBarcode()));
                }

                for (String rfid : Keys) {
                    if (rfidItems.stream().anyMatch(r -> r.getRfid().equals(rfid))) {
                        RfidItems Item = rfidItems.stream().filter(r -> r.getRfid().equals(rfid)).findFirst().get();
                        MatchedUPC.add(Item.getBarcode());
                    }
                }
                for (String UPC : MatchedUPC) {
                    ExpectedUPC.remove(UPC);
                }
                ArrayList<String> MissingUPC = ExpectedUPC;
                Data.addAll(MissingUPC);
            } catch (Exception ex) {
                error(ex.getMessage());
                log(ex.getMessage());
                return null;
            }

            return Data;
        }

        public ArrayList<String> GetMissing ( boolean withIS){
            ArrayList<String> Data = new ArrayList<>();
            try {
                //List<UpcItems> upcItems= auditRFIDModel.getUpcItems();
                List<RfidItems> rfidItems = auditRFIDModel.getRfidItems();
                List<String> Keys = hmList.keySet().stream().filter(x -> validItemRfid(x)).collect(Collectors.toList());

                ArrayList<String> ExpectedUPC = new ArrayList<>();
                ArrayList<String> MatchedUPC = new ArrayList<>();
                for (RfidItems item : rfidItems) {
                    ExpectedUPC.add(">> "+item.getBarcode() + (withIS ? "\n" + item.getRfid() : ""));
                }

                for (String rfid : Keys) {
                    if (rfidItems.stream().anyMatch(r -> r.getRfid().equals(rfid))) {
                        RfidItems Item = rfidItems.stream().filter(r -> r.getRfid().equals(rfid)).findFirst().get();
                        MatchedUPC.add(">> "+Item.getBarcode() + (withIS ? "\n" + Item.getRfid() : ""));
                    }
                }
                for (String UPC : MatchedUPC) {
                    ExpectedUPC.remove(UPC);
                }
                ArrayList<String> MissingUPC = ExpectedUPC;
                Data.addAll(MissingUPC);
            } catch (Exception ex) {
                error(ex.getMessage());
                log(ex.getMessage());
                return null;
            }

            return Data;
        }

        public ArrayList<String> GetRFIDList () {
            ArrayList<String> Data = new ArrayList<>();
            List<String> Keys = hmList.keySet().stream().filter(x -> validItemRfid(x)).collect(Collectors.toList());
            Data.addAll(Keys);
            return Data;
        }

        public ArrayList<String> GetRFIDList ( boolean withIS){
            ArrayList<String> Data = new ArrayList<>();
            List<String> Keys = hmList.keySet().stream().filter(x -> validItemRfid(x)).collect(Collectors.toList());
            if (withIS) {
                for (String rfid : Keys) {
                    if (detectedLotBondedItems.containsKey(rfid)) {
                        Data.add(detectedLotBondedItems.get(rfid) + "-" + rfid);
                    } else {
                        Data.add("No Serial -" + rfid);
                    }
                }
            } else Data.addAll(Keys);
            return Data;
        }

        private boolean ContinueRead = true;
        private boolean removeUPcs = false;


        //收藏窗口
        @SuppressLint("ResourceAsColor")
        @RequiresApi(api = Build.VERSION_CODES.M)
        private void ProceedRFIDToAPI (ArrayList < AuditRFIDModelPostItems > Items) {

            try {
                AuditRFIDModelPost model =
                        new AuditRFIDModelPost(ValidatedAuditID, ValidatedBoxNumber, auditRFIDModel.getBoxRFID(), mStorage.getDataString("StationCode", ""), UserID, Items, true);


                mStorage = new Storage(this);
                //   String IPAddressWarehouseManager = mStorage.getDataString("IPAddressWarehouseManager", "192.168.10.82");
                BasicApi api = APIClient.getInstanceStatic(IPAddress, true).create(BasicApi.class);
                CompositeDisposable compositeDisposable = new CompositeDisposable();
                compositeDisposable.addAll(
                        api.PostNonBBBAudit(model)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe((s) -> {
                                    if (s != null) {
                                        if (s.getAuditResult().toLowerCase().contains("box not released")) {
                                            playError();
                                            failAudit("Box Not Released", s.getAuditResult());
                                            failLocation("No Location", "Audit Failed");
                                            Logger.Debug("API", "Rfid Audit : response " + s.getAuditResult());
                                        } else {
                                            playSuccess();
                                            Logger.Debug(" s.getRemoveUPCs", "Rfid Audit :  s.getRemoveUPCs " + s.getRemoveUPCs());
                                            successAudit("Box Released", s.getAuditResult());
                                            if(s.getLocation().equals("") || s.getLocation().equals("No Location"))
                                                failLocation(truncate(s.getLocation(),50),s.getResult());

                                            else if(s.getLocation().equals("W1005") && s.getRemoveUPCs()){
                                                removeUPcs=true;
                                                    failLocation("Remove UPCs","Remove Extra UPCs");

                                            }
                                            else
                                                successLocation(s.getLocation(), s.getResult());
                                            Logger.Debug("API", "Rfid Audit : response " + s);
                                        }

                                    }
                                }, (throwable) -> {

                                    if (throwable instanceof HttpException) {
                                        HttpException ex = (HttpException) throwable;
                                        String response = ex.response().errorBody().string();
                                        if (response.isEmpty()) {
                                            response = throwable.getMessage();
                                        }
                                        failAudit("Failed", "Error: " + response + " (API Error)");
                                        failLocation("No Location", "Audit Failed");
                                        //showMessage(response);
                                        Logger.Debug("API", "Error - Returned HTTP Error " + response);
                                    } else {
                                        failAudit("Failed", " Error : " + throwable.getMessage() + " (API Error)");
                                        failLocation("No Location", "Audit Failed");
                                        Logger.Error("API", " Error In API Response: " + throwable.getMessage());
                                        //showMessage(throwable.getMessage());
                                    }
                                    playError();


                                }));
            } catch (Throwable e) {
                failAudit("Failed", "Error: " + e.getMessage() + " (Exception)");
                failLocation("No Location", "Audit Failed");
                playError();
                Logger.Error("API", "Audit Exception: " + e.getMessage());
                //showMessage(e.getMessage());

            }


        }


        @RequiresApi(api = Build.VERSION_CODES.M)
        public void PostRFID (ArrayList < AuditRFIDModelPostItems > Items) {
            try {

                ProceedRFIDToAPI(Items);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    RefreshView();
                }

                General1.getGeneral(this).InRFID = false;
            } catch (Exception ex) {
                log(ex.getMessage());
            }
        }


        String ValidatedBoxNumber = "";
        String ValidatedBoxRFID = "";
        Integer ValidatedAuditID = 0;

        @SuppressLint("ResourceAsColor")
        public void ProceedSuccessScan (Integer AuditID, String BoxNumber, String BoxRFID){
            Log.i("ProceedSuccessScan", "ProceedSuccessScan " + BoxNumber);
            ValidatedBoxNumber = BoxNumber;
            ValidatedBoxRFID = BoxRFID;
            ValidatedAuditID = AuditID;
            IsRFIDConnected = true;
            runOnUiThread(() -> {
                btnStart.setText(PAUSE);
                btnBoxBarcode.setText(ValidatedBoxNumber);
            });
        }

        AuditRFIDModel auditRFIDModel = null;


        public String TagModelKey (Tag_Model model){
            if (model._EPC.equals("E28068940000400C86079169")) {
                return "30342CBD280E70D0A43701F3-E280689020004006CF0064AE";
            }
            if (model._EPC != null && model._TID != null && !model._EPC.isEmpty() && !model._TID.isEmpty())
                return model._EPC + "-" + model._TID;
            return "";
        }

        Integer totalReadCount = 0;

        public String EPCToUPC (String EPC){
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


        public String formatNumber(int x){
            return  ""+x;
        }
        @RequiresApi(api = Build.VERSION_CODES.O)
        private void RefreshView () {
            runOnUiThread(() -> {

                try {
                    if (auditRFIDModel != null) {
                        int Count;
                        int Expected;
                        int Matched;
                        int Missed;
                        int Added;
                        synchronized (hmList_Lock) {

                            ArrayList<String> list = GetRFIDList();
                            Count = list == null ? 0 : list.size() ;
                            list = GetExpected();
                            Expected = list == null ? 0 : list.size() ;
                            list = GetMatched();
                            Matched = list == null ? 0 : list.size() ;
                            list = GetMissing();
                            Missed = list == null ? 0 : list.size() ;
                            list = GetAdded();
                            Added = list == null ? 0 : list.size() ;
                        }



                        btnFoundedRFID.setText(formatNumber( Count));
                        btnMatchedRFID.setText(formatNumber( Matched));
                        btnExpectedRFID.setText(formatNumber( Expected));
                        btnMissingRFID.setText(formatNumber( Missed));
                        btnAddedRFID.setText(formatNumber( Added));

                        RFIDUPCView.add(0, "RFID Count:" + Count);
                        RFIDUPCView.add(0, "Matched:" + Matched);
                        RFIDUPCView.add(0, "Expected:" + Expected);
                        RFIDUPCView.add(0, "Missed:" + Missed);
                        RFIDUPCView.add(0, "Added:" + Added);
                    }

                } catch (Exception ex) {
                    error =" Error E1: " +ex.getMessage();
                    log(" Error E1 : " + ex.getMessage());
                }

            });

        }


        private void initPermission () {
            PermissionUtil.requestEach(ReplenishmentAuditActivity.this, new PermissionUtil.OnPermissionListener() {


                @Override
                public void onFailed(boolean showAgain) {

                }

                @Override
                public void onSucceed() {
                    //授权成功后打开蓝牙
                    log("申请成功");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            if (Analysis.isOpenGPS(ReplenishmentAuditActivity.this)) {

                            } else
                                startLocation();
                        }
                    }, 1000);

                }
            }, PermissionUtil.LOCATION);
        }

        private Object beep_Lock = new Object();

        private void startLocation () {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
            builder.setTitle("hint")
                    .setMessage("Please go to open the location permission of the phone!")
                    .setCancelable(false)
                    .setPositiveButton("determine", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent, 10);
                        }
                    }).show();
        }

        @Override
        protected void onPause () {
            super.onPause();
            PauseRFID();
        }

        private void PauseRFID () {
            ContinueRead = false;
            checkingBoxNumber = false;
            if (RFIDDevicesManager.getSingleAntennaReader() != null)
                RFIDDevicesManager.getSingleAntennaReader().stop();

        }

        @Override
        protected void onResume () {
            super.onResume();
        }


        private String error = "";
        private String connectedDevice = "";

        @ViewById(R.id.btnAuditResult)
        private Button btnAuditResult;


        @ViewById(R.id.btnPopUp)
        private Button btnPopUp;
        @ViewById(R.id.btnBoxBarcode)
        private Button btnBoxBarcode;
        @ViewById(R.id.btnLocation)
        private Button btnLocation;




        @ViewById(R.id.btnStart)
        private Button btnStart;

        @ViewById(R.id.btnStop)
        private Button btnDone;

        @ViewById(R.id.btnExpectedRFID)
        private Button btnExpectedRFID;

        @ViewById(R.id.btnMatchedRFID)
        private Button btnMatchedRFID;

        @ViewById(R.id.btnMissingRFID)
        private Button btnMissingRFID;

        @ViewById(R.id.btnAddedRFID)
        private Button btnAddedRFID;

        @ViewById(R.id.btnFoundedRFID)
        private Button btnFoundedRFID;

        @ViewById(R.id.textTableRfid)
        private TextView txtTableRfid;

        @ViewById(R.id.textTableAnt)
        private TextView txtTableAnt;

        @ViewById(R.id.textTableCounter)
        private TextView txtTableCounter;


        private Storage mStorage;

        private List<DeviceModule> mModuleArray = new ArrayList<>();
        private List<DeviceModule> mFilterModuleArray = new ArrayList<>();

        private int mStartDebug = 1;
        private String IPAddress = "";
        private int UserID = -1;


        @Override
        protected void onDestroy () {
            ContinueRead = false;
            checkingBoxNumber = false;
            IsRFIDConnected = false;
            PauseRFID();
            super.onDestroy();
        }

        private Boolean IsReading () {
            return btnStart.getText().equals(PAUSE);
        }

        String PAUSE = "PAUSE";
        String START = "START";


        private void RestartScreen () {
            PauseRFID();
            error = "";
            removeUPcs=false;
            resetLocation();
            resetAudit();
            ValidatedBoxNumber = "";
            ValidatedBoxRFID = "";
            ValidatedAuditID = 0;
            auditRFIDModel = null;
            PauseRFID();
            boxBarcodeFullMessage="";

            btnFoundedRFID.setText("000");
            btnMatchedRFID.setText("000");
            btnExpectedRFID.setText("000");
            btnMissingRFID.setText("000");
            btnAddedRFID.setText("000");

            RFIDMessages.clear();
            RFIDUPCView.clear();
            RFIDUPC.clear();
            hmList.clear();
            boxMap.clear();
            detectedLotBondedItems.clear();
        }

        private void ReadMultiRFIDV2 () {
            if (!IsRFIDConnected)
                return;
            ContinueRead = true;
            if (!IsReading())
                return;

            try {
                read();

            } catch (Exception ex) {
                LogError("ReadRFID Exception:" + ex.getMessage());
            } finally {
            }
        }


        private void LogError (String str){
            Logging.LogError(getApplicationContext(), str);
        }

        private void LogDebug (String str){
            Logging.LogDebug(getApplicationContext(), str);
        }


        private ArrayList<String> RFIDMessages = new ArrayList();

        @SuppressLint("ResourceAsColor")
        private void initRFID () {
            connectedDevice="";
            try {
                String Connection = "";
                RFIDDevicesManager.setOutput(new RFIDOutput(this));
                antList = RFIDDevicesManager.getSingleAntennaReader().getAntennaListInt();
                int RFIDCount = RFIDReader.HP_CONNECT.size();
                antList = RFIDDevicesManager.getSingleAntennaReader().getAntennaListInt();
                connectedDevice = "Connected to: " + RFIDDevicesManager.getSingleAntennaReader().getIPAddress();
                HashMap<String, BaseConnect> lst = RFIDReader.HP_CONNECT;
                if (lst.keySet().stream().count() > 0) {
                    IsRFIDConnected = true;
                } else {

                }
                connectedDevice += "\nConnection: " + (IsRFIDConnected?"success":"failed");
                //Test
                //IsRFIDConnected=true;
            } catch (Exception ex) {
                connectedDevice+= "\n"+ ex.getMessage();
            }


        }

        boolean IsRFIDConnected = false;

        private HashMap<String, Tag_Model> hmList = new HashMap<String, Tag_Model>();
        private Object hmList_Lock = new Object();


        public static final int FRAGMENT_STATE_DATA = 0x06;
        public static final int FRAGMENT_STATE_SERVICE_VELOCITY = 0x13;
        public static final int FRAGMENT_STATE_NUMBER = 0x07;
        public static final int FRAGMENT_STATE_CONNECT_STATE = 0x08;
        public static final int FRAGMENT_STATE_SEND_SEND_TITLE = 0x09;
        public static final int FRAGMENT_STATE_LOG_MESSAGE = 0x011;
        ArrayList<String> RFIDUPC = new ArrayList<>();
        ArrayList<String> RFIDUPCView = new ArrayList<>();


        @Override
        public void WriteDebugMsg (String s){
            // TODO Auto-generated method stub

        }

        @Override
        public void WriteLog (String s){
            // TODO Auto-generated method stub

        }

        @Override
        public void PortConnecting (String s){
            // TODO Auto-generated method stub

        }

        @Override
        public void PortClosing (String s){
            // TODO Auto-generated method stub

        }

        @Override
        public void OutPutTagsOver () {
            // TODO Auto-generated method stub

        }

        @Override
        public void GPIControlMsg ( int i, int j, int k){
            // TODO Auto-generated method stub

        }

        @Override
        public void OutPutScanData ( byte[] scandata){
            // TODO Auto-generated method stub
        }


    private void showMessage(String title, String msg) {

        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(msg)
                    .setPositiveButton("OK", (dialog, which) -> {
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    // .setCancelable(false)
                    .show();

        });
    }


    private void showMessage(String title, ArrayList<String> msgs) {

        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(msgs==null?"":String.join("\n---------------------------------\n",msgs))
                    .setPositiveButton("OK", (dialog, which) -> {
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    // .setCancelable(false)
                    .show();

        });
    }




}