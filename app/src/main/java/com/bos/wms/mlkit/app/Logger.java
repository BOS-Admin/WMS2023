package com.bos.wms.mlkit.app;

import android.content.Context;
import android.util.Log;

import com.bos.wms.mlkit.R;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import Remote.APIClient;
import Remote.BasicApi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class Logger {

    private static Context appContext;
    private static boolean isInitialized = false;

    private static ArrayList<String> LoggerFileTypes;

    /**
     * Initializes the logger by cleaning up log files older than 10 days
     * @param context the application context, used to get the logger output directory
     */
    public static void Initialize(Context context, String[] LogTypes){
        if(!isInitialized) {
            appContext = context;
            isInitialized = true;

            LoggerFileTypes = new ArrayList<>();

            for(String logType : LogTypes){
                if(!LoggerFileTypes.contains(logType))
                    LoggerFileTypes.add(logType);
            }

            AddLoggerType("Error");
            AddLoggerType("Debug");

            CleanUpLogFiles(10);
            Debug("LOGGER", "Logger Has Been Initialized");

        }else {
            Debug("LOGGER", "Logger Already Has Been Initialized, Initialize Not Successful");
        }
    }

    /**
     * Cleans up (deletes) old log files, that are older from the current day by the integer days
     * @param days
     */
    private static void CleanUpLogFiles(int days){
        for(String type : LoggerFileTypes) {
            CleanUpSingleLogFile(days, new File(getOutputDirectory(), "/Logs/" + type + "/"));
        }
        CleanUpAPKS(days, new File(getOutputDirectory(), "/Downloads/"));//Cleaned up the downloaded files
        Debug("LOGGER", "CleanUpLogFiles - Done All Log Files Cleanups");
    }

    /**
     * Loops through all the files in a folder, and deletes the files whose name has a date and the date is older than the number of days provided
     * @param days Number of days to be older from current date
     * @param file The folder path of the log files
     */
    private static void CleanUpSingleLogFile(int days, File file){
        if(file.exists()){
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                String dateString = files[i].getName().replaceAll("Log ", "").replaceAll(".txt", "");
                try{
                    Date date = new SimpleDateFormat("dd-MM-yyyy").parse(dateString);
                    Date now = new Date();
                    long difference = now.getTime() - date.getTime();
                    long totalDays
                            = (difference
                            / (1000 * 60 * 60 * 24))
                            % 365;
                    if(totalDays > days){
                        files[i].delete();
                    }
                }catch(ParseException ex){
                    Error("LOGGER", "CleanUpSingleLogFile - Failed Parsing Date For File " + file.getAbsolutePath() + " Date: " + dateString);
                }
            }
        }
        Debug("LOGGER", "CleanUpSingleLogFile - Done Cleanup For " + file.getAbsolutePath());
    }


    /**
     * Loops through the file and cleans up all apks older than the number of days
     * @param days
     * @param file
     */
    private static void CleanUpAPKS(int days, File file){
        if(file.exists()){
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                Date date = new Date(files[i].lastModified());
                Date now = new Date();
                long difference = now.getTime() - date.getTime();
                long totalDays
                        = (difference
                        / (1000 * 60 * 60 * 24))
                        % 365;
                if(totalDays > days){
                    files[i].delete();
                }
            }
        }
        Debug("LOGGER", "CleanUpAPKS - Done Cleanup For " + file.getAbsolutePath());
    }


    /**
     * Sends a debug log which is saved to the current day's log file in the /Debug/ Folder
     * @param tag The log tag
     * @param msg The log message
     */
    public static void Debug(String tag, String msg) {
        Log.d(tag, msg);
        if(!isInitialized){
            Log.e("LOGGER", "Logger Not Initialized Yet");
            return;
        }
        LogToFile("Debug", tag, msg);
    }


    /**
     * Sends a error log which is saved to the current day's log file in the /Error/ Folder
     * @param tag The log tag
     * @param msg The log message
     */
    public static void Error(String tag, String msg)  {
        Log.e(tag, msg);
        if(!isInitialized) {
            Log.e("LOGGER", "Logger Not Initialized Yet");
            return;
        }

        LogToFile("Error", tag, msg);

    }

    /**
     * Sends a debug log which is saved to the current day's log file in the /TYPE/ Folder
     * @param type the log type, the log will be saved in a folder names by the type
     * @param tag The log tag
     * @param msg The log message
     */
    public static void Log(String type, String tag, String msg) {
        Log.d(tag, msg);
        if(!isInitialized){
            Log.e("LOGGER", "Logger Not Initialized Yet");
            return;
        }
        LogToFile(type, tag, msg);
    }

    /**
     * Saves The Log Data To A File
     * @param type
     * @param tag
     * @param msg
     */
    private static void LogToFile(String type, String tag, String msg){

        if(!IsLoggerTypeValid(type)){
            Error("LOGGER", "Attempting To Log Date For Type: " + type + " While Logger Type Is Not Valid!");
            return;
        }

        File FolerPath = new File(
                getOutputDirectory(),
                "/Logs/" + type + "/"
        );


        String time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        String data = "[" + tag + "] " + time + ": " + msg +"\n\r";
        FolerPath.mkdirs();
        String fileName = new SimpleDateFormat("dd-MM-yyyy'.txt'").format(new Date());
        fileName = "Log " + fileName;
        File logFile = new File(
                FolerPath, fileName);

        if (!logFile.exists()){
            try {
                logFile.createNewFile();
            }catch(IOException e){
                Log.e("LOGGER", "Failed Creating LogFile (" + logFile.getAbsolutePath() + ")");
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

    /**
     * Adds A Logger Type Such As Debug, Error, And Such
     * @param type
     */
    public static void AddLoggerType(String type){
        if(LoggerFileTypes.contains(type))
            return;
        LoggerFileTypes.add(type);
    }

    /**
     * Checks If A Logger Type Is Valid
     * @param type
     * @return
     */
    public static boolean IsLoggerTypeValid(String type){
        return LoggerFileTypes.contains(type);
    }

    /**
     * Returns The Application Output Directory
     * @return
     */
    private static File getOutputDirectory() {
        File[] allMediaDirs = appContext.getExternalMediaDirs();
        File mediaDir = allMediaDirs.length > 0 ? allMediaDirs[0] : null;
        if(mediaDir == null) {
            new File(appContext.getResources().getString(R.string.app_name)).mkdirs();
        }

        return (mediaDir != null && mediaDir.exists()) ? mediaDir : appContext.getFilesDir();
    }

}