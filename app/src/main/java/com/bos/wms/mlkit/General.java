package com.bos.wms.mlkit;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import Model.SystemControlModel;
import Model.SystemControlModelItem;
import Model.UserLoginResultModel;

public class General {
    private static volatile General instance = null;
    public Integer UserID=0;
    public Integer FloorID=0;
    public String AppVersion="1.2.2 04/08/2022";
    int interval = 3600;    // when there's no activity

    /**
     * Return the single instance of this class, creating it
     * if necessary. This method is thread-safe.
     */
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
    public static ToneGenerator toneGenerator;
    public static  void playSuccess(){
        new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_PIP, 150);
        //playTone(SUCESS_TONE,500);
    }
    public static   void playError(){
        playTone(ToneGenerator.TONE_SUP_ERROR,4000);
        playTone(ToneGenerator.TONE_SUP_ERROR,3000);
        playTone(ToneGenerator.TONE_SUP_ERROR,2000);
        playTone(ToneGenerator.TONE_SUP_ERROR,1000);
        //new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 800);
        //new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 800);
        //new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 800);
        //new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 800);
        //playTone(ERROR_TONE,4000);
        //playTone(ERROR_TONE,4000);
    }
    /**
     * 播放声音
     *
     * @param toneType 参见ToneGenerator
     */
    public static  void playTone(final int toneType, final int timout) {
        try {
            new Thread() {
                @Override
                public void run() {
                    try {
                        sleep(timout);
                        if (toneGenerator != null) {
                            toneGenerator.stopTone();
                        }
                    } catch (InterruptedException e) {
                    }
                }
            }.start();
            if (toneGenerator == null) {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 99);
            }
            toneGenerator.startTone(toneType);
        } catch (Exception e) {
        }
    }

    public static Boolean ValidateShelfCode(String str){
        return !str.isEmpty() && str.length()>1;
    }
    public static Boolean ValidateBinCode(String str){
        return !str.isEmpty() && str.length()>1;
    }
    public static Boolean ValidatePalleteCode(String str){
        return !str.isEmpty() && str.length()>1;
    }

    public static Boolean ValidateAppointmentNoFormat(String str){
        return !str.isEmpty() && ToInteger(str,0)>0 && ToInteger(str,1000000000)<1000000000;
    }

    public static Boolean ValidateBolNoFormat(String str){
        return !str.isEmpty() && ToInteger(str,0)>0 && ToInteger(str,1000000000)<1000000000;
    }

    public static Boolean ValidateAppointmentNo(String str){
        return !str.isEmpty() && ToInteger(str,0)>0 && ToInteger(str,1000000000)<1000000000;
    }

    public static Boolean ValidateBolNo(String str){
        return !str.isEmpty() && ToInteger(str,0)>0 && ToInteger(str,1000000000)<1000000000;
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
    public  static  Integer ToInteger(String str,int Default){
        try
        {
            return Integer.parseInt(str);
        }
        catch (NumberFormatException e) {
            return  Default;
        }
        catch (Exception ex) {
            return  Default;
        }
    }
    public static Boolean ValidatePalleteNo(String str){

        return !str.isEmpty() && ToInteger(str,0)>0 && ToInteger(str,100)<100;
    }
    public static Boolean ValidateNbOfCartons(String str){
        return !str.isEmpty() && ToInteger(str,0)>0 && ToInteger(str,100)<100;
    }

    public static Boolean ValidateCartonCode(String str){
        return !str.isEmpty() && str.length()>10;
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


    public static void showSoftKeyboard(Activity activity) {

        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);

        View currentFocus = activity.getCurrentFocus();

        if (inputMethodManager != null) {
            IBinder windowToken = activity.getWindow().getDecorView().getRootView().getWindowToken();
            inputMethodManager.showSoftInputFromInputMethod(windowToken, 0);
            inputMethodManager.showSoftInputFromInputMethod(windowToken, InputMethodManager.SHOW_IMPLICIT);

            if (currentFocus != null) {
                inputMethodManager.showSoftInputFromInputMethod(currentFocus.getWindowToken(), 0);
            }
        }

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