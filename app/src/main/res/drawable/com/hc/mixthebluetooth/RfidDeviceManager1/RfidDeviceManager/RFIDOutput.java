package com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager;

import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.Models.Tag_Model;

public class RFIDOutput implements IAsynchronousMessage {


    RFIDListener listener;

    public RFIDOutput(RFIDListener listener) {
        this.listener = listener;
    }

    public void OutPutTags(RFIDDevice device, Tag_Model tag_model) {
        listener.notifyListener(device,tag_model);

    }

    public void AntennaStarted(int ant) {
        listener.notifyStartAntenna(ant);

    }

    public void AntennaStopped(int ant) {
        listener.notifyStopAntenna(ant);

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
}
