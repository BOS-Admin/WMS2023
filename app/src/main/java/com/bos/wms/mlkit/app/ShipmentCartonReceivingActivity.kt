package com.bos.wms.mlkit.app

import Remote.APIClient
import Remote.BasicApi
import Remote.VolleyMultipartRequest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.R
import com.bos.wms.mlkit.storage.Storage
import id.zelory.compressor.Compressor
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_shipment_pallete_carton.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class ShipmentCartonReceivingActivity : AppCompatActivity() {
    //Our variables
    private var mImageView: ImageView? = null
    private var mUri: Uri? = null
    private var mFilePath: File? = null
    //Our widgets
    private lateinit var lblStatus: TextView
   // private lateinit var btnCapture: Button
   // private lateinit var btnDone : Button
    //Our constants
    private val OPERATION_CAPTURE_PHOTO = 1
    private val OPERATION_CHOOSE_PHOTO = 2
    private lateinit var TextChangeEvent:TextWatcher

    private fun initializeWidgets() {
       // btnCapture = findViewById(R.id.btnCapture)
        //btnDone = findViewById(R.id.btnShipPallReciDone)
        mImageView = findViewById(R.id.mImageView)
        lblStatus=findViewById(R.id.lblStatus)
    }

    private fun show(message: String) {
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
    }
    private fun capturePhoto(){
        var capturedImage = InitializeImageFile()

        //capturedImage =  File(File(externalCacheDir,"Appointment"), capturedImage.name)
        capturedImage =  File(externalCacheDir,capturedImage.name)

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

                    lifecycleScope.launch {
                        val compressedImageFile = Compressor.compress(applicationContext, mFilePath!!)
                        CompressedBitmap = BitmapFactory.decodeFile(compressedImageFile.path)
                        UploadBitmap=CompressedBitmap!!
                        UploadPath=mFilePath!!
                        mImageView!!.setImageBitmap(bitmap)
                        txtShipCartonReciBolNb.requestFocus()
                    }

                }
            OPERATION_CHOOSE_PHOTO ->
                if (resultCode == Activity.RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= 19) {
                        handleImageOnKitkat(data)
                    }
                }
        }
    }
    var UpdatingText:Boolean=false
    var CompressedBitmap:Bitmap?=null;
    lateinit var  mStorage:Storage ;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shipment_pallete_carton)

        initializeWidgets()
        mStorage = Storage(applicationContext)
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")
       /* btnCapture.setOnClickListener{

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


        }*/
        //btnDone.setEnabled(false)
        //btnDone.setOnClickListener{
            //check permission at runtime

       // }
        TextChangeEvent=object : TextWatcher {

            @SuppressLint("ResourceAsColor")
            override fun afterTextChanged(s: Editable) {
                if(UpdatingText)
                    return;
                UpdatingText=true;
                var seqVal:Int=0
                //var AppointmentNbStr:String=txtShipCartonReciAppointmentNb.text.toString()
                var BolNbStr:String=txtShipCartonReciBolNb.text.toString()
                var PalleteNbStr:String=txtShipCartonReciPalleteNb.text.toString()
                var CartonCodeStr:String=txtShipCartonReciCartonCode.text.toString()


                /*if(!General.ValidateAppointmentNoFormat(AppointmentNbStr)){
                    seqVal=0
                }
                else  if(!General.ValidateAppointmentNo(AppointmentNbStr)){
                    seqVal=0
                }*/

                if(!General.ValidateBolNoFormat(BolNbStr)){
                    seqVal=1
                }
                else  if(!General.ValidateBolNo(BolNbStr)){
                    seqVal=1
                } else  if(!General.ValidatePalleteNo(PalleteNbStr)){
                    seqVal=2
                }
                else  if(!General.ValidateCartonCode(CartonCodeStr)){
                    seqVal=3
                }
                else{
                    seqVal=4
                }
                when (seqVal) {

                    1 -> {
                        txtShipCartonReciBolNb.setText("")
                        txtShipCartonReciPalleteNb.setText("")
                        txtShipCartonReciCartonCode.setText("")
                        txtShipCartonReciBolNb.requestFocus()
                        UpdatingText=false;
                    }
                    2 -> {
                        ValidateAppointmentBol(-1,General.ToInteger(BolNbStr,-1));
                    }
                    3 -> {
                        txtShipCartonReciCartonCode.setText("")
                        txtShipCartonReciCartonCode.requestFocus()
                        UpdatingText=false;
                    }
                    4 -> {
                        PostShipmentReceivingCarton(
                            //General.ToInteger(AppointmentNbStr,-1),
                            BolNbStr,
                            General.ToInteger(PalleteNbStr,-1),
                             CartonCodeStr,
                        )

                    }
                }
                txtShipCartonReciBolNb.setShowSoftInputOnFocus(false);
                txtShipCartonReciPalleteNb.setShowSoftInputOnFocus(false);
                txtShipCartonReciCartonCode.setShowSoftInputOnFocus(false);
            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {

            }
        }
        General.hideSoftKeyboard(this)
        txtShipCartonReciBolNb.setShowSoftInputOnFocus(false);
        txtShipCartonReciPalleteNb.setShowSoftInputOnFocus(false);
        txtShipCartonReciCartonCode.setShowSoftInputOnFocus(false);
        //btnCapture.performClick()
        txtShipCartonReciBolNb.addTextChangedListener(TextChangeEvent);
        txtShipCartonReciPalleteNb.addTextChangedListener(TextChangeEvent);
        txtShipCartonReciCartonCode.addTextChangedListener(TextChangeEvent);
        txtShipCartonReciBolNb.requestFocus()
    }
    var ColorGreen = Color.parseColor("#52ac24")
    var ColorRed = Color.parseColor("#ef2112")
    var ColorWhite = Color.parseColor("#ffffff")
    fun RestartScreen(){

    }
    fun PostShipmentReceivingCarton(BolNumber: String, PalleteNb:Int, CartonCode:String){
        try {
            // TODO: handle loggedInUser authentication
            var UserID: Int=General.getGeneral(applicationContext).UserID
            api = APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.ShipmentReceivingCarton(UserID,BolNumber, PalleteNb, CartonCode)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            var ErrorMsg = ""
                            try {
                                ErrorMsg = s.string()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                            if (ErrorMsg.isEmpty()) {
                                ProceedSuccess("Success")
                               // UploadTrial=0
                               // uploadBitmap(UploadBitmap, UploadPath)


                            }else {
                                if(ErrorMsg.startsWith("Success:")){
                                    ProceedSuccess(ErrorMsg)
                                 //   UploadTrial=0
                                   // uploadBitmap(UploadBitmap, UploadPath)

                                }
                                else{
                                    ProceedFailure(ErrorMsg)
                                }
                            }
                            txtShipCartonReciCartonCode.setText("")
                            txtShipCartonReciCartonCode.requestFocus()
                            UpdatingText=false;
                        },
                        { t: Throwable? ->
                            run {
                                showErrorProcessing(t.toString())
                                ProceedFailure(t.toString())
                                txtShipCartonReciCartonCode.setText("")
                                txtShipCartonReciCartonCode.requestFocus()
                                UpdatingText=false;
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            //throw(IOException("Error PostShipmentReceivingCarton", e))
            ProceedFailure(e.message!!)
            txtShipCartonReciCartonCode.setText("")
            txtShipCartonReciCartonCode.requestFocus()
            UpdatingText=false;
        } finally {
        }
    }
    fun ProceedSuccess(str:String){
        General.playSuccess()
        lblStatus.text = str
        lblStatus.setTextColor(ColorGreen)
    }
    fun ProceedFailure(str:String){
        General.playError()
        lblStatus.text = str
        lblStatus.setTextColor(ColorRed)
    }
    lateinit var api: BasicApi
    var compositeDisposable = CompositeDisposable()
    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }
    var IPAddress =""
    var AppointmentValidated:Boolean=false
    var BolValidated:Boolean=false
    fun ValidateAppointment(AppointmentNo: Int) {

        try {
            // TODO: handle loggedInUser authentication

            api = APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.ValidateShipmentAppointment(AppointmentNo)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            if (s){
                                AppointmentValidated=true;
                                ProceedSuccess("Scan Bol Number")
                                txtShipCartonReciBolNb.setText("")
                                txtShipCartonReciPalleteNb.setText("")
                                txtShipCartonReciCartonCode.setText("")
                                txtShipCartonReciBolNb.requestFocus()
                            }
                            else{
                                AppointmentValidated=false;
                                ProceedFailure("Invalid Appointment")
                               // txtShipCartonReciAppointmentNb.setText("")
                                txtShipCartonReciBolNb.setText("")
                                txtShipCartonReciPalleteNb.setText("")
                                txtShipCartonReciCartonCode.setText("")
                                txtShipCartonReciBolNb.requestFocus()
                        }
                            UpdatingText=false;
                        },
                        { t: Throwable? ->
                            run {
                                showErrorProcessing(t.toString())
                                UpdatingText=false;
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            throw(IOException("Error ValidateAppointment", e))
            UpdatingText=false;
        } finally {
        }
    }
    fun ValidateAppointmentBol(AppointmentNo: Int,BolNo: Int) {

        try {
            // TODO: handle loggedInUser authentication

            api = APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.ValidateShipmentBol(AppointmentNo,BolNo)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            if (s){
                                BolValidated=true;
                                ProceedSuccess("Scan Pallete Nb")
                                txtShipCartonReciPalleteNb.setText("")
                                txtShipCartonReciCartonCode.setText("")
                                txtShipCartonReciPalleteNb.requestFocus()
                            }
                            else{
                                BolValidated=false;
                                ProceedFailure("Invalid Bol Nb")
                                txtShipCartonReciBolNb.setText("")
                                txtShipCartonReciPalleteNb.setText("")
                                txtShipCartonReciCartonCode.setText("")
                                txtShipCartonReciBolNb.requestFocus()
                            }
                            UpdatingText=false;
                        },
                        { t: Throwable? ->
                            run {
                                showErrorProcessing(t.toString())
                                UpdatingText=false;
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            throw(IOException("Error ValidateAppointmentBol", e))
            UpdatingText=false;
        } finally {
        }
    }
    private var toast: Toast? = null
    fun showToast(message: String?, Big: Boolean = false) {
        if (toast != null) {
            toast!!.cancel()
        }
        toast = Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT)
        if (Big) {
            val group = toast!!.view as ViewGroup
            val messageTextView = group.getChildAt(0) as TextView
            messageTextView.textSize = 35f
        }
        toast!!.show()
    }
    private fun showErrorProcessing(errorString: String) {
        showToast(errorString)
    }
    lateinit var UploadBitmap:Bitmap
    lateinit var UploadPath:File
    private fun UploadImage(filePath:File) {

            if (filePath != null) {
                try {
                    lifecycleScope.launch {
                        val compressedImageFile = Compressor.compress(applicationContext, filePath)
                        val bitmap = BitmapFactory.decodeFile(compressedImageFile.path)
                        uploadBitmap(bitmap, filePath)
                    }

                } catch (e: IOException) {
                    //Log("UploadImages file"+filePath.absoluteFile+" Error:"+e.message)
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(
                    this@ShipmentCartonReceivingActivity, "no image selected",
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
        var FolerPath = File(
            getOutputDirectory(),
            "/" + "Appointment" + "/"
        )
        var BolNbStr:String=txtShipCartonReciBolNb.text.toString()
        var PalleteNbStr:String=txtShipCartonReciPalleteNb.text.toString()

        FolerPath = File(
            FolerPath,
            "/" + BolNbStr + "/"
        )
        FolerPath = File(
            FolerPath,
            "/" + PalleteNbStr + "/"
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
    var UploadTrial:Int=0
    private fun uploadBitmap(bitmap: Bitmap, FileName: File) {
        UploadTrial=UploadTrial+1
        if(UploadTrial>5){
         lblStatus.setText("Image upload failure, retry!!")
            lblStatus.setTextColor(ColorRed)
            General.playError()
            return
        }
        lblStatus.setText(lblStatus.text.toString()+" Uploading Image..")
       // btnDone.setEnabled(false)
        var UploadAPI:String=IPAddress
        if(!IPAddress.endsWith("/"))
            UploadAPI=IPAddress+"/"
        val volleyMultipartRequest: VolleyMultipartRequest =
            object : VolleyMultipartRequest(
                //      Request.Method.POST, "http://10.188.30.110/BOS_WMS_API/FileUpload",

                Request.Method.POST, "http://"+UploadAPI+"FileUpload",
                //Request.Method.POST, "http://192.168.10.82:5000/FileUpload",
                Response.Listener {
                    val response = it
                    //btnDone.setEnabled(true)
                    lblStatus.setText("Image Uploaded")
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        recreate();
                    }, 3500)


                    Log.e("",it.toString())
                    // finishSend(response, comment)
                },
                Response.ErrorListener {
                    Log.e("",it.toString())
                    uploadBitmap(bitmap, FileName)
                }
            ) {
                override fun getByteData(): Map<String, DataPart>? {
                    val params: MutableMap<String, DataPart> = HashMap()
                    //val imagename = FileName//System.currentTimeMillis()
                    val imagename=InitializeImageFile()
                    params["image"] = DataPart(
                        imagename.absolutePath,//FileName.absolutePath.replace("cache","Appointment"),
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
