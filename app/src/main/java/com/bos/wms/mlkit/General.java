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


public class General {

    public static String _IsCommand6Cor6B = "6C";

    private static volatile General instance = null;
    public Integer UserID=0;
    public Integer FloorID=0;

    public String UserName="";
    public String AppVersion="2.2.36 13/02/2024";
    int interval = 3600;    // when there's no activity

    public String ipAddress="";

    public String UserCode="";

    public String boxNb;
    public String destination;
    public String packReason="";
    public Integer packReasonId=-1;
    public String printerStationCode="";
    public String packType="";
    public Integer stockType=-1;

    public Integer operationType=-1;
    public String BoxStartsWith="";
    public String StandStartsWith = "";
    public String BoxNbDigits= "" ;
    public String StandNbDigits= "" ;

    public String LocationString="";

    public Integer locationTypeID=0;

    public Integer mainLocationID =0;
    public String mainLocation ;
    public Integer subLocationID =0;
    public String subLocation ;

    public String userFullName="" ;

    public Integer transactionType=-1;

    public String transferNavNo="" ;
    public boolean isReceiving=false ;


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
            ipAddress = sp.getString("ipAddress", ipAddress);

            UserName = sp.getString("UserName", "");
            UserCode = sp.getString("UserCode", "");
            userFullName = UserCode+", "+UserName;

            locationTypeID = sp.getInt("LocationTypeID", locationTypeID);
            mainLocationID = sp.getInt("MainLocationId", mainLocationID);
            mainLocation = sp.getString("MainLocation", mainLocation);
            subLocationID = sp.getInt("SubLocationId", subLocationID);
            subLocation = sp.getString("SubLocation", subLocation);
            LocationString =  mainLocation+", "+subLocation;


            boxNb = sp.getString("BoxNb", "");
            destination = sp.getString("Destination", "");

            packReason = sp.getString("PackReason", "");
            packReasonId = sp.getInt("packReasonId", packReasonId);
            printerStationCode = sp.getString("PrinterStationCode", "");

            packType = sp.getString("PackType", "");
            transferNavNo = sp.getString("TransferNavNo", "");
            BoxStartsWith=sp.getString("BoxStartsWith","");
            StandStartsWith=sp.getString("StandStartsWith","");
            BoxNbDigits=sp.getString("BoxNbDigits","");
            StandNbDigits=sp.getString("StandNbDigits","");
            stockType=sp.getInt("StockType",-1);
            operationType=sp.getInt("OperationType",-1);
            transactionType=sp.getInt("TransactionType",-1);
            isReceiving=sp.getBoolean("IsReceiving",false);




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


            sp.putString("UserName",UserName);
            sp.putInt("LocationTypeID",locationTypeID);
            sp.putInt("MainLocationId",mainLocationID);
            sp.putInt("SubLocationId",subLocationID);
            sp.putString("MainLocation",mainLocation);
            sp.putString("SubLocation",subLocation);
            sp.putString("LocationString",mainLocation+" ,"+subLocation);

            sp.putString("UserCode",UserCode);
            sp.putString("BoxNb",boxNb);
            sp.putString("Destination",destination);

            sp.putString("PackReason",packReason);
            sp.putInt("packReasonId",packReasonId);
            sp.putString("PrinterStationCode",printerStationCode);

            sp.putString("PackType",packType);
            sp.putString("TransferNavNo",transferNavNo);
            sp.putString("ipAddress",ipAddress);
            sp.putInt("StockType",stockType);
            sp.putInt("OperationType",operationType);
            sp.putInt("TransactionType",transactionType);
            sp.putBoolean("IsReceiving",isReceiving);

            sp.commit();
        } catch (Exception e) {
            Log.e("General", "exception reading preferences: " + e, e);
            // TODO: report it
        }
    }



    public String getFullLocation(){
        return mainLocation +", "+subLocation;
    }
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
        //new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_PIP, 150);
        playTone(ToneGenerator.TONE_SUP_PIP,5000);
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
        return !str.isEmpty();
    }


    public static Boolean ValidateDestination(String str){
        return str!=null && !str.isEmpty() && str.length() >2;
    }
    public static Boolean ValidatePalleteCode(String str){
        return !str.isEmpty() && str.length()>1;
    }

    public static Boolean ValidateItemSerialCode(String str){
        return !str.isEmpty() && str.length()>10 && (str.toUpperCase().startsWith("IS") || str.startsWith("22"));
    }

    public static Boolean ValidateAppointmentNoFormat(String str){
        return !str.isEmpty() && ToInteger(str,0)>0 && ToInteger(str,1000000000)<1000000000;
    }
/*

    public static Boolean ValidateBolNoFormat(String str){
        return !str.isEmpty() && ToInteger(str,0)>0 && ToInteger(str,1000000000)<1000000000;
    }
*/

    public static Boolean ValidateBolNoFormat(String str){
        return !str.isEmpty() && ToLong(str,0)>0;
    }

    public static Boolean ValidateAppointmentNo(String str){
        return !str.isEmpty() && ToInteger(str,0)>0 && ToInteger(str,1000000000)<1000000000;
    }
    public static Boolean ValidateBolNo(String str){
        return !str.isEmpty() && ToLong(str,0)>0;
    }
    /*

    public static Boolean ValidateBolNo(String str){
        return !str.isEmpty() && ToInteger(str,0)>0 && ToInteger(str,1000000000)<1000000000;
    }*/
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
    public  static  long ToLong(String str,long Default){
        try
        {
            return Long.parseLong(str);
        }
        catch (NumberFormatException e) {
            return  Default;
        }
        catch (Exception ex) {
            return  Default;
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

    public static Boolean ValidateNbOfSerialsGenerate(String str){
        return !str.isEmpty() && ToInteger(str,0)>0 && ToInteger(str,101)<101;
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

    public static Boolean ValidatePG(String str){
        return !str.isEmpty() && str.length()==10;
    }

    public static Boolean ValidateSeason(String str){
        return !str.isEmpty() && str.length()>4;
    }
    public static Boolean ValidateItemSerial(String str){
        return !str.isEmpty() && str.length()>1;
    }
    public static Boolean ValidateFoldingStationCode(String str){
        return !str.isEmpty() && str.length()>4;
    }

    public static Boolean ValidateUserCode(String str){
        return str!=null && !str.isEmpty() && str.length()>=4;
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
        state.putString("LocationString",LocationString);
        state.putString("UserName",UserName);

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
        LocationString=state.getString("LocationString");
        UserName=state.getString("UserName");

    }
}