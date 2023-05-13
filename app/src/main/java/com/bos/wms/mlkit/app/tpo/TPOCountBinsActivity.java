package com.bos.wms.mlkit.app.tpo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.app.Logger;
import com.bos.wms.mlkit.app.adapters.TPOItemsDialogDataModel;
import com.bos.wms.mlkit.storage.Storage;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import Model.TPO.TPOAvailableBinModel;
import Model.TPO.TPOTruckInfoModel;
import Remote.APIClient;
import Remote.BasicApi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class TPOCountBinsActivity extends AppCompatActivity {


    EditText insertBarcodeEditText;
    TextView tpoMenuTitle;

    Button tpoInfoBtn, truckInfoBtn, scanBoxesTxt, confirmBtn;

    String CurrentTruckBarcode = "";

    String IPAddress = "";
    int UserID = -1;
    int TPOID = -1;
    String ToLocation = "";
    String DateCreated = "";

    boolean isBusy = false;

    boolean isTruckValid = false, isCountValid = false;

    String currentLocation = "";

    int CurrentBoxCount = 0;

    public CountDownTimer countButtonHoldTimer = null;

    int ButtonHoldTime = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tpocount_bins);

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
        UserID = General.getGeneral(getApplicationContext()).UserID;

        //Get The Current Location The Device
        currentLocation = General.getGeneral(getApplicationContext()).mainLocation;

        try{
            int holdTime = Integer.parseInt(General.getGeneral(this).getSetting(this,"TPOCountButtonHoldTime"));
            ButtonHoldTime = holdTime;
            Logger.Debug("SystemControl", "Read Field TPOCountButtonHoldTime From System Control, Value: " + ButtonHoldTime);
        }catch(Exception ex){
            Logger.Error("SystemControl", "Error Getting Value For TPOCountButtonHoldTime, " + ex.getMessage());
        }

        try{
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                TPOID = extras.getInt("TPOID");
                ToLocation = extras.getString("ToLocation");
                DateCreated = extras.getString("DateCreated");
            }
        }catch(Exception ex){
            Logger.Error("Activity", "TPOCountBinsActivity - Failed Getting TPO Info From TPO Main Activity");
            finish();
        }

        tpoMenuTitle = findViewById(R.id.tpoMenuTitle);
        tpoInfoBtn = findViewById(R.id.tpoInfoBtn);
        truckInfoBtn = findViewById(R.id.truckInfoBtn);
        scanBoxesTxt = findViewById(R.id.scanBoxesTxt);
        confirmBtn = findViewById(R.id.confirmBtn);

        insertBarcodeEditText = findViewById(R.id.insertBarcodeEditText);

        tpoMenuTitle.setText("Current Location " + currentLocation);

        /**
         * This is used to always keep focus on the edit text so we can detect its text change
         * The text changes only when the user uses the scan method on the device and the text is pasted not typed
         */
        insertBarcodeEditText.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                insertBarcodeEditText.requestFocus();
            }
        });

        /**
         * This functions blocks the keyboard from poping up incase the uses presses on the edit text
         */
        insertBarcodeEditText.setOnClickListener((click) -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(insertBarcodeEditText.getWindowToken(), 0);
        });

        /**
         * Check for when the text of the edit is changed and add the pasted text to the list of upcs scanned
         */
        insertBarcodeEditText.addTextChangedListener(new TextWatcher() {

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
                    insertBarcodeEditText.removeTextChangedListener(this);
                    insertBarcodeEditText.setText(" ");
                    insertBarcodeEditText.addTextChangedListener(this);

                    if(!isTruckValid){
                        ValidateTruckBarcode(s.toString().replaceAll(" ", ""));
                    }
                }else if(s.length() != 0 && !s.toString().isEmpty()){
                    insertBarcodeEditText.removeTextChangedListener(this);
                    insertBarcodeEditText.setText(" ");
                    insertBarcodeEditText.addTextChangedListener(this);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(insertBarcodeEditText.getWindowToken(), 0);
                }
            }
        });


        insertBarcodeEditText.requestFocus();

        tpoInfoBtn.setText("TPO ID: " + TPOID + "\nHeading To " + ToLocation + "\nCreated At " + DateCreated.replaceAll("T", " "));

        scanBoxesTxt.setText("Please Scan A Truck Barcode To Begin!");
        scanBoxesTxt.setBackgroundColor(Color.parseColor("#D10000"));

        confirmBtn.setEnabled(false);

        confirmBtn.setOnClickListener(view -> {
            VerifyTPOBinShipmentCount();
        });

        scanBoxesTxt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    if(isCountValid && countButtonHoldTimer == null) {
                        countButtonHoldTimer = new CountDownTimer(ButtonHoldTime, ButtonHoldTime) {
                            @Override
                            public void onTick(long l) {

                            }

                            @Override
                            public void onFinish() {
                                SubmitCount();
                                countButtonHoldTimer = null;
                            }
                        }.start();
                    }

                } else if (action == MotionEvent.ACTION_UP
                        || action == MotionEvent.ACTION_CANCEL) {
                    if(countButtonHoldTimer != null){
                        countButtonHoldTimer.cancel();
                        countButtonHoldTimer = null;
                    }
                }
                return false;
            }
        });


    }

    /**
     * This function is used to increment the count of the bins by one
     */
    public void SubmitCount(){
        CurrentBoxCount++;
        Logger.Debug("TPO", "TPOCountBinsActivity-SubmitCount - Count Incremented, New Count: " + CurrentBoxCount);
        confirmBtn.setEnabled(true);
        try{
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

            v.vibrate(400);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ObjectAnimator animate = ObjectAnimator.ofPropertyValuesHolder(scanBoxesTxt,
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

        }catch(Exception ex){
            Logger.Error("TPO", "TPOCountBinsActivity-SubmitCount Failed Vibrating: " + ex.getMessage());
        }
    }

    /**
     * This function checks if a truck barcode is valid and gets its info
     * @param barcode
     */
    public void ValidateTruckBarcode(String barcode){
        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Getting Truck Info For: " + barcode + ", Please wait...", true);
        mainProgressDialog.show();

        Logger.Debug("TPO", "TPOCountBinsActivity-ValidateTruckBarcode - Getting Truck Info For Truck: " + barcode);

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.VerifyTPOTruckForShipment(barcode, true)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try {

                                        String result = s.string();

                                        Logger.Debug("TPO", "TPOCountBinsActivity-ValidateTruckBarcode - Received Truck Info For Truck: " + barcode + " " + result);

                                        /* We Will Get The Truck Info As Json And Convert It To TPOTruckInfoModel */
                                        TPOTruckInfoModel model = new Gson().fromJson(result, TPOTruckInfoModel.class);

                                        mainProgressDialog.cancel();
                                        CurrentTruckBarcode = barcode;
                                        isTruckValid = true;

                                        General.playSuccess();

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                scanBoxesTxt.setClickable(false);
                                                scanBoxesTxt.setText("Truck Needs To Be In Count Mode To Count Bins!");
                                                //scanBoxesTxt.setBackgroundColor(Color.parseColor("#00A300"));

                                                StartTruckBinCount(barcode);

                                                truckInfoBtn.setText("Truck Name: " + model.getTruckName() + "\nTruck Barcode: " +
                                                        model.getTruckBarcode() + "\nTruck Plate: " + model.getTruckPlate());

                                            }
                                        });

                                    }catch(Exception ex){
                                        mainProgressDialog.cancel();
                                        Logger.Error("JSON", "TPOCountBinsActivity-ValidateTruckBarcode - Error: " + ex.getMessage());
                                        ShowErrorDialog(ex.getMessage());
                                    }
                                }
                            }, (throwable) -> {
                                //This Will Translate The Error Response And Get The Error Body If Available
                                String response = "";
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    response = ex.response().errorBody().string();
                                    if(response.isEmpty()){
                                        response = throwable.getMessage();
                                    }
                                    Logger.Debug("TPO", "TPOCountBinsActivity-ValidateTruckBarcode - Returned Error: " + response);
                                    mainProgressDialog.cancel();
                                    ShowSnackbar(response);
                                    General.playError();
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "TPOCountBinsActivity-ValidateTruckBarcode - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());

                                    mainProgressDialog.cancel();
                                    ShowErrorDialog(response);
                                }
                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();
            Logger.Error("API", "TPOCountBinsActivity-ValidateTruckBarcode - Error Connecting: " + e.getMessage());
            ShowSnackbar("Connection To Server Failed!");
            General.playError();
        }
    }

    /**
     * This function sets the truck in counting mode
     * @param barcode
     */
    public void StartTruckBinCount(String barcode){
        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Setting The Truck In Count Mode, Truck: " + barcode + ", Please wait...", true);
        mainProgressDialog.show();

        Logger.Debug("TPO", "TPOCountBinsActivity-StartTruckBinCount - Starting Count For Truck: " + barcode);

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.StartTruckBinCount(barcode)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try {

                                        String result = s.string();

                                        Logger.Debug("TPO", "TPOCountBinsActivity-StartTruckBinCount - Received Truck Count: " + barcode + " " + result);

                                        mainProgressDialog.cancel();

                                        General.playSuccess();

                                        isCountValid = true;

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                scanBoxesTxt.setClickable(true);
                                                scanBoxesTxt.setText("Please Hold This Button To Increment The Box Count By One!");
                                                scanBoxesTxt.setBackgroundColor(Color.parseColor("#00A300"));

                                            }
                                        });

                                    }catch(Exception ex){
                                        mainProgressDialog.cancel();
                                        Logger.Error("JSON", "TPOCountBinsActivity-StartTruckBinCount - Error: " + ex.getMessage());
                                        ShowErrorDialog(ex.getMessage());
                                    }
                                }
                            }, (throwable) -> {
                                //This Will Translate The Error Response And Get The Error Body If Available
                                String response = "";
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    response = ex.response().errorBody().string();
                                    if(response.isEmpty()){
                                        response = throwable.getMessage();
                                    }
                                    Logger.Debug("TPO", "TPOCountBinsActivity-StartTruckBinCount - Returned Error: " + response);
                                    mainProgressDialog.cancel();
                                    ShowSnackbar(response);
                                    General.playError();
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "TPOCountBinsActivity-StartTruckBinCount - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());

                                    mainProgressDialog.cancel();
                                    ShowErrorDialog(response);
                                }
                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();
            Logger.Error("API", "TPOCountBinsActivity-StartTruckBinCount - Error Connecting: " + e.getMessage());
            ShowSnackbar("Connection To Server Failed!");
            General.playError();
        }
    }

    /**
     * This Is Called When The Bins Are All Scanned And We Need To Send The Truck To The Destination
     */
    public void VerifyTPOBinShipmentCount(){
        if(isBusy)
            return;

        isBusy = true;

        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Submitting TPO Shipment Count, Please wait...", true);
        mainProgressDialog.show();

        Logger.Debug("TPO", "VerifyTPOBinShipmentCount - Submitting TPO Shipment Count For TPO ID: " + TPOID + " Count: " + CurrentBoxCount);

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.VerifyTPOBinShipmentCount(TPOID, CurrentTruckBarcode, CurrentBoxCount, UserID)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try {

                                        String result = s.string();

                                        Logger.Debug("TPO", "VerifyTPOBinShipmentCount - Received Result: " + result);


                                        mainProgressDialog.cancel();


                                        ShowSnackbar(result);
                                        General.playSuccess();

                                        finish();

                                    }catch(Exception ex){
                                        mainProgressDialog.cancel();
                                        Logger.Error("JSON", "VerifyTPOBinShipmentCount - Error: " + ex.getMessage());
                                        ShowErrorDialog(ex.getMessage());
                                    }
                                    isBusy = false;
                                }
                            }, (throwable) -> {
                                //This Will Translate The Error Response And Get The Error Body If Available
                                String response = "";
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    response = ex.response().errorBody().string();
                                    if(response.isEmpty()){
                                        response = throwable.getMessage();
                                    }
                                    Logger.Debug("TPO", "VerifyTPOBinShipmentCount - Returned Error: " + response);
                                    mainProgressDialog.cancel();
                                    ShowErrorDialog(response);
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "VerifyTPOBinShipmentCount - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                    mainProgressDialog.cancel();
                                    ShowErrorDialog(response);
                                }
                                isBusy = false;
                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();
            Logger.Error("API", "VerifyTPOBinShipmentCount - Error Connecting: " + e.getMessage());
            ShowErrorDialog("Connection To Server Failed!");
            isBusy = false;
        }
    }

    /**
     * Check If The User Added A bin Then Pressed Back Then Set The Truck In Normal Mode
     */
    @Override
    public void onBackPressed() {

        if(isTruckValid) {
            ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                    "Resetting Truck Count Mode, Please wait...", true);
            mainProgressDialog.show();

            Logger.Debug("TPO", "TPOCountBinsActivity-OnBackPressed - Resetting Truck Count Mode For Truck: " + CurrentTruckBarcode);

            try {
                BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
                CompositeDisposable compositeDisposable = new CompositeDisposable();


                compositeDisposable.addAll(
                        api.ResetTruckBinCount(CurrentTruckBarcode, true)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe((s) -> {
                                    if (s != null) {
                                        try {

                                            String result = s.string();

                                            Logger.Debug("TPO", "TPOCountBinsActivity-onBackPressed - Received Truck Count Reset For Truck: " + CurrentTruckBarcode + " " + result);

                                            mainProgressDialog.cancel();
                                            super.onBackPressed();

                                        } catch (Exception ex) {
                                            Logger.Error("JSON", "TPOCountBinsActivity-onBackPressed - Error: " + ex.getMessage());
                                            mainProgressDialog.cancel();
                                            General.playError();
                                            super.onBackPressed();
                                        }
                                    }
                                }, (throwable) -> {
                                    //This Will Translate The Error Response And Get The Error Body If Available
                                    String response = "";
                                    if (throwable instanceof HttpException) {
                                        HttpException ex = (HttpException) throwable;
                                        response = ex.response().errorBody().string();
                                        if (response.isEmpty()) {
                                            response = throwable.getMessage();
                                        }
                                        Logger.Debug("TPO", "TPOCountBinsActivity-onBackPressed - Returned Error: " + response);

                                    } else {
                                        response = throwable.getMessage();
                                        Logger.Error("API", "TPOCountBinsActivity-onBackPressed - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                    }
                                    mainProgressDialog.cancel();
                                    General.playError();
                                    super.onBackPressed();
                                }));

            } catch (Throwable e) {
                mainProgressDialog.cancel();
                Logger.Error("API", "TPOCountBinsActivity-onBackPressed - Error Connecting: " + e.getMessage());
                General.playError();
                super.onBackPressed();
            }
        }else {
            super.onBackPressed();
        }
    }

    /**
     * This Function Is A Shortcut For Displaying Alert Dialogs
     * @param title
     * @param message
     * @param icon
     */
    public void ShowAlertDialog(String title, String message, int icon){
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(icon)
                .show();

    }

    /**
     * This Function Is A Shortcut For Displaying The Error Dialog
     * @param message
     */
    public void ShowErrorDialog(String message){
        ShowAlertDialog("Error", message, android.R.drawable.ic_dialog_alert);
        General.playError();
    }

    /**
     * This Functions Will Help Remove Replicate Code For Showing The SnackBar
     * @param message
     */
    public void ShowSnackbar(String message){
        Snackbar.make(findViewById(R.id.tpoCountBinsActivityLayout), message, Snackbar.LENGTH_LONG)
                .setAction("No action", null).show();
    }

}