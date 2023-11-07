package com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager;

import com.rfidread.Models.Tag_Model;

public interface RFIDListener {

    public void notifyListener(RFIDDevice device, Tag_Model tag_model);

    public void notifyStartAntenna(int ant);
    public void notifyStopAntenna(int ant);

    public void notifyStartDevice(String message);
    public void notifyEndDevice(String message);



}
