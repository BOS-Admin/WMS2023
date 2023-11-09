package com.hc.mixthebluetooth.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.hc.mixthebluetooth.R;

public class AuditMenuActivity extends AppCompatActivity {
    public static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audit_menu);
        context=this;


        Button btnDevices =findViewById(R.id.btnDevice);
        btnDevices.setOnClickListener(e->{

            Intent myIntent = new Intent(AuditMenuActivity.this, DeviceActivity.class);
            AuditMenuActivity.this.startActivity(myIntent);

        });



        Button btnTestRead =findViewById(R.id.btnTestRead);
        btnTestRead.setOnClickListener(e->{

            Intent myIntent = new Intent(AuditMenuActivity.this, AuditAntennaActivity.class);
            AuditMenuActivity.this.startActivity(myIntent);

        });

        Button btnAudit =findViewById(R.id.btnAudit);
        btnAudit.setOnClickListener(e->{
            Intent myIntent = new Intent(AuditMenuActivity.this, BBBAuditAntennaActivity.class);
            AuditMenuActivity.this.startActivity(myIntent);

        });


    }
}