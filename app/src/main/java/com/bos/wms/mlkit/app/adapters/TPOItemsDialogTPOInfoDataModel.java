package com.bos.wms.mlkit.app.adapters;

public class TPOItemsDialogTPOInfoDataModel extends TPOItemsDialogDataModel{

    private int id;
    private String toLocation;

    private String dateCreated;
    public TPOItemsDialogTPOInfoDataModel(int id, String toLocation, String message){
        super(message);
        this.id = id;
        this.toLocation = toLocation;
        this.dateCreated = "";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getToLocation() {
        return toLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }
}
