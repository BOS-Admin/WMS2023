package com.bos.wms.mlkit.app.tpo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.app.Logger;
import com.bos.wms.mlkit.app.ZebraPrinter;
import com.bos.wms.mlkit.app.adapters.BinBarcodeRemovedListener;
import com.bos.wms.mlkit.app.adapters.BinBarcodeScannedAdapter;
import com.bos.wms.mlkit.app.adapters.TPOItemsDialogDataModel;
import com.bos.wms.mlkit.storage.Storage;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.util.ArrayList;

import Model.TPO.OverrideBinModel;
import Model.TPO.ReceivedTPOBinsModel;
import Model.TPO.TPOAvailableBinModel;
import Model.TPO.TPOReceivedBinModel;
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

    boolean isTruckValid = false;

    String currentLocation = "";

    String TruckBarcodeStartsWith = "NULL";

    ArrayList<TPOItemsDialogDataModel> dataModels;

    BinBarcodeScannedAdapter adapter;

    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tporeceive_bins);

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
        UserID = General.getGeneral(getApplicationContext()).UserID;

        //Get The Current Location The Device
        currentLocation = General.getGeneral(getApplicationContext()).mainLocation;
        //currentLocation = "W1005";


        tpoMenuTitle = findViewById(R.id.tpoMenuTitle);
        estimateInfoBtn = findViewById(R.id.estimateInfoBtn);
        receivedInfoBtn = findViewById(R.id.receivedInfoBtn);
        scanBoxesTxt = findViewById(R.id.scanBoxesTxt);
        confirmBtn = findViewById(R.id.confirmBtn);

        insertBarcodeEditText = findViewById(R.id.insertBarcodeEditText);

        tpoMenuTitle.setText("Current Location " + currentLocation);


        listView = findViewById(R.id.tpoReceiveBins);

        dataModels = new ArrayList<>();

        adapter = new BinBarcodeScannedAdapter(dataModels, getApplicationContext(), listView, new BinBarcodeRemovedListener() {
            @Override
            public void onItemRemoved(TPOItemsDialogDataModel dataModel) {

            }
        });

        listView.setAdapter(adapter);

        TPOReceivedInfo.OverrideBins = new ArrayList<>();
        TPOReceivedInfo.BinIDS = new ArrayList<>();
        TPOReceivedInfo.UsedOverridePasswords = new ArrayList<>();

        try{
            String barcode = General.getGeneral(this).getSetting(this,"TPOTruckBarcodeStartsWith");
            TruckBarcodeStartsWith = barcode;
            Logger.Debug("SystemControl", "Read Field TPOTruckBarcodeStartsWith From System Control, Value: " + TruckBarcodeStartsWith);
        }catch(Exception ex){
            Logger.Error("SystemControl", "Error Getting Value For TPOTruckBarcodeStartsWith, " + ex.getMessage());
        }

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

                    /* Check If A Truck Is Scanned First Or If The User Finished Scanning Boxes And Scanned Another Truck */

                    String barcode = s.toString().replaceAll(" ", "");

                    if(!isTruckValid){
                        ValidateTruckBarcode(barcode);
                    }else {
                        if(barcode.startsWith(TruckBarcodeStartsWith)){
                            ValidateTruckBarcode(barcode);
                        }else {
                            ProcessBinBarcode(barcode);
                        }
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

        scanBoxesTxt.setText("Please Scan A Truck Barcode To Begin!");
        scanBoxesTxt.setBackgroundColor(Color.parseColor("#D10000"));

        confirmBtn.setEnabled(true);

        confirmBtn.setOnClickListener(view -> {

            confirmBtn.setEnabled(false);
            confirmBtn.setVisibility(View.GONE);

            int totalBins = 0;
            int receivedBins = 0;

            for(TPOReceivedBinModel model : TPOReceivedInfo.ReceivedItems){
                if(model.getTruckBarcode().equalsIgnoreCase(CurrentTruckBarcode)){
                    totalBins++;
                    if(TPOReceivedInfo.BinIDS.contains(model.getId())){
                        receivedBins++;
                    }
                }
            }

            //Check If The User Received All The Bins They Were Supposed To Receive
            if(totalBins > receivedBins){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Enter The Override Password To Leave " + (totalBins - receivedBins) + " Behind!");

                final EditText input = new EditText(this);

                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                builder.setPositiveButton("Override", null);

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                AlertDialog openedDialog = builder.show();

                int finalTotalBins = totalBins;
                int finalReceivedBins = receivedBins;
                openedDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String password = input.getText().toString();
                    if(TPOReceivedInfo.OverridePasswords.contains(password)){
                        Logger.Debug("TPO", "User Entered An Override Password: " + password + " For TruckOverride: " + currentLocation +
                                " TotalOverridePasswords: " + TPOReceivedInfo.OverridePasswords.size() + " TotalBins: " + finalTotalBins + " ReceivedBins: " + finalReceivedBins);
                        TPOReceivedInfo.UsedOverridePasswords.add(password);

                        AttemptReceiveShipment();

                        General.playSuccess();
                        openedDialog.cancel();
                    }else {
                        //builder.setTitle("Invalid Override Password!");
                        ShowErrorDialog("Invalid Override Password!");
                        Logger.Debug("TPO", "User Entered An Invalid Override Password: " + password + " For TruckOverride: " + currentLocation +
                                " TotalOverridePasswords: " + TPOReceivedInfo.OverridePasswords.size());
                    }
                });
            }else {
                AttemptReceiveShipment();
            }
        });

        GetOverridePasswords();

    }

    /**
     * This function checks if a truck barcode is valid and gets its info
     * @param barcode
     */
    public void ValidateTruckBarcode(String barcode){
        if(barcode.startsWith(TruckBarcodeStartsWith)){
            if(IsTruckValid(barcode)){
                isTruckValid = true;
                CurrentTruckBarcode = barcode;

                scanBoxesTxt.setText("Please Scan The Bin Barcode, Or A New Truck's Barcode!");
                scanBoxesTxt.setBackgroundColor(Color.parseColor("#00A300"));

                UpdateUIForTruck();


            }else {
                Logger.Debug("TPO-RECEIVE", "ValidateTruckBarcode - Scanned A Truck Barcode That Wasn't Supposed To Be Received: " + barcode);
                ShowErrorDialog("The Truck Barcode You Scanned: " + barcode + " Is Not Supposed To Be Received, Or Invalid!");
            }
        }else {
            Logger.Debug("TPO-RECEIVE", "ValidateTruckBarcode - Scanned An Invalid Truck Barcode: " + barcode);
            ShowSnackbar("Invalid Truck Barcode: " + barcode);
            General.playError();
        }
    }

    /**
     * This function attempts to add a bin barcode to the list after communicating with the background
     * @param barcode
     */
    public void ProcessBinBarcode(String barcode){
        boolean binNeedsOverride = true;
        for (TPOReceivedBinModel model : TPOReceivedInfo.ReceivedItems) {
            if(model.getBinBarcode().equalsIgnoreCase(barcode)){

                binNeedsOverride = false;//The Bin Is Valid And Doesn't Need Override

                if(model.getTruckBarcode().equalsIgnoreCase(CurrentTruckBarcode)){

                    //Process The Bin Here
                    if(TPOReceivedInfo.BinIDS == null){
                        TPOReceivedInfo.BinIDS = new ArrayList<>();
                    }

                    if(TPOReceivedInfo.BinIDS.contains(model.getId())){
                        ShowSnackbar("The Bin Barcode You Scanned: " + barcode + " Was Already Scanned And Received!");
                        General.playError();
                    }else {
                        Logger.Debug("TPO-RECEIVE", "ProcessBinBarcode - Scanned A Bin Barcode To Be Received. " +
                                "CurrentTruck: " + CurrentTruckBarcode + " BinBarcode: " + barcode + " BinTruckBarcode: " + model.getTruckBarcode() +
                                " BinTPOID: " + model.getTpoID() + " BinID: " + model.getId());
                        TPOReceivedInfo.BinIDS.add(model.getId());
                        UpdateUIForBins();
                        ShowSnackbar("The Bin Barcode You Scanned: " + barcode + " Is Received Successfully!");
                        General.playSuccess();
                    }

                    break;
                }else {
                    Logger.Debug("TPO-RECEIVE", "ProcessBinBarcode - Scanned A Bin Barcode That Isn't In The Current Truck. " +
                            "CurrentTruck: " + CurrentTruckBarcode + " BinBarcode: " + barcode + " BinTruckBarcode: " + model.getTruckBarcode() +
                            " BinTPOID: " + model.getTpoID());
                    ShowErrorDialog("The Bin Barcode You Scanned: " + barcode + " Does Not Belong To This Truck, Please Scan The New Truck Barcode!");
                    break;
                }
            }
        }
        if(binNeedsOverride){
            //Bin Needs To Be Overriden
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Please Type The Override Password");

            final EditText input = new EditText(this);

            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            builder.setPositiveButton("Override", null);

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            AlertDialog openedDialog = builder.show();

            openedDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String password = input.getText().toString();
                if(TPOReceivedInfo.OverridePasswords.contains(password)){
                    Logger.Debug("TPO-RECEIVE", "ProcessBinBarcode - Scanned A Bin Barcode To Be Received. " +
                            "CurrentTruck: " + CurrentTruckBarcode + " BinBarcode: " + barcode + " No Extra Data (Bin Was Overrided With Password: " + password + ")");
                    TPOReceivedInfo.OverrideBins.add(new OverrideBinModel(barcode, CurrentTruckBarcode, password));
                    TPOReceivedInfo.UsedOverridePasswords.add(password);
                    ShowSnackbar("The Bin Barcode You Scanned: " + barcode + " Is Received Successfully!");
                    General.playSuccess();
                    openedDialog.cancel();
                    UpdateUIForTruck();
                }else {
                    //builder.setMessage("Invalid Override Password!");
                    ShowErrorDialog("Invalid Override Password!");
                    Logger.Debug("TPO", "User Entered An Invalid Override Password: " + password + " For Bin: " + barcode +
                            " TotalOverridePasswords: " + TPOReceivedInfo.OverridePasswords.size());
                }
            });

        }

        UpdateUIForTruck();

    }

    /**
     * This Is Called When The Bins Are All Scanned And We Need To Send The Truck To The Destination
     */
    public void AttemptReceiveShipment(){

        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Finalizing TPO Shipment, Please wait...", true);
        mainProgressDialog.show();

        Logger.Debug("TPO", "AttemptReceiveShipment - Finalizing TPO Shipment Total Received Bins: " + (TPOReceivedInfo.BinIDS.size() + TPOReceivedInfo.OverrideBins.size()));

        try {
            BasicApi api = APIClient.getNewInstanceStatic(IPAddress,120).create(BasicApi.class);//120 Seconds For Timeout
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.AddReceivedTPOBins(new ReceivedTPOBinsModel(TPOReceivedInfo.BinIDS, TPOReceivedInfo.OverrideBins, TPOReceivedInfo.UsedOverridePasswords,
                                    currentLocation, UserID))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try {

                                        String result = s.string();

                                        Logger.Debug("TPO", "AttemptReceiveShipment - Received Result: " + result);


                                        mainProgressDialog.cancel();

                                        ShowAlertDialog("TPO Shipment Done", result, android.R.drawable.ic_dialog_alert, true);

                                        General.playSuccess();

                                    }catch(Exception ex){
                                        mainProgressDialog.cancel();
                                        confirmBtn.setEnabled(true);
                                        confirmBtn.setVisibility(View.VISIBLE);
                                        Logger.Error("JSON", "AttemptReceiveShipment - Error: " + ex.getMessage());
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
                                    Logger.Debug("TPO", "AttemptReceiveShipment - Returned Error: " + response);
                                    mainProgressDialog.cancel();
                                    ShowErrorDialog(response);
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "AttemptReceiveShipment - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                    mainProgressDialog.cancel();
                                    ShowErrorDialog(response);
                                }

                                confirmBtn.setEnabled(true);
                                confirmBtn.setVisibility(View.VISIBLE);

                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();
            Logger.Error("API", "AttemptReceiveShipment - Error Connecting: " + e.getMessage());
            ShowErrorDialog("Connection To Server Failed!");
            confirmBtn.setEnabled(true);
            confirmBtn.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Check If The User Added A bin Then Pressed Back Then Set The Truck In Normal Mode
     */
    @Override
    public void onBackPressed() {
        return;
    }

    /**
     * This function will check if the current truck is supposed to be received and we have data on it
     * @param barcode
     * @return
     */
    public boolean IsTruckValid(String barcode){
        for (TPOReceivedBinModel model : TPOReceivedInfo.ReceivedItems) {
            if(model.getTruckBarcode().equalsIgnoreCase(barcode)){
                return true;
            }
        }
        return false;
    }

    /**
     * This Function Will Get All The Override Passwords Before We Can Begin The Bin Receiving Process
     */
    public void GetOverridePasswords(){
        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Retrieving Other Info, Please wait...", true);
        mainProgressDialog.show();

        Logger.Debug("TPO", "GetOverridePasswords - Retrieving Override Passwords");

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.GetAllOverridePasswords("TPOBinOverride")
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try {

                                        Logger.Debug("TPO", "GetOverridePasswords - Received TPO Override Passwords: " + s.size());

                                        TPOReceivedInfo.OverridePasswords = new ArrayList<>();

                                        for(String pass : s){
                                            TPOReceivedInfo.OverridePasswords.add(pass);
                                        }

                                        mainProgressDialog.cancel();


                                    }catch(Exception ex){
                                        mainProgressDialog.cancel();
                                        Logger.Error("JSON", "GetOverridePasswords - Error: " + ex.getMessage());
                                        ShowErrorDialog(ex.getMessage(), true);
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
                                    Logger.Debug("TPO", "GetOverridePasswords - Returned Error: " + response);
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "GetOverridePasswords - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                }

                                mainProgressDialog.cancel();

                                ShowErrorDialog(response, true);

                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();
            Logger.Error("API", "GetOverridePasswords - Error Connecting: " + e.getMessage());
            ShowErrorDialog("Connection To Server Failed!", true);
        }
    }

    /**
     * This Updates The UI To View The Remaining Bins To Scan
     */
    public void UpdateUIForBins(){
        //dataModels.clear();
        //adapter.notifyDataSetChanged();
        for(TPOReceivedBinModel model : TPOReceivedInfo.ReceivedItems){
            if(model.getTruckBarcode().equalsIgnoreCase(CurrentTruckBarcode)){
                if(TPOReceivedInfo.BinIDS.contains(model.getId())){
                    for(TPOItemsDialogDataModel item : dataModels.toArray(new TPOItemsDialogDataModel[] {})){
                        if(item.getMessage().equalsIgnoreCase(model.getBinBarcode())){
                            dataModels.remove(item);
                        }
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    /**
     * This functions updates the truck received and estimated bins
     */
    public void UpdateUIForTruck(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int totalBins = 0;
                int receivedBins = 0;
                int overridedBins = 0;

                dataModels.clear();
                adapter.notifyDataSetChanged();

                for(TPOReceivedBinModel model : TPOReceivedInfo.ReceivedItems){
                    if(model.getTruckBarcode().equalsIgnoreCase(CurrentTruckBarcode)){
                        totalBins++;
                        if(TPOReceivedInfo.BinIDS.contains(model.getId())){
                            receivedBins++;
                        }else {
                            dataModels.add(new TPOItemsDialogDataModel(model.getBinBarcode()));

                            adapter.notifyDataSetChanged();
                        }
                    }
                }

                for(OverrideBinModel model : TPOReceivedInfo.OverrideBins){
                    if(model.getTruckBarcode().equalsIgnoreCase(CurrentTruckBarcode)){
                        overridedBins++;
                    }
                }

                estimateInfoBtn.setText(CurrentTruckBarcode + " Estimated Bin Amount Is " + totalBins);
                receivedInfoBtn.setText(CurrentTruckBarcode + " Received Bin Amount Is " + receivedBins + (overridedBins > 0 ? " Overrided " + overridedBins + " Bins" : ""));
            }
        });
    }

    /**
     * This Function Is A Shortcut For Displaying Alert Dialogs
     * @param title
     * @param message
     * @param icon
     */
    public void ShowAlertDialog(String title, String message, int icon, boolean closeOnDone){
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(closeOnDone){
                            finish();
                        }
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
        ShowAlertDialog("Error", message, android.R.drawable.ic_dialog_alert, false);
        General.playError();
    }

    /**
     * This Function Is A Shortcut For Displaying The Error Dialog With Closing The App On Done
     * @param message
     */
    public void ShowErrorDialog(String message, boolean closeOnDone){
        ShowAlertDialog("Error", message, android.R.drawable.ic_dialog_alert, closeOnDone);
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