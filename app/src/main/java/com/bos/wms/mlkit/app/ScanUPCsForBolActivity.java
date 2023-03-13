package com.bos.wms.mlkit.app;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.app.adapters.UPCScannedAdapter;
import com.bos.wms.mlkit.app.adapters.UPCScannedItemDataModel;
import com.bos.wms.mlkit.storage.Storage;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import Remote.APIClient;
import Remote.BasicApi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class ScanUPCsForBolActivity extends AppCompatActivity {

    ListView listView;
    ImageButton clearUSPCSImageButton, printerImageButton;
    EditText insertUPCEditText;
    ArrayList<UPCScannedItemDataModel> dataModels;

    UPCScannedAdapter adapter;


    public int CurrentMinUPCS = 0;

    public boolean isBusy = false;

    public String IPAddress = "";

    public TextView scanUPCSHelpText;

    public ZebraPrinter printer;

    public String[] currentPrintData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_upcs_for_bol);

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
        printer = new ZebraPrinter(mStorage.getDataString("PrinterMacAddress", "00"));

        listView = findViewById(R.id.scannedUPCsList);
        insertUPCEditText = findViewById(R.id.insertUPCEditText);
        clearUSPCSImageButton = findViewById(R.id.clearUSPCSImageButton);
        scanUPCSHelpText = findViewById(R.id.scanUPCSHelpText);

        dataModels = new ArrayList<>();

        adapter = new UPCScannedAdapter(dataModels, getApplicationContext(), listView, this);

        /* Printer Data */

        printerImageButton = findViewById(R.id.printerImageButton);
        printerImageButton.setImageResource(R.drawable.baseline_print_disabled_icon);

        //This will allow us to enable the printer in the UI first, then we can double check if it is still connecting after the first connection
        if(ZebraPrinter.isFirstConnectionEstablished()){
            printerImageButton.setImageResource(R.drawable.baseline_print_icon);
        }else {
            printerImageButton.setImageResource(R.drawable.baseline_print_disabled_icon);
        }

        printer.setOnPrinterConnectionFailListener((result) -> {
            if(!result){ //Connection Failed
                printerImageButton.setImageResource(R.drawable.baseline_print_disabled_icon);
                ZebraPrinter.setFirstConnectionEstablished(false);
            }
        });

        //Open the mac dialog to change the mac address
        printerImageButton.setOnLongClickListener((click) -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Please scan or type the printer's mac address");

            final EditText input = new EditText(this);

            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);
            builder.setPositiveButton("Pair", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ZebraPrinter.AttemptConnection(input.getText().toString(), (result) -> {
                        if(result){ //Connection Established
                            printerImageButton.setImageResource(R.drawable.baseline_print_icon);
                            ZebraPrinter.setFirstConnectionEstablished(true);
                            mStorage.saveData("PrinterMacAddress", input.getText().toString());
                            printer.setMacAddress(input.getText().toString());
                            Snackbar.make(findViewById(R.id.scanUPCsForBolActivityLayout), "Printer Paired Successfully", Snackbar.LENGTH_LONG)
                                    .setAction("No action", null).show();
                        }else {
                            //printerImageButton.setImageResource(R.drawable.baseline_print_disabled_icon);
                            //printerEnabled = false; This isnt needed because if the old printer was enabled we need to be able to reprint to it
                            Snackbar.make(findViewById(R.id.scanUPCsForBolActivityLayout), "Printer Failed To Pair", Snackbar.LENGTH_LONG)
                                    .setAction("No action", null).show();
                        }
                    });
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
            input.requestFocus();
            return false;
        });

        printerImageButton.setOnClickListener((click) -> {
            if(ZebraPrinter.isFirstConnectionEstablished()){
                if(currentPrintData != null && currentPrintData.length == 2){
                    printer.printBolData(currentPrintData[0], currentPrintData[1]);
                }
            }else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Please scan or type the printer's mac address");

                final EditText input = new EditText(this);

                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                builder.setPositiveButton("Pair", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ZebraPrinter.AttemptConnection(input.getText().toString(), (result) -> {
                            if(result){ //Connection Established
                                printerImageButton.setImageResource(R.drawable.baseline_print_icon);
                                ZebraPrinter.setFirstConnectionEstablished(true);
                                mStorage.saveData("PrinterMacAddress", input.getText().toString());
                                printer.setMacAddress(input.getText().toString());
                                Snackbar.make(findViewById(R.id.scanUPCsForBolActivityLayout), "Printer Paired Successfully", Snackbar.LENGTH_LONG)
                                        .setAction("No action", null).show();
                            }else {
                                printerImageButton.setImageResource(R.drawable.baseline_print_disabled_icon);
                                ZebraPrinter.setFirstConnectionEstablished(false);
                                Snackbar.make(findViewById(R.id.scanUPCsForBolActivityLayout), "Printer Failed To Pair", Snackbar.LENGTH_LONG)
                                        .setAction("No action", null).show();
                            }
                        });
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
                input.requestFocus();
            }
        });


        clearUSPCSImageButton.setOnClickListener((click) -> {
            dataModels.clear();
            adapter.notifyDataSetChanged();
            CurrentMinUPCS = BolRecognitionActivity.MinScannedItemsUPC;
            scanUPCSHelpText.setText("Start Scanning UPCS");
            isBusy = false;
        });


        /**
         * This is used to always keep focus on the edit text so we can detect its text change
         * The text changes only when the user uses the scan method on the device and the text is pasted not typed
         */
        insertUPCEditText.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                insertUPCEditText.requestFocus();
            }
        });

        /**
         * This functions blocks the keyboard from poping up incase the uses presses on the edit text
         */
        insertUPCEditText.setOnClickListener((click) -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(insertUPCEditText.getWindowToken(), 0);
        });

        CurrentMinUPCS = BolRecognitionActivity.MinScannedItemsUPC;

        /**
         * Check for when the text of the edit is changed and add the pasted text to the list of upcs scanned
         */
        insertUPCEditText.addTextChangedListener(new TextWatcher() {

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
                    insertUPCEditText.removeTextChangedListener(this);
                    insertUPCEditText.setText(" ");
                    insertUPCEditText.addTextChangedListener(this);
                    if(!isBusy) {
                        if(!getAllUPCs().contains(s.toString().replaceAll(" ", ""))){
                            if (dataModels.size() < CurrentMinUPCS) {
                                dataModels.add(new UPCScannedItemDataModel(s.toString()));
                                adapter.notifyDataSetChanged();
                                if (dataModels.size() == CurrentMinUPCS) {
                                    SubmitUPCS();
                                }
                            } else {
                                SubmitUPCS();
                            }
                        }
                    }
                }else if(s.length() != 0 && !s.toString().isBlank()){;
                    insertUPCEditText.setText(" ");
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(insertUPCEditText.getWindowToken(), 0);
                }
            }
        });


        listView.setAdapter(adapter);
        /*listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                UPCScannedItemDataModel dataModel = dataModels.get(position);


            }
        });*/

        insertUPCEditText.requestFocus();

    }

    /**
     * This function is called to make sure that when an item is deleted from the upc list and the user was required to scan more items it decreases the amount of items
     * by 1 so the user doesnt have to scan 2 more. also it makes sure the minimum scan value is always 4
     *
     * NOTE: Please test if the values are under 4 and an item is deleted
     */
    public void CheckMinUPCForChanges(){
        if(dataModels.size() < CurrentMinUPCS && CurrentMinUPCS != BolRecognitionActivity.MinScannedItemsUPC){
            if(CurrentMinUPCS - 1 > dataModels.size()){
                CurrentMinUPCS -= 1;
                CheckMinUPCForChanges();
            }
        }
    }

    /**
     * Submit the scanned upcs to the GetBolByUPCs API and get the result
     * If the UPCS dont match a bol number the user is requested to scan more items
     * If the UPCS lead to a bol number then the Bol number and Box serial are then printed automatically using the agent's portable printer
     *
     * The function also checks if the users reached a maximum amount of upcs scanned (Max Number is MinScannedItemsUPC Got From the System Control Table
     * multiplied by 3) If this maximum is reached then the BOL can't be identified and the user is requested to scan different items or move on from the box
     *
     */
    public void SubmitUPCS(){
        /*ProgressDialog dialog = ProgressDialog.show(ScanUPCS.this, "",
                "Checking UPCs Please Wait...", true);*/
        isBusy = true;
        Logger.Debug("UPC", "SubmitUPCS - Submit Total " + dataModels.size() + " UPCS");
;        if(CurrentMinUPCS + 1 > BolRecognitionActivity.MinScannedItemsUPC * 3) {

            try {
                BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
                CompositeDisposable compositeDisposable = new CompositeDisposable();

                List<String> upcs = getAllUPCs();

                compositeDisposable.addAll(
                        api.GetBolByUPCs(General.getGeneral(getApplicationContext()).UserID, upcs)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe((s) -> {
                                    if(s != null){
                                        try{
                                            JSONObject json = new JSONObject(s.string());
                                            String formattedResult = "BOL: " + json.getString("bol") + " Box Serial: " + json.getString("boxSerial");
                                            /*new AlertDialog.Builder(this)
                                                    .setTitle("BOL Number")
                                                    .setMessage(formattedResult)
                                                    .setPositiveButton("Print", new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            //Send API Request To Print Data
                                                            CurrentMinUPCS = BolRecognitionActivity.MinScannedItemsUPC;
                                                            dataModels.clear();
                                                            adapter.notifyDataSetChanged();
                                                            isBusy = false;
                                                        }
                                                    })
                                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            CurrentMinUPCS = BolRecognitionActivity.MinScannedItemsUPC;
                                                            dataModels.clear();
                                                            adapter.notifyDataSetChanged();
                                                            isBusy = false;
                                                        }
                                                    })
                                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                                    .show();*/
                                            isBusy = false;
                                            scanUPCSHelpText.setText(formattedResult);
                                            currentPrintData = new String[]{
                                                    json.getString("bol"),
                                                    json.getString("boxSerial")
                                            };
                                            if(ZebraPrinter.isFirstConnectionEstablished()) {
                                                //Print Bol Number And Serial
                                                printer.printBolData(json.getString("bol"), json.getString("boxSerial"));
                                            }
                                            Logger.Debug("UPC", "SubmitUPCS - Detected " + formattedResult);
                                        }catch(Exception ex){
                                        /*Snackbar.make(findViewById(R.id.mainActivityLayout), "Result Parsing Failed", Snackbar.LENGTH_LONG)
                                                .setAction("No action", null).show();*/
                                            /*new AlertDialog.Builder(this)
                                                    .setTitle("Error")
                                                    .setMessage(s.string())
                                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            Snackbar.make(findViewById(R.id.scanUPCsForBolActivityLayout), "Please Try Other Items", Snackbar.LENGTH_LONG)
                                                                    .setAction("No action", null).show();
                                                            CurrentMinUPCS = BolRecognitionActivity.MinScannedItemsUPC;
                                                            dataModels.clear();
                                                            adapter.notifyDataSetChanged();
                                                            isBusy = false;
                                                        }
                                                    })
                                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                                    .show();*/
                                            Snackbar.make(findViewById(R.id.scanUPCsForBolActivityLayout), "Please Scan Other Items", Snackbar.LENGTH_LONG)
                                                    .setAction("No action", null).show();
                                            scanUPCSHelpText.setText("Please Scan Other Items");
                                            CurrentMinUPCS = BolRecognitionActivity.MinScannedItemsUPC;
                                            dataModels.clear();
                                            adapter.notifyDataSetChanged();
                                            isBusy = false;
                                            Logger.Debug("API", "SubmitUPCS - Error: " + ex.getMessage() + " ReturnedResult: " + s.string());
                                        }

                                    }
                                }, (throwable) -> {
                                    if(throwable instanceof HttpException){
                                        HttpException ex = (HttpException) throwable;
                                        /*new AlertDialog.Builder(this)
                                                .setTitle("Error")
                                                .setMessage(ex.response().errorBody().string())
                                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Snackbar.make(findViewById(R.id.scanUPCsForBolActivityLayout), "Please Try Other Items", Snackbar.LENGTH_LONG)
                                                                .setAction("No action", null).show();
                                                        CurrentMinUPCS = BolRecognitionActivity.MinScannedItemsUPC;
                                                        dataModels.clear();
                                                        adapter.notifyDataSetChanged();
                                                        isBusy = false;
                                                    }
                                                })
                                                .setIcon(android.R.drawable.ic_dialog_alert)
                                                .show();*/
                                        Snackbar.make(findViewById(R.id.scanUPCsForBolActivityLayout), "Please Scan Other Items", Snackbar.LENGTH_LONG)
                                                .setAction("No action", null).show();
                                        scanUPCSHelpText.setText("Please Scan Other Items");
                                        CurrentMinUPCS = BolRecognitionActivity.MinScannedItemsUPC;
                                        dataModels.clear();
                                        adapter.notifyDataSetChanged();
                                        isBusy = false;
                                        Logger.Debug("API", "SubmitUPCS - Error In HTTP Response: " + ex.response().errorBody().string());
                                    }else {
                                        Snackbar.make(findViewById(R.id.scanUPCsForBolActivityLayout), "Internal Error Occurred", Snackbar.LENGTH_LONG)
                                                .setAction("No action", null).show();
                                        Logger.Error("API", "SubmitUPCS - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                        isBusy = false;
                                    }
                                }));

            } catch (Throwable e) {
                Logger.Error("API", "SubmitUPCS - Error Connecting: " + e.getMessage());
                Snackbar.make(findViewById(R.id.scanUPCsForBolActivityLayout), "Connection To Server Failed!", Snackbar.LENGTH_LONG)
                        .setAction("No action", null).show();
                isBusy = false;
            }

        }else {

            try {
                BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
                CompositeDisposable compositeDisposable = new CompositeDisposable();

                List<String> upcs = getAllUPCs();

                compositeDisposable.addAll(
                        api.GetBolByUPCs(General.getGeneral(getApplicationContext()).UserID, upcs)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe((s) -> {
                                    if(s != null){
                                        try{
                                            JSONObject json = new JSONObject(s.string());
                                            String formattedResult = "BOL: " + json.getString("bol") + " " + "Box Serial: " + json.getString("boxSerial");
                                            /*new AlertDialog.Builder(this)
                                                    .setTitle("BOL Number")
                                                    .setMessage(formattedResult)
                                                    .setPositiveButton("Print", new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            //Send API Request To Print Data
                                                            CurrentMinUPCS = BolRecognitionActivity.MinScannedItemsUPC;
                                                            dataModels.clear();
                                                            adapter.notifyDataSetChanged();
                                                            isBusy = false;
                                                        }
                                                    })
                                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            //Do Nothing
                                                            CurrentMinUPCS = BolRecognitionActivity.MinScannedItemsUPC;
                                                            dataModels.clear();
                                                            adapter.notifyDataSetChanged();
                                                            isBusy = false;
                                                        }
                                                    })
                                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                                    .show();*/
                                            isBusy = false;
                                            scanUPCSHelpText.setText(formattedResult);
                                            currentPrintData = new String[]{
                                                    json.getString("bol"),
                                                    json.getString("boxSerial")
                                            };
                                            if(ZebraPrinter.isFirstConnectionEstablished()) {
                                                //Print Bol Number And Serial
                                                printer.printBolData(json.getString("bol"), json.getString("boxSerial"));
                                            }
                                            Logger.Debug("UPC", "SubmitUPCS - Detected " + formattedResult);
                                        }catch(Exception ex){
                                        /*Snackbar.make(findViewById(R.id.mainActivityLayout), "Result Parsing Failed", Snackbar.LENGTH_LONG)
                                                .setAction("No action", null).show();*/
                                            /*new AlertDialog.Builder(this)
                                                    .setTitle("Error")
                                                    .setMessage(s.string())
                                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            Snackbar.make(findViewById(R.id.scanUPCsForBolActivityLayout), "Please Scan More Items", Snackbar.LENGTH_LONG)
                                                                    .setAction("No action", null).show();
                                                            CurrentMinUPCS += BolRecognitionActivity.MinScannedItemsUPC;
                                                            isBusy = false;
                                                        }
                                                    })
                                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                                    .show();*/
                                            Snackbar.make(findViewById(R.id.scanUPCsForBolActivityLayout), "Please Scan More Items", Snackbar.LENGTH_SHORT)
                                                    .setAction("No action", null).show();
                                            CurrentMinUPCS += 1;
                                            isBusy = false;
                                            scanUPCSHelpText.setText("Please Scan More Items");
                                            Logger.Debug("API", "SubmitUPCS - Error: " + ex.getMessage() + " ReturnedResult: " + s.string());
                                        }

                                    }
                                }, (throwable) -> {
                                    if(throwable instanceof HttpException){
                                        HttpException ex = (HttpException) throwable;
                                        /*new AlertDialog.Builder(this)
                                                .setTitle("Error")
                                                .setMessage(ex.response().errorBody().string())
                                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Snackbar.make(findViewById(R.id.scanUPCsForBolActivityLayout), "Please Scan More Items", Snackbar.LENGTH_LONG)
                                                                .setAction("No action", null).show();
                                                        CurrentMinUPCS += BolRecognitionActivity.MinScannedItemsUPC;
                                                        isBusy = false;
                                                    }
                                                })
                                                .setIcon(android.R.drawable.ic_dialog_alert)
                                                .show();*/
                                        Snackbar.make(findViewById(R.id.scanUPCsForBolActivityLayout), "Please Scan More Items", Snackbar.LENGTH_SHORT)
                                                .setAction("No action", null).show();
                                        CurrentMinUPCS += 1;
                                        isBusy = false;
                                        scanUPCSHelpText.setText("Please Scan More Items");
                                        Logger.Debug("API", "SubmitUPCS - Error In HTTP Response: " + ex.response().errorBody().string());
                                    }else {
                                        Snackbar.make(findViewById(R.id.scanUPCsForBolActivityLayout), "Internal Error Occurred", Snackbar.LENGTH_LONG)
                                                .setAction("No action", null).show();
                                        Logger.Error("API", "SubmitUPCS - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                        isBusy = false;
                                    }
                                }));

            } catch (Throwable e) {
                Logger.Error("API", "SubmitUPCS - Error Connecting: " + e.getMessage());
                Snackbar.make(findViewById(R.id.scanUPCsForBolActivityLayout), "Connection To Server Failed!", Snackbar.LENGTH_LONG)
                        .setAction("No action", null).show();
                isBusy = false;
            }
        }
    }

    /**
     *
     * @return A list of string containing all upcs scanned from the dataModel of the the upcs
     */
    public List<String> getAllUPCs(){
        List<String> upcs = new ArrayList<>();

        for(UPCScannedItemDataModel model : dataModels){
            upcs.add(model.getUPC().replaceAll(" ", ""));
        }
        return upcs;
    }

}