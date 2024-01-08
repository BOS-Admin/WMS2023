package com.hc.mixthebluetooth.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.hc.mixthebluetooth.R;

import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDDevice;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDDevicesManager;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDListener;
import com.hc.mixthebluetooth.RfidDeviceManager1.RfidDeviceManager.RFIDOutput;
import com.rfidread.Enumeration.eReadType;
import com.rfidread.Models.Tag_Model;

import java.util.ArrayList;
import java.util.HashMap;


public class AuditAntennaActivity extends AppCompatActivity implements RFIDListener {

    ArrayList<String> listItems = new ArrayList<>();
    HashMap<String, String> mapItems = new HashMap<String, String>();
    ListView listView;
    ArrayAdapter<String> arr;
    int count = 0;
    private Object listLock = new Object();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audit_antenna);

        RFIDDevicesManager.setOutput(new RFIDOutput(this));


        listView = findViewById(R.id.listView);
        arr = new ArrayAdapter<String>(
                AuditAntennaActivity.this,
                R.layout.support_simple_spinner_dropdown_item,
                listItems);
        listView.setAdapter(arr);

        Button btnRead = findViewById(R.id.btnRead);
        btnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Started Read", Toast.LENGTH_SHORT).show();
                EditText txtTime = (EditText) findViewById(R.id.txtTime);
                new Thread(() -> {
                    try {
                        RFIDDevicesManager.readEPCSingleAntenna(eReadType.Inventory, Integer.parseInt(txtTime.getText().toString()));
                    } catch (Exception e) {
                        showMessage("Error: " + e.getMessage());
                    }
                    //showMessage("Stopped Reading ");
                }).start();

            }
        });

        Button btnStop = findViewById(R.id.btnStop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RFIDDevicesManager.stopSingleAntenna1();
                Toast.makeText(getApplicationContext(), "Stopped Read", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnClear = findViewById(R.id.btnClear);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                synchronized (listLock) {
                    mapItems.clear();
                    listItems.clear();
                    arr.notifyDataSetChanged();

                }

            }
        });

    }

    @Override
    public void notifyListener(RFIDDevice device, Tag_Model tag_model) {
        synchronized (listLock) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //if (!mapItems.containsKey(tag_model._EPC)) {
                      //  mapItems.put(tag_model._EPC, "");
                        listItems.add(tag_model._EPC + " ( " + tag_model._ANT_NUM + " ) ");
                        arr.notifyDataSetChanged();
                    //}
                    count++;

                }
            });


        }



    }


    @Override
    public void onDestroy() {
        RFIDDevicesManager.stopSingleAntenna1();
        super.onDestroy();
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


    public void showMessage(String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();

            }
        });


        }

    }