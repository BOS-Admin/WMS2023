package com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager;

import android.os.StrictMode;
import android.util.Log;

import com.rfidread.Enumeration.eAntennaNo;
import com.rfidread.Enumeration.eReadType;
import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.Models.Tag_Model;
import com.rfidread.RFIDReader;

import java.util.ArrayList;

public class FakeAntenna extends Antenna implements IAsynchronousMessage {

    private String IPAddress="";
    private boolean connected=false;
    private RFIDOutput output;
    private int antennaNo;
    private ArrayList<eAntennaNo> antennaList=new ArrayList<>();
    private boolean isReading=false;
    private Object isReadingLock=new Object();


    public void addAntenna(int i){
       switch (i){
           case 1:antennaList.add(eAntennaNo._1);break;
           case 2:antennaList.add(eAntennaNo._2);break;
           case 3:antennaList.add(eAntennaNo._3);break;
           case 4:antennaList.add(eAntennaNo._4);break;
           case 5:antennaList.add(eAntennaNo._5);break;
           case 6:antennaList.add(eAntennaNo._6);break;
           case 7:antennaList.add(eAntennaNo._7);break;
           case 8:antennaList.add(eAntennaNo._8);break;
           case 9:antennaList.add(eAntennaNo._9);break;
           case 10:antennaList.add(eAntennaNo._10);break;
           case 11:antennaList.add(eAntennaNo._11);break;
           case 12:antennaList.add(eAntennaNo._12);break;
           case 13:antennaList.add(eAntennaNo._13);break;
           case 14:antennaList.add(eAntennaNo._14);break;
           case 15:antennaList.add(eAntennaNo._15);break;
           case 16:antennaList.add(eAntennaNo._16);break;
           case 17:antennaList.add(eAntennaNo._17);break;
           case 18:antennaList.add(eAntennaNo._18);break;
           case 19:antennaList.add(eAntennaNo._19);break;
           case 20:antennaList.add(eAntennaNo._20);break;
           case 21:antennaList.add(eAntennaNo._21);break;
           case 22:antennaList.add(eAntennaNo._22);break;
           case 23:antennaList.add(eAntennaNo._23);break;
           case 24:antennaList.add(eAntennaNo._24);break;
       }

    }


    public int readEPC(eReadType readType,eAntennaNo ant) {
        RFIDReader._Config.SetReaderANT(IPAddress, ant.GetNum());
        return  RFIDReader._Tag6C.GetEPC_TID(IPAddress, ant.GetNum(), readType);
    }

    public ArrayList<eAntennaNo> getAntennaList() {
        return antennaList;
    }

    @Override
    public boolean connect() {
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

        }
       // connected= RFIDReader.CreateTcpConn(IPAddress, this);
        new Thread(()->{

            boolean isReadingLocal;
            while(true){
                synchronized (isReadingLock){
                    isReadingLocal=isReading;
                }

                if(isReadingLocal){
                    try {
                        Tag_Model model=new Tag_Model(new byte[20],1);
                        model._EPC="10000000000000";
                        model._TID="20000000000000";
                        model._ANT_NUM=1;
                        OutPutTags(model);
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else{
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }






        }).start();


        connected=true;
        return connected;
    }


    @Override
    public int readEPC(eReadType readType) {

        synchronized (isReadingLock){
            isReading=true;
        }
        return 0;

    }

    @Override
    public String stop() {

        synchronized (isReadingLock){
            isReading=false;
        }
        return "Success";
    }

    @Override
    public boolean SetBeep(boolean on) {
        return true;
    }


    @Override
    public void WriteDebugMsg(String s) {

    }

    @Override
    public void WriteLog(String s) {

    }

    @Override
    public void PortConnecting(String s) {

    }

    @Override
    public void PortClosing(String s) {

    }

    @Override
    public void OutPutTags(Tag_Model tag_model) {
        RFIDDevicesManager.getOutput().OutPutTags(this,tag_model);

    }

    @Override
    public void OutPutTagsOver() {

    }

    @Override
    public void GPIControlMsg(int i, int i1, int i2) {

    }

    @Override
    public void OutPutScanData(byte[] bytes) {

    }

    public void setIPAddress(String IPAddress) {
        this.IPAddress = IPAddress;
        setConnParam(IPAddress);
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }


    public String getIPAddress() {
        return IPAddress;
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean refresh() {
        return false;
    }

    @Override
    public boolean disconnect() {
        try{
            RFIDReader.CloseConn(getIPAddress());
            return true;
        }
        catch (Exception ex){
            Log.i("AH-Log",ex.getMessage());
            return false;
        }
    }


    public int getAntennaNo() {
        return antennaNo;
    }

    public void setAntennaNo(int antennaNo) {
        this.antennaNo = antennaNo;
    }
}
