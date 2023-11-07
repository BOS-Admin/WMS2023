package com.hc.mixthebluetooth.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import static android.os.ParcelFileDescriptor.MODE_APPEND;

public class Storage {

    private final String widthKey = "widthKey";
    private final String firstTimeStartKey = "firstTimeStartKey";
    private final String invalidAT = "invalidAT";
    private final String codedFormatKey = "codedFormatKey";
    private SharedPreferences sp;
    @SuppressLint("WrongConstant")
    public Storage(Context context){
        sp = context.getSharedPreferences("storage",
                MODE_APPEND| Context.MODE_PRIVATE);
    }

    public  String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    public void saveData(String key, boolean value){
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(key,value);
        editor.apply();
    }

    public void saveData(String key, String value){
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key,value);
        editor.apply();
    }


    public void saveData(String key, Integer value){
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(key,value);
        editor.apply();
    }

    public void saveData(String key, Float value){
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(key,value);
        editor.apply();
    }


    public void saveCodedFormat(String code){
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(codedFormatKey,code);
        editor.apply();
    }

    public boolean getData(String key){
        return sp.getBoolean(key,false);
    }
    public boolean getData(String key,Boolean DefVal){
        return sp.getBoolean(key,DefVal);
    }

    public String getDataString(String key){
        return sp.getString(key,null);
    }
    public String getDataString(String key,String DefVal){
        return sp.getString(key,DefVal);
    }

    public float getDataFloat(String key){
        return sp.getFloat(key,0f);
    }
    public float getDataFloat(String key,float DefVal){
        return sp.getFloat(key,DefVal);
    }

    public int getDataInt(String key){
        return sp.getInt(key,0);
    }
    public int getDataInt(String key,Integer DefVal){
        return sp.getInt(key,DefVal);
    }
    public int getWidth(){return sp.getInt(widthKey,-1);}


    public boolean getInvalidAT(){ return sp.getBoolean(invalidAT,true);}

    public String getCodedFormat(){ return sp.getString(codedFormatKey,"GBK");}

}
