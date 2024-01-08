package com.hc.mixthebluetooth.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import com.google.gson.Gson;
import com.hc.basiclibrary.ioc.ViewById;
import com.hc.basiclibrary.permission.PermissionUtil;
import com.hc.basiclibrary.recyclerAdapterBasic.ItemClickListener;
import com.hc.basiclibrary.titleBasic.DefaultNavigationBar;
import com.hc.basiclibrary.viewBasic.BasActivity;
import com.hc.basiclibrary.viewBasic.LibGeneral;
import com.hc.basiclibrary.viewBasic.manage.ViewPagerManage;
import com.hc.basiclibrary.viewBasic.tool.IMessageInterface;
import com.hc.bluetoothlibrary.DeviceModule;
import com.hc.mixthebluetooth.R;
import com.hc.mixthebluetooth.activity.single.HoldBluetooth;
import com.hc.mixthebluetooth.activity.tool.Analysis;
import com.hc.mixthebluetooth.customView.PopWindowMain;
import com.hc.mixthebluetooth.fragment.FragmentLog;
import com.hc.mixthebluetooth.fragment.FragmentMessage;
import com.hc.mixthebluetooth.recyclerData.MainRecyclerAdapter;
import com.hc.mixthebluetooth.recyclerData.itemHolder.FragmentLogItem;
import com.hc.mixthebluetooth.recyclerData.itemHolder.FragmentMessageItem;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Model.FillPalleteRFIDModel;
import Remote.APIClient;
import com.hc.mixthebluetooth.Remote.Routes.BasicApi;
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
 * @version: V1.19
 */
