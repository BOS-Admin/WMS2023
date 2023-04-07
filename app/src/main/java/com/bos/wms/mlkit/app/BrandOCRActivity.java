package com.bos.wms.mlkit.app;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.DisplayOrientedMeteringPointFactory;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.InputType;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.storage.Storage;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
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
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import Model.BolRecognitionModel;
import Remote.APIClient;
import Remote.BasicApi;
import Remote.VolleyMultipartRequest;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class BrandOCRActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1000;

    private PreviewView cameraPreview;
    private ImageView cameraPreviewImageView;

    private ImageAnalysis imageAnalyzer;

    private ImageButton captureImageButton, pauseCaptureButton, confirmButton;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    public String IPAddress = "";

    public String CurrentBarcode = null;

    public CountDownTimer cameraCaptureTimer = null;
    public int cameraCaptureCountDownCurrentTime = 0;

    public TextView ocrHelpText;

    public boolean IsCapturePaused = false;

    Camera currentCamera;

    int ImageAutoCaptureCountDownTime = 10;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brand_ocractivity);

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");

        try{
            int ocrAutoCaptureTime = Integer.parseInt(General.getGeneral(this).getSetting(this,"OCRAutoCaptureTime"));
            ImageAutoCaptureCountDownTime = ocrAutoCaptureTime;
            Logger.Debug("SystemControl", "Read Field OCRAutoCaptureTime From System Control, Value: " + ImageAutoCaptureCountDownTime);
        }catch(Exception ex){
            Logger.Error("SystemControl", "Error Getting Value For OCRAutoCaptureTime, " + ex.getMessage());
        }

        ocrHelpText = findViewById(R.id.ocrHelpText);

        captureImageButton = findViewById(R.id.captureImageButton);
        captureImageButton.setEnabled(false);

        pauseCaptureButton = findViewById(R.id.pauseCaptureButton);

        cameraPreviewImageView = findViewById(R.id.cameraPreviewImageView);
        cameraPreviewImageView.setVisibility(View.INVISIBLE);

        confirmButton = findViewById(R.id.confirmButton);

        pauseCaptureButton = findViewById(R.id.pauseCaptureButton);

        cameraPreview = findViewById(R.id.cameraPreview);

        cameraExecutor = Executors.newSingleThreadExecutor();

        captureImageButton.setOnClickListener((click) -> {
            if(CurrentBarcode != null){
                captureImage(true, 0);
            }
        });

        pauseCaptureButton.setOnClickListener((click) -> {
            if(IsCapturePaused){
                IsCapturePaused = false;
                pauseCaptureButton.setImageResource(R.drawable.baseline_pause_icon);
                if(cameraCaptureTimer != null){
                    cameraCaptureTimer.cancel();
                    cameraCaptureTimer = null;
                }
                if(CurrentBarcode != null){
                    ProceedAutomaticImageCapture(CurrentBarcode);
                }
            }else {
                IsCapturePaused = true;
                pauseCaptureButton.setImageResource(R.drawable.baseline_play_arrow_icon);
                ocrHelpText.setText("Automatic Capture Paused, Press The Capture Button To Capture");
                if(cameraCaptureTimer != null){
                    cameraCaptureTimer.cancel();
                    cameraCaptureTimer = null;
                }
            }
        });

        if(cameraPermissionsGranted()){
            startCamera();
        }else {
            requestCameraPermissions();
        }

        captureImageButton.setEnabled(false);
        pauseCaptureButton.setEnabled(false);
        confirmButton.setVisibility(View.INVISIBLE);

        try {
            File FolderPath = new File(
                    getOutputDirectory(),
                    "/OCR/TempFolder/"
            );
            if(FolderPath.exists()){
                DeleteFolderRecursive(FolderPath);
            }
        }catch (Exception ex){
            Logger.Error("OCR", "Failed Deleting Old Temp Folder: " + ex.getMessage());
        }

        confirmButton.setOnClickListener(view -> {

            if(CurrentBarcode != null){
                File FolderPath = new File(
                        getOutputDirectory(),
                        "/OCR/TempFolder/"
                );

                File NewFolderPath = new File(
                        getOutputDirectory(),
                        "/OCR/" + CurrentBarcode + "/"
                );

                if(FolderPath.renameTo(NewFolderPath)){
                    Logger.Debug("OCR", "Saved OCR Images For Item " + CurrentBarcode);
                }else {
                    Logger.Debug("OCR", "Couldn't Save OCR Images For Item " + CurrentBarcode);
                }
            }

            ocrHelpText.setText("Place The Camera On An Item Barcode To Start");
            if(cameraCaptureTimer != null){
                cameraCaptureTimer.cancel();
                cameraCaptureTimer = null;
            }
            CurrentBarcode = null;

            pauseCaptureButton.setImageResource(R.drawable.baseline_pause_icon);
            captureImageButton.setEnabled(false);
            pauseCaptureButton.setEnabled(false);
            IsCapturePaused = false;
            confirmButton.setVisibility(View.INVISIBLE);
        });

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

                //Create the barcode analyzer
                imageAnalyzer = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                imageAnalyzer.setAnalyzer(cameraExecutor, image -> {
                    recognizeBarcode(image);
                });



                currentCamera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture, imageAnalyzer);

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
                                DisplayOrientedMeteringPointFactory factory = new DisplayOrientedMeteringPointFactory(getDisplay(), currentCamera.getCameraInfo(), (float)cameraPreviewImageView.getWidth(), (float)cameraPreviewImageView.getHeight());
                                MeteringPoint autoFocusPoint = factory.createPoint(motionEvent.getX(), motionEvent.getY());

                                currentCamera.getCameraControl().startFocusAndMetering(
                                        new FocusMeteringAction.Builder(
                                                autoFocusPoint,
                                                FocusMeteringAction.FLAG_AF
                                        ).disableAutoCancel().build());
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
    public void captureImage(boolean focus, int trail) {

        File FolderPath = new File(
                getOutputDirectory(),
                "/OCR/TempFolder/"
        );

        FolderPath.mkdirs();

        String fileNameFormatStr = new SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS", Locale.US
        ).format(System.currentTimeMillis()) + ".jpg";

        File photoFile = new File(
                FolderPath,
                fileNameFormatStr
        );

        if(focus){
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
                                    }
                                });

                                ImageCapture.OutputFileOptions outputFileOptions =
                                        new ImageCapture.OutputFileOptions.Builder(photoFile).build();
                                imageCapture.takePicture(outputFileOptions, cameraExecutor,
                                        new ImageCapture.OnImageSavedCallback() {
                                            @Override
                                            public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        cameraPreviewImageView.setVisibility(View.INVISIBLE);
                                                        Logger.Debug("CAMERA", "CaptureImage - Image Saved Successfully");
                                                        confirmButton.setVisibility(View.VISIBLE);
                                                        confirmButton.setEnabled(true);
                                                    }
                                                });
                                            }
                                            @Override
                                            public void onError(ImageCaptureException error) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        cameraPreviewImageView.setVisibility(View.INVISIBLE);
                                                    }
                                                });
                                                Snackbar.make(findViewById(R.id.brandOCRActiviyLayout), "Failed Capturing Image", Snackbar.LENGTH_LONG)
                                                        .setAction("No action", null).show();
                                                Logger.Error("CAMERA", "CaptureImage - Image Failed To Save: " + error.getMessage());
                                            }
                                        }
                                );
                            }else {
                                if(trail < 2){
                                    captureImage(true, trail + 1);
                                    Logger.Debug("Camera", "Couldn't Auto Focus Camera, Retrying");
                                }else {
                                    captureImage(false, 0);
                                    Logger.Debug("Camera", "Couldn't Auto Focus Camera, Capturing Image With No Focus");
                                }
                            }
                        } catch (Exception ex) {
                            Logger.Error("Camera", "cameraFocusListener - " + ex.getMessage());
                            captureImage(false, 0);
                        }
                    }
                }, cameraExecutor);

            } catch (Exception e) {
                Logger.Error("Camera", "cameraFocus - " + e.getMessage());
                captureImage(false, 0);
            }
        }else {
            try{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cameraPreviewImageView.setImageBitmap(cameraPreview.getBitmap());
                        cameraPreviewImageView.setVisibility(View.VISIBLE);
                    }
                });
            }catch(Exception ex){
                Logger.Error("CAMERA", "Error Showing Camera Preview: " + ex.getMessage());
            }

            ImageCapture.OutputFileOptions outputFileOptions =
                    new ImageCapture.OutputFileOptions.Builder(photoFile).build();
            imageCapture.takePicture(outputFileOptions, cameraExecutor,
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    cameraPreviewImageView.setVisibility(View.INVISIBLE);
                                    Logger.Debug("CAMERA", "CaptureImage - Image Saved Successfully");
                                    confirmButton.setVisibility(View.VISIBLE);
                                    confirmButton.setEnabled(true);
                                }
                            });
                        }
                        @Override
                        public void onError(ImageCaptureException error) {
                            cameraPreviewImageView.setVisibility(View.INVISIBLE);
                            Snackbar.make(findViewById(R.id.brandOCRActiviyLayout), "Failed Capturing Image", Snackbar.LENGTH_LONG)
                                    .setAction("No action", null).show();
                            Logger.Error("CAMERA", "CaptureImage - Image Failed To Save: " + error.getMessage());
                        }
                    }
            );
        }
    }


    /**
     * This function forces the camera to focus
     */
    public void cameraForceFocus(int trail) {
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

                            }else {
                                if(trail < 2){
                                    cameraForceFocus(trail + 1);
                                    Logger.Debug("Camera", "cameraForceFocus - Couldn't Force Focus Camera, Retrying");
                                }else {
                                    Logger.Debug("Camera", "cameraForceFocus - Couldn't Force Focus Camera");
                                }
                            }
                        } catch (Exception ex) {
                            Logger.Error("Camera", "cameraForceFocus - cameraFocusListener - " + ex.getMessage());
                        }
                    }
                }, cameraExecutor);

            } catch (Exception e) {
                Logger.Error("Camera", "cameraForceFocus - " + e.getMessage());
            }
    }

    /**
     * Barcode recognition system
     */
    //This is used to verify that the barcode is being detected correctly
    private int CurrentDetectedBarcodeCount = 0;
    private int RequiredDetectedBarcodeCount = 3;

    private String lastDetectedBarcode = "";

    BarcodeScanner barcodeScanner = null;

    /**
     * Recognize the barcode using google mobile vision kit
     * @param proxy
     */
    @SuppressLint("UnsafeOptInUsageError")
    public void recognizeBarcode(ImageProxy proxy){

        Image mediaImage = proxy.getImage();
        if(mediaImage == null)
            return;

        InputImage image = InputImage.fromMediaImage(mediaImage, proxy.getImageInfo().getRotationDegrees());

        //This is changed from the previous version because it spammed an error in the console, this should be initiated only once
        if(barcodeScanner == null) {
            BarcodeScannerOptions.Builder options = new BarcodeScannerOptions.Builder();
            options.setBarcodeFormats(Barcode.FORMAT_UPC_A, Barcode.FORMAT_EAN_13, Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_93);
            barcodeScanner = BarcodeScanning.getClient(options.build());
        }

        barcodeScanner.process(image).addOnSuccessListener(barcodes -> {
            if(CurrentBarcode == null && barcodes.size() == 1){
                String barcode = barcodes.get(0).getRawValue().toUpperCase(Locale.getDefault());
                if(barcode.length() == 13 && barcode.startsWith("IS")){

                    //This means that the current detected barcode is the same as the last one detected
                    if(barcode.equals(lastDetectedBarcode)){
                        CurrentDetectedBarcodeCount++;
                    }else {//If the barcode is not the same as the last one then reset the count to get a more stable result
                        CurrentDetectedBarcodeCount = 0;
                        lastDetectedBarcode = barcode;
                    }

                    //If the current barcode reached the required amount to be a stable barcode we proceed
                    if(CurrentDetectedBarcodeCount == RequiredDetectedBarcodeCount){
                        CurrentDetectedBarcodeCount = 0;
                        lastDetectedBarcode = "";

                        if(CurrentBarcode == null || CurrentBarcode.isEmpty()){
                            CheckBarcodePreviousOCR(barcode);
                            CurrentBarcode = barcodes.get(0).getRawValue();
                        }
                    }

                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Logger.Error("CAMERA", "Camera barcode analyzer failed analyzing image, Error: " + e.getMessage());
            }
        }).addOnCompleteListener(task -> {
                proxy.close();
        });


    }

    /**
     * This function checks if the current detected barcode has ocr done already or not, if not it proceeds to the image captures
     * @param barcode
     */
    public void CheckBarcodePreviousOCR(String barcode){
        Logger.Debug("API", "CheckBarcodePreviousOCR - Detected Barcode: " + barcode + " , Checking for Previous OCR Data");
        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.CheckBarcodeOCRDone(barcode)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    if(s){
                                        //Ocr was already done for this item, ask the user to pick a new item
                                        ShowAlertDialog("Error", "Error CheckBarcodeOCRDone For Item: " + barcode);
                                        CurrentBarcode = null;
                                    }else {
                                        //The item was not passed through the ocr process before so we start capturing images
                                        ProceedToImageCaptures(barcode);
                                    }
                                }
                            }, (throwable) -> {
                                Logger.Error("API", "CheckBarcodePreviousOCR - Error In Response: " + throwable.getMessage());
                                ShowSnackBar("Server Error: " + throwable.getMessage(), Snackbar.LENGTH_LONG);
                            }));
        } catch (Throwable e) {
            Logger.Error("API", "CheckBarcodePreviousOCR - Error Connecting: " + e.getMessage());
            CurrentBarcode = null;
            ShowSnackBar("Connection Error Occurred", Snackbar.LENGTH_LONG);
        }
    }

    /**
     * Start waiting for image captures and wait for the user input to finish
     * @param barcode
     */
    public void ProceedToImageCaptures(String barcode){
        Logger.Debug("CAMERA", "ProceedToImageCaptures - Starting Image Captures For Barcode: " + barcode);
        ocrHelpText.setText("Starting Image Capture For " + barcode);
        captureImageButton.setEnabled(true);
        pauseCaptureButton.setEnabled(true);
        IsCapturePaused = false;
        confirmButton.setVisibility(View.INVISIBLE);
        pauseCaptureButton.setImageResource(R.drawable.baseline_pause_icon);

        ProceedAutomaticImageCapture(barcode);

    }

    /**
     * This function is used to capture images automatically
     * @param barcode
     */
    public void ProceedAutomaticImageCapture(String barcode){
        cameraCaptureCountDownCurrentTime = ImageAutoCaptureCountDownTime;

        cameraCaptureTimer = new CountDownTimer(1000 * ImageAutoCaptureCountDownTime, 1000) {
            @Override
            public void onTick(long l) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cameraCaptureCountDownCurrentTime--;
                        if(cameraCaptureCountDownCurrentTime == 1){
                            ocrHelpText.setText("Capturing Image Automatically Now");
                        }else if(cameraCaptureCountDownCurrentTime > 1){
                            ocrHelpText.setText("Capturing Image Automatically In " + cameraCaptureCountDownCurrentTime + " Seconds...");
                        }
                    }
                });
                if(cameraCaptureCountDownCurrentTime == 1){
                    captureImage(true, 0);
                }
            }

            @Override
            public void onFinish() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ocrHelpText.setText("Image Captured, Capturing Another Image");
                        cameraCaptureCountDownCurrentTime = 0;
                    }
                });
                ProceedAutomaticImageCapture(barcode);
            }
        }.start();
    }

    /**
     * Clean up the camera when the activity is destroyed
     */
    @Override
    protected void onDestroy() {
        cameraExecutor.shutdown();
        if(cameraCaptureTimer != null) {
            cameraCaptureTimer.cancel();
            cameraCaptureTimer = null;
        }
        super.onDestroy();
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
     * This is used to get the output directory of the application were we can save a temp image of the camera capture
     * @return File path
     */
    private File getOutputDirectory() {
        File[] allMediaDirs = getExternalMediaDirs();
        File mediaDir = allMediaDirs.length > 0 ? allMediaDirs[0] : null;
        if(mediaDir == null) {
            new File(getResources().getString(R.string.app_name)).mkdirs();
        }

        return (mediaDir != null && mediaDir.exists()) ? mediaDir : getFilesDir();
    }

    public void ShowSnackBar(String message, int length){
        Snackbar.make(findViewById(R.id.brandOCRActiviyLayout), message, length)
                .setAction("No action", null).show();
    }

    public void ShowAlertDialog(String title, String message){
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * This function deletes a folder and all its subdirectories and files recursively
     * @param fileOrDirectory
     */
    public void DeleteFolderRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                DeleteFolderRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

}