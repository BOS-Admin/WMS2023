package com.bos.wms.mlkit.app;

import android.content.Context;
import android.util.Log;

import com.bos.wms.mlkit.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    private static Context appContext;
    private static boolean isInitialized = false;

    public static void Initialize(Context context){
        appContext = context;
        isInitialized = true;
        Debug("LOGGER", "Logger Has Been Initialized");
    }

    public static void Debug(String tag, String msg) {
        Log.d(tag, msg);
        if(!isInitialized){
            Log.e("LOGGER", "Logger Not Initialized Yet");
            return;
        }
        File FolerPath = new File(
                getOutputDirectory(),
                "/Debug/"
        );


        String time = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(new Date());
        String data = "[" + tag + "] " + time + ": " + msg +"\n\r";
        FolerPath.mkdirs();
        String fileName = new SimpleDateFormat("yyyyMMdd'.txt'").format(new Date());
        fileName = "Log_" + fileName;
        File logFile = new File(
                FolerPath, fileName);

        if (!logFile.exists()){
            try {
                logFile.createNewFile();
            }catch(IOException e){
                Log.e("LOGGER", "Failed Creating Debug LogFile (" + logFile.getAbsolutePath() + ")");
            }
        }
        try {
            FileOutputStream outStream = new FileOutputStream(logFile, true) ;
            OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream);

            outStreamWriter.append(data);
            outStreamWriter.flush();
        }catch(IOException e){
            Log.e("LOGGER", "Failed Creating An Output Stream Writer (" + logFile.getAbsolutePath() + ")");
        }

    }

    public static void Error(String tag, String msg)  {
        Log.e(tag, msg);
        if(!isInitialized) {
            Log.e("LOGGER", "Logger Not Initialized Yet");
            return;
        }
        File FolerPath = new File(
                getOutputDirectory(),
                "/Error/"
        );


        String time = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(new Date());
        String data = "[" + tag + "] " + time + ": " + msg +"\n\r";
        FolerPath.mkdirs();
        String fileName = new SimpleDateFormat("yyyyMMdd'.txt'").format(new Date());
        fileName = "Log_" + fileName;
        File logFile = new File(
                FolerPath, fileName);

        if (!logFile.exists()){
            try {
                logFile.createNewFile();
            }catch(IOException e){
                Log.e("LOGGER", "Failed Creating Debug LogFile (" + logFile.getAbsolutePath() + ")");
            }
        }
        try {
            FileOutputStream outStream = new FileOutputStream(logFile, true) ;
            OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream);

            outStreamWriter.append(data);
            outStreamWriter.flush();
        }catch(IOException e){
            Log.e("LOGGER", "Failed Creating An Output Stream Writer (" + logFile.getAbsolutePath() + ")");
        }


    }

    private static File getOutputDirectory() {
        File[] allMediaDirs = appContext.getExternalMediaDirs();
        File mediaDir = allMediaDirs.length > 0 ? allMediaDirs[0] : null;
        if(mediaDir == null) {
            new File(appContext.getResources().getString(R.string.app_name)).mkdirs();
        }

        return (mediaDir != null && mediaDir.exists()) ? mediaDir : appContext.getFilesDir();
    }

}
