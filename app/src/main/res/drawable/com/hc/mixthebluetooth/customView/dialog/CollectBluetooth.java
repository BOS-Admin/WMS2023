package com.hc.mixthebluetooth.customView.dialog;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.hc.basiclibrary.dialog.CommonDialog;
import com.hc.basiclibrary.ioc.OnClick;
import com.hc.basiclibrary.ioc.ViewById;
import com.hc.basiclibrary.ioc.ViewUtils;
import com.hc.bluetoothlibrary.DeviceModule;
import com.hc.mixthebluetooth.R;

public class CollectBluetooth extends LinearLayout {



    private DeviceModule mDeviceModule;

    private OnCollectCallback mCallback;

    private CommonDialog.Builder mBuilder;

    public CollectBluetooth(Context context) {
        this(context,null);
    }

    public CollectBluetooth(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CollectBluetooth(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
         ViewUtils.inject(this);

    }

    public CollectBluetooth setBuilder(CommonDialog.Builder mBuilder) {
        this.mBuilder = mBuilder;
        return this;
    }

    public CollectBluetooth setDevice(DeviceModule device){
        this.mDeviceModule = device;

        return this;
    }

    public void setCallback(OnCollectCallback mCallback) {
        this.mCallback = mCallback;
    }
 

    private void affirmState2(View view) {
        if (mDeviceModule != null){
            mDeviceModule.setCollectModule(view.getContext(),null);
        }
        if (mBuilder != null){
            mBuilder.dismiss();
        }
        if (mCallback != null){
            mCallback.callback();
        }
    }

    private void cancel() {
        if (mBuilder != null)
            mBuilder.dismiss();
    }

    private void affirm(View view) {
        if (mDeviceModule != null){
            mDeviceModule.setCollectModule(view.getContext(),mDeviceModule.getOriginalName(view.getContext()));
        }
        if (mBuilder != null){
            mBuilder.dismiss();
        }
        if (mCallback != null){
            mCallback.callback();
        }
    }


    public interface OnCollectCallback{
        void callback();
    }

}
