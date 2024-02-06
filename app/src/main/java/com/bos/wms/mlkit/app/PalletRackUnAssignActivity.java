package com.bos.wms.mlkit.app;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Button;

import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.app.Utils.ActivityActionsResultState;
import com.bos.wms.mlkit.app.Utils.AppHelperActivity;
import com.bos.wms.mlkit.app.Utils.BarcodeScannedListener;
import com.bos.wms.mlkit.storage.Storage;

import Remote.APIClient;
import Remote.BasicApi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class PalletRackUnAssignActivity extends AppHelperActivity {

    String IPAddress = "";

    ProgressDialog mainProgressDialog;

    Button scannedPallet;

    Button btnResult;

    String CurrentScannedPallet = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pallet_rack_assignment);
        SetActivityMainView(findViewById(R.id.palletRackAssignmentActivity));

        Storage mStorage = new Storage(this);

        IPAddress = mStorage.getDataString("IPAddress", "192.168.50.20");


        scannedPallet = findViewById(R.id.scannedPallet);

        btnResult = findViewById(R.id.btnResult);
        SetActivityResultButton(btnResult);


        scannedPallet.setOnClickListener(view -> {
            scannedPallet.setText("Waiting For Pallet");
            CurrentScannedPallet = null;
        });

        CreateBarcodeScanner(findViewById(R.id.insertBarcode), new BarcodeScannedListener() {
            @Override
            public void onBarcodeScanned(String barcode) {
                ProcessDetectedBarcode(barcode);
            }
        });

        setTitle("Pallet Rack UnAssign");
    }

    /**
     * Processes A Detected Barcode
     * @param barcode
     */
    public void ProcessDetectedBarcode(String barcode){

        if(IsValidPutAwayBin(barcode) && CurrentScannedPallet == null) {
            CurrentScannedPallet = barcode;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    scannedPallet.setText("Detected Pallet " + CurrentScannedPallet);
                }
            });

            if(CurrentScannedPallet != null){
                ProcessInputComplete();
            }else {
                ShowActivityResultButton(null, ActivityActionsResultState.None);
            }

        }

    }

    public void ProcessInputComplete(){
        ProcessUnAssign(CurrentScannedPallet);

        scannedPallet.setText("Waiting For Pallet");
        CurrentScannedPallet = null;
    }

    public void ProcessUnAssign(String pallet){

        if(pallet != null){
            mainProgressDialog = ProgressDialog.show(this, "", "UnAssigning Bin Please Wait...", true);

            try {

                Logger.Debug("API", "ProcessUnAssign - Process UnAssign For Bin '" + pallet + "'");

                BasicApi api = APIClient.getNewInstanceStatic(IPAddress,60).create(BasicApi.class);
                CompositeDisposable compositeDisposable = new CompositeDisposable();

                compositeDisposable.addAll(
                        api.RemoveBinFromRack(pallet, General.getGeneral(this).UserID)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe((s) -> {
                                    if(s != null){
                                        String result = s.string();
                                        Logger.Debug("API", "ProcessUnAssign - Returned: " + result);
                                        ShowActivityResultButton(result, ActivityActionsResultState.Success);
                                        mainProgressDialog.cancel();
                                        PlaySuccessSound();
                                    }
                                }, (throwable) -> {
                                    String error = throwable.toString();
                                    if(throwable instanceof HttpException){
                                        HttpException ex = (HttpException) throwable;
                                        error = ex.response().errorBody().string();
                                        if(error.isEmpty()) error = throwable.getMessage();
                                        Logger.Debug("API", "ProcessUnAssign - Error In HTTP Response: " + error);
                                    }else {
                                        Logger.Error("API", "ProcessUnAssign - Error In API Response: " + throwable.getMessage());
                                    }

                                    ShowActivityResultButton(error, ActivityActionsResultState.Error);
                                    mainProgressDialog.cancel();
                                    PlayErrorSound();
                                }));


            }catch (Exception ex){
                ShowActivityResultButton("Internal Error!", ActivityActionsResultState.Error);
                PlayErrorSound();
                ShowAlertDialog("Error", ex.toString());
                Logger.Error("API", "ProcessUnAssign - Error Connecting: " + ex.getMessage());
                mainProgressDialog.cancel();
            }

        }else {
            PlayErrorSound();
            ShowActivityResultButton("Invalid Input!", ActivityActionsResultState.Error);
        }
    }

    public boolean IsValidPutAwayBin(String box){
        String validations = General.getGeneral(getApplication()).getSetting(getApplicationContext(),"PalletRackAssignPrefix");

        if(validations == null)
        {
            Logger.Error("SystemControl", "Failed Finding System Control Value With PalletRackAssignPrefix");
            return false;
        }

        String[] args = validations.split(",");

        if (args.length == 0)
            return box.startsWith(validations);

        for (String arg : args) {
            if (box.startsWith(arg))
                return true;
        }

        return false;

    }

}