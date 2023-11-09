package com.hc.mixthebluetooth.activity.adapters;

public class ItemSerialScannedDataModel {

    private String itemSerial;

    public ItemSerialScannedDataModel(String itemSerial){
        this.itemSerial = itemSerial;
    }

    public String getItemSerial(){
        return itemSerial;
    }

    public void setItemSerial(String itemSerial, String upc){
        this.itemSerial = itemSerial;
    }
}
