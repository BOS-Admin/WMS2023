package com.hc.mixthebluetooth.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.hc.basiclibrary.ioc.ViewById;
import com.hc.basiclibrary.permission.PermissionUtil;
import com.hc.basiclibrary.titleBasic.DefaultNavigationBar;
import com.hc.basiclibrary.viewBasic.BasActivity;
import com.hc.bluetoothlibrary.DeviceModule;
import com.hc.mixthebluetooth.R;
import com.hc.mixthebluetooth.activity.tool.Analysis;
import com.hc.mixthebluetooth.customView.PopWindowMain;
import com.hc.mixthebluetooth.storage.Storage;
import com.util.Logging;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;

public class PackingDCActivity extends BasActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_packing_dc);
        LogDebug("RFID1-" + "OnCreate");
        setTitle();
        setContext(this);

    }



    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("ResourceAsColor")
    @Override
    public void initAll() {
        try {
            RefreshSettings();
            mStorage = new Storage(this);//sp存储

            IPAddress = mStorage.getDataString("IPAddress", "192.168.50.20:5000");
            UserID = mStorage.getDataInt("UserID", -1);

            initPermission();

            initView();

        } catch (Exception ex) {

        }

    }






    private void RefreshView() {


    }





    private void refresh() {
        if (false) {
            mModuleArray.clear();
            mFilterModuleArray.clear();
            mTitle.updateLoadingState(true);

        }

    }
    private boolean ContinueRead = true;

    private void LogError(String str) {
        Logging.LogError(getApplicationContext(), str);
    }

    private void LogDebug(String str) {
        Logging.LogDebug(getApplicationContext(), str);
    }



    private void initView() {
        setMainBackIcon();

    }




    public static final int FRAGMENT_STATE_DATA = 0x06;
    public static final int FRAGMENT_STATE_SERVICE_VELOCITY = 0x13;//读取实时速度
    public static final int FRAGMENT_STATE_NUMBER = 0x07;
    public static final int FRAGMENT_STATE_CONNECT_STATE = 0x08;
    public static final int FRAGMENT_STATE_SEND_SEND_TITLE = 0x09;
    public static final int FRAGMENT_STATE_LOG_MESSAGE = 0x011;


    //设置头部
    private void setTitle() {
        mTitle = new DefaultNavigationBar
                .Builder(this, (ViewGroup) findViewById(R.id.activity_scan_rfid))
                .setLeftText("Packing DC")
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
        new PopWindowMain(v, PackingDCActivity.this, new PopWindowMain.DismissListener() {
            @Override
            public void onDismissListener(boolean resetEngine) {//弹出窗口销毁的回调
                mTitle.updateRightImage(false);
                if (resetEngine) {//更换搜索引擎，重新搜索
                    refresh();
                }
            }
        });
    }

    //设置点击事件

    private Disposable disposable;


    //设置列表的背景图片是否显示
    private void setMainBackIcon() {
        if (mFilterModuleArray.size() == 0) {
            mNotBluetooth.setVisibility(View.VISIBLE);
        } else {
            mNotBluetooth.setVisibility(View.GONE);
        }
    }

    //初始化位置权限
    private void initPermission() {
        PermissionUtil.requestEach(PackingDCActivity.this, new PermissionUtil.OnPermissionListener() {


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

                        if (Analysis.isOpenGPS(PackingDCActivity.this))
                            refresh();
                        else
                            startLocation();
                    }
                }, 1000);

            }
        }, PermissionUtil.LOCATION);
    }

    private Object beep_Lock = new Object();

    //开启位置权限
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

    }



    @Override
    protected void onResume() {
        super.onResume();

    }



    private DefaultNavigationBar mTitle;

    private Storage mStorage;

    private List<DeviceModule> mModuleArray = new ArrayList<>();
    private List<DeviceModule> mFilterModuleArray = new ArrayList<>();



    private int mStartDebug = 1;
    private String IPAddress = "";
    private int UserID = -1;
    @ViewById(R.id.main_back_not)
    private LinearLayout mNotBluetooth;



    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    public void RefreshSettings() {

    }



}