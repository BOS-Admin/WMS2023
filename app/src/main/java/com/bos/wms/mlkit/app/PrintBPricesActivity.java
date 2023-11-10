package com.bos.wms.mlkit.app;

import android.app.AlertDialog;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.storage.Storage;

import Model.ClassBRepriceResponseModel;
import Remote.APIClient;
import Remote.BasicApi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class PrintBPricesActivity extends AppCompatActivity {


    public String IPAddress = "";

    int UserID = -1;


    private Button btnPopUp;
    private Button btnPrint;


    private EditText txtItem;


    private TextView txtOldLocation;


    private TextView txtNewLocation;

    private TextView lblResult;

    private String popUpMessages="";
    private String branch="";




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_prices);

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");

        UserID = General.getGeneral(this).UserID;
        branch= General.getGeneral(this).mainLocation;

        btnPopUp=findViewById(R.id.btnPopUp);
        btnPrint=findViewById(R.id.btnPrint);
        txtItem=findViewById(R.id.txtItem);
        lblResult=findViewById(R.id.lblResult);




        btnPopUp.setOnClickListener(e -> {
            showMessage("Info",popUpMessages);
        });

        lblResult.setOnClickListener(e->{
            showMessage("Info",popUpMessages);
        });



        btnPrint.setOnClickListener(e->{
            int x=0;
            try{
                String count=txtItem.getText().toString();
                x=Integer.parseInt(count);
            }catch (Exception ex){
                showMessage("Invalid Count Input","Invalid Count Input");
                return;
            }

            print(x);
        });




        txtItem.requestFocus();



lblResult.setText(branch);

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


    boolean updatingText = false;
    private String error = "";



    private void print(int count) {

        btnPrint.setEnabled(false);
        lblResult.setText("");
        reset();
        Storage mStorage = new Storage(getApplicationContext());
        String PricingLineCode = mStorage.getDataString("PricingLineCode", "None");

        try {
            //   String IPAddressWarehouseManager = mStorage.getDataString("IPAddressWarehouseManager", "192.168.10.82");
            BasicApi api = APIClient.getInstanceStatic(IPAddress, true).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.PrintV2(branch,count,PricingLineCode)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                String res="";
                                try{
                                    res=s.string();
                                }catch (Exception e){

                                }

                                if (res != null && res.equalsIgnoreCase("Success") ) {
                                    success("Success",res);

                                    Logger.Debug("API", "Print Prices : response " + s);
                                }


                                runOnUiThread(() -> {
                                    btnPrint.setEnabled(true);


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

                                runOnUiThread(() -> {
                                    btnPrint.setEnabled(true);

                                });


                            }));
        } catch (Throwable e) {
            runOnUiThread(() -> {
                btnPrint.setEnabled(true);

            });


            showMessage("Exception", e.getMessage());
            fail("Failed",e.getMessage());
            Logger.Error("API", "Print Prices  Exception: " + e.getMessage());
            //showMessage(e.getMessage());

        }
    }

    
    String fullMessage = "";
    private void reset() {
        lblResult.setText("");
        fullMessage = "";
        popUpMessages="";

    }

    private void Beep() {
        new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300);
    }
    private void success(String message, String fullMessage){
        this.fullMessage = fullMessage;
        popUpMessages=message+"\n"+fullMessage;
        runOnUiThread(() -> {

            lblResult.setText("Success");
            lblResult.setTextColor(Color.WHITE);
            lblResult.setBackgroundResource(R.drawable.rounded_corner_green);
        });
    }


    private void fail (String message, String fullMessage){

        this.fullMessage = fullMessage;
        error += "\n" + fullMessage;
        popUpMessages=message+"\n"+fullMessage;
        runOnUiThread(() -> {
            lblResult.setText("Failed");
            lblResult.setTextColor(Color.WHITE);
            lblResult.setBackgroundResource(R.drawable.rounded_corner_red);
            Beep();
        });


    }




}