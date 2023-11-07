package com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager;

import android.annotation.SuppressLint;
import android.util.Log;

import com.rfidread.Enumeration.eAntennaNo;
import com.rfidread.Enumeration.eReadType;
import com.rfidread.RFIDReader;

import java.util.ArrayList;

public class RFIDDevicesManager {

    private static ArrayList<RFIDDevice> devices = new ArrayList<>();
    private static RFIDOutput output;
    private static Antenna singleAntennaReader;
    private static Slid singleSlid;
    private static Object IsReadingLock = new Object();
    private static boolean IsReading = false;


    public static Antenna getSingleAntennaReader() {
        return singleAntennaReader;
    }
    public static Slid getSingleSlid() {
        return singleSlid;
    }

    public static boolean connectRFIDDevice(Slid device) {
        if (device.connect()) {
            devices.add(0, device);
            device.SetBeep(false);
            return true;
        }
        return false;

    }


    public static boolean connectRFIDDevice(Antenna device) {
        if (device.connect()) {
            devices.add(device);

            return true;
        }
        return false;

    }


    public static boolean connectSingleAntenna(Antenna device) {
        RFIDReader.CloseAllConnect();
        if (device.connect()) {
            singleAntennaReader = device;
            return true;
        }
        return false;

    }

    public static boolean connectSingleSlid(Slid device) {
        RFIDReader.CloseAllConnect();
        if (device.connect()) {
            singleSlid = device;
            return true;
        }
        return false;

    }


    public static int[] readEPC(eReadType readType) {
        int[] rs = new int[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            rs[i] = -1000;
        }
        int i = 0;
        for (RFIDDevice device : devices) {
            rs[i] = device.readEPC(readType);
        }

        return rs;

    }

    @SuppressLint("SuspiciousIndentation")
    public static int readEPCSingleSlid(eReadType readType) {
        if(singleSlid==null)
            return -1000;
           return singleSlid.readEPC(readType);
     }





    public static void readEPC(eReadType readType,int readTime)  {

        new Thread(()->{
            Log.i("RfidBulkActivity-notifyListener", "Devices Count "+devices.size() );
            int[] rs = new int[devices.size()];
            for (int i = 0; i < devices.size(); i++) {
                rs[i] = -10000;
            }
            int i = 0;
            for (RFIDDevice device : devices) {
                rs[i] = device.readEPC(readType);
                output.listener.notifyStartDevice(device.getConnParam()+" : "+rs[i]);
                try {
                    Thread.sleep(readTime);
                } catch (InterruptedException e) {

                }
                device.stop();

                output.listener.notifyEndDevice(device.getConnParam());
            }

           // return rs;
        }).start();


    }

    public static int[] readEPCSingleAntenna(eReadType readType, int millis) {
        if(singleAntennaReader==null)
            return new int[0];
        int[] rs = new int[singleAntennaReader.getAntennaList().size()];
        String[] rsStop = new String[singleAntennaReader.getAntennaList().size()];
        int i = 0;
        IsReading=true;
        while(true){


            for ( i=0;i<singleAntennaReader.getAntennaList().size() ;i++) {

                eAntennaNo ant=singleAntennaReader.getAntennaList().get(i);

                synchronized (IsReadingLock){
                    if(!IsReading) {
                        return rs;
                    }
                    rs[i] = singleAntennaReader.readEPC(readType, ant);
                    output.AntennaStarted(((int)Math.pow(Integer.parseInt(ant.toString()),-2))+1);

                }

                Log.i("AH-Log", "Reading from Antenna " + ant);
                Log.i("AH-Log", "Reading from Antenna " + ant + " => Result " + rs[i]);
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    Log.i("AH-Log", "Reading from Antenna " + ant + " => Interrupted " + e.getMessage());
                }
                rsStop[i] = singleAntennaReader.stop();
                output.AntennaStopped(((int)Math.pow(Integer.parseInt(ant.toString()),-2))+1);
                Log.i("AH-Log", "Stopped Reading from Antenna " + ant);
                Log.i("AH-Log", "Stopped Reading from Antenna " + ant + " => Result " + rsStop[i]);


            }
        }




    }


    public static int readEPCSingleAntenna(int ant) {
        if(singleAntennaReader==null)
            return -1000;
        int rs ;
        String rsStop ;
        rs = singleAntennaReader.readEPC(eReadType.Inventory, singleAntennaReader.getAntennaNum(ant));
        return rs;
    }


    public static int readEPCSingleAntennaAll() {
        if(singleAntennaReader==null)
            return -1000;
        int rs ;
        rs = singleAntennaReader.readEPCAll(eReadType.Inventory);
        return rs;
    }



    public static int readEPCSingleAntenna(eReadType readType,eAntennaNo ant) {
        if(singleAntennaReader==null)
            return -1000;
        int rs ;
        String rsStop ;
        rs = singleAntennaReader.readEPC(readType, ant);
        Log.i("AH-Log", "Reading from Antenna " + ant);
        Log.i("AH-Log", "Reading from Antenna " + ant + " => Result " + rs);
        rsStop = singleAntennaReader.stop();
        Log.i("AH-Log", "Stopped Reading from Antenna " + ant);
        Log.i("AH-Log", "Stopped Reading from Antenna " + ant + " => Result " + rsStop);

        return rs;
    }


    public static void setOutput(RFIDOutput output1) {
        output = output1;
    }



    public static RFIDOutput getOutput() {
        return output;
    }


    public static void stop() {
        for (RFIDDevice device : devices) {
            device.stop();
        }
    }


    public static String stopSingleAntenna1() {
        synchronized (IsReadingLock){
            IsReading=false;
        }
        if(singleAntennaReader!=null)
            return singleAntennaReader.stop();
        return "";
    }

    public static String stopSingleSlid() {

        if(singleSlid!=null)
            return singleSlid.stop();
        return "";
    }

    public static String stopSingleAntenna() {

        if(singleAntennaReader!=null)
            return singleAntennaReader.stop();
        return "";
    }


    public static ArrayList<RFIDDevice> getDevices() {

        return devices;
    }

    public static boolean disconnectSingle() {
        try {
            RFIDReader.CloseAllConnect();
            RFIDDevicesManager.devices=new ArrayList<>();
            RFIDDevicesManager.singleAntennaReader=null;
            RFIDDevicesManager.singleSlid=null;
            return true;
        }
        catch (Exception e){
            Log.i("AH-Log","Closing Connection : "+e.getMessage());
            return false;
        }


    }

    public static void clearAllSingleAntennas() {
        if(singleAntennaReader!=null)
            try{
                singleAntennaReader.getAntennaList().clear();
            }catch (Exception e){

            }

    }


}
