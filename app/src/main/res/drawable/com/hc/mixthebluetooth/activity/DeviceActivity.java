package com.hc.mixthebluetooth.activity;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.hc.mixthebluetooth.R;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.Antenna;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDDevice;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDDevicesManager;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDListener;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDOutput;


import com.hc.mixthebluetooth.activity.Rfid.RfidBulkTypeUpdateActivity;
import com.hc.mixthebluetooth.storage.Storage;
import com.rfidread.Enumeration.eAntennaNo;
import com.rfidread.Models.Tag_Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class DeviceActivity extends AppCompatActivity implements RFIDListener {

    Antenna antenna;
    int antennaReadTime=-1;
    private Storage mStorage;

    boolean DiscardItemSerial = false;
    public static boolean isReplenishment = false;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        mStorage = new Storage(this);//sp存储
        String antennaIPAddress="";
        final EditText  textReadTime= (EditText) findViewById(R.id.textReaderTime);
        try {
            mStorage.saveData("AntennaConnected", false);
            antennaIPAddress=mStorage.getDataString("AntennaReaderIPAddress", "");
            String[] antennas= mStorage.getDataString("AntennasSelected","1").split(",");
            antennaReadTime=mStorage.getDataInt("AntennaReadTime", 10000);
            textReadTime.setText(""+antennaReadTime);
            setAntennasCheckBoxesInt(Arrays.asList(antennas));
        }
        catch (Exception e){

        }

        try{
            DiscardItemSerial = getIntent().getBooleanExtra("DiscardItemSerial", false);
        }catch(Exception ex){

        }

        final EditText  textAntennaIP= (EditText) findViewById(R.id.textAntenna);
        textAntennaIP.setText(antennaIPAddress);
        Button btnConnect =findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(e->{

            try{
                antennaReadTime=Integer.parseInt(textReadTime.getText().toString());
            }catch (Exception ex){
                Toast.makeText(getApplicationContext(),"Read Time Error:"+ex.getMessage(),Toast.LENGTH_LONG).show();
                return;
            }

            mStorage.saveData("AntennaReadTime", antennaReadTime);
            mStorage.saveData("AntennaReaderIPAddress", textAntennaIP.getText().toString());
            antenna=new Antenna();
            antenna.setIPAddress(textAntennaIP.getText().toString());
            try{
                setAntennas(antenna);
                String listString = antenna.getAntennaListInt().toString();
                //Toast.makeText(getApplicationContext(),"antenna.getAntennaListInt():"+listString,Toast.LENGTH_SHORT).show();
                mStorage.saveData("AntennasSelected",listString.substring(1, listString.length()-1).replace(" ",""));
               // Toast.makeText(getApplicationContext(),"SetAntenna Successful",Toast.LENGTH_SHORT).show();
            }catch (Exception ex){
                Toast.makeText(getApplicationContext(),"Set Antenna Failed:"+ex.getMessage(),Toast.LENGTH_SHORT).show();
            }

            RFIDDevicesManager.setOutput(new RFIDOutput(this));
            boolean b= RFIDDevicesManager.connectSingleAntenna(antenna);
            //Test
           // b=true;


            if(b){
                //Toast.makeText(getApplicationContext(),"Connection Successful",Toast.LENGTH_SHORT).show();
                mStorage.saveData("AntennaConnected", true);

                //Toast.makeText(getApplicationContext(),"mStorage.getDataString(\"AntennaConnectionNextStep\", \"\")"+mStorage.getDataString("AntennaConnectionNextStep", ""),Toast.LENGTH_SHORT).show();
                if(Objects.equals(mStorage.getDataString("AntennaConnectionNextStep", ""), "RfidBulkUpdate")){
                    Intent myIntent = new Intent(getApplicationContext(), RfidBulkTypeUpdateActivity.class);
                    startActivity(myIntent);
                    finish();
                }
                if(Objects.equals(mStorage.getDataString("AntennaConnectionNextStep", ""), "AuditAntenna")){
                    Intent myIntent = new Intent(getApplicationContext(),isReplenishment?ReplenishmentAuditActivity.class:BBBAuditAllAntennasActivity.class);
                    myIntent.putExtra("DiscardItemSerial", DiscardItemSerial);
                    startActivity(myIntent);
                    finish();
                }
                if(Objects.equals(mStorage.getDataString("AntennaConnectionNextStep", ""), "PackingAntenna")){
                    Intent myIntent = new Intent(getApplicationContext(), PackingReasonActivity.class);
                    startActivity(myIntent);
                    finish();
                }
            }
            else{
                Toast.makeText(getApplicationContext(),"Connection Failed",Toast.LENGTH_SHORT).show();
                mStorage.saveData("AntennaConnected", false);
            }



        });

        Button btnDisconnect =findViewById(R.id.btnDisconnect);
        btnDisconnect.setOnClickListener(e->{


            boolean b= RFIDDevicesManager.disconnectSingle();
            if(b){
                Toast.makeText(getApplicationContext(),"Disconnection Successful",Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(getApplicationContext(),"Disconnection Failed",Toast.LENGTH_LONG).show();

            }



        });

        Button btnSetAntenna =findViewById(R.id.btnSetAntenna);
        btnSetAntenna.setOnClickListener(e->{
            if(antenna!=null){
                try{
                    setAntennas(antenna);
                   // Toast.makeText(getApplicationContext(),"SetAntenna Successful",Toast.LENGTH_SHORT).show();
                }catch (Exception ex){
                    Toast.makeText(getApplicationContext(),"Set Antenna Failed",Toast.LENGTH_SHORT).show();
                }

            }
            else{
                Toast.makeText(getApplicationContext(),"Set Antenna Failed : No antenna connected !! ",Toast.LENGTH_SHORT).show();

            }



        });




    }

    private void setAntennas(Antenna antenna){
        RFIDDevicesManager.clearAllSingleAntennas();
        if(((CheckBox) findViewById(R.id.check1)).isChecked()) antenna.addAntenna(1);
        if(((CheckBox) findViewById(R.id.check2)).isChecked()) antenna.addAntenna(2);
        if(((CheckBox) findViewById(R.id.check3)).isChecked()) antenna.addAntenna(3);
        if(((CheckBox) findViewById(R.id.check4)).isChecked()) antenna.addAntenna(4);
        if(((CheckBox) findViewById(R.id.check5)).isChecked()) antenna.addAntenna(5);
        if(((CheckBox) findViewById(R.id.check6)).isChecked()) antenna.addAntenna(6);
        if(((CheckBox) findViewById(R.id.check7)).isChecked()) antenna.addAntenna(7);
        if(((CheckBox) findViewById(R.id.check8)).isChecked()) antenna.addAntenna(8);
        if(((CheckBox) findViewById(R.id.check9)).isChecked()) antenna.addAntenna(9);
        if(((CheckBox) findViewById(R.id.check10)).isChecked()) antenna.addAntenna(10);
        if(((CheckBox) findViewById(R.id.check11)).isChecked()) antenna.addAntenna(11);
        if(((CheckBox) findViewById(R.id.check12)).isChecked()) antenna.addAntenna(12);
        if(((CheckBox) findViewById(R.id.check13)).isChecked()) antenna.addAntenna(13);
        if(((CheckBox) findViewById(R.id.check14)).isChecked()) antenna.addAntenna(14);
        if(((CheckBox) findViewById(R.id.check15)).isChecked()) antenna.addAntenna(15);
        if(((CheckBox) findViewById(R.id.check16)).isChecked()) antenna.addAntenna(16);
        if(((CheckBox) findViewById(R.id.check17)).isChecked()) antenna.addAntenna(17);
        if(((CheckBox) findViewById(R.id.check18)).isChecked()) antenna.addAntenna(18);
        if(((CheckBox) findViewById(R.id.check19)).isChecked()) antenna.addAntenna(19);
        if(((CheckBox) findViewById(R.id.check20)).isChecked()) antenna.addAntenna(20);
        if(((CheckBox) findViewById(R.id.check21)).isChecked()) antenna.addAntenna(21);
        if(((CheckBox) findViewById(R.id.check22)).isChecked()) antenna.addAntenna(22);
        if(((CheckBox) findViewById(R.id.check23)).isChecked()) antenna.addAntenna(23);
        if(((CheckBox) findViewById(R.id.check24)).isChecked()) antenna.addAntenna(24);

    }

    private void setAntennasCheckBoxes(ArrayList<eAntennaNo> antennaList) {
        ((CheckBox) findViewById(R.id.check1)).setChecked(antennaList.contains(eAntennaNo._1));
        ((CheckBox) findViewById(R.id.check2)).setChecked(antennaList.contains(eAntennaNo._2));
        ((CheckBox) findViewById(R.id.check3)).setChecked(antennaList.contains(eAntennaNo._3));
        ((CheckBox) findViewById(R.id.check4)).setChecked(antennaList.contains(eAntennaNo._4));
        ((CheckBox) findViewById(R.id.check5)).setChecked(antennaList.contains(eAntennaNo._5));
        ((CheckBox) findViewById(R.id.check6)).setChecked(antennaList.contains(eAntennaNo._6));
        ((CheckBox) findViewById(R.id.check7)).setChecked(antennaList.contains(eAntennaNo._7));
        ((CheckBox) findViewById(R.id.check8)).setChecked(antennaList.contains(eAntennaNo._8));
        ((CheckBox) findViewById(R.id.check9)).setChecked(antennaList.contains(eAntennaNo._9));
        ((CheckBox) findViewById(R.id.check10)).setChecked(antennaList.contains(eAntennaNo._10));
        ((CheckBox) findViewById(R.id.check11)).setChecked(antennaList.contains(eAntennaNo._11));
        ((CheckBox) findViewById(R.id.check12)).setChecked(antennaList.contains(eAntennaNo._12));
        ((CheckBox) findViewById(R.id.check13)).setChecked(antennaList.contains(eAntennaNo._13));
        ((CheckBox) findViewById(R.id.check14)).setChecked(antennaList.contains(eAntennaNo._14));
        ((CheckBox) findViewById(R.id.check15)).setChecked(antennaList.contains(eAntennaNo._15));
        ((CheckBox) findViewById(R.id.check16)).setChecked(antennaList.contains(eAntennaNo._16));
        ((CheckBox) findViewById(R.id.check17)).setChecked(antennaList.contains(eAntennaNo._17));
        ((CheckBox) findViewById(R.id.check18)).setChecked(antennaList.contains(eAntennaNo._18));
        ((CheckBox) findViewById(R.id.check19)).setChecked(antennaList.contains(eAntennaNo._19));
        ((CheckBox) findViewById(R.id.check20)).setChecked(antennaList.contains(eAntennaNo._20));
        ((CheckBox) findViewById(R.id.check21)).setChecked(antennaList.contains(eAntennaNo._21));
        ((CheckBox) findViewById(R.id.check22)).setChecked(antennaList.contains(eAntennaNo._22));
        ((CheckBox) findViewById(R.id.check23)).setChecked(antennaList.contains(eAntennaNo._23));
        ((CheckBox) findViewById(R.id.check24)).setChecked(antennaList.contains(eAntennaNo._24));
    }

    private void setAntennasCheckBoxesInt(List<String> antennaList) {
        ((CheckBox) findViewById(R.id.check1)).setChecked(antennaList.contains("1"));
        ((CheckBox) findViewById(R.id.check2)).setChecked(antennaList.contains("2"));
        ((CheckBox) findViewById(R.id.check3)).setChecked(antennaList.contains("3"));
        ((CheckBox) findViewById(R.id.check4)).setChecked(antennaList.contains("4"));
        ((CheckBox) findViewById(R.id.check5)).setChecked(antennaList.contains("5"));
        ((CheckBox) findViewById(R.id.check6)).setChecked(antennaList.contains("6"));
        ((CheckBox) findViewById(R.id.check7)).setChecked(antennaList.contains("7"));
        ((CheckBox) findViewById(R.id.check8)).setChecked(antennaList.contains("8"));
        ((CheckBox) findViewById(R.id.check9)).setChecked(antennaList.contains("9"));
        ((CheckBox) findViewById(R.id.check10)).setChecked(antennaList.contains("10"));
        ((CheckBox) findViewById(R.id.check11)).setChecked(antennaList.contains("11"));
        ((CheckBox) findViewById(R.id.check12)).setChecked(antennaList.contains("12"));
        ((CheckBox) findViewById(R.id.check13)).setChecked(antennaList.contains("13"));
        ((CheckBox) findViewById(R.id.check14)).setChecked(antennaList.contains("14"));
        ((CheckBox) findViewById(R.id.check15)).setChecked(antennaList.contains("15"));
        ((CheckBox) findViewById(R.id.check16)).setChecked(antennaList.contains("16"));
        ((CheckBox) findViewById(R.id.check17)).setChecked(antennaList.contains("17"));
        ((CheckBox) findViewById(R.id.check18)).setChecked(antennaList.contains("18"));
        ((CheckBox) findViewById(R.id.check19)).setChecked(antennaList.contains("19"));
        ((CheckBox) findViewById(R.id.check20)).setChecked(antennaList.contains("20"));
        ((CheckBox) findViewById(R.id.check21)).setChecked(antennaList.contains("21"));
        ((CheckBox) findViewById(R.id.check22)).setChecked(antennaList.contains("22"));
        ((CheckBox) findViewById(R.id.check23)).setChecked(antennaList.contains("23"));
        ((CheckBox) findViewById(R.id.check24)).setChecked(antennaList.contains("24"));
    }

        @Override
    public void notifyListener(RFIDDevice device, Tag_Model tag_model) {

    }

    @Override
    public void notifyStartAntenna(int ant) {

    }

    @Override
    public void notifyStopAntenna(int ant) {

    }

    @Override
    public void notifyStartDevice(String message) {

    }

    @Override
    public void notifyEndDevice(String message) {

    }
}