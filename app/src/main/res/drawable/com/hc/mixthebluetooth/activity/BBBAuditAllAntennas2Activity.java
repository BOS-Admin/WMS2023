package com.hc.mixthebluetooth.activity;

import android.os.Bundle;

import com.hc.basiclibrary.viewBasic.BasActivity;
import com.hc.mixthebluetooth.R;

public class BBBAuditAllAntennas2Activity extends BasActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.replenishment_audit);
        //设置头部
        setTitle("BBB Audit New");
        setContext(this);

//        if(!isReplenishment){
//            layoutLocation.setVisibility(View.INVISIBLE);
//
//        }


//        new CountDownTimer(2000, 2000) {
//            public void onTick(long l) {
//
//            }
//
//            public void onFinish() {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    RefreshView();
//                }
//                start();
//            }
//        }.start();
//
//        try{
//            DiscardItemSerial = getIntent().getBooleanExtra("DiscardItemSerial", false);
//        }catch(Exception ex){
//
//        }





    }

    @Override
    public void initAll() {

    }




}