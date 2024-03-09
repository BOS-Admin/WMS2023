package com.bos.wms.mlkit.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.app.Utils.AppHelperActivity;
import com.bos.wms.mlkit.app.adapters.UPCScannedItemDataModel;
import com.bos.wms.mlkit.storage.Storage;
import com.bos.wms.mlkit.utils.RFID.RFIDConnectionListener;
import com.bos.wms.mlkit.utils.RFID.RFIDController;
import com.bos.wms.mlkit.utils.RFID.RFIDHandlerOutputListener;
import com.bos.wms.mlkit.utils.RFID.RFIDSledHandler;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.snackbar.Snackbar;
import com.rfidread.Models.Tag_Model;

import org.json.JSONObject;

import java.util.List;

import Remote.APIClient;
import Remote.BasicApi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class EmptyBoxActivity extends AppHelperActivity {

    EditText insertBinBoxBarcode;

    Button scannedItemsCount;

    LinearLayout confirmationLayout;

    TextView confirmationMessage;

    FlexboxLayout confirmationButtons;

    Button confirmationYes, confirmationNo;

    int totalScannedItems = 0;

    public String IPAddress = "";

    public String CurrentBoxBarcode = null, CurrentBoxRFID = null;

    ProgressDialog mainProgressDialog;

    int UserID = -1;

    Button scannedItem;

    TextView scannedBoxBarcode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty_box);
        SetActivityMainView(findViewById(R.id.emptyBoxActivityLayout));
        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");

        UserID = General.getGeneral(this).UserID;

        insertBinBoxBarcode = findViewById(R.id.insertBinBoxBarcode);
        scannedItemsCount = findViewById(R.id.scannedItemsCount);

        confirmationLayout = findViewById(R.id.confirmationLayout);
        confirmationMessage = findViewById(R.id.confirmationMessage);
        confirmationButtons = findViewById(R.id.confirmationButtons);

        confirmationYes = findViewById(R.id.confirmationYes);
        confirmationNo = findViewById(R.id.confirmationNo);

        scannedItem = findViewById(R.id.scannedItem);
        scannedBoxBarcode = findViewById(R.id.scannedBoxBarcode);

        /**
         * This is used to always keep focus on the edit text so we can detect its text change
         * The text changes only when the user uses the scan method on the device and the text is pasted not typed
         */
        insertBinBoxBarcode.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                insertBinBoxBarcode.requestFocus();
            }
        });

        /**
         * This functions blocks the keyboard from poping up incase the uses presses on the edit text
         */
        insertBinBoxBarcode.setOnClickListener((click) -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(insertBinBoxBarcode.getWindowToken(), 0);
        });


        /**
         * Check for when the text of the edit is changed and add the pasted text to the scanned barcodes
         */
        insertBinBoxBarcode.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                if(s.length() != 0 && count > 2){
                    insertBinBoxBarcode.removeTextChangedListener(this);
                    insertBinBoxBarcode.setText(" ");
                    insertBinBoxBarcode.addTextChangedListener(this);

                    CurrentBoxBarcode = s.toString().replaceAll(" ", "");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scannedBoxBarcode.setText(CurrentBoxBarcode);
                            if(CurrentBoxRFID != null){
                                VerifyBinBarcode(CurrentBoxBarcode, CurrentBoxRFID);
                            }
                        }
                    });


                    //VerifyBinBarcode(s.toString().replaceAll(" ", ""));
                }else if(s.length() != 0 && !s.toString().isEmpty()){;
                    insertBinBoxBarcode.removeTextChangedListener(this);
                    insertBinBoxBarcode.setText(" ");
                    insertBinBoxBarcode.addTextChangedListener(this);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(insertBinBoxBarcode.getWindowToken(), 0);
                }
            }
        });

        insertBinBoxBarcode.requestFocus();

        if(RFIDController.CurrentSledHandler == null){
            RFIDController.CurrentSledHandler = new RFIDSledHandler(mStorage.getDataString("RFIDMac","00:00:00:00:00"));
        }



        mainProgressDialog = ProgressDialog.show(this, "",
                "Connecting To Sled, Please wait...", true);
        mainProgressDialog.show();


        RFIDController.CurrentSledHandler.SetConnectionListener(new RFIDConnectionListener() {
            @Override
            public void onConnectionEstablished(String deviceName, boolean newConnection) {
                mainProgressDialog.cancel();
            }

            @Override
            public void onConnectionFailed(String reason, boolean newConnection) {
                mainProgressDialog.cancel();
                //ShowErrorReturnDialog("Connection Error", "Connection With RFID Sled " + RFIDController.CurrentSledHandler.getMacAddress() + " Failed! Reason: " + reason);
            }
        });

        RFIDController.CurrentSledHandler.setTriggerConfigs(30000, 0);
        RFIDController.CurrentSledHandler.setNumberOfReadsBeforeProcess(2);
        RFIDController.CurrentSledHandler.setTriggerBased(true);
        RFIDController.CurrentSledHandler.setCurrentPower(7);

        if(!RFIDController.CurrentSledHandler.isConnected()){
            RFIDController.CurrentSledHandler.StartConnection(true);
        }else {
            mainProgressDialog.cancel();
        }

        RFIDController.CurrentSledHandler.SetOnOutputEventListener(new RFIDHandlerOutputListener() {
            @Override
            public void onTagRead(String rfid, Tag_Model model) {
                if(CurrentBoxRFID == null)
                {
                    CurrentBoxRFID = rfid;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scannedItem.setText("Rescan RFID");
                        }
                    });
                }
                else if(!CurrentBoxRFID.equalsIgnoreCase(rfid)){
                    ProcessMultiReadRFIDS(true);
                }
            }

            @Override
            public void onTagProcessed(String rfid, Tag_Model model, int count, boolean isTheOnlyTagRead) {
                Logger.Error("TEST", "Read RFID: " + rfid + " Is The Only One? " + isTheOnlyTagRead);
                if(isTheOnlyTagRead){
                    CreateViewBeatAnimation(scannedItem, 200, new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    scannedItem.setText("RFID Detected");
                                    scannedItem.setBackgroundColor(Color.parseColor("#52ac24"));
                                    if(CurrentBoxBarcode != null){
                                        VerifyBinBarcode(CurrentBoxBarcode, rfid);
                                    }
                                }
                            });
                        }
                    });
                }else ProcessMultiReadRFIDS(true);
            }
        });

        scannedItem.setOnClickListener(view -> {
            scannedItem.setText("Waiting For RFID");
            scannedItem.setBackgroundColor(Color.parseColor("#6200EE"));
            scannedBoxBarcode.setText("");
            CurrentBoxRFID = null;
            CurrentBoxBarcode = null;
        });

    }

    public void VerifyBinBarcode(String barcode, String rfid){
        try {

            if(Looper.myLooper() == null) {
                Looper.prepare();
            }

            Logger.Debug("API", "VerifyBinBarcode - Start Verify For Bin, Barcode '" + barcode + "'");

            mainProgressDialog = ProgressDialog.show(this, "",
                    "Verify Bin RFID, Please Wait...", true);
            mainProgressDialog.show();

            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();

            compositeDisposable.addAll(
                    api.VerifyEmptyBin(barcode, rfid)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    Logger.Debug("API", "VerifyBinBarcode - Returned: " + s.string());
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            scannedItem.setText("Waiting For RFID");
                                            scannedItem.setBackgroundColor(Color.parseColor("#6200EE"));
                                            scannedBoxBarcode.setText("");
                                            CurrentBoxRFID = null;
                                            CurrentBoxBarcode = null;
                                        }
                                    });
                                    ProcessBinBarCode(barcode);
                                    mainProgressDialog.cancel();
                                }
                            }, (throwable) -> {
                                String error = throwable.toString();
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    error = ex.response().errorBody().string();
                                    if(error.isEmpty()) error = throwable.getMessage();
                                    Logger.Debug("API", "VerifyBinBarcode - Error In HTTP Response: " + error);
                                }else {
                                    Logger.Error("API", "VerifyBinBarcode - Error In API Response: " + throwable.getMessage());
                                }
                                mainProgressDialog.cancel();
                                String finalError = error;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        scannedItem.setText("Waiting For RFID");
                                        scannedItem.setBackgroundColor(Color.parseColor("#6200EE"));
                                        scannedBoxBarcode.setText("");
                                        CurrentBoxRFID = null;
                                        CurrentBoxBarcode = null;
                                        ShowAlertDialog("Error", finalError);
                                    }
                                });
                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();
            Logger.Error("API", "VerifyBinBarcode - Error Connecting: " + e.getMessage());
            Snackbar.make(findViewById(R.id.emptyBoxActivityLayout), "Connection To Server Failed!", Snackbar.LENGTH_LONG)
                    .setAction("No action", null).show();
        }
    }


    /**
     * This function checks if the given barcode is valid and can be deleted
     * @param barcode
     */
    public void ProcessBinBarCode(String barcode){
        try {

            Logger.Debug("API", "ProcessBinBarCode - Start Process For Bin, Barcode '" + barcode + "'");

            confirmationButtons.setVisibility(View.GONE);
            confirmationMessage.setText("");
            confirmationLayout.setVisibility(View.GONE);

            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();

            compositeDisposable.addAll(
                    api.CheckBinEmpty(UserID, barcode)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    if(s.getSuccess()){
                                        confirmationButtons.setVisibility(View.GONE);
                                        confirmationMessage.setText(s.getMessage());
                                        confirmationLayout.setVisibility(View.VISIBLE);
                                        Logger.Debug("API", "ProcessBinBarCode - Bin Emptied Successfully, Barcode '" + barcode + "'");
                                        AddScannedItem();
                                    }else {
                                        confirmationButtons.setVisibility(View.VISIBLE);
                                        confirmationMessage.setText(s.getMessage() + ", Are You Sure You Want To Empty This Bin (" + barcode + ") ?");
                                        confirmationLayout.setVisibility(View.VISIBLE);

                                        Logger.Debug("API", "ProcessBinBarCode - Requesting Confirmation '" + s.getMessage() + "'");

                                        confirmationYes.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                EmptyBin(barcode);
                                            }
                                        });

                                        confirmationNo.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                confirmationButtons.setVisibility(View.GONE);
                                                confirmationMessage.setText("");
                                                confirmationLayout.setVisibility(View.GONE);
                                            }
                                        });

                                    }
                                }
                            }, (throwable) -> {
                                String error = throwable.toString();
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    error = ex.response().errorBody().string();
                                    if(error.isEmpty()) error = throwable.getMessage();
                                    Logger.Debug("API", "ProcessBinBarCode - Error In HTTP Response: " + error);
                                }else {
                                    Logger.Error("API", "ProcessBinBarCode - Error In API Response: " + throwable.getMessage());
                                }

                                confirmationButtons.setVisibility(View.GONE);
                                confirmationMessage.setText(error);
                                confirmationLayout.setVisibility(View.VISIBLE);

                            }));

        } catch (Throwable e) {
            Logger.Error("API", "ProcessBinBarCode - Error Connecting: " + e.getMessage());
            Snackbar.make(findViewById(R.id.emptyBoxActivityLayout), "Connection To Server Failed!", Snackbar.LENGTH_LONG)
                    .setAction("No action", null).show();
        }
    }

    /**
     * This function forcefully empties a bin without checking it
     * @param barcode
     */
    public void EmptyBin(String barcode){
        try {

            Logger.Debug("API", "ProcessBinBarCode - Bin Empty Start, Barcode '" + barcode + "'");

            confirmationButtons.setVisibility(View.GONE);
            confirmationMessage.setText("Please Wait, Emptying Bin");
            confirmationLayout.setVisibility(View.VISIBLE);

            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();

            compositeDisposable.addAll(
                    api.EmptyBin(UserID, barcode)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    if(s.contains("Success")){
                                        Logger.Debug("API", "ProcessBinBarCode - Bin Emptied Successfully, Barcode '" + barcode + "'");
                                        confirmationButtons.setVisibility(View.GONE);
                                        confirmationMessage.setText("Bin Emptied Successfully");
                                        confirmationLayout.setVisibility(View.VISIBLE);
                                        AddScannedItem();
                                    }else {
                                        Logger.Error("API", "ProcessBinBarCode - Occured While Trying To Empty Bin, Barcode '" + barcode + "'");
                                        confirmationButtons.setVisibility(View.GONE);
                                        confirmationMessage.setText("An Unknown Error Occured While Trying To Empty Bin (" + barcode + ").");
                                        confirmationLayout.setVisibility(View.VISIBLE);
                                    }
                                }
                            }, (throwable) -> {
                                String error = throwable.toString();
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    error = ex.response().errorBody().string();
                                    if(error.isEmpty()) error = throwable.getMessage();
                                    Logger.Debug("API", "EmptyBin - Error In HTTP Response: " + error);
                                }else {
                                    Logger.Error("API", "EmptyBin - Error In API Response: " + throwable.getMessage());
                                }

                                confirmationButtons.setVisibility(View.GONE);
                                confirmationMessage.setText(error);
                                confirmationLayout.setVisibility(View.VISIBLE);

                            }));

        } catch (Throwable e) {
            Logger.Error("API", "EmptyBin - Error Connecting: " + e.getMessage());
            Snackbar.make(findViewById(R.id.emptyBoxActivityLayout), "Connection To Server Failed!", Snackbar.LENGTH_LONG)
                    .setAction("No action", null).show();
        }
    }

    /**
     * This function increments the scanned items and creates an animation
     */
    public void AddScannedItem(){
        totalScannedItems++;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ObjectAnimator animate = ObjectAnimator.ofPropertyValuesHolder(scannedItemsCount,
                        PropertyValuesHolder.ofFloat("scaleX", 1.1f),
                        PropertyValuesHolder.ofFloat("scaleY", 1.1f));
                animate.setDuration(200);
                animate.setRepeatCount(ObjectAnimator.RESTART);
                animate.setRepeatMode(ObjectAnimator.REVERSE);
                animate.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(@NonNull Animator animator) {

                    }

                    @Override
                    public void onAnimationEnd(@NonNull Animator animator) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                scannedItemsCount.setText(totalScannedItems + " Bins Emptied");
                            }
                        });
                    }

                    @Override
                    public void onAnimationCancel(@NonNull Animator animator) {

                    }

                    @Override
                    public void onAnimationRepeat(@NonNull Animator animator) {

                    }
                });
                animate.start();
            }
        });
    }

    public void ProcessMultiReadRFIDS(boolean showError){
        if(showError)
            ShowSnackBar( "Multi RFID Read", Snackbar.LENGTH_LONG);

        CurrentBoxRFID = null;
        RFIDController.CurrentSledHandler.resetAllReadRFIDS();
        CreateViewBeatAnimation(scannedItem, 200, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        scannedItem.setText("Waiting For RFID");
                        scannedItem.setBackgroundColor(Color.parseColor("#6200EE"));
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        RFIDController.CurrentSledHandler.CloseConnection();
        super.onDestroy();
    }

}