package com.hc.mixthebluetooth.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentManager;

import com.hc.basiclibrary.ioc.ViewById;
import com.hc.basiclibrary.permission.PermissionUtil;
import com.hc.basiclibrary.titleBasic.DefaultNavigationBar;
import com.hc.basiclibrary.viewBasic.BasActivity;
import com.hc.bluetoothlibrary.DeviceModule;
import com.hc.mixthebluetooth.Logger;
import com.hc.mixthebluetooth.Model.RFIDAuditInfoReceivedModel;
import com.hc.mixthebluetooth.R;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDDevice;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDDevicesManager;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDListener;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDOutput;
import com.hc.mixthebluetooth.activity.tool.Analysis;
import com.hc.mixthebluetooth.customView.PopWindowMain;
import com.hc.mixthebluetooth.storage.Storage;
import com.rfidread.Connect.BaseConnect;
import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.Models.Tag_Model;
import com.rfidread.RFIDReader;
import com.util.General1;
import com.util.Logging;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import Model.AuditRFIDModel;
import Model.AuditRFIDModelPost;
import Model.AuditRFIDModelPostItems;
import Model.PostListItems;
import Model.RFIDAuditInfoModel;
import Model.RfidAuditResponse;
import Model.RfidItems;
import Model.UpcItems;
import Remote.APIClient;

import com.hc.mixthebluetooth.Remote.Routes.BasicApi;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;

public class BBBAuditAllAntennasActivity extends BasActivity implements IAsynchronousMessage, RFIDListener {

    private HashMap<String, Tag_Model> boxMap = new HashMap<String, Tag_Model>();
    private Object boxMap_Lock = new Object();
    private Object boxListLock = new Object();
    private List<String> nonValidRfids = new ArrayList<>();
    private boolean checkingBoxNumber = false;
    private Object apiDone_Lock = new Object();
    private boolean apiDone;
    ArrayList<Integer> antList;
    String validatedBoxNumber1="";
    public static boolean isReplenishment=false;

    private String itemSerialRfidPrefix = "BBB";
    private String binRfidPrefix = "BBB020,BBB20";
    private List<String> itemPrefixList = new ArrayList<>();
    private List<String> binPrefixList = new ArrayList<>();

    private boolean DiscardItemSerial = false;

    public boolean validItemRfid(String s) {
        if(s == null)
            return false;
        if(DiscardItemSerial){
            return true;
        }else {
            return itemPrefixList.stream().anyMatch(i->s.toLowerCase().startsWith(i.toLowerCase()));
        }
    }

    public boolean validBinRfid(String s) {
        return s != null && binPrefixList.stream().anyMatch(i -> s.toLowerCase().startsWith(i.toLowerCase()));
    }


    public void read() {
        txtReadingFromANt.setText("Reading");
        int rs = RFIDDevicesManager.readEPCSingleAntennaAll();
        // Log.i("AH-Log-time", "Reading from Antenna " +RFIDDevicesManager.getSingleAntennaReader().getAntennaNo());
        Log.i("AH-Log-time", "Reading from Antenna result " + rs);
        switch (rs) {
            case 1:
                txtReadingFromANt.setText("Reading Error: Antenna port Parameter err ");
                break;
            case 2:
                txtReadingFromANt.setText("Reading Error: Choosing tag Parameter error ");
                break;
            case 3:
                txtReadingFromANt.setText("Reading Error: TID parameter error ");
                break;
            case 4:
                txtReadingFromANt.setText("Reading Error: user data area parameter error ");
                break;
            case 5:
                txtReadingFromANt.setText("Reading Error: reserved area parameter error ");
                break;
            case 6:
                txtReadingFromANt.setText("Reading Error: Other parameter error ");
                break;
        }


    }


