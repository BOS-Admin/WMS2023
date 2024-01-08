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
import com.bos.wms.mlkit.utils.UPCAHelper;


import Model.ClassBRepriceResponseModel;
import Remote.APIClient;
import Remote.BasicApi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class RepriceActivity extends AppCompatActivity {


    public String IPAddress = "";

    int UserID = -1;


    private Button btnPopUp;


    private EditText txtItem;


    private TextView txtOldLocation;


    private TextView txtNewLocation;

    private TextView lblResult;

    private String popUpMessages="";
    private String branch="";

    private TextView textPrevLBPPrice;
    private TextView textPrevLetter;
    private TextView textPrevUSDPrice;

    private TextView textNewLBPPrice;
    private TextView textNewLetter;
    private TextView textNewUSDPrice;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reprice);

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");

        UserID = General.getGeneral(this).UserID;
        branch= General.getGeneral(this).mainLocation;

        btnPopUp=findViewById(R.id.btnPopUp);
        txtItem=findViewById(R.id.txtItem);
        lblResult=findViewById(R.id.lblResult);

        textPrevLBPPrice=findViewById(R.id.textPrevLBPPrice);
        textPrevLetter=findViewById(R.id.textPrevLetter);
        textPrevUSDPrice=findViewById(R.id.textPrevUSDPrice);

        textNewLBPPrice=findViewById(R.id.textNewLBPPrice);
        textNewLetter=findViewById(R.id.textNewLetter);
        textNewUSDPrice=findViewById(R.id.textNewUSDPrice);


        btnPopUp.setOnClickListener(e -> {
            showMessage("Info",popUpMessages);
        });

        lblResult.setOnClickListener(e->{
            showMessage("Info",popUpMessages);
        });

        txtItem.setInputType(InputType.TYPE_NULL);

        txtItem.requestFocus();

        txtItem.addTextChangedListener(new ItemTextWatcher());

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



    class ItemTextWatcher implements TextWatcher {

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
            String item = txtItem.getText().toString();
            if (item.length() < 10 || item.length() >13) {
                Beep();
                showMessage("Error","Invalid Item Scanned ("+item+")");
                txtItem.setText("");
                updatingText = false;
                return;
            }

            UPCAHelper helper =new UPCAHelper();
            if(helper.isValidUPCA(item))
                item = helper.convertToIS(item);

            if(isNumeric(item)){
                item="IN"+item;
            }





            txtItem.setEnabled(false);
            Reprice(item);

        }
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch(Exception e){
            return false;
        }
    }





    private void Reprice(String item
    ) {

        txtItem.setEnabled(false);
        try {
            //   String IPAddressWarehouseManager = mStorage.getDataString("IPAddressWarehouseManager", "192.168.10.82");
            BasicApi api = APIClient.getInstanceStatic(IPAddress, true).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.Reprice(UserID,item,branch)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {
                                    success("Success",s);

                                    Logger.Debug("API", "Undo Assign Location : response " + s);
                                }


                                runOnUiThread(() -> {
                                    txtItem.setEnabled(true);
                                    updatingText = true;
                                    txtItem.setText("");
                                    txtItem.requestFocus();
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

                                runOnUiThread(() -> {
                                    txtItem.setEnabled(true);
                                    updatingText = true;
                                    txtItem.setText("");
                                    txtItem.requestFocus();
                                    updatingText = false;

                                });


                            }));
        } catch (Throwable e) {
            runOnUiThread(() -> {
                txtItem.setEnabled(true);
                updatingText = true;
                txtItem.setText("");
                txtItem.requestFocus();
                updatingText = false;

            });


            showMessage("Exception", e.getMessage());
            fail("Failed",e.getMessage());
            Logger.Error("API", "Assign Box  Exception: " + e.getMessage());
            //showMessage(e.getMessage());

        }
    }

    
    String fullMessage = "";
    private void reset() {
        lblResult.setText("");
        fullMessage = "";
        popUpMessages="";

        textPrevLBPPrice.setText("");
        textPrevLetter.setText("");
        textPrevUSDPrice.setText("");


        textNewLBPPrice.setText("");
        textNewLetter.setText("");
        textNewUSDPrice.setText("");

    }

    private void Beep() {
        new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300);
    }
    private void success(String message, ClassBRepriceResponseModel res){
        this.fullMessage = fullMessage;
        popUpMessages=message+"\n"+fullMessage;
        runOnUiThread(() -> {
            textPrevLBPPrice.setText(""+res.getPrevSalesPrice());
            textPrevLetter.setText(res.getPrevLetter());
            textPrevUSDPrice.setText(res.getPrevUSDPrice());


            textNewLBPPrice.setText(""+res.getNewSalesPrice());
            textNewLetter.setText(res.getNewLetter());
            textNewUSDPrice.setText(res.getNewUSDPrice());

            lblResult.setText("Sucess");
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