package com.bos.wms.mlkit.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
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
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Remote.APIClient;
import Remote.BasicApi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class BolRecognitionActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1000;

    private PreviewView cameraPreview;
    private ImageView cameraPreviewImageView;
    private ImageButton captureImageButton, scanUPCSButton;

    private FirebaseAuth firebaseAuh;
    private FirebaseFunctions firebaseFunctions;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    public static int MinScannedItemsUPC = 4;

    public String IPAddress = "";

    public TextView bolHelpText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bol_recognition);

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");

        FirebaseApp.initializeApp(this);
        firebaseAuh = FirebaseAuth.getInstance();
        CloudLogin();

        bolHelpText = findViewById(R.id.bolHelpText);

        captureImageButton = findViewById(R.id.captureImageButton);
        captureImageButton.setEnabled(false);

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

        APICheck();

    }

    /* OCR Processing */

    public String[] possibleBOLAliases = {
            "BOL", "OL", "BL", "BO"
    };

    public void processOCRData(String text){
        String[] lines = text.split("\n");
        for(String line : lines){
            for(String alias : possibleBOLAliases){
                if(line.contains(alias)){
                    String toCheck = line.replaceAll("[^0-9]", "");
                    if(toCheck.length() == 8){
                        processBOLNumber(toCheck);
                        return;
                    }
                }
            }
        }
    }

    public void processMultipleOCRData(String text){
        String[] lines = text.split("\n");
        ArrayList<String> bols = new ArrayList<String>();
        for(String line : lines){
            String[] args = line.split(" ");
            for(String arg : args){
                String[] args2 = arg.split("\r");
                for(String  arg2 : args2){
                    arg2 = arg2.replaceAll("[^0-9]", "");
                    if(arg2.length() == 8 && !bols.contains(arg2)){
                        bols.add(arg2);
                    }
                }
            }
        }
        processBOLNumber(bols.toArray(new String[0]));
    }

    public void processBOLNumber(String... bols){
        Logger.Debug("OCR", "ProcessBOLNumber - Detected BOLS " + Arrays.toString(bols));

        try {
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();


            compositeDisposable.addAll(
                    api.ValidateBol(20, Arrays.asList(bols))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if(s != null){
                                    try{
                                        JSONObject json = new JSONObject(s.string());
                                        String formattedResult = "BOL: " + json.getString("bol") + " Box Serial: " + json.getString("boxSerial");
                                        /*new AlertDialog.Builder(this)
                                                .setTitle("BOL Number")
                                                .setMessage(formattedResult)
                                                .setPositiveButton("Print", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        //Send API Request To Print Data
                                                    }
                                                })
                                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        //Do Nothing
                                                    }
                                                })
                                                .setIcon(android.R.drawable.ic_dialog_alert)
                                                .show();*/


                                        //Print Bol Number And Serial
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

                                        bolHelpText.setText(s.string());
                                        Logger.Debug("API", "ProcessBOLNumber - Returned Error " + s.string());
                                    }

                                }
                            }, (throwable) -> {
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
                                    Logger.Debug("API", "ProcessBOLNumber - Returned HTTP Error " + response);
                                    bolHelpText.setText(response);
                                }else {
                                    bolHelpText.setText("Press one of the options below to start");
                                    Snackbar.make(findViewById(R.id.bolRecognitionActiviyLayout), "Internal Error Occurred", Snackbar.LENGTH_LONG)
                                            .setAction("No action", null).show();
                                    Logger.Error("API", "ProcessBOLNumber - Error In API Response: " + throwable.getMessage() + " " + throwable.toString());
                                }
                            }));

        } catch (Throwable e) {
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

            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));

    }

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
                        uploadImage(image);
                        boolean delete = photoFile.delete();
                        Logger.Debug("CAMERA", "CaptureImage - Image Saved And Uploaded, Deleted Image File? " + delete);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    public boolean cameraPermissionsGranted(){
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestCameraPermissions(){
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Logger.Error("CAMERA", "OnRequestPermissionsResult - Camera Started, User Granted Permissions");
                startCamera();
            } else {
                Toast.makeText(this, "Can't use camera scan", Toast.LENGTH_LONG).show();
                Logger.Error("CAMERA", "OnRequestPermissionsResult - User Failed To Grant Camera Permissions");
            }
        }
    }

    private File getOutputDirectory() {
        File[] allMediaDirs = getExternalMediaDirs();
        File mediaDir = allMediaDirs.length > 0 ? allMediaDirs[0] : null;
        if(mediaDir == null) {
            new File(getResources().getString(R.string.app_name)).mkdirs();
        }

        return (mediaDir != null && mediaDir.exists()) ? mediaDir : getFilesDir();
    }

    /*public void uploadImage(Bitmap bitmap, File fileName, int trails){

        if(trails>1000){
            return;
        }

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, "http://192.168.50.20:5000/FileUpload", new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
                String resultResponse = new String(response.data);
                try {
                    JSONObject result = new JSONObject(resultResponse);
                    String status = result.getString("status");

                } catch (JSONException e) {
                    e.printStackTrace();
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
                } else {
                    String result = new String(networkResponse.data);
                    try {
                        JSONObject response = new JSONObject(result);
                        String status = response.getString("status");
                        String message = response.getString("message");

                        Log.e("Error Status", status);
                        Log.e("Error Message", message);

                        if (networkResponse.statusCode == 404) {
                            errorMessage = "Resource not found";
                        } else if (networkResponse.statusCode == 401) {
                            errorMessage = message+" Please login again";
                        } else if (networkResponse.statusCode == 400) {
                            errorMessage = message+ " Check your inputs";
                        } else if (networkResponse.statusCode == 500) {
                            errorMessage = message+" Something is getting wrong";
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Log.i("Error", errorMessage);
                error.printStackTrace();
            }
        }) {

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                params.put("image", new DataPart(fileName.getAbsolutePath(), getFileDataFromBitmap(bitmap)));
                return params;
            }
        };
        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

    }

    public static byte[] getFileDataFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

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

    public Task<JsonElement> annotateImage(String jsonRequest){
        return firebaseFunctions.getHttpsCallable("annotateImage").call(jsonRequest).continueWith((task) -> JsonParser.parseString(new Gson().toJson(task.getResult().getData())));
    }
    */

    /* Firebase Processing */

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

    public void uploadImage(Bitmap originalBitmap){
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

        annotateImage(request.toString()).addOnCompleteListener((task) -> {
            if(task.isSuccessful()){
                cameraPreviewImageView.setVisibility(View.INVISIBLE);
                captureImageButton.setEnabled(true);
                try {
                    if(task.getResult() != null && task.getResult().getAsJsonArray() != null){
                        String resultText = task.getResult().getAsJsonArray().get(0).getAsJsonObject().get("fullTextAnnotation").getAsJsonObject()
                                .get("text").getAsString();
                        Logger.Debug("OCR", "UploadImage - Analyzed Image Data, Processing The OCR For BOL");
                        processMultipleOCRData(resultText);

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
                Logger.Debug("OCR", "Error: " + task.getException().getMessage());
            }
        });


        Logger.Debug("OCR", "UploadImage - Sent Request To Google Vision, Bytes Size: " + imageBytes.length);



    }

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