    public void validateBoxNumber() {
        new Thread(() -> {
            List<Tag_Model> boxRfids;
            HashMap<String, String> boxMapResults = new HashMap<>();
            updateTextValidateBox("", ColorGreen);
            updateTextBoxList(boxMapResults);
            Log.i("AH-Log-Box", "starting validation process");
            int count = 0;
            while (checkingBoxNumber) {
                count++;
                Log.i("AH-Log-Box", "==================> validation process cycle <" + count + ">  <=============================");

                boxRfids = new ArrayList<>();
                Log.i("AH-Log-Box", "requesting lock");
                synchronized (boxMap_Lock) {
                    Log.i("AH-Log-Box", "lock acquired");
                    boxRfids.addAll(boxMap.values());
                    Log.i("AH-Log-Box", "BoxMap values:");
                    for (Tag_Model m : boxMap.values())
                        Log.i("AH-Log-Box", m._EPC);
                }
                Log.i("AH-Log-Box", "================");
                Log.i("AH-Log-Box", "nonValidRfids values:");
                for (String m : nonValidRfids)
                    Log.i("AH-Log-Box", m);

                Log.i("AH-Log-Box", "=============================");
                for (Tag_Model model : boxRfids) {
                    Log.i("AH-Log-Box", "For Loop : " + model._EPC);
                    if (!nonValidRfids.contains(model._EPC) && ValidatedBoxNumber == null || ValidatedBoxNumber.isEmpty()) {
                        Log.i("AH-Log-Box", "Inside If : " + model._EPC);
                        String EPC = model._EPC.startsWith("30396062C39ADD4000231D87") ? "BBB0200700003917AC011038" : model._EPC;
                        String BarcodeFromEPC = EPCToUPC(EPC);
                        if (validBinRfid(EPC)) {
                            synchronized (boxListLock) {
                                if (!boxMapResults.containsKey(EPC))
                                    boxMapResults.put(EPC, ": <<<< Checking >>>>");
                                updateTextBoxList(boxMapResults);
                            }

                            Log.i("AH-Log-Box", "Initialize RFID API For : " + model._EPC);
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


                            Log.i("AH-Log-Box", "ValidatedBoxNumber : " + ValidatedBoxNumber);
                            if (ValidatedBoxNumber == null || ValidatedBoxNumber == "" || ValidatedBoxNumber.isEmpty() || ValidatedBoxNumber.length() < 5) {
                                nonValidRfids.add(model._EPC);
                                boxMapResults.put(EPC, ": Not Valid ");
                                updateTextBoxList(boxMapResults);
                            } else {
                                checkingBoxNumber = false;
                                Log.i("AH-Log-Box", "Returning ... ValidatedBoxNumber : " + ValidatedBoxNumber);
                                updateTextValidateBox("ValidatedBoxNumber : " + ValidatedBoxNumber, ColorGreen);
                                validatedBoxNumber1 = ValidatedBoxNumber;
                                boxMapResults.put(EPC, ": Valid ");
                                updateTextBoxList(boxMapResults);
                                return;
                            }

                            Log.i("AH-Log-Box", "nonValidRfids values:");
                            for (String m : nonValidRfids)
                                Log.i("AH-Log-Box", m);

                            Log.i("AH-Log-Box", "===========================");


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


    private void updateErrorLabel(String message, int color) {
        runOnUiThread(() -> {
            lblError.setText(message);
            lblError.setTextColor(color);
        });

    }

    private void updateTextValidateBox(String message, int color) {
        runOnUiThread(() -> {
            TextView txt = (TextView) findViewById(R.id.textboxValidation);
            txt.setText(message);
            txt.setTextColor(color);
        });

    }

    private void updateTextBoxList(HashMap<String, String> map) {
        runOnUiThread(() -> {
            TextView txt = (TextView) findViewById(R.id.textViewBoxList);
            String message = "";
            if (map.size() > 1) {
                txt.setTextColor(ColorRed);
                message += "!!!!!!!!!!!!!!!! " + map.size() + " box numbers were found !!!!!!!!!!!!!!!!\n";
            } else {
                txt.setTextColor(Color.BLUE);
            }

            for (Map.Entry<String, String> entry : map.entrySet()) {
                message += entry.getKey() + " " + entry.getValue() + "\n";
            }

            txt.setText(message);


        });

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

                runOnUiThread(() -> {
                    txtTableRfid.setText(tmodel);
                    txtTableAnt.setText("" + model._ANT_NUM);
                    txtTableCounter.setText(++totalReadCount + "");
                });

                if (!hmList.containsKey(tmodel)) {
                    LogDebug("EPC Read11" + tmodel);
                    if (validBinRfid(tmodel)) {
                        synchronized (boxMap_Lock) {
                            if (!boxMap.containsKey(tmodel))
                                boxMap.put(tmodel, model);
                        }

                    } else
                    {
                        String rfid = TagModelKey(model);
                        /*if(!PendingItems.containsKey(rfid) && !ProcessedItems.contains(rfid)) {
                            PendingItems.put(rfid, model);
                        }*/
                        hmList.put(rfid, model);
                    }

                }

            }


        } catch (Exception ex) {
            Log.d("Debug", "Tags output exceptions:" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bbb_audit_antenna);
        //设置头部
        setTitle();
        setContext(this);

        if(!isReplenishment){
            layoutLocation.setVisibility(View.INVISIBLE);

        }


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

        try{
            DiscardItemSerial = getIntent().getBooleanExtra("DiscardItemSerial", false);
        }catch(Exception ex){

        }





    }


    @Override
    public void notifyListener(RFIDDevice device, Tag_Model tag_model) {
        OutPutTags(tag_model);

    }

    @Override
    public void notifyStartAntenna(int ant) {
        runOnUiThread(() -> {
            txtReadingFromANt.setText("Reading From Antenna: " + ant);
        });

    }

    @Override
    public void notifyStopAntenna(int ant) {
        runOnUiThread(() -> {
            txtReadingFromANt.setText("");
        });
    }

    @Override
    public void notifyStartDevice(String message) {

    }

    @Override
    public void notifyEndDevice(String message) {

    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("ResourceAsColor")
    @Override
    public void initAll() {
        try {
            RefreshSettings();

            mStorage = new Storage(this);//sp存储
            IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
            UserID = mStorage.getDataInt("UserID", -1);

            initPermission();
            initView();
            //Toast.makeText(view.getContext(),"selected: "+selectedItemSerial,Toast.LENGTH_SHORT).show();
            itemSerialRfidPrefix = mStorage.getDataString("ItemSerialRfidPrefix", "BBB");
            binRfidPrefix = mStorage.getDataString("BinRfidPrefix", "BBB020,BBB20");
            lblRfidHeader.setText("Rfid (" + itemSerialRfidPrefix + ") Bin (" + binRfidPrefix + ")");
            itemPrefixList.addAll(Arrays.asList(itemSerialRfidPrefix.split(",")));
            binPrefixList.addAll(Arrays.asList(binRfidPrefix.split(",")));


            new CountDownTimer(500, 500) {
                public void onTick(long l) {

                }

                public void onFinish() {
                    initRFID();
                }
            }.start();


            btnScanRFIDStart.setOnClickListener(view -> {
            lblError.setText("");
            lbLocation.setText("");

                if (btnScanRFIDStart.getText().equals(START)) {
                    btnScanRFIDStart.setText(PAUSE);
                    if (!checkingBoxNumber && (ValidatedBoxNumber == null || ValidatedBoxNumber.isEmpty())) {
                        checkingBoxNumber = true;
                        validateBoxNumber();
                    }

                    ReadMultiRFIDV2();
                } else {
                    btnScanRFIDStart.setText(START);
                    checkingBoxNumber = false;
                    PauseRFID();
                }

            });


            btnFoundedRFID.setOnClickListener(view -> {
                if (auditRFIDModel == null)
                    return;
                if (btnScanRFIDStart.getText().equals(START)) {
                    GetUPCByRFID(GetRFIDList(), "RFID List", this.getSupportFragmentManager());

                } else {
                    return;
                }
            });

            btnExpectedRFID.setOnClickListener(view -> {
                if (auditRFIDModel == null)
                    return;
                if (btnScanRFIDStart.getText().equals(START)) {

                    General1.ShowDialog(this.getSupportFragmentManager(), "Expected", GetExpected());
                } else {
                    return;
                }
            });
            btnAddedRFID.setOnClickListener(view -> {
                if (auditRFIDModel == null)
                    return;
                if (btnScanRFIDStart.getText().equals(START)) {
                    GetUPCByRFID(GetAdded(), "Added List", this.getSupportFragmentManager());
                } else {
                    return;
                }
            });
            btnMatchedRFID.setOnClickListener(view -> {
                if (auditRFIDModel == null)
                    return;
                if (btnScanRFIDStart.getText().equals(START)) {

                    General1.ShowDialog(this.getSupportFragmentManager(), "Matched", GetMatched());
                } else {
                    return;
                }
            });
            btnMissingRFID.setOnClickListener(view -> {
                if (auditRFIDModel == null)
                    return;
                if (btnScanRFIDStart.getText().equals(START)) {
                    General1.ShowDialog(this.getSupportFragmentManager(), "Missing", GetMissing(true));
                } else {
                    return;
                }
            });

            btnScanRFIDDone.setOnClickListener(view -> {
                checkingBoxNumber = false;
                btnScanRFIDStart.setText(START);
                PauseRFID();

                if (auditRFIDModel == null)
                    return;


                ArrayList<AuditRFIDModelPostItems> Items = new ArrayList<>();

                ArrayList<String> Missing = GetMissing();
                ArrayList<String> RFIDItems = GetRFIDList();
                ArrayList<String> Added = GetAdded();
                ArrayList<String> Expected = GetExpected();
                ArrayList<String> Matched = GetMatched();
                List<RfidItems> rfidItems = new ArrayList<RfidItems>();

                if (auditRFIDModel == null) {
                    lblError.setText("Button Done : API Call: RFID Audit Model is Null ");
                    lblError.setTextColor(ColorRed);
                    return;
                }

                if (auditRFIDModel.getRfidItems() == null) {
                    lblError.setText("Button Done : API Call : RFID Items Is Null ");
                    lblError.setTextColor(ColorRed);
                    return;
                }

                if (auditRFIDModel.getUpcItems() == null) {
                    lblError.setText("Button Done : API Call : UPC Items Is Null ");
                    lblError.setTextColor(ColorRed);
                    return;
                }
                if (auditRFIDModel.getAuditID() <= 0) {
                    lblError.setText("Button Done : API Call : Audit Id = 0 ");
                    lblError.setTextColor(ColorRed);
                    return;
                }

                if (auditRFIDModel.getBoxRFID() == null) {
                    lblError.setText("Button Done : API Call : BoxRFID is NULL ");
                    lblError.setTextColor(ColorRed);
                    return;
                }
                if (auditRFIDModel.getBoxBarcode() == null) {
                    lblError.setText("Button Done : API Call : Audit BoxBarcode = 0 ");
                    lblError.setTextColor(ColorRed);
                    return;
                }
                try {
                    rfidItems = auditRFIDModel.getRfidItems();
                } catch (Exception e) {
                    lblError.setText(e.getMessage());
                    lblError.setTextColor(ColorRed);
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
            lblError.setText(ex.getMessage());
            lblError.setTextColor(ColorRed);
        }

    }


    @SuppressLint("ResourceAsColor")
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void InitRFIDApi(String BoxBarcode, String BoxRFID) {
        try {
            synchronized (apiDone_Lock) {
                apiDone = false;
            }


            Log.i("AH-Log", "InitRFID => " + BoxBarcode + " " + BoxRFID);
            int UserID = mStorage.getDataInt("UserID");

            BasicApi api = APIClient.INSTANCE.getInstance(IPAddress, false).create(BasicApi.class);
            Call<AuditRFIDModel> call = api.InitNonBBBAudit(UserID, BoxBarcode, BoxRFID);
            call.enqueue(new Callback<AuditRFIDModel>() {
                @Override
                public void onResponse(Call<AuditRFIDModel> call, Response<AuditRFIDModel> response) {

                    synchronized (apiDone_Lock) {
                        apiDone = true;
                    }


                    Log.i("AH-Log-API-INIT", "Response From API");
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
                            Log.i("AH-Log-API-INIT", "auditModel RFID Items ");
                            for (RfidItems s : auditRFIDModel.getRfidItems()) {
                                Log.i("AH-Log", "auditModel RFID Items " + s.getRfid() + "  with  " + s.getBarcode());
                                Log.i("AH-Log-API-INIT", "auditModel RFID Items " + s.getRfid() + "  with  " + s.getBarcode());
                            }
                            Log.i("AH-Log-API-INIT", "auditModel UPC Items ");
                            for (UpcItems s : auditRFIDModel.getUpcItems()) {
                                Log.i("AH-Log", "auditModel UPC Items " + s.getBarcode());
                                Log.i("AH-Log-API-INIT", "auditModel UPC Items " + s.getBarcode());
                            }

                            if (auditRFIDModel != null && auditRFIDModel.getAuditID() != -1) {
                                checkingBoxNumber = false;
                                ProceedSuccessScan(auditRFIDModel.getAuditID(), auditRFIDModel.getBoxBarcode(), auditRFIDModel.getBoxRFID());

                            } else {
                                String msg = auditRFIDModel == null ? "Invalid Box Barcode" : auditRFIDModel.getBoxBarcode();
                                updateErrorLabel(msg, ColorRed);
                                log(msg);
                            }
                        } else {

                            String msg = "API Returned Null For BoxBarcode: " + BoxBarcode + " BoxRFID: " + BoxRFID;

                            log(msg);
                        }

                    } catch (Exception ex) {

                        updateErrorLabel(ex.getMessage(), ColorRed);
                    }


                }

                @Override
                public void onFailure(Call<AuditRFIDModel> call, Throwable t) {

                    try {
                        updateErrorLabel(t.getMessage(), ColorRed);
                        log(t.getMessage());
                    } catch (Exception ex) {
                        log(ex.getMessage());
                        updateErrorLabel(t.getMessage(), ColorRed);
                    }
                }
            });


        } catch (Exception ex) {
            updateErrorLabel(ex.getMessage(), ColorRed);
            log(ex.getMessage());


        } finally {


        }
    }

    public void GetAddedRFIDSItemSerial(ArrayList<String> rfids, String title, FragmentManager manager){

        ProgressDialog dialog = ProgressDialog.show(BBBAuditAllAntennasActivity.this, "", "Getting Added Items, Please Wait...", true);

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.GetRFIDAuditInfo(new RFIDAuditInfoModel(rfids))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){

                                    dialog.cancel();

                                    ArrayList<String> data = new ArrayList<>();

                                    for(RFIDAuditInfoReceivedModel model : s){
                                        if(model.getStatus() == 1){
                                            data.add(model.getItemSerial() + " - " + model.getRfid());
                                        }else {
                                            data.add("No Serial - " + model.getRfid());
                                        }
                                    }

                                    General1.ShowDialog(manager,title, data);

                                    Logger.Debug("API", "ProcessDetectedRFIDTag - RFID Info Received: " + s);
                                }
                            }, (throwable) -> {
                                String error = throwable.getMessage();
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    String response = ex.response().errorBody().string();
                                    if(response.isEmpty()){
                                        response = throwable.getMessage();
                                    }
                                    Logger.Debug("API", "ProcessDetectedRFIDTag - Returned HTTP Error " + response);
                                    error = response;

                                }else {
                                    error = throwable.getMessage();
                                    Logger.Error("API", "ProcessDetectedRFIDTag - Error In API Response: " + error);
                                }
                                lblError.setTextColor(ColorRed);
                                lblError.setText(error);
                                log(error);

                                dialog.cancel();

                            }));
        } catch (Throwable e) {
            Logger.Error("API", "ProcessDetectedRFIDTag - Error Connecting: " + e.getMessage());
            lblError.setTextColor(ColorRed);
            lblError.setText(e.getMessage());
            log(e.getMessage());
            dialog.cancel();
        }
    }

    @SuppressLint("ResourceAsColor")
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void GetUPCByRFID(ArrayList<String> Items, String title, FragmentManager manager) {
        GetAddedRFIDSItemSerial(Items, title, manager);
    }

    public ArrayList<String> GetAdded() {
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
            lblError.setText(e.getMessage());
            lblError.setTextColor(ColorRed);
            return null;
        }

        return Data;
    }

    public ArrayList<String> GetExpected() {
        ArrayList<String> Data = new ArrayList<>();
        try {
            List<UpcItems> upcItems = auditRFIDModel.getUpcItems();
            for (UpcItems item : upcItems) {
                Data.add((item.getBarcode()));
            }
        } catch (Exception e) {
            log(e.getMessage());
            lblError.setText(e.getMessage());
            lblError.setTextColor(ColorRed);
            return null;
        }
        return Data;
    }

    public ArrayList<String> GetMatched() {

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
            lblError.setText(e.getMessage());
            lblError.setTextColor(ColorRed);
            return null;
        }

        return Data;
    }

