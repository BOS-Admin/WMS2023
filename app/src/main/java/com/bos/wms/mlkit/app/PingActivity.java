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
import com.bos.wms.mlkit.app.bosApp.PackingDCActivity;
import com.bos.wms.mlkit.customView.UPCCardDoneView;
import com.bos.wms.mlkit.storage.Storage;

import java.util.ArrayList;

import Model.BrandInToIsResponseModel;
import Model.Pricing.PricingStandModel;
import Remote.APIClient;
import Remote.BasicApi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class PingActivity extends AppCompatActivity {


    public String IPAddress = "";

    int UserID = -1;

    private Button btnPopUp;
    private Button btnDone;


    private EditText txtItem;

    private TextView lblResult;

    private String popUpMessages="";


    String PricingLineCode="";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ping);

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
         PricingLineCode = mStorage.getDataString("PricingLineCode", "PL001");

        UserID = General.getGeneral(this).UserID;
        btnPopUp=findViewById(R.id.btnPopUp);
        btnDone=findViewById(R.id.btnDone);
        txtItem=findViewById(R.id.txtItem);
        lblResult=findViewById(R.id.lblResult);

//        textPrevLBPPrice=findViewById(R.id.textPrevLBPPrice);
//        textPrevLetter=findViewById(R.id.textPrevLetter);
//        textPrevUSDPrice=findViewById(R.id.textPrevUSDPrice);
//        textItemSerial=findViewById(R.id.textItemSerial);
//        textItemNo=findViewById(R.id.textItemNo);

        upcsLayout=findViewById(R.id.linearLayoutUPCs);


        btnPopUp.setOnClickListener(e -> {
            showMessage("Info",
                    "Printing Session Id= "+(stand==null?"":stand.getId())+"\n-------------------------\n"
                    +"Pinter = "+(PricingLineCode)+"\n-------------------------\n"
                    +popUpMessages);
        }); 
        btnDone.setOnClickListener(e -> {
            try{
                String s=SendPrintOrder(txtItem.getText().toString());
                showMessage("Ping",s);
            }
            catch (Exception exx){
                showMessage("Ping",exx.getMessage());
            }

        });

        lblResult.setOnClickListener(e->{
            showMessage("Info",popUpMessages);
        });

        txtItem.requestFocus();
