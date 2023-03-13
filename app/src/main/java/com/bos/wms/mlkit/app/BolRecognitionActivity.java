package com.bos.wms.mlkit.app;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.DisplayOrientedMeteringPointFactory;
import androidx.camera.core.FocusMeteringAction;
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

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Model.BolRecognitionModel;
import Remote.APIClient;
import Remote.BasicApi;
import Remote.VolleyMultipartRequest;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class BolRecognitionActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1000, BLUETOOTH_PERMISSION_REQUEST_CODE = 1001;

    private PreviewView cameraPreview;
    private ImageView cameraPreviewImageView;
    private ImageButton captureImageButton, scanUPCSButton, printerImageButton;

    private FirebaseAuth firebaseAuh;
    private FirebaseFunctions firebaseFunctions;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    public static int MinScannedItemsUPC = 4;

    public String IPAddress = "";

    public TextView bolHelpText;

    public ZebraPrinter printer;

    public String[] currentPrintData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bol_recognition);

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
        printer = new ZebraPrinter(mStorage.getDataString("PrinterMacAddress", "00"));

        //Initialize firebase and the the firebase authenticator
        FirebaseApp.initializeApp(this);
        firebaseAuh = FirebaseAuth.getInstance();
        CloudLogin();

        bolHelpText = findViewById(R.id.bolHelpText);

        captureImageButton = findViewById(R.id.captureImageButton);
        captureImageButton.setEnabled(false);

        printerImageButton = findViewById(R.id.printerImageButton);
        printerImageButton.setImageResource(R.drawable.baseline_print_disabled_icon);

        //This will allow us to enable the printer in the UI first, then we can double check if it is still connecting after the first connection
        if(ZebraPrinter.isFirstConnectionEstablished()){
            printerImageButton.setImageResource(R.drawable.baseline_print_icon);
        }else {
            printerImageButton.setImageResource(R.drawable.baseline_print_disabled_icon);
        }

        printer.AttemptConnection((result) -> {
            if(result){ //Connection Established
                printerImageButton.setImageResource(R.drawable.baseline_print_icon);
                ZebraPrinter.setFirstConnectionEstablished(true);
            }else {
                printerImageButton.setImageResource(R.drawable.baseline_print_disabled_icon);
                ZebraPrinter.setFirstConnectionEstablished(false);
            }
        });

        printer.setOnPrinterConnectionFailListener((result) -> {
            if(!result){ //Connection Failed
                printerImageButton.setImageResource(R.drawable.baseline_print_disabled_icon);
                ZebraPrinter.setFirstConnectionEstablished(false);
            }
        });

        //Open the mac dialog to change the mac address
        printerImageButton.setOnLongClickListener((click) -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Please scan or type the printer's mac address");

            final EditText input = new EditText(this);

            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);
            builder.setPositiveButton("Pair", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ZebraPrinter.AttemptConnection(input.getText().toString(), (result) -> {
                        if(result){ //Connection Established
                            printerImageButton.setImageResource(R.drawable.baseline_print_icon);
                            ZebraPrinter.setFirstConnectionEstablished(true);
                            mStorage.saveData("PrinterMacAddress", input.getText().toString());
                            printer.setMacAddress(input.getText().toString());
                            Snackbar.make(findViewById(R.id.bolRecognitionActiviyLayout), "Printer Paired Successfully", Snackbar.LENGTH_LONG)
                                    .setAction("No action", null).show();
                        }else {
                            //printerImageButton.setImageResource(R.drawable.baseline_print_disabled_icon);
                            //printerEnabled = false; This isnt needed because if the old printer was enabled we need to be able to reprint to it
                            Snackbar.make(findViewById(R.id.bolRecognitionActiviyLayout), "Printer Failed To Pair", Snackbar.LENGTH_LONG)
                                    .setAction("No action", null).show();
                        }
                    });
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
            input.requestFocus();
            return false;
        });

        printerImageButton.setOnClickListener((click) -> {
            if(ZebraPrinter.isFirstConnectionEstablished()){
                if(currentPrintData != null && currentPrintData.length == 2){
                    printer.printBolData(currentPrintData[0], currentPrintData[1]);
                }
            }else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Please scan or type the printer's mac address");

                final EditText input = new EditText(this);

                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                builder.setPositiveButton("Pair", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ZebraPrinter.AttemptConnection(input.getText().toString(), (result) -> {
                            if(result){ //Connection Established
                                printerImageButton.setImageResource(R.drawable.baseline_print_icon);
                                ZebraPrinter.setFirstConnectionEstablished(true);
                                mStorage.saveData("PrinterMacAddress", input.getText().toString());
                                printer.setMacAddress(input.getText().toString());
                                Snackbar.make(findViewById(R.id.bolRecognitionActiviyLayout), "Printer Paired Successfully", Snackbar.LENGTH_LONG)
                                        .setAction("No action", null).show();
                            }else {
                                printerImageButton.setImageResource(R.drawable.baseline_print_disabled_icon);
                                ZebraPrinter.setFirstConnectionEstablished(false);
                                Snackbar.make(findViewById(R.id.bolRecognitionActiviyLayout), "Printer Failed To Pair", Snackbar.LENGTH_LONG)
                                        .setAction("No action", null).show();
                            }
                        });
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
                input.requestFocus();
            }
        });

        cameraPreviewImageView = findViewById(R.id.cameraPreviewImageView);
        cameraPreviewImageView.setVisibility(View.INVISIBLE);

        scanUPCSButton = findViewById(R.id.scanUPCSButton);

        cameraPreview = findViewById(R.id.cameraPreview);

        cameraExecutor = Executors.newSingleThreadExecutor();

        captureImageButton.setOnClickListener((click) -> {
            //pickImage();
            captureImage();
        });

        scanUPCSButton.setOnClickListener((click) -> {
            Intent intent = new Intent(this, ScanUPCsForBolActivity.class);
            startActivity(intent);
        });

        if(cameraPermissionsGranted()){
            startCamera();
        }else {
            requestCameraPermissions();
        }

        if(!bluetoothPermissionsGranted()){
            requestBluetoothPermissions();
        }

        APICheck();

    }

    /* OCR Processing */

    /**
     * Process the text to gather the bol number by splitting the text to lines
     * Splitting the lines by spaces
     * In some cases the spaces might be \r so to be safe we split the result of the spaces split by \r
     * then we clear any unwanted characters like text that might be read along the line
     * then we add the bol to an array after checking that its 8 numbers to send to the respective api and check its validity
     *
     * Note: Since we are splitting the line by spaces and we are looping through all the chunks of letter/numbers left
     * We can skip the part for the text filtering (removing unwanted characters and leaving numbers) and check if the chunk of characters we have
     * are only numbers for better number accuracy. But in some cases we might not know what the Google Vision OCR might return. So for that reason this is
     * going to stay for better results accuracy.
     * @param text The actual text received from the google vision api
     * @param fileName This is just an identifier to know which image belongs to which item in the database
     */
    public void processMultipleOCRData(File fileName, String text){
        String[] lines = text.split("\n");
        ArrayList<String> bols = new ArrayList<String>();
        for(String line : lines){//Bot/3812321
            String[] args = line.split(" ");
            for(String arg : args){
                String[] args2 = arg.split("\r");
                for(String  arg2 : args2){
                    arg2 = arg2.replaceAll("/", "1").replaceAll("[^0-9]", "");
                    if(arg2.length() == 8 && !bols.contains(arg2)){
                        bols.add(arg2);
                    }
                }
            }
        }
        processBOLNumber(fileName, text, bols.toArray(new String[0]));
    }

    /**
     * Process a list of bol numbers or single bol number by sending the numbers to the ValidateBol api and verifying that the bol
     * number is valid.
     *
     * If The number is invalid the API will return a BadRequest result and we can show the error message to the user
     *
     * The Api returns the Box Serial as well, and the bol number + the box serial will automatically be printed via the agent's portable printer
     * @param bols A Single Bol, Or An Array Of Bols To Process
     * @param text The original text is needed in case the orc failed to analyze any bols we can upload to the database
     * @param fileName The original image file to upload incase the analysis fails
     */
    public void processBOLNumber(File fileName, String text, String... bols){
        Logger.Debug("OCR", "ProcessBOLNumber - Detected BOLS " + Arrays.toString(bols));

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.ValidateBol(General.getGeneral(getApplicationContext()).UserID, Arrays.asList(bols))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try{
                                        JSONObject json = new JSONObject(s.string());
                                        String formattedResult = "BOL: " + json.getString("bol") + " Box Serial: " + json.getString("boxSerial");

                                        currentPrintData = new String[]{
                                                json.getString("bol"),
                                                json.getString("boxSerial")
                                        };
                                        if(ZebraPrinter.isFirstConnectionEstablished()) {
                                            //Print Bol Number And Serial
                                            printer.printBolData(json.getString("bol"), json.getString("boxSerial"));
                                        }
                                        Logger.Debug("API", "ProcessBOLNumber - Analyzed Data " + formattedResult);
                                        bolHelpText.setText(formattedResult);

                                    }catch(Exception ex){
                                        /*Snackbar.make(findViewById(R.id.mainActivityLayout), "Result Parsing Failed", Snackbar.LENGTH_LONG)
                                                .setAction("No action", null).show();*/
                                        /*new AlertDialog.Builder(this)
                                                .setTitle("Error")
                                                .setMessage(s.string())
                                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int which) {

                                                    }
                                                })
                                                .setIcon(android.R.drawable.ic_dialog_alert)
                                                .show();*/
                                        currentPrintData = null;
                                        bolHelpText.setText(s.string());

                                        Bitmap image = BitmapFactory.decodeFile(fileName.getAbsolutePath());
                                        uploadDebugImage(text, Arrays.toString(bols), s.string(), image, fileName, 0);
                                        boolean delete = fileName.delete();
                                        Logger.Debug("IMAGE", "processBOLNumber - Image Of Failed Bol Recognition Uploaded, Deleted Image File? " + delete);

                                        Logger.Debug("API", "ProcessBOLNumber - Returned Error " + s.string());
                                    }

                                }
                            }, (throwable) -> {
                                currentPrintData = null;
                                if(throwable instanceof HttpException){
                                    HttpException ex = (HttpException) throwable;
                                    /*new AlertDialog.Builder(this)
                                            .setTitle("Error")
                                            .setMessage(ex.response().errorBody().string())
                                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {

                                                }
                                            })
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .show();*/
                                    String response = ex.response().errorBody().string();
                                    if(response.isEmpty()){
                                        response = "API Error Occurred";
                                    }
                                    Logger.Debug("API", "ProcessBOLNumber - Returned HTTP Error " + response);
                                    bolHelpText.setText(response);
                                    Bitmap image = BitmapFactory.decodeFile(fileName.getAbsolutePath());
                                    uploadDebugImage(text, Arrays.toString(bols), response, image, fileName, 0);
                                    boolean delete = fileName.delete();
                                    Logger.Debug("IMAGE", "processBOLNumber - Image Of Failed Bol Recognition Uploaded, Deleted Image File? " + delete);
                                }else {
                                    bolHelpText.setText("Press one of the options below to start");
                                    Snackbar.make(findViewById(R.id.bolRecognitionActiviyLayout), "Internal Error Occurred", Snackbar.LENGTH_LONG)
                                            .setAction("No action", null).show();
                                    Logger.Error("API", "ProcessBOLNumber - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                }
                            }));

        } catch (Throwable e) {
            currentPrintData = null;
            bolHelpText.setText("Press one of the options below to start");
            Logger.Error("API", "ProcessBOLNumber - Error Connecting: " + e.getMessage());
            Snackbar.make(findViewById(R.id.bolRecognitionActiviyLayout), "Connection To Server Failed!", Snackbar.LENGTH_LONG)
                    .setAction("No action", null).show();
        }
    }


    /* Camera Processing */

    /*public void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 100);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                //Display an error
                return;
            }
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                uploadImage(bitmap);
            }catch(Exception ex){
                Log.e("PICKIMAGE", "File not found");
            }
        }
    }*/

    /**
     * Start the back camera, and build the preview for the user to see
     */
    public void startCamera(){
        captureImageButton.setEnabled(true);
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

                Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageCapture, preview);
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
                                DisplayOrientedMeteringPointFactory factory = new DisplayOrientedMeteringPointFactory(getDisplay(), camera.getCameraInfo(), (float)cameraPreviewImageView.getWidth(), (float)cameraPreviewImageView.getHeight());
                                MeteringPoint autoFocusPoint = factory.createPoint(motionEvent.getX(), motionEvent.getY());

                                camera.getCameraControl().startFocusAndMetering(
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
     * Get the LotIdentificationMinUpcs from the SystemControl table by using the GetSystemControlValue api
     * After the value is receiving we update the MinScannedItemsUPC value to match that of the SystemControl table
     */
    public void APICheck(){
        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.GetSystemControlValue("LotIdentificationMinUpcs")
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try{
                                        MinScannedItemsUPC = Integer.parseInt(s.toString());
                                    }catch(Exception ex){
                                        Logger.Error("API", "APICheck - LotIdentificationMinUpcs Returned Error: " + ex.getMessage());
                                    }

                                }
                            }, (throwable) -> {
                                Logger.Error("API", "APICheck - Error In Response: " + throwable.getMessage());
                            }));
        } catch (Throwable e) {
            Logger.Error("API", "APICheck - Error Connecting: " + e.getMessage());
        }
    }

    /**
     * Capture an image and upload it to the Google Vision api
     */
    public void captureImage() {

        captureImageButton.setEnabled(false);

        File FolerPath = new File(
                getOutputDirectory(),
                "/Images/"
        );

        FolerPath.mkdirs();

        String fileNameFormatStr = new SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS", Locale.US
        ).format(System.currentTimeMillis()) + ".jpg";

        File photoFile = new File(
                FolerPath,
                fileNameFormatStr
        );

        cameraPreviewImageView.setImageBitmap(cameraPreview.getBitmap());
        cameraPreviewImageView.setVisibility(View.VISIBLE);

        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(outputFileOptions, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                        Bitmap image = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                        uploadImage(photoFile, image);
                        Logger.Debug("CAMERA", "CaptureImage - Image Saved And Uploaded");
                    }
                    @Override
                    public void onError(ImageCaptureException error) {
                        cameraPreviewImageView.setVisibility(View.INVISIBLE);
                        captureImageButton.setEnabled(true);
                        Snackbar.make(findViewById(R.id.bolRecognitionActiviyLayout), "Failed Capturing Image", Snackbar.LENGTH_LONG)
                                .setAction("No action", null).show();
                        Logger.Error("CAMERA", "CaptureImage - Image Failed To Saved: " + error.getMessage());
                    }
                }
        );

    }

    /**
     * Clean up the camera when the activity is destroyed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
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

    /**
     * Check if the bluetooth permissions are granted for the portable printer
     * @return
     */
    public boolean bluetoothPermissionsGranted(){
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request the camera permissions from the user
     */
    public void requestBluetoothPermissions(){
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.BLUETOOTH_ADMIN}, BLUETOOTH_PERMISSION_REQUEST_CODE);
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
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Logger.Debug("BLUETOOTH", "OnRequestPermissionsResult - Bluetooth Can Pair, User Granted Permissions");
                startCamera();
            } else {
                Toast.makeText(this, "Can't use portable printer", Toast.LENGTH_LONG).show();
                Logger.Error("BLUETOOTH", "OnRequestPermissionsResult - User Failed To Grant Bluetooth Permissions");
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

    public void uploadDebugImage(String text, String detectedBols, String errorText, Bitmap bitmap, File fileName, int trails){
        if(trails == 0){
            Logger.Debug("IMAGE", "uploadDebugImage - Starting Upload For: " + fileName.getAbsolutePath());
        }
        if(trails>1000){
            Logger.Error("IMAGE", "uploadDebugImage - Trails reached 1000");;
            return;
        }

        String ipAddress = "http://" + IPAddress + (IPAddress.endsWith("/") ? "" : "/");

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, ipAddress + "api/BolRecognition/UploadImage", new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(response.toString().contains("Success")){
                    try {
                        BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
                        CompositeDisposable compositeDisposable = new CompositeDisposable();
                        compositeDisposable.addAll(
                                api.ProceedBolFailedUpload(new BolRecognitionModel(fileName.getName(), text, detectedBols, errorText, General.getGeneral(getApplicationContext()).UserID))
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe((s) -> {
                                            if(s != null){
                                                try{
                                                    Logger.Debug("API", "uploadDebugImage - ProceedBolFailedUpload Returned Value: " + s);
                                                }catch(Exception ex){
                                                    Logger.Error("API", "uploadDebugImage - Returned Error: " + ex.getMessage());
                                                }
                                            }
                                        }, (throwable) -> {
                                            Logger.Error("API", "uploadDebugImage - Error In Response: " + throwable.getMessage());
                                        }));
                    } catch (Throwable e) {
                        Logger.Error("API", "APITest - Error Connecting: " + e.getMessage());
                    }
                }else {
                    Logger.Error("IMAGE", "uploadDebugImage - Got Image Upload Response: " + response.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                String errorMessage = "Unknown error";
                if (networkResponse == null) {
                    if (error.getClass().equals(TimeoutError.class)) {
                        errorMessage = "Request timeout";
                    } else if (error.getClass().equals(NoConnectionError.class)) {
                        errorMessage = "Failed to connect server";
                    }
                    uploadDebugImage(text, detectedBols, errorText, bitmap, fileName, trails + 1);
                } else {
                    String result = new String(networkResponse.data);
                    try {
                        errorMessage = result;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if(errorMessage.isEmpty()){
                    errorMessage = error.getMessage();
                }
                Logger.Error("IMAGE", "uploadDebugImage - Response Error: " + errorMessage);
                error.printStackTrace();
            }
        }) {

            @Override
            public Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                params.put("image", new DataPart(fileName.getAbsolutePath(), getFileDataFromBitmap(bitmap), null));
                return params;
            }
        };
        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        this.runOnUiThread(new Runnable() {
            public void run() {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Volley.newRequestQueue(getApplicationContext()).add(multipartRequest);
                    }
                }, 1000L * Math.min(trails,100));
            }
        });
    }

    public static byte[] getFileDataFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    /* Firebase Processing */

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
     * Uploads a bitmap image to the Google Vision API using the annotateImage function and waits for the result
     * The image is first resized to the set maxDimension of 640
     * Then the image is encoded into a Base64 string
     * A json object is then created that holds the info of the request and the variables required by the Google Vision API
     * After the image is analyzed and the Google Vision API returns a result we process the result using the processMultipleOCRData function
     * @param originalBitmap The capture of the camera as a bitmap
     * @param fileName This is used just as an identifier to know which text is for which image on the database
     */
    public void uploadImage(File fileName, Bitmap originalBitmap){
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
                    cameraPreviewImageView.setVisibility(View.INVISIBLE);
                    captureImageButton.setEnabled(true);
                    try {
                        if(task.getResult() != null && task.getResult().getAsJsonArray() != null){
                            String resultText = task.getResult().getAsJsonArray().get(0).getAsJsonObject().get("fullTextAnnotation").getAsJsonObject()
                                    .get("text").getAsString();
                            Logger.Debug("OCR", "UploadImage - Analyzed Image Data, Processing The OCR For BOL");
                            processMultipleOCRData(fileName, resultText);

                        }else {
                            Snackbar.make(findViewById(R.id.bolRecognitionActiviyLayout), "Failed analyzing the data", Snackbar.LENGTH_LONG)
                                    .setAction("No action", null).show();
                            Logger.Debug("OCR", "UploadImage - Failed analyzing the data");
                        }
                    }catch (Exception ex){
                        Snackbar.make(findViewById(R.id.bolRecognitionActiviyLayout), "An internal error occurred", Snackbar.LENGTH_LONG)
                                .setAction("No action", null).show();
                        Logger.Error("OCR", "UploadImage - Returned Error: " + ex.getMessage());
                    }
                }else {
                    cameraPreviewImageView.setVisibility(View.INVISIBLE);
                    captureImageButton.setEnabled(true);
                    Snackbar.make(findViewById(R.id.bolRecognitionActiviyLayout), "A cloud error occurred", Snackbar.LENGTH_LONG)
                            .setAction("No action", null).show();
                    Logger.Error("OCR", "UploadImage - Error: " + task.getException().getMessage());
                }
            });
            Logger.Debug("OCR", "UploadImage - Sent Request To Google Vision, Bytes Size: " + imageBytes.length);
        }else {
            cameraPreviewImageView.setVisibility(View.INVISIBLE);
            captureImageButton.setEnabled(true);
            Snackbar.make(findViewById(R.id.bolRecognitionActiviyLayout), "An Internal Error Occurred", Snackbar.LENGTH_LONG)
                    .setAction("No action", null).show();
            Logger.Error("FIREBASE", "UploadImage - Error, FireBase Functions Not Initiated");
        }
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
}