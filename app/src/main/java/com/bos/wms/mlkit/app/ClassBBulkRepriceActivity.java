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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.customView.UPCCardDoneView;
import com.bos.wms.mlkit.storage.Storage;

import java.util.ArrayList;

import Model.BrandInToIsResponseModel;
import Model.ClassBRepriceResponseModel;
import Model.Pricing.PricingStandModel;
import Remote.APIClient;
import Remote.BasicApi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class ClassBBulkRepriceActivity extends AppCompatActivity {


    public String IPAddress = "";

    private ArrayList<String> PricedItems = new ArrayList<>();

    int UserID = -1;

    private Button btnPopUp;
    private Button btnDone;


    private EditText txtItem;

    private TextView lblResult;

    private String popUpMessages="";


    String PricingLineCode="";

    private String branch="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brands_in_to_is);

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
        PricingLineCode = mStorage.getDataString("PricingLineCode", "PL001");

        branch= General.getGeneral(this).mainLocation;

        UserID = General.getGeneral(this).UserID;
        btnPopUp = findViewById(R.id.btnPopUp);
        btnDone = findViewById(R.id.btnDone);
        txtItem = findViewById(R.id.txtItem);
        lblResult = findViewById(R.id.lblResult);

        ((TextView)findViewById(R.id.lblTitle)).setText("Class B Reprice - " + branch);
        ((TextView)findViewById(R.id.lblScanUPC)).setText("Scan Item Serial");

//        textPrevLBPPrice=findViewById(R.id.textPrevLBPPrice);
//        textPrevLetter=findViewById(R.id.textPrevLetter);
//        textPrevUSDPrice=findViewById(R.id.textPrevUSDPrice);
//        textItemSerial=findViewById(R.id.textItemSerial);
//        textItemNo=findViewById(R.id.textItemNo);

        upcsLayout=findViewById(R.id.linearLayoutUPCs);


        btnPopUp.setOnClickListener(e -> {
            showMessage("Info",
                    "Pinter = "+(PricingLineCode)+"\n-------------------------\n"
                    +popUpMessages);
        }); 
        btnDone.setOnClickListener(e -> {
            SendPrintOrder();
        });

        lblResult.setOnClickListener(e->{
            showMessage("Info",popUpMessages);
        });

        txtItem.requestFocus();
        txtItem.setInputType(InputType.TYPE_NULL);
        txtItem.addTextChangedListener(new ItemTextWatcher());


    }



    public void SendPrintOrder(){
        try {
            if(currentItems.size() == 0){
                showMessage("No Printing Items","Scan Valid Items First !!");
                return;
            }
            txtItem.setEnabled(false);
            btnDone.setEnabled(false);
            Logger.Debug("API", "RepriceClassB-SendPrintOrder");
            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.PrintV2(branch, -1, PricingLineCode)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                        if (s != null) {
                                            String message = "";
                                            try {
                                                message = s.string();
                                                Logger.Debug("API", "UInToIs-SendPrintOrder - Returned Response: " + message);
                                            } catch (Exception ex) {
                                                Logger.Error("API", "InToIs-SendPrintOrder - Error In Inner Response: " + ex.getMessage());
                                                message="Error "+ex.getMessage();
                                            }
                                            if(message.startsWith("Success") && message.equalsIgnoreCase("Success")){
                                                successPrint(message);

                                            }
                                            else{
                                                failPrint(message);
                                            }


                                        } else {
                                            Logger.Error("API", "RepriceClassB-SendPrintOrder - retuned null");
                                            failPrint("Web Service Returned Null");
                                        }
                                        currentItems.clear();
                                        PricedItems.clear();
                                        renderItems();
                                        txtItem.setEnabled(true);
                                        txtItem.requestFocus();
                                        btnDone.setEnabled(true);
                                    }
                                    , (throwable) -> {
                                        String error = "";
                                        if (throwable instanceof HttpException) {
                                            HttpException ex = (HttpException) throwable;
                                            error = ex.response().errorBody().string();
                                            if (error.isEmpty()) error = throwable.getMessage();
                                            Logger.Debug("API", "RepriceClassB-SendPrintOrder - Error In HTTP Response: " + error);
                                            failPrint( error + "\n(API Http Error)");
                                        } else {
                                            Logger.Error("API", "RepriceClassB-SendPrintOrder - Error In API Response: " + throwable.getMessage());
                                            failPrint( throwable.getMessage() + "/n(API Error)");
                                        }
                                        currentItems.clear();
                                        PricedItems.clear();
                                        renderItems();
                                        txtItem.setEnabled(true);
                                        txtItem.requestFocus();
                                        btnDone.setEnabled(true);
                                    }));

        } catch (Throwable e) {
            Logger.Error("API", "RepriceClassB-SendPrintOrder  Error" + e.getMessage());
            failPrint( e.getMessage() + " (Exception)");

            currentItems.clear();
            PricedItems.clear();
            renderItems();
            txtItem.setEnabled(true);
            txtItem.requestFocus();
            btnDone.setEnabled(true);


        }
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

            if(item.startsWith("22"))
                item = converterUPCAToItemSerial(item);

            if (item.length() != 13) {
                Beep();
                showMessage("Error","Invalid ItemSerial Scanned ("+item+")");
                txtItem.setText("");
                updatingText = false;
                return;
            }

            if(PricedItems.contains(item)){
                Beep();
                showMessage("Error","Item Already Scanned ("+item+")");
                txtItem.setText("");
                updatingText = false;
                return;
            }

            if(item.startsWith("IS")){

            }
            else{
                Beep();
                showMessage("Error","Invalid ItemSerial Scanned ("+item+")");
                txtItem.setText("");
                updatingText = false;
                return;
            }

            txtItem.setEnabled(false);

            Post(item);

        }
    }

    ArrayList<ClassBRepriceResponseModel> currentItems=new ArrayList<>();
    LinearLayout upcsLayout;

    public void renderItems(){
        if(currentItems == null)
            return;
        upcsLayout.removeAllViews();
        for (ClassBRepriceResponseModel detail:currentItems) {
            runOnUiThread(()->{
                upcsLayout.addView(new UPCCardDoneView(
                        this, ""
                        ,detail.getItemSerial(),"New \t\t\t\t\t\t\t Old",
                        detail.getPrevUSDPrice()==null?"":detail.getPrevUSDPrice(),
                        detail.getNewUSDPrice() ==null?"":detail.getNewUSDPrice()
                ));
            });


        }
    }




    private void Post(String item
    ) {

        txtItem.setEnabled(false);
        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress, true).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.Reprice(UserID,item,branch)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {
                                    if(!PricedItems.contains(item)){
                                        PricedItems.add(item);
                                    }
                                    s.setItemSerial(item);
                                    successItem("Success",s);

                                    Logger.Debug("API", "response " + s);
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
                                failItem("Failed",err);

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
            failItem("Failed",e.getMessage());
            Logger.Error("API", " Exception: " + e.getMessage());
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
    private void successItem(String message, ClassBRepriceResponseModel res){
        this.fullMessage = fullMessage;
        popUpMessages=message+"\n"+fullMessage;
        runOnUiThread(() -> {

            currentItems.add(0,res);
            renderItems();

            lblResult.setText("Sucess");
            lblResult.setTextColor(Color.WHITE);
            lblResult.setBackgroundResource(R.drawable.rounded_corner_green);
        });
    }


    private void failItem(String message, String fullMessage){

        this.fullMessage = fullMessage;
        popUpMessages=message+"\n"+fullMessage;
        runOnUiThread(() -> {
            lblResult.setText("Failed");
            lblResult.setTextColor(Color.WHITE);
            lblResult.setBackgroundResource(R.drawable.rounded_corner_red);
            Beep();
        });


    }

    private void successPrint(String message){
        this.fullMessage = message;
        showMessage("✓✓✓✓✓✓ Success ✓✓✓✓✓",message);
        popUpMessages=message+"\n"+fullMessage;
        currentItems.clear();
        PricedItems.clear();
        renderItems();

        runOnUiThread(() -> {
            lblResult.setText("Sucess");
            lblResult.setTextColor(Color.WHITE);
            lblResult.setBackgroundResource(R.drawable.rounded_corner_green);
        });
    }


    private void failPrint(String message){
        this.fullMessage = message;
        showMessage("xxxxx Fail xxxxx",message);
        popUpMessages=message;
        currentItems.clear();
        PricedItems.clear();
        renderItems();
        runOnUiThread(() -> {
            lblResult.setText("Failed");
            lblResult.setTextColor(Color.WHITE);
            lblResult.setBackgroundResource(R.drawable.rounded_corner_red);
            Beep();
        });


    }

    private void showMessageAndExit(String title, String msg, int color) {
        if (color == Color.RED)
            Beep();
        runOnUiThread(() -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(msg)
                    .setPositiveButton("Ok", (dialog, which) -> {
                        finish();
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .show();
        });
    }

    public String converterUPCAToItemSerial(String upca){
        return "IS00" + upca.substring(2, upca.length() - 1);
    }

}