package com.hc.mixthebluetooth.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.hc.basiclibrary.ioc.ViewById;
import com.hc.basiclibrary.viewBasic.BasActivity;
import com.hc.mixthebluetooth.Logger;
import com.hc.mixthebluetooth.R;
import com.hc.mixthebluetooth.Remote.Routes.BasicApi;
import com.hc.mixthebluetooth.storage.Storage;

import java.util.ArrayList;
import java.util.HashMap;

import Model.AssignBoxLocationResponse;
import Remote.APIClient;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class UndoBoxLocationAssignmentActivity extends BasActivity  {

    @ViewById(R.id.btnPopUp)
    private Button btnPopUp;

    @ViewById(R.id.txtBox)
    private EditText txtBox;

    @ViewById(R.id.txtOldLocation)
    private TextView txtOldLocation;

    @ViewById(R.id.txtNewLocation)
    private TextView txtNewLocation;
    @ViewById(R.id.lblResult)
    private TextView lblResult;

    private Storage mStorage;
    private String IPAddress;
    private int UserID;

    private AssignBoxLocationResponse model=null;
    private String popUpMessages="";


    private int boxLocationScoreId=0;
    HashMap<String,ArrayList<String>> removedItems=new HashMap<>();

    public static String ValidBoxBarcode =null;
    public static String StationCode =null;

    private boolean removeUPcs = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_undo_box_location);
        //设置头部
        //setTitle();
        setContext(this);

        mStorage = new Storage(this);//sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
        UserID = mStorage.getDataInt("UserID", -1);
        StationCode = mStorage.getDataString("StationCode", "");

        btnPopUp.setOnClickListener(e -> {
            showMessage("Info",popUpMessages);
        });


        txtBox.setInputType(InputType.TYPE_NULL);

        txtBox.requestFocus();

        txtBox.addTextChangedListener(new BoxTextWatcher());




    }




    boolean updatingText = false;
    private String error = "";

    private void Beep() {
        new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300);
    }

    private void showMessage(String title,String msg) {

        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(msg)
                    .setPositiveButton("OK", (dialog, which) -> {
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    // .setCancelable(false)
                    .show();

        });
    }

    class BoxTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (updatingText)
                return;
            updatingText = true;

            reset();
            String box = txtBox.getText().toString();
            if (box.length() < 10 || box.length() >13) {
                Beep();
                showMessage("Error","Invalid Box Code ("+box+")");
                txtBox.setText("");
                updatingText = false;
                return;
            }

            txtBox.setEnabled(false);
            UndoBoxAssignment(box);

        }
    }

    private void UndoBoxAssignment(String BoxBarcode) {

        txtBox.setEnabled(false);
        try {

            mStorage = new Storage(this);
            //   String IPAddressWarehouseManager = mStorage.getDataString("IPAddressWarehouseManager", "192.168.10.82");
            BasicApi api = APIClient.getInstanceStatic(IPAddress, true).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.UndoBoxLocationAssignment(UserID, BoxBarcode)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {
                                    String res="";
                                    try{
                                        res=s.string();
                                    }catch (Exception e){

                                    }
                                        success("Success",res);

                                    Logger.Debug("API", "Undo Assign Location : response " + s);
                                }


                                runOnUiThread(() -> {
                                    txtBox.setEnabled(true);
                                    updatingText = true;
                                    txtBox.setText("");
                                    txtBox.requestFocus();
                                    updatingText = false;

                                });


                            }, (throwable) -> {
                                String err="";
                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    String response = ex.response().errorBody().string();
                                    if (response.isEmpty()) {
                                        response = throwable.getMessage();
                                    }
//
                                    showMessage("Error", response);
                                    err= response;
                                    Logger.Debug("API", "Error - Returned HTTP Error " + response);
                                } else {
//
                                    showMessage("Error", throwable.getMessage());
                                    err=  throwable.getMessage();
                                }
                                fail("Failed",err);
                                playError();
                                runOnUiThread(() -> {
                                    txtBox.setEnabled(true);
                                    updatingText = true;
                                    txtBox.setText("");
                                    txtBox.requestFocus();
                                    updatingText = false;

                                });


                            }));
        } catch (Throwable e) {
            runOnUiThread(() -> {
                txtBox.setEnabled(true);
                updatingText = true;
                txtBox.setText("");
                txtBox.requestFocus();
                updatingText = false;

            });


            showMessage("Exception", e.getMessage());
            fail("Failed",e.getMessage());
            playError();
            Logger.Error("API", "Assign Box  Exception: " + e.getMessage());
            //showMessage(e.getMessage());

        }
    }

    String fullMessage = "";

    private void reset() {
        lblResult.setText("");
        fullMessage = "";
        popUpMessages="";
    }

    private void fail (String message, String fullMessage){
        this.fullMessage = fullMessage;
        error += "\n" + fullMessage;
        popUpMessages=message+"\n"+fullMessage;
        runOnUiThread(() -> {
            lblResult.setText("Failed");
            lblResult.setBackgroundResource(R.drawable.rounded_corner_red);
        });
    }



    private void success(String message, String fullMessage){
        this.fullMessage = fullMessage;
        popUpMessages=message+"\n"+fullMessage;
        runOnUiThread(() -> {
            lblResult.setText("Sucess");
            lblResult.setBackgroundResource(R.drawable.rounded_corner_green);
        });
    }



    @Override
    public void initAll() {

    }


}