//        txtItem.setInputType(InputType.TYPE_NULL);
//        txtItem.addTextChangedListener(new ItemTextWatcher());


    }




    public String SendPrintOrder(String ip){
        return  PackingDCActivity.executeCommand(ip);
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
            if (item.length() < 10 || item.length() >16) {
                Beep();
                showMessage("Error","Invalid UPC Scanned ("+item+")");
                txtItem.setText("");
                updatingText = false;
                return;
            }

            if(isNumeric(item)){
                item="IN"+item;
            }
            else{
                Beep();
                showMessage("Error","Invalid UPC Scanned ("+item+")");
                txtItem.setText("");
                updatingText = false;
                return;
            }

            txtItem.setEnabled(false);

            if(stand==null)
                PostAndCreateStand(item);
            else
                Post(item);

        }
    }

    public static boolean isNumeric(String str) {
        try {
            String num=str.toLowerCase()
                    .replace("a","")
                    .replace("b","")
                    .replace("c","")
                    .replace("d","")
                    .replace("e","")
                    .replace("f","");

            Double.parseDouble(num);
            return true;
        } catch(Exception e){
            return false;
        }
    }


    private void PostAndCreateStand(String item
    ) {
        try {
            Logger.Debug("API", "InToIs-Creating Stand");
            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            int UserId=General.getGeneral(this.getApplicationContext()).UserID;
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.CreateBrandsPricingStand(UserId, PricingLineCode)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {

                                        currentItems.clear();
                                        renderItems();

                                        if (s != null) {
                                            Logger.Debug("API", "InToIs-Create Printing Session  Returned Result: " + s + " Userid: " + UserId + " PricingLineCode: " + PricingLineCode);
                                            stand = s;

                                            runOnUiThread(() -> {
                                                Post(item);

                                            });
                                        } else {
                                            txtItem.setEnabled(false);
                                            Logger.Error("API", "InToIs-Create Printing Session - retuned null:  Userid: " + UserId + " PricingLineCode: " + PricingLineCode);
                                            General.playError();
                                            showMessageAndExit("Failed To Create Printing Session", "Web Service Returned Null", Color.RED);
                                        }
                                    }
                                    , (throwable) -> {

                                        currentItems.clear();
                                        renderItems();
                                        txtItem.setEnabled(false);
                                        String error = throwable.toString();
                                        if (throwable instanceof HttpException) {
                                            HttpException ex = (HttpException) throwable;
                                            error = ex.response().errorBody().string();
                                            if (error.isEmpty()) error = throwable.getMessage();
                                            Logger.Debug("API", "InToIs-Create Printing Session- Error In HTTP Response: " + error);
                                            showMessageAndExit("Failed To Create Printing Session", error + " (API Http Error)", Color.RED);
                                        } else {
                                            Logger.Error("API", "Brands InToIs-Creating Stand - Error In API Response: " + throwable.getMessage());
                                            showMessageAndExit("Failed To Create Printing Session", throwable.getMessage() + " (API Error)", Color.RED);
                                        }



                                    }));

        } catch (Throwable e) {

            currentItems.clear();
            renderItems();

            txtItem.setEnabled(false);
            Logger.Error("API", "InToIs-Creating Stand: Error" + e.getMessage());
            showMessageAndExit("Failed To Create Printing Session", e.getMessage() + " (Exception)", Color.RED);


        }
    }

    ArrayList<BrandInToIsResponseModel> currentItems=new ArrayList<>();
    LinearLayout upcsLayout;

    public void renderItems(){
        if(currentItems == null)
            return;
//        showMessage("Items",""+currentItems.size());

        upcsLayout.removeAllViews();
        for (BrandInToIsResponseModel detail:currentItems) {
            runOnUiThread(()->{
                upcsLayout.addView(new UPCCardDoneView(
                        this, detail.getCompany()==null?"":detail.getCompany()
                        ,detail.getItemSerial()==null?"":detail.getItemSerial(),
                        detail.getItemNo()==null?"":detail.getItemNo(),
                        detail.getPrevPrice()==null?"":detail.getPrevPrice(),
                        detail.getNewPrice() ==null?"":detail.getNewPrice()
                ));
            });


        }
    }




    private void Post(String item
    ) {

        txtItem.setEnabled(false);
        if(stand==null){

            failItem("Failed","No Printing Session Available");

            runOnUiThread(() -> {
                txtItem.setEnabled(true);
                updatingText = true;
                txtItem.setText("");
                txtItem.requestFocus();
                updatingText = false;

            });
            return;

        }
        try {
            //   String IPAddressWarehouseManager = mStorage.getDataString("IPAddressWarehouseManager", "192.168.10.82");
            BasicApi api = APIClient.getInstanceStatic(IPAddress, true).create(BasicApi.class);

            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.BrandsInToIs(UserID,item,stand.getId(),PricingLineCode)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {
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
//        textPrevLBPPrice.setText("");
//        textPrevLetter.setText("");
//        textPrevUSDPrice.setText("");
//        textItemSerial.setText("");
////        textItemNo.setText("");
//        currentItems.clear();
//        renderItems();

    }

    private void Beep() {
        new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300);
    }
    private void successItem(String message, BrandInToIsResponseModel res){
        this.fullMessage = fullMessage;
        popUpMessages=message+"\n"+fullMessage;
        runOnUiThread(() -> {

            currentItems.add(0,res);
            renderItems();

//
//            textPrevLBPPrice.setText(""+res.getPrevPrice());
//            textPrevLBPPrice.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
//            textPrevLetter.setText(res.getLetter());
//            textPrevUSDPrice.setText(res.getNewPrice());
//            textItemSerial.setText(res.getItemSerial());
//            textItemNo.setText(res.getItemNo());


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
        renderItems();
        runOnUiThread(() -> {
            lblResult.setText("Failed");
            lblResult.setTextColor(Color.WHITE);
            lblResult.setBackgroundResource(R.drawable.rounded_corner_red);
            Beep();
        });


    }
    PricingStandModel stand;

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


}