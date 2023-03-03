package com.bos.wms.mlkit.app.adapters;

public class UPCScannedItemDataModel {

    private String upc;

    public UPCScannedItemDataModel(String upc){
        this.upc = upc;
    }

    public String getUPC(){
        return upc;
    }

    public void setUPC(String upc){
        this.upc = upc;
    }

}
