package com.bos.wms.mlkit.app.tpo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

public class TPOLoadBinsActivity extends AppCompatActivity {


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

    boolean isTruckValid = false;

    String currentLocation = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tpoload_bins);

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
        UserID = General.getGeneral(getApplicationContext()).UserID;

        //Get The Current Location The Device
        currentLocation = General.getGeneral(getApplicationContext()).mainLocation;

        try{
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                TPOID = extras.getInt("TPOID");
                ToLocation = extras.getString("ToLocation");
                DateCreated = extras.getString("DateCreated");
            }
        }catch(Exception ex){
            Logger.Error("Activity", "TPOLoadBinsActivity - Failed Getting TPO Info From TPO Main Activity");
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
                    }else {
                        LoadBinItem(s.toString().replaceAll(" ", ""));
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
            AttemptTPOShipmentPrepared();
        });

        /**
         * These Are For Testing
         */
        //CurrentTruckBarcode = "Truck";//For Testing
        //confirmBtn.setEnabled(true);

    }

    /**
     * This function checks if a truck barcode is valid and gets its info
     * @param barcode
     */
    public void ValidateTruckBarcode(String barcode){
        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Getting Truck Info For: " + barcode + ", Please wait...", true);
        mainProgressDialog.show();

        Logger.Debug("TPO", "ValidateTruckBarcode - Getting Truck Info For Truck: " + barcode);

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.VerifyTPOTruckForShipment(barcode, false)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try {

                                        String result = s.string();

                                        Logger.Debug("TPO", "ValidateTruckBarcode - Received Truck Info For Truck: " + barcode + " " + result);

                                        /* We Will Get The Truck Info As Json And Convert It To TPOTruckInfoModel */
                                        TPOTruckInfoModel model = new Gson().fromJson(result, TPOTruckInfoModel.class);

                                        mainProgressDialog.cancel();
                                        CurrentTruckBarcode = barcode;
                                        isTruckValid = true;

                                        General.playSuccess();

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                scanBoxesTxt.setText("Please Scan The Bin Barcode Before Loading!");
                                                scanBoxesTxt.setBackgroundColor(Color.parseColor("#00A300"));

                                                truckInfoBtn.setText("Truck Name: " + model.getTruckName() + "\nTruck Barcode: " +
                                                        model.getTruckBarcode() + "\nTruck Plate: " + model.getTruckPlate());

                                            }
                                        });

                                    }catch(Exception ex){
                                        mainProgressDialog.cancel();
                                        Logger.Error("JSON", "ValidateTruckBarcode - Error: " + ex.getMessage());
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
                                    Logger.Debug("TPO", "ValidateTruckBarcode - Returned Error: " + response);
                                    mainProgressDialog.cancel();
                                    ShowSnackbar(response);
                                    General.playError();
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "ValidateTruckBarcode - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());

                                    mainProgressDialog.cancel();
                                    ShowErrorDialog(response);
                                }
                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();
            Logger.Error("API", "ValidateTruckBarcode - Error Connecting: " + e.getMessage());
            ShowSnackbar("Connection To Server Failed!");
            General.playError();
        }
    }

    /**
     * This function attempts to add a bin barcode to the list after communicating with the background
     * @param barcode
     */
    public void LoadBinItem(String barcode){
        if(isBusy)
            return;

        isBusy = true;

        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Loading Bin #" + barcode + " To The Truck, Please wait...", true);
        mainProgressDialog.show();

        Logger.Debug("TPO", "LoadBinItem - Adding Bin #" + barcode + " To TPO ID: " + TPOID);

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.VerifyTPOBinShipment(TPOID, CurrentTruckBarcode, barcode, UserID)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try {

                                        String result = s.string();

                                        Logger.Debug("TPO", "LoadBinItem - Received Result: " + result);


                                        mainProgressDialog.cancel();

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                confirmBtn.setEnabled(true);
                                            }
                                        });

                                        ShowSnackbar(result);
                                        General.playSuccess();

                                    }catch(Exception ex){
                                        mainProgressDialog.cancel();
                                        Logger.Error("JSON", "LoadBinItem - Error: " + ex.getMessage());
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
                                    Logger.Debug("TPO", "LoadBinItem - Returned Error: " + response);
                                    mainProgressDialog.cancel();
                                    ShowSnackbar(response);
                                    General.playError();
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "LoadBinItem - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                    mainProgressDialog.cancel();
                                    ShowErrorDialog(response);
                                }
                                isBusy = false;
                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();
            Logger.Error("API", "LoadBinItem - Error Connecting: " + e.getMessage());
            ShowErrorDialog("Connection To Server Failed!");
            isBusy = false;
        }
    }

    /**
     * This Is Called When The Bins Are All Scanned And We Need To Send The Truck To The Destination
     */
    public void AttemptTPOShipmentPrepared(){
        if(isBusy)
            return;

        isBusy = true;

        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Finalizing TPO Shipment, Please wait...", true);
        mainProgressDialog.show();

        Logger.Debug("TPO", "AttemptTPOShipmentPrepared - Finalizing TPO Shipment For TPO ID: " + TPOID);

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.AttemptTPOShipmentPrepared(TPOID, CurrentTruckBarcode, UserID)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try {

                                        String result = s.string();

                                        Logger.Debug("TPO", "AttemptTPOShipmentPrepared - Received Result: " + result);


                                        mainProgressDialog.cancel();

                                        General.playSuccess();
                                        ShowAlertDialog("Success", result, true);

                                    }catch(Exception ex){
                                        mainProgressDialog.cancel();
                                        Logger.Error("JSON", "AttemptTPOShipmentPrepared - Error: " + ex.getMessage());
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
                                    Logger.Debug("TPO", "AttemptTPOShipmentPrepared - Returned Error: " + response);
                                    mainProgressDialog.cancel();
                                    ShowErrorDialog(response);
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "AttemptTPOShipmentPrepared - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                    mainProgressDialog.cancel();
                                    ShowErrorDialog(response);
                                }
                                isBusy = false;
                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();
            Logger.Error("API", "AttemptTPOShipmentPrepared - Error Connecting: " + e.getMessage());
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

            Logger.Debug("TPO", "TPOLoadBinsActivity-OnBackPressed - Resetting Truck Count Mode For Truck: " + CurrentTruckBarcode);

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

                                            Logger.Debug("TPO", "TPOLoadBinsActivity-onBackPressed - Received Truck Count Reset For Truck: " + CurrentTruckBarcode + " " + result);

                                            mainProgressDialog.cancel();
                                            super.onBackPressed();

                                        } catch (Exception ex) {
                                            Logger.Error("JSON", "TPOLoadBinsActivity-onBackPressed - Error: " + ex.getMessage());
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
                                        Logger.Debug("TPO", "TPOLoadBinsActivity-onBackPressed - Returned Error: " + response);

                                    } else {
                                        response = throwable.getMessage();
                                        Logger.Error("API", "TPOLoadBinsActivity-onBackPressed - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                    }
                                    mainProgressDialog.cancel();
                                    General.playError();
                                    super.onBackPressed();
                                }));

            } catch (Throwable e) {
                mainProgressDialog.cancel();
                Logger.Error("API", "TPOLoadBinsActivity-onBackPressed - Error Connecting: " + e.getMessage());
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
     * This Function Is A Shortcut For Displaying Alert Dialogs
     * @param title
     * @param message
     * @param finishOnClose
     */
    public void ShowAlertDialog(String title, String message, boolean finishOnClose){
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(finishOnClose)
                            finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
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
        Snackbar.make(findViewById(R.id.tpoLoadBinsActivityLayout), message, Snackbar.LENGTH_LONG)
                .setAction("No action", null).show();
    }

}