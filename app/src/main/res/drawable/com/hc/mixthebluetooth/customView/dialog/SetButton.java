package com.hc.mixthebluetooth.customView.dialog;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.hc.basiclibrary.dialog.CommonDialog;
import com.hc.basiclibrary.ioc.OnClick;
import com.hc.basiclibrary.ioc.ViewById;
import com.hc.basiclibrary.ioc.ViewUtils;
import com.hc.mixthebluetooth.R;
import com.hc.mixthebluetooth.customView.CheckBoxSample;

public class SetButton extends LinearLayout {

    private OnCollectCallback mCallback;

    private CommonDialog.Builder mBuilder;

    public SetButton(Context context) {
        super(context);
    }

    public SetButton setBuilder(CommonDialog.Builder mBuilder) {
        this.mBuilder = mBuilder;
        return this;
    }

    public void setCallback(OnCollectCallback mCallback) {
        this.mCallback = mCallback;
    }


    public void showMove(boolean isClick){

        setHideTimeLinear(isClick);
    }

    public SetButton setEditText(String name,String content){

        return this;
    }

    public SetButton setTime(int time){
        return this;
    }

    private void setHideTimeLinear(boolean isClick) {

    }

    private void cancel() {
        if (mBuilder != null)
            mBuilder.dismiss();
    }

    private void affirm() {

        if (mBuilder != null){
            mBuilder.dismiss();
        }
        if (mCallback != null ){

        }
    }



    public interface OnCollectCallback{
        void callback(String name,String content);
        void callLongClick(String name,String content,boolean isLongClick,String time);
    }

}
