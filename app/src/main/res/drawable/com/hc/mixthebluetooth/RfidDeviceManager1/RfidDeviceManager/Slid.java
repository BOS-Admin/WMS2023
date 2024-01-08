package com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager;

import android.os.StrictMode;

import androidx.navigation.ui.AppBarConfiguration;

import com.rfidread.Enumeration.eReadType;
import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.Models.Tag_Model;
import com.rfidread.RFIDReader;

import java.util.List;

public class Slid extends RFIDDevice implements IAsynchronousMessage {

    private AppBarConfiguration appBarConfiguration;

    private String bluetoothAddress="";
    //private TextView txtView=null;

    public String result="Nothing";

    public String getBluetoothAddress() {
        return bluetoothAddress;
    }

    public void setBluetoothAddress(String bluetoothAddress) {
        setConnParam(bluetoothAddress);
        this.bluetoothAddress = bluetoothAddress;
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

    @Override
    public boolean connect() {

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

        }
        String[] bt4DeviceName=new String[1];

        if(RFIDReader.isSupportBluetooth()){
            List<String> listName = RFIDReader.GetBT4DeviceStrList();
            bt4DeviceName = listName.toArray(new String[listName.size()]);

            for(String s : listName){

            }
        }
        else{

        }
        return RFIDReader.CreateBT4Conn(bluetoothAddress, this);

    }

    @Override
    public int readEPC(eReadType readType) {
        return RFIDReader._Tag6C.GetEPC_TID(bluetoothAddress,1, readType);

    }

    @Override
    public String stop() {
        return RFIDReader.Stop(bluetoothAddress);
    }

    public boolean SetBeep(boolean on){
       return RFIDReader.SetBeep(bluetoothAddress,on?0:1).equals("0");
    }

    @Override
    public boolean isConnected() {
        return RFIDReader.HP_CONNECT.containsKey(bluetoothAddress);
    }

    public boolean refresh(){
        RFIDReader.CloseConn(bluetoothAddress);
        return RFIDDevicesManager.connectRFIDDevice(this);
    }
    public String toString(){
        return "Slid,"+bluetoothAddress+",1,"+getTime()+","+getPower();
    }

    @Override
    public boolean disconnect() {
        try{
            RFIDReader.CloseConn(getBluetoothAddress());
            return true;
        }
        catch (Exception ex){
            return false;
        }
    }

}