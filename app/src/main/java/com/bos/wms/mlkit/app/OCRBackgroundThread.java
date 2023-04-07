package com.bos.wms.mlkit.app;


import androidx.annotation.NonNull;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import Model.ItemOCRModel;
import Remote.APIClient;
import Remote.BasicApi;
import Remote.VolleyMultipartRequest;
import id.zelory.compressor.Compressor;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class OCRBackgroundThread {

    public static int Delay = 5000;

    private static FirebaseAuth firebaseAuh;
    private static FirebaseFunctions firebaseFunctions;

    private static Context currentContext;


    private static StringBuffer ThreadWaiterStringBuffer = new StringBuffer();

    private static String IPAddress = "";

    /**
     * Initializes the class
     * @param context
     */
    public static void Initialize(Context context){

        currentContext = context;

        Storage mStorage = new Storage(currentContext);
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");

        //Initialize firebase and the the firebase authenticator
        FirebaseApp.initializeApp(currentContext);
        firebaseAuh = FirebaseAuth.getInstance();
        CloudLogin();

        File FolderPath = new File(
                getOutputDirectory(),
                "/OCR/"
        );
        if(!FolderPath.exists()){
            FolderPath.mkdirs();
        }
    }

    /**
     * This function starts the background thread and starts processing each ocr folder by folder
     */
    private static void RunBackgroundThread(){
        new Thread( new Runnable() { @Override public void run() {

            File FolerPath = new File(
                    getOutputDirectory(),
                    "/OCR/"
            );

            //Get all the files from the OCR folder
            File[] allFolders = FolerPath.listFiles();
            for(File currentFolder : allFolders){
                //Verify that the current folder actually is an ocr item folder
                if(currentFolder.isDirectory() && currentFolder.getName().startsWith("IS")){
                    Logger.Debug("OCR-THREAD", "Starting OCR Process For Folder " + currentFolder.getAbsolutePath());
                    File[] allFiles = currentFolder.listFiles();
                    for(File currentFile : allFiles){
                        ProcessImageFile(currentFile, currentFolder);
                    }

                    //This is used here to make sure that the folder didnt remain from the last application instance
                    StartFolderRevision(currentFolder);

                    try {
                        Thread.sleep(Delay);
                    } catch (InterruptedException e) {

                    }
                }
            }

            try {
                Thread.sleep(Delay);
                RunBackgroundThread();
            } catch (InterruptedException e) {

            }
        } } ).start();
    }

    /**
     * This function processes the image file, by getting the text from the cloud
     * @param currentFile
     * @param currentFolder
     */
    public static void ProcessImageFile(File currentFile, File currentFolder){
        //Get The file info such as name, extension
        String filePath = currentFile.getAbsolutePath();
        String extension = filePath.substring(filePath.lastIndexOf("."));
        String fileNameWithExtension = currentFile.getName();
        String fileName = fileNameWithExtension.substring(0, fileNameWithExtension.lastIndexOf("."));

        if(currentFile.isFile() && extension.equalsIgnoreCase(".jpg")){
            File ocrTextFile = new File(currentFolder, "/" + fileName + ".txt");
            if(ocrTextFile.exists()){
                //Upload Image to backend
                Logger.Debug("OCR-THREAD", "Image file and OCR text file found " + fileName + " Uploading file to backend now");
                try {
                    File compressedImageFile = new Compressor(currentContext).setMaxHeight(762).setMaxWidth(1016).setQuality(100).compressToFile(currentFile);
                    Bitmap bitmap = BitmapFactory.decodeFile(compressedImageFile.getAbsolutePath());
                    uploadBitmap(bitmap, currentFile, currentFolder,0);
                }catch (Exception ex){
                    Logger.Error("OCR-THREAD", "ProcessImageFile - Error while compressing the image " + ex.toString());
                }

            }else {
                //Get The Text
                uploadImage(fileName, currentFile, currentFolder);
            }

        }
    }

    /**
     * This function is called after each image upload of a folder to check if the images are all uploaded then we upload the ocr text to the backend
     * @param currentFolder
     */
    public static void StartFolderRevision(File currentFolder){
        if(currentFolder.isDirectory() && currentFolder.getName().startsWith("IS")){
            File[] allFiles = currentFolder.listFiles();

            ArrayList<String> allOCRS = new ArrayList<>();
            ArrayList<String> allFileNames = new ArrayList<>();
            ArrayList<String> allLogos = new ArrayList<>();

            boolean foundImage = false;

            for(File currentFile : allFiles){
                String filePath = currentFile.getAbsolutePath();
                String extension = filePath.substring(filePath.lastIndexOf("."));



                //If its an image then the folder is not done processing yet
                if(extension.equalsIgnoreCase(".jpg")){
                    foundImage = true;
                    break;
                }

                if(extension.equalsIgnoreCase(".txt")){
                    String fileNameWithExtension = currentFile.getName();
                    String fileName = fileNameWithExtension.substring(0, fileNameWithExtension.lastIndexOf(".")) + ".jpg";

                    allOCRS.add(readFileString(currentFile));
                    allFileNames.add(fileName);

                }

                if(extension.equalsIgnoreCase(".ini")){
                    allLogos.add(readFileString(currentFile));
                }
            }

            if(foundImage) return;

            String currentItemSerial = currentFolder.getName();


            if(allOCRS.size() > 0 && allFileNames.size() > 0){

                int userID = General.getGeneral(currentContext).UserID;

                ItemOCRModel model = new ItemOCRModel(currentItemSerial, "", userID, allOCRS.toArray(new String[0]), allFileNames.toArray(new String[0]), allLogos.toArray(new String[0]));

                //Send the items to the api
                try {
                    BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
                    CompositeDisposable compositeDisposable = new CompositeDisposable();
                    compositeDisposable.addAll(
                            api.ProceedItemOCR(model)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe((s) -> {
                                        try {
                                            DeleteFolderRecursive(currentFolder);
                                            Logger.Debug("OCR-THREAD", "Done Processing OCR Item " + currentItemSerial);
                                        }catch (Exception ex){
                                            Logger.Debug("OCR-THREAD", "Failed Deleting Folder For Item " + currentItemSerial);
                                        }
                                    }, (throwable) -> {
                                        Logger.Error("API", "StartFolderRevision - Error In Response: " + throwable.getMessage());
                                        Logger.Error("API", "StartFolderRevision - Error Response Data Sent: " + currentItemSerial + " " + userID +
                                                " " + allOCRS.size() + " " + allFileNames.size());
                                    }));
                } catch (Throwable e) {
                    Logger.Error("API", "StartFolderRevision - Error Connecting: " + e.getMessage());
                }
            }else {
                Logger.Debug("OCR-THREAD", "Couldn't Find Any OCR Text Files For Item " + currentItemSerial);
            }
        }
    }

    /**
     * Sign in to firebase using a username and password. This is needed to be able to call the Google Vision API
     */
    public static void CloudLogin(){
        firebaseAuh.signInWithEmailAndPassword("ocr@katayagroup.com","Feras@!@#123").addOnCompleteListener((Activity) currentContext, (result) -> {
            if(result.isSuccessful()){
                Logger.Debug("FIREBASE", "CloudLogin - Logged In Successfully");
                firebaseFunctions = FirebaseFunctions.getInstance(firebaseAuh.getApp());
                Logger.Debug("FIREBASE", "CloudLogin - Created Functions Instance " + firebaseAuh.getCurrentUser().getUid());

                RunBackgroundThread();

            }else {
                Logger.Debug("FIREBASE", "CloudLogin - Firebase Login Failed");
                if(result.getException() !=null && result.getException().getMessage() !=null) {
                    Logger.Error("FIREBASE", "CloudLogin- " + result.getException().getMessage());
                }
            }

        });
    }

    /**
     * Sends the image to the Google Vision API (Uses FireBase Functions - annotateImage Function)
     * @param requestJson The json string of the request including the image encoded in Base64 and some variables for the google vision api to use
     * @return Returns a result from the Google Vision API as a JsonElement
     */
    private static Task<JsonElement> annotateImage(String requestJson) {
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
     * After the image is analyzed and the Google Vision API returns a result we save the ocr text in a file
     */
    public static void uploadImage(String fileName, File currentFile, File currentFolder){
        Bitmap originalBitmap = BitmapFactory.decodeFile(currentFile.getAbsolutePath());
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
        JsonObject feature2 = new JsonObject();
        feature2.add("type", new JsonPrimitive("LOGO_DETECTION"));
        JsonArray features = new JsonArray();
        features.add(feature);
        features.add(feature2);

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
                            //Receive the text from google ocr and save them into the file
                            String resultText = "";

                            try {
                                resultText = task.getResult().getAsJsonArray().get(0).getAsJsonObject().get("fullTextAnnotation").getAsJsonObject()
                                        .get("text").getAsString();
                            }catch(Exception ex){
                                Logger.Debug("OCR-THREAD", "UploadImage - Trying To Get OCR Text Error: " + ex.getMessage());
                            }

                            Logger.Debug("OCR-THREAD", "UploadImage - OCR Detected Text: " + resultText);

                            String logoName = "No Logo";
                            float currentScore = 0;

                            try {
                                JsonElement obj = task.getResult().getAsJsonArray().get(0).getAsJsonObject().get("logoAnnotations");

                                if(obj.getAsJsonArray().size() > 0){

                                    for(int i = 0; i < obj.getAsJsonArray().size(); i++){
                                        JsonElement detectedLogo = obj.getAsJsonArray().get(i);
                                        String _logoName = detectedLogo.getAsJsonObject().get("description").getAsString();
                                        float accuracy = detectedLogo.getAsJsonObject().get("score").getAsFloat() * 100;
                                        Logger.Debug("OCR-THREAD", "UploadImage - OCR Detected Logo " + _logoName + " | " + String.format("%.2f", accuracy) + "%");

                                        if(accuracy > currentScore){
                                            currentScore = accuracy;
                                            logoName = _logoName;
                                        }

                                    }

                                    Logger.Debug("OCR-THREAD", "UploadImage - Calculated Logo With Highest Accuracy " + logoName + " | " + String.format("%.2f", currentScore) + "%");

                                }else {
                                    Logger.Debug("OCR-THREAD", "UploadImage - No Logos Were Detected");
                                }
                            }catch(Exception ex){
                                Logger.Error("OCR-THREAD", "UploadImage - Error While Trying To Read OCR Detected Logos, " + ex.getMessage());
                            }

                            Logger.Debug("OCR-THREAD", "UploadImage - Analyzed Image Data, Saving OCR Data To File " + fileName + ".txt" + " For Item " + currentFolder.getName());
                            File saveFile = new File(currentFolder, "/" + fileName + ".txt");
                            WriteToFile(saveFile, resultText);

                            File logosSaveFile = new File(currentFolder, "/" + fileName + ".ini");
                            WriteToFile(logosSaveFile, logoName + "|" + currentScore);

                            //Attempt to process the image file again this time to upload it to the server
                            ProcessImageFile(currentFile, currentFolder);

                        }else {
                            Logger.Debug("OCR-THREAD", "UploadImage - Failed analyzing the data");
                        }

                    }catch (Exception ex){
                        Logger.Error("OCR-THREAD", "UploadImage - Returned Error: " + ex.getMessage());
                        if(ex instanceof NullPointerException){
                            Logger.Debug("OCR-THREAD", "UploadImage - Returned Error: " + ex.getMessage() + " Uploading Image With Empty Text For Item " + currentFolder.getName());
                            File saveFile = new File(currentFolder, "/" + fileName + ".txt");
                            WriteToFile(saveFile, "");

                            //Attempt to process the image file again this time to upload it to the server
                            ProcessImageFile(currentFile, currentFolder);
                        }
                    }
                }else {
                    Logger.Error("OCR-THREAD", "UploadImage - Error: " + task.getException().getMessage());
                }
            });
            Logger.Debug("OCR-THREAD", "UploadImage - Sent Request To Google Vision, Bytes Size: " + imageBytes.length);
        }else {
            Logger.Error("FIREBASE", "UploadImage - Error, FireBase Functions Not Initiated");
        }

    }

    /**
     * This function writes text into a file
     * @param file
     * @param text
     */
    public static void WriteToFile(File file, String text) {


        if (!file.exists()){
            try {
                file.createNewFile();
            }catch(IOException e){
                Logger.Error("OCR-THREAD", "Failed Creating OCR Text File (" + file.getAbsolutePath() + ")");
            }
        }
        try {
            FileOutputStream outStream = new FileOutputStream(file, true) ;
            OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream);

            outStreamWriter.append(text);
            outStreamWriter.flush();
        }catch(IOException e){
            Logger.Error("OCR-THREAD", "Failed Creating An Output Stream Writer (" + file.getAbsolutePath() + ")");
        }

    }

    /**
     * This functions resizes a bitmap to a given dimension
     * @param bitmap
     * @param maxDimension
     * @return
     */
    private static Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
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
     * Uploads the ocr image to the backend
     * @param bitmap
     * @param fileName
     * @param trails
     */
    public static void uploadBitmap(Bitmap bitmap, File fileName, File currentFolder, int trails){
        if(trails == 0){
            Logger.Debug("OCR-THREAD", "uploadBitmap - Starting Upload For: " + fileName.getAbsolutePath());
        }
        if(trails>1000){
            Logger.Error("OCR-THREAD", "uploadBitmap - Trails reached 1000");;
            return;
        }

        String ipAddress = "http://" + IPAddress + (IPAddress.endsWith("/") ? "" : "/");

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, ipAddress + "FileUpload", new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(response.toString().contains("Success")){
                    Logger.Debug("OCR-THREAD", "Image " + fileName.getName() + " Uploaded Successfully, Deleting Image");
                    StartFolderRevision(currentFolder);
                    try{
                        fileName.delete();
                    }catch(Exception ex){
                        Logger.Debug("OCR-THREAD", "Error Uploading " + fileName.getName() + " " + ex.getMessage());
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
                    uploadBitmap(bitmap, fileName, currentFolder, trails + 1);
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
                Logger.Error("OCR-THREAD", "uploadBitmap - Response Error: " + errorMessage);
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

        Volley.newRequestQueue(currentContext).add(multipartRequest);
    }

    public static byte[] getFileDataFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private static File getOutputDirectory() {
        File[] allMediaDirs = currentContext.getExternalMediaDirs();
        File mediaDir = allMediaDirs.length > 0 ? allMediaDirs[0] : null;
        if(mediaDir == null) {
            new File(currentContext.getResources().getString(R.string.app_name)).mkdirs();
        }

        return (mediaDir != null && mediaDir.exists()) ? mediaDir : currentContext.getFilesDir();
    }

    /**
     * This function reads a file and returns a string of the content
     * @param file
     * @return
     */
    public static String readFileString(File file){
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            return "Error Reading OCR File";
        }
        return text.toString();
    }

    /**
     * This function deletes a folder and all its subdirectories and files recursively
     * @param fileOrDirectory
     */
    public static void DeleteFolderRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                DeleteFolderRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }


}
