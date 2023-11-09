package com.hc.mixthebluetooth.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.hc.basiclibrary.ioc.ViewById;
import com.hc.basiclibrary.permission.PermissionUtil;
import com.hc.basiclibrary.titleBasic.DefaultNavigationBar;
import com.hc.basiclibrary.viewBasic.BasActivity;
import com.hc.basiclibrary.viewBasic.LibGeneral;
import com.hc.basiclibrary.viewBasic.tool.IMessageInterface;
import com.hc.bluetoothlibrary.DeviceModule;
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
import com.util.General;
import com.util.Logging;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import Remote.APIClient;
import com.hc.mixthebluetooth.Remote.Routes.BasicApi;
import io.reactivex.disposables.Disposable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
/*
  特别说明：HC蓝牙助手是广州汇承信息科技有限公司独自研发的手机APP，方便用户调试蓝牙模块。
  本软件提供代码和注释，免费给购买汇承蓝牙模块的用户学习和研究，但不能用于商业开发，
  最终解析权在广州汇承信息科技有限公司。
  :)
  **/

/**
 * @author 广州汇承信息科技有限公司
 * @data: 2020-07-21
 * @version: V1.1
 */
public class ScanRFIDActivity extends BasActivity implements
        IAsynchronousMessage {


    @ViewById(R.id.lblError)
    private TextView lblError;


    @ViewById(R.id.main_back_not)
    private LinearLayout mNotBluetooth;

    @ViewById(R.id.btnScanRFIDGenerateRFID)
    private Button btnScanRFIDGenerateRFID;

    @ViewById(R.id.btnScanRFIDDone)
    private Button btnScanRFIDDone;

    @ViewById(R.id.RFIDLOTBondView)
    private ListView mRFIDLotBondView;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_rfid);
        //设置头部
        setTitle();
        setContext(this);

        txtScanRFIDScan.setShowSoftInputOnFocus(false);
        txtScanRFIDScan.requestFocus();
        txtScanRFIDScan.setShowSoftInputOnFocus(false);




     //   txtScanRFIDScan.addTextChangedListener(new TextWatcher() {
     //       @Override
     //       public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//
     //       @Override
     //       public void onTextChanged(CharSequence s, int start, int before, int count) {}
