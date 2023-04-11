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
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.storage.Storage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.rfidread.Connect.BaseConnect;
import com.rfidread.Enumeration.eReadType;
import com.rfidread.Helper.Helper_ThreadPool;
import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.Models.Tag_Model;
import com.rfidread.RFIDReader;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Remote.APIClient;
import Remote.BasicApi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class PasBrandOCRActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1000;

    private PreviewView cameraPreview;
    private ImageView cameraPreviewImageView;

    private ImageButton captureImageButton, pauseCaptureButton, confirmButton;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    public String IPAddress = "";

    public String CurrentBarcode = null;

    public CountDownTimer cameraCaptureTimer = null;
    public int cameraCaptureCountDownCurrentTime = 0;

    public TextView ocrHelpText;

    public boolean IsCapturePaused = false;

    ProgressDialog mainProgressDialog;

    Camera currentCamera;

    //In Seconds
    int ImageAutoCaptureCountDownTime = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pas_brand_ocractivity);

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

            ocrHelpText.setText("Scan An Item's RFID To Start");
            if(cameraCaptureTimer != null){
                cameraCaptureTimer.cancel();
                cameraCaptureTimer = null;
            }
            CurrentBarcode = null;
            lastDetectedRFIDTag = "";

            pauseCaptureButton.setImageResource(R.drawable.baseline_pause_icon);
            captureImageButton.setEnabled(false);
            pauseCaptureButton.setEnabled(false);
            IsCapturePaused = false;
            confirmButton.setVisibility(View.INVISIBLE);

            RFIDStartRead();

        });

        AttemptRFIDReaderConnection();

        mainProgressDialog = ProgressDialog.show(this, "",
                "Connecting To Sled, Please wait...", true);
        mainProgressDialog.show();

        if(cameraPermissionsGranted()){
            startCamera();
        }else {
            requestCameraPermissions();
        }

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
     * This function gets the Item Serial From The RFID Tag
     * @param rfid
     */
    public void CheckDetectedRFIDTag(String rfid){
        Logger.Debug("API", "CheckBarcodePreviousOCR - Detected RFID: " + rfid + " Checking IS Now");
        RFIDStopRead();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ocrHelpText.setText("Reading The IS Please Wait...");
            }
        });
        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.GetRFIDItemSerial(rfid)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ocrHelpText.setText("Checking The IS OCR Status");
                                        }
                                    });
                                    CheckBarcodePreviousOCR(s);
                                    CurrentBarcode = s;
                                }
                            }, (throwable) -> {
                                RFIDStartRead();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ocrHelpText.setText("Scan An Item's RFID To Start");
                                    }
                                });
                                Logger.Error("API", "CheckDetectedRFIDTag - Error In Response: " + throwable.getMessage());
                                ShowSnackBar("Server Error: " + throwable.getMessage(), Snackbar.LENGTH_LONG);
                            }));
        } catch (Throwable e) {
            Logger.Error("API", "CheckDetectedRFIDTag - Error Connecting: " + e.getMessage());
            CurrentBarcode = null;
            ShowSnackBar("Connection Error Occurred", Snackbar.LENGTH_LONG);
            RFIDStartRead();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ocrHelpText.setText("Scan An Item's RFID To Start");
                }
            });
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
        Snackbar.make(findViewById(R.id.pasBrandOCRActiviyLayout), message, length)
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

    /**
     * Bluetooth rfid reader info
     */

    String RFIDMac = "";

    List<BluetoothDevice> BluetoothDeviceList = new ArrayList();
    List<String> BluetoothDevicelistStr = new ArrayList();
    List<String> BluetoothDevicelistMac = new ArrayList();

    Boolean IsRFIDConnected = false;

    String RFIDName = "";

    String lastDetectedRFIDTag = "";

    /**
     * This function returns all the Bluetooth paired devices
     * @return
     */
    @SuppressLint("MissingPermission")
    public List<String> GetBT4DeviceStrList() {

        BluetoothDeviceList.clear();
        BluetoothDevicelistStr.clear();
        BluetoothDevicelistMac.clear();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            Iterator var1 = pairedDevices.iterator();

            while (var1.hasNext()) {
                BluetoothDevice device = (BluetoothDevice) var1.next();
                try {
                    BluetoothDeviceList.add(device);
                    BluetoothDevicelistStr.add(device.getName());
                    BluetoothDevicelistMac.add(device.getAddress());
                } catch (Exception var4) {
                }
            }
        }

        return BluetoothDevicelistStr;
    }

    /**
     * Gets the bluetooth device name from its mac address
     * @param Mac
     * @return
     */
    @SuppressLint("MissingPermission")
    private String GetBluetoothDeviceNameFromMac(String Mac) {
        if (!(Mac != null && !Mac.isEmpty())) {
            return "";
        }
        GetBT4DeviceStrList();
        for (BluetoothDevice d : BluetoothDeviceList) {
            if (d.getAddress() != null && d.getAddress().contains(Mac))
                return d.getName();
            //something here
        }
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(Mac);
        if (device == null)
            return "";
        return device.getName();
    }

    /**
     * This function attempts the rfid reader connection with the corresponding mac address
     */
    public void AttemptRFIDReaderConnection() {
        try {
            GetBT4DeviceStrList();
            Storage mStorage = new Storage(this);//sp存储
            RFIDMac = mStorage.getDataString("RFIDMac", "00:15:83:3A:4A:26");
            RFIDName = GetBluetoothDeviceNameFromMac(RFIDMac);
            new CountDownTimer(200, 200) {
                public void onTick(long l) {

                }

                public void onFinish() {
                    ConnectToRFIDReader();
                }
            }.start();
        } catch (Exception ex) {
            Logger.Error("RFID-READER", "AttemptRFIDReaderConnection - " + ex.getMessage());
        }

    }

    /**
     * This function initializes the connection to the rfid reader
     */
    private void ConnectToRFIDReader() {
        try {
            RFIDReader.CloseAllConnect();
            RFIDReader.GetBT4DeviceStrList();
            if (RFIDName != null && !RFIDName.isEmpty()) {
                if (!RFIDReader.CreateBT4Conn(RFIDName, new IAsynchronousMessage() {
                    @Override
                    public void WriteDebugMsg(String s) {

                    }

                    @Override
                    public void WriteLog(String s) {

                    }

                    @Override
                    public void PortConnecting(String s) {

                    }

                    @Override
                    public void PortClosing(String s) {

                    }

                    @Override
                    public void OutPutTags(Tag_Model model) {
                        String key = TagModelKey(model);
                        if(!key.isEmpty()){
                            if(lastDetectedRFIDTag != key){
                                lastDetectedRFIDTag = key;
                                CheckDetectedRFIDTag(key);
                            }
                        }

                    }

                    @Override
                    public void OutPutTagsOver() {

                    }

                    @Override
                    public void GPIControlMsg(int i, int i1, int i2) {

                    }

                    @Override
                    public void OutPutScanData(byte[] bytes) {

                    }
                })) {
                    Logger.Error("RFID-READER", "ConnectToRFIDReader - Error Connecting to " + RFIDMac);
                } else {
                    Logger.Debug("RFID-READER", "ConnectToRFIDReader - RFID-" + RFIDMac + " Connected Successfully");
                    OnRFIDReaderConnected();
                    RFIDReader._Config.SetReaderAutoSleepParam(RFIDName, false, "");
                }
            }
            int RFIDCount = RFIDReader.HP_CONNECT.size();
            HashMap<String, BaseConnect> lst = RFIDReader.HP_CONNECT;
            if (lst.keySet().stream().count() > 0) {
                IsRFIDConnected = true;
            } else {
                OnRFIDReaderError("Couldn't Connect To Sled With Mac " + RFIDMac);
                Logger.Error("RFID-READER", "ConnectToRFIDReader - Couldn't Connect To Sled");
            }
        } catch (Exception ex) {
            OnRFIDReaderError(ex.getMessage());
            Logger.Error("RFID-READER", "ConnectToRFIDReader - " + ex.getMessage());
        }
    }

    /**
     * This function returns the rfid key of a tag
     * @param model
     * @return
     */
    public String TagModelKey(Tag_Model model) {
        if (model._EPC != null && model._TID != null && !model._EPC.isEmpty() && !model._TID.isEmpty())
            return model._EPC + "-" + model._TID;
        return "";
    }

    /**
     * This function is called when a rfid sled is connected successfully
     */
    public void OnRFIDReaderConnected(){
        mainProgressDialog.cancel();
        RFIDStartRead();
    }

    /**
     * This function is called when the rfid sled connection fails
     */
    public void OnRFIDReaderError(String error){
        mainProgressDialog.cancel();
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Connection Error")
                .setMessage(error)
                .setNegativeButton("Close", (dialogInterface, i) ->
                {
                    finish();
                })
                .show();
    }

    /**
     * This function configures the RFID reader to read EPC 6C/6B Tags
     */
    public void RFIDStartRead() {


        Helper_ThreadPool.ThreadPool_StartSingle(new Runnable() {
            @Override
            public void run() {
                try {
                    if (General._IsCommand6Cor6B.equals("6C")) {// read 6c tags
                        GetEPC_6C();
                    } else {// read 6b tags
                        GetEPC_6B();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * This function stop the configured rfid readings
     */
    public void RFIDStopRead() {
        try {
            RFIDReader._Config.Stop(RFIDName);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Logger.Error("RFID-READER", "RFIDStopRead - Error While Stopping The Read Command: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private int GetEPC_6C() {
        String RFID = RFIDName;
        int ret = -1;
        if (!RFIDReader.HP_CONNECT.containsKey(RFID))
            return ret;
        ret = RFIDReader._Tag6C.GetEPC_TID(RFID, 1, eReadType.Inventory);
        return ret;
    }

    private int GetEPC_6B() {
        String RFID = RFIDName;
        int ret = -1;
        if (!RFIDReader.HP_CONNECT.containsKey(RFID))
            return ret;
        ret = RFIDReader._Tag6B.Get6B(RFID, 1, eReadType.Inventory.GetNum(), 0);
        return ret;
    }

}