package com.bos.wms.mlkit.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.bos.wms.mlkit.CustomListAdapter;
import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.app.adapters.ItemSerialScannedAdapter;
import com.bos.wms.mlkit.app.adapters.ItemSerialScannedDataModel;
import com.bos.wms.mlkit.app.adapters.UPCScannedAdapter;
import com.bos.wms.mlkit.app.adapters.UPCScannedItemDataModel;
import com.bos.wms.mlkit.storage.Storage;
import com.google.android.flexbox.FlexboxLayout;
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

public class UPCPricingActivity extends AppCompatActivity {

    EditText insertBarcode;

    String IPAddress = "", PricingLineCode = "";

    Button scannedItemUPC, scannedItemSerial;

    Button currentSelectedButton;

    String currentItemSerial = "", currentItemUPC = "";

    ArrayList<String> scannedItemSerials;

    ArrayList<ItemSerialScannedDataModel> dataModels;

    ItemSerialScannedAdapter scannedItemsAdapter;

    ListView scannedItemsListView;

    boolean isBusy = false;

    Button confirmBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upc_pricing);

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
        PricingLineCode = mStorage.getDataString("PricingLineCode", "PL001");

        insertBarcode = findViewById(R.id.insertBarcode);

        scannedItemSerials = new ArrayList<>();

        scannedItemSerial = findViewById(R.id.scannedItemSerial);
        scannedItemUPC = findViewById(R.id.scannedItemUPC);


        scannedItemsListView = findViewById(R.id.scannedItemsListView);
        dataModels = new ArrayList<>();
        scannedItemsAdapter = new ItemSerialScannedAdapter(dataModels, this, scannedItemsListView);

        /**
         * This event handler listens for when the user manually removes an item to remove it from the local list
         */
        scannedItemsAdapter.setItemSerialScannedListener((itemSerial, itemUPC) -> {
            if(scannedItemSerials.contains(itemSerial)){
                scannedItemSerials.remove(itemSerial);
            }
            if(scannedItemSerials.size() == 0){
                confirmBtn.setEnabled(false);
            }
        });

        scannedItemsListView.setAdapter(scannedItemsAdapter);

        confirmBtn = findViewById(R.id.confirmBtn);
        confirmBtn.setEnabled(false);

        confirmBtn.setOnClickListener(view -> {
            ProcessAllItems();
        });

        /**
         * This part helps identify which selection to paste the barcode scan to
         */
        currentSelectedButton = scannedItemSerial;

        scannedItemSerial.setOnClickListener(view -> {
            currentSelectedButton = scannedItemSerial;
            scannedItemSerial.setText("Scan An Item To Start");
        });

        scannedItemUPC.setOnClickListener(view -> {
            currentSelectedButton = scannedItemUPC;
            scannedItemUPC.setText("Scan An Item To Start");
        });

        /**
         * This is used to always keep focus on the edit text so we can detect its text change
         * The text changes only when the user uses the scan method on the device and the text is pasted not typed
         */
        insertBarcode.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                insertBarcode.requestFocus();
            }
        });

        /**
         * This functions blocks the keyboard from poping up incase the uses presses on the edit text
         */
        insertBarcode.setOnClickListener((click) -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(insertBarcode.getWindowToken(), 0);
        });


        /**
         * Check for when the text of the edit is changed and add the pasted text to the scanned barcodes
         */
        insertBarcode.addTextChangedListener(new TextWatcher() {

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
                    insertBarcode.removeTextChangedListener(this);
                    insertBarcode.setText(" ");
                    insertBarcode.addTextChangedListener(this);

                    ProcessBarCode(s.toString());
                }else if(s.length() != 0 && !s.toString().isEmpty()){;
                    insertBarcode.setText(" ");
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(insertBarcode.getWindowToken(), 0);
                }
            }
        });

        insertBarcode.requestFocus();

    }

    /**
     * This function processes a scanned barcode to place it or discard it
     * @param code
     */
    public void ProcessBarCode(String code){

        if(isBusy) return; //Make sure the api isnt busy processing the old item scan

        if(currentSelectedButton != null){
            code = code.replaceAll(" " , "");
            if(currentSelectedButton == scannedItemSerial){
                /**
                 * Check if the upc is valid
                 */
                if(General.ValidateItemSerialCode(code)){

                    /**
                     * Check if the item serial was already validated before
                     */
                    if(scannedItemSerials.contains(code)){

                        currentItemSerial = "";

                        scannedItemSerials.clear();
                        confirmBtn.setEnabled(false);

                        currentSelectedButton = scannedItemSerial;
                        SetScannedItemText(scannedItemUPC, "Scan An Item To Start");
                        SetScannedItemText(scannedItemSerial, "Scan An Item To Start");

                        dataModels.clear();
                        scannedItemsAdapter.notifyDataSetChanged();


                        Snackbar.make(findViewById(R.id.upcPricingActivityLayout), "Item Serial Already Added, Please Retry", Snackbar.LENGTH_SHORT)
                                .setAction("No action", null).show();

                        return;
                    }

                    currentItemSerial = code;
                    SetScannedItemText(scannedItemSerial, code);

                    /**
                     * Check if we can move to the next scan which is upc scanning
                     */
                    if(scannedItemUPC.getText().toString().equalsIgnoreCase("Scan An Item To Start")){
                        currentSelectedButton = scannedItemUPC;
                    }else {
                        /**
                         * Process the item serial and upc data we have
                         */
                        ProcessItem();
                    }
                }else {
                    Logger.Error("InvalidSerial", code);
                    Snackbar.make(findViewById(R.id.upcPricingActivityLayout), "Invalid Item Serial", Snackbar.LENGTH_SHORT)
                            .setAction("No action", null).show();
                }
            }else {
                /**
                 * Check if the upc is valid
                 */
                if(General.ValidateItemCode(code)){

                    currentItemUPC = code;
                    SetScannedItemText(scannedItemUPC, code);

                    /**
                     * Check if we can move to the next scan which is item serial scanning
                     */
                    if(scannedItemSerial.getText().toString().equalsIgnoreCase("Scan An Item To Start")){
                        currentSelectedButton = scannedItemSerial;
                    }else {
                        /**
                         * Process the item serial and upc data we have
                         */
                        ProcessItem();
                    }
                }else {
                    Snackbar.make(findViewById(R.id.upcPricingActivityLayout), "Invalid Item UPC", Snackbar.LENGTH_SHORT)
                            .setAction("No action", null).show();
                }
            }
        }
    }


    /**
     * This function is called once we have an item serial and item upc scanned and ready to validate
     */
    public void ProcessItem(){
        try {
            isBusy = true;
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();

            compositeDisposable.addAll(
                    api.ValidateUPCPricing(currentItemSerial, currentItemUPC)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    if(!s.isEmpty() && s.toLowerCase().startsWith("success")){

                                        Logger.Debug("API", "UPCPricing-ProcessItem Returned Result: " + s);

                                        General.playSuccess();

                                        SetScannedItemText(scannedItemUPC, "Scan An Item To Start");
                                        SetScannedItemText(scannedItemSerial, "Scan An Item To Start");

                                        currentSelectedButton = scannedItemSerial;

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                confirmBtn.setEnabled(true);
                                                scannedItemSerials.add(currentItemSerial);
                                                dataModels.add(new ItemSerialScannedDataModel(currentItemSerial, currentItemUPC));
                                                scannedItemsAdapter.notifyDataSetChanged();
                                            }
                                        });

                                    }
                                    else{

                                        Logger.Error("API", "UPCPricing-ProcessItem - Received Error: " + s);

                                        General.playError();

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Snackbar.make(findViewById(R.id.upcPricingActivityLayout), s, Snackbar.LENGTH_LONG)
                                                        .setAction("No action", null).show();
                                            }
                                        });

                                        if(s.toLowerCase().contains("upc")){
                                            SetScannedItemText(scannedItemUPC, "Scan An Item To Start");
                                            currentSelectedButton = scannedItemSerial;
                                        }
                                        else{
                                            SetScannedItemText(scannedItemUPC, "Scan An Item To Start");
                                            SetScannedItemText(scannedItemSerial, "Scan An Item To Start");
                                            currentSelectedButton = scannedItemSerial;
                                        }
                                    }
                                    isBusy = false;
                                }
                            }, (throwable) -> {
                                String error = throwable.toString();
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    error = ex.response().errorBody().string();
                                    if(error.isEmpty()) error = throwable.getMessage();
                                    Logger.Debug("API", "UPCPricing-ProcessItem - Error In HTTP Response: " + error);
                                }else {
                                    Logger.Error("API", "UPCPricing-ProcessItem - Error In API Response: " + throwable.getMessage());
                                }

                                isBusy = false;

                            }));

        } catch (Throwable e) {
            isBusy = false;
            Logger.Error("API", "UPCPricing-ProcessItem - Error Connecting: " + e.getMessage());
            Snackbar.make(findViewById(R.id.upcPricingActivityLayout), "Connection To Server Failed!", Snackbar.LENGTH_LONG)
                    .setAction("No action", null).show();
        }
    }

    /**
     * This function processes all items
     */
    public void ProcessAllItems(){
        try {
            isBusy = true;
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();

            int UserID = General.getGeneral(getApplicationContext()).UserID;

            ArrayList<String> allItemSerials = new ArrayList<>(), allUPCs = new ArrayList<>();

            for(ItemSerialScannedDataModel model : dataModels){
                allItemSerials.add(model.getItemSerial());
                allUPCs.add(model.getUPC());
            }


            compositeDisposable.addAll(
                    api.PostUPCPricing(UserID, String.join(",", allItemSerials), String.join(",", allUPCs), PricingLineCode)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    String message = "";
                                    try {
                                        message = s.string();
                                        Logger.Debug("API", "UPCPricing-ProcessAllItems - Returned Response: " + message);
                                    } catch (Exception ex) {
                                        Logger.Error("API", "UPCPricing-ProcessAllItems - Error In Inner Response: " + ex.getMessage());
                                    }

                                    if(message.isEmpty()){
                                        General.playSuccess();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {

                                                currentItemSerial = "";

                                                scannedItemSerials.clear();
                                                confirmBtn.setEnabled(false);

                                                currentSelectedButton = scannedItemSerial;
                                                SetScannedItemText(scannedItemUPC, "Scan An Item To Start");
                                                SetScannedItemText(scannedItemSerial, "Scan An Item To Start");

                                                dataModels.clear();
                                                scannedItemsAdapter.notifyDataSetChanged();

                                                Snackbar.make(findViewById(R.id.upcPricingActivityLayout), "Pricing Done Successfully", Snackbar.LENGTH_LONG)
                                                        .setAction("No action", null).show();

                                            }
                                        });

                                    }else {
                                        General.playError();
                                        Snackbar.make(findViewById(R.id.upcPricingActivityLayout), message, Snackbar.LENGTH_LONG)
                                                .setAction("No action", null).show();
                                    }

                                    isBusy = false;
                                }
                            }, (throwable) -> {
                                String error = throwable.toString();
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    error = ex.response().errorBody().string();
                                    if(error.isEmpty()) error = throwable.getMessage();
                                    Logger.Debug("API", "UPCPricing-ProcessAllItems - Error In HTTP Response: " + error);
                                }else {
                                    Logger.Error("API", "UPCPricing-ProcessAllItems - Error In API Response: " + throwable.getMessage());
                                }

                                isBusy = false;

                            }));

        } catch (Throwable e) {
            isBusy = false;
            Logger.Error("API", "UPCPricing-ProcessAllItems - Error Connecting: " + e.getMessage());
            Snackbar.make(findViewById(R.id.upcPricingActivityLayout), "Connection To Server Failed!", Snackbar.LENGTH_LONG)
                    .setAction("No action", null).show();
        }
    }


    /**
     * This function sets the text of the item serial and item  upc button with an animation
     * @param btn
     * @param message
     */
    public void SetScannedItemText(Button btn, String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ObjectAnimator animate = ObjectAnimator.ofPropertyValuesHolder(btn,
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
                                btn.setText(message);
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

}
