package com.bos.wms.mlkit.app.adapters;

public class ItemSerialScannedDataModel {

    private String itemSerial, upc;

    public ItemSerialScannedDataModel(String itemSerial, String upc){
        this.itemSerial = itemSerial;
        this.upc = upc;
    }

    public String getItemSerial(){
        return itemSerial;
    }

    public void setItemSerial(String itemSerial, String upc){
        this.itemSerial = itemSerial;
    }

    public String getUPC(){
        return upc;
    }

    public void setUPC(String upc){
        this.upc = upc;
    }

}
