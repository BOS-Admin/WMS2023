package com.util;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.hc.bluetoothlibrary.DeviceModule;
import com.hc.mixthebluetooth.Model.PackReasonModelItem;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

import Model.SystemControlModel;
import Model.SystemControlModelItem;

public class General {
    private static volatile General instance = null;
    public Boolean InRFID = false;
    public DeviceModule Device;
    public Integer UserID = 0;
    public Integer FloorID = 0;
    public String AppVersion = "App Version 2.2.31 12/2/2024";
    int interval = 3600;    // when there's no activity

    public PackReasonModelItem currentPackReason = null;

    public String mainLocation = "";
    public int mainLocationId = -1;


    /**
     * Return the single instance of this class, creating it
     * if necessary. This method is thread-safe.
     */

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static String GetMacAddress(Context ctx) {
        WifiManager manager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

        }
        String address = info.getMacAddress();
        return  address;
    }

    public  static  Boolean IsInteger(String str){
        try
        {
            Integer.parseInt(str);
            return true;
        }
        catch (NumberFormatException e) {
            return  false;
        }
    }

    public  static  Boolean IsUPC(String str){
        try
        {
            if (str.length() <12  || str.length() > 14)
                return  false;
            Long.parseLong(removeLastChar(str));
            return true;
        }
        catch (NumberFormatException e) {
            return  false;
        }
    }
    public static String removeLastChar(String s) {
        return (s == null || s.length() == 0)
                ? null
                : (s.substring(0, s.length() - 1));
    }
    public  static  Boolean IsLong(String str){
        try
        {
            Long.parseLong(str);
            return true;
        }
        catch (NumberFormatException e) {
            return  false;
        }
    }
    public static String getMacAddress(){
        try{
            List<NetworkInterface> networkInterfaceList = Collections.list(NetworkInterface.getNetworkInterfaces());
            String stringMac = "";
            for(NetworkInterface networkInterface : networkInterfaceList)
            {
                if(networkInterface.getName().equalsIgnoreCase("wlon0"));
                {
                    for(int i = 0 ;i <networkInterface.getHardwareAddress().length; i++){
                        String stringMacByte = Integer.toHexString(networkInterface.getHardwareAddress()[i]& 0xFF);
                        if(stringMacByte.length() == 1)
                        {
                            stringMacByte = "0" +stringMacByte;
                        }
                        stringMac = stringMac + stringMacByte.toUpperCase() + ":";
                    }
                    break;
                }
            }
            return stringMac;
        }catch (SocketException e)
        {
            e.printStackTrace();
        }
        return  "0";
    }
    public static General getGeneral(Context ctx) {
        if (instance == null) {
            synchronized(General.class) {
                if (instance == null) {
                    instance = new General();
                    instance.readGeneral(ctx);
                }
            }
        }
        return instance;
    }
    public static String AppStage="";
    public static Boolean ValidateShelfCode(String str){
        return !str.isEmpty() && str.length()>1;
    }
    public static Boolean ValidateBinCode(String str){
        return !str.isEmpty() && str.length()>1;
    }
    public static Boolean ValidatePalleteCode(String str){
        return !str.isEmpty() && str.length()>1;
    }
    public static Boolean ValidateRFIDCode(String str){
        return !str.isEmpty() && str.length()>1;
    }
    public static Boolean ValidateItemCode(String str){
        return !str.isEmpty() && str.length()>8;
    }
    public static Boolean ValidateItemSerial(String str){
        return !str.isEmpty() && str.length()>1;
    }
    public static Boolean ValidateFoldingStationCode(String str){
        return !str.isEmpty() && str.length()>4;
    }

    public static void hideSoftKeyboard(Activity activity) {

        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);

        View currentFocus = activity.getCurrentFocus();

        if (inputMethodManager != null) {
            IBinder windowToken = activity.getWindow().getDecorView().getRootView().getWindowToken();
            inputMethodManager.hideSoftInputFromWindow(windowToken, 0);
            inputMethodManager.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS);

            if (currentFocus != null) {
                inputMethodManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
        }

    }
    /**
     * Re-read all preferences (you never need to call this explicitly)
     */
    private void readGeneral(Context ctx) {
        try {
            SharedPreferences sp =
                    PreferenceManager.getDefaultSharedPreferences(ctx);

            UserID = sp.getInt("UserID", UserID);
            interval = sp.getInt("interval", interval);
            FloorID = sp.getInt("FloorID", FloorID);


        } catch (Exception e) {
            Log.e("General", "exception reading preferences: " + e, e);
            // TODO: report it
        }
    }

    public String getSetting(Context ctx,String SettingsCode) {
        try {
            SharedPreferences sp =PreferenceManager.getDefaultSharedPreferences(ctx);
            return sp.getString(SettingsCode, "");
        } catch (Exception e) {
            Logging.LogError(ctx,"Exception reading SystemControl:"+e.getMessage());
            Log.e("General", "exception reading preferences: " + e, e);
            // TODO: report it
        }
        return "";
    }

    /**
     * Save preferences; you can call this from onPause()
     */
    public void saveGeneral(Context ctx) {
        try {
            SharedPreferences.Editor sp =
                    PreferenceManager.getDefaultSharedPreferences(ctx).edit();

            sp.putInt("UserID", UserID);
            sp.putInt("interval", interval);
            sp.putInt("FloorID",FloorID);
            sp.commit();
        } catch (Exception e) {
            Log.e("General", "exception reading preferences: " + e, e);
            // TODO: report it
        }
    }
    public void saveGeneral(Context ctx, SystemControlModel Model) {
        try {
            SharedPreferences.Editor sp =
                    PreferenceManager.getDefaultSharedPreferences(ctx).edit();

            for (SystemControlModelItem s:Model.getSettings()) {
                sp.putString(s.getCode(),s.getValue());
            }
            sp.commit();
        } catch (Exception e) {
            Log.e("General", "exception reading preferences: " + e, e);
            // TODO: report it
        }
    }

    /**
     * Save preferences to a bundle. You don't really need to implement
     * this, but it can make start-up go faster.
     * Call this from onSaveInstanceState()
     */
    public void onSaveInstanceState(Bundle state) {

        state.putInt("UserID", UserID);
        state.putInt("interval", interval);
        state.putInt("FloorID",FloorID);
    }

    /**
     * Recall preferences from a bundle. You don't really need to implement
     * this, but it can make start-up go faster.
     * Call this from onCreate()
     */
    public void restoreInstanceState(Bundle state) {
        UserID = state.getInt("UserID");
        interval = state.getInt("interval");
        FloorID=state.getInt("FloorID");
    }
}

