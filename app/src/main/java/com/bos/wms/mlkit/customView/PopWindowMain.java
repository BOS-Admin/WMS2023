package com.bos.wms.mlkit.customView;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.widget.PopupWindowCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.storage.Storage;

public class PopWindowMain {

    public static final String BLE_KEY = "BLE_KEY_POP_WINDOW";
    public static final String NAME_KEY = "NAME_KEY_POP_WINDOW";
    public static final String FILTER_KEY = "FILTER_KEY_POP_WINDOW";
    public static final String CUSTOM_KEY = "CUSTOM_KEY_POP_WINDOW";
    public static final String DATA_KEY = "DATA_KEY_POP_WINDOW";

    private AppCompatEditText txtScaleMac;
    private AppCompatEditText txtPricingLineCode;
    private AppCompatEditText txtRFIDMac;
    private AppCompatEditText txtIP;
    private Button btnSave ;
    private LinearLayout layoutFilter;


    private Storage storage;

    private DismissListener listener;

    private boolean isResetEngine;//记录下是否切换搜索方式

    public PopWindowMain(View view,Activity activity,DismissListener listener){
        storage = new Storage(activity);
        this.listener = listener;
        isResetEngine = false;
        showPopupWindow(R.layout.pop_window_main,view,activity);
    }

    private void showPopupWindow(int layout, View view, final Activity activity) {

        // 一个自定义的布局，作为显示的内容
        View contentView = LayoutInflater.from(view.getContext()).inflate(
                layout, null);
        txtPricingLineCode=contentView.findViewById(R.id.txtPricingLineCode);
        txtIP = contentView.findViewById(R.id.txtIPAddress);
        txtRFIDMac = contentView.findViewById(R.id.txtRFIDMac);
        txtScaleMac = contentView.findViewById(R.id.txtScaleMac);
        btnSave=contentView.findViewById(R.id.btnSave);
        layoutFilter = contentView.findViewById(R.id.pop_main_filter);

        final PopupWindow popupWindow = new PopupWindow(contentView,
                activity.getWindowManager().getDefaultDisplay().getWidth(), ViewGroup.LayoutParams.WRAP_CONTENT, true);

        //需要先测量，PopupWindow还未弹出时，宽高为0
        contentView.measure(makeDropDownMeasureSpec(popupWindow.getWidth()),
                makeDropDownMeasureSpec(popupWindow.getHeight()));

        //可以退出
        popupWindow.setTouchable(true);

        //设置动画
        popupWindow.setAnimationStyle(R.style.pop_window_anim);


        int offsetX =  view.getWidth() - popupWindow.getContentView().getMeasuredWidth();
        int offsetY = 0;
        PopupWindowCompat.showAsDropDown(popupWindow, view, offsetX, offsetY, Gravity.START);

        View.OnClickListener viewListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == -1){
                    return;
                }
                switch (v.getId()){

                    case R.id.btnSave:
                        storage.saveData("RFIDMac",txtRFIDMac.getText().toString());
                        storage.saveData("WeightMac",txtScaleMac.getText().toString());
                        storage.saveData("IPAddress",txtIP.getText().toString());
                        storage.saveData("PricingLineCode",txtPricingLineCode.getText().toString());

                        break;
                }
            }
        };

        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                storage.saveData("RFIDMac",txtRFIDMac.getText().toString());
                storage.saveData("WeightMac",txtScaleMac.getText().toString());
                storage.saveData("IPAddress",txtIP.getText().toString());
                storage.saveData("PricingLineCode",txtPricingLineCode.getText().toString());
                if (listener != null){
                    listener.onDismissListener(isResetEngine);
                }
            }
        });

        // 设置按钮的点击事件
        setItemClickListener(contentView,viewListener);

        // 设置好参数之后再show
        popupWindow.showAsDropDown(view);

        boolean b = storage.getData(BLE_KEY);

        txtRFIDMac.setText(storage.getDataString("RFIDMac","BTR-80021070009"));
        txtIP.setText(storage.getDataString("IPAddress","192.168.10.82"));
        txtPricingLineCode.setText(storage.getDataString("PricingLineCode","PL001"));
        txtScaleMac.setText(storage.getDataString("WeightMac","58:DA:04:A4:50:14"));

    }


    /**
     * 设置子View的ClickListener
     */
    private void setItemClickListener(View view,View.OnClickListener listener) {
        if(view instanceof ViewGroup){
            ViewGroup viewGroup = (ViewGroup) view;
            int childCount = viewGroup.getChildCount();
            for (int i=0;i<childCount;i++){
                //不断的递归给里面所有的View设置OnClickListener
                View childView = viewGroup.getChildAt(i);
                setItemClickListener(childView,listener);
            }
        }else{
            view.setOnClickListener(listener);
        }
    }


    @SuppressWarnings("ResourceType")
    private static int makeDropDownMeasureSpec(int measureSpec) {
        int mode;
        if (measureSpec == ViewGroup.LayoutParams.WRAP_CONTENT) {
            mode = View.MeasureSpec.UNSPECIFIED;
        } else {
            mode = View.MeasureSpec.EXACTLY;
        }
        return View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(measureSpec), mode);
    }

    public interface DismissListener{
        void onDismissListener(boolean resetEngine);
    }

}
