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

public class TPOReceiveBinsActivity extends AppCompatActivity {


    EditText insertBarcodeEditText;
    TextView tpoMenuTitle;

    Button estimateInfoBtn, receivedInfoBtn, scanBoxesTxt, confirmBtn;

    String CurrentTruckBarcode = "";

    String IPAddress = "";
    int UserID = -1;

    boolean isBusy = false;

    boolean isTruckValid = false;

    String currentLocation = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tporeceive_bins);

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
        UserID = General.getGeneral(getApplicationContext()).UserID;

        //Get The Current Location The Device
        currentLocation = General.getGeneral(getApplicationContext()).mainLocation;

        tpoMenuTitle = findViewById(R.id.tpoMenuTitle);
        estimateInfoBtn = findViewById(R.id.estimateInfoBtn);
        receivedInfoBtn = findViewById(R.id.receivedInfoBtn);
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

        //tpoInfoBtn.setText("TPO ID: " + TPOID + "\nHeading To " + ToLocation + "\nCreated At " + DateCreated.replaceAll("T", " "));

        scanBoxesTxt.setText("Please Scan A Truck Barcode To Begin!");
        scanBoxesTxt.setBackgroundColor(Color.parseColor("#D10000"));

        confirmBtn.setEnabled(false);

        confirmBtn.setOnClickListener(view -> {
            AttemptReceiveShipment();
        });
    }

    /**
     * This function checks if a truck barcode is valid and gets its info
     * @param barcode
     */
    public void ValidateTruckBarcode(String barcode){

    }

    /**
     * This function attempts to add a bin barcode to the list after communicating with the background
     * @param barcode
     */
    public void LoadBinItem(String barcode){

    }

    /**
     * This Is Called When The Bins Are All Scanned And We Need To Send The Truck To The Destination
     */
    public void AttemptReceiveShipment(){
        /*if(isBusy)
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


                                        ShowSnackbar(result);
                                        General.playSuccess();

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
        }*/
    }

    /**
     * Check If The User Added A bin Then Pressed Back Then Set The Truck In Normal Mode
     */
    @Override
    public void onBackPressed() {
        //Wait
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
        Snackbar.make(findViewById(R.id.tpoReceiveBinsActivityLayout), message, Snackbar.LENGTH_LONG)
                .setAction("No action", null).show();
    }

}