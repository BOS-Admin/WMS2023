package com.bos.wms.mlkit.app;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.app.Utils.ActivityActionsResultState;
import com.bos.wms.mlkit.app.Utils.AppHelperActivity;
import com.bos.wms.mlkit.app.Utils.BarcodeScannedListener;
import com.bos.wms.mlkit.storage.Storage;
import com.google.android.material.snackbar.Snackbar;

import Remote.APIClient;
import Remote.BasicApi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class PalletBinAssociationActivity extends AppHelperActivity {


    String IPAddress = "";

    ProgressDialog mainProgressDialog;

    Button scannedPallet, scannedBag, btnResult;

    String CurrentScannedPallet = null;

    String CurrentScannedBag = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pallet_bin_association);
        SetActivityMainView(findViewById(R.id.palletBinAssociationActivity));

        Storage mStorage = new Storage(this);

        IPAddress = mStorage.getDataString("IPAddress", "192.168.50.20");


        scannedPallet = findViewById(R.id.scannedPallet);
        scannedBag = findViewById(R.id.scannedBag);

        btnResult = findViewById(R.id.btnResult);
        SetActivityResultButton(btnResult);


        scannedPallet.setOnClickListener(view -> {
            scannedPallet.setText("Waiting For Pallet");
            CurrentScannedPallet = null;
        });

        scannedBag.setOnClickListener(view -> {
            scannedBag.setText("Waiting For Bag");
            CurrentScannedBag = null;
        });

        CreateBarcodeScanner(findViewById(R.id.insertBarcode), new BarcodeScannedListener() {
            @Override
            public void onBarcodeScanned(String barcode) {
                ProcessDetectedBarcode(barcode);
            }
        });

        setTitle("Internal Pallet Bin Association");
    }


    /**
     * Processes A Detected Barcode
     * @param barcode
     */
    public void ProcessDetectedBarcode(String barcode){

        if(barcode.startsWith("201") && CurrentScannedPallet == null) {
            CurrentScannedPallet = barcode;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    scannedPallet.setText("Detected Pallet " + CurrentScannedPallet);
                }
            });

            if(CurrentScannedPallet != null && CurrentScannedBag != null){
                ProcessInputComplete();
            }else {
                ShowActivityResultButton(null, ActivityActionsResultState.None);
            }

        }else if(barcode.startsWith("2000") && CurrentScannedBag == null){
            CurrentScannedBag = barcode;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    scannedBag.setText("Detected Bag " + CurrentScannedBag);
                }
            });

            if(CurrentScannedPallet != null && CurrentScannedBag != null){
                ProcessInputComplete();
            }else {
                ShowActivityResultButton(null, ActivityActionsResultState.None);
            }

        }

    }

    public void ProcessInputComplete(){
        ProcessAssociation(CurrentScannedPallet, CurrentScannedBag);

        scannedPallet.setText("Waiting For Pallet");
        CurrentScannedPallet = null;

        scannedBag.setText("Waiting For Bag");
        CurrentScannedBag = null;
    }

    public void ProcessAssociation(String pallet, String bag){

        if(pallet != null && bag != null){
            mainProgressDialog = ProgressDialog.show(this, "", "Associating Bin Please Wait...", true);

            try {

                Logger.Debug("API", "ProcessAssociation - Process Association For Pallet '" + pallet + "' And Bin '" + bag + "'");

                BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
                CompositeDisposable compositeDisposable = new CompositeDisposable();

                compositeDisposable.addAll(
                        api.PalletAssociateBin(pallet, bag, General.getGeneral(this).mainLocationID, General.getGeneral(this).UserID)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe((s) -> {
                                    if(s != null){
                                        String result = s.string();
                                        Logger.Debug("API", "ProcessAssociation - Returned: " + result);
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
                                        Logger.Debug("API", "ProcessAssociation - Error In HTTP Response: " + error);
                                    }else {
                                        Logger.Error("API", "ProcessAssociation - Error In API Response: " + throwable.getMessage());
                                    }

                                    ShowActivityResultButton(error, ActivityActionsResultState.Error);
                                    mainProgressDialog.cancel();
                                    PlayErrorSound();
                                }));


            }catch (Exception ex){
                ShowActivityResultButton("Internal Error!", ActivityActionsResultState.Error);
                PlayErrorSound();
                ShowAlertDialog("Error", ex.toString());
                Logger.Error("API", "ProcessAssociation - Error Connecting: " + ex.getMessage());
                mainProgressDialog.cancel();
            }

        }else {
            PlayErrorSound();
            ShowActivityResultButton("Invalid Input!", ActivityActionsResultState.Error);
        }
    }

}