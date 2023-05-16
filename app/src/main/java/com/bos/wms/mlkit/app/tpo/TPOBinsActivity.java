package com.bos.wms.mlkit.app.tpo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.bos.wms.mlkit.Extensions;
import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.app.BolRecognitionActivity;
import com.bos.wms.mlkit.app.Logger;
import com.bos.wms.mlkit.app.adapters.BinBarcodeRemovedListener;
import com.bos.wms.mlkit.app.adapters.BinBarcodeScannedAdapter;
import com.bos.wms.mlkit.app.adapters.TPOItemsDialogAdapter;
import com.bos.wms.mlkit.app.adapters.TPOItemsDialogDataModel;
import com.bos.wms.mlkit.app.adapters.TPOItemsDialogTPOInfoDataModel;
import com.bos.wms.mlkit.app.adapters.UPCScannedAdapter;
import com.bos.wms.mlkit.app.adapters.UPCScannedItemDataModel;
import com.bos.wms.mlkit.storage.Storage;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.util.ArrayList;

import Model.TPO.TPOAvailableBinModel;
import Model.TPO.TPOModel;
import Remote.APIClient;
import Remote.BasicApi;
import Remote.UserPermissions.UserPermissions;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class TPOBinsActivity extends AppCompatActivity {

    ListView listView;
    ImageButton shipTPOBinsIcon, currentModifyMode;
    EditText insertBarcodeEditText;
    TextView tpoMenuTitle;
    ArrayList<TPOItemsDialogDataModel> dataModels;

    BinBarcodeScannedAdapter adapter;

    String IPAddress = "";
    int UserID = -1;
    int TPOID = -1;

    boolean isBusy = false;

    boolean CurrentModeAdding = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tpobins);

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
        UserID = General.getGeneral(getApplicationContext()).UserID;

        try{
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                int value = extras.getInt("TPOID");
                TPOID = value;
            }
        }catch(Exception ex){
            Logger.Error("Activity", "TPOBinsActivity - Failed Getting TPOID From TPO Main Activity");
            finish();
        }

        listView = findViewById(R.id.binItemsList);
        insertBarcodeEditText = findViewById(R.id.insertBarcodeEditText);
        shipTPOBinsIcon = findViewById(R.id.shipTPOBinsIcon);
        currentModifyMode = findViewById(R.id.currentModifyMode);
        tpoMenuTitle = findViewById(R.id.tpoMenuTitle);

        //Verify The Permission For Shipment
        UserPermissions.ValidatePermission("WMSApp.TPO.ReadyTPO", shipTPOBinsIcon);

        dataModels = new ArrayList<>();

        adapter = new BinBarcodeScannedAdapter(dataModels, getApplicationContext(), listView, new BinBarcodeRemovedListener() {
            @Override
            public void onItemRemoved(TPOItemsDialogDataModel dataModel) {
                RemoveBinItem(dataModel.getMessage());
            }
        });

        currentModifyMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CurrentModeAdding = !CurrentModeAdding;

                if(CurrentModeAdding){
                    Extensions.setImageDrawableWithAnimation(currentModifyMode, getDrawable(R.drawable.baseline_add_24), 300);
                    tpoMenuTitle.setText("Adding Bins");
                    tpoMenuTitle.setBackgroundColor(Color.parseColor("#00A300"));//Green Color
                }else {
                    Extensions.setImageDrawableWithAnimation(currentModifyMode, getDrawable(R.drawable.upc_item_delete_icon), 300);
                    tpoMenuTitle.setText("Removing Bins");
                    tpoMenuTitle.setBackgroundColor(Color.parseColor("#D10000"));//Red Color
                }

            }
        });


        shipTPOBinsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReadyTPOBins();
            }
        });

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

                    if(CurrentModeAdding){
                        AddBinItem(s.toString().replaceAll(" ", ""));
                    }else {
                        RemoveBinItem(s.toString().replaceAll(" ", ""));
                    }
                }else if(s.length() != 0 && !s.toString().isEmpty()){;
                    insertBarcodeEditText.removeTextChangedListener(this);
                    insertBarcodeEditText.setText(" ");
                    insertBarcodeEditText.addTextChangedListener(this);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(insertBarcodeEditText.getWindowToken(), 0);
                }
            }
        });


        listView.setAdapter(adapter);

        insertBarcodeEditText.requestFocus();

        GetPreviousBinsForThisTPO();

    }

    /**
     * This Function gets all available bins for this tpo and adds them to the list
     */
    public void GetPreviousBinsForThisTPO(){
        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Getting Available Bins For TPO ID: " + TPOID + ", Please wait...", true);
        mainProgressDialog.show();

        Logger.Debug("TPO", "GetPreviousBinsForThisTPO - Getting All Previous Bins For TPO ID: " + TPOID);

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.GetAvailableTPOBins(TPOID)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try {

                                        String result = s.string();

                                        Logger.Debug("TPO", "GetPreviousBinsForThisTPO - Received Available TPO Bins For TPOID: " + TPOID + " " + result);

                                        /* We Will Get The Array As Json And Convert It To An Array List */
                                        TPOAvailableBinModel[] models = new Gson().fromJson(result, TPOAvailableBinModel[].class);

                                        mainProgressDialog.cancel();

                                        //Add the bins to the adapter
                                        for(TPOAvailableBinModel binModel : models){
                                            dataModels.add(new TPOItemsDialogDataModel(binModel.getBinBarcode()));
                                        }

                                        adapter.notifyDataSetChanged();

                                    }catch(Exception ex){
                                        mainProgressDialog.cancel();
                                        Logger.Error("JSON", "GetPreviousBinsForThisTPO - Error: " + ex.getMessage());
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
                                    Logger.Debug("TPO", "GetPreviousBinsForThisTPO - Returned Error: " + response);
                                    mainProgressDialog.cancel();
                                    ShowSnackbar(response);
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "GetPreviousBinsForThisTPO - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());

                                    mainProgressDialog.cancel();
                                    ShowErrorDialog(response);
                                }
                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();
            Logger.Error("API", "GetPreviousBinsForThisTPO - Error Connecting: " + e.getMessage());
            ShowSnackbar("Connection To Server Failed!");
            General.playError();
        }
    }

    /**
     * This function attempts to add a bin barcode to the list after communicating with the background
     * @param barcode
     */
    public void AddBinItem(String barcode){
        if(isBusy)
            return;

        if(DoesBinBarcodeExists(barcode)){
            ShowSnackbar("Barcode Already Exists!");
            General.playError();
            return;
        }

        isBusy = true;

        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Adding Bin #" + barcode + " To TPO, Please wait...", true);
        mainProgressDialog.show();

        Logger.Debug("TPO", "AddBinItem - Adding Bin #" + barcode + " To TPO ID: " + TPOID);

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.VerifyTPOBinAdd(TPOID, barcode, UserID)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try {

                                        String result = s.string();

                                        Logger.Debug("TPO", "AddBinItem - Received Result: " + result);


                                        mainProgressDialog.cancel();

                                        dataModels.add(new TPOItemsDialogDataModel(barcode));

                                        adapter.notifyDataSetChanged();

                                        ShowSnackbar(result);
                                        General.playSuccess();

                                    }catch(Exception ex){
                                        mainProgressDialog.cancel();
                                        Logger.Error("JSON", "AddBinItem - Error: " + ex.getMessage());
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
                                    Logger.Debug("TPO", "AddBinItem - Returned Error: " + response);
                                    mainProgressDialog.cancel();
                                    ShowSnackbar(response);
                                    General.playError();
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "AddBinItem - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                    mainProgressDialog.cancel();
                                    ShowErrorDialog(response);
                                }
                                isBusy = false;
                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();
            Logger.Error("API", "AddBinItem - Error Connecting: " + e.getMessage());
            ShowErrorDialog("Connection To Server Failed!");
            isBusy = false;
        }
    }

    /**
     * This function attempts to remove a bin item from the list after communicating with the background
     * @param barcode
     */
    public void RemoveBinItem(String barcode){
        if(isBusy)
            return;

        if(!DoesBinBarcodeExists(barcode)){
            ShowSnackbar("Barcode Doesn't Exists!");
            General.playError();
            return;
        }

        isBusy = true;

        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Removing Bin #" + barcode + " From TPO, Please wait...", true);
        mainProgressDialog.show();

        Logger.Debug("TPO", "RemoveBinItem - Removing Bin #" + barcode + " From TPO ID: " + TPOID);

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.RemoveBinFromTPO(TPOID, barcode)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try {

                                        String result = s.string();

                                        Logger.Debug("TPO", "RemoveBinItem - Received Result: " + result);


                                        mainProgressDialog.cancel();

                                        for(TPOItemsDialogDataModel model : dataModels.toArray(new TPOItemsDialogDataModel[0])){
                                            if(model.getMessage().equalsIgnoreCase(barcode)){
                                                dataModels.remove(model);
                                            }
                                        }

                                        adapter.notifyDataSetChanged();

                                        ShowSnackbar(result);
                                        General.playSuccess();

                                    }catch(Exception ex){
                                        mainProgressDialog.cancel();
                                        Logger.Error("JSON", "RemoveBinItem - Error: " + ex.getMessage());
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
                                    Logger.Debug("TPO", "RemoveBinItem - Returned Error: " + response);
                                    mainProgressDialog.cancel();
                                    ShowSnackbar(response);
                                    General.playError();
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "RemoveBinItem - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                    mainProgressDialog.cancel();
                                    ShowErrorDialog(response);
                                }
                                isBusy = false;
                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();
            Logger.Error("API", "RemoveBinItem - Error Connecting: " + e.getMessage());
            ShowErrorDialog("Connection To Server Failed!");
            isBusy = false;
        }
    }

    /**
     * This Functions verifies all the boxes and their item count then allows them for shipment
     */
    public void ReadyTPOBins(){
        if(isBusy)
            return;

        if(dataModels.isEmpty()){
            ShowSnackbar("This TPO Contains No Bins, You Can't Start The Shipment Yet!");
            General.playError();
            return;
        }

        isBusy = true;

        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Verifying All The TPO Bins, Please wait...", true);
        mainProgressDialog.show();

        Logger.Debug("TPO", "ReadyTPOBins - Verifying TPO Bins For TPO ID: " + TPOID);

        try {
            BasicApi api = APIClient.getNewInstanceStatic(IPAddress,120).create(BasicApi.class);//Create A Timeout Instance Of 60 Seconds
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.ReadyTPOBins(TPOID, UserID)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try {

                                        String result = s.string();

                                        Logger.Debug("TPO", "ReadyTPOBins - Received Result: " + result);


                                        mainProgressDialog.cancel();

                                        ShowSnackbar(result);
                                        General.playSuccess();

                                    }catch(Exception ex){
                                        mainProgressDialog.cancel();
                                        Logger.Error("JSON", "ReadyTPOBins - Error: " + ex.getMessage());
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
                                    Logger.Debug("TPO", "ReadyTPOBins - Returned Error: " + response);
                                    mainProgressDialog.cancel();
                                    ShowErrorDialog(response);
                                    General.playError();
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "ReadyTPOBins - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                    mainProgressDialog.cancel();
                                    ShowErrorDialog(response);
                                }
                                isBusy = false;
                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();
            Logger.Error("API", "ReadyTPOBins - Error Connecting: " + e.getMessage());
            ShowErrorDialog("Connection To Server Failed!");
            isBusy = false;
        }
    }

    /**
     * This function Checks if a bin barcode is in the list of our data models currently
     * @param barcode
     * @return
     */
    public boolean DoesBinBarcodeExists(String barcode){
        for(TPOItemsDialogDataModel model : dataModels.toArray(new TPOItemsDialogDataModel[0])){
            if(model.getMessage().equalsIgnoreCase(barcode)){
                return true;
            }
        }
        return false;
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
        Snackbar.make(findViewById(R.id.tpoBinsActivityLayout), message, Snackbar.LENGTH_LONG)
                .setAction("No action", null).show();
    }

}