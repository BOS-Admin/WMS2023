package com.bos.wms.mlkit.app.bosApp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.app.Logger;
import com.bos.wms.mlkit.storage.Storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import Model.BosApp.PackReasonModelItem;
import Remote.APIClient;
import Remote.BasicApi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class NewStandSwitchActivity extends AppCompatActivity {
    Button btnResult;
    Spinner spinner;
    private int UserID = -1;
    Storage mStorage = null;
    private String IPAddress = "";
    ArrayList<PackReasonModelItem> reasonModelItemList;
    ArrayList<String> reasonsList;
    HashMap<Integer, String> reasonsHashMap = new HashMap<>();
    General general;
    ArrayAdapter<String> adapter;
    EditText edtStand, edtBox;
    String BoxStr, StandStr;
    int packReasonId, LocationId;
    boolean ableToUpdateStand = true, ableToUpdateBox = true;

    boolean boxScanned = false, standScanned = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_stand_switch);
        mStorage = new Storage(this);
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
        UserID = General.getGeneral(this).UserID;
        general = General.getGeneral(this);
        spinner = findViewById(R.id.spnReason);
        btnResult = findViewById(R.id.btnResult);
        edtStand = findViewById(R.id.edtStand);
        edtBox = findViewById(R.id.edtBox);
        LocationId = general.mainLocationID;
        GetPackReasons();


        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedReason = reasonsList.get(i);
                if (!selectedReason.equals("Choose a reason")) {
                    packReasonId = getIdFromName(selectedReason, reasonsHashMap);
                    edtBox.setEnabled(true);
                    edtStand.setEnabled(true);
                    edtStand.requestFocus();
                    setBtnResultState(5,"");

                } else {
                    packReasonId = -1;
                    edtBox.setEnabled(false);
                    edtStand.setEnabled(false);
                    //setBtnResultState(0,"Choose a valid Reason");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        edtStand.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //NA
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //NA
            }

            @Override
            public void afterTextChanged(Editable editable) {
                Logger.Debug("SwitchDebug", "Text Changed Stand");
                if (!ableToUpdateStand) return;

                StandStr = String.valueOf(edtStand.getText());
                if (!StandNbValidated(StandStr)) {
                    ResetStand();
                    setBtnResultState(0, "Scan a valid Stand");
                }
                validateBoxScanned(StandStr, new ApiCallback() {
                    @Override
                    public void onResultSuccess() {
                        standScanned = true;
                        if (boxScanned)
                            DoAction();
                        else
                            edtBox.requestFocus();

                        setBtnResultState(5,"");
                    }

                    @Override
                    public void onResultFailure(String errorMessage) {
                        ResetStand();
                        setBtnResultState(0, "Scan a valid Stand");

                    }
                });
            }
        });


        edtBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!ableToUpdateBox) return;


                BoxStr = String.valueOf(edtBox.getText());
                if (!BoxNbValidated(BoxStr)) {
                    ResetBox();
                    setBtnResultState(0, "Scan a valid Box");
                }
                validateBoxScanned(BoxStr, new ApiCallback() {
                    @Override
                    public void onResultSuccess() {
                        boxScanned = true;
                        if (standScanned)
                            DoAction();
                        else
                            edtStand.requestFocus();

                        setBtnResultState(5,"");
                    }

                    @Override
                    public void onResultFailure(String errorMessage) {
                        ResetBox();
                        setBtnResultState(0, "Scan a valid Box");
                    }
                });
            }
        });
    }

    /**
     * This method stores all pack reasons into a list from api
     */
    private void GetPackReasons() {
        try {

            Logger.Debug("SwitchDebug", "API -  GetPackReasons");

            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();

            compositeDisposable.addAll(
                    api.GetPackReasons()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {
                                    Logger.Debug("SwitchDebug", "pack reasons fetched successfully: " + s);
                                    reasonModelItemList = (ArrayList<PackReasonModelItem>) s;
                                    reasonsList = new ArrayList<>();
                                    Logger.Debug("SwitchDebug", "reason Model list:  " + reasonModelItemList.size());
                                    reasonsList.add("Choose a reason");
                                    for (PackReasonModelItem item : reasonModelItemList) {
                                        reasonsHashMap.put(item.getId(), item.getName());
                                        reasonsList.add(item.getName());
                                    }

                                    adapter = new ArrayAdapter<>(this, R.layout.spinner_item_layout, reasonsList);
                                    adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
                                    spinner.setAdapter(adapter);
                                    spinner.setSelection(0);
                                }
                            }, (throwable) -> {
                                String error = throwable.toString();
                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    error = ex.response().errorBody().string();
                                    if (error.isEmpty()) error = throwable.getMessage();
                                    Logger.Debug("API", "GetPackReasons - Error In HTTP Response: " + error);
                                    setBtnResultState(0, error);
                                } else {
                                    Logger.Error("API", "GetPackReasons - Error In API Response: " + throwable.getMessage());
                                    setBtnResultState(0, throwable.getMessage());
                                }
                            }));

        } catch (Throwable e) {
            Logger.Error("API", "GetPackReasons - Error Connecting: " + e.getMessage());
            setBtnResultState(0, e.getMessage());
        }
    }

    public int getIdFromName(String name, HashMap<Integer, String> packReasonMap) {
        int id = 0;
        for (Map.Entry<Integer, String> entry : packReasonMap.entrySet()) {
            if (entry.getValue().equals(name)) {
                id = entry.getKey();
            }
        }
        return id;
    }

    private void showAlertDialog(String msgBody) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Error")
                .setMessage(msgBody)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Handle positive button click
                        dialog.dismiss(); // Dismiss the dialog
                    }
                });
        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void validateBoxScanned(String box, ApiCallback callback) {
        Logger.Debug("SwitchDebug", "Validation Starts.. of box: " + box);

        try {

            Logger.Debug("SwitchDebug", "API -  GetPackReasons");
            Logger.Debug("SwitchDebug", "UserID: " + UserID);

            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();

            compositeDisposable.addAll(
                    api.ValidateBin(box, packReasonId, UserID, LocationId)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {
                                    Logger.Debug("SwitchDebug", "validation Api results got: " + s);
                                    callback.onResultSuccess();
                                }
                            }, (throwable) -> {
                                String error = throwable.toString();
                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    error = ex.response().errorBody().string();
                                    if (error.isEmpty()) error = throwable.getMessage();
                                    Logger.Debug("API", "GetPackReasons - Error In HTTP Response: " + error);
                                    callback.onResultFailure(error);
                                    setBtnResultState(0, "Scanning Box/Stand :" + error);
                                } else {
                                    Logger.Error("API", "GetPackReasons - Error In API Response: " + throwable.getMessage());
                                    setBtnResultState(0, throwable.getMessage());
                                    callback.onResultFailure(throwable.getMessage());
                                }
                            }));

        } catch (Throwable e) {
            Logger.Error("API", "GetPackReasons - Error Connecting: " + e.getMessage());
            setBtnResultState(0, e.getMessage());
            callback.onResultFailure(e.getMessage());
        }
    }

    private void setBtnResultState(int Case, String BodyMsg) {
        if (Case == 0) {
            btnResult.setText("Error");
            btnResult.setBackgroundColor(getResources().getColor(R.color.custom_red));
        } else if (Case == 1) {
         
                btnResult.setText("Success");
            btnResult.setBackgroundColor(getResources().getColor(R.color.custom_green));
        }else if( Case == 5){
            btnResult.setText("Result");
            btnResult.setBackgroundColor(getResources().getColor(R.color.black));
        }
        btnResult.setOnClickListener((click) -> {
            showAlertDialog(BodyMsg);
        });

    }

    /**
     * This method do the Switch Action from Stand to a Box
     */
    private void DoAction() {
        try {

            Logger.Debug("SwitchDebug", "API -  FillBinStockTake");

            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();

            compositeDisposable.addAll(
                    api.FillBinStockTake(UserID, packReasonId, StandStr, BoxStr, LocationId)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {
                                    setBtnResultState(1, "Stand has been switched successfully");
                                    RestartView();
                                }
                            }, (throwable) -> {
                                String error = throwable.toString();
                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    error = ex.response().errorBody().string();
                                    if (error.isEmpty()) error = throwable.getMessage();
                                    Logger.Debug("API", "FillBinStockTake - Error In HTTP Response: " + error);
                                    setBtnResultState(0, error);
                                    RestartView();
                                } else {
                                    Logger.Error("API", "FillBinStockTake - Error In API Response: " + throwable.getMessage());
                                    setBtnResultState(0, throwable.getMessage());
                                    RestartView();
                                }
                            }));

        } catch (Throwable e) {
            Logger.Error("API", "FillBinStockTake - Error Connecting: " + e.getMessage());
            setBtnResultState(0, e.getMessage());
            RestartView();
        }

    }



    private boolean BoxNbValidated(String box) {
        if (!General.ValidateBinCode(box)) {
            return false;
        }

        boolean success = false;
        for (String s : general.BoxNbDigits.split(",")) {
            if (box.length() == Integer.parseInt(s)) {
                success = true;
                break;
            }
        }

        if (success) {
            for (String s : general.BoxStartsWith.split(",")) {
                if (box.startsWith(s)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean StandNbValidated(String box) {
        if (!General.ValidateBinCode(box)) {
            return false;
        }

        boolean success = false;
        for (String s : general.StandNbDigits.split(",")) {
            if (box.length() == Integer.parseInt(s)) {
                success = true;
                break;
            }
        }

        if (success) {
            for (String s : general.StandStartsWith.split(",")) {
                if (box.startsWith(s)) {
                    return true;
                }
            }
        }

        return false;
    }


    private void RestartView(){
       ResetStand();
       ResetBox();
    }

    private void ResetStand(){
        ableToUpdateStand = false;
        standScanned = false;
        edtStand.setText("");
        ableToUpdateStand = true;
    }

    private void ResetBox(){
        ableToUpdateBox = false;
        boxScanned = false;
        edtBox.setText("");
        ableToUpdateBox = true;
    }

    public interface ApiCallback {
        void onResultSuccess();

        void onResultFailure(String errorMessage);
    }

}