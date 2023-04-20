package com.bos.wms.mlkit.app.adapters;

public class ItemSerialScannedDataModel {

    private String itemSerial, upc,price;

    public ItemSerialScannedDataModel(String itemSerial, String upc,String price){
        this.itemSerial = itemSerial;
        this.upc = upc;
        this.price=price;
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

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }
}
