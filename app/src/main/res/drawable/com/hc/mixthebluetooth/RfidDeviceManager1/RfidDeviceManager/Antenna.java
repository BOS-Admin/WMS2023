package com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager;

import android.os.StrictMode;
import android.util.Log;

import com.rfidread.Enumeration.eAntennaNo;
import com.rfidread.Enumeration.eReadType;
import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.Models.Tag_Model;
import com.rfidread.RFIDReader;

import java.util.ArrayList;

public class Antenna extends RFIDDevice implements IAsynchronousMessage {

    private String IPAddress="";
    private boolean connected=false;
    private RFIDOutput output;
    private int antennaNo;
    private ArrayList<eAntennaNo> antennaList=new ArrayList<>();
    private ArrayList<Integer> antennaListInt=new ArrayList<>();


    public void addAntenna(int i){
        antennaListInt.add(i);
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

    public eAntennaNo getAntennaNum(int i){

        switch (i){
            case 1:return eAntennaNo._1;
            case 2:return eAntennaNo._2;
            case 3:return eAntennaNo._3;
            case 4:return eAntennaNo._4;
            case 5:return eAntennaNo._5;
            case 6:return eAntennaNo._6;
            case 7:return eAntennaNo._7;
            case 8:return eAntennaNo._8;
            case 9:return eAntennaNo._9;
            case 10:return eAntennaNo._10;
            case 11:return eAntennaNo._11;
            case 12:return eAntennaNo._12;
            case 13:return eAntennaNo._13;
            case 14:return eAntennaNo._14;
            case 15:return eAntennaNo._15;
            case 16:return eAntennaNo._16;
            case 17:return eAntennaNo._17;
            case 18:return eAntennaNo._18;
            case 19:return eAntennaNo._19;
            case 20:return eAntennaNo._20;
            case 21:return eAntennaNo._21;
            case 22:return eAntennaNo._22;
            case 23:return eAntennaNo._23;
            case 24:return eAntennaNo._24;
        }
        return null;
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
        connected= RFIDReader.CreateTcpConn(IPAddress, this);
        return connected;
    }


    @Override
    public int readEPC(eReadType readType) {
        return RFIDReader._Tag6C.GetEPC_TID(IPAddress, antennaNo, readType);
    }

    @Override
    public String stop() {
        return
                RFIDReader.Stop(IPAddress);
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

    public ArrayList<Integer> getAntennaListInt() {
        return antennaListInt;

    }

    public int readEPCAll(eReadType readType) {
        if(antennaList.size()<1)
            return -1000;
        int antNum=0;
        for( eAntennaNo antNo : antennaList){
            antNum+=antNo.GetNum();
        }

        antennaNo=antNum;
        RFIDReader._Config.SetReaderANT(IPAddress,antennaNo);
        return RFIDReader._Tag6C.GetEPC_TID(IPAddress, antennaNo, readType);


    }
}