    public ArrayList<String> GetMissing() {
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
            lblError.setText(ex.getMessage());
            lblError.setTextColor(ColorRed);
            log(ex.getMessage());
            return null;
        }

        return Data;
    }

    public ArrayList<String>GetMissing(boolean withIS){
        ArrayList<String> Data= new ArrayList<>();
        try{
            //List<UpcItems> upcItems= auditRFIDModel.getUpcItems();
            List<RfidItems> rfidItems= auditRFIDModel.getRfidItems();
            List<String> Keys=hmList.keySet().stream().filter(x->validItemRfid(x)).collect(Collectors.toList());

            ArrayList<String> ExpectedUPC= new ArrayList<>();
            ArrayList<String> MatchedUPC= new ArrayList<>();
            for (RfidItems item:rfidItems) {
                ExpectedUPC.add(item.getBarcode() + (withIS ? "-" + item.getRfid() : ""));
            }

            for (String rfid:Keys) {
                if(rfidItems.stream().anyMatch(r -> r.getRfid().equals(rfid))){
                    RfidItems Item=rfidItems.stream().filter(r -> r.getRfid().equals(rfid)).findFirst().get();
                    MatchedUPC.add(Item.getBarcode() + (withIS ? "-" + Item.getRfid() : ""));
                }
            }
            for (String UPC:MatchedUPC) {
                ExpectedUPC.remove(UPC);
            }
            ArrayList<String> MissingUPC= ExpectedUPC;
            Data.addAll(MissingUPC);
        }catch(Exception ex){
            lblError.setText(ex.getMessage());
            lblError.setTextColor(ColorRed);
            log(ex.getMessage());
            return null;
        }

        return  Data;
    }

    public ArrayList<String> GetRFIDList() {
        ArrayList<String> Data = new ArrayList<>();
        List<String> Keys = hmList.keySet().stream().filter(x -> validItemRfid(x)).collect(Collectors.toList());
        Data.addAll(Keys);
        return Data;
    }

    private boolean ContinueRead = true;


    //收藏窗口
    @SuppressLint("ResourceAsColor")
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void ProceedRFIDToAPI(ArrayList<AuditRFIDModelPostItems> Items) {

            try {
                AuditRFIDModelPost model =
                        new AuditRFIDModelPost(ValidatedAuditID, ValidatedBoxNumber, auditRFIDModel.getBoxRFID(), mStorage.getDataString("StationCode", ""),UserID, Items,isReplenishment);


                mStorage = new Storage(this);
                //   String IPAddressWarehouseManager = mStorage.getDataString("IPAddressWarehouseManager", "192.168.10.82");
                BasicApi api = APIClient.getInstanceStatic(IPAddress,true).create(BasicApi.class);
                CompositeDisposable compositeDisposable = new CompositeDisposable();
                compositeDisposable.addAll(
                        api.PostNonBBBAudit(model)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe((s) -> {
                                    if(s != null){
                                        if(s.getAuditResult().toLowerCase().contains("box not released")){
                                            playError();
                                            lblAuditResult.setText(s.getAuditResult());
                                            lblAuditResult.setTextColor(ColorRed);
                                            lblError.setText(s.getResult());
                                            lblError.setTextColor(ColorRed);
                                            //showMessage("Send Bin To: "+s);
                                            Logger.Debug("API", "Rfid Audit : response "+ s);
                                        }else{
                                            playSuccess();
                                            lblAuditResult.setText(s.getAuditResult()==""?"Success":s.getAuditResult());
                                            lblAuditResult.setTextColor(ColorGreen);
                                            lblError.setText(s.getResult());
                                            lblError.setTextColor(ColorGreen);
                                            lbLocation.setText(s.getLocation());
                                            lbLocation.setTextColor(ColorGreen);
                                            //showMessage("Send Bin To: "+s);
                                            Logger.Debug("API", "Rfid Audit : response "+ s);
                                        }

                                    }
                                }, (throwable) -> {
                                    lblAuditResult.setText("Failed XX");
                                    lblAuditResult.setTextColor(ColorRed);
                                    lbLocation.setText("Failed XX");
                                    lbLocation.setTextColor(ColorRed);
                                    lblError.setTextColor(ColorRed);

                                    if(throwable instanceof HttpException){
                                        HttpException ex = (HttpException) throwable;
                                        String response = ex.response().errorBody().string();
                                        if(response.isEmpty()){
                                            response = throwable.getMessage();
                                        }
                                        lblError.setText("Error: "+response+" (API Error)");
                                        showMessage(response);
                                        Logger.Debug("API", "Error - Returned HTTP Error " + response);
                                    }else {
                                        lblError.setText(" Error : "+throwable.getMessage()+" (API Error)");
                                        Logger.Error("API", " Error In API Response: " + throwable.getMessage());
                                        showMessage(throwable.getMessage());
                                    }
                                    playError();


                                }));
            } catch (Throwable e) {
                lblAuditResult.setText("Failed XX");
                lblAuditResult.setTextColor(ColorRed);
                lbLocation.setText("Failed XX");
                lbLocation.setTextColor(ColorRed);
                lblError.setTextColor(ColorRed);
                lblError.setText("Error: "+ e.getMessage() +" (Exception)");
                lbLocation.setTextColor(ColorRed);
                playError();
                Logger.Error("API", "AssignBinToLocation  Error Connecting: " + e.getMessage());
                showMessage(e.getMessage());

            }


    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    public void PostRFID(ArrayList<AuditRFIDModelPostItems> Items) {
        try {

            ProceedRFIDToAPI(Items);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                RefreshView();
            }
            //Assign adapter to ListView
            General1.getGeneral(this).InRFID = false;
        } catch (Exception ex) {
            log(ex.getMessage());
        }
    }


    String ValidatedBoxNumber = "";
    String ValidatedBoxRFID = "";
    Integer ValidatedAuditID = 0;

    @SuppressLint("ResourceAsColor")
    public void ProceedSuccessScan(Integer AuditID, String BoxNumber, String BoxRFID) {
        Log.i("ProceedSuccessScan", "ProceedSuccessScan " + BoxNumber);
        ValidatedBoxNumber = BoxNumber;
        ValidatedBoxRFID = BoxRFID;
        ValidatedAuditID = AuditID;
        IsRFIDConnected = true;
        runOnUiThread(() -> {

            btnScanRFIDStart.setText(PAUSE);
            lblError.setTextColor(R.color.green);
            lblError.setText("Box Loaded");
        });


        //  ReadMultiRFIDV2();
    }

    AuditRFIDModel auditRFIDModel = null;


    public String TagModelKey(Tag_Model model) {
        if (model._EPC.equals("E28068940000400C86079169")) {
            return "30342CBD280E70D0A43701F3-E280689020004006CF0064AE";
        }
        if (model._EPC != null && model._TID != null && !model._EPC.isEmpty() && !model._TID.isEmpty())
            return model._EPC + "-" + model._TID;
        return "";
    }

    Integer totalReadCount = 0;

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


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void RefreshView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
                LocalTime localTime = LocalTime.now();
                txtTime.setText(dtf.format(localTime));

                try {
                    Integer index = 0;


                    if (!IsRFIDConnected) {
                        RFIDUPCView.add(0, "Loading Box..");
                        adapter.notifyDataSetChanged();
                        return;
                    }
                    RFIDUPCView.clear();
                    for (String message : RFIDMessages.stream().limit(10).collect(Collectors.toList())) {
                        RFIDUPCView.add(0, message);
                    }

                    if (auditRFIDModel != null) {
                        String Count;
                        String Expected;
                        String Matched;
                        String Missed;
                        String Added;
                        synchronized (hmList_Lock) {

                            ArrayList<String> list = GetRFIDList();
                            Count = list == null ? "--" : list.size() + "";
                            list = GetExpected();
                            Expected = list == null ? "--" : list.size() + "";
                            list = GetMatched();
                            Matched = list == null ? "--" : list.size() + "";
                            list = GetMissing();
                            Missed = list == null ? "--" : list.size() + "";
                            list = GetAdded();
                            Added = list == null ? "--" : list.size() + "";
                        }


                        btnFoundedRFID.setText("RFID:" + Count);
                        btnMatchedRFID.setText("Matched:" + Matched);
                        btnExpectedRFID.setText("Expected:" + Expected);
                        btnMissingRFID.setText("Missing:" + Missed);
                        btnAddedRFID.setText("Added:" + Added);

                        RFIDUPCView.add(0, "RFID Count:" + Count);
                        RFIDUPCView.add(0, "Matched:" + Matched);
                        RFIDUPCView.add(0, "Expected:" + Expected);
                        RFIDUPCView.add(0, "Missed:" + Missed);
                        RFIDUPCView.add(0, "Added:" + Added);
                    }


                    adapter = new ArrayAdapter(getApplicationContext(),
                            android.R.layout.simple_list_item_1, android.R.id.text1, RFIDUPCView) {

                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            TextView textView = (TextView) super.getView(position, convertView, parent);
                            textView.setTextSize(25);
                            String txt = textView.getText().toString();
                            if (txt.startsWith("RFID Count:")) {
                                textView.setTextSize(25);
                            }
                            return textView;
                        }

                        ;
                    };

                    adapter.notifyDataSetChanged();
                } catch (Exception ex) {
                    lblError.setText(ex.getMessage());
                    lblError.setTextColor(ColorRed);
                    log(" Error: " + ex.getMessage());
                }

            }


        });

    }


    private void setMainBackIcon() {
        if (mFilterModuleArray.size() == 0) {
            mNotBluetooth.setVisibility(View.VISIBLE);
        } else {
            mNotBluetooth.setVisibility(View.GONE);
        }
    }

    private void initPermission() {
        PermissionUtil.requestEach(BBBAuditAllAntennasActivity.this, new PermissionUtil.OnPermissionListener() {


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

                        if (Analysis.isOpenGPS(BBBAuditAllAntennasActivity.this))
                            refresh();
                        else
                            startLocation();
                    }
                }, 1000);

            }
        }, PermissionUtil.LOCATION);
    }

    private Object beep_Lock = new Object();

    private void startLocation() {
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
    protected void onPause() {
        super.onPause();
        PauseRFID();
        //退出这个界面，或是返回桌面时，停止扫描

    }

    private void PauseRFID() {
        ContinueRead = false;
        checkingBoxNumber = false;
        txtReadingFromANt.setText("");
        if (RFIDDevicesManager.getSingleAntennaReader() != null)
            RFIDDevicesManager.getSingleAntennaReader().stop();

    }

    @Override
    protected void onResume() {
        super.onResume();
        //  if(IsReading()){
        //  ReadMultiRFIDV2();
        //  }
    }

    private void setTitle() {
        mTitle = new DefaultNavigationBar
                .Builder(this, (ViewGroup) findViewById(R.id.activity_scan_rfid))
                .setLeftText((isReplenishment?"Replenishment ":"") +"Antenna Audit")
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

    //头部下拉窗口
    private void setPopWindow(View v) {
        new PopWindowMain(v, BBBAuditAllAntennasActivity.this, new PopWindowMain.DismissListener() {
            @Override
            public void onDismissListener(boolean resetEngine) {//弹出窗口销毁的回调
                mTitle.updateRightImage(false);
                if (resetEngine) {//更换搜索引擎，重新搜索
                    refresh();
                }
            }
        });
    }


    int ColorGreen = Color.parseColor("#52ac24");
    int ColorRed = Color.parseColor("#ef2112");


    @ViewById(R.id.lblError)
    private TextView lblError;
    @ViewById(R.id.lblAuditResult)
    private TextView lblAuditResult;
 @ViewById(R.id.lblLocation)
    private TextView lbLocation;
 @ViewById(R.id.layoutLocation)
    private LinearLayout layoutLocation;

    @ViewById(R.id.rfidHeader)
    private TextView lblRfidHeader;


    @ViewById(R.id.main_back_not)
    private LinearLayout mNotBluetooth;

    @ViewById(R.id.btnScanRFIDStart)
    private Button btnScanRFIDStart;

    @ViewById(R.id.btnScanRFIDDone)
    private Button btnScanRFIDDone;

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

    @ViewById(R.id.textReadingFromAnt)
    private TextView txtReadingFromANt;

    @ViewById(R.id.textTime)
    private TextView txtTime;


    private DefaultNavigationBar mTitle;

    private Storage mStorage;

    private List<DeviceModule> mModuleArray = new ArrayList<>();
    private List<DeviceModule> mFilterModuleArray = new ArrayList<>();

    private int mStartDebug = 1;
    private String IPAddress = "";
    private int UserID = -1;


    private String RFIDGrp1ReadTime1 = "";


    @Override
    protected void onDestroy() {
        Log.i("AH-Log-distroy", "distroyed");
        ContinueRead = false;
        checkingBoxNumber = false;
        IsRFIDConnected = false;
        PauseRFID();
        super.onDestroy();
    }

    public void RefreshSettings() {
        mStorage = new Storage(this);
        RFIDGrp1ReadTime1 = mStorage.getDataString("RFIDGrp1ReadTime1", "300");
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
        UserID = mStorage.getDataInt("UserID", -1);
    }


    private Boolean IsReading() {
        return btnScanRFIDStart.getText().equals(PAUSE);
    }

    String PAUSE = "Pause";
    String START = "Start";

    private void RestartScreen() {
        PauseRFID();
        lblError.setTextColor(ColorGreen);
        lbLocation.setTextColor(ColorGreen);
        lbLocation.setText("");
       // lblError.setText("Done");
        ValidatedBoxNumber = "";
        ValidatedBoxRFID = "";
        ValidatedAuditID = 0;
        auditRFIDModel = null;
        PauseRFID();

        btnFoundedRFID.setText("RFID");
        btnMatchedRFID.setText("Matched");
        btnExpectedRFID.setText("Expected");
        btnMissingRFID.setText("Missing");
        btnAddedRFID.setText("Added");

        RFIDMessages.clear();
        RFIDUPCView.clear();
        RFIDUPC.clear();
        hmList.clear();
        boxMap.clear();
    }

    private void ReadMultiRFIDV2() {
        if (!IsRFIDConnected)
            return;
        ContinueRead = true;
        if (!IsReading())
            return;


        try {

//            new Thread(() -> {
//                try {
//                  //  RFIDDevicesManager.readEPCSingleAntenna(eReadType.Inventory,Integer.valueOf(RFIDGrp1ReadTime1)<1000 ?1000:Integer.valueOf(RFIDGrp1ReadTime1));
//                } catch (Exception e) {
//                    showMessage("Error: " + e.getMessage());
//                }
//
//            }).start();
            RFIDGrp1ReadTime1 = mStorage.getDataString("RFIDGrp1ReadTime1", "1000");
            read();

        } catch (Exception ex) {
            LogError("ReadRFID Exception:" + ex.getMessage());
        } finally {
        }
    }


    private void LogError(String str) {
        Logging.LogError(getApplicationContext(), str);
    }

    private void LogDebug(String str) {
        Logging.LogDebug(getApplicationContext(), str);
    }


    private ArrayList<String> RFIDMessages = new ArrayList();

    @SuppressLint("ResourceAsColor")
    private void initRFID() {
        try {
            String Connection = "";
            RFIDDevicesManager.setOutput(new RFIDOutput(this));
            antList = RFIDDevicesManager.getSingleAntennaReader().getAntennaListInt();
            int RFIDCount = RFIDReader.HP_CONNECT.size();
            lblError.setTextColor(ColorGreen);
            lblError.setText(String.valueOf(RFIDCount) + " Connected RFID Devices\n" + Connection);
            HashMap<String, BaseConnect> lst = RFIDReader.HP_CONNECT;
            if (lst.keySet().stream().count() > 0) {
                IsRFIDConnected = true;
            } else {

            }

            //Test
            //IsRFIDConnected=true;
        } catch (Exception ex) {
            lblError.setTextColor(ColorRed);
            lblError.setText(ex.getMessage());
        }

    }

    boolean IsRFIDConnected = false;


    private HashMap<String, Tag_Model> hmList = new HashMap<String, Tag_Model>();
    private Object hmList_Lock = new Object();


    private void initView() {
        setMainBackIcon();

        adapter = new ArrayAdapter(getApplicationContext(),
                android.R.layout.simple_list_item_1, android.R.id.text1, RFIDUPCView) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setTextSize(12);
                return textView;
            }

            ;
        };
        adapter.notifyDataSetChanged();
    }

    ArrayAdapter adapter = null;


    private void refresh() {
        if (false) {
            mModuleArray.clear();
            mFilterModuleArray.clear();
            mTitle.updateLoadingState(true);
            adapter.notifyDataSetChanged();
        }

    }






    public static final int FRAGMENT_STATE_DATA = 0x06;
    public static final int FRAGMENT_STATE_SERVICE_VELOCITY = 0x13;//读取实时速度
    public static final int FRAGMENT_STATE_NUMBER = 0x07;
    public static final int FRAGMENT_STATE_CONNECT_STATE = 0x08;
    public static final int FRAGMENT_STATE_SEND_SEND_TITLE = 0x09;
    public static final int FRAGMENT_STATE_LOG_MESSAGE = 0x011;
    ArrayList<String> RFIDUPC = new ArrayList<>();
    ArrayList<String> RFIDUPCView = new ArrayList<>();


    @Override
    public void WriteDebugMsg(String s) {
        // TODO Auto-generated method stub

    }

    @Override
    public void WriteLog(String s) {
        // TODO Auto-generated method stub

    }

    @Override
    public void PortConnecting(String s) {
        // TODO Auto-generated method stub

    }

    @Override
    public void PortClosing(String s) {
        // TODO Auto-generated method stub

    }

    @Override
    public void OutPutTagsOver() {
        // TODO Auto-generated method stub

    }

    @Override
    public void GPIControlMsg(int i, int j, int k) {
        // TODO Auto-generated method stub

    }

    @Override
    public void OutPutScanData(byte[] scandata) {
        // TODO Auto-generated method stub
    }

    public void showMessage(String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();

            }
        });


    }
}