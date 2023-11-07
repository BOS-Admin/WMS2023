package com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager;

import com.rfidread.Enumeration.eReadType;
import com.rfidread.Interface.IAsynchronousMessage;
public abstract class RFIDDevice implements IAsynchronousMessage {

    private String connParam;
    private String type;
    private int antennaNo;
    private int power;
    private int time;
    private boolean connected;

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getConnParam() {
        return connParam;
    }

    public void setConnParam(String connParam) {
        this.connParam = connParam;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getAntennaNo() {
        return antennaNo;
    }

    public void setAntennaNo(int antennaNo) {
        this.antennaNo = antennaNo;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public abstract boolean connect();

    public abstract int readEPC(eReadType readType);

    public abstract String stop();
    public abstract boolean SetBeep(boolean on);


    public abstract boolean isConnected();


    public abstract boolean refresh();

    public abstract boolean disconnect() ;
}
