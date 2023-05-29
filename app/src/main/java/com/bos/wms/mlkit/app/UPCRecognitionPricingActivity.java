package com.bos.wms.mlkit.app;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.DisplayOrientedMeteringPointFactory;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.app.Logger;
import com.bos.wms.mlkit.app.adapters.ItemSerialScannedAdapter;
import com.bos.wms.mlkit.app.adapters.ItemSerialScannedDataModel;
import com.bos.wms.mlkit.storage.Storage;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Model.Pricing.PricingStandModel;
import Model.Pricing.UPCPricingItemModel;
import Remote.APIClient;
import Remote.BasicApi;
import Remote.UserPermissions.UserPermissions;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class UPCRecognitionPricingActivity extends AppCompatActivity {

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

    LinearLayout upcCameraLayout;

    private FirebaseAuth firebaseAuh;
    private FirebaseFunctions firebaseFunctions;

    private PreviewView cameraPreview;

    private ImageView cameraPreviewImageView;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    private Camera currentCamera;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1000;

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
                                                txtStandId.setText("(UPC Pricing) Stand Id: " + s.getId());
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
        setContentView(R.layout.activity_upcrecognition_pricing);

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

        upcCameraLayout = findViewById(R.id.upcCameraLayout);

        cameraPreviewImageView = findViewById(R.id.cameraPreviewImageView);

        cameraPreview = findViewById(R.id.cameraPreview);

        cameraExecutor = Executors.newSingleThreadExecutor();

        //Initialize firebase and the the firebase authenticator
        FirebaseApp.initializeApp(this);
        firebaseAuh = FirebaseAuth.getInstance();
        CloudLogin();

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
            CloseCameraUPC();
        });
        scannedItemUPC.setOnClickListener(view -> {
            StartUPCCamera();
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


        CreateStand();
        insertBarcode.setEnabled(false);
        //insertBarcode.requestFocus();

        if(cameraPermissionsGranted()){
            startCamera();
        }else {
            requestCameraPermissions();
        }

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
                        StartUPCCamera();
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
                        CloseCameraUPC();
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
                    api.PriceUPCItem(model)
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
                                            CloseCameraUPC();
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
                                    CloseCameraUPC();

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

    /* This Is The Camera Scanner For UPC */

    ProgressDialog mainProgressDialog;

    public void StartUPCCamera(){
        upcCameraLayout.setVisibility(View.VISIBLE);
    }

    public void GotUPCFromCamera(String upc){
        mainProgressDialog.cancel();
        cameraPreviewImageView.setVisibility(View.INVISIBLE);
        currentSelectedButton = scannedItemUPC;
        ProcessBarCode(upc);
    }

    public void FailedUPCCamera(){
        mainProgressDialog.cancel();
        cameraPreviewImageView.setVisibility(View.INVISIBLE);
        Logger.Error("OCR", "No UPC Was Detected In UPC Recognition");
        showMessage("Error", "No UPC Was Detected!", Color.RED);
    }

    public void CloseCameraUPC(){
        upcCameraLayout.setVisibility(View.INVISIBLE);
        cameraPreviewImageView.setVisibility(View.INVISIBLE);
    }

    /**
     * Start the back camera, and build the preview for the user to see
     */
    public void startCamera(){
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                //Create Preview And Bind It
                Preview preview = new Preview.Builder()
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                imageCapture =
                        new ImageCapture.Builder()
                                .build();

                currentCamera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageCapture, preview);
                /**
                 * This code helps the camera focus on click
                 */
                cameraPreview.setOnTouchListener(new View.OnTouchListener() {
                    @RequiresApi(api = Build.VERSION_CODES.R)
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                        {
                            try {
                                captureImage(0);
                                mainProgressDialog = ProgressDialog.show(UPCRecognitionPricingActivity.this, "",
                                        "Detecting UPC, Please wait...", true);
                                mainProgressDialog.show();
                            } catch (Exception e) {
                                Logger.Error("Camera", "cameraFocus - " + e.getMessage());
                            }
                        }
                        return true;
                    }
                });

            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));

    }

    /**
     * Capture an image and save it for the handler to handle and analyze
     * The handler is a background service that processes all the ocr data
     */
    public void captureImage(int trail) {

        try {
            SurfaceOrientedMeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(1f, 1f);
            MeteringPoint autoFocusPoint = factory.createPoint(0.5f, 0.5f);

            ListenableFuture<FocusMeteringResult>future = currentCamera.getCameraControl().startFocusAndMetering(
                    new FocusMeteringAction.Builder(
                            autoFocusPoint,
                            FocusMeteringAction.FLAG_AF
                    ).build());

            future.addListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        FocusMeteringResult result = future.get();
                        if(result.isFocusSuccessful()){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    cameraPreviewImageView.setImageBitmap(cameraPreview.getBitmap());
                                    cameraPreviewImageView.setVisibility(View.VISIBLE);
                                    GetUPCFromImage(cameraPreview.getBitmap());
                                }
                            });


                        }else {
                            if(trail < 2){
                                captureImage(trail + 1);
                                Logger.Debug("Camera", "Couldn't Auto Focus Camera, Retrying");
                            }else {
                                mainProgressDialog.cancel();
                                showMessage("Camera", "Error Focusing Image For Capture", Color.RED);
                                Logger.Debug("Camera", "Couldn't Auto Focus Camera, Capturing Image With No Focus");
                            }
                            cameraPreviewImageView.setVisibility(View.INVISIBLE);
                        }
                    } catch (Exception ex) {
                        mainProgressDialog.cancel();
                        cameraPreviewImageView.setVisibility(View.INVISIBLE);
                        Logger.Error("Camera", "cameraFocusListener - " + ex.getMessage());
                        showMessage("Camera", "Error Focusing Image For Capture: " + ex.getMessage(), Color.RED);
                    }
                }
            }, cameraExecutor);

        } catch (Exception e) {
            mainProgressDialog.cancel();
            cameraPreviewImageView.setVisibility(View.INVISIBLE);
            Logger.Error("Camera", "cameraFocus - " + e.getMessage());
            showMessage("Camera", "Error Focusing Image For Capture: " + e.getMessage(), Color.RED);
        }

    }

    public void GetUPCFromImage(Bitmap originalBitmap){
        Bitmap bitmap = scaleBitmapDown(originalBitmap, 640);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        String base64encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

        JsonObject request = new JsonObject();

        //Add image to the request
        JsonObject imageJson = new JsonObject();
        imageJson.add("content", new JsonPrimitive(base64encoded));

        request.add("image", imageJson);

        //Add features to the request
        JsonObject feature = new JsonObject();
        feature.add("type", new JsonPrimitive("TEXT_DETECTION"));
        JsonArray features = new JsonArray();
        features.add(feature);

        request.add("features", features);

        JsonObject imageContext = new JsonObject();
        JsonArray languageHints = new JsonArray();
        languageHints.add("en");
        imageContext.add("languageHints", languageHints);

        request.add("imageContext", imageContext);

        if(firebaseFunctions != null){
            annotateImage(request.toString()).addOnCompleteListener((task) -> {
                if(task.isSuccessful()){
                    try {
                        if(task.getResult() != null && task.getResult().getAsJsonArray() != null){
                            String resultText = task.getResult().getAsJsonArray().get(0).getAsJsonObject().get("fullTextAnnotation").getAsJsonObject()
                                    .get("text").getAsString();
                            Logger.Debug("OCR", "UploadImage - Analyzed Image Data, Processing The OCR For UPC");
                            processMultipleOCRData(resultText);

                        }else {
                            mainProgressDialog.cancel();
                            cameraPreviewImageView.setVisibility(View.INVISIBLE);
                            showMessage("OCR Error", "Failed analyzing the data, please try again!", Color.RED);
                            Logger.Debug("OCR", "UploadImage - Failed analyzing the data");
                        }
                    }catch (Exception ex){
                        mainProgressDialog.cancel();
                        cameraPreviewImageView.setVisibility(View.INVISIBLE);
                        showMessage("OCR Error", "Failed analyzing the data, please try again!\nError : " + ex.getMessage(), Color.RED);
                        Logger.Error("OCR", "UploadImage - Returned Error: " + ex.getMessage());
                    }
                }else {
                    mainProgressDialog.cancel();
                    cameraPreviewImageView.setVisibility(View.INVISIBLE);
                    showMessage("OCR Error", "A Cloud Error Occurred: " + task.getException().getMessage(), Color.RED);
                    Logger.Error("OCR", "UploadImage - Error: " + task.getException().getMessage());
                }
            });
            Logger.Debug("OCR", "UploadImage - Sent Request To Google Vision, Bytes Size: " + imageBytes.length);
        }else {
            mainProgressDialog.cancel();
            cameraPreviewImageView.setVisibility(View.INVISIBLE);
            showMessage("OCR Error", "An Internal Error Occurred: Firebase Functions Not Initiated", Color.RED);
            Logger.Error("FIREBASE", "UploadImage - Error, FireBase Functions Not Initiated");
        }
    }

    /**
     * Sends the image to the Google Vision API (Uses FireBase Functions - annotateImage Function)
     * @param requestJson The json string of the request including the image encoded in Base64 and some variables for the google vision api to use
     * @return Returns a result from the Google Vision API as a JsonElement
     */
    private Task<JsonElement> annotateImage(String requestJson) {
        return firebaseFunctions
                .getHttpsCallable("annotateImage")
                .call(requestJson)
                .continueWith(new Continuation<HttpsCallableResult, JsonElement>() {
                    @Override
                    public JsonElement then(@NonNull Task<HttpsCallableResult> task) {
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down.
                        return JsonParser.parseString(new Gson().toJson(task.getResult().getData()));
                    }
                });
    }

    /**
     * This functions resizes a bitmap to a given dimension
     * @param bitmap
     * @param maxDimension
     * @return
     */
    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    /**
     * Process the text to gather the UPC number by splitting the text to lines
     * Splitting the lines by spaces
     * In some cases the spaces might be \r so to be safe we split the result of the spaces split by \r
     * then we clear any unwanted characters like text that might be read along the line
     * then we add the upc to an array after checking that its 12-13 numbers to send to the respective api and check its validity
     *
     * Note: Since we are splitting the line by spaces and we are looping through all the chunks of letter/numbers left
     * We can skip the part for the text filtering (removing unwanted characters and leaving numbers) and check if the chunk of characters we have
     * are only numbers for better number accuracy. But in some cases we might not know what the Google Vision OCR might return. So for that reason this is
     * going to stay for better results accuracy.
     * @param text The actual text received from the google vision api
     */
    public void processMultipleOCRData(String text){
        Logger.Debug("OCR", "Received OCR Text For UPC: " + text);
        String[] lines = text.split("\n");
        ArrayList<String> upcs = new ArrayList<String>();
        for(String line : lines){
            String[] args = line.split(" ");
            for(String arg : args){
                String[] args2 = arg.split("\r");
                for(String  arg2 : args2){
                    arg2 = arg2.replaceAll("/", "1").replaceAll("[^0-9]", "");
                    if(arg2.length() >= 12){
                        String currentUPC = "";
                        for(int i = 0; i < arg2.length(); i++){
                            if(arg2.length() - i == 12 || arg2.charAt(i) != '0'){
                                currentUPC = arg2.substring(i);
                                break;
                            }
                        }
                        Logger.Debug("OCR", "Got Original UPC Text: " + arg2 + " Removed Leading Zeros: " + currentUPC);
                        if((currentUPC.length() == 12 || currentUPC.length() == 13)  && !upcs.contains(currentUPC)){
                            upcs.add(currentUPC);
                        }
                    }
                }
            }
            String line2 = line.replaceAll("/", "1").replaceAll("[^0-9]", "").replaceAll(" ", "");
            if(line2.length() >= 12){
                String currentUPC = "";
                for(int i = 0; i < line2.length(); i++){
                    if(line2.length() - i == 12 || line2.charAt(i) != '0'){
                        currentUPC = line2.substring(i);
                        break;
                    }
                }
                Logger.Debug("OCR", "Got Original UPC Text: " + line2 + " Removed Leading Zeros: " + currentUPC);
                if(currentUPC.length() == 12  && !upcs.contains(currentUPC)){
                    upcs.add(currentUPC);
                }
            }
        }
        if(upcs.size() == 0){
            FailedUPCCamera();
        }else {
            GotUPCFromCamera(upcs.get(0));
        }
    }

    /**
     * Sign in to firebase using a username and password. This is needed to be able to call the Google Vision API
     */
    public void CloudLogin(){
        firebaseAuh.signInWithEmailAndPassword("ocr@katayagroup.com","Feras@!@#123").addOnCompleteListener(this, (result) -> {
            if(result.isSuccessful()){
                Logger.Debug("FIREBASE", "CloudLogin - Logged In Successfully");
                firebaseFunctions = FirebaseFunctions.getInstance(firebaseAuh.getApp());
                Logger.Debug("FIREBASE", "CloudLogin - Created Functions Instance " + firebaseAuh.getCurrentUser().getUid());
            }else {
                Logger.Debug("FIREBASE", "CloudLogin - Firebase Login Failed");
                if(result.getException() !=null && result.getException().getMessage() !=null) {
                    Logger.Error("FIREBASE", "CloudLogin- " + result.getException().getMessage());
                }
                Toast.makeText(getBaseContext(), "Authentication failed.",
                        Toast.LENGTH_SHORT).show();
            }

        });
    }

    /**
     * Check if the camera permissions are granted for the camera preview and capture
     * @return
     */
    public boolean cameraPermissionsGranted(){
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request the camera permissions from the user
     */
    public void requestCameraPermissions(){
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Logger.Debug("CAMERA", "OnRequestPermissionsResult - Camera Started, User Granted Permissions");
                startCamera();
            } else {
                Toast.makeText(this, "Can't use camera scan", Toast.LENGTH_LONG).show();
                Logger.Error("CAMERA", "OnRequestPermissionsResult - User Failed To Grant Camera Permissions");
            }
        }
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