public class FillPalleteActivity extends BasActivity implements
        IAsynchronousMessage {

    @ViewById(R.id.main_swipe)
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @ViewById(R.id.lblError)
    private TextView lblError;


    @ViewById(R.id.main_back_not)
    private LinearLayout mNotBluetooth;

    @ViewById(R.id.main_recycler)
    private RecyclerView mRecyclerView;


    @ViewById(R.id.WeightRecyclerView)
    private ListView mWeightListView;



    private MainRecyclerAdapter mainRecyclerAdapter;

    private DefaultNavigationBar mTitle;

    private Storage mStorage;

    private List<DeviceModule> mModuleArray = new ArrayList<>();
    private List<DeviceModule> mFilterModuleArray = new ArrayList<>();

    private HoldBluetooth mHoldBluetooth;

    private int mStartDebug = 1;
    private String RFIDName ="";
    private String RFIDMac ="";
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
    private String WeightMac ="";
    private String IPAddress ="";

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

    public void RefreshSettings(){
        GetBT4DeviceStrList();
        mStorage = new Storage(this);//sp存储
        RFIDMac= mStorage.getDataString("RFIDMac",  "00:15:83:5C:4D:65");
        RFIDMac2=mStorage.getDataString("RFIDMac2","00:15:83:5C:4D:86");
        RFIDMac3=mStorage.getDataString("RFIDMac3","00:15:83:5C:4D:09");
        RFIDMac4=mStorage.getDataString("RFIDMac4","00:15:83:5C:4D:61");
        RFIDMac5=mStorage.getDataString("RFIDMac5","00:15:83:5C:4D:61");
        RFIDMac6=mStorage.getDataString("RFIDMac6","00:15:83:5C:4D:61");

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
    private int UserID=-1;
    private List<String> WeightsLst=new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //设置头部
        setTitle();
        setContext(this);

        Thread.setDefaultUncaughtExceptionHandler((thread, thrown) -> {
            try {
                showError(thrown.getMessage());
                LogError(thrown.getMessage());

            } catch (Error err) {
                LogError(err.getMessage());
                Runtime.getRuntime().exit(-2);
            }
        });
    }
    private void showError(String Error) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
            builder.setMessage(Error)
                    .setPositiveButton("Close Error", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            // Create the AlertDialog object and return it
            builder.create();
            builder.show();
        }
        catch (Exception ex){
            Logging.LogError(getApplicationContext(),"showError:"+ ex.getMessage());
        }

    }

    public  void ThrowError() throws  Exception{
        throw new Exception("Thrown on Create");
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    @SuppressLint("ResourceAsColor")
    @Override
    public void initAll() {
try {
    RefreshSettings();
    //初始化单例模式中的蓝牙扫描回调
    initHoldBluetooth();

    //初始化权限
    initPermission();

    //初始化View
    initView();

    initRFID();
    //初始化下拉刷新
    initRefresh();
    //设置RecyclerView的Item的点击事件
    setRecyclerListener();
} catch (Exception ex){
    lblError.setText(ex.getMessage());
    lblError.setTextColor(R.color.red);
}

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
                    // lblError.setTextColor(R.color.red);
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
                    //lblError.setTextColor(R.color.red);
                    //lblError.setText("Error connecting to RFID2");
                } else {
                    Connection += "RFID2-" + RFIDMac2 + "Connected\n";
                    LogDebug("RFID2-" + RFIDMac2 + "Connected\n");
                }
            }
            if (RFIDName3 != null && !RFIDName3.isEmpty()) {
                if (!RFIDReader.CreateBT4Conn(RFIDName3, this)) {
                    LogError("RFID Error Connecting to " + RFIDMac3);
                    //lblError.setTextColor(R.color.red);
                    //lblError.setText("Error connecting to RFID3");
                } else {
                    Connection += "RFID3-" + RFIDMac3 + "Connected\n";
                    LogDebug("RFID3-" + RFIDMac3 + "Connected\n");
                }
            }
            if (RFIDName4 != null && !RFIDName4.isEmpty()) {
                if (!RFIDReader.CreateBT4Conn(RFIDName4, this)) {
                    LogError("RFID Error Connecting to " + RFIDMac4);
                    //lblError.setTextColor(R.color.red);
                    //lblError.setText("Error connecting to RFID4");
                } else {
                    Connection += "RFID4-" + RFIDMac4 + "Connected\n";
                    LogDebug("RFID4-" + RFIDMac4 + "Connected\n");
                }
            }
            if (RFIDName5 != null && !RFIDName5.isEmpty()) {
                if (!RFIDReader.CreateBT4Conn(RFIDName5, this)) {
                    LogError("RFID Error Connecting to " + RFIDMac5);
                    // lblError.setTextColor(R.color.red);
                    //lblError.setText("Error connecting to RFID5");
                } else {
                    Connection += "RFID5-" + RFIDMac5 + "Connected\n";
                    LogDebug("RFID5-" + RFIDMac5 + "Connected\n");
                }
            }
            if (RFIDName6 != null && !RFIDName6.isEmpty()) {
                if (!RFIDReader.CreateBT4Conn(RFIDName6, this)) {
                    LogError("RFID Error Connecting to " + RFIDMac6);
                    //lblError.setTextColor(R.color.red);
                    //lblError.setText("Error connecting to RFID6");
                } else {
                    Connection += "RFID6-" + RFIDMac6 + "Connected\n";
                    LogDebug("RFID6-" + RFIDMac6 + "Connected\n");
                }
            }
            int RFIDCount=RFIDReader.HP_CONNECT.size();
            lblError.setTextColor(R.color.green);
            lblError.setText(String.valueOf(RFIDCount)+"Connected RFID Devices:\n"+Connection);
            HashMap<String, BaseConnect>  lst=RFIDReader.HP_CONNECT;
        }
        catch ( Exception ex){
            lblError.setTextColor(R.color.red);
            lblError.setText(ex.getMessage());
        }
    }
    private void LogError(String str){
        Logging.LogError(getApplicationContext(),str);
    }
    private void LogDebug(String str){
        Logging.LogDebug(getApplicationContext(),str);
    }
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

    private HashMap<String, Tag_Model> hmList = new HashMap<String, Tag_Model>();
    private Object hmList_Lock = new Object();
    protected List<Map<String, Object>> GetData() {
        List<Map<String, Object>> rt = new ArrayList<Map<String, Object>>();
        synchronized (hmList_Lock) {
            // if(hmList.size() > 0){ //
            Iterator iter = hmList.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String key = (String) entry.getKey();
                Tag_Model val = (Tag_Model) entry.getValue();
                Map<String, Object> map = new HashMap<String, Object>();

                    map.put("EPC", val._EPC);
                    map.put("ReadCount", val._TotalCount);
                    rt.add(map);
                }
            }
        return rt;
    }
    private String GetBluetoothDeviceNameFromMac(String Mac){
        if(Mac==null)
            return "";
        if(Mac.trim().isEmpty())
            return "";
        if(Mac.trim().length()<8)
            return "";

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
    private void initHoldBluetooth() {
        GetBT4DeviceStrList();
        mHoldBluetooth = HoldBluetooth.getInstance();
        final HoldBluetooth.UpdateList updateList = new HoldBluetooth.UpdateList() {
            @Override
            public void update(boolean isStart,DeviceModule deviceModule) {

                if (isStart && deviceModule == null){//更新距离值
                    mainRecyclerAdapter.notifyDataSetChanged();
                    return;
                }

                if (isStart){
                    setMainBackIcon();
                    mModuleArray.add(deviceModule);
                    addFilterList(deviceModule,true);
                }else {
                    mTitle.updateLoadingState(false);
                }
            }

            @Override
            public void updateMessyCode(boolean isStart, DeviceModule deviceModule) {
                for(int i= 0; i<mModuleArray.size();i++){
                    if (mModuleArray.get(i).getMac().equals(deviceModule.getMac())){
                        mModuleArray.remove(mModuleArray.get(i));
                        mModuleArray.add(i,deviceModule);
                        upDateList();
                        break;
                    }
                }
            }
        };
        mHoldBluetooth.initHoldBluetooth(FillPalleteActivity.this,updateList);
    }

    private void initView() {
        setMainBackIcon();
        mainRecyclerAdapter = new MainRecyclerAdapter(this,mFilterModuleArray,R.layout.item_recycler_main);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mainRecyclerAdapter);
    }

    //初始化下拉刷新
    private void initRefresh() {
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {//设置刷新监听器
            @Override
            public void onRefresh() {
                mSwipeRefreshLayout.setRefreshing(false);
               // restart();
            }
        });
    }
    private void restart(){
        WeightValue=0;
        WeightValueCount=0;
        General.getGeneral(this).InRFID=false;
        upDateList();
    }
    //刷新的具体实现
    private void refresh(){
        if (mHoldBluetooth.scan(false)){
            mModuleArray.clear();
            mFilterModuleArray.clear();
            mTitle.updateLoadingState(true);
            mainRecyclerAdapter.notifyDataSetChanged();
        }
    }

    //根据条件过滤列表，并选择是否更新列表
    @SuppressLint("ResourceAsColor")
    private Boolean addFilterList(DeviceModule deviceModule, boolean isRefresh){
        try {
                 /*  if (mStorage.getData(PopWindowMain.NAME_KEY) && deviceModule.getName().equals("N/A")){
            return;
        }

        if (mStorage.getData(PopWindowMain.BLE_KEY) && !deviceModule.isBLE()){
            return;
        }

        if ((mStorage.getData(PopWindowMain.FILTER_KEY) || mStorage.getData(PopWindowMain.CUSTOM_KEY))
         && !deviceModule.isHcModule(mStorage.getData(PopWindowMain.CUSTOM_KEY),mStorage.getDataString(PopWindowMain.DATA_KEY))){
            return;
        }*/
            if(!deviceModule.getMac().equals(WeightMac)){
                return false;
            }
        /*deviceModule.isCollectName(FillPalleteRFIDActivity.this);
        mFilterModuleArray.add(deviceModule);
        if (isRefresh)
            mainRecyclerAdapter.notifyDataSetChanged();*/
            mHoldBluetooth.setDevelopmentMode(FillPalleteActivity.this);//设置是否进入开发模式
            mHoldBluetooth.connect(deviceModule);

            initFragment();
            initDataListener();
            return  true;
            //startActivity(CommunicationActivity.class);
        }catch (Exception ex){
            lblError.setTextColor(R.color.red);
            lblError.setText("Error connecting to Weight scale");
            return  false;
        }

    }
    private List<DeviceModule> modules;

    private Handler mFragmentHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            FragmentMessageItem item = (FragmentMessageItem) msg.obj;
            mHoldBluetooth.sendData(item.getModule(),item.getByteData().clone());
            return false;
        }
    });
    @ViewById(R.id.communication_fragment)
    private ViewPager mViewPager;
    private void initFragment() {
        ViewPagerManage manage = new ViewPagerManage(mViewPager);

        //获取Fragment的接口，方便操作数据
        mMessage = (IMessageInterface) manage.addFragment(new FragmentMessage());
        //传入Handler方便Fragment数据回传
        mMessage.setHandler(mFragmentHandler);

        //传过去头部，主要是为了获取连接状况
        manage.setDuration(400);//控制ViewPager速度，400ms
        mLog = (IMessageInterface) manage.addFragment(new FragmentLog());
        //mViewPager.setAdapter(manage.getAdapter());
        //mViewPager.setOffscreenPageLimit(4);
    }

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
private void ReadRfid(int[] Indexes, int ReadTimeInms){
    try {
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
        Thread.sleep(ReadTimeInms);
        }
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    finally {
        for (Integer i:Indexes) {
            Pingpong_Stop(i-1);
        }
    }

}
    public static String byteArr2HexStr(byte[] arrB) throws Exception {
        int iLen = arrB.length;
        StringBuffer sb = new StringBuffer(iLen * 2);
        for(int i = 0; i < iLen; ++i) {
            int intTmp;
            for(intTmp = arrB[i]; intTmp < 0; intTmp += 256) {
            }

            if (intTmp < 16) {
                sb.append("0");
            }
            sb.append(Integer.toString(intTmp, 16)+"-");
        }

        return sb.toString();
    }
    private DeviceModule mErrorDisconnect;
    private void initDataListener() {

        HoldBluetooth.OnReadDataListener dataListener = new HoldBluetooth.OnReadDataListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void readData(String mac, byte[] data) {
                LibGeneral.AppStage="readData";
                if (modules != null && modules.size()>0) {
                    String weightStr=mMessage.readData(FRAGMENT_STATE_DATA, modules.get(0), data);
                    if(!weightStr.isEmpty()){
                        try {
                            Float WeightVal = Float.parseFloat(weightStr);
                            if(Math.signum(WeightVal)==0){
                                restart();
                            }
                            else if (!General.getGeneral(getApplicationContext()).InRFID && WeightVal>0.15)
                            {
                                if (WeightVal.equals(WeightValue)) {
                                    WeightValueCount++;
                                } else if(WeightValueCount<6) {
                                    WeightValueCount = 1;
                                    WeightValue = WeightVal;
                                }
                                if (WeightValueCount == 6) {
                                    General.getGeneral(getApplicationContext()).InRFID = true;
                                    WeightsLst.clear();
                                    WeightsLst.add(0,WeightVal.toString());
                                    ArrayAdapter  adapter = new ArrayAdapter(getApplicationContext(),
                                            android.R.layout.simple_list_item_1, android.R.id.text1, WeightsLst) {
                                        @Override
                                        public View getView(int position, View convertView, ViewGroup parent) {
                                            TextView textView = (TextView) super.getView(position, convertView, parent);
                                            textView.setTextSize(12);
                                            return textView;
                                        };
                                    };
                                    ArrayList<String> lst=new ArrayList<String>();
                                    // Assign adapter to ListView
                                    adapter.notifyDataSetChanged();
                                    mWeightListView.setAdapter(adapter);
                                    hmList.clear();
                                    int[] rfidDevices=null;
                                    LibGeneral.AppStage="rfidDevices1"+RFIDGroup1;
                                    if (RFIDGroup1 != null && !RFIDGroup1.isEmpty() && !RFIDGroup1.trim().isEmpty()){
                                        rfidDevices= Arrays.stream(RFIDGroup1.split(",")).mapToInt(Integer::parseInt).toArray();
                                        ReadRfid(rfidDevices,Integer.valueOf(RFIDGrp1ReadTime1));
                                        adapter.insert("Read1",0);
                                    }
                                    LibGeneral.AppStage="rfidDevices2"+RFIDGroup2;
                                    if (RFIDGroup2 != null && !RFIDGroup2.isEmpty() && !RFIDGroup2.trim().isEmpty()) {
                                        rfidDevices = Arrays.stream(RFIDGroup2.split(",")).mapToInt(Integer::parseInt).toArray();
                                        ReadRfid(rfidDevices, Integer.valueOf(RFIDGrp1ReadTime2));
                                        adapter.insert("Read2", 0);
                                    }
                                    LibGeneral.AppStage="rfidDevices3"+RFIDGroup3;
                                    if (RFIDGroup3 != null && !RFIDGroup3.isEmpty() && !RFIDGroup3.trim().isEmpty()) {
                                        rfidDevices = Arrays.stream(RFIDGroup3.split(",")).mapToInt(Integer::parseInt).toArray();
                                        ReadRfid(rfidDevices, Integer.valueOf(RFIDGrp1ReadTime3));
                                        adapter.insert("Read3", 0);
                                    }
                                    LibGeneral.AppStage="rfidDevices4"+RFIDGroup4;
                                    if (RFIDGroup4 != null && !RFIDGroup4.isEmpty() && !RFIDGroup4.trim().isEmpty()) {
                                        rfidDevices = Arrays.stream(RFIDGroup4.split(",")).mapToInt(Integer::parseInt).toArray();
                                        ReadRfid(rfidDevices, Integer.valueOf(RFIDGrp1ReadTime4));
                                        adapter.insert("Read4", 0);
                                    }
                                    LibGeneral.AppStage="rfidDevices5"+RFIDGroup5;
                                    if (RFIDGroup5 != null && !RFIDGroup5.isEmpty() && !RFIDGroup5.trim().isEmpty()) {
                                        rfidDevices = Arrays.stream(RFIDGroup5.split(",")).mapToInt(Integer::parseInt).toArray();
                                        ReadRfid(rfidDevices, Integer.valueOf(RFIDGrp1ReadTime5));
                                        adapter.insert("Read5", 0);
                                    }
                                    LibGeneral.AppStage="rfidDevices6"+RFIDGroup6;
                                    if (RFIDGroup6 != null && !RFIDGroup6.isEmpty() && !RFIDGroup6.trim().isEmpty()) {
                                        rfidDevices = Arrays.stream(RFIDGroup6.split(",")).mapToInt(Integer::parseInt).toArray();
                                        ReadRfid(rfidDevices, Integer.valueOf(RFIDGrp1ReadTime6));
                                        adapter.insert("Read6", 0);
                                    }
                                    lst = new ArrayList<>(hmList.keySet());
                                    adapter.addAll(lst);

                                    adapter.insert("RFID Count:"+ lst.size(),0);
                                    adapter.notifyDataSetChanged();
                                    LibGeneral.AppStage="ProceedRFIDToAPI";
                                    ProceedRFIDToAPI();
                                    // Assign adapter to ListView
                                    General.getGeneral(getApplicationContext()).InRFID=false;
                                }
                            }
                        }
                        catch (Exception ex){
                            log(ex.getMessage());
                        }

                    }
                }
            }

            @Override
            public void reading(boolean isStart) {
                if (isStart)
                    mMessage.updateState(CommunicationActivity.FRAGMENT_STATE_1);
                else
                    mMessage.updateState(CommunicationActivity.FRAGMENT_STATE_2);
            }

            @Override
            public void connectSucceed() {
                modules = mHoldBluetooth.getConnectedArray();
                mMessage.readData(FRAGMENT_STATE_DATA, modules.get(0), null);
                isConnected=true;
                log("连接成功: "+modules.get(0).getName());
            }
            @Override
            public void errorDisconnect(final DeviceModule deviceModule) {//蓝牙异常断开
                if (mErrorDisconnect == null) {//判断是否已经重复连接
                    mErrorDisconnect = deviceModule;
                    if (mHoldBluetooth != null && deviceModule != null) {
                        mFragmentHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mHoldBluetooth.connect(deviceModule);
                                //设置正在连接状态
                            }
                        },2000);
                        return;
                    }
                }
                isConnected=false;//设置断开状态
                if (deviceModule != null)
                    toast("连接" + deviceModule.getName() + "失败，点击右上角的已断线可尝试重连", Toast.LENGTH_LONG);
                else
                    toast("连接模块失败，请返回上一个页面重连", Toast.LENGTH_LONG);
            }
            @Override
            public void readNumber(int number) {
                mMessage.readData(FRAGMENT_STATE_NUMBER, number, null);
            }

            @Override
            public void readLog(String className, String data, String lv) {
                //拿到日志
                if (mLog != null)
                    mLog.readData(FRAGMENT_STATE_LOG_MESSAGE,new FragmentLogItem(className,data,lv),null);
            }

            @Override
            public void readVelocity(int velocity) {
                if (mMessage != null)
                    mMessage.readData(FRAGMENT_STATE_SERVICE_VELOCITY,10,null);
            }

        };
        mHoldBluetooth.setOnReadListener(dataListener);
    }


    private final String CONNECTED = "CONNECTED",CONNECTING = "CONNECTING",DISCONNECT = "DISCONNECT";
    private void setState(String state){
        switch (state){
            case CONNECTED:
                mTitle.updateRight(CONNECTED);
                mErrorDisconnect = null;
                break;

            case CONNECTING:
                mTitle.updateRight(CONNECTING);
                mTitle.updateLoadingState(true);
                break;

            case DISCONNECT:
                mTitle.updateRight(DISCONNECT);
                break;
        }
    }

    private void setTitle() {
        mTitle = new DefaultNavigationBar
                .Builder(this,(ViewGroup)findViewById(R.id.main_name))
                .setLeftText("BOS RFID Scale")
                .hideLeftIcon()
                .setRightIcon()
                .setLeftClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int RFIDCount=RFIDReader.HP_CONNECT.size();
                        if (mStartDebug % 3 ==0){

                           lblError.setText(String.valueOf(RFIDCount)+"Connected RFID Devices!");

                            {
                                General.getGeneral(getApplicationContext()).InRFID = true;
                                WeightsLst.clear();
                                WeightsLst.add(0,String.valueOf(RFIDCount));

                                ArrayAdapter  adapter = new ArrayAdapter(getApplicationContext(),
                                        android.R.layout.simple_list_item_1, android.R.id.text1, WeightsLst) {
                                    @Override
                                    public View getView(int position, View convertView, ViewGroup parent) {
                                        TextView textView = (TextView) super.getView(position, convertView, parent);
                                        textView.setTextSize(12);

                                        return textView;
                                    }

                                    ;
                                };

                                // Assign adapter to ListView
                                adapter.notifyDataSetChanged();
                                mWeightListView.setAdapter(adapter);
                                List<String> lst=new ArrayList<String>();

                                hmList.clear();
                                int[] rfidDevices=null;
                                LibGeneral.AppStage="rfidDevices1"+RFIDGroup1;
                                if (RFIDGroup1 != null && !RFIDGroup1.isEmpty() && !RFIDGroup1.trim().isEmpty()){
                                    rfidDevices= Arrays.stream(RFIDGroup1.split(",")).mapToInt(Integer::parseInt).toArray();
                                    ReadRfid(rfidDevices,Integer.valueOf(RFIDGrp1ReadTime1));
                                    adapter.insert("Read1",0);
                                }
                                LibGeneral.AppStage="rfidDevices2"+RFIDGroup2;
                                if (RFIDGroup2 != null && !RFIDGroup2.isEmpty() && !RFIDGroup2.trim().isEmpty()) {
                                    rfidDevices = Arrays.stream(RFIDGroup2.split(",")).mapToInt(Integer::parseInt).toArray();
                                    ReadRfid(rfidDevices, Integer.valueOf(RFIDGrp1ReadTime2));
                                    adapter.insert("Read2", 0);
                                }
                                LibGeneral.AppStage="rfidDevices3"+RFIDGroup3;
                                if (RFIDGroup3 != null && !RFIDGroup3.isEmpty() && !RFIDGroup3.trim().isEmpty()) {
                                    rfidDevices = Arrays.stream(RFIDGroup3.split(",")).mapToInt(Integer::parseInt).toArray();
                                    ReadRfid(rfidDevices, Integer.valueOf(RFIDGrp1ReadTime3));
                                    adapter.insert("Read3", 0);
                                }
                                LibGeneral.AppStage="rfidDevices4"+RFIDGroup4;
                                if (RFIDGroup4 != null && !RFIDGroup4.isEmpty() && !RFIDGroup4.trim().isEmpty()) {
                                    rfidDevices = Arrays.stream(RFIDGroup4.split(",")).mapToInt(Integer::parseInt).toArray();
                                    ReadRfid(rfidDevices, Integer.valueOf(RFIDGrp1ReadTime4));
                                    adapter.insert("Read4", 0);
                                }
                                LibGeneral.AppStage="rfidDevices5"+RFIDGroup5;
                                if (RFIDGroup5 != null && !RFIDGroup5.isEmpty() && !RFIDGroup5.trim().isEmpty()) {
                                    rfidDevices = Arrays.stream(RFIDGroup5.split(",")).mapToInt(Integer::parseInt).toArray();
                                    ReadRfid(rfidDevices, Integer.valueOf(RFIDGrp1ReadTime5));
                                    adapter.insert("Read5", 0);
                                }
                                LibGeneral.AppStage="rfidDevices6"+RFIDGroup6;
                                if (RFIDGroup6 != null && !RFIDGroup6.isEmpty() && !RFIDGroup6.trim().isEmpty()) {
                                    rfidDevices = Arrays.stream(RFIDGroup6.split(",")).mapToInt(Integer::parseInt).toArray();
                                    ReadRfid(rfidDevices, Integer.valueOf(RFIDGrp1ReadTime6));
                                    adapter.insert("Read6", 0);
                                }
                                lst = new ArrayList<>(hmList.keySet());
                                adapter.addAll(lst);

                                adapter.insert("RFID Count:"+ lst.size(),0);
                                adapter.notifyDataSetChanged();
                                LogDebug("3 Click Items:"+gson.toJson(hmList));
                                // Assign adapter to ListView
                                General.getGeneral(getApplicationContext()).InRFID=false;
                            }
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
        new PopWindowMain(v, FillPalleteActivity.this, new PopWindowMain.DismissListener() {
            @Override
            public void onDismissListener(boolean resetEngine) {//弹出窗口销毁的回调
               upDateList();
               mTitle.updateRightImage(false);
                RefreshSettings();
               if (resetEngine){//更换搜索引擎，重新搜索
                   mHoldBluetooth.stopScan();
                   refresh();
               }
            }
        });
    }

    //设置点击事件
    private void setRecyclerListener() {
        mainRecyclerAdapter.setOnItemClickListener(new ItemClickListener() {
            @Override
            public void onItemClick(int position, View view) {
                log("viewId:"+view.getId()+" item_main_icon:"+R.id.item_main_icon);
                if (view.getId() == R.id.item_main_icon){

                }else {
                    mHoldBluetooth.setDevelopmentMode(FillPalleteActivity.this);//设置是否进入开发模式
                    mHoldBluetooth.connect(mFilterModuleArray.get(position));
                    startActivity(CommunicationActivity.class);
                }
            }
        });
    }


    //收藏窗口
    @SuppressLint("ResourceAsColor")
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void ProceedRFIDToAPI() {
        try
        {
            LogDebug("ProceedRFIDToAPI Items:"+gson.toJson(hmList));
            BasicApi api = APIClient.INSTANCE.getInstance(IPAddress,false).create(BasicApi.class);

            FillPalleteRFIDModel pallete=new FillPalleteRFIDModel("",1, hmList.keySet().toArray(new String[hmList.size()]),WeightValue,WeightMac,RFIDMac,General.getMacAddress());
            Call<ResponseBody> call = api.ProceedFillPalleteRFID(pallete);
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
                        lblError.setTextColor(R.color.red);
                    }
                    else if(ErrorMsg.isEmpty()){
                        lblError.setText("Success!!");
                        lblError.setTextColor(R.color.green);
                    }
                    else if(!ErrorMsg.isEmpty()){
                        lblError.setTextColor(R.color.red);
                        lblError.setText(ErrorMsg);
                        log(ErrorMsg);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    lblError.setTextColor(R.color.red);
                    lblError.setText(t.getMessage());
                    log(t.getMessage());
                }
            });
        }
        catch (Exception ex)
        {
            lblError.setTextColor(R.color.red);
            lblError.setText(ex.getMessage());
            throw(ex);
        } finally
        {

        }
    }



    //更新列表
    @SuppressLint("ResourceAsColor")
    private Boolean upDateList(){
        mFilterModuleArray.clear();
        Boolean found=false;
        for (DeviceModule deviceModule : mModuleArray) {
            if(addFilterList(deviceModule,false)){
                found=true;
                break;
            }
        }
        mainRecyclerAdapter.notifyDataSetChanged();
        setMainBackIcon();
        if(!found){
            lblError.setTextColor(R.color.red);
            lblError.setText("Error connecting to Weight Scale");
        }
        return found;
    }

    //设置列表的背景图片是否显示
    private void setMainBackIcon(){
        if (mFilterModuleArray.size() == 0){
            mNotBluetooth.setVisibility(View.VISIBLE);
        }else {
            mNotBluetooth.setVisibility(View.GONE);
        }
    }

    private void initPermission(){
        PermissionUtil.requestEach(FillPalleteActivity.this, new PermissionUtil.OnPermissionListener() {


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
                        if (mHoldBluetooth.bluetoothState()){
                            if (Analysis.isOpenGPS(FillPalleteActivity.this))
                                refresh();
                            else
                                startLocation();
                        }
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
        if(mHoldBluetooth != null) mHoldBluetooth.stopScan();
        super.onPause();
        //退出这个界面，或是返回桌面时，停止扫描
    }
    Gson gson = new Gson();

    public  String TagModelKey(Tag_Model model){
        return model._EPC +"-"+ model._TID;
    }
    @Override
    public void OutPutTags(Tag_Model model) {
        try {
            synchronized (hmList_Lock) {
                LogDebug("RFID Item info:"+TagModelKey(model));
                //LogDebug("RFID Data"+ gson.toJson(model));
                if (hmList.containsKey(TagModelKey(model))) {
                    Tag_Model tModel = hmList.get(TagModelKey(model));
                    tModel._TotalCount++;
                    model._TotalCount = tModel._TotalCount;
                    hmList.remove(TagModelKey(model));
                    hmList.put(TagModelKey(model), model);
                } else {
                    if(!TagModelKey(model).toLowerCase().startsWith("bbb")){
                        model._TotalCount = 1;
                        hmList.put(TagModelKey(model), model);
                    }
                }
               // LogDebug("RFID Items:"+gson.toJson(hmList));
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
        // TODO Auto-generated method stub
    }
}