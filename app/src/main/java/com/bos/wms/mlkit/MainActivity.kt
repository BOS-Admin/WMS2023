package com.bos.wms.mlkit


import Model.ItemOCRModel
import Remote.APIClient
import Remote.BasicApi
import Remote.VolleyMultipartRequest
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import com.bos.wms.mlkit.storage.Storage
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import id.zelory.compressor.Compressor
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var LatestImage: Uri? = null
    private var BarcodesCounter: Int = 0
    private var OCRText: String = ""
    private lateinit var OCRList: ArrayList<String>
    private lateinit var OCRFileNames: ArrayList<String>
    private lateinit var OCRFileURLs: ArrayList<File>

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

    lateinit var api: BasicApi
    var compositeDisposable = CompositeDisposable()

    override fun onStop() {
        compositeDisposable.clear()
        StopTimer()
        super.onStop()
    }

    fun CheckBarcodeOCRDone(Barcode: String) {

        try {
            // TODO: handle loggedInUser authentication

            api = APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.CheckBarcodeOCRDone(Barcode)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            if (s)
                                showOCRDone(Barcode)
                            else
                                ProceedTOCR(Barcode)
                        },
                        { t: Throwable? ->
                            run {
                                showErrorProcessing(t.toString())
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            throw(IOException("Error logging in", e))
        } finally {
        }
    }

    private fun showOCRDone(Barcode: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Barcode Done!!")
        builder.setMessage(Barcode)
        builder.setPositiveButton(
            "OK",
            DialogInterface.OnClickListener { dialog, which ->
                finish();
                overridePendingTransition(0, 0);
                startActivity(getIntent());
                overridePendingTransition(0, 0);
            })
        builder.show()

    }

    var CurrentBarcode: String = ""

    private fun Pause_Resume_Capturing() {
        if (btnPause.text == "Pause") {
            PauseTimer()
            btnPause.text = "Continue"
        } else {
            ContinueTimer()
            btnPause.text = "Pause"
        }
    }

    private fun UploadImages() {
        OCRFileURLs.forEach {
            var filePath = it
            if (filePath != null) {
                try {
                    GlobalScope.launch {
                        val compressedImageFile = Compressor.compress(applicationContext, filePath)
                        var bitmap = BitmapFactory.decodeFile(compressedImageFile.path)
                        uploadBitmap(bitmap, filePath)
                        //imageView.setImageBitmap(bitmap)
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(
                    this@MainActivity, "no image selected",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun ProceedOCRToAPI() {
        StopTimer()

        var mycontext: Context = this
        val FolerPath = File(
            outputDirectory,
            "/" + BarcodeFolder + "/"
        )
        FolerPath.mkdirs()
        val OCRFile = File(
            FolerPath, "OCR.txt"
        )

        OCRFile.writeText(OCRText)


        try {
            // TODO: handle loggedInUser authentication

            var ItemOCR: ItemOCRModel =
                ItemOCRModel(CurrentBarcode, 1, OCRList.toTypedArray(), OCRFileNames.toTypedArray())


            api = APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.ProceedItemOCR(ItemOCR)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s ->
                            UploadImages()
                            finish();
                            overridePendingTransition(0, 0);
                            startActivity(getIntent());
                            overridePendingTransition(0, 0);
                        },
                        { t: Throwable? ->
                            run {
                                showErrorProcessing(t.toString())
                                finish();
                                overridePendingTransition(0, 0);
                                startActivity(getIntent());
                                overridePendingTransition(0, 0);
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            throw(IOException(e.message, e))
        } finally {
        }
    }

    private fun PauseTimer() {
        Counter = -10
    }

    private fun StopTimer() {
        Counter = -100
    }

    private fun ContinueTimer() {
        Counter = 10
    }

    var Counter: Int = 10
    private fun ProceedTOCR(Barcode: String) {
        BarcodeFolder = Barcode
        ContinueTimer()
        OCRList = arrayListOf()
        OCRFileNames = arrayListOf()
        OCRFileURLs = arrayListOf()
        btnPause.visibility = View.VISIBLE
        btnDone.visibility = View.VISIBLE
        btnCapture.visibility = View.VISIBLE
        object : CountDownTimer(1000000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (Counter == -100) {
                    this.cancel()
                    return
                }

                if (Counter == -10)
                    return
                if (Counter == 5) {
                    viewFinder.visibility = View.VISIBLE
                    imageView.visibility = View.INVISIBLE
                }
                showToast(Counter.toString(), true)
                Counter = Counter - 1
                if (Counter == 0) {
                    PauseTimer()
                    takePhoto()
                }
            }

            override fun onFinish() {
                ProceedOCRToAPI()
            }
        }.start()

    }

    private fun showErrorProcessing(errorString: String) {
        showToast(errorString)
    }


    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())

            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy) {

            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            listener(luma)

            image.close()
        }
    }

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    lateinit var  mStorage:Storage ;  //sp存储
    var IPAddress =""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mStorage = Storage(applicationContext)
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        btnPause.visibility = View.INVISIBLE
        btnDone.visibility = View.INVISIBLE
        btnCapture.visibility = View.INVISIBLE
        // Set up the listener for take photo button
        btnDone.setOnClickListener { ProceedOCRToAPI() }
        btnPause.setOnClickListener { Pause_Resume_Capturing() }
        btnCapture.setOnClickListener {
            PauseTimer()
            takePhoto()
        }
        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder()
                .build()
            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { image ->
                        recognizeBarcode(image, cameraProvider)
                        // insert your code here.
                    })
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
    override fun onBackPressed() {
        val dialogClickListener =
            DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        //Yes button clicked
                       this.finish()
                    }
                    DialogInterface.BUTTON_NEGATIVE -> {
                    }
                }
            }
        val builder = android.app.AlertDialog.Builder(
            applicationContext
        )
        builder.setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Closing Form")
            .setMessage("Are you sure you want to close the form?")
            .setPositiveButton("Yes", dialogClickListener)
            .setNegativeButton("No", dialogClickListener).show()
    }
    private var BarcodeFolder: String? = "All"
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val FolerPath = File(
            outputDirectory,
            "/" + BarcodeFolder + "/"
        )
        FolerPath.mkdirs()
        val photoFile = File(
            FolerPath,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                    val savedUri = Uri.fromFile(photoFile)
                    val image = InputImage.fromFilePath(baseContext, savedUri)

                    var myBitmap: Bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    viewFinder.visibility = View.INVISIBLE
                    imageView.visibility = View.VISIBLE
                    imageView.setImageBitmap(myBitmap)
                    OCRFileURLs.add(photoFile)
                    OCRFileNames.add(photoFile.name)
                    recognizeTextOnDevice(image)
                    ContinueTimer()
                    // val msg = "Photo capture succeeded: $savedUri"
                    //Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    //77Log.d(TAG, msg)
                }
            })
    }

    fun ImageProxy.toBitmap(): Bitmap? {
        val nv21 = yuv420888ToNv21(this)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        return yuvImage.toBitmap()
    }

    private fun YuvImage.toBitmap(): Bitmap? {
        val out = ByteArrayOutputStream()
        if (!compressToJpeg(Rect(0, 0, width, height), 100, out))
            return null
        val imageBytes: ByteArray = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
        val pixelCount = image.cropRect.width() * image.cropRect.height()
        val pixelSizeBits = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)
        val outputBuffer = ByteArray(pixelCount * pixelSizeBits / 8)
        imageToByteBuffer(image, outputBuffer, pixelCount)
        return outputBuffer
    }

    private fun imageToByteBuffer(image: ImageProxy, outputBuffer: ByteArray, pixelCount: Int) {
        assert(image.format == ImageFormat.YUV_420_888)

        val imageCrop = image.cropRect
        val imagePlanes = image.planes

        imagePlanes.forEachIndexed { planeIndex, plane ->
            // How many values are read in input for each output value written
            // Only the Y plane has a value for every pixel, U and V have half the resolution i.e.
            //
            // Y Plane            U Plane    V Plane
            // ===============    =======    =======
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            val outputStride: Int

            // The index in the output buffer the next value will be written at
            // For Y it's zero, for U and V we start at the end of Y and interleave them i.e.
            //
            // First chunk        Second chunk
            // ===============    ===============
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            var outputOffset: Int

            when (planeIndex) {
                0 -> {
                    outputStride = 1
                    outputOffset = 0
                }
                1 -> {
                    outputStride = 2
                    // For NV21 format, U is in odd-numbered indices
                    outputOffset = pixelCount + 1
                }
                2 -> {
                    outputStride = 2
                    // For NV21 format, V is in even-numbered indices
                    outputOffset = pixelCount
                }
                else -> {
                    // Image contains more than 3 planes, something strange is going on
                    return@forEachIndexed
                }
            }

            val planeBuffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride

            // We have to divide the width and height by two if it's not the Y plane
            val planeCrop = if (planeIndex == 0) {
                imageCrop
            } else {
                Rect(
                    imageCrop.left / 2,
                    imageCrop.top / 2,
                    imageCrop.right / 2,
                    imageCrop.bottom / 2
                )
            }

            val planeWidth = planeCrop.width()
            val planeHeight = planeCrop.height()

            // Intermediate buffer used to store the bytes of each row
            val rowBuffer = ByteArray(plane.rowStride)

            // Size of each row in bytes
            val rowLength = if (pixelStride == 1 && outputStride == 1) {
                planeWidth
            } else {
                // Take into account that the stride may include data from pixels other than this
                // particular plane and row, and that could be between pixels and not after every
                // pixel:
                //
                // |---- Pixel stride ----|                    Row ends here --> |
                // | Pixel 1 | Other Data | Pixel 2 | Other Data | ... | Pixel N |
                //
                // We need to get (N-1) * (pixel stride bytes) per row + 1 byte for the last pixel
                (planeWidth - 1) * pixelStride + 1
            }

            for (row in 0 until planeHeight) {
                // Move buffer position to the beginning of this row
                planeBuffer.position(
                    (row + planeCrop.top) * rowStride + planeCrop.left * pixelStride
                )

                if (pixelStride == 1 && outputStride == 1) {
                    // When there is a single stride value for pixel and output, we can just copy
                    // the entire row in a single step
                    planeBuffer.get(outputBuffer, outputOffset, rowLength)
                    outputOffset += rowLength
                } else {
                    // When either pixel or output have a stride > 1 we must copy pixel by pixel
                    planeBuffer.get(rowBuffer, 0, rowLength)
                    for (col in 0 until planeWidth) {
                        outputBuffer[outputOffset] = rowBuffer[col * pixelStride]
                        outputOffset += outputStride
                    }
                }
            }
        }
    }

    private var IsProcessing: Boolean = false

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun recognizeBarcode(
        imgProxy: ImageProxy, cameraProvider: ProcessCameraProvider
    ) {
        if (IsProcessing)
            return
        val rotDegree = imgProxy.imageInfo.rotationDegrees
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_UPC_A
                )
            .build()
        val scanner = BarcodeScanning.getClient(options)
        @androidx.camera.core.ExperimentalGetImage
        if (imgProxy != null) {
            @androidx.camera.core.ExperimentalGetImage
            val imageNew = InputImage.fromBitmap(imgProxy.toBitmap(), rotDegree)


            @androidx.camera.core.ExperimentalGetImage
            val result = scanner.process(imageNew)
                .addOnSuccessListener { barcodes ->
                    // Task completed successfully
                    if (barcodes.count() == 1) {
                        try {
                            cameraProvider.unbind(imageAnalyzer)
                            IsProcessing = true
                            BarcodesCounter = BarcodesCounter + 1
                            CurrentBarcode = barcodes[0].rawValue
                            showToast(".Barcode Detected:" + barcodes[0].rawValue)
                            CheckBarcodeOCRDone(barcodes[0].rawValue)

                            // Unbind use cases before rebinding
                        } catch (exc: Exception) {
                            Log.e(TAG, "Use case binding failed", exc)
                        }
                    }

                    /* for (barcode in barcodes) {
                         val builder = AlertDialog.Builder(this)
                         builder.setTitle("Barcode Scanned!!")
                         builder.setMessage(barcode.rawValue)
                         builder.setPositiveButton("Yes") { dialogInterface, which ->

                         }
                         builder.show()
                         Toast.makeText(
                             applicationContext,
                             barcode.rawValue,
                             Toast.LENGTH_LONG
                         )
                             .show()
                         Log.e(TAG, barcode.rawValue)
                         val bounds = barcode.boundingBox
                         val corners = barcode.cornerPoints

                         val rawValue = barcode.rawValue

                         val valueType = barcode.valueType
                         // See API reference for complete list of supported types
                         when (valueType) {
                             Barcode.TYPE_WIFI -> {
                                 val ssid = barcode.wifi!!.ssid
                                 val password = barcode.wifi!!.password
                                 val type = barcode.wifi!!.encryptionType
                             }
                             Barcode.TYPE_URL -> {
                                 val title = barcode.url!!.title
                                 val url = barcode.url!!.url
                             }
                         }
                     }*/
                }
                .addOnFailureListener {
                    Log.e(TAG, it.message, it.cause)
                }
                .addOnCompleteListener {
                    imgProxy.close()
                    //mediaImage.close()
                }
        }
    }


    fun getFileDataFromDrawable(bitmap: Bitmap): ByteArray? {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    private fun uploadBitmap(bitmap: Bitmap, FileName: File) {

        val volleyMultipartRequest: VolleyMultipartRequest =
            object : VolleyMultipartRequest(
 //               Request.Method.POST, "http://10.188.30.110/BOS_WMS_API/FileUpload",
                Request.Method.POST, "http://192.168.10.82:5000/FileUpload",
                Response.Listener<JSONObject> {
                    fun onResponse(response: NetworkResponse) {
                        try {
                            val obj = JSONObject(String(response.data))
                            Toast.makeText(
                                applicationContext,
                                obj.getString("message"),
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                },
                Response.ErrorListener() {
                    fun onErrorResponse(error: VolleyError) {
                        Toast.makeText(applicationContext, error.message, Toast.LENGTH_LONG).show()
                        Log.e("GotError", "" + error.message)
                    }
                }) {
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


    private fun recognizeTextOnDevice(
        image: InputImage
    ) {
        // Pass image to an ML Kit Vision API

        val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Task completed successfully
                OCRList.add(visionText.text)

                val resultText = visionText.text
                OCRText = OCRText + "\n------------------------------------\n"
                OCRText = OCRText + resultText
                // Toast.makeText(baseContext, resultText, Toast.LENGTH_LONG).show()
                for (block in visionText.textBlocks) {
                    val blockText = block.text
                    val blockCornerPoints = block.cornerPoints
                    val blockFrame = block.boundingBox
                    for (line in block.lines) {
                        val lineText = line.text
                        val lineCornerPoints = line.cornerPoints
                        val lineFrame = line.boundingBox
                        for (element in line.elements) {
                            val elementText = element.text
                            val elementCornerPoints = element.cornerPoints
                            val elementFrame = element.boundingBox
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Task failed with an exception
                Log.e(TAG, "Text recognition error", exception)

            }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}