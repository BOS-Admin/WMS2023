package com.hc.mixthebluetooth.activity.rfidLotBond;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.ProgressDialog;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.hc.basiclibrary.viewBasic.BasActivity;
import com.hc.mixthebluetooth.Logger;
import com.hc.mixthebluetooth.Model.RfidLotBond.RfidLotBondSessionModel;
import com.hc.mixthebluetooth.R;
import com.hc.mixthebluetooth.Remote.Routes.BasicApi;
import com.hc.mixthebluetooth.Remote.UserPermissions.UserPermissions;
import com.hc.mixthebluetooth.activity.adapters.ItemSerialScannedAdapter;
import com.hc.mixthebluetooth.activity.adapters.ItemSerialScannedDataModel;
import com.hc.mixthebluetooth.storage.Storage;
import com.util.General;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;

import Remote.APIClient;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class ItemSerialLotBondActivity extends BasActivity {

    EditText insertBarcode;

    String IPAddress = "";

    Button scannedCartonNumber, scannedLotNumber;

    Button currentSelectedButton;

    String currentCartonNumber = "", currentLotNumber = "";

    ArrayList<String> scannedItemSerials;

    ArrayList<ItemSerialScannedDataModel> dataModels;

    ItemSerialScannedAdapter scannedItemsAdapter;

    ListView scannedItemsListView;
    Button confirmBtn, clipBoardBtn;

    int UserId = -1;

    boolean isBusy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_serial_lot_bond);

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");

        UserId = mStorage.getDataInt("UserID", -1);

        Logger.Debug("UserID", "Current User ID: " + UserId);

        insertBarcode = findViewById(R.id.insertBarcode);
        scannedItemSerials = new ArrayList<>();
        scannedCartonNumber = findViewById(R.id.scannedCartonNumber);
        scannedLotNumber = findViewById(R.id.scannedLotNumber);

        scannedItemsListView = findViewById(R.id.scannedItemsListView);

        dataModels = new ArrayList<>();
        scannedItemsAdapter = new ItemSerialScannedAdapter(dataModels, this, scannedItemsListView);

        scannedItemsListView.setAdapter(scannedItemsAdapter);

        confirmBtn = findViewById(R.id.confirmBtn);
        confirmBtn.setEnabled(false);

        clipBoardBtn = findViewById(R.id.clipBoardBtn);

        confirmBtn.setOnClickListener(view -> {
            finish();
        });

        /**
         * This part helps identify which selection to paste the barcode scan to
         */
        currentSelectedButton = scannedLotNumber;

        scannedLotNumber.setOnClickListener(view -> {
            currentSelectedButton = scannedLotNumber;
            scannedLotNumber.setText("Scan An Item To Start");
        });

        scannedCartonNumber.setOnClickListener(view -> {
            currentSelectedButton = scannedCartonNumber;
            scannedCartonNumber.setText("Scan An Item To Start");
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
                    insertBarcode.removeTextChangedListener(this);
                    insertBarcode.setText(" ");
                    insertBarcode.addTextChangedListener(this);
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


        //insertBarcode.setEnabled(false);
        insertBarcode.requestFocus();

    }

    /**
     * This function processes a scanned barcode to place it or discard it
     *
     * @param code
     */
    public void ProcessBarCode(String code) {


        if (isBusy) return; //Make sure the api isnt busy processing the old item scan

        Logger.Debug("ISLOTBOND", "Read Barcode: " + code);

        if (currentSelectedButton != null) {
            code = code.replaceAll(" ", "");
            if (currentSelectedButton == scannedLotNumber) {
                /**
                 * Check if the lot number is valid
                 */
                if (code.length() > 4) {

                    currentLotNumber = code;
                    SetScannedItemText(scannedLotNumber, code);

                    /**
                     * Check if we can move to the next scan which is carton scanning
                     */
                    if (scannedCartonNumber.getText().toString().equalsIgnoreCase("Scan An Item To Start")) {
                        currentSelectedButton = scannedCartonNumber;
                        ValidateLotNumber();
                    } else {
                        /*currentSelectedButton = null;
                        */
                        ValidateCartonNumber();
                    }
                } else {
                    Logger.Error("ISLOTBOND", "Invalid Lot Number Scanned: " + code);
                    ShowSnackbar("Invalid Lot Number: " + code);
                }
            } else {
                /**
                 * Check if the carton is valid
                 */
                if (code.length() > 10) {

                    currentCartonNumber = code;
                    SetScannedItemText(scannedCartonNumber, code);

                    /**
                     * Check if we can move to the next scan which is lot number scanning
                     */
                    if (scannedLotNumber.getText().toString().equalsIgnoreCase("Scan An Item To Start")) {
                        currentSelectedButton = scannedLotNumber;
                    } else {
                        /*
                        currentSelectedButton = null;*/
                        ValidateCartonNumber();
                    }
                } else {
                    Logger.Error("ISLOTBOND", "Invalid Carton Number Scanned: " + code);
                    ShowSnackbar("Invalid Carton Number: " + code);
                }
            }
        }else {
            /**
             * Process the item serial
             */
            ProcessItem(code);
        }
    }

    /**
     * This function is called once we have an item serial, Lot Number, And Carton Number scanned and ready to proceed with the Item Serial Lot Bond
     */
    public void ProcessItem(String itemSerial) {
        try {
            isBusy = true;

            Logger.Debug("API", "ISLotBond-ProcessItem Processing LotNumber: " + currentLotNumber + " Carton: " + currentCartonNumber + " ItemSerial: " + itemSerial);
            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();

            RfidLotBondSessionModel model = new RfidLotBondSessionModel();

            model.setActivity("Item Serial Lot Bond");
            model.setBol(currentLotNumber.replaceAll(" ", ""));
            model.setCartonNo(currentCartonNumber.replaceAll(" ", ""));
            model.setStationCode(null);
            DateTimeFormatter formatter = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
                model.setSessionStartDate(formatter.format(LocalDateTime.now()));
                model.setRfidReadStart(formatter.format(LocalDateTime.now()));
                model.setRfidReadStop(formatter.format(LocalDateTime.now()));
                model.setRfidLotBondStart(formatter.format(LocalDateTime.now()));
                model.setRfidLotBondStop(formatter.format(LocalDateTime.now()));
            }
            model.setRfid("ItemSerialLotBond");//("30342B01C04A55077ED7640-E280116020006520A4FE0AA7");
            model.setItemSerial(itemSerial);
            model.setUserId(UserId);

            compositeDisposable.addAll(
                    api.RFIDLotBondEnhanced(model)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {
                                    Logger.Debug("API", "ISLotBond-ProcessItem Returned Result: " + s.getRfidLotBondMessage() + " ItemSerial: " + s.getItemSerial());
                                    playSuccess();//rfidLotBondMessage

                                    PostRFIDLotBondSession(model, s.getRfidLotBondMessage());
                                    runOnUiThread(() -> {
                                        confirmBtn.setEnabled(true);
                                        dataModels.add(new ItemSerialScannedDataModel(s.getItemSerial()));
                                        scannedItemsAdapter.notifyDataSetChanged();
                                        ShowSnackbar(s.getRfidLotBondMessage());
                                        //SetScannedItemText(scannedLotNumber, "Scan An Item To Start");
                                        //SetScannedItemText(scannedCartonNumber, "Scan An Item To Start");
                                        //currentSelectedButton = scannedLotNumber;

                                    });

                                    isBusy = false;
                                    currentSelectedButton = null;
                                }
                            }, (throwable) -> {
                                String error = throwable.toString();
                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    error = ex.response().errorBody().string();
                                    if (error.isEmpty()) error = throwable.getMessage();
                                    Logger.Debug("API", "UPCPricing-ProcessItem - Error In HTTP Response: " + error);
                                    ShowMessage("Scan Error", error + " (API Http Error)", Color.RED);
                                    PostRFIDLotBondSession(model, error);
                                } else {
                                    Logger.Error("API", "UPCPricing-ProcessItem - Error In API Response: " + throwable.getMessage());
                                    ShowMessage("Scan Error", throwable.getMessage() + " (API Error)", Color.RED);
                                    PostRFIDLotBondSession(model, error);
                                }
                                isBusy = false;
                                runOnUiThread(() -> {
                                    //SetScannedItemText(scannedLotNumber, "Scan An Item To Start");
                                    //SetScannedItemText(scannedCartonNumber, "Scan An Item To Start");
                                    //currentSelectedButton = scannedLotNumber;

                                });

                            }));

        } catch (Throwable e) {
            isBusy = false;
            Logger.Error("API", "UPCPricing-ProcessItem - Error Connecting: " + e.getMessage());
            ShowMessage("Web Service Error: " , e.getMessage() , Color.RED);
        }
    }

    public void PostRFIDLotBondSession(RfidLotBondSessionModel model, String message){
        if(model == null)
            return;

        model.setRfidLotBondMessage(message);
        try {

            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();

            compositeDisposable.addAll(
                    api.postRidLotBondSession(model)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {
                                    Logger.Debug("API", "ISLotBond-PostRFIDLotBondSession Returned Result: " + s.string() );
                                }
                            }, (throwable) -> {
                                String error = throwable.toString();
                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    error = ex.response().errorBody().string();
                                    if (error.isEmpty()) error = throwable.getMessage();
                                    Logger.Debug("API", "UPCPricing-PostRFIDLotBondSession - Error In HTTP Response: " + error);
                                } else {
                                    Logger.Error("API", "UPCPricing-PostRFIDLotBondSession - Error In API Response: " + throwable.getMessage());
                                }

                            }));

        } catch (Throwable e) {
            Logger.Error("API", "UPCPricing-PostRFIDLotBondSession - Error Connecting: " + e.getMessage());
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

    /**
     * This function validates the current Lot Number After its being scanned
     */
    public void ValidateLotNumber(){
        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Validating Lot #" + currentLotNumber + ", Please wait...", true);
        mainProgressDialog.show();
        try {
            isBusy = true;

            Logger.Debug("API", "ISLotBond-ValidateLotNumber Validating LotNumber: " + currentLotNumber);
            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.ValidateBol(currentLotNumber)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {
                                    Logger.Debug("API", "ISLotBond-ValidateLotNumber Returned Result: " + s + " LotNumber: " + currentLotNumber);

                                    if(s){
                                        ShowSnackbar("Lot Number: " + currentLotNumber + " Valid!");
                                        playSuccess();
                                    }else {
                                        runOnUiThread(() -> {
                                            SetScannedItemText(scannedLotNumber, "Scan An Item To Start");
                                            currentSelectedButton = scannedLotNumber;
                                            ShowSnackbar("Invalid Lot Number");
                                        });
                                        playError();
                                    }
                                    mainProgressDialog.cancel();
                                    isBusy = false;
                                }
                            }, (throwable) -> {
                                String error = throwable.toString();
                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    error = ex.response().errorBody().string();
                                    if (error.isEmpty()) error = throwable.getMessage();
                                    Logger.Debug("API", "UPCPricing-ValidateLotNumber - Error In HTTP Response: " + error);
                                    ShowMessage("Scan Error", error + " (API Http Error)", Color.RED);
                                } else {
                                    Logger.Error("API", "UPCPricing-ValidateLotNumber - Error In API Response: " + throwable.getMessage());
                                    ShowMessage("Scan Error", throwable.getMessage() + " (API Error)", Color.RED);
                                }
                                isBusy = false;
                                runOnUiThread(() -> {
                                    SetScannedItemText(scannedLotNumber, "Scan An Item To Start");
                                    SetScannedItemText(scannedCartonNumber, "Scan An Item To Start");
                                    currentSelectedButton = scannedLotNumber;

                                });
                                mainProgressDialog.cancel();

                            }));

        } catch (Throwable e) {
            isBusy = false;
            Logger.Error("API", "UPCPricing-ValidateLotNumber - Error Connecting: " + e.getMessage());
            ShowMessage("Web Service Error: " , e.getMessage() , Color.RED);
            mainProgressDialog.cancel();
        }
    }

    /**
     * This function validates the current Lot Number After its being scanned
     */
    public void ValidateCartonNumber(){
        ProgressDialog mainProgressDialog = ProgressDialog.show(this, "",
                "Validating Lot #" + currentLotNumber + " And Carton #" + currentCartonNumber + ", Please wait...", true);
        mainProgressDialog.show();
        try {
            isBusy = true;

            Logger.Debug("API", "ISLotBond-ValidateCartonNumber Validating LotNumber: " + currentLotNumber + " Carton Number: " + currentCartonNumber);
            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.ValidateCarton(currentCartonNumber, currentLotNumber)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {
                                    Logger.Debug("API", "ISLotBond-ValidateCartonNumber Returned Result: " + s + " LotNumber: " + currentLotNumber + " Carton Number: " + currentCartonNumber);

                                    if(s){
                                        ShowSnackbar("Please Start Scanning Item Serials");
                                        currentSelectedButton = null;
                                        playSuccess();
                                    }else {
                                        runOnUiThread(() -> {
                                            SetScannedItemText(scannedCartonNumber, "Scan An Item To Start");
                                            currentSelectedButton = scannedCartonNumber;
                                            ShowSnackbar("Invalid Carton Number");
                                        });
                                        playError();
                                    }
                                    mainProgressDialog.cancel();
                                    isBusy = false;
                                }
                            }, (throwable) -> {
                                String error = throwable.toString();
                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    error = ex.response().errorBody().string();
                                    if (error.isEmpty()) error = throwable.getMessage();
                                    Logger.Debug("API", "UPCPricing-ValidateCartonNumber - Error In HTTP Response: " + error);
                                    ShowMessage("Scan Error", error + " (API Http Error)", Color.RED);
                                } else {
                                    Logger.Error("API", "UPCPricing-ValidateCartonNumber - Error In API Response: " + throwable.getMessage());
                                    ShowMessage("Scan Error", throwable.getMessage() + " (API Error)", Color.RED);
                                }
                                isBusy = false;
                                runOnUiThread(() -> {
                                    SetScannedItemText(scannedLotNumber, "Scan An Item To Start");
                                    SetScannedItemText(scannedCartonNumber, "Scan An Item To Start");
                                    currentSelectedButton = scannedLotNumber;

                                });
                                mainProgressDialog.cancel();

                            }));

        } catch (Throwable e) {
            isBusy = false;
            Logger.Error("API", "UPCPricing-ValidateCartonNumber - Error Connecting: " + e.getMessage());
            ShowMessage("Web Service Error: " , e.getMessage() , Color.RED);
            mainProgressDialog.cancel();
        }
    }

    /**
     * This function only shows a snackbar message in the main menu
     * @param message
     */
    public void ShowSnackbar(String message){
        Snackbar.make(findViewById(R.id.itemSerialLotBondActivityLayout), message, Snackbar.LENGTH_LONG)
                .setAction("No action", null).show();
    }

    @Override
    public void initAll() {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }


    /**
     * This function shows a message dialog popup with the corresponding title, and color
     * @param title
     * @param msg
     * @param color
     */
    private void ShowMessage(String title, String msg, int color) {
        if (color == Color.RED)
            playError();
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

}
