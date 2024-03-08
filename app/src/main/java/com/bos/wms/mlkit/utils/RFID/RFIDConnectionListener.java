package com.bos.wms.mlkit.utils.RFID;

public interface RFIDConnectionListener {

    public void onConnectionEstablished(String deviceName, boolean newConnection);
    public void onConnectionFailed(String reason, boolean newConnection);

}
