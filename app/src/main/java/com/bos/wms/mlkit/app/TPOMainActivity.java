package com.bos.wms.mlkit.app;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.app.adapters.ItemSerialScannedAdapter;
import com.bos.wms.mlkit.app.adapters.ItemSerialScannedDataModel;
import com.bos.wms.mlkit.app.adapters.TPOItemsDialogAdapter;
import com.bos.wms.mlkit.app.adapters.TPOItemsDialogDataModel;
import com.bos.wms.mlkit.storage.Storage;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tpomain);

        Button btnCreateNewTPO, btnModifyTPOBins, btnTPOReady, btnTPOShipment, btnTPOReceive;

        TextView tpoMenuTitle = findViewById(R.id.tpoMenuTitle);

        btnCreateNewTPO = findViewById(R.id.btnCreateNewTPO);
        btnModifyTPOBins = findViewById(R.id.btnModifyTPOBins);
        btnTPOReady = findViewById(R.id.btnTPOReady);
        btnTPOShipment = findViewById(R.id.btnTPOShipment);
        btnTPOReceive = findViewById(R.id.btnTPOReceive);

        /* Verify The Menu Permissions */
        UserPermissions.ValidatePermission("WMSApp.TPO.CreateTPO", btnCreateNewTPO);
        UserPermissions.ValidatePermission("WMSApp.TPO.ModifyBins", btnModifyTPOBins);
        UserPermissions.ValidatePermission("WMSApp.TPO.ReadyTPO", btnTPOReady);
        UserPermissions.ValidatePermission("WMSApp.TPO.CreateShipment", btnTPOShipment);
        UserPermissions.ValidatePermission("WMSApp.TPO.ReceiveShipment", btnTPOReceive);

        //Get The Current Location The Device
        currentLocation = General.getGeneral(getApplicationContext()).mainLocation;

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");

        tpoMenuTitle.setText("Current Location " + currentLocation);

        btnCreateNewTPO.setOnClickListener(v -> {

            OpenCreateTPODialog();
        });

    }

    public void OpenCreateTPODialog(){
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

                                        /* We Will Get The Array And Place It Into A List */
                                        TPOTransferLocation[] locations = new Gson().fromJson(result, TPOTransferLocation[].class);

                                        mainProgressDialog.cancel();


                                        Dialog dialog = new Dialog(TPOMainActivity.this);
                                        dialog.setContentView(R.layout.tpo_locations_dialog);
                                        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                        dialog.getWindow().getAttributes().windowAnimations = R.style.tpoDialogAnimation;

                                        ArrayList<TPOItemsDialogDataModel> dataModels = new ArrayList<>();

                                        for(TPOTransferLocation location : locations){
                                            dataModels.add(new TPOItemsDialogDataModel(location.getLocationCode()));
                                        }

                                        ListView itemsListView = dialog.findViewById(R.id.itemsListView);

                                        TPOItemsDialogAdapter itemsAdapter = new TPOItemsDialogAdapter(dataModels, this, itemsListView);
                                        itemsListView.setAdapter(itemsAdapter);

                                        itemsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                            @Override
                                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                                                TPOItemsDialogDataModel dataModel= dataModels.get(position);
                                                CreateNewTPO(dataModel.getLocation());
                                                dialog.cancel();
                                            }
                                        });

                                        dialog.show();
                                    }catch(Exception ex){
                                        Logger.Error("JSON", "OpenCreateTPODialog - Error: " + ex.getMessage());
                                    }
                                }
                            }, (throwable) -> {
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
                                new AlertDialog.Builder(this)
                                        .setTitle("Error")
                                        .setMessage(response)
                                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        })
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .show();

                            }));

        } catch (Throwable e) {
            mainProgressDialog.cancel();
            Logger.Error("API", "OpenCreateTPODialog - Error Connecting: " + e.getMessage());
            Snackbar.make(findViewById(R.id.tpoMainActivityLayout), "Connection To Server Failed!", Snackbar.LENGTH_LONG)
                    .setAction("No action", null).show();
        }

    }

    public void CreateNewTPO(String location){
        Logger.Debug("TPO", "CreateNewTPO - Attempting To Create A New TPO Heading From: " + currentLocation + " To: " + location);
    }

}