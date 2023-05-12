package com.bos.wms.mlkit.app.tpo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.app.Logger;
import com.bos.wms.mlkit.app.adapters.TPOItemsDialogAdapter;
import com.bos.wms.mlkit.app.adapters.TPOItemsDialogDataModel;
import com.bos.wms.mlkit.app.adapters.TPOItemsDialogTPOInfoDataModel;
import com.bos.wms.mlkit.storage.Storage;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.util.ArrayList;

import Model.TPO.TPOModel;
import Model.TPO.TPOReceivedBinModel;
import Model.TPO.TPOTransferLocation;
import Remote.APIClient;
import Remote.BasicApi;
import Remote.UserPermissions.UserPermissions;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class TPOMainActivity extends AppCompatActivity {

    String currentLocation = "";
    String IPAddress = "";

    int UserID = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tpomain);

        Button btnCreateNewTPO, btnModifyTPOBins, btnTPOReceive, btnLoadTPOBins, btnCountTPOBins, btnResetTruckCountMode;

        TextView tpoMenuTitle = findViewById(R.id.tpoMenuTitle);

        btnCreateNewTPO = findViewById(R.id.btnCreateNewTPO);
        btnModifyTPOBins = findViewById(R.id.btnModifyTPOBins);
        btnTPOReceive = findViewById(R.id.btnTPOReceive);
        btnLoadTPOBins = findViewById(R.id.btnLoadTPOBins);
        btnCountTPOBins = findViewById(R.id.btnCountTPOBins);
        btnResetTruckCountMode = findViewById(R.id.btnResetTruckCountMode);

        /* Verify The Menu Permissions */
        UserPermissions.ValidatePermission("WMSApp.TPO.CreateTPO", btnCreateNewTPO);
        UserPermissions.ValidatePermission("WMSApp.TPO.ModifyBins", btnModifyTPOBins);
        UserPermissions.ValidatePermission("WMSApp.TPO.ReceiveShipment", btnTPOReceive);
        UserPermissions.ValidatePermission("WMSApp.TPO.LoadBins", btnLoadTPOBins);
        UserPermissions.ValidatePermission("WMSApp.TPO.CountBins", btnCountTPOBins);
        UserPermissions.ValidatePermission("WMSApp.TPO.ResetTruckCount", btnResetTruckCountMode);

        //Get The Current Location The Device
        currentLocation = General.getGeneral(getApplicationContext()).mainLocation;
        //currentLocation = "W1005";

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
        UserID = General.getGeneral(getApplicationContext()).UserID;

        tpoMenuTitle.setText("Current Location " + currentLocation);

        btnCreateNewTPO.setOnClickListener(v -> {

            OpenCreateTPODialog();
        });

        btnModifyTPOBins.setOnClickListener(v -> {

            OpenSelectTPODialogForBins();
        });

        btnLoadTPOBins.setOnClickListener(v -> {
            OpenSelectTPODialogForLoadBins();
        });

        btnCountTPOBins.setOnClickListener(v -> {
            OpenSelectTPODialogForCountBins();
        });

        btnTPOReceive.setOnClickListener(v -> {
            AttemptTPOReceiveGetInfo();
        });

        ValidateAuthToken();

    }

    /**
     * This Function Will Get All The Available Receiving Locations To Select From And Create A New TPO To
     */
    public void OpenCreateTPODialog(){
        //Create A Progress Dialog
        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Retrieving Locations, Please wait...", true);
        mainProgressDialog.show();

        Logger.Debug("TPO", "OpenCreateTPODialog - Retrieving Sending Locations From: " + currentLocation);

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.GetAllTransferLocations(currentLocation)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try {

                                        String result = s.string();

                                        Logger.Debug("TPO", "OpenCreateTPODialog - Received Transfer Locations: " + result);

                                        /* We Will Get The Array As Json And Convert It To An Array List */
                                        TPOTransferLocation[] locations = new Gson().fromJson(result, TPOTransferLocation[].class);

                                        mainProgressDialog.cancel();

                                        /* We Will Then Show A Dialog With A Custom Layout To Show The Available Locations */
                                        Dialog dialog = new Dialog(TPOMainActivity.this);
                                        dialog.setContentView(R.layout.tpo_locations_dialog);
                                        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                        dialog.getWindow().getAttributes().windowAnimations = R.style.tpoDialogAnimation;

                                        /* Transfer The Location Response Models To Layout Data Models */
                                        ArrayList<TPOItemsDialogDataModel> dataModels = new ArrayList<>();
                                        for(TPOTransferLocation location : locations){
                                            dataModels.add(new TPOItemsDialogDataModel(location.getLocationCode()));
                                        }

                                        ListView itemsListView = dialog.findViewById(R.id.itemsListView);

                                        TextView itemsDialogTitle = dialog.findViewById(R.id.itemsMenuTitle);

                                        itemsDialogTitle.setText("TPO Heading To");

                                        /* Create A Custom Adapter For The Location Items */
                                        TPOItemsDialogAdapter itemsAdapter = new TPOItemsDialogAdapter(dataModels, this, itemsListView);
                                        itemsListView.setAdapter(itemsAdapter);

                                        itemsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                            @Override
                                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                                                /* Once A Location Is Clicked Attempt To Create A New TPO Heading To That Location */
                                                TPOItemsDialogDataModel dataModel= dataModels.get(position);
                                                CreateNewTPO(dataModel.getMessage());
                                                dialog.cancel();
                                            }
                                        });

                                        dialog.show();
                                    }catch(Exception ex){
                                        Logger.Error("JSON", "OpenCreateTPODialog - Error: " + ex.getMessage());
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
                                    Logger.Debug("TPO", "OpenCreateTPODialog - Returned Error: " + response);
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "OpenCreateTPODialog - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                }

                                mainProgressDialog.cancel();

                                ShowErrorDialog(response);

                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();
            Logger.Error("API", "OpenCreateTPODialog - Error Connecting: " + e.getMessage());
            ShowSnackbar("Connection To Server Failed!");
            General.playError();
        }

    }

    /**
     * This Function Will Attempt To Create A TPO From The Current Location Heading To The Location Specified By The User
     * @param location
     */
    public void CreateNewTPO(String location){
        Logger.Debug("TPO", "CreateNewTPO - Attempting To Create A New TPO Heading From: " + currentLocation + " To: " + location);

        //Show A Progress Dialog
        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Creating TPO Heading To " + location + ", Please wait...", true);
        mainProgressDialog.show();

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.CreateTPO(currentLocation, location, UserID)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    String result = s.string();

                                    mainProgressDialog.cancel();

                                    Logger.Debug("TPO", "CreateNewTPO - Received Result: " + result);

                                    ShowSnackbar(result == null ? "API Returned Success, But Empty Result" : result);
                                    General.playSuccess();
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
                                    Logger.Debug("TPO", "CreateNewTPO - Returned Error: " + response);
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "CreateNewTPO - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                }

                                mainProgressDialog.cancel();

                                ShowErrorDialog(response);

                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();

            Logger.Error("API", "OpenCreateTPODialog - Error Connecting: " + e.getMessage());
            ShowErrorDialog("Connection To Server Failed!");
        }
    }

    /**
     * This Function Will Get All The Available Sending TPOS That We Can Currently Modify The Bins Of
     */
    public void OpenSelectTPODialogForBins(){
        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Retrieving TPOS, Please wait...", true);
        mainProgressDialog.show();

        Logger.Debug("TPO", "OpenSelectTPODialogForBins - Retrieving Sending TPOS From: " + currentLocation);

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.GetAllAvailableSendingTPOS(currentLocation)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try {

                                        String result = s.string();

                                        Logger.Debug("TPO", "OpenSelectTPODialogForBins - Received Sending TPOS: " + result);

                                        /* We Will Get The Array As Json And Convert It To An Array List */
                                        TPOModel[] locations = new Gson().fromJson(result, TPOModel[].class);

                                        mainProgressDialog.cancel();

                                        /* We Will Then Show A Dialog With A Custom Layout To Show The Available Locations */
                                        Dialog dialog = new Dialog(TPOMainActivity.this);
                                        dialog.setContentView(R.layout.tpo_locations_dialog);
                                        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                        dialog.getWindow().getAttributes().windowAnimations = R.style.tpoDialogAnimation;

                                        /* Transfer The Location Response Models To Layout Data Models */
                                        ArrayList<TPOItemsDialogDataModel> dataModels = new ArrayList<>();
                                        for(TPOModel location : locations){
                                            dataModels.add(new TPOItemsDialogTPOInfoDataModel(location.getId(), location.getToLocation(), "ID: " + location.getId() + ", " + location.getToLocation()));
                                        }

                                        ListView itemsListView = dialog.findViewById(R.id.itemsListView);

                                        TextView itemsDialogTitle = dialog.findViewById(R.id.itemsMenuTitle);

                                        itemsDialogTitle.setText("Select TPO");

                                        /* Create A Custom Adapter For The Location Items */
                                        TPOItemsDialogAdapter itemsAdapter = new TPOItemsDialogAdapter(dataModels, this, itemsListView);
                                        itemsListView.setAdapter(itemsAdapter);

                                        itemsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                            @Override
                                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                                                /* Once A Location Is Clicked Attempt To Create A New TPO Heading To That Location */
                                                TPOItemsDialogTPOInfoDataModel dataModel = (TPOItemsDialogTPOInfoDataModel)dataModels.get(position);
                                                OpenModifyTPOBinsActivity(dataModel.getId(), dataModel.getToLocation());
                                                dialog.cancel();
                                            }
                                        });

                                        dialog.show();
                                    }catch(Exception ex){
                                        mainProgressDialog.cancel();
                                        Logger.Error("JSON", "OpenSelectTPODialogForBins - Error: " + ex.getMessage());
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
                                    Logger.Debug("TPO", "OpenSelectTPODialogForBins - Returned Error: " + response);
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "OpenSelectTPODialogForBins - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                }

                                mainProgressDialog.cancel();

                                ShowErrorDialog(response);

                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();
            Logger.Error("API", "OpenSelectTPODialogForBins - Error Connecting: " + e.getMessage());
            ShowErrorDialog("Connection To Server Failed!");
        }
    }

    /**
     * This Function Will Get All The Available Sending TPOS That We Can Currently Modify The Bins Of
     */
    public void OpenSelectTPODialogForLoadBins(){
        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Retrieving TPOS, Please wait...", true);
        mainProgressDialog.show();

        Logger.Debug("TPO", "OpenSelectTPODialogForLoadBins - Retrieving Sending TPOS From: " + currentLocation);

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.GetAllTruckSendingTPOS(currentLocation)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try {

                                        String result = s.string();

                                        Logger.Debug("TPO", "OpenSelectTPODialogForLoadBins - Received Sending TPOS: " + result);

                                        /* We Will Get The Array As Json And Convert It To An Array List */
                                        TPOModel[] locations = new Gson().fromJson(result, TPOModel[].class);

                                        mainProgressDialog.cancel();

                                        /* We Will Then Show A Dialog With A Custom Layout To Show The Available Locations */
                                        Dialog dialog = new Dialog(TPOMainActivity.this);
                                        dialog.setContentView(R.layout.tpo_locations_dialog);
                                        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                        dialog.getWindow().getAttributes().windowAnimations = R.style.tpoDialogAnimation;

                                        /* Transfer The Location Response Models To Layout Data Models */
                                        ArrayList<TPOItemsDialogDataModel> dataModels = new ArrayList<>();
                                        for(TPOModel location : locations){
                                            TPOItemsDialogTPOInfoDataModel model = new TPOItemsDialogTPOInfoDataModel(location.getId(), location.getToLocation(), "ID: " + location.getId() + ", " + location.getToLocation());
                                            model.setDateCreated(location.getDateCreated());
                                            dataModels.add(model);
                                        }

                                        ListView itemsListView = dialog.findViewById(R.id.itemsListView);

                                        TextView itemsDialogTitle = dialog.findViewById(R.id.itemsMenuTitle);

                                        itemsDialogTitle.setText("Select TPO");

                                        /* Create A Custom Adapter For The Location Items */
                                        TPOItemsDialogAdapter itemsAdapter = new TPOItemsDialogAdapter(dataModels, this, itemsListView);
                                        itemsListView.setAdapter(itemsAdapter);

                                        itemsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                            @Override
                                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                                                /* Once A Location Is Clicked Attempt To Create A New TPO Heading To That Location */
                                                TPOItemsDialogTPOInfoDataModel dataModel = (TPOItemsDialogTPOInfoDataModel)dataModels.get(position);
                                                OpenLoadTPOBinsActivity(dataModel.getId(), dataModel.getToLocation(), dataModel.getDateCreated());
                                                dialog.cancel();
                                            }
                                        });

                                        dialog.show();
                                    }catch(Exception ex){
                                        mainProgressDialog.cancel();
                                        Logger.Error("JSON", "OpenSelectTPODialogForLoadBins - Error: " + ex.getMessage());
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
                                    Logger.Debug("TPO", "OpenSelectTPODialogForLoadBins - Returned Error: " + response);
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "OpenSelectTPODialogForLoadBins - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                }

                                mainProgressDialog.cancel();

                                ShowErrorDialog(response);

                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();
            Logger.Error("API", "OpenSelectTPODialogForBins - Error Connecting: " + e.getMessage());
            ShowErrorDialog("Connection To Server Failed!");
        }
    }

    /**
     * This Function Will Get All The Available Sending TPOS That We Can Currently Modify The Bins Of
     */
    public void OpenSelectTPODialogForCountBins(){
        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Retrieving TPOS, Please wait...", true);
        mainProgressDialog.show();

        Logger.Debug("TPO", "OpenSelectTPODialogForCountBins - Retrieving Sending TPOS From: " + currentLocation);

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.GetAllTruckSendingTPOS(currentLocation)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try {

                                        String result = s.string();

                                        Logger.Debug("TPO", "OpenSelectTPODialogForCountBins - Received Sending TPOS: " + result);

                                        /* We Will Get The Array As Json And Convert It To An Array List */
                                        TPOModel[] locations = new Gson().fromJson(result, TPOModel[].class);

                                        mainProgressDialog.cancel();

                                        /* We Will Then Show A Dialog With A Custom Layout To Show The Available Locations */
                                        Dialog dialog = new Dialog(TPOMainActivity.this);
                                        dialog.setContentView(R.layout.tpo_locations_dialog);
                                        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                        dialog.getWindow().getAttributes().windowAnimations = R.style.tpoDialogAnimation;

                                        /* Transfer The Location Response Models To Layout Data Models */
                                        ArrayList<TPOItemsDialogDataModel> dataModels = new ArrayList<>();
                                        for(TPOModel location : locations){
                                            TPOItemsDialogTPOInfoDataModel model = new TPOItemsDialogTPOInfoDataModel(location.getId(), location.getToLocation(), "ID: " + location.getId() + ", " + location.getToLocation());
                                            model.setDateCreated(location.getDateCreated());
                                            dataModels.add(model);
                                        }

                                        ListView itemsListView = dialog.findViewById(R.id.itemsListView);

                                        TextView itemsDialogTitle = dialog.findViewById(R.id.itemsMenuTitle);

                                        itemsDialogTitle.setText("Select TPO");

                                        /* Create A Custom Adapter For The Location Items */
                                        TPOItemsDialogAdapter itemsAdapter = new TPOItemsDialogAdapter(dataModels, this, itemsListView);
                                        itemsListView.setAdapter(itemsAdapter);

                                        itemsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                            @Override
                                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                                                /* Once A Location Is Clicked Attempt To Create A New TPO Heading To That Location */
                                                TPOItemsDialogTPOInfoDataModel dataModel = (TPOItemsDialogTPOInfoDataModel)dataModels.get(position);
                                                OpenCountTPOBinsActivity(dataModel.getId(), dataModel.getToLocation(), dataModel.getDateCreated());
                                                dialog.cancel();
                                            }
                                        });

                                        dialog.show();
                                    }catch(Exception ex){
                                        mainProgressDialog.cancel();
                                        Logger.Error("JSON", "OpenSelectTPODialogForCountBins - Error: " + ex.getMessage());
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
                                    Logger.Debug("TPO", "OpenSelectTPODialogForCountBins - Returned Error: " + response);
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "OpenSelectTPODialogForCountBins - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                }

                                mainProgressDialog.cancel();

                                ShowErrorDialog(response);

                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();
            Logger.Error("API", "OpenSelectTPODialogForCountBins - Error Connecting: " + e.getMessage());
            ShowErrorDialog("Connection To Server Failed!");
        }
    }

    /**
     * This function downloads all the available receiving tpos and their data
     */
    public void AttemptTPOReceiveGetInfo(){
        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Retrieving TPO Info, Please wait...", true);
        mainProgressDialog.show();

        Logger.Debug("TPO", "AttemptTPOReceiveGetInfo - Retrieving Receiving TPOS From: " + currentLocation);

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.GetAllAvailableReceivingTPOSData(currentLocation)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try {

                                        String result = s.string();

                                        Logger.Debug("TPO", "AttemptTPOReceiveGetInfo - Received TPO Data: " + result);

                                        /* We Will Get The Array As Json And Convert It To An Array List */
                                        TPOReceivedBinModel[] bins = new Gson().fromJson(result, TPOReceivedBinModel[].class);

                                        TPOReceivedInfo.ReceivedItems = new ArrayList<>();

                                        for(TPOReceivedBinModel model : bins){
                                            TPOReceivedInfo.ReceivedItems.add(model);
                                        }

                                        mainProgressDialog.cancel();

                                        OpenReceiveTPOActivity();


                                    }catch(Exception ex){
                                        mainProgressDialog.cancel();
                                        Logger.Error("JSON", "AttemptTPOReceiveGetInfo - Error: " + ex.getMessage());
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
                                    Logger.Debug("TPO", "AttemptTPOReceiveGetInfo - Returned Error: " + response);
                                }else {
                                    response = throwable.getMessage();
                                    Logger.Error("API", "AttemptTPOReceiveGetInfo - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                }

                                mainProgressDialog.cancel();

                                ShowErrorDialog(response);

                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();
            Logger.Error("API", "AttemptTPOReceiveGetInfo - Error Connecting: " + e.getMessage());
            ShowErrorDialog("Connection To Server Failed!");
        }
    }

    /**
     *
     */
    public void OpenReceiveTPOActivity(){
        Logger.Debug("TPO", "OpenReceiveTPOActivity - Opening Activity");

        Intent i = new Intent(this, TPOReceiveBinsActivity.class);
        startActivity(i);
    }

    /**
     * This Functions Opens The TPO Loading Bins Activity
     * @param id
     * @param location
     */
    public void OpenLoadTPOBinsActivity(int id, String location, String dateCreated){
        Logger.Debug("TPO", "OpenLoadTPOBinsActivity - Opening Activity For TPO ID: " + id + " ToLocation: " + location);

        Intent i = new Intent(this, TPOLoadBinsActivity.class);
        i.putExtra("TPOID", id);
        i.putExtra("ToLocation", location);
        i.putExtra("DateCreated", dateCreated);
        startActivity(i);
    }

    /**
     * This Functions Opens The TPO Counting Bins Activity
     * @param id
     * @param location
     */
    public void OpenCountTPOBinsActivity(int id, String location, String dateCreated){
        Logger.Debug("TPO", "OpenCountTPOBinsActivity - Opening Activity For TPO ID: " + id + " ToLocation: " + location);

        Intent i = new Intent(this, TPOCountBinsActivity.class);
        i.putExtra("TPOID", id);
        i.putExtra("ToLocation", location);
        i.putExtra("DateCreated", dateCreated);
        startActivity(i);
    }

    /**
     * This Functions Opens The TPO Bins Modifications Activity
     * @param id
     * @param location
     */
    public void OpenModifyTPOBinsActivity(int id, String location){
        Logger.Debug("TPO", "OpenModifyTPOBinsActivity - Opening Activity For TPO ID: " + id + " ToLocation: " + location);

        Intent i = new Intent(this, TPOBinsActivity.class);
        i.putExtra("TPOID", id);
        startActivity(i);
    }

    /**
     * This Functions Sends A Request With The User's Authentication Token In The Header And Validates It
     */
    public void ValidateAuthToken(){
        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.ValidateAuthToken()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    Logger.Debug("API", "ValidateAuthToken - " + s.string());

                                }
                            }, (throwable) -> {
                                String response = "";
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    response = ex.response().errorBody().string();
                                    if(response.isEmpty()){
                                        response = throwable.getMessage();
                                    }
                                }else {
                                    response = throwable.getMessage();
                                }
                                Logger.Error("API", "ValidateAuthToken - Error In Response: " + response);
                            }));
        } catch (Throwable e) {
            Logger.Error("API", "ValidateAuthToken - Error Connecting: " + e.getMessage());
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
        Snackbar.make(findViewById(R.id.tpoMainActivityLayout), message, Snackbar.LENGTH_LONG)
                .setAction("No action", null).show();
    }

}