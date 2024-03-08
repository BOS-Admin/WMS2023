package com.bos.wms.mlkit.utils.RFID;

import static com.rfidread.RFIDReader._Tag6B;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import com.bos.wmsapp.Utils.Logger;
import com.rfidread.Connect.BaseConnect;
import com.rfidread.Enumeration.eReadType;
import com.rfidread.Helper.Helper_ThreadPool;
import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.Models.Tag_Model;
import com.rfidread.RFIDReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RFIDSledHandler {

    private String macAddress;
    private String deviceName;

    private List<BluetoothDevice> BluetoothDeviceList = new ArrayList();
    private List<String> BluetoothDevicelistStr = new ArrayList();
    private List<String> BluetoothDevicelistMac = new ArrayList();

    boolean IsConnected = false, IsReading = false;

    boolean PowerChanged = false;

    boolean isMacAddressADeviceName = false;

    //private ArrayList<RFIDHandlerOutputListener> outputListeners;

    private RFIDHandlerOutputListener outputListener;

    private RFIDHandlerTriggerListener triggerListener;

    private int currentPower = 3;

    private int numberOfReadsBeforeProcess = 1;

    private RFIDConnectionListener connectionListener;

    private HashMap<String, Integer> AllReadRFIDS;

    private ArrayList<String> TriggerCachedRFIDs = new ArrayList<>();

    private String currentRFIDCommand = "6C";//Command Could Either Be 6C or 6B For Reading

    private boolean IsTriggerKeyPressed = false;

    private boolean IsTriggerBased = false;

    private boolean IsReadingPaused = false;

    /**
     * Intance Class For The RFID Sled Handler
     * @param macAddress
     */
    public RFIDSledHandler(String macAddress){
        this.macAddress = macAddress;
        deviceName = "Unknown";

        //outputListeners = new ArrayList<>();

        isMacAddressADeviceName = !IsValidMacAddress(macAddress);

        AllReadRFIDS = new HashMap<>();

    }

    /**
     * This function attempts the rfid reader connection with the corresponding mac address or device name
     */
    public void StartConnection(boolean newConnection) {
        Helper_ThreadPool.ThreadPool_StartSingle(new Runnable() {
            @Override
            public void run() {
                try {
                    if(!isMacAddressADeviceName) {
                        GetBT4DeviceStrList();
                        deviceName = GetBluetoothDeviceNameFromMac(macAddress);
                    }else {
                        deviceName = macAddress;
                    }

                    CloseConnection();

                    RFIDReader.GetBT4DeviceStrList();

                    if (deviceName != null && !deviceName.isEmpty()) {
                        if (!RFIDReader.CreateBT4Conn(deviceName, new IAsynchronousMessage() {
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
                            public void OutPutTags(Tag_Model model) {
                                ProcessDetectedTag(model);
                            }

                            @Override
                            public void OutPutTagsOver() {
                                Logger.Log("RFIDHandler", "RFID-SLED", macAddress + " Output Tags Over!");
                            }

                            @Override
                            public void GPIControlMsg(int i, int i1, int i2) {
                                //i1 and i2 = 1 Trigger Released, otherwise trigger clicked
                                Logger.Error("RFID-SLED",  "GPI Control: i=" + i + " i1=" + i1 + "i2=" + i2);
                                if(i1 == 0 && i2 == 0){
                                    IsTriggerKeyPressed = true;

                                    if(triggerListener != null)
                                        triggerListener.onTriggerPressed();
                                }else {
                                    if(triggerListener != null && IsTriggerKeyPressed)
                                    {
                                        triggerListener.onTriggerReleased();
                                        triggerListener.onTriggerReleasedRFIDs(TriggerCachedRFIDs);
                                        TriggerCachedRFIDs.clear();
                                    }
                                    IsTriggerKeyPressed = false;
                                }
                            }

                            @Override
                            public void OutPutScanData(byte[] bytes) {

                            }
                        })) {

                        } else {
                            Logger.Log("RFIDHandler", "RFID-SLED", "StartConnection - Sled " + deviceName + " MAC " + macAddress + " Connected Successfully!");
                            RFIDReader._Config.SetReaderAutoSleepParam(deviceName, false, "");
                            if(PowerChanged)
                                applyCurrentPower(true, 0);
                            PowerChanged = false;
                        }
                    }

                    HashMap<String, BaseConnect> lst = RFIDReader.HP_CONNECT;
                    if (lst.keySet().stream().count() > 0) {
                        IsConnected = true;
                        if(connectionListener != null){
                            connectionListener.onConnectionEstablished(deviceName, newConnection);
                        }
                    } else {

                        if(connectionListener != null){
                            connectionListener.onConnectionFailed("Couldn't Connect To Sled With Mac " + macAddress, newConnection);
                        }
                        Logger.Log("RFIDHandler", "RFID-SLED", "StartConnection - Couldn't Connect To Sled With Mac " + macAddress);
                        if(!macAddress.isEmpty() && !macAddress.equalsIgnoreCase("00:00:00:00:00:00")) {
                            StartConnection(false);
                        }
                    }

                } catch (Exception ex) {
                    if(connectionListener != null){
                        connectionListener.onConnectionFailed(ex.getMessage(), newConnection);
                    }
                    Logger.Log("RFIDHandler", "RFID-SLED", "StartConnection - " + ex.getMessage());
                }
            }
        });
    }

    /**
     * Closes The Sled Connection
     */
    public void CloseConnection(){
        if(IsReading)
            StopRead();
        IsConnected = false;
        RFIDReader.CloseAllConnect();
    }

    /**
     * Processes The Detected Tag And Triggers The Corresponding Events
     * @param tag
     */
    public void ProcessDetectedTag(Tag_Model tag){

        if(IsReadingPaused)
            return;

        if(IsTriggerBased && !IsTriggerKeyPressed)
            return;

        String rfid = GetRFIDFromTag(tag);


        if(!rfid.startsWith("BBB"))
            return;

        if(IsTriggerBased && !TriggerCachedRFIDs.contains(rfid)){
            TriggerCachedRFIDs.add(rfid);
        }

        /*for(RFIDHandlerOutputListener listener: outputListeners){
            if(listener != null){
                listener.onTagRead(rfid, tag);
            }
        }*/

        if(outputListener != null){
            outputListener.onTagRead(rfid, tag);
        }

        if(numberOfReadsBeforeProcess == -1){
            if(AllReadRFIDS.containsKey(rfid)){
                AllReadRFIDS.replace(rfid, AllReadRFIDS.get(rfid) + 1);
            }else {
                AllReadRFIDS.put(rfid, 1);
            }

            /*for(RFIDHandlerOutputListener listener: outputListeners){
                if(listener != null){
                    listener.onTagProcessed(rfid, tag, AllReadRFIDS.get(rfid), AllReadRFIDS.size() == 1);
                }
            }*/
            if(outputListener != null){
                outputListener.onTagProcessed(rfid, tag, AllReadRFIDS.get(rfid), AllReadRFIDS.size() == 1);
            }
            return;

        }

        if(AllReadRFIDS.containsKey(rfid)){
            AllReadRFIDS.replace(rfid, AllReadRFIDS.get(rfid) + 1);
        }else {
            AllReadRFIDS.put(rfid, 1);
        }

        if(AllReadRFIDS.get(rfid) >= numberOfReadsBeforeProcess){
            /*for(RFIDHandlerOutputListener listener: outputListeners){
                if(listener != null){
                    listener.onTagProcessed(rfid, tag, AllReadRFIDS.get(rfid) != null ? AllReadRFIDS.get(rfid) : 0, AllReadRFIDS.size() == 1);
                }
            }*/
            if(outputListener != null){
                outputListener.onTagProcessed(rfid, tag, AllReadRFIDS.get(rfid) != null ? AllReadRFIDS.get(rfid) : 0, AllReadRFIDS.size() == 1);
            }

            AllReadRFIDS.remove(rfid);
        }
    }

    public RFIDHandlerTriggerListener getTriggerListener() {
        return triggerListener;
    }

    public void setTriggerListener(RFIDHandlerTriggerListener triggerListener) {
        this.triggerListener = triggerListener;
    }

    /**
     * Adds an output listener for when a tag is detected, and when it is processed past the configured constraints
     * @param listener
     */
    /*public void AddOnOutputEventListener(RFIDHandlerOutputListener listener){
        outputListeners.clear();
        outputListeners.add(listener);
    }*/

    /**
     * Adds an output listener for when a tag is detected, and when it is processed past the configured constraints
     * @param listener
     */
    public void SetOnOutputEventListener(RFIDHandlerOutputListener listener){
        setReadingPaused(false);
        this.outputListener = listener;
        resetAllReadRFIDS();
    }

    /**
     * Sets The Connection Listener For The RFID Handler
     * @param listener
     */
    public void SetConnectionListener(RFIDConnectionListener listener){
        this.connectionListener = listener;
    }

    /**
     * This function returns the rfid key of a tag
     * @param model
     * @return
     */
    public String GetRFIDFromTag(Tag_Model model) {
        if (model._EPC != null && model._TID != null && !model._EPC.isEmpty() && !model._TID.isEmpty())
            return (model._EPC.length() > 24 ? model._EPC.substring(0, 24) : model._EPC) + "-" + (model._TID.length() > 24 ? model._TID.substring(0, 24) : model._TID);
        return "";
    }

    /**
     * This function configures the RFID reader to read EPC 6C/6B Tags
     */
    public void StartRead() {
        Helper_ThreadPool.ThreadPool_StartSingle(new Runnable() {
            @Override
            public void run() {
                try {
                    StartRFIDRead(currentRFIDCommand);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Starts The RFID Reading Depending On The Command
     * @param command
     */
    private int StartRFIDRead(String command){
        if(command.equalsIgnoreCase("6C")){

            if (!RFIDReader.HP_CONNECT.containsKey(deviceName))
                return -1;
            IsReading = true;
            return RFIDReader._Tag6C.GetEPC_TID(deviceName, 1, eReadType.Inventory);
        }else {

            if (!RFIDReader.HP_CONNECT.containsKey(deviceName))
                return -1;
            IsReading = true;
            return _Tag6B.Get6B(deviceName, 1, eReadType.Inventory.GetNum(), 0);
        }
    }

    /**
     * This function stop the configured rfid readings
     */
    public void StopRead() {
        try {
            IsReading = false;
            RFIDReader._Config.Stop(deviceName);
        } catch (Exception e) {
            Logger.Error("RFID-READER", "RFIDStopRead - Error While Stopping The Read Command: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This function returns all the Bluetooth paired devices
     * @return
     */
    @SuppressLint("MissingPermission")
    public List<String> GetBT4DeviceStrList() {

        BluetoothDeviceList.clear();
        BluetoothDevicelistStr.clear();
        BluetoothDevicelistMac.clear();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            Iterator var1 = pairedDevices.iterator();

            while (var1.hasNext()) {
                BluetoothDevice device = (BluetoothDevice) var1.next();
                try {
                    BluetoothDeviceList.add(device);
                    BluetoothDevicelistStr.add(device.getName());
                    BluetoothDevicelistMac.add(device.getAddress());
                } catch (Exception var4) {
                }
            }
        }

        return BluetoothDevicelistStr;
    }

    /**
     * Gets the bluetooth device name from its mac address
     * @param Mac
     * @return
     */
    @SuppressLint("MissingPermission")
    private String GetBluetoothDeviceNameFromMac(String Mac) {
        if (!(Mac != null && !Mac.isEmpty())) {
            return "";
        }
        GetBT4DeviceStrList();
        for (BluetoothDevice d : BluetoothDeviceList) {
            if (d.getAddress() != null && d.getAddress().contains(Mac))
                return d.getName();
            //something here
        }
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(Mac);
        if (device == null)
            return "";
        return device.getName();
    }

    /**
     * This Checks If A Given String Is A Valid Mac Address
     * @param str
     * @return
     */
    private boolean IsValidMacAddress(String str)
    {
        String regex = "^([0-9A-Fa-f]{2}[:-])"
                + "{5}([0-9A-Fa-f]{2})|"
                + "([0-9a-fA-F]{4}\\."
                + "[0-9a-fA-F]{4}\\."
                + "[0-9a-fA-F]{4})$";

        Pattern p = Pattern.compile(regex);
        if (str == null)
        {
            return false;
        }
        Matcher m = p.matcher(str);
        return m.matches();
    }

    public int getCurrentPower() {
        return currentPower;
    }

    /**
     * Applies The Current Power Of The Sled
     * @return true if the power is applied, false if an error occurred
     */
    private boolean applyCurrentPower(boolean retry, int trails){
        HashMap<Integer, Integer> powerParam = new HashMap<Integer, Integer>();
        powerParam.put(1, currentPower);
        if(RFIDReader._Config.SetANTPowerParam (deviceName, powerParam) != 0){
            Logger.Log("RFIDHandler", "RFID-SLED", "ApplyPower - Failed Applying Power For Sled " + deviceName + " Mac " + macAddress + " Power " + currentPower);
            if(retry && trails < 50)
                return applyCurrentPower(true, trails + 1);
            return false;
        }else {
            Logger.Log("RFIDHandler", "RFID-SLED", "ApplyPower - Applied Power For Sled " + deviceName + " Mac " + macAddress + " Power " + currentPower);
            return true;
        }
    }

    /**
     * Turn Off Or On The Device Beep
     * @param toggle
     */
    public void setDeviceBeep(boolean toggle){
        String result = RFIDReader.SetBeep(deviceName, toggle ? 0 : 1);
        Logger.Log("RFIDHandler", "RFID-SLED", "Apply Beep Toggle: " + toggle + " For Device: " + deviceName + " Returned: " + result);
    }

    /**
     * Set The Trigger Read Time And Stop Time
     * @param readTime
     * @param stopTime
     */
    public void setTriggerConfigs(int readTime, int stopTime){

        boolean enableTagFocus = false;
        boolean enableFastID = false;

        int bits = enableTagFocus ? 16 : 0;
        if(enableFastID)
            bits |= 32;

        String str2 = "1,0000" + m7206a((short) bits);
        String str = "";
        if (readTime > 0) {
            str = str2 + "&3,0103" + m7206a((short) (readTime / 10));
        } else {
            str = str2 + "&3,00030000";
        }
        String result = RFIDReader.SetBaseBandX(deviceName, str + "&4," + m7204a((byte) (stopTime / 10)) + "000000");
        Logger.Log("RFIDHandler", "RFID-SLED", "Apply Configs ReadTime: " + readTime + " StopTime: " + stopTime + " For Device: " + deviceName + " Returned: " + result);
    }


    /**
     * Sets The Current Sled Power, Note To Apply Power Changes After Connecting To The Sled, You Must Close The Connection And Open It Again
     * @param currentPower
     */
    public void setCurrentPower(int currentPower) {
        this.currentPower = currentPower;
        PowerChanged = true;
        if(IsConnected)
            StartConnection(false);
    }

    public int getNumberOfReadsBeforeProcess() {
        return numberOfReadsBeforeProcess;
    }

    /**
     * Sets The Number Of Times The Same RFID Must Be Read Before Its Processed
     * @param numberOfReadsBeforeProcess
     */
    public void setNumberOfReadsBeforeProcess(int numberOfReadsBeforeProcess) {
        this.numberOfReadsBeforeProcess = numberOfReadsBeforeProcess;
    }

    public String getCurrentRFIDCommand() {
        return currentRFIDCommand;
    }

    /**
     * Sets the reading command for the rfid sled, Either 6C or 6B
     * @param currentRFIDCommand
     */
    public void setCurrentRFIDCommand(String currentRFIDCommand) {
        this.currentRFIDCommand = currentRFIDCommand;
    }

    /**
     * Returns All The Current Read RFIDs With Their Count
     * @return
     */
    public HashMap<String, Integer> getAllReadRFIDS(){
        return AllReadRFIDS;
    }

    /**
     * Resets All The Read RFIDs And Their Count
     */
    public void resetAllReadRFIDS(){
        AllReadRFIDS.clear();
        Logger.Log("RFIDHandler", "RFID-SLED", "ResetAllReadRFIDS - Reset All RFIDs!");
    }

    /**
     * Clears All Output Listeners
     */
    /*public void clearAllOutPutListeners(){
        this.outputListeners = new ArrayList<>();
    }*/

    /**
     * Function From The Decompiled Scripts
     * @param s
     * @return
     */
    private String m7206a(short s) {
        String a = m7204a((byte) ((s >> 8) & 255));
        return a + m7204a((byte) (s & 255));
    }

    /**
     * Function From The Decompiled Scripts
     * @param b
     * @return
     */
    private String m7204a(byte b) {
        try {
            String hexString = Integer.toHexString(b & 255);
            if (hexString.length() == 1) {
                hexString = '0' + hexString;
            }
            return hexString.toUpperCase();
        } catch (Exception unused) {
            return "";
        }
    }

    /**
     * Function that will write given EPC into TAG of Type 6C using the Sled
     * */
    public boolean writeEPC_6C(String newEPC, String oldEPC)
    {
        // RFID Reader name not found
        if (!RFIDReader.HP_CONNECT.containsKey(deviceName))
        {
            Logger.Log("RFIDHandler", "RFID-SLED", "RFID-SLED name not found !");
            return false;
        }
        return (RFIDReader._Tag6C.WriteEPC_MatchEPC(deviceName, 1, newEPC, oldEPC, 0)) != -1;
    }

    /**
     * Function that will write given EPC into TAG of Type 6B using the Sled.
     * */
    public boolean writeEPC_6B(String TID, String newEPC)
    {
        // RFID Reader name not found
        if (!RFIDReader.HP_CONNECT.containsKey(deviceName))
        {
            Logger.Log("RFIDHandler", "RFID-SLED", "RFID-SLED name not found !");
            return false;
        }
        return (RFIDReader._Tag6B.Write6B(deviceName, 1, TID, 0, newEPC)) != -1;
    }

    public boolean isTriggerBased() {
        return IsTriggerBased;
    }

    /**
     * Sets if the sled should only read rfids when the trigger key is held
     * @param triggerBased
     */
    public void setTriggerBased(boolean triggerBased) {
        IsTriggerBased = triggerBased;
    }

    public boolean isTriggerKeyPressed() {
        return IsTriggerKeyPressed;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public boolean isConnected() {
        return IsConnected;
    }

    public boolean isReading() {
        return IsReading;
    }

    public boolean isPowerChanged() {
        return PowerChanged;
    }

    public boolean isMacAddressADeviceName() {
        return isMacAddressADeviceName;
    }

    public boolean isReadingPaused() {
        return IsReadingPaused;
    }

    public void setReadingPaused(boolean readingPaused) {
        IsReadingPaused = readingPaused;
    }
}
