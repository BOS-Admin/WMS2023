package com.hc.mixthebluetooth.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.google.android.material.snackbar.Snackbar;
import com.hc.basiclibrary.ioc.ViewById;
import com.hc.basiclibrary.permission.PermissionUtil;
import com.hc.basiclibrary.titleBasic.DefaultNavigationBar;
import com.hc.basiclibrary.viewBasic.BasActivity;
import com.hc.basiclibrary.viewBasic.LibGeneral;
import com.hc.basiclibrary.viewBasic.tool.IMessageInterface;
import com.hc.bluetoothlibrary.DeviceModule;
import com.hc.mixthebluetooth.Logger;
import com.hc.mixthebluetooth.Model.RFIDAuditInfoReceivedModel;
import com.hc.mixthebluetooth.R;
import com.hc.mixthebluetooth.activity.tool.Analysis;
import com.hc.mixthebluetooth.customView.PopWindowMain;
import com.hc.mixthebluetooth.storage.Storage;
import com.hopeland.androidpc.example.PublicData;
import com.rfidread.Connect.BaseConnect;
import com.rfidread.Enumeration.eReadType;
import com.rfidread.Helper.Helper_ThreadPool;
import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.Models.Tag_Model;
import com.rfidread.RFIDReader;
import com.util.General1;
import com.util.Logging;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import Model.AuditRFIDModel;
import Model.AuditRFIDModelPost;
import Model.AuditRFIDModelPostItems;
import Model.PostListItems;
import Model.RFIDAuditInfoModel;
import Model.RFIDPackingInfoModel;
import Model.RfidItems;
import Model.UpcItems;
import Remote.APIClient;
import com.hc.mixthebluetooth.Remote.Routes.BasicApi;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;

public class BBBAuditActivity extends BasActivity implements IAsynchronousMessage {

    private HashMap<String, Tag_Model> boxMap = new HashMap<String, Tag_Model>();
    private Object boxMap_Lock = new Object();
    private List<String> nonValidRfids=new ArrayList<>();
    private boolean checkingBoxNumber=false;
    private Object apiDone_Lock = new Object();
    private boolean apiDone;

    private String itemSerialRfidPrefix="BBB";
    private String binRfidPrefix="BBB020,BBB20";
    private List<String> itemPrefixList=new ArrayList<>();
    private List<String> binPrefixList=new ArrayList<>();

    private boolean DiscardItemSerial = false;

    private ArrayList<String> ProcessedItems = new ArrayList<>();
    private HashMap<String, Tag_Model> PendingItems = new HashMap<>();

    public boolean validItemRfid(String s){
        if(s == null)
            return false;
        if(DiscardItemSerial){
            return true;
        }else {
            return itemPrefixList.stream().anyMatch(i->s.toLowerCase().startsWith(i.toLowerCase()));
        }
    }
    public boolean validBinRfid(String s){
        return  s!=null && binPrefixList.stream().anyMatch(i->s.toLowerCase().startsWith(i.toLowerCase()));
    }


