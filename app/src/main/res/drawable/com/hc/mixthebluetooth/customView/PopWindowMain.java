package com.hc.mixthebluetooth.customView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.widget.PopupWindowCompat;

import com.hc.mixthebluetooth.Model.RfidType;
import com.hc.mixthebluetooth.R;
import com.hc.mixthebluetooth.Remote.UserPermissions.UserPermissions;
import com.hc.mixthebluetooth.storage.Storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import Remote.APIClient;
import com.hc.mixthebluetooth.Remote.Routes.BasicApi;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class PopWindowMain {

    public static final String BLE_KEY = "BLE_KEY_POP_WINDOW";
    public static final String NAME_KEY = "NAME_KEY_POP_WINDOW";
    public static final String FILTER_KEY = "FILTER_KEY_POP_WINDOW";
    public static final String CUSTOM_KEY = "CUSTOM_KEY_POP_WINDOW";
    public static final String DATA_KEY = "DATA_KEY_POP_WINDOW";
    public List<RfidType> rfidTypes=null;

    private AppCompatEditText txtLotBond;
    private AppCompatEditText txtStationCode;
    private AppCompatEditText txtScaleMac;
    private AppCompatEditText txtRFIDMac;
    private AppCompatEditText txtRFIDMac2;
    private AppCompatEditText txtRFIDMac3;
    private AppCompatEditText txtRFIDMac4;
    private AppCompatEditText txtRFIDMac5;
    private AppCompatEditText txtRFIDMac6;
    private AppCompatEditText txtReads;
    private AppCompatEditText txtIP;
    private AppCompatEditText txtIP2;

    private Button btnSetRFIDGroup1 ;
    private Button btnSetRFIDGroup2 ;
    private Button btnSetRFIDGroup3 ;
    private Button btnSetRFIDGroup4 ;
    private Button btnSetRFIDGroup5 ;
    private Button btnSetRFIDGroup6 ;
    private Button btnSave ;

    private TextView txtRFIDGroup1 ;
    private TextView txtRFIDGroup2 ;
    private TextView txtRFIDGroup3 ;
    private TextView txtRFIDGroup4 ;
    private TextView txtRFIDGroup5 ;
    private TextView txtRFIDGroup6 ;


    private TextView txtRFIDGrp1ReadTime1 ;
    private TextView txtRFIDGrp1ReadTime2 ;
    private TextView txtRFIDGrp1ReadTime3 ;
    private TextView txtRFIDGrp1ReadTime4 ;
    private TextView txtRFIDGrp1ReadTime5 ;
    private TextView txtRFIDGrp1ReadTime6 ;



    private SeekBar brRFIDReadTime1;
    private SeekBar brRFIDReadTime2;
    private SeekBar brRFIDReadTime3;
    private SeekBar brRFIDReadTime4;
    private SeekBar brRFIDReadTime5;
    private SeekBar brRFIDReadTime6;

    private Spinner spinItemSerial;
    private Spinner spinBin;
    private TextView lblError ;

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
    boolean[] selectedRFIDDevices;
    ArrayList<Integer> RFIDDeviceList = new ArrayList<>();

    boolean[] selectedRFIDDevices1;
    ArrayList<Integer> RFIDDeviceList1 = new ArrayList<>();
    boolean[] selectedRFIDDevices2;
    ArrayList<Integer> RFIDDeviceList2 = new ArrayList<>();
    boolean[] selectedRFIDDevices3;
    ArrayList<Integer> RFIDDeviceList3 = new ArrayList<>();
    boolean[] selectedRFIDDevices4;
    ArrayList<Integer> RFIDDeviceList4 = new ArrayList<>();
    boolean[] selectedRFIDDevices5;
    ArrayList<Integer> RFIDDeviceList5 = new ArrayList<>();
    boolean[] selectedRFIDDevices6;
    ArrayList<Integer> RFIDDeviceList6 = new ArrayList<>();

    String[] RFIDDeviceArray = {"1", "2", "3", "4", "5", "6"};


    private void getRfidTypes(View view) {
        try

        {


            String IPAddress="192.168.50.20:8000";
            try{
                IPAddress= txtIP.getText().toString();
            }catch (Exception e){

            }
            BasicApi api = APIClient.getNewInstanceStatic(IPAddress).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.GetRfidTypes()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {
                                    try {
                                        rfidTypes=s;
                                        String ItemSerial=storage.getDataString("ItemSerialRfidPrefix","");
                                        String Bin=storage.getDataString("BinRfidPrefix","");

                                        ArrayList<String> itemSerials=new ArrayList<>();
                                        ArrayList<String> bins=new ArrayList<>();
                                        int i=0,b=0,selectedItemPos=-1,selectedBinPos=-1;
                                        for (RfidType t:rfidTypes ){
                                            if(t.getTypeId()==1){
                                                itemSerials.add(t.getHeader());
                                                if(Objects.equals(t.getHeader(), ItemSerial) && selectedItemPos<0)
                                                    selectedItemPos=i;
                                                i++;
                                            }

                                            if(t.getTypeId()==2){
                                                bins.add(t.getHeader());
                                                if(Objects.equals(t.getHeader(), Bin) && selectedBinPos<0)
                                                    selectedBinPos=b;
                                                b++;
                                            }


                                        }
                                        ArrayAdapter adItemSerial
                                                = new ArrayAdapter(
                                                view.getContext(),
                                                android.R.layout.simple_spinner_item,
                                                itemSerials);
                                        adItemSerial.setDropDownViewResource(
                                                android.R.layout
                                                        .simple_spinner_dropdown_item);
                                        spinItemSerial.setAdapter(adItemSerial);

                                        ArrayAdapter adBins
                                                = new ArrayAdapter(
                                                view.getContext(),
                                                android.R.layout.simple_spinner_item,
                                                bins);
                                        adBins.setDropDownViewResource(
                                                android.R.layout
                                                        .simple_spinner_dropdown_item);
                                        spinBin.setAdapter(adBins);
                                        if(itemSerials.size()>0)
                                            spinItemSerial.setSelection(selectedItemPos>=0?selectedItemPos:0);
                                        if(bins.size()>0)
                                            spinBin.setSelection(selectedBinPos>=0?selectedBinPos:0);

                                    } catch (Exception e) {
                                        lblError.setText("Error:"+e.getMessage());
                                    }

                                }
                            }, (throwable) -> {
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    String response="(API Error)";
                                    try{
                                        response = ex.response().errorBody().string();
                                    }
                                    catch(Exception e){

                                    }
                                    final String response1=response;
                                    new Handler().post(() -> {
                                        lblError.setText("Error:"+response1+" (API Http Error)");
                                    });

                                }else {
                                    new Handler().post(() -> {
                                        lblError.setText("Error:"+throwable.getMessage()+"(API Error)");
                                    });
                                }
                            }));
        } catch (Throwable e) {
            new Handler().post(() -> {
                lblError.setText("Exception:"+e.getMessage());
            });
        }
    }
    private void showPopupWindow(int layout, View view, final Activity activity) {

        // 一个自定义的布局，作为显示的内容
        View contentView = LayoutInflater.from(view.getContext()).inflate(
                layout, null);

        txtIP = contentView.findViewById(R.id.txtIPAddress);
        txtIP2 = contentView.findViewById(R.id.txtIPAddress2);
        txtRFIDMac = contentView.findViewById(R.id.txtRFIDMac);
        txtRFIDMac2 = contentView.findViewById(R.id.txtRFIDMac2);
        txtRFIDMac3 = contentView.findViewById(R.id.txtRFIDMac3);
        txtRFIDMac4 = contentView.findViewById(R.id.txtRFIDMac4);
        txtRFIDMac5 = contentView.findViewById(R.id.txtRFIDMac5);
        txtRFIDMac6 = contentView.findViewById(R.id.txtRFIDMac6);
        txtReads = contentView.findViewById(R.id.txtReads);
        txtScaleMac = contentView.findViewById(R.id.txtScaleMac);
        txtLotBond = contentView.findViewById(R.id.txtLotBond);
        txtStationCode = contentView.findViewById(R.id.txtStationCode);

        btnSetRFIDGroup1=contentView.findViewById(R.id.btnSetRFIDGroup1);
        btnSetRFIDGroup2=contentView.findViewById(R.id.btnSetRFIDGroup2);
        btnSetRFIDGroup3=contentView.findViewById(R.id.btnSetRFIDGroup3);
        btnSetRFIDGroup4=contentView.findViewById(R.id.btnSetRFIDGroup4);
        btnSetRFIDGroup5=contentView.findViewById(R.id.btnSetRFIDGroup5);
        btnSetRFIDGroup6=contentView.findViewById(R.id.btnSetRFIDGroup6);

        txtRFIDGroup1=contentView.findViewById(R.id.txtRFIDGroup1);
        txtRFIDGroup2=contentView.findViewById(R.id.txtRFIDGroup2);
        txtRFIDGroup3=contentView.findViewById(R.id.txtRFIDGroup3);
        txtRFIDGroup4=contentView.findViewById(R.id.txtRFIDGroup4);
        txtRFIDGroup5=contentView.findViewById(R.id.txtRFIDGroup5);
        txtRFIDGroup6=contentView.findViewById(R.id.txtRFIDGroup6);

        txtRFIDGrp1ReadTime1=contentView.findViewById(R.id.txtRFIDGrp1ReadTime1);
        txtRFIDGrp1ReadTime2=contentView.findViewById(R.id.txtRFIDGrp1ReadTime2);
        txtRFIDGrp1ReadTime3=contentView.findViewById(R.id.txtRFIDGrp1ReadTime3);
        txtRFIDGrp1ReadTime4=contentView.findViewById(R.id.txtRFIDGrp1ReadTime4);
        txtRFIDGrp1ReadTime5=contentView.findViewById(R.id.txtRFIDGrp1ReadTime5);
        txtRFIDGrp1ReadTime6=contentView.findViewById(R.id.txtRFIDGrp1ReadTime6);

        brRFIDReadTime1=contentView.findViewById(R.id.brRFIDReadTime1);
        brRFIDReadTime2=contentView.findViewById(R.id.brRFIDReadTime2);
        brRFIDReadTime3=contentView.findViewById(R.id.brRFIDReadTime3);
        brRFIDReadTime4=contentView.findViewById(R.id.brRFIDReadTime4);
        brRFIDReadTime5=contentView.findViewById(R.id.brRFIDReadTime5);
        brRFIDReadTime6=contentView.findViewById(R.id.brRFIDReadTime6);

        spinItemSerial=contentView.findViewById(R.id.spinItemSerial);
        spinBin=contentView.findViewById(R.id.spinBin);
        lblError=contentView.findViewById(R.id.lblError);

        SeekBar.OnSeekBarChangeListener SeekBarListener=new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                switch (seekBar.getId()) {
                    case R.id.brRFIDReadTime1:
                        txtRFIDGrp1ReadTime1.setText(String.valueOf(i));
                        break;
                    case R.id.brRFIDReadTime2:
                        txtRFIDGrp1ReadTime2.setText(String.valueOf(i));
                        break;
                    case R.id.brRFIDReadTime3:
                        txtRFIDGrp1ReadTime3.setText(String.valueOf(i));
                        break;
                    case R.id.brRFIDReadTime4:
                        txtRFIDGrp1ReadTime4.setText(String.valueOf(i));
                        break;
                    case R.id.brRFIDReadTime5:
                        txtRFIDGrp1ReadTime5.setText(String.valueOf(i));
                        break;
                    case R.id.brRFIDReadTime6:
                        txtRFIDGrp1ReadTime6.setText(String.valueOf(i));
                        break;
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        brRFIDReadTime1.setOnSeekBarChangeListener(SeekBarListener);
        brRFIDReadTime2.setOnSeekBarChangeListener(SeekBarListener);
        brRFIDReadTime3.setOnSeekBarChangeListener(SeekBarListener);
        brRFIDReadTime4.setOnSeekBarChangeListener(SeekBarListener);
        brRFIDReadTime5.setOnSeekBarChangeListener(SeekBarListener);
        brRFIDReadTime6.setOnSeekBarChangeListener(SeekBarListener);

        selectedRFIDDevices = new boolean[RFIDDeviceArray.length];

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

        View.OnClickListener viewListener = v -> {
            if (v.getId() == -1){
                return;
            }
            switch (v.getId()){

                case R.id.btnSave:
                    storage.saveData("RFIDMac",txtRFIDMac.getText().toString());
                    storage.saveData("RFIDMac2",txtRFIDMac2.getText().toString());
                    storage.saveData("RFIDMac3",txtRFIDMac3.getText().toString());
                    storage.saveData("RFIDMac4",txtRFIDMac4.getText().toString());
                    storage.saveData("RFIDMac5",txtRFIDMac5.getText().toString());
                    storage.saveData("RFIDMac6",txtRFIDMac6.getText().toString());
                    storage.saveData("Reads",txtReads.getText().toString());
                    storage.saveData("WeightMac",txtScaleMac.getText().toString());
                    storage.saveData("LotBondStation",txtLotBond.getText().toString());
                    storage.saveData("StationCode",txtStationCode.getText().toString());
                    storage.saveData("IPAddress",txtIP.getText().toString());
                    storage.saveData("IPAddressWarehouseManager",txtIP2.getText().toString());

                    storage.saveData("RFIDGroup1",txtRFIDGroup1.getText().toString());
                    storage.saveData("RFIDGroup2",txtRFIDGroup2.getText().toString());
                    storage.saveData("RFIDGroup3",txtRFIDGroup3.getText().toString());
                    storage.saveData("RFIDGroup4",txtRFIDGroup4.getText().toString());
                    storage.saveData("RFIDGroup5",txtRFIDGroup5.getText().toString());
                    storage.saveData("RFIDGroup6",txtRFIDGroup6.getText().toString());

                    storage.saveData("RFIDGrp1ReadTime1",txtRFIDGrp1ReadTime1.getText().toString());
                    storage.saveData("RFIDGrp1ReadTime2",txtRFIDGrp1ReadTime2.getText().toString());
                    storage.saveData("RFIDGrp1ReadTime3",txtRFIDGrp1ReadTime3.getText().toString());
                    storage.saveData("RFIDGrp1ReadTime4",txtRFIDGrp1ReadTime4.getText().toString());
                    storage.saveData("RFIDGrp1ReadTime5",txtRFIDGrp1ReadTime5.getText().toString());
                    storage.saveData("RFIDGrp1ReadTime6",txtRFIDGrp1ReadTime6.getText().toString());


                    if(rfidTypes!=null){
                        //Toast.makeText(view.getContext(),"selected: "+selectedItemSerial,Toast.LENGTH_SHORT).show();
                        storage.saveData("ItemSerialRfidPrefix",spinItemSerial.getSelectedItem().toString());
                        storage.saveData("BinRfidPrefix",spinBin.getSelectedItem().toString());
                       // Toast.makeText(view.getContext(),"selected: "+selectedBin,Toast.LENGTH_SHORT).show();
                    }

                    break;

                case R.id.btnSetRFIDGroup1:
                    SetRfidCheckList(1);
                    break;
                case R.id.btnSetRFIDGroup2:
                    SetRfidCheckList(2);
                    break;
                case R.id.btnSetRFIDGroup3:
                    SetRfidCheckList(3);
                    break;
                case R.id.btnSetRFIDGroup4:
                    SetRfidCheckList(4);
                    break;
                case R.id.btnSetRFIDGroup5:
                    SetRfidCheckList(5);
                    break;
                case R.id.btnSetRFIDGroup6:
                    SetRfidCheckList(6);
                    break;
            }
        };

        popupWindow.setOnDismissListener(() -> {
            storage.saveData("RFIDMac",txtRFIDMac.getText().toString());
            storage.saveData("RFIDMac2",txtRFIDMac2.getText().toString());
            storage.saveData("RFIDMac3",txtRFIDMac3.getText().toString());
            storage.saveData("RFIDMac4",txtRFIDMac4.getText().toString());
            storage.saveData("RFIDMac5",txtRFIDMac5.getText().toString());
            storage.saveData("RFIDMac6",txtRFIDMac6.getText().toString());
            storage.saveData("Reads",txtReads.getText().toString());
            storage.saveData("WeightMac",txtScaleMac.getText().toString());
            storage.saveData("LotBondStation",txtLotBond.getText().toString());
            storage.saveData("StationCode",txtStationCode.getText().toString());
            storage.saveData("IPAddress",txtIP.getText().toString());
            storage.saveData("IPAddressWarehouseManager",txtIP2.getText().toString());

            storage.saveData("RFIDGroup1",txtRFIDGroup1.getText().toString());
            storage.saveData("RFIDGroup2",txtRFIDGroup2.getText().toString());
            storage.saveData("RFIDGroup3",txtRFIDGroup3.getText().toString());
            storage.saveData("RFIDGroup4",txtRFIDGroup4.getText().toString());
            storage.saveData("RFIDGroup5",txtRFIDGroup5.getText().toString());
            storage.saveData("RFIDGroup6",txtRFIDGroup6.getText().toString());


            storage.saveData("RFIDGrp1ReadTime1",txtRFIDGrp1ReadTime1.getText().toString());
            storage.saveData("RFIDGrp1ReadTime2",txtRFIDGrp1ReadTime2.getText().toString());
            storage.saveData("RFIDGrp1ReadTime3",txtRFIDGrp1ReadTime3.getText().toString());
            storage.saveData("RFIDGrp1ReadTime4",txtRFIDGrp1ReadTime4.getText().toString());
            storage.saveData("RFIDGrp1ReadTime5",txtRFIDGrp1ReadTime5.getText().toString());
            storage.saveData("RFIDGrp1ReadTime6",txtRFIDGrp1ReadTime6.getText().toString());
            if (listener != null){
                listener.onDismissListener(isResetEngine);
            }
        });

        // 设置按钮的点击事件
        setItemClickListener(contentView,viewListener);

        // 设置好参数之后再show
        popupWindow.showAsDropDown(view);

        boolean b = storage.getData(BLE_KEY);

        txtRFIDMac.setText(storage.getDataString("RFIDMac","BTR-80021070009"));
        txtRFIDMac2.setText(storage.getDataString("RFIDMac2",""));
        txtRFIDMac3.setText(storage.getDataString("RFIDMac3",""));
        txtRFIDMac4.setText(storage.getDataString("RFIDMac4",""));
        txtRFIDMac5.setText(storage.getDataString("RFIDMac5",""));
        txtRFIDMac6.setText(storage.getDataString("RFIDMac6",""));
        txtReads.setText(storage.getDataString("Reads","1"));


        txtIP.setText(storage.getDataString("IPAddress","192.168.10.82"));
        txtIP2.setText(storage.getDataString("IPAddressWarehouseManager","192.168.10.82"));
        txtScaleMac.setText(storage.getDataString("WeightMac","58:DA:04:A4:50:14"));

        txtLotBond.setText(storage.getDataString("LotBondStation","58:DA:04"));
        txtStationCode.setText(storage.getDataString("StationCode",""));

        txtRFIDGroup1.setText(storage.getDataString("RFIDGroup1","1"));
        txtRFIDGroup2.setText(storage.getDataString("RFIDGroup2","2"));
        txtRFIDGroup3.setText(storage.getDataString("RFIDGroup3","3"));
        txtRFIDGroup4.setText(storage.getDataString("RFIDGroup4","4"));
        txtRFIDGroup5.setText(storage.getDataString("RFIDGroup5","5"));
        txtRFIDGroup6.setText(storage.getDataString("RFIDGroup6","6"));

        txtRFIDGrp1ReadTime1.setText(storage.getDataString("RFIDGrp1ReadTime1","300"));
        txtRFIDGrp1ReadTime2.setText(storage.getDataString("RFIDGrp1ReadTime2","300"));
        txtRFIDGrp1ReadTime3.setText(storage.getDataString("RFIDGrp1ReadTime3","300"));
        txtRFIDGrp1ReadTime4.setText(storage.getDataString("RFIDGrp1ReadTime4","300"));
        txtRFIDGrp1ReadTime5.setText(storage.getDataString("RFIDGrp1ReadTime5","300"));
        txtRFIDGrp1ReadTime6.setText(storage.getDataString("RFIDGrp1ReadTime6","300"));

        brRFIDReadTime1.setProgress(Integer.valueOf(txtRFIDGrp1ReadTime1.getText().toString()));
        brRFIDReadTime2.setProgress(Integer.valueOf(txtRFIDGrp1ReadTime2.getText().toString()));
        brRFIDReadTime3.setProgress(Integer.valueOf(txtRFIDGrp1ReadTime3.getText().toString()));
        brRFIDReadTime4.setProgress(Integer.valueOf(txtRFIDGrp1ReadTime4.getText().toString()));
        brRFIDReadTime5.setProgress(Integer.valueOf(txtRFIDGrp1ReadTime5.getText().toString()));
        brRFIDReadTime6.setProgress(Integer.valueOf(txtRFIDGrp1ReadTime6.getText().toString()));

        if(UserPermissions.HasPermission("RFID.ADMIN.SETTINGS")){
            spinItemSerial.setEnabled(true);
            spinBin.setEnabled(true);
        }
        else{
            spinItemSerial.setEnabled(false);
            spinBin.setEnabled(false);
        }

        getRfidTypes(view);

    }


    /**
     * 设置子View的ClickListener
     */

    private void SetRfidCheckList(int GroupID){
    String RFIDText="";
        switch (GroupID){
            case 1:RFIDText=txtRFIDGroup1.getText().toString();
                RFIDDeviceList=RFIDDeviceList1;
                break;
            case 2:RFIDText=txtRFIDGroup2.getText().toString();
                RFIDDeviceList=RFIDDeviceList2;
                break;
            case 3:RFIDText=txtRFIDGroup3.getText().toString();
                RFIDDeviceList=RFIDDeviceList3;
                break;
            case 4:RFIDText=txtRFIDGroup4.getText().toString();
                RFIDDeviceList=RFIDDeviceList4;
                break;
            case 5:RFIDText=txtRFIDGroup5.getText().toString();
                RFIDDeviceList=RFIDDeviceList5;
                break;
            case 6:RFIDText=txtRFIDGroup6.getText().toString();
                RFIDDeviceList=RFIDDeviceList6;
                break;
        }
        selectedRFIDDevices=new boolean[RFIDDeviceArray.length];
        RFIDDeviceList.clear();
        for (int j = 0; j < RFIDDeviceArray.length; j++) {
            if(RFIDText.contains(RFIDDeviceArray[j])){
                RFIDDeviceList.add(j);
                selectedRFIDDevices[j]=true;
            }
            else
                selectedRFIDDevices[j]=false;
        }
            // Initialize alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(btnSetRFIDGroup1.getContext());
            // set title
            builder.setTitle("Select Devices");

            // set dialog non cancelable
            builder.setCancelable(false);

            builder.setMultiChoiceItems(RFIDDeviceArray, selectedRFIDDevices, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                    // check condition
                    if (b) {
                        // when checkbox selected
                        // Add position  in lang list
                        if(!RFIDDeviceList.contains(i))
                            RFIDDeviceList.add(i);
                        // Sort array list
                        Collections.sort(RFIDDeviceList);
                    } else {
                        // when checkbox unselected
                        // Remove position from langList
                        RFIDDeviceList.remove(Integer.valueOf(i));
                    }
                }
            });

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Initialize string builder
                    StringBuilder stringBuilder = new StringBuilder();
                    // use for loop
                    for (int j = 0; j < RFIDDeviceList.size(); j++) {
                        // concat array value
                        stringBuilder.append(RFIDDeviceArray[RFIDDeviceList.get(j)]);
                        // check condition
                        if (j != RFIDDeviceList.size() - 1) {
                            // When j value  not equal
                            // to lang list size - 1
                            // add comma
                            stringBuilder.append(",");
                        }
                    }
                    // set text on textView

                    String strRfidDevices = stringBuilder.toString().replace("RFID", "");

                    switch (GroupID){
                        case 1:txtRFIDGroup1.setText(strRfidDevices);
                            break;
                        case 2:txtRFIDGroup2.setText(strRfidDevices);
                            break;
                        case 3:txtRFIDGroup3.setText(strRfidDevices);
                            break;
                        case 4:txtRFIDGroup4.setText(strRfidDevices);
                            break;
                        case 5:txtRFIDGroup5.setText(strRfidDevices);
                            break;
                        case 6:txtRFIDGroup6.setText(strRfidDevices);
                            break;
                    }
                    switch (GroupID){
                        case 1:selectedRFIDDevices1=selectedRFIDDevices;
                            RFIDDeviceList1=RFIDDeviceList;
                            break;
                        case 2:selectedRFIDDevices2=selectedRFIDDevices;
                            RFIDDeviceList2=RFIDDeviceList;
                            break;
                        case 3:selectedRFIDDevices3=selectedRFIDDevices;
                            RFIDDeviceList3=RFIDDeviceList;
                            break;
                        case 4:selectedRFIDDevices4=selectedRFIDDevices;
                            RFIDDeviceList4=RFIDDeviceList;
                            break;
                        case 5:selectedRFIDDevices5=selectedRFIDDevices;
                            RFIDDeviceList5=RFIDDeviceList;
                            break;
                        case 6:selectedRFIDDevices6=selectedRFIDDevices;
                            RFIDDeviceList6=RFIDDeviceList;
                            break;
                    }
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // dismiss dialog
                    dialogInterface.dismiss();
                }
            });
            builder.setNeutralButton("Clear All", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // use for loop
                    for (int j = 0; j < selectedRFIDDevices.length; j++) {
                        // remove all selection
                        selectedRFIDDevices[j] = false;
                    }
                        // clear language list
                        RFIDDeviceList.clear();
                        // clear text view value
                        switch (GroupID){
                            case 1:txtRFIDGroup1.setText("");
                                RFIDDeviceList1.clear();
                                break;
                            case 2:txtRFIDGroup2.setText("");
                                RFIDDeviceList2.clear();
                                break;
                            case 3:txtRFIDGroup3.setText("");
                                RFIDDeviceList3.clear();
                                break;
                            case 4:txtRFIDGroup4.setText("");
                                RFIDDeviceList4.clear();
                                break;
                            case 5:txtRFIDGroup5.setText("");
                                RFIDDeviceList5.clear();
                                break;
                            case 6:txtRFIDGroup6.setText("");
                                RFIDDeviceList6.clear();
                                break;
                    }
                }
            });
            // show dialog
            builder.show();
    }
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
