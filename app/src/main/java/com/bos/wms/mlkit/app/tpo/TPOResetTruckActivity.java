package com.bos.wms.mlkit.app.tpo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.app.Logger;
import com.bos.wms.mlkit.app.adapters.UPCScannedItemDataModel;
import com.bos.wms.mlkit.storage.Storage;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

import java.util.List;

import Remote.APIClient;
import Remote.BasicApi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class TPOResetTruckActivity extends AppCompatActivity {

    EditText insertBinBoxBarcode;

    Button scannedItemsCount;

    public String IPAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tporeset_truck);

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");


        insertBinBoxBarcode = findViewById(R.id.insertBinBoxBarcode);
        scannedItemsCount = findViewById(R.id.scannedItemsCount);

        /**
         * This is used to always keep focus on the edit text so we can detect its text change
         * The text changes only when the user uses the scan method on the device and the text is pasted not typed
         */
        insertBinBoxBarcode.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                insertBinBoxBarcode.requestFocus();
            }
        });

        /**
         * This functions blocks the keyboard from poping up incase the uses presses on the edit text
         */
        insertBinBoxBarcode.setOnClickListener((click) -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(insertBinBoxBarcode.getWindowToken(), 0);
        });


        /**
         * Check for when the text of the edit is changed and add the pasted text to the scanned barcodes
         */
        insertBinBoxBarcode.addTextChangedListener(new TextWatcher() {

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
                    insertBinBoxBarcode.removeTextChangedListener(this);
                    insertBinBoxBarcode.setText(" ");
                    insertBinBoxBarcode.addTextChangedListener(this);

                    ProcessTruckBarCode(s.toString().replaceAll(" ", ""));
                }else if(s.length() != 0 && !s.toString().isEmpty()){;
                    insertBinBoxBarcode.removeTextChangedListener(this);
                    insertBinBoxBarcode.setText(" ");
                    insertBinBoxBarcode.addTextChangedListener(this);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(insertBinBoxBarcode.getWindowToken(), 0);
                }
            }
        });

        insertBinBoxBarcode.requestFocus();

    }

    /**
     * This function resets the truck count mode for the current barcode
     * @param barcode
     */
    public void ProcessTruckBarCode(String barcode){
        try {

            Logger.Debug("API", "ProcessTruckBarCode - Start Process For Truck, Barcode '" + barcode + "'");

            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();

            compositeDisposable.addAll(
                    api.ResetTruckBinCountMode(barcode)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    String result = s.string();
                                    Logger.Debug("API", "ProcessTruckBarCode - Received Result For Barcode '" + barcode + "', " + result);
                                    General.playSuccess();
                                    ShowSnackbar(result);
                                    AddScannedItem(barcode);
                                }
                            }, (throwable) -> {
                                String error = throwable.toString();
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    error = ex.response().errorBody().string();
                                    if(error.isEmpty()) error = throwable.getMessage();
                                    Logger.Debug("API", "ProcessTruckBarCode - Error In HTTP Response: " + error);
                                    General.playError();
                                    ShowSnackbar(error);
                                }else {
                                    Logger.Error("API", "ProcessTruckBarCode - Error In API Response: " + throwable.getMessage());
                                    General.playError();
                                    ShowErrorDialog(error);
                                }

                            }));

        } catch (Throwable e) {
            Logger.Error("API", "ProcessTruckBarCode - Error Connecting: " + e.getMessage());
            General.playError();
            ShowSnackbar("Connection To Server Failed!");
        }
    }

    /**
     * This function sets the scanned item text and creates an animation
     */
    public void AddScannedItem(String BinBarcode){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ObjectAnimator animate = ObjectAnimator.ofPropertyValuesHolder(scannedItemsCount,
                        PropertyValuesHolder.ofFloat("scaleX", 1.1f),
                        PropertyValuesHolder.ofFloat("scaleY", 1.1f));
                animate.setDuration(200);
                animate.setRepeatCount(ObjectAnimator.RESTART);
                animate.setRepeatMode(ObjectAnimator.REVERSE);
                animate.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(@NonNull Animator animator) {

                    }

                    @Override
                    public void onAnimationEnd(@NonNull Animator animator) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                scannedItemsCount.setText(BinBarcode + " Truck Reset Successfully");
                            }
                        });
                    }

                    @Override
                    public void onAnimationCancel(@NonNull Animator animator) {

                    }

                    @Override
                    public void onAnimationRepeat(@NonNull Animator animator) {

                    }
                });
                animate.start();
            }
        });
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
        Snackbar.make(findViewById(R.id.tpoResetTruckModeLayout), message, Snackbar.LENGTH_LONG)
                .setAction("No action", null).show();
    }

}