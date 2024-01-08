package com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager;

import android.os.Environment;
import android.util.Log;

import java.io.File;

public class Storage {

    static String pathToDCIM;

    public static boolean init(){
        Log.i("PathToDCIM", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath());
        return true;
    }

    public static File getDevicesConfig(){
        return new File(pathToDCIM+"/app.config" );

    }


}