//
     //       @Override
     //       public void afterTextChanged(Editable s) {
     //           if (txtScanRFIDScan.getText().toString().isEmpty()) {
     //               return;
     //           }
     //           if (txtScanRFIDScan.length() <7  || txtScanRFIDScan.length() > 10) {
     //               txtScanRFIDScan.setText("");
     //               return;
     //           }
     //           InitRFIDApi(txtScanRFIDScan.getText().toString());
     //       }
     //   });


        new CountDownTimer(1000, 1000)
        {
            public void onTick(long l) {

            }
            public void onFinish()
            {

                Log.i("AH-Log",RFIDName);
                RFIDReader.StartBluetoothDeviceScan(RFIDName);
            }
        }.start();



    }
    @Override
    protected void onDestroy() {
        ContinueRead=false;
        try {
            this.disposable.dispose();
        }catch(Exception ex){
            log(ex.getMessage());
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        ContinueRead=false;
        try {
            this.disposable.dispose();
        }catch(Exception ex){
            log(ex.getMessage());
        }
        super.onBackPressed();
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
            //初始化单例模式中的蓝牙扫描回调

            //初始化权限
            initPermission();

            //初始化View
            initView();

            //
            //初始化下拉刷新

            new CountDownTimer(100000, 1000)
            {
                public void onTick(long l) {
                    RefreshView();
                }
                public void onFinish()
                {
                    //Code hear
                    start();
                }
            }.start();
            new CountDownTimer(200, 200)
            {
                public void onTick(long l) {

                }
                public void onFinish()
                {
                    initRFID();
                }
            }.start();
            btnScanRFIDGenerateRFID.setOnClickListener(view -> {
                PauseRFID();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss.SSS", Locale.getDefault());
                String RFID ="E2"+ sdf.format(new Date());
                RFIDUPC.add(RFID);
                PromptInput(RFID);
            });

            btnScanRFIDDone.setOnClickListener(view -> {
                RestartScreen();
            });
        } catch (Exception ex){
            lblError.setText(ex.getMessage());
            lblError.setTextColor(ColorRed);
        }

    }
    private boolean ContinueRead=true;
    private void RestartScreen(){
        PauseRFID();
        lblError.setTextColor(ColorGreen);
        lblError.setText("Done");
        ValidatedLotNumber="";
        txtScanRFIDScan.setText("");
        txtScanRFIDScan.setEnabled(true);
        txtScanRFIDScan.requestFocus();
        RFIDMessages.clear();
        RFIDUPCView.clear();
        RFIDUPC.clear();
        hmList.clear();
    }

    private void ReadMultiRFIDV2() {
        if(!IsRFIDConnected)
            return;;
        ContinueRead=true;
        if(ValidatedLotNumber==null)
            return;
        if(ValidatedLotNumber.isEmpty())
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
    public List<String> GetBT4DeviceStrList() {

        BluetoothDeviceList.clear();
        BluetoothDevicelistStr.clear();
        BluetoothDevicelistMac.clear();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
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
                    Connection = "RFID1-" + RFIDMac + "Connected\n";
                    LogDebug("RFID1-" + RFIDMac + "Connected\n");
                    RFIDReader._Config.SetReaderAutoSleepParam(RFIDName, false, "");
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
                //ReadMultiRFIDV2();
            }
            else{
                //   initRFID();
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
        adapter.notifyDataSetChanged();
        mRFIDLotBondView.setAdapter(adapter);
    }
    ArrayAdapter  adapter=null;
    //初始化下拉刷新

    private void restart(){
        WeightValue=0;
        WeightValueCount=0;
        General.getGeneral(this).InRFID=false;
    }
    //刷新的具体实现
    private void refresh(){
        if (false){
            mModuleArray.clear();
            mFilterModuleArray.clear();
            mTitle.updateLoadingState(true);
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
    ArrayList<String> lstUPC=new ArrayList<>();
    ArrayList<String> RFIDUPCView=new ArrayList<>();
    private DeviceModule mErrorDisconnect;

    private void RefreshView(){
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Integer index=0;

                txtScanRFIDScan.setShowSoftInputOnFocus(false);
                txtScanRFIDScan.requestFocus();
                txtScanRFIDScan.setShowSoftInputOnFocus(false);
                if(!IsRFIDConnected)
                {
                    RFIDUPCView.add(0,"Connecting..");
                    adapter.notifyDataSetChanged();
                    return;
                }
                RFIDUPCView.clear();
                //String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                //for (String i:RFIDUPC) {
                //  index=index+1;
                // RFIDUPCView.add(0,index.toString()+". "+ i);
                //}
                //RFIDUPCView.add(0,"--RFID Requests--");
                for (String message:RFIDMessages.stream().limit(10).collect(Collectors.toList()))
                {
                    RFIDUPCView.add(0,message);
                }
                // RFIDUPCView.add(0,"--RFID Messages--");
                RFIDUPCView.add(0,"RFID Count:"+ RFIDUPC.size());
                //RFIDUPCView.add(0,"-----"+date+"-----");
                adapter = new ArrayAdapter(getApplicationContext(),
                        android.R.layout.simple_list_item_1, android.R.id.text1, RFIDUPCView) {

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView textView = (TextView) super.getView(position, convertView, parent);
                        textView.setTextSize(12);
                        String txt=textView.getText().toString();
                        if(txt.startsWith("RFID Count:")){
                            textView.setTextSize(50);
                        }
                        if(txt.startsWith("Success")||txt.startsWith("Valid")||txt.startsWith("RFID Count:")){
                            //textView.setBackgroundColor(ColorGreen);
                        }else{
                            textView.setBackgroundColor(ColorRed);
                            textView.setTextColor(ColorWhite);
                        }
                        return textView;
                    };
                };
                mRFIDLotBondView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }
        });

    }



    public void PromptInput(String RFID) {
        InUPCPrompt=true;
        AlertDialog.Builder alertdialog = new AlertDialog.Builder(ScanRFIDActivity.this);
        alertdialog.setTitle("Scan item UPC");
        final EditText lblRFID = new EditText(this);
        final EditText txtUPC = new EditText(this);


        lblRFID.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        txtUPC.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        lblRFID.setText(RFID);
        lblRFID.setEnabled(false);
        txtUPC.setHint("UPC");  //editbox1 hint
        txtUPC.setGravity(Gravity.CENTER); //editbox in center
        txtUPC.requestFocus();




        //set up in a linear layout
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(20, 20, 20, 20); //set margin

        LinearLayout lp = new LinearLayout(getApplicationContext());
        lp.setOrientation(LinearLayout.VERTICAL);
        lp.addView(lblRFID, layoutParams);
        lp.addView(txtUPC, layoutParams);
        alertdialog.setView(lp);
        alertdialog.setPositiveButton("OK", (dialogInterface, i) -> {
        });

        alertdialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //RestartScreen("Canceled", true);
                dialogInterface.dismiss();
            }
        });

        this.runOnUiThread(() -> {
            AlertDialog alert = alertdialog.create();
            alert.setCanceledOnTouchOutside(false);
            alert.show();

            alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean wantToCloseDialog = false;
                    boolean IsValid=false;
                    String UPCStr = txtUPC.getText().toString().trim();
                    String RFIDStr = lblRFID.getText().toString().trim();

                    if(UPCStr.isEmpty()||RFIDStr.isEmpty())
                        IsValid=false;
                    else
                        IsValid=true;
                    //Do stuff, possibly set wantToCloseDialog to true then...
                    if(IsValid){

                        lstUPC.add(UPCStr);
                        ProceedRFIDToAPI(RFID,UPCStr);
                        RefreshView();
                        General.getGeneral(getApplicationContext()).InRFID=false;
                        InUPCPrompt=false;
                        ResumeRFID();
                    }
                    if(IsValid)
                        alert.dismiss();

                    //else dialog stays open. Make sure you have an obvious way to close the dialog especially if you set cancellable to false.
                }
            });
            txtUPC.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (txtUPC.getText().toString().isEmpty()) {
                        return;
                    }
                    if(General.IsUPC(txtUPC.getText().toString())){
                        alert.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                    }
                    else{
                        txtUPC.setText("");
                        return;
                    }
                }
            } );
        });
        PauseRFID();
    }
    int ColorGreen=Color.parseColor("#52ac24");
    int ColorRed=Color.parseColor("#ef2112");
    int ColorWhite=Color.parseColor("#ffffff");
    boolean InUPCPrompt=false;
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void PostRFID(String RFID) {
        try {
            if(InUPCPrompt)
                return;
            if(RFID.toLowerCase().startsWith("bbb"))
                return;
            if(RFIDUPC.contains(RFID))
                return;
            //  PauseRFID();
            LogDebug("Post RFID UPC Bond:"+RFID);
            RFIDUPC.add(RFID);

            PromptInput(RFID);

            //lstUPC.add("");
            //ProceedRFIDToAPI(RFID,"");
            //RefreshView();
            //Assign adapter to ListView
            //General.getGeneral(this).InRFID=false;
        }
        catch (Exception ex){
            log(ex.getMessage());
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
                .setLeftText("LOT RFID Bond")
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
        new PopWindowMain(v, ScanRFIDActivity.this, new PopWindowMain.DismissListener() {
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


    //收藏窗口
    @SuppressLint("ResourceAsColor")
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void ProceedRFIDToAPI(String RFID,String UPC) {
        try
        {
            //lblError.setText("Trying to bond:"+RFID);
            //lblError.setTextColor(ColorGreen);
            BasicApi api = APIClient.INSTANCE.getInstance(IPAddress,false).create(BasicApi.class);
            Call<ResponseBody> call = api.RFIDLotBonding(UserID,ValidatedLotNumber,RFID,UPC);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    int statusCode = response.code();
                    String ErrorMsg = "";
                    if (response.body() != null) {
                        try {
                            ErrorMsg = response.body().string();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    ResponseBody errorBody = response.errorBody();

                    if(statusCode==500 ||  errorBody!=null){
                        try {
                            lblError.setText(errorBody.string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        lblError.setTextColor(ColorRed);
                        playError();
                    }
                    else if(ErrorMsg.isEmpty()){
                        playSuccess();
                        lblError.setText("Success!!");
                        lblError.setTextColor(ColorGreen);
                    }
                    else if(!ErrorMsg.isEmpty()){
                        LogDebug(ErrorMsg);
                        RFIDMessages.add(0,ErrorMsg);
                        if(ErrorMsg.startsWith("Success")){
                            playSuccess();
                            lblError.setTextColor(ColorGreen);
                        }

                        else{
                            playError();
                            lblError.setTextColor(ColorRed);
                        }
                        lblError.setText(ErrorMsg);
                        log(ErrorMsg);
                    }

                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    lblError.setTextColor(ColorRed);
                    lblError.setText(t.getMessage());
                    log(t.getMessage());
                }
            });
        }
        catch (Exception ex)
        {
            lblError.setTextColor(ColorRed);
            lblError.setText(ex.getMessage());
            throw(ex);
        } finally
        {

        }
    }
    String ValidatedLotNumber="";

    public void ProceedSuccessScan(String LotNumber){
        ValidatedLotNumber=LotNumber;
        IsRFIDConnected=true;
        txtScanRFIDScan.setEnabled(false);
        ReadMultiRFIDV2();
    }
    private void InitRFIDApi(String LotNumber) {
        try
        {
            //lblError.setText("Trying to bond:"+RFID);
            //lblError.setTextColor(ColorGreen);
            BasicApi api = APIClient.INSTANCE.getInstance(IPAddress,false).create(BasicApi.class);
            Call<ResponseBody> call = api.InitRFIDLotBonding(UserID,LotNumber);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    int statusCode = response.code();
                    String ErrorMsg = "";
                    if (response.body() != null) {
                        try {
                            ErrorMsg = response.body().string();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    ResponseBody errorBody = response.errorBody();

                    if(statusCode==500 ||  errorBody!=null){
                        try {
                            lblError.setText(errorBody.string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        lblError.setTextColor(ColorRed);
                    }
                    else if(ErrorMsg.isEmpty()){
                        ProceedSuccessScan(LotNumber);
                        lblError.setText("Valid Lot Number!!");
                        lblError.setTextColor(ColorGreen);
                    }
                    else if(!ErrorMsg.isEmpty()){
                        RFIDMessages.add(0,ErrorMsg);
                        if(ErrorMsg.startsWith("Valid")){
                            lblError.setTextColor(ColorGreen);
                            ProceedSuccessScan(LotNumber);
                        }
                        else
                            lblError.setTextColor(ColorRed);
                        lblError.setText(ErrorMsg);
                        log(ErrorMsg);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    lblError.setTextColor(ColorRed);
                    lblError.setText(t.getMessage());
                    log(t.getMessage());
                }
            });
        }
        catch (Exception ex)
        {
            lblError.setTextColor(ColorRed);
            lblError.setText(ex.getMessage());
            throw(ex);
        } finally
        {

        }
    }


    //设置列表的背景图片是否显示
    private void setMainBackIcon(){
        if (mFilterModuleArray.size() == 0){
            mNotBluetooth.setVisibility(View.VISIBLE);
        }else {
            mNotBluetooth.setVisibility(View.GONE);
        }
    }

    //初始化位置权限
    private void initPermission(){
        PermissionUtil.requestEach(ScanRFIDActivity.this, new PermissionUtil.OnPermissionListener() {


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

                        if (Analysis.isOpenGPS(ScanRFIDActivity.this))
                            refresh();
                        else
                            startLocation();
                    }
                },1000);

            }
        }, PermissionUtil.LOCATION);
    }
    private Object beep_Lock = new Object();
    //开启位置权限
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
    private void PauseRFID(){
        ContinueRead=false;
        int[] rfidDevices = null;
        if (RFIDGroup1 != null && !RFIDGroup1.isEmpty() && !RFIDGroup1.trim().isEmpty()) {
            rfidDevices = Arrays.stream(RFIDGroup1.split(",")).mapToInt(Integer::parseInt).toArray();
            ReadRfid(rfidDevices, Integer.valueOf(RFIDGrp1ReadTime1));
        }
        for (Integer i:rfidDevices) {
            Pingpong_Stop(i-1);
        }
    }
    private void ResumeRFID() {
        if(ValidatedLotNumber!=null && !ValidatedLotNumber.isEmpty()){
            ReadMultiRFIDV2();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        // ResumeRFID();

        //PingPong_Read();
        //退出这个界面，或是返回桌面时，停止扫描
    }

    public  String TagModelKey(Tag_Model model){
        if(model._EPC!=null && model._TID!=null && !model._EPC.isEmpty() && !model._TID.isEmpty())
            return model._EPC +"-"+ model._TID;
        return "";
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void OutPutTags(Tag_Model model) {
        try {

            synchronized (hmList_Lock) {


                if(TagModelKey(model).isEmpty() ||TagModelKey(model).toLowerCase().startsWith("bbb"))
                {

                }
                else if (hmList.containsKey(TagModelKey(model))) {

                    Tag_Model tModel = hmList.get(TagModelKey(model));
                    tModel._TotalCount++;
                    model._TotalCount = tModel._TotalCount;
                    LogDebug("EPC Read"+TagModelKey(model)+" count:" +String.valueOf(model._TotalCount));
                    hmList.remove(TagModelKey(model));
                    hmList.put(TagModelKey(model), model);
                    if(model._TotalCount % RFIDBondNbofReads==0 )
                        PostRFID(TagModelKey(model));
                }
                else {
                    LogDebug("EPC Read11"+TagModelKey(model)+" count:" +String.valueOf(model._TotalCount));
                    LogDebug("RFIDBondNbofReads:"+ RFIDBondNbofReads);
                    if(RFIDBondNbofReads<=1){

                        PostRFID(TagModelKey(model));
                    }
                    model._TotalCount = 1;
                    hmList.put(TagModelKey(model), model);
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
    Integer totalReadCount=0;
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
        Log.i("AH-Log","Length "+scandata.length);
    }
}