    public void validateBoxNumber(){
        new Thread(() -> {
            List<Tag_Model> boxRfids;
            HashMap<String,String> boxMapResults=new HashMap<>();
            updateTextValidateBox("",ColorGreen);
            updateTextBoxList(boxMapResults);
            Log.i("AH-Log-Box","starting validation process");
            int count=0;
            while(checkingBoxNumber){
                count++;
                Log.i("AH-Log-Box","==================> validation process cycle <"+count+">  <=============================");

                boxRfids = new ArrayList<>();
                Log.i("AH-Log-Box","requesting lock");
                synchronized (boxMap_Lock){
                    Log.i("AH-Log-Box","lock acquired");
                    boxRfids.addAll(boxMap.values());
                    Log.i("AH-Log-Box","BoxMap values:");
                    for(Tag_Model m : boxMap.values())
                        Log.i("AH-Log-Box",m._EPC);
                }
                Log.i("AH-Log-Box","================");
                Log.i("AH-Log-Box","nonValidRfids values:");
                for(String m : nonValidRfids)
                    Log.i("AH-Log-Box",m);

                Log.i("AH-Log-Box","=============================");
                for(Tag_Model model:boxRfids){
                    Log.i("AH-Log-Box","For Loop : "+model._EPC);
                    if(!nonValidRfids.contains(model._EPC) && ValidatedBoxNumber==null || ValidatedBoxNumber.isEmpty()){
                        Log.i("AH-Log-Box","Inside If : "+model._EPC);
                        String EPC=model._EPC.startsWith("30396062C39ADD4000231D87")?"BBB0200700003917AC011038":model._EPC;
                        String BarcodeFromEPC=EPCToUPC(EPC);
                        if(validBinRfid(EPC)){
                            if(!boxMapResults.containsKey(EPC))
                                boxMapResults.put(EPC,": <<<< Checking >>>>");
                            updateTextBoxList(boxMapResults);
                            Log.i("AH-Log-Box","Initialize RFID API For : "+model._EPC);
                            InitRFIDApi(BarcodeFromEPC,EPC);

                            while(true){
                                synchronized (apiDone_Lock){
                                    if(apiDone)
                                        break;
                                }

                                try {
                                    Thread.sleep(200);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }



                            Log.i("AH-Log-Box","ValidatedBoxNumber : "+ValidatedBoxNumber);
                            if(ValidatedBoxNumber==null || ValidatedBoxNumber=="" || ValidatedBoxNumber.isEmpty() || ValidatedBoxNumber.length()<5) {
                                nonValidRfids.add(model._EPC);
                                boxMapResults.put(EPC,": Not Valid ");
                                updateTextBoxList(boxMapResults);
                            }
                            else {
                                checkingBoxNumber=false;
                                Log.i("AH-Log-Box","Returning ... ValidatedBoxNumber : "+ValidatedBoxNumber);
                                updateTextValidateBox("ValidatedBoxNumber : "+ValidatedBoxNumber,ColorGreen);
                                boxMapResults.put(EPC,": Valid ");
                                updateTextBoxList(boxMapResults);
                                return;
                            }

                            Log.i("AH-Log-Box","nonValidRfids values:");
                            for(String m : nonValidRfids)
                                Log.i("AH-Log-Box",m);

                            Log.i("AH-Log-Box","===========================");



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

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void OutPutTags(Tag_Model model) {
        try {

            synchronized (hmList_Lock) {
                Log.i("AH-Log",model._EPC);

                if(TagModelKey(model).isEmpty()){
                    return;
                }


                if (hmList.containsKey(TagModelKey(model))) {

                    Tag_Model tModel = hmList.get(TagModelKey(model));
                    tModel._TotalCount++;
                    model._TotalCount = tModel._TotalCount;
                    LogDebug("EPC Read"+TagModelKey(model)+" count:" +String.valueOf(model._TotalCount));
                    hmList.remove(TagModelKey(model));
                    hmList.put(TagModelKey(model), model);
                    Log.i("AH-Log","If");

                }
                else {
                    LogDebug("EPC Read11"+TagModelKey(model)+" count:" +String.valueOf(model._TotalCount));
                    model._TotalCount = 1;


                    if(validBinRfid(TagModelKey(model)))
                    {
                        synchronized (boxMap_Lock) {
                            if (!boxMap.containsKey(TagModelKey(model)))
                                boxMap.put(TagModelKey(model), model);
                        }

                    }
                    else{
                        String rfid = TagModelKey(model);
                        hmList.put(rfid, model);
                    }


                }
            }
            synchronized (beep_Lock) {
                beep_Lock.notify();
            }
            totalReadCount++;

        } catch (Exception ex) {
            Log.d("Debug", "Tags output exceptions:" + ex.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("ResourceAsColor")
    @Override
    public void initAll() {
        try {
            RefreshSettings();
            GetBT4DeviceStrList();
            mStorage = new Storage(this);//sp存储
            RFIDMac=mStorage.getDataString("RFIDMac","00:15:83:3A:4A:26");
            WeightMac=mStorage.getDataString("WeightMac","58:DA:04:A4:50:14");
            IPAddress=mStorage.getDataString("IPAddress","192.168.10.82");
            UserID=mStorage.getDataInt("UserID",-1);
            //RFIDName=GetBluetoothDeviceNameFromMac(RFIDMac);
            RFIDName=GetBluetoothDeviceNameFromMac(RFIDMac);

            initPermission();

            initView();

            itemSerialRfidPrefix=mStorage.getDataString("ItemSerialRfidPrefix","BBB");
            binRfidPrefix=mStorage.getDataString("BinRfidPrefix","BBB020,BBB20");
            lblRfidHeader.setText("Rfid Filter ("+ itemSerialRfidPrefix+") Bin ("+binRfidPrefix+")");
            itemPrefixList.addAll(Arrays.asList(itemSerialRfidPrefix.split(",")));
            binPrefixList.addAll(Arrays.asList(binRfidPrefix.split(",")));

            new CountDownTimer(500, 500)
            {
                public void onTick(long l) {

                }
                public void onFinish()
                {
                    initRFID();
                }
            }.start();



            btnScanRFIDStart.setOnClickListener(view -> {
                if(btnScanRFIDStart.getText().equals(START)){
                    btnScanRFIDStart.setText(PAUSE);
                    if(ValidatedBoxNumber==null || ValidatedBoxNumber.isEmpty() ){
                        checkingBoxNumber=true;
                        validateBoxNumber();
                    }

                    ReadMultiRFIDV2();
                }else{
                    btnScanRFIDStart.setText(START);
                    checkingBoxNumber=false;
                    PauseRFID();
                }
            });


            btnFoundedRFID.setOnClickListener(view -> {
                if(auditRFIDModel==null)
                    return;
                if(btnScanRFIDStart.getText().equals(START)){
                    GetUPCByRFID(GetRFIDList(), "RFID List", this.getSupportFragmentManager());

                }else{
                    return;
                }
            });

            btnExpectedRFID.setOnClickListener(view -> {
                if(auditRFIDModel==null)
                    return;
                if(btnScanRFIDStart.getText().equals(START)){

                    General1.ShowDialog(this.getSupportFragmentManager(),"Expected",GetExpected());
                }else{
                    return;
                }
            });
            btnAddedRFID.setOnClickListener(view -> {
                if(auditRFIDModel==null)
                    return;
                if(btnScanRFIDStart.getText().equals(START)){
                    GetUPCByRFID(GetAdded(), "Added List", this.getSupportFragmentManager());
                }else{
                    return;
                }
            });
            btnMatchedRFID.setOnClickListener(view -> {
                if(auditRFIDModel==null)
                    return;
                if(btnScanRFIDStart.getText().equals(START)){

                    General1.ShowDialog(this.getSupportFragmentManager(),"Matched",GetMatched());
                }else{
                    return;
                }
            });
            btnMissingRFID.setOnClickListener(view -> {
                if(auditRFIDModel==null)
                    return;
                if(btnScanRFIDStart.getText().equals(START)){
                    General1.ShowDialog(this.getSupportFragmentManager(),"Missing",GetMissing(true));
                }else{
                    return;
                }
            });

            btnScanRFIDDone.setOnClickListener(view -> {

                checkingBoxNumber=false;
                btnScanRFIDStart.setText(START);
                PauseRFID();

                if(auditRFIDModel==null)
                    return;


                ArrayList<AuditRFIDModelPostItems> Items= new ArrayList<>();

                ArrayList<String> Missing=GetMissing();
                ArrayList<String> RFIDItems=GetRFIDList();
                ArrayList<String> Added=GetAdded();
                ArrayList<String> Expected=GetExpected();
                ArrayList<String> Matched=GetMatched();
                List<RfidItems> rfidItems= new ArrayList<RfidItems>();

                if(auditRFIDModel==null){
                    lblError.setText("Button Done : API Call: RFID Audit Model is Null ");
                    lblError.setTextColor(ColorRed);
                    return;
                }

                if(auditRFIDModel.getRfidItems()==null){
                    lblError.setText("Button Done : API Call : RFID Items Is Null ");
                    lblError.setTextColor(ColorRed);
                    return;
                }

                if(auditRFIDModel.getUpcItems()==null){
                    lblError.setText("Button Done : API Call : UPC Items Is Null ");
                    lblError.setTextColor(ColorRed);
                    return;
                }
                if(auditRFIDModel.getAuditID() <= 0 ){
                    lblError.setText("Button Done : API Call : Audit Id = 0 ");
                    lblError.setTextColor(ColorRed);
                    return;
                }

                if(auditRFIDModel.getBoxRFID() ==null ){
                    lblError.setText("Button Done : API Call : BoxRFID is NULL ");
                    lblError.setTextColor(ColorRed);
                    return;
                }
                if(auditRFIDModel.getBoxBarcode() ==null ){
                    lblError.setText("Button Done : API Call : Audit BoxBarcode = 0 ");
                    lblError.setTextColor(ColorRed);
                    return;
                }
                try{
                    rfidItems= auditRFIDModel.getRfidItems();
                }
                catch (Exception e){
                    lblError.setText(e.getMessage());
                    lblError.setTextColor(ColorRed);
                    log(e.getMessage());
                }


                for (String str:Matched) {
                    RfidItems Item=rfidItems.stream().filter(r -> r.getRfid().equals(str.split(":")[1])).findFirst().get();
                    Items.add(new AuditRFIDModelPostItems(Item.getId(), Item.getLotBondId(),Item.getBarcode(),Item.getRfid(),2,"Matched"));
                }

                for (String rfid:Added) {
                    Items.add(new AuditRFIDModelPostItems(-1,-1,"",rfid,3,"Added"));
                }

                for (String UPC:Missing) {
                    Items.add(new AuditRFIDModelPostItems(-1, -1,UPC,"",4,"Missed"));
                }
                for (String rfid:RFIDItems) {
                    Items.add(new AuditRFIDModelPostItems(-1,-1,"",rfid,5,"RFIDList"));
                }
                for (String UPC:Expected) {
                    Items.add(new AuditRFIDModelPostItems(-1, -1,UPC,"",6,"Expected"));
                }

                PostRFID(Items);
                RestartScreen();

            });
        } catch (Exception ex){
            lblError.setText(ex.getMessage());
            lblError.setTextColor(ColorRed);
        }

    }


    @SuppressLint("ResourceAsColor")
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void InitRFIDApi(String BoxBarcode,String BoxRFID) {
        try

        {

            synchronized (apiDone_Lock){
                apiDone=false;
            }

            // updateTextValidateBox("Validating Box: "+BoxRFID,ColorGreen);
            Log.i("AH-Log","InitRFID => " + BoxBarcode +" "+BoxRFID);
            int UserID=mStorage.getDataInt("UserID");

            BasicApi api = APIClient.INSTANCE.getInstance(IPAddress,false).create(BasicApi.class);
            Call<AuditRFIDModel> call = api.InitNonBBBAudit(UserID,BoxBarcode,BoxRFID);
            call.enqueue(new Callback<AuditRFIDModel>() {
                @Override
                public void onResponse(Call<AuditRFIDModel> call, Response<AuditRFIDModel> response) {


                    synchronized (apiDone_Lock){
                        apiDone=true;
                    }


                    try{
                        auditRFIDModel=response.body();
                        if(auditRFIDModel==null){
                            // updateErrorLabel("API Call: RFID Audit Model is Null ",ColorRed);
                            return;
                        }

                        if(auditRFIDModel.getRfidItems()==null){
                            //  updateErrorLabel("API Call : RFID Items Is Null ",ColorRed);
                            return;
                        }

                        if(auditRFIDModel.getUpcItems()==null){
                            // updateErrorLabel("API Call : UPC Items Is Null ",ColorRed);
                            return;
                        }
                        if(auditRFIDModel.getAuditID() <= 0 ){
                            //  updateErrorLabel("API Call : Audit Id = 0 ",ColorRed);
                            return;
                        }

                        if(auditRFIDModel.getBoxRFID() ==null ){
                            //   updateErrorLabel("API Call : BoxRFID is NULL ",ColorRed);
                            return;
                        }
                        if(auditRFIDModel.getBoxBarcode() ==null ){
                            //  updateErrorLabel("API Call : Audit BoxBarcode = 0 ",ColorRed);
                            return;
                        }

                        if(auditRFIDModel!=null && auditRFIDModel.getRfidItems()!=null && auditRFIDModel.getUpcItems()!=null ) {
                            for (RfidItems s : auditRFIDModel.getRfidItems())
                                Log.i("AH-Log", "auditModel RFID Items " + s.getRfid() + "  with  " + s.getBarcode());
                            for (UpcItems s : auditRFIDModel.getUpcItems())
                                Log.i("AH-Log", "auditModel UPC Items " + s.getBarcode());
                            if (auditRFIDModel != null && auditRFIDModel.getAuditID() != -1) {
                                checkingBoxNumber=false;
                                ProceedSuccessScan(auditRFIDModel.getAuditID(), auditRFIDModel.getBoxBarcode(), auditRFIDModel.getBoxRFID());


                            } else {

                                String msg = auditRFIDModel == null ? "Invalid Box Barcode" : auditRFIDModel.getBoxBarcode();
                                updateErrorLabel(msg,ColorRed);
                                log(msg);
                            }
                        }
                        else{

                            String msg = "API Returned Null For BoxBarcode: "+BoxBarcode +" BoxRFID: "+BoxRFID;
                            //   updateErrorLabel(msg,ColorRed);

                            log(msg);
                        }

                    }catch(Exception ex){

                        updateErrorLabel(ex.getMessage(),ColorRed);
                    }


                }

                @Override
                public void onFailure(Call<AuditRFIDModel> call, Throwable t) {

                    try{
                        updateErrorLabel(t.getMessage(),ColorRed);
                        log(t.getMessage());
                    }
                    catch (Exception ex){
                        log(ex.getMessage());
                        updateErrorLabel(t.getMessage(),ColorRed);
                    }
                }
            });


        }
        catch (Exception ex)
        {
            updateErrorLabel(ex.getMessage(),ColorRed);
            log(ex.getMessage());


        } finally
        {


        }
    }


    CountDownTimer refreshViewTimer = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bbb_audit);
        //设置头部
        setTitle();
        setContext(this);
        txtScanRFIDScan.setEnabled(false);


        try{
            DiscardItemSerial = getIntent().getBooleanExtra("DiscardItemSerial", false);
        }catch(Exception ex){

        }



        refreshViewTimer = new CountDownTimer(2000, 2000)
        {
            public void onTick(long l) {

            }
            public void onFinish()
            {
                RefreshView();
                start();
            }
        }.start();
        txtScanRFIDScan.setShowSoftInputOnFocus(false);
        txtScanRFIDScan.requestFocus();
        txtScanRFIDScan.setShowSoftInputOnFocus(false);

        txtScanRFIDScan.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (txtScanRFIDScan.getText().toString().isEmpty()) {
                    return;
                }
                if (txtScanRFIDScan.length() <7  || txtScanRFIDScan.length() > 20) {
                    txtScanRFIDScan.setText("");
                    return;
                }
            }
        });
    }

    public void ShowAlertDialog(String title, String message) {
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

    private void updateErrorLabel(String message,int color){
        runOnUiThread(()->{
            lblError.setText(message);
            lblError.setTextColor(color);
        });

    }


    private void updateTextValidateBox(String message,int color){
        runOnUiThread(()->{
            TextView txt= (TextView) findViewById(R.id.textboxValidation);
            txt.setText(message);
            txt.setTextColor(color);
        });

    }
    private void updateTextBoxList(HashMap<String,String> map){
        runOnUiThread(()->{
            TextView txt= (TextView) findViewById(R.id.textViewBoxList);
            String message="";
            if(map.size()>1){
                txt.setTextColor(ColorRed);
                message+="!!!!!!!!!!!!!!!! "+map.size()+" box numbers were found !!!!!!!!!!!!!!!!\n";
            }
            else{
                txt.setTextColor(Color.BLUE);
            }

            for(Map.Entry<String,String> entry: map.entrySet() ){
                message+=entry.getKey()+" "+entry.getValue()+"\n";
            }
            txt.setText(message);

        });

    }

    public void GetAddedRFIDSItemSerial(ArrayList<String> rfids, String title, FragmentManager manager){

        ProgressDialog dialog = ProgressDialog.show(BBBAuditActivity.this, "", "Getting Added Items, Please Wait...", true);

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

    public ArrayList<String>GetAdded(){
        ArrayList<String> Data= new ArrayList<>();
        try{
            List<RfidItems> rfidItems= auditRFIDModel.getRfidItems();
            List<String> Keys=hmList.keySet().stream().filter(x->validItemRfid(x)).collect(Collectors.toList());
            for (String rfid:Keys) {
                if(rfid.equals(ValidatedBoxRFID))
                    continue;
                if(rfidItems.stream().anyMatch(r -> r.getRfid().equals(rfid))){
                }
                else{
                    Data.add(rfid);
                }
            }

        }catch (Exception e){
            log(e.getMessage());
            lblError.setText(e.getMessage());
            lblError.setTextColor(ColorRed);
            return null;
        }

        return  Data;
    }

    public ArrayList<String>GetExpected(){
        ArrayList<String> Data= new ArrayList<>();
        try {
            List<UpcItems> upcItems = auditRFIDModel.getUpcItems();
            for (UpcItems item : upcItems) {
                Data.add((item.getBarcode()));
            }
        }catch (Exception e){
            log(e.getMessage());
            lblError.setText(e.getMessage());
            lblError.setTextColor(ColorRed);
            return null;
        }
        return Data;
    }
    public ArrayList<String>GetMatched(){

        ArrayList<String> Data= new ArrayList<>();
        try{
            List<RfidItems> rfidItems= auditRFIDModel.getRfidItems();
            List<String> Keys=hmList.keySet().stream().filter(x->validItemRfid(x)).collect(Collectors.toList());

            for (String rfid:Keys) {
                if(rfid.equals(ValidatedBoxRFID))
                    continue;
                if(rfidItems.stream().anyMatch(r -> r.getRfid().equals(rfid))){
                    RfidItems Item=rfidItems.stream().filter(r -> r.getRfid().equals(rfid)).findFirst().get();
                    Data.add(Item.getBarcode() + ":" + Item.getRfid());
                }

            }


        }catch (Exception e){
            log(e.getMessage());
            lblError.setText(e.getMessage());
            lblError.setTextColor(ColorRed);
            return null;
        }

        return  Data;
    }
    public ArrayList<String>GetMissing(){
        ArrayList<String> Data= new ArrayList<>();
        try{
            List<UpcItems> upcItems= auditRFIDModel.getUpcItems();
            List<RfidItems> rfidItems= auditRFIDModel.getRfidItems();
            List<String> Keys=hmList.keySet().stream().filter(x->validItemRfid(x)).collect(Collectors.toList());

            ArrayList<String> ExpectedUPC= new ArrayList<>();
            ArrayList<String> MatchedUPC= new ArrayList<>();
            for (UpcItems item:upcItems) {
                ExpectedUPC.add((item.getBarcode()));
            }

            for (String rfid:Keys) {
                if(rfidItems.stream().anyMatch(r -> r.getRfid().equals(rfid))){
                    RfidItems Item=rfidItems.stream().filter(r -> r.getRfid().equals(rfid)).findFirst().get();
                    MatchedUPC.add(Item.getBarcode());
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

    public ArrayList<String>GetRFIDList(){
        ArrayList<String> Data= new ArrayList<>();
        List<String> Keys=hmList.keySet().stream().filter(x->validItemRfid(x)).collect(Collectors.toList());
        Data.addAll(Keys);
        return Data;
    }

    private boolean ContinueRead=true;


    //收藏窗口
    @SuppressLint("ResourceAsColor")
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void ProceedRFIDToAPI(ArrayList<AuditRFIDModelPostItems> Items) {



        try {
            AuditRFIDModelPost model =
                    new AuditRFIDModelPost(ValidatedAuditID, ValidatedBoxNumber, auditRFIDModel.getBoxRFID(), mStorage.getDataString("StationCode", ""),UserID, Items,false);


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
                                    playSuccess();
                                    lblError.setText("Success!! "+s.getResult());
                                    lblError.setTextColor(ColorGreen);
                                    Logger.Debug("API", "Rfid Audit : response "+ s);
                                }
                            }, (throwable) -> {
                                lblError.setTextColor(ColorRed);

                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    String response = ex.response().errorBody().string();
                                    if(response.isEmpty()){
                                        response = throwable.getMessage();
                                    }
                                    lblError.setText("Failed : "+response+"(Api Error)");
                                    Logger.Debug("API", "Error - Returned HTTP Error " + response);
                                }else {

                                    Logger.Error("API", " Error In API Response: " + throwable.getMessage());
                                    lblError.setText("Failed : "+throwable.getMessage()+"(Api Error)");
                                }
                                playError();
                            }));
        } catch (Throwable e) {
            lblError.setTextColor(ColorRed);
            lblError.setText("Failed: "+ e.getMessage() +" (Exception)");

            playError();
            Logger.Error("API", "AssignBinToLocation  Error Connecting: " + e.getMessage());


        }
    }










    @RequiresApi(api = Build.VERSION_CODES.M)
    public void PostRFID(ArrayList<AuditRFIDModelPostItems> Items) {
        try {

            ProceedRFIDToAPI(Items);
            RefreshView();
            //Assign adapter to ListView
            General1.getGeneral(this).InRFID=false;
        }
        catch (Exception ex){
            log(ex.getMessage());
        }
    }


    String ValidatedBoxNumber="";
    String ValidatedBoxRFID="";
    Integer ValidatedAuditID=0;
    @SuppressLint("ResourceAsColor")
    public void ProceedSuccessScan(Integer AuditID, String BoxNumber, String BoxRFID){
        Log.i("ProceedSuccessScan","ProceedSuccessScan "+BoxNumber);
        ValidatedBoxNumber=BoxNumber;
        ValidatedBoxRFID=BoxRFID;
        ValidatedAuditID=AuditID;
        IsRFIDConnected=true;
        runOnUiThread(()->{
            txtScanRFIDScan.setText(BoxNumber);
            txtScanRFIDScan.setEnabled(false);
            btnScanRFIDStart.setText(PAUSE);
            lblError.setTextColor(R.color.green);
            lblError.setText("Box Loaded");
        });



        ReadMultiRFIDV2();
    }
    AuditRFIDModel auditRFIDModel=null;



    public  String TagModelKey(Tag_Model model){
        if(model._EPC.equals("E28068940000400C86079169")){
            return "30342CBD280E70D0A43701F3-E280689020004006CF0064AE";
        }
        if(model._EPC!=null && model._TID!=null && !model._EPC.isEmpty() && !model._TID.isEmpty())
            return model._EPC +"-"+ model._TID;
        return "";
    }

    Integer totalReadCount=0;

    public String EPCToUPC(String EPC){
        String Barcode=EPC.substring(3,16).toUpperCase();
        Boolean IsNumeric=true;

        if(     Barcode.endsWith("A")||
                Barcode.endsWith("B")||
                Barcode.endsWith("C")||
                Barcode.endsWith("D")||
                Barcode.endsWith("E")||
                Barcode.endsWith("F")
        )
            IsNumeric=false;
        if(Barcode.startsWith("0")&& IsNumeric && Barcode.length()>12)
            Barcode=Barcode.substring(1);

        return  Barcode;
    }






























    private void RefreshView(){
        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                try {
                    Integer index=0;

                    txtScanRFIDScan.setShowSoftInputOnFocus(false);
                    txtScanRFIDScan.requestFocus();
                    txtScanRFIDScan.setShowSoftInputOnFocus(false);
                    if(!IsRFIDConnected)
                    {
                        RFIDUPCView.add(0,"Loading Box..");
                        if(adapter!=null)
                            adapter.notifyDataSetChanged();
                        return;
                    }
                    RFIDUPCView.clear();
                    for (String message:RFIDMessages.stream().limit(10).collect(Collectors.toList()))
                    {
                        RFIDUPCView.add(0,message);
                    }

                    if(auditRFIDModel!=null) {
                        String Count ;
                        String Expected;
                        String Matched;
                        String Missed ;
                        String Added ;
                        synchronized (hmList_Lock){

                            ArrayList<String> list = GetRFIDList();
                            Count = list==null?"--":list.size()+"";
                            list=GetExpected();
                            Expected = list==null?"--":list.size()+"";
                            list=GetMatched();
                            Matched = list==null?"--":list.size()+"";
                            list=GetMissing();
                            Missed = list==null?"--":list.size()+"";
                            list=GetAdded();
                            Added =  list==null?"--":list.size()+"";
                        }


                        btnFoundedRFID.setText("RFID:"+ Count);
                        btnMatchedRFID.setText("Matched:"+ Matched);
                        btnExpectedRFID.setText("Expected:"+ Expected);
                        btnMissingRFID.setText("Missing:"+ Missed);
                        btnAddedRFID.setText("Added:"+ Added);

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
                            String txt=textView.getText().toString();
                            if(txt.startsWith("RFID Count:")){
                                textView.setTextSize(25);
                            }
                            return textView;
                        };
                    };
                    if(adapter!=null){
                        mLstView.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                    }

                }
                catch (Exception ex){
                    try{
                        log(" Error: "+ex.getMessage());
                        lblError.setText(ex.getMessage());
                        lblError.setTextColor(ColorRed);

                    }
                    catch (Exception e){

                    }

                }

            }


        });

    }



    private void setMainBackIcon(){
        if (mFilterModuleArray.size() == 0){
            mNotBluetooth.setVisibility(View.VISIBLE);
        }else {
            mNotBluetooth.setVisibility(View.GONE);
        }
    }
    private void initPermission(){
        PermissionUtil.requestEach(BBBAuditActivity.this, new PermissionUtil.OnPermissionListener() {


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

                        if (Analysis.isOpenGPS(BBBAuditActivity.this))
                            refresh();
                        else
                            startLocation();
                    }
                },1000);

            }
        }, PermissionUtil.LOCATION);
    }
    private Object beep_Lock = new Object();
    private void startLocation(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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
        PauseRFID();
        super.onPause();
        //退出这个界面，或是返回桌面时，停止扫描

    }


    @Override
    public void onBackPressed() {
        ContinueRead=false;
        checkingBoxNumber=false;
        PauseRFID();
        try {
            refreshViewTimer.cancel();
            this.disposable.dispose();
        }
        catch (Exception e){
            log(e.getMessage());
        }
        super.onBackPressed();
    }

    private void PauseRFID(){
        ContinueRead=false;
        int[] rfidDevices = null;
        if (RFIDGroup1 != null && !RFIDGroup1.isEmpty() && !RFIDGroup1.trim().isEmpty()) {
            rfidDevices = Arrays.stream(RFIDGroup1.split(",")).mapToInt(Integer::parseInt).toArray();
            //  ReadRfid(rfidDevices, Integer.valueOf(RFIDGrp1ReadTime1));
        }
        if(rfidDevices!=null)
            for (Integer i:rfidDevices) {
                Pingpong_Stop(i-1);
            }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(IsReading()){
            ReadMultiRFIDV2();
        }
    }


    private final String CONNECTED = "CONNECTED",CONNECTING = "CONNECTING",DISCONNECT = "DISCONNECT";
    private void setState(String state){
        switch (state){
            case CONNECTED://连接成功
                mTitle.updateRight(CONNECTED);
                mErrorDisconnect = null;
                break;

            case CONNECTING://连接中
                mTitle.updateRight(CONNECTING);
                mTitle.updateLoadingState(true);
                break;

            case DISCONNECT://连接断开
                mTitle.updateRight(DISCONNECT);
                break;
        }
    }
    //设置头部
    private void setTitle() {
        mTitle = new DefaultNavigationBar
                .Builder(this,(ViewGroup)findViewById(R.id.activity_scan_rfid))
                .setLeftText("Slid Audit")
                .hideLeftIcon()
                .setRightIcon()
                .setLeftClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mStartDebug % 4 ==0){
                            startActivity(DebugActivity.class);
                        }
                        mStartDebug++;
                    }
                })
                .setRightClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (Build.VERSION.SDK_INT< Build.VERSION_CODES.LOLLIPOP){
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
    private void setPopWindow(View v){
        new PopWindowMain(v, BBBAuditActivity.this, new PopWindowMain.DismissListener() {
            @Override
            public void onDismissListener(boolean resetEngine) {//弹出窗口销毁的回调
                mTitle.updateRightImage(false);
                if (resetEngine){//更换搜索引擎，重新搜索
                    refresh();
                }
            }
        });
    }

    //设置点击事件

    private Disposable disposable;








    int ColorGreen=Color.parseColor("#52ac24");
    int ColorRed=Color.parseColor("#ef2112");




    @ViewById(R.id.lblError)
    private TextView lblError;

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

    @ViewById(R.id.LstView)
    private ListView mLstView;



    @ViewById(R.id.txtScanRFIDScan)
    private EditText txtScanRFIDScan;

    private DefaultNavigationBar mTitle;

    private Storage mStorage;

    private List<DeviceModule> mModuleArray = new ArrayList<>();
    private List<DeviceModule> mFilterModuleArray = new ArrayList<>();

    private int  RFIDBondNbofReads=1;

    private int mStartDebug = 1;
    private String RFIDName ="";
    private String RFIDMac ="";
    private String WeightMac ="";
    private String IPAddress ="";
    private int UserID=-1;

    private String RFIDName2 ="";
    private String RFIDMac2 ="";
    private String RFIDName3 ="";
    private String RFIDMac3 ="";
    private String RFIDName4 ="";
    private String RFIDMac4 ="";
    private String RFIDName5 ="";
    private String RFIDMac5 ="";
    private String RFIDName6 ="";
    private String RFIDMac6 ="";

    private String RFIDGroup1="";
    private String RFIDGroup2="";
    private String RFIDGroup3="";
    private String RFIDGroup4="";
    private String RFIDGroup5="";
    private String RFIDGroup6="";


    private String RFIDGrp1ReadTime1="";
    private String RFIDGrp1ReadTime2="";
    private String RFIDGrp1ReadTime3="";
    private String RFIDGrp1ReadTime4="";
    private String RFIDGrp1ReadTime5="";
    private String RFIDGrp1ReadTime6="";



    @Override
    protected void onDestroy() {
        ContinueRead=false;
        checkingBoxNumber=false;
        PauseRFID();
        try {
            refreshViewTimer.cancel();
            this.disposable.dispose();
        }catch(Exception ex){
            log(ex.getMessage());
        }
        super.onDestroy();
    }
    public void RefreshSettings(){
        GetBT4DeviceStrList();
        mStorage = new Storage(this);//sp存储
        RFIDMac= mStorage.getDataString("RFIDMac",  "");
        RFIDMac2=mStorage.getDataString("RFIDMac2","");
        RFIDMac3=mStorage.getDataString("RFIDMac3","");
        RFIDMac4=mStorage.getDataString("RFIDMac4","");
        RFIDMac5=mStorage.getDataString("RFIDMac5","");
        RFIDMac6=mStorage.getDataString("RFIDMac6","");

        RFIDGroup1=mStorage.getDataString("RFIDGroup1","1");
        RFIDGroup2=mStorage.getDataString("RFIDGroup2","2");
        RFIDGroup3=mStorage.getDataString("RFIDGroup3","3");
        RFIDGroup4=mStorage.getDataString("RFIDGroup4","4");
        RFIDGroup5=mStorage.getDataString("RFIDGroup5","5");
        RFIDGroup6=mStorage.getDataString("RFIDGroup6","6");

        RFIDGrp1ReadTime1=mStorage.getDataString("RFIDGrp1ReadTime1","300");
        RFIDGrp1ReadTime2=mStorage.getDataString("RFIDGrp1ReadTime2","300");
        RFIDGrp1ReadTime3=mStorage.getDataString("RFIDGrp1ReadTime3","300");
        RFIDGrp1ReadTime4=mStorage.getDataString("RFIDGrp1ReadTime4","300");
        RFIDGrp1ReadTime5=mStorage.getDataString("RFIDGrp1ReadTime5","300");
        RFIDGrp1ReadTime6=mStorage.getDataString("RFIDGrp1ReadTime6","300");



        WeightMac=mStorage.getDataString("WeightMac","58:DA:04:A4:50:14");
        IPAddress=mStorage.getDataString("IPAddress","192.168.10.82");
        UserID=mStorage.getDataInt("UserID",-1);
        //RFIDName=GetBluetoothDeviceNameFromMac(RFIDMac);
        RFIDName=GetBluetoothDeviceNameFromMac(RFIDMac);
        RFIDName2=GetBluetoothDeviceNameFromMac(RFIDMac2);
        RFIDName3=GetBluetoothDeviceNameFromMac(RFIDMac3);
        RFIDName4=GetBluetoothDeviceNameFromMac(RFIDMac4);
        RFIDName5=GetBluetoothDeviceNameFromMac(RFIDMac5);
        RFIDName6=GetBluetoothDeviceNameFromMac(RFIDMac6);
    }

    public  void ShowAlert(ArrayList<String>Data){

    }


    private Boolean IsReading(){
        return btnScanRFIDStart.getText().equals(PAUSE);
    }
    String PAUSE="Pause";
    String START="Start";
    private void RestartScreen(){
        PauseRFID();
        lblError.setTextColor(ColorGreen);
        lblError.setText("Done");
        ValidatedBoxNumber="";
        ValidatedBoxRFID="";
        ValidatedAuditID=0;
        auditRFIDModel=null;
        PauseRFID();
        txtScanRFIDScan.setText("");
        txtScanRFIDScan.setEnabled(true);
        txtScanRFIDScan.requestFocus();

        btnFoundedRFID.setText("RFID");
        btnMatchedRFID.setText("Matched");
        btnExpectedRFID.setText("Expected");
        btnMissingRFID.setText("Missing");
        btnAddedRFID.setText("Added");

        RFIDMessages.clear();
        PendingItems.clear();
        ProcessedItems.clear();
        RFIDUPCView.clear();
        RFIDUPC.clear();
        hmList.clear();
        boxMap.clear();
    }

    private void ReadMultiRFIDV2() {
        if(!IsRFIDConnected)
            return;;
        ContinueRead=true;
        if(!IsReading())
            return;


        try {
            int[] rfidDevices = null;
            LibGeneral.AppStage = "rfidDevices1" + RFIDGroup1;
            if (RFIDGroup1 != null && !RFIDGroup1.isEmpty() && !RFIDGroup1.trim().isEmpty()) {
                rfidDevices = Arrays.stream(RFIDGroup1.split(",")).mapToInt(Integer::parseInt).toArray();
                ReadRfid(rfidDevices, Integer.valueOf(RFIDGrp1ReadTime1));
            }
        }
        catch (Exception ex){
            LogError("ReadRFID:"+ex.getMessage());
        }
        finally { }
    }

    private  int GetEPC_6C(Integer Index)
    {
        String RFID="";
        switch(Index) {
            case 0:
                RFID=RFIDName;
                break;
            case 1:
                RFID=RFIDName2;
                break;
            case 2:
                RFID=RFIDName3;
                break;
            case 3:
                RFID=RFIDName4;
                break;
            case 4:
                RFID=RFIDName5;
                break;
            case 5:
                RFID=RFIDName6;
                break;
            default:
                break;
        }
        int ret=-1;
        if(!RFIDReader.HP_CONNECT.containsKey(RFID))
            return ret;
        ret = RFIDReader._Tag6C.GetEPC_TID(RFID,1, eReadType.Inventory);
        return  ret;
    }
    private  int GetEPC_6B(Integer Index)
    {
        String RFID="";
        switch(Index) {
            case 0:
                RFID=RFIDName;
                break;
            case 1:
                RFID=RFIDName2;
                break;
            case 2:
                RFID=RFIDName3;
                break;
            case 3:
                RFID=RFIDName4;
                break;
            case 4:
                RFID=RFIDName5;
                break;
            case 5:
                RFID=RFIDName6;
                break;
            default:
                break;
        }
        int ret=-1;
        if(!RFIDReader.HP_CONNECT.containsKey(RFID))
            return ret;
        ret = RFIDReader._Tag6B.Get6B(RFID, 1, eReadType.Inventory.GetNum(), 0);
        return  ret;
    }

    List<BluetoothDevice> BluetoothDeviceList = new ArrayList();
    List<String> BluetoothDevicelistStr = new ArrayList();
    List<String> BluetoothDevicelistMac = new ArrayList();

    @SuppressLint("MissingPermission")
    public List<String> GetBT4DeviceStrList() {

        BluetoothDeviceList.clear();
        BluetoothDevicelistStr.clear();
        BluetoothDevicelistMac.clear();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            Iterator var1 = pairedDevices.iterator();

            while(var1.hasNext()) {
                BluetoothDevice device = (BluetoothDevice)var1.next();
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
    private void LogError(String str){
        Logging.LogError(getApplicationContext(),str);
    }
    private void LogDebug(String str){
        Logging.LogDebug(getApplicationContext(),str);
    }





    private  ArrayList<String> RFIDMessages=new ArrayList();
    @SuppressLint("ResourceAsColor")
    private void initRFID() {
        try {
            String Connection = "";
            RFIDReader.CloseAllConnect();
            //Thread.sleep(200);
            //Thread.sleep(500);
            RFIDReader.GetBT4DeviceStrList();
            if (RFIDName != null && !RFIDName.isEmpty()){
                if (!RFIDReader.CreateBT4Conn(RFIDName, this)) {
                    LogError("RFID Error Connecting to " + RFIDMac);
                    // lblError.setTextColor(ColorRed);
                    //lblError.setText("Error connecting to RFID1");
                } else {
                    Connection = "RFID1-" + RFIDMac + " Connected\n";
                    LogDebug("RFID1-" + RFIDMac + " Connected\n");
                    RFIDReader._Config.SetReaderAutoSleepParam(RFIDName, false, "");
//                    HashMap<Integer, Integer> dicPower = new HashMap<Integer,
//                            Integer>();
//                    dicPower.put(1, 30);
//                    if(RFIDReader._Config. SetANTPowerParam (RFIDName, dicPower) != 0){
//                        //Toast.makeText(getApplicationContext(),"Set Power failed",Toast.LENGTH_SHORT).show();
//                        log("Set Power failed");
//                        Log.i("Set-Power","Set Power failed");
//
//                    }else{
//                        // Toast.makeText(getApplicationContext(),"Set Power OK",Toast.LENGTH_SHORT).show();
//                        log("Set Power OK");
//                        Log.i("Set-Power","Set Power success");
//                    }


                }
            }
            if (RFIDName2 != null && !RFIDName2.isEmpty()) {
                if (!RFIDReader.CreateBT4Conn(RFIDName2, this)) {
                    LogError("RFID Error Connecting to " + RFIDMac2);
                    //lblError.setTextColor(ColorRed);
                    //lblError.setText("Error connecting to RFID2");
                } else {
                    Connection += "RFID2-" + RFIDMac2 + "Connected\n";
                    LogDebug("RFID2-" + RFIDMac2 + "Connected\n");
                }
            }
            if (RFIDName3 != null && !RFIDName3.isEmpty()) {
                if (!RFIDReader.CreateBT4Conn(RFIDName3, this)) {
                    LogError("RFID Error Connecting to " + RFIDMac3);
                    //lblError.setTextColor(ColorRed);
                    //lblError.setText("Error connecting to RFID3");
                } else {
                    Connection += "RFID3-" + RFIDMac3 + "Connected\n";
                    LogDebug("RFID3-" + RFIDMac3 + "Connected\n");
                }
            }
            if (RFIDName4 != null && !RFIDName4.isEmpty()) {
                if (!RFIDReader.CreateBT4Conn(RFIDName4, this)) {
                    LogError("RFID Error Connecting to " + RFIDMac4);
                    //lblError.setTextColor(ColorRed);
                    //lblError.setText("Error connecting to RFID4");
                } else {
                    Connection += "RFID4-" + RFIDMac4 + "Connected\n";
                    LogDebug("RFID4-" + RFIDMac4 + "Connected\n");
                }
            }
            if (RFIDName5 != null && !RFIDName5.isEmpty()) {
                if (!RFIDReader.CreateBT4Conn(RFIDName5, this)) {
                    LogError("RFID Error Connecting to " + RFIDMac5);
                    // lblError.setTextColor(ColorRed);
                    //lblError.setText("Error connecting to RFID5");
                } else {
                    Connection += "RFID5-" + RFIDMac5 + "Connected\n";
                    LogDebug("RFID5-" + RFIDMac5 + "Connected\n");
                }
            }
            if (RFIDName6 != null && !RFIDName6.isEmpty()) {
                if (!RFIDReader.CreateBT4Conn(RFIDName6, this)) {
                    LogError("RFID Error Connecting to " + RFIDMac6);
                    //lblError.setTextColor(ColorRed);
                    //lblError.setText("Error connecting to RFID6");
                } else {
                    Connection += "RFID6-" + RFIDMac6 + "Connected\n";
                    LogDebug("RFID6-" + RFIDMac6 + "Connected\n");
                }
            }
            int RFIDCount=RFIDReader.HP_CONNECT.size();
            lblError.setTextColor(ColorGreen);
            lblError.setText(String.valueOf(RFIDCount)+"Connected RFID Devices:\n"+Connection);
            HashMap<String, BaseConnect>  lst=RFIDReader.HP_CONNECT;
            if(lst.keySet().stream().count()>0){
                IsRFIDConnected=true;
            }
            else{

            }
        }
        catch ( Exception ex){
            lblError.setTextColor(ColorRed);
            lblError.setText(ex.getMessage());
        }
    }
    boolean IsRFIDConnected=false;
    public void Pingpong_Stop(Integer Index) {
        try {
            String RFID="";
            switch(Index) {
                case 0:
                    RFID=RFIDName;
                    break;
                case 1:
                    RFID=RFIDName2;
                    break;
                case 2:
                    RFID=RFIDName3;
                    break;
                case 3:
                    RFID=RFIDName4;
                    break;
                case 4:
                    RFID=RFIDName5;
                    break;
                case 5:
                    RFID=RFIDName6;
                    break;
                default:
                    break;
            }
            RFIDReader._Config.Stop(RFID);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public void PingPong_Read(Integer Index) {


        Helper_ThreadPool.ThreadPool_StartSingle(new Runnable() {
            @Override
            public void run() {
                try {
                    String rt = "";
                    if (PublicData._IsCommand6Cor6B.equals("6C")) {// read 6c tags
                        GetEPC_6C(Index);
                    } else {// read 6b tags
                        GetEPC_6B(Index);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void ReadRfid(int[] Indexes, int ReadTimeInms){
        try {
            if(!ContinueRead)
                return;

            boolean FoundConnected=false;
            for (Integer i:Indexes) {
                String RFID="";
                switch(i-1) {
                    case 0:
                        RFID=RFIDName;
                        break;
                    case 1:
                        RFID=RFIDName2;
                        break;
                    case 2:
                        RFID=RFIDName3;
                        break;
                    case 3:
                        RFID=RFIDName4;
                        break;
                    case 4:
                        RFID=RFIDName5;
                        break;
                    case 5:
                        RFID=RFIDName6;
                        break;
                    default:
                        break;
                }
                if(RFIDReader.HP_CONNECT.containsKey(RFID)){
                    FoundConnected=true;
                    break;
                }
            }

            if(FoundConnected){
                for (Integer i:Indexes) {
                    PingPong_Read(i-1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {

        }

    }
    private HashMap<String, Tag_Model> hmList = new HashMap<String, Tag_Model>();
    private Object hmList_Lock = new Object();
    @SuppressLint("MissingPermission")
    private String GetBluetoothDeviceNameFromMac(String Mac){
        if(!(Mac != null && !Mac.isEmpty())){
            return "";
        }
        GetBT4DeviceStrList();
        for(BluetoothDevice d : BluetoothDeviceList){
            if(d.getAddress() != null && d.getAddress().contains(Mac))
                return  d.getName();
            //something here
        }
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(Mac);
        if(device==null)
            return  "";
        return device.getName();
    }

    @SuppressLint("MissingPermission")
    private String GetBluetoothMacFromDeviceName(String DeviceName){
        GetBT4DeviceStrList();
        for(BluetoothDevice d : BluetoothDeviceList){
            if(d.getAddress() != null && d.getName().contains(DeviceName))
                return  d.getAddress();
            //something here
        }
        return "";
    }


    private void initView() {
        setMainBackIcon();

        adapter = new ArrayAdapter(getApplicationContext(),
                android.R.layout.simple_list_item_1, android.R.id.text1, RFIDUPCView) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setTextSize(12);
                return textView;
            };
        };
        // Assign adapter to ListView
        if(adapter !=null){
            adapter.notifyDataSetChanged();
            mLstView.setAdapter(adapter);

        }

    }
    ArrayAdapter  adapter=null;
    //初始化下拉刷新

    private void restart(){
        WeightValue=0;
        WeightValueCount=0;
        General1.getGeneral(this).InRFID=false;
    }
    //刷新的具体实现
    private void refresh(){
        if (false){
            mModuleArray.clear();
            mFilterModuleArray.clear();
            mTitle.updateLoadingState(true);
            if(adapter!=null)
                 adapter.notifyDataSetChanged();
        }

    }

    //根据条件过滤列表，并选择是否更新列表

    private List<DeviceModule> modules;




    public static final int FRAGMENT_STATE_DATA = 0x06;
    public static final int FRAGMENT_STATE_SERVICE_VELOCITY = 0x13;//读取实时速度
    public static final int FRAGMENT_STATE_NUMBER = 0x07;
    public static final int FRAGMENT_STATE_CONNECT_STATE = 0x08;
    public static final int FRAGMENT_STATE_SEND_SEND_TITLE = 0x09;
    public static final int FRAGMENT_STATE_LOG_MESSAGE = 0x011;
    private IMessageInterface mMessage,mLog;
    private Boolean isConnected=false;
    private float WeightValue=0;
    private int WeightValueCount=0;
    ArrayList<String> RFIDUPC=new ArrayList<>();
    ArrayList<String> RFIDUPCView=new ArrayList<>();
    private DeviceModule mErrorDisconnect;



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
}