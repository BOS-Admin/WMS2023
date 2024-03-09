package com.bos.wms.mlkit.utils.RFID;

import android.os.StrictMode;

import com.bos.wms.mlkit.app.Logger;
import com.rfidread.Connect.BaseConnect;
import com.rfidread.Enumeration.eAntennaNo;
import com.rfidread.Enumeration.eReadType;
import com.rfidread.Helper.Helper_ThreadPool;
import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.Models.Tag_Model;
import com.rfidread.RFIDReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RFIDAntennaHandler
{
    private String ipAddress = ""; // Same as Device Name

    private int antennaNo;

    private boolean isReading = false;

    private boolean isConnected = false;

    private ArrayList<eAntennaNo> antennaList;

    private ArrayList<Integer> antennaListInt;

    private String currentRFIDCommand = "6C"; //Command Could Either Be 6C or 6B For Reading

    private HashMap<String, Integer> allReadRFIDS;

    private boolean isReadingPaused = true;

    private RFIDHandlerOutputListener outputListener;

    private RFIDConnectionListener connectionListener;

    private int numberOfReadsBeforeProcess = 1;

    private boolean isPowerChanged = false;

    private int currentPower = 3;

    //region Getters and Setters
    public void setIsReading(boolean isReading)
    {
        this.isReading = isReading;
    }

    public boolean getIsReading()
    {
        return this.isReading;
    }

    public void setAntennaNo(int antennaNo)
    {
        this.antennaNo = antennaNo;
    }

    public int getAntennaNo()
    {
        return this.antennaNo;
    }

    public void setIpAddress(String ipAddress)
    {
        this.ipAddress = ipAddress;
    }

    public String getIpAddress()
    {
        return this.ipAddress;
    }

    public void setIsConnected(boolean isConnected)
    {
        this.isConnected = isConnected;
    }

    public boolean getIsConnected()
    {
        return this.isConnected;
    }

    public void setAntennaList(ArrayList<eAntennaNo> antennaList)
    {
        this.antennaList = antennaList;
    }

    public ArrayList<eAntennaNo> getAntennaList()
    {
        return this.antennaList;
    }

    public void setAntennaListInt(ArrayList<Integer> antennaListInt)
    {
        this.antennaListInt = antennaListInt;
    }

    public ArrayList<Integer> getAntennaListInt()
    {
        return this.antennaListInt;
    }

    public void setCurrentRFIDCommand(String currentRFIDCommand)
    {
        this.currentRFIDCommand = currentRFIDCommand;
    }

    public String getCurrentRFIDCommand()
    {
        return this.currentRFIDCommand;
    }

    public void setAllReadRFIDS(HashMap<String, Integer> allReadRFIDS)
    {
        this.allReadRFIDS = allReadRFIDS;
    }

    public HashMap<String, Integer> getAllReadRFIDS()
    {
        return this.allReadRFIDS;
    }

    public void setIsReadingPaused(boolean isReadingPaused)
    {
        this.isReadingPaused = isReadingPaused;
    }

    public boolean getIsReadingPaused()
    {
        return this.isReadingPaused;
    }

    public void setOutputListener(RFIDHandlerOutputListener outputListener)
    {
        this.outputListener = outputListener;
        resetAllReadRFIDS();
    }

    public RFIDHandlerOutputListener getOutputListener()
    {
        return outputListener;
    }

    public void setConnectionListener(RFIDConnectionListener connectionListener)
    {
        this.connectionListener = connectionListener;
    }

    public RFIDConnectionListener getConnectionListener()
    {
        return this.connectionListener;
    }

    public void SetConnectionListener(RFIDConnectionListener listener){
        this.connectionListener = listener;
    }

    public void setNumberOfReadsBeforeProcess(int numberOfReadsBeforeProcess)
    {
        this.numberOfReadsBeforeProcess = numberOfReadsBeforeProcess;
    }

    public int getNumberOfReadsBeforeProcess()
    {
        return this.numberOfReadsBeforeProcess;
    }
    //endregion

    public RFIDAntennaHandler(String ipAddress)
    {
        this.ipAddress = (isValidIpAddress(ipAddress)) ? "" : ipAddress;
        this.allReadRFIDS = new HashMap<>();
        this.antennaList = new ArrayList<>();
        this.antennaListInt = new ArrayList<>();
    }

    public void setIsPowerChanged(boolean isPowerChanged)
    {
        this.isPowerChanged = isPowerChanged;
    }

    public boolean getIsPowerChanged()
    {
        return this.isPowerChanged;
    }

    public int getCurrentPower()
    {
        return currentPower;
    }

    public void setCurrentPower(int currentPower)
    {
        this.currentPower = currentPower;
        this.isPowerChanged = true;
        if(this.isConnected)
            StartConnection(false);
    }

    public void addAntenna(int i)
    {
        getAntennaListInt().add(i);

        switch (i)
        {
            case 1 :getAntennaList().add(eAntennaNo._1); break;
            case 2 :getAntennaList().add(eAntennaNo._2); break;
            case 3 :getAntennaList().add(eAntennaNo._3); break;
            case 4 :getAntennaList().add(eAntennaNo._4); break;
            case 5 :getAntennaList().add(eAntennaNo._5); break;
            case 6 :getAntennaList().add(eAntennaNo._6); break;
            case 7 :getAntennaList().add(eAntennaNo._7); break;
            case 8 :getAntennaList().add(eAntennaNo._8); break;
            case 9 :getAntennaList().add(eAntennaNo._9); break;
            case 10:getAntennaList().add(eAntennaNo._10);break;
            case 11:getAntennaList().add(eAntennaNo._11);break;
            case 12:getAntennaList().add(eAntennaNo._12);break;
            case 13:getAntennaList().add(eAntennaNo._13);break;
            case 14:getAntennaList().add(eAntennaNo._14);break;
            case 15:getAntennaList().add(eAntennaNo._15);break;
            case 16:getAntennaList().add(eAntennaNo._16);break;
            case 17:getAntennaList().add(eAntennaNo._17);break;
            case 18:getAntennaList().add(eAntennaNo._18);break;
            case 19:getAntennaList().add(eAntennaNo._19);break;
            case 20:getAntennaList().add(eAntennaNo._20);break;
            case 21:getAntennaList().add(eAntennaNo._21);break;
            case 22:getAntennaList().add(eAntennaNo._22);break;
            case 23:getAntennaList().add(eAntennaNo._23);break;
            case 24:getAntennaList().add(eAntennaNo._24);break;
        }
    }

    public eAntennaNo getAntennaNum(int i)
    {
        switch (i)
        {
            case 1 :return eAntennaNo._1;
            case 2 :return eAntennaNo._2;
            case 3 :return eAntennaNo._3;
            case 4 :return eAntennaNo._4;
            case 5 :return eAntennaNo._5;
            case 6 :return eAntennaNo._6;
            case 7 :return eAntennaNo._7;
            case 8 :return eAntennaNo._8;
            case 9 :return eAntennaNo._9;
            case 10:return eAntennaNo._10;
            case 11:return eAntennaNo._11;
            case 12:return eAntennaNo._12;
            case 13:return eAntennaNo._13;
            case 14:return eAntennaNo._14;
            case 15:return eAntennaNo._15;
            case 16:return eAntennaNo._16;
            case 17:return eAntennaNo._17;
            case 18:return eAntennaNo._18;
            case 19:return eAntennaNo._19;
            case 20:return eAntennaNo._20;
            case 21:return eAntennaNo._21;
            case 22:return eAntennaNo._22;
            case 23:return eAntennaNo._23;
            case 24:return eAntennaNo._24;
            default: return null;
        }
    }

    /**
     * This function configures the Antenna RFID reader to read EPC 6C/6B Tags
     */
    public void StartRead()
    {
        Helper_ThreadPool.ThreadPool_StartSingle(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    StartRFIDRead(getCurrentRFIDCommand());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    public void StartReadAfterWriteMatchTid(String tid)
    {
        Helper_ThreadPool.ThreadPool_StartSingle(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    StartRFIDReadAfterWriteMatchTid(getCurrentRFIDCommand(), tid);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    /*
    * This function will use all of the antennas to read tags
    * */
    private int StartRFIDRead(String command)
    {

        Logger.Debug("StartRFIDRead - RFIDAntennaHandler", "RFID Reading Started!");

        if (getAntennaList().size() < 1)
            return -1000;

        int antNum = 0;

        for (eAntennaNo antNo : getAntennaList())
        {
            antNum += antNo.GetNum();
        }

        setAntennaNo(antNum);

        RFIDReader._Config.SetReaderANT(getIpAddress(), getAntennaNo());

        if (!RFIDReader.HP_CONNECT.containsKey(getIpAddress()))
            return -1;

        setIsReading(true);

        if (command.equalsIgnoreCase("6C"))
        {
            return RFIDReader._Tag6C.GetEPC_TID(getIpAddress(), getAntennaNo(), eReadType.Inventory);
        }
        else
        {
            return RFIDReader._Tag6B.Get6B(getIpAddress(), 1, eReadType.Inventory.GetNum(), 0);
        }
    }

    private int StartRFIDReadAfterWriteMatchTid(String command, String tid)
    {
        Logger.Debug("StartRFIDReadAfterWriteMatchTid - RFIDAntennaHandler", "RFID Reading Started and will read Tag with TID = " + tid);

        if (getAntennaList().size() < 1)
            return -1000;

        int antNum = 0;

        for (eAntennaNo antNo : getAntennaList())
        {
            antNum += antNo.GetNum();
        }

        setAntennaNo(antNum);

        RFIDReader._Config.SetReaderANT(getIpAddress(), getAntennaNo());

        if (!RFIDReader.HP_CONNECT.containsKey(getIpAddress()))
            return -1;

        setIsReading(true);

        if (command.equalsIgnoreCase("6C"))
        {
            return RFIDReader._Tag6C.GetEPC_MatchTID(getIpAddress(), getAntennaNo(), eReadType.Inventory, tid);
        }
        else
        {
            //return RFIDReader._Tag6B.Get6B(getIpAddress(), 1, eReadType.Inventory.GetNum(), 0);
            return -1;
        }
    }

    /**
     * This function is also used to close the connection with the Antenna Reader
     * */
    public void CloseConnection()
    {
        if(getIsReading())
            StopRead();

        setIsReading(false);

        RFIDReader.CloseAllConnect();
    }

    /*
    * This function is used to stop the Antenna from reading Tags
    * */
    public void StopRead()
    {
        try
        {
            setIsReading(false);
            RFIDReader._Config.Stop(getIpAddress());
        }
        catch (Exception e)
        {
            Logger.Error("RFID-ANTENNA-READER", "RFIDAntennaStopRead - Error While Stopping The Read Command: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
    * This function is used to validate the format of an IP Address
    * */
    private boolean isValidIpAddress(String ipAddress)
    {
        // Regex for digit from 0 to 255.
        String zeroTo255 = "(\\d{1,2}|(0|1)\\" + "d{2}|2[0-4]\\d|25[0-5])";
        // Regex for a digit from 0 to 255 and followed by a dot, repeat 4 times. this is the regex to validate an IP address.
        String regex= zeroTo255 + "\\."+ zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255;
        // Compile the ReGex
        Pattern p = Pattern.compile(regex);
        // If the IP address is empty return false
        if (ipAddress == null)
        {
            return false;
        }
        // Pattern class contains matcher() method to find matching between given IP address and regular expression.
        Matcher m = p.matcher(ipAddress);
        // Return if the IP address matched the ReGex
        return m.matches();
    }

    public void processDetectedTag(Tag_Model tag){

        Logger.Debug("RFID-ANTENNA", "processDetectedTag");

        if(getIsReadingPaused())
            return;

        String rfid = GetRFIDFromTag(tag);

        if(getOutputListener() != null)
            getOutputListener().onTagRead(rfid, tag);

        if(getNumberOfReadsBeforeProcess() == -1)
        {
            if(getAllReadRFIDS().containsKey(rfid))
            {
                getAllReadRFIDS().replace(rfid, getAllReadRFIDS().get(rfid) + 1);
            }
            else
            {
                getAllReadRFIDS().put(rfid, 1);
            }

            if(getOutputListener() != null)
            {
                getOutputListener().onTagProcessed(rfid, tag, getAllReadRFIDS().get(rfid), getAllReadRFIDS().size() == 1);
            }
            return;
        }

        if(getAllReadRFIDS().containsKey(rfid))
        {
            getAllReadRFIDS().replace(rfid, getAllReadRFIDS().get(rfid) + 1);
        }
        else
        {
            getAllReadRFIDS().put(rfid, 1);
        }

        if(getAllReadRFIDS().get(rfid) >= getNumberOfReadsBeforeProcess())
        {
            if(getOutputListener() != null)
            {
                getOutputListener().onTagProcessed(rfid, tag, getAllReadRFIDS().get(rfid) != null ? getAllReadRFIDS().get(rfid) : 0, getAllReadRFIDS().size() == 1);
            }

            getAllReadRFIDS().remove(rfid);
        }
    }

    public String GetRFIDFromTag(Tag_Model model) {
        if (model._EPC != null && model._TID != null && !model._EPC.isEmpty() && !model._TID.isEmpty())
            return (model._EPC.length() > 24 ? model._EPC.substring(0, 24) : model._EPC) + "-" + (model._TID.length() > 24 ? model._TID.substring(0, 24) : model._TID);
        return "";
    }

    public void resetAllReadRFIDS()
    {
        getAllReadRFIDS().clear();
        Logger.Debug("RFID-ANTENNA", "ResetAllReadRFIDS - Reset All RFIDs!");
    }

    public void StartConnection(boolean newConnection)
    {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Helper_ThreadPool.ThreadPool_StartSingle(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    CloseConnection();

                    if (getIpAddress() != null && !getIpAddress().isEmpty())
                    {
                        if (!RFIDReader.CreateTcpConn(getIpAddress(), new IAsynchronousMessage()
                        {
                            @Override
                            public void WriteDebugMsg(String s) {}

                            @Override
                            public void WriteLog(String s) {}

                            @Override
                            public void PortConnecting(String s) {}

                            @Override
                            public void PortClosing(String s) {}

                            @Override
                            public void OutPutTags(Tag_Model model)
                            {
                                processDetectedTag(model);
                            }

                            @Override
                            public void OutPutTagsOver() {}

                            @Override
                            public void GPIControlMsg(int i, int i1, int i2) {}

                            @Override
                            public void OutPutScanData(byte[] bytes) {}
                        }))
                        {

                        }
                        else
                        {
                            Logger.Debug("RFID-ANTENNA", "StartConnection - Antenna " + getIpAddress() + " Connected Successfully!");
                            RFIDReader._Config.SetReaderAutoSleepParam(getIpAddress(), false, "");

                            if(getIsPowerChanged())
                                applyCurrentPowerToAllAntennas();

                            setIsPowerChanged(false);
                        }
                    }

                    HashMap<String, BaseConnect> lst = RFIDReader.HP_CONNECT;

                    if (lst.keySet().stream().count() > 0)
                    {
                        setIsConnected(true);

                        if(getConnectionListener() != null)
                        {
                            getConnectionListener().onConnectionEstablished(getIpAddress(), newConnection);
                        }
                    }
                    else
                    {
                        if(getConnectionListener() != null)
                            getConnectionListener().onConnectionFailed("Couldn't Connect To Antenna With IP " + getIpAddress(), newConnection);

                        Logger.Debug("RFID-ANTENNA", "StartConnection - Couldn't Connect To Antenna With Ip " + getIpAddress());
                    }
                }
                catch (Exception ex)
                {
                    if(getConnectionListener() != null)
                        getConnectionListener().onConnectionFailed(ex.getMessage(), newConnection);

                    Logger.Debug("RFID-ANTENNA", "StartConnection - " + ex.getMessage());
                }
            }
        });
    }

    private boolean applyCurrentPowerToAllAntennas()
    {
        HashMap<Integer, Integer> powerParam = new HashMap<Integer, Integer>();

        for(int i = 1; i <= 24; i++)
        {
            powerParam.put(i, getCurrentPower());
        }

        if(RFIDReader._Config.SetANTPowerParam (getIpAddress(), powerParam) != 0)
        {
            Logger.Debug("RFID-ANTENNA", "ApplyPower - Failed Applying Power For Antenna " + getIpAddress() + " Power " + getCurrentPower());
            return false;
        }
        else
        {
            Logger.Debug("RFID-ANTENNA", "ApplyPower - Applied Power For Antenna " + getIpAddress() + " Power " + getCurrentPower());
            return true;
        }
    }

    /**
     * Function that will write given EPC into TAG of Type 6C using the Antenna.
     * */
    public boolean writeEPC_6C(String newEPC, String tid, int antNo)
    {
        // RFID Reader name not found
        if (!RFIDReader.HP_CONNECT.containsKey(getIpAddress()))
        {
            Logger.Log("RFIDHandler", "RFID-ANTENNA", "RFID-Antenna not found !");
            return false;
        }
        return (RFIDReader._Tag6C.WriteEPC_MatchTID(getIpAddress(), antNo, newEPC, tid, 0)) != -1;
    }

    /**
     * Function that will write given EPC into TAG of Type 6B using the Antenna.
     * */
    public boolean writeEPC_6B(String TID, String newEPC, int antNo)
    {
        // RFID Reader name not found
        if (!RFIDReader.HP_CONNECT.containsKey(getIpAddress()))
        {
            Logger.Log("RFIDHandler", "RFID-ANTENNA", "RFID-Antenna name not found !");
            return false;
        }
        return (RFIDReader._Tag6B.Write6B(getIpAddress(), antNo, TID, 0, newEPC)) != -1;
    }
}