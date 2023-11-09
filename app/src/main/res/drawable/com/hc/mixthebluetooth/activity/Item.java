package com.hc.mixthebluetooth.activity;

import java.util.ArrayList;

public class Item implements Runnable{

    public static int idSerial=1;
    public int id;
    private boolean doneRfidLotBond;
    private boolean doneRfidReading;
    private boolean readingRfid;
    private String mName;
    private String step1;
    private String step2;
    private String step3;
    private String step4;
    private String UPC;
    private String RFID;
    private ReceivingMachineActivity parent;

    private boolean notDone;

    public void setUPC(String upc) {
        UPC = upc;
        step1="UPC: "+upc;
    }

    public void setRFID(String rfid) {
        RFID=rfid;
        step2="RFID: "+rfid;
    }

    @Override
    public void run() {

        while(!parent.requestRFIDResource(this)){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        readingRfid=true;
        parent.startReading();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        parent.stopReading();
        readingRfid=false;
        doneRfidReading=true;
        setRFID(parent.getRFIDList());
        parent.notifyItemChanged(this);
        if(RFID.startsWith("multi"))
            return;

    }











    public Item(String upc,ReceivingMachineActivity parent){
        setUPC(upc);
        this.parent=parent;
        parent.notifyItemChanged(this);
        // Getting RFID
        id=idSerial;
        idSerial++;
    }


    public boolean isDoneRfidReading() {
        return doneRfidReading;
    }

    public void setDoneRfidReading(boolean doneRfidReading) {
        this.doneRfidReading = doneRfidReading;
    }

    public static int getIdSerial() {
        return idSerial;
    }

    public static void setIdSerial(int idSerial) {
        Item.idSerial = idSerial;
    }

    public boolean isReadingRfid() {
        return readingRfid;
    }

    public void setReadingRfid(boolean readingRfid) {
        this.readingRfid = readingRfid;
    }

    public boolean isDoneRfidLotBond() {
        return doneRfidLotBond;
    }

    public void setDoneRfidLotBond(boolean doneRfidLotBond) {
        this.doneRfidLotBond = doneRfidLotBond;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public String getStep1() {
        return step1;
    }

    public void setStep1(String step1) {
        this.step1 = step1;
    }

    public String getStep2() {
        return step2;
    }

    public void setStep2(String step2) {
        this.step2 = step2;
    }

    public String getStep3() {
        return step3;
    }

    public void setStep3(String step3) {
        this.step3 = step3;
    }

    public String getStep4() {
        return step4;
    }

    public void setStep4(String step4) {
        this.step4 = step4;
    }

    public boolean ismOnline() {
        return mOnline;
    }

    public void setmOnline(boolean mOnline) {
        this.mOnline = mOnline;
    }

    public static int getLastContactId() {
        return lastContactId;
    }

    public static void setLastContactId(int lastContactId) {
        Item.lastContactId = lastContactId;
    }

    private boolean mOnline;

    public Item(String name, boolean online) {
        mName = name;
        mOnline = online;
    }

    public String getName() {
        return mName;
    }

    public boolean isOnline() {
        return mOnline;
    }

    private static int lastContactId = 0;

    public static ArrayList<Item> createContactsList(int num) {
        ArrayList<Item> contacts = new ArrayList<Item>();
        return contacts;
    }



}
