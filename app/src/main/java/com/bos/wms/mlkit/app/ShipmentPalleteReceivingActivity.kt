package com.bos.wms.mlkit.app

import Remote.VolleyMultipartRequest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.R
import id.zelory.compressor.Compressor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class ShipmentPalleteReceivingActivity : AppCompatActivity() {
    //Our variables
    private var mImageView: ImageView? = null
    private var mUri: Uri? = null
    private var mFilePath: File? = null
    //Our widgets
    private lateinit var lblStatus: TextView
    private lateinit var btnCapture: Button
    private lateinit var btnDone : Button
    //Our constants
    private val OPERATION_CAPTURE_PHOTO = 1
    private val OPERATION_CHOOSE_PHOTO = 2


    private fun initializeWidgets() {
        btnCapture = findViewById(R.id.btnCapture)
        btnDone = findViewById(R.id.btnShipPallReciDone)
        mImageView = findViewById(R.id.mImageView)
        lblStatus=findViewById(R.id.lblStatus)
    }

    private fun show(message: String) {
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
    }
    private fun capturePhoto(){
        //val capturedImage = InitializeImageFile()
        val capturedImage =  File(externalCacheDir, "My_Captured_Photo.jpg")
        if(capturedImage.exists()) {
            capturedImage.delete()
        }
        capturedImage.createNewFile()
        mFilePath=capturedImage
        mUri = if(Build.VERSION.SDK_INT >= 24){
            FileProvider.getUriForFile(this, "com.bos.wms.mlkit.fileprovider",
                capturedImage)
        } else {
            Uri.fromFile(capturedImage)
        }

        val intent = Intent("android.media.action.IMAGE_CAPTURE")
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri)
        startActivityForResult(intent, OPERATION_CAPTURE_PHOTO)
        //dispatchTakePictureIntent();
        //val intent = Intent("android.media.action.IMAGE_CAPTURE")
        //intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri)
        //startActivityForResult(intent, OPERATION_CAPTURE_PHOTO)
    }
    val REQUEST_IMAGE_CAPTURE = 1

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Log.e("CaptureImage",ex.message.toString())
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.bos.wms.mlkit.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }
    private fun openGallery(){
        val intent = Intent("android.intent.action.GET_CONTENT")
        intent.type = "image/*"
        startActivityForResult(intent, OPERATION_CHOOSE_PHOTO)
    }
    private fun renderImage(imagePath: String?){
        if (imagePath != null) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            mImageView?.setImageBitmap(bitmap)
        }
        else {
            show("ImagePath is null")
        }
    }
    @SuppressLint("Range")
    private fun getImagePath(uri: Uri?, selection: String?): String {
        var path: String? = null
        val cursor = uri?.let { contentResolver.query(it, null, selection, null, null ) }
        if (cursor != null){
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
            }
            cursor.close()
        }
        return path!!
    }
    @TargetApi(19)
    private fun handleImageOnKitkat(data: Intent?) {
        var imagePath: String? = null
        val uri = data!!.data
        //DocumentsContract defines the contract between a documents provider and the platform.
        if (DocumentsContract.isDocumentUri(this, uri)){
            val docId = DocumentsContract.getDocumentId(uri)
            if (uri != null) {
                if ("com.android.providers.media.documents" == uri.authority){
                    val id = docId.split(":")[1]
                    val selsetion = MediaStore.Images.Media._ID + "=" + id
                    imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        selsetion)
                } else if (uri != null) {
                    if ("com.android.providers.downloads.documents" == uri.authority){
                        val contentUri = ContentUris.withAppendedId(Uri.parse(
                            "content://downloads/public_downloads"), java.lang.Long.valueOf(docId))
                        imagePath = getImagePath(contentUri, null)
                    }
                }
            }
        }
        else if (uri != null) {
            if ("content".equals(uri.scheme, ignoreCase = true)){
                imagePath = getImagePath(uri, null)
            }
            else if ("file".equals(uri.scheme, ignoreCase = true)){
                imagePath = uri.path
            }
        }
        renderImage(imagePath)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>
    , grantedResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantedResults)
        when(requestCode){
            1 ->
                if (grantedResults.isNotEmpty() && grantedResults.get(0) ==
                 PackageManager.PERMISSION_GRANTED){
                    openGallery()
                }else {
                    show("Unfortunately You are Denied Permission to Perform this Operataion.")
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            OPERATION_CAPTURE_PHOTO ->
                if (resultCode == Activity.RESULT_OK) {
                    val bitmap = BitmapFactory.decodeStream(
                        mUri?.let { getContentResolver().openInputStream(it) })
                    mImageView!!.setImageBitmap(bitmap)
                    //val bitmap = data!!.extras!!.get("data") as Bitmap
                    //mImageView!!.setImageBitmap(bitmap)
                    //

                    GlobalScope.launch {
                        val compressedImageFile = Compressor.compress(applicationContext, mFilePath!!)
                        CompressedBitmap = BitmapFactory.decodeFile(compressedImageFile.path)
                        //imageView.setImageBitmap(bitmap)
                    }
                    uploadBitmap(CompressedBitmap!!, mFilePath!!)
                }
            OPERATION_CHOOSE_PHOTO ->
                if (resultCode == Activity.RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= 19) {
                        handleImageOnKitkat(data)
                    }
                }
        }
    }
    var CompressedBitmap:Bitmap?=null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shipment_pallete_receiving)

        initializeWidgets()

        btnCapture.setOnClickListener{

            val checkSelfPermission = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA)
            if (checkSelfPermission != PackageManager.PERMISSION_GRANTED){
                //Requests permissions to be granted to this application at runtime
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.CAMERA), 1)
            }
            else{
                capturePhoto()
            }


        }
        btnDone.setEnabled(false)
        btnDone.setOnClickListener{
            //check permission at runtime

        }

        btnCapture.performClick()
    }
    private fun UploadImage(filePath:File) {

            if (filePath != null) {
                try {
                    GlobalScope.launch {
                        val compressedImageFile = Compressor.compress(applicationContext, filePath)
                        var bitmap = BitmapFactory.decodeFile(compressedImageFile.path)
                        uploadBitmap(bitmap, filePath)
                        //imageView.setImageBitmap(bitmap)
                    }

                } catch (e: IOException) {
                    //Log("UploadImages file"+filePath.absoluteFile+" Error:"+e.message)
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(
                    this@ShipmentPalleteReceivingActivity, "no image selected",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }
    private val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    private fun InitializeImageFile():File{
        val FolerPath = File(
            getOutputDirectory(),
            "/" + "Appointment" + "/"
        )
        FolerPath.mkdirs()
        val photoFile = File(
            FolerPath,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )
        return  photoFile;
    }
    lateinit var currentPhotoPath: String

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }
    private fun uploadBitmap(bitmap: Bitmap, FileName: File) {
        lblStatus.setText("Uploading Image")
        btnDone.setEnabled(false)
        val volleyMultipartRequest: VolleyMultipartRequest =
            object : VolleyMultipartRequest(
                //      Request.Method.POST, "http://10.188.30.110/BOS_WMS_API/FileUpload",


                Request.Method.POST, "http://192.168.1.172/BOS_WMS_API/FileUpload",
                //Request.Method.POST, "http://192.168.10.82:5000/FileUpload",
                Response.Listener {
                    val response = it
                    btnDone.setEnabled(true)
                    lblStatus.setText("Image Uploaded - Fill Pallete info")
                    Log.e("",it.toString())
                    // finishSend(response, comment)
                },
                Response.ErrorListener {
                    Log.e("",it.toString())
                }
            ) {
                override fun getByteData(): Map<String, DataPart>? {
                    val params: MutableMap<String, DataPart> = HashMap()
                    val imagename = FileName//System.currentTimeMillis()
                    params["image"] = DataPart(
                        FileName.absolutePath,
                        getFileDataFromDrawable(bitmap)!!
                    )
                    return params
                }
            }

        //adding the request to volley

        Volley.newRequestQueue(this).add(volleyMultipartRequest)
    }
    fun getFileDataFromDrawable(bitmap: Bitmap): ByteArray? {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }
}
//end
