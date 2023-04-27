package com.bos.wms.mlkit.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.app.adapters.ItemSerialScannedAdapter;
import com.bos.wms.mlkit.app.adapters.ItemSerialScannedDataModel;
import com.bos.wms.mlkit.storage.Storage;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import Model.Pricing.PricingStandModel;
import Model.Pricing.UPCPricingItemModel;
import Remote.APIClient;
import Remote.BasicApi;
import Remote.UserPermissions.UserPermissions;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class UPCPricingOverrideActivity extends AppCompatActivity {

    EditText insertBarcode;
    TextView txtStandId;
    String IPAddress = "", PricingLineCode = "";
    Button scannedItemUPC, scannedItemSerial;
    Button currentSelectedButton;
    String currentItemSerial = "", currentItemUPC = "";
    ArrayList<String> scannedItemSerials;
    ArrayList<ItemSerialScannedDataModel> dataModels;
    ItemSerialScannedAdapter scannedItemsAdapter;
    ListView scannedItemsListView;
    boolean isBusy = false;
    Button confirmBtn, clipBoardBtn;
    PricingStandModel stand;
    int UserId = -1;


    public void CreateStand() {
        try {
            Logger.Debug("API", "UPCPricing-Creating Stand");
            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.CreatePricingStand(UserId, PricingLineCode)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                        if (s != null) {
                                            Logger.Debug("API", "UPCPricing-Create Stand Returned Result: " + s + " Userid: " + UserId + " PricingLineCode: " + PricingLineCode);
                                            stand = s;
                                            runOnUiThread(() -> {
                                                txtStandId.setText("Pricing Override Stand Id: " + s.getId());
                                                insertBarcode.setEnabled(true);
                                                insertBarcode.requestFocus();
                                            });
                                        } else {
                                            Logger.Error("API", "UPCPricing-Create Stand - retuned null:  Userid: " + UserId + " PricingLineCode: " + PricingLineCode);
                                            General.playError();
                                            showMessageAndExit("Failed To Create Stand", "Web Service Returned Null", Color.RED);
                                        }
                                    }
                                    , (throwable) -> {
                                        String error = throwable.toString();
                                        if (throwable instanceof HttpException) {
                                            HttpException ex = (HttpException) throwable;
                                            error = ex.response().errorBody().string();
                                            if (error.isEmpty()) error = throwable.getMessage();
                                            Logger.Debug("API", "UPCPricing-Creating Stand - Error In HTTP Response: " + error);
                                            showMessageAndExit("Failed To Create Stand", error + " (API Http Error)", Color.RED);
                                        } else {
                                            Logger.Error("API", "UPCPricing-Creating Stand - Error In API Response: " + throwable.getMessage());
                                            showMessageAndExit("Failed To Create Stand", throwable.getMessage() + " (API Error)", Color.RED);
                                        }

                                    }));

        } catch (Throwable e) {
            Logger.Error("API", "UPCPricing-Creating Stand: Error" + e.getMessage());
            showMessageAndExit("Failed To Create Stand", e.getMessage() + " (Exception)", Color.RED);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upcpricing_override);

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
        PricingLineCode = mStorage.getDataString("PricingLineCode", "PL001");
        UserId = General.getGeneral(getApplicationContext()).UserID;
        insertBarcode = findViewById(R.id.insertBarcode);
        scannedItemSerials = new ArrayList<>();
        scannedItemSerial = findViewById(R.id.scannedItemSerial);
        scannedItemUPC = findViewById(R.id.scannedItemUPC);
        txtStandId = findViewById(R.id.txtStandId);
        scannedItemsListView = findViewById(R.id.scannedItemsListView);
        dataModels = new ArrayList<>();
        scannedItemsAdapter = new ItemSerialScannedAdapter(dataModels, this, scannedItemsListView);
        scannedItemsListView.setAdapter(scannedItemsAdapter);
        confirmBtn = findViewById(R.id.confirmBtn);
        confirmBtn.setEnabled(false);
        clipBoardBtn = findViewById(R.id.clipBoardBtn);
        confirmBtn.setOnClickListener(view -> {
            SendPrintOrder();
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
        insertBarcode.setOnFocusChangeListener((v, hasFocus) -> insertBarcode.requestFocus());

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
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {

                if (s.length() != 0 && count > 2) {
                    insertBarcode.removeTextChangedListener(this);
                    insertBarcode.setText(" ");
                    insertBarcode.addTextChangedListener(this);

                    ProcessBarCode(s.toString());
                } else if (s.length() != 0 && !s.toString().isEmpty()) {
                    ;
                    insertBarcode.setText(" ");
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(insertBarcode.getWindowToken(), 0);
                }
            }
        });

        //Checks if the user has permission to see clipboard and displays the button for it
        UserPermissions.ValidatePermission("WMSApp.Clipboard", clipBoardBtn);

        //Show The Keyboard When The Button Is Clicked
        clipBoardBtn.setOnClickListener(view -> {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        });
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);


        CreateStand();
        insertBarcode.setEnabled(false);
        //insertBarcode.requestFocus();

    }

    /**
     * This function processes a scanned barcode to place it or discard it
     *
     * @param code
     */
    public void ProcessBarCode(String code) {


        if(stand==null){
            showMessage("Stand Not Ready Yet","Creating Stand", Color.RED);
            return;
        }


        if (isBusy) return; //Make sure the api isnt busy processing the old item scan

        if (currentSelectedButton != null) {
            code = code.replaceAll(" ", "");
            if (currentSelectedButton == scannedItemSerial) {
                /**
                 * Check if the upc is valid
                 */
                if (General.ValidateItemSerialCode(code)) {

                    /**
                     * Check if the item serial was already validated before
                     */
                    if (scannedItemSerials.contains(code)) {

                        showMessageAndExit("XXXXXXX Failure XXXXXXX","ItemSerial Scanned Twice !!!!!",Color.RED);
//                        currentItemSerial = "";
//                        scannedItemSerials.clear();
//                        confirmBtn.setEnabled(false);
//                        currentSelectedButton = scannedItemSerial;
//                        SetScannedItemText(scannedItemUPC, "Scan An Item To Start");
//                        SetScannedItemText(scannedItemSerial, "Scan An Item To Start");
//                        dataModels.clear();
//                        scannedItemsAdapter.notifyDataSetChanged();
//                        Snackbar.make(findViewById(R.id.upcPricingActivityLayout), "Item Serial Already Added, Please Retry", Snackbar.LENGTH_SHORT)
//                                .setAction("No action", null).show();
                        return;
                    }

                    currentItemSerial = code;
                    SetScannedItemText(scannedItemSerial, code);

                    /**
                     * Check if we can move to the next scan which is upc scanning
                     */
                    if (scannedItemUPC.getText().toString().equalsIgnoreCase("Scan An Item To Start")) {
                        currentSelectedButton = scannedItemUPC;
                    } else {
                        /**
                         * Process the item serial and upc data we have
                         */
                        ProcessItem();
                    }
                } else {
                    Logger.Error("InvalidSerial", code);
                    Snackbar.make(findViewById(R.id.upcPricingActivityLayout), "Invalid Item Serial", Snackbar.LENGTH_SHORT)
                            .setAction("No action", null).show();
                }
            } else {
                /**
                 * Check if the upc is valid
                 */
                if (General.ValidateItemCode(code)) {

                    currentItemUPC = code;
                    SetScannedItemText(scannedItemUPC, code);

                    /**
                     * Check if we can move to the next scan which is item serial scanning
                     */
                    if (scannedItemSerial.getText().toString().equalsIgnoreCase("Scan An Item To Start")) {
                        currentSelectedButton = scannedItemSerial;
                    } else {
                        /**
                         * Process the item serial and upc data we have
                         */
                        ProcessItem();
                    }
                } else {
                    Snackbar.make(findViewById(R.id.upcPricingActivityLayout), "Invalid Item UPC", Snackbar.LENGTH_SHORT)
                            .setAction("No action", null).show();
                }
            }
        }
    }

    /**
     * This function is called once we have an item serial and item upc scanned and ready to validate
     */
    public void ProcessItem() {
        try {
            isBusy = true;

            Logger.Debug("API", "UPCPricing-ProcessItem Processing ItemSerial: " + currentItemSerial + " UPC: " + currentItemUPC);
            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            UPCPricingItemModel model=new UPCPricingItemModel(UserId,currentItemSerial,currentItemUPC,stand.getId());
            compositeDisposable.addAll(
                    api.OverrideItemPrice(model)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {
                                    Logger.Debug("API", "UPCPricing-ProcessItem Returned Result: " + s + " ItemSerial: " + s.getItemSerial() + " Price: " + s.getUSDPrice());
                                    General.playSuccess();
                                    SetScannedItemText(scannedItemUPC, "Scan An Item To Start");
                                    SetScannedItemText(scannedItemSerial, "Scan An Item To Start");
                                    currentSelectedButton = scannedItemSerial;
                                    runOnUiThread(() -> {
                                        confirmBtn.setEnabled(true);
                                        scannedItemSerials.add(currentItemSerial);
                                        dataModels.add(0,new ItemSerialScannedDataModel(s.getItemSerial(), currentItemUPC,s.getUSDPrice()));
                                        scannedItemsAdapter.notifyDataSetChanged();

                                        SetScannedItemText(scannedItemUPC, "Scan An Item To Start");
                                        SetScannedItemText(scannedItemSerial, "Scan An Item To Start");
                                        currentSelectedButton = scannedItemSerial;

                                    });

                                    isBusy = false;
                                }
                            }, (throwable) -> {
                                String error = throwable.toString();
                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    error = ex.response().errorBody().string();
                                    if (error.isEmpty()) error = throwable.getMessage();
                                    Logger.Debug("API", "UPCPricing-ProcessItem - Error In HTTP Response: " + error);
                                    showMessage("Scan Error", error + " (API Http Error)", Color.RED);
                                } else {
                                    Logger.Error("API", "UPCPricing-ProcessItem - Error In API Response: " + throwable.getMessage());
                                    showMessage("Scan Error", throwable.getMessage() + " (API Error)", Color.RED);
                                }
                                isBusy = false;
                                runOnUiThread(() -> {
                                    SetScannedItemText(scannedItemUPC, "Scan An Item To Start");
                                    SetScannedItemText(scannedItemSerial, "Scan An Item To Start");
                                    currentSelectedButton = scannedItemSerial;

                                });


                            }));

        } catch (Throwable e) {
            isBusy = false;
            Logger.Error("API", "UPCPricing-ProcessItem - Error Connecting: " + e.getMessage());
            showMessage("Web Service Error: " , e.getMessage() , Color.RED);
        }
    }


    public void SendPrintOrder(){
        try {
            Logger.Debug("API", "UPCPricing-SendPrintOrder");
            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.SendPrintingStand(stand)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                        if (s != null) {
                                            String message = "";
                                            try {
                                                message = s.string();
                                                Logger.Debug("API", "UUPCPricing-SendPrintOrder - Returned Response: " + message);
                                            } catch (Exception ex) {
                                                Logger.Error("API", "UPCPricing-SendPrintOrder - Error In Inner Response: " + ex.getMessage());
                                                message="Error "+ex.getMessage();
                                            }
                                            if(message.startsWith("Success")){
                                                showMessageAndExit("✓✓✓✓✓✓ Success ✓✓✓✓✓",message,Color.GREEN);
                                            }
                                            else{
                                                showMessageAndExit("XXXXXX Failure XXXXXX",message,Color.RED);
                                            }


                                        } else {
                                            Logger.Error("API", "UPCPricing-SendPrintOrder - retuned null");
                                            showMessageAndExit("XXXXXX Failure XXXXXX", "Web Service Returned Null", Color.RED);
                                        }
                                    }
                                    , (throwable) -> {
                                        String error = "";
                                        if (throwable instanceof HttpException) {
                                            HttpException ex = (HttpException) throwable;
                                            error = ex.response().errorBody().string();
                                            if (error.isEmpty()) error = throwable.getMessage();
                                            Logger.Debug("API", "UPCPricing-SendPrintOrder - Error In HTTP Response: " + error);
                                            showMessageAndExit("XXXXXX Failure XXXXXX", error + "\n(API Http Error)", Color.RED);
                                        } else {
                                            Logger.Error("API", "UPCPricing-SendPrintOrder - Error In API Response: " + throwable.getMessage());
                                            showMessageAndExit("XXXXXX Failure XXXXXX", throwable.getMessage() + "/n(API Error)", Color.RED);
                                        }

                                    }));

        } catch (Throwable e) {
            Logger.Error("API", "UPCPricing-SendPrintOrder  Error" + e.getMessage());
            showMessageAndExit("XXXXXX Failure XXXXXX", e.getMessage() + " (Exception)", Color.RED);
        }
    }


    /**
     * This function sets the text of the item serial and item  upc button with an animation
     *
     * @param btn
     * @param message
     */
    public void SetScannedItemText(Button btn, String message) {
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

    public void Beep() {
        General.playError();
    }

    private void showMessage(String title, String msg, int color) {
        if (color == Color.RED)
            Beep();
        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(msg)
                    .setPositiveButton("Ok", (dialog, which) -> {

                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        });

    }

    private void showMessageAndExit(String title, String msg, int color) {
        if (color == Color.RED)
            Beep();
        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(msg)
                    .setPositiveButton("Ok", (dialog, which) -> {
                        finish();
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .show();
        });
    }

    /**
     * This function processes all items
     */
//    public void ProcessAllItems() {
//        try {
//            isBusy = true;
//            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
//            CompositeDisposable compositeDisposable = new CompositeDisposable();
//
//            UserId = General.getGeneral(getApplicationContext()).UserID;
//
//            ArrayList<String> allItemSerials = new ArrayList<>(), allUPCs = new ArrayList<>();
//
//            for (ItemSerialScannedDataModel model : dataModels) {
//                allItemSerials.add(model.getItemSerial());
//                allUPCs.add(model.getUPC());
//            }
//
//            try {
//                Logger.Debug("API", "UPCPricing-ProcessAllItems - Start Process Set ItemSerials: " + String.join(",", allItemSerials));
//                Logger.Debug("API", "UPCPricing-ProcessAllItems - Start Process Set UPCS: " + String.join(",", allUPCs));
//                Logger.Debug("API", "UPCPricing-ProcessAllItems - Start Process Set Pricing Line Code: " + PricingLineCode);
//            } catch (Exception ex) {
//                Logger.Debug("API", "UPCPricing-ProcessAllItems - Starting Process For All Items, Error: " + ex.getMessage());
//            }
//
//            compositeDisposable.addAll(
//                    api.PostUPCPricing(new UPCPricingModel(UserId, allItemSerials, allUPCs, PricingLineCode))
//                            .subscribeOn(Schedulers.io())
//                            .observeOn(AndroidSchedulers.mainThread())
//                            .subscribe((s) -> {
//                                if (s != null) {
//                                    String message = "";
//                                    try {
//                                        message = s.string();
//                                        Logger.Debug("API", "UPCPricing-ProcessAllItems - Returned Response: " + message);
//                                    } catch (Exception ex) {
//                                        Logger.Error("API", "UPCPricing-ProcessAllItems - Error In Inner Response: " + ex.getMessage());
//                                    }
//
//                                    if (message.isEmpty()) {
//                                        General.playSuccess();
//                                        Logger.Debug("API", "UPCPricing-ProcessAllItems - Response Empty, Pricing Done Successfully");
//                                        runOnUiThread(new Runnable() {
//                                            @Override
//                                            public void run() {
//
//                                                currentItemSerial = "";
//
//                                                scannedItemSerials.clear();
//                                                confirmBtn.setEnabled(false);
//
//                                                currentSelectedButton = scannedItemSerial;
//                                                SetScannedItemText(scannedItemUPC, "Scan An Item To Start");
//                                                SetScannedItemText(scannedItemSerial, "Scan An Item To Start");
//
//                                                dataModels.clear();
//                                                scannedItemsAdapter.notifyDataSetChanged();
//
//                                                Snackbar.make(findViewById(R.id.upcPricingActivityLayout), "Pricing Done Successfully", Snackbar.LENGTH_LONG)
//                                                        .setAction("No action", null).show();
//
//                                            }
//                                        });
//
//                                    } else {
//                                        General.playError();
//                                        Snackbar.make(findViewById(R.id.upcPricingActivityLayout), message, Snackbar.LENGTH_LONG)
//                                                .setAction("No action", null).show();
//                                    }
//
//                                    isBusy = false;
//                                }
//                            }, (throwable) -> {
//                                String error = throwable.toString();
//                                if (throwable instanceof HttpException) {
//                                    HttpException ex = (HttpException) throwable;
//                                    error = ex.response().errorBody().string();
//                                    if (error.isEmpty()) error = throwable.getMessage();
//                                    Logger.Debug("API", "UPCPricing-ProcessAllItems - Error In HTTP Response: " + error);
//                                } else {
//                                    Logger.Error("API", "UPCPricing-ProcessAllItems - Error In API Response: " + throwable.getMessage());
//                                }
//
//                                isBusy = false;
//
//                            }));
//
//        } catch (Throwable e) {
//            isBusy = false;
//            Logger.Error("API", "UPCPricing-ProcessAllItems - Error Connecting: " + e.getMessage());
//            Snackbar.make(findViewById(R.id.upcPricingActivityLayout), "Connection To Server Failed!", Snackbar.LENGTH_LONG)
//                    .setAction("No action", null).show();
//        }
//    }
}