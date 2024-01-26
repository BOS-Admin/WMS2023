package com.bos.wms.mlkit.ui.login

import Model.LocationModel
import Model.UserLoginModel
import Model.UserLoginResultModel
import Remote.APIClient
import Remote.BasicApi
import Remote.UserPermissions.UserPermissions
import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.bos.wms.mlkit.BuildConfig
import com.bos.wms.mlkit.Extensions
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.R
import com.bos.wms.mlkit.app.Logger
import com.bos.wms.mlkit.app.MainMenu
import com.bos.wms.mlkit.app.OCRBackgroundThread
import com.bos.wms.mlkit.app.ZebraPrinter
import com.bos.wms.mlkit.customView.PopWindowMain
import com.bos.wms.mlkit.databinding.ActivityLoginBinding
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_login.*
import retrofit2.HttpException
import java.io.*
import java.net.URL


class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding
    lateinit var api: BasicApi
    var compositeDisposable= CompositeDisposable()
    val REQUEST_CODE = 0x0000c0de
    private var apkFilePath = ""
    private var progressDialog: ProgressDialog? = null

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }
    fun GetSystemControl(UserID: Int, FloorID: Int) {

        try {
            // TODO: handle loggedInUser authentication

            api= APIClient.getInstance(IPAddress ,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.GetSystemControls(UserID,FloorID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            for(x in s.settings){
                                Log.i("settings-ah",x.code+" => "+x.value)

                        }

                            General.getGeneral(applicationContext).saveGeneral(applicationContext,s)
                            val DefaultShelfNbOfBins:String= General.getGeneral(application).getSetting(applicationContext,"DefaultShelfNbOfBins")
                            Log.println(Log.DEBUG,"DefaultShelfNbOfBins",DefaultShelfNbOfBins)
                        },
                        {t:Throwable?->
                            run {
                                showLoginFailed(t.toString())
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            showLoginFailed(e.toString())
        }
        finally {
        }
    }
    lateinit var  mStorage:Storage ;  //sp存储
    var IPAddress =""
    fun getLocations( LocationTypeID: Int) {

        try {
            // TODO: handle loggedInUser authentication

            val LocalIP=mStorage.localIpAddress

            api= APIClient.getInstance(IPAddress,true).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.GetLocation(LocalIP,LocationTypeID,-1)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            updateUiWithLocations(s)

                        },
                        {t:Throwable?->
                            run {
                                showLoginFailed(t.toString())
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            showLoginFailed(e.toString())
        }
        finally {
        }
    }


    //region Version
     fun getHighestAppVersion() {
        progressDialog = ProgressDialog.show(this, "", " Please Wait Checking for updates...", true)
        try {
            checkPermission()
            val currentAppVersion: String = UserPermissions.AppVersion
            Logger.Debug( "Version API", "Getting max App Version.., current App Version: $currentAppVersion")
            val api: BasicApi = APIClient.getInstance(IPAddress,true).create<BasicApi>(BasicApi::class.java)
            val compositeDisposable = CompositeDisposable()
            compositeDisposable.addAll(
                    api.GetAppVersion("WMSApp")
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe ({ s ->
                                if (s != null) {
                                    Logger.Debug("Version API", "Got Highest Version: " + s.version)
                                    val highestVersion: String = s.version
                                    if (toUpdateApp(currentAppVersion, highestVersion)) {
                                        Logger.Debug("Version API", "App Version isn't up to date, current App Version: $currentAppVersion highest App Version: $highestVersion")
                                        val fileName = "WMSApp" + s.version + ".apk"
                                        var downloadFileUrl: String = s.apiPath + fileName
                                        progressDialog!!.cancel()
                                        saveFile(downloadFileUrl, fileName)

                                    }else {
                                        Logger.Debug("Version API", "This Device Is Using The Latest Version No Need For Update")
                                        progressDialog!!.cancel()
                                    }
                                }
                            }, {t:Throwable?->
                                progressDialog!!.cancel()
                                Logger.Debug("Login", "Version API", "Error: " + t!!.message)
                            }))
        } catch (e: Throwable) {
            progressDialog!!.cancel()
            Logger.Debug("Login", "Version API", "Error Connecting: " + e.message)
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE)
        }
    }

    private fun toUpdateApp(currentVersion: String, highestVersion: String): Boolean {
        var currentVersion = currentVersion
        var highestVersion = highestVersion
        currentVersion = currentVersion.replace(".", "")
        highestVersion = highestVersion.replace(".", "")
        val currVersion = currentVersion.toInt()
        val highVersion = highestVersion.toInt()
        return currVersion < highVersion
    }


    private fun saveFile(url: String, fileName: String) {
        if(progressDialog != null){
            progressDialog!!.cancel()
            progressDialog = null
        }
        progressDialog = ProgressDialog(this)
        progressDialog!!.setMessage("Downloading $fileName Please Wait...")
        progressDialog!!.isIndeterminate = false
        progressDialog!!.setCancelable(false)
        progressDialog!!.max = 100
        progressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog!!.show()
        Thread {
            try {
                val dir = File(getOutputDirectory(), "/Downloads/") // Choose the directory to save to
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val file = File(dir, fileName) // Create a new file in the directory
                Logger.Debug("AutoUpdate", "Creating File For Download: " + file.absolutePath)
                if (!file.exists()) {
                    file.createNewFile()
                }
                val webURL = URL(url)
                val connection = webURL.openConnection()
                connection.connect()
                val lengthOfFile = connection.contentLength
                val input: InputStream = BufferedInputStream(webURL.openStream(), 8192)
                val output: OutputStream = FileOutputStream(file)
                val data = ByteArray(1024)
                var total: Long = 0
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    progressDialog!!.progress = (total * 100 / lengthOfFile).toInt()
                    output.write(data, 0, count)
                }
                Logger.Debug("AutoUpdate", "Auto Update Done Received: " + Extensions.HumanReadableByteCountBin(total))
                output.flush()
                output.close()
                input.close()
                apkFilePath = file.absolutePath
                if (apkFilePath != "") installAndOpenUpdatedApp()
                if (progressDialog != null) {
                    progressDialog!!.cancel()
                }
                finish()
            } catch (e: Exception) {
                if (progressDialog != null) progressDialog!!.cancel()
                Logger.Error("AutoUpdate", "Error Downloading File, $e")
            }
        }.start()
    }

    private fun getOutputDirectory(): File? {
        val allMediaDirs = externalMediaDirs
        val mediaDir = if (allMediaDirs.size > 0) allMediaDirs[0] else null
        if (mediaDir == null) {
            File(resources.getString(R.string.app_name)).mkdirs()
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    fun installAndOpenUpdatedApp() {
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
        val apkUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", File(apkFilePath))
        intent.data = apkUri
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        this.startActivity(intent)
    }

//end region

    var locationModel: LocationModel?=null
    private fun updateUiWithLocations(model: LocationModel) {
        locationModel=model

        val adapter: ArrayAdapter<*> = object : ArrayAdapter<Any?>(
            applicationContext,
            android.R.layout.simple_list_item_1, android.R.id.text1, model.locations.map { x->x.location }
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView = super.getView(position, convertView, parent) as TextView
                textView.textSize = 12f
                textView.setText(model.locations[position].location)

                return textView
            }
        }

        // Assign adapter to ListView

        // Assign adapter to ListView
        adapter.notifyDataSetChanged()
        val ddlLogin: Spinner = findViewById(R.id.ddlFloor)
        ddlLogin.setAdapter(adapter)
        getHighestAppVersion()
    }
    fun login(username: String, password: String) {

        try {
            // TODO: handle loggedInUser authentication
            val ddlLogin: Spinner = findViewById(R.id.ddlFloor)
            if(locationModel==null)
                return
            val location:String=locationModel!!.locations[ddlLogin.selectedItemPosition].location
            val locationID:Int = locationModel!!.locations[ddlLogin.selectedItemPosition].id
            val mainLocation:String=locationModel!!.mainLocation
            val mainLocationId:Int=locationModel!!.mainLocationID
            val subLocation:String = locationModel!!.locations[ddlLogin.selectedItemPosition].location
            //Log.i("LocationId","LocationId "+)
            General.getGeneral(applicationContext).LocationString=location;
            General.getGeneral(applicationContext).FloorID=locationID
            General.getGeneral(applicationContext).UserCode=username
            General.getGeneral(applicationContext).ipAddress=IPAddress
            General.getGeneral(applicationContext).mainLocation=mainLocation
            General.getGeneral(applicationContext).mainLocationID=mainLocationId
            General.getGeneral(applicationContext).subLocation=subLocation
            General.getGeneral(applicationContext).saveGeneral(applicationContext)

            Log.println(Log.DEBUG,"Location",location+ " - " + locationID.toString())
            var usr: UserLoginModel =  UserLoginModel(username,password, UserPermissions.AppName, UserPermissions.AppVersion)
            api= APIClient.getInstance(IPAddress,true).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.Login(usr)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->

                            try {
                                if(s.AuthToken != null){
                                    UserPermissions.AuthToken = s.AuthToken
                                    Logger.Debug("LOGIN", "Received A Valid Authorization Token");
                                }else {
                                    Logger.Debug("LOGIN", "Received An Empty Authorization Token");
                                }

                            }catch(e: Throwable){
                                Logger.Debug("LOGIN", "Failed Receiving Authorization Token");
                            }

                            updateUiWithUser(s) },
                        {t:Throwable?->
                            if(t is HttpException){
                                var ex: HttpException =t as HttpException
                                run {
                                    showLoginFailed( ex.response().errorBody()!!.string()+ " (Http Error)")
                                }
                            }
                            else{
                                if(t?.message!=null)
                                {
                                    run {
                                        showLoginFailed(t.message.toString()+ " (API Error )")
                                    }
                                }
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            throw(IOException("Error logging in", e))
        }
        finally {
        }
    }
    private fun setPopWindow(v: View) {
        Extensions.setImageDrawableWithAnimation(btnSettings, getDrawable(R.drawable.baseline_close_icon), 300);
        PopWindowMain(
            v, this@LoginActivity
        ) {
            IPAddress= mStorage.getDataString("IPAddress", "192.168.10.82")
            getLocations(2)
            getHighestAppVersion()
            Extensions.setImageDrawableWithAnimation(btnSettings, getDrawable(R.drawable.baseline_settings_icon), 300);
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        mStorage=Storage(this);
        IPAddress=mStorage.getDataString("IPAddress", "192.168.10.82")
        val username:EditText = findViewById(R.id.txtusername)
        val password:EditText = findViewById(R.id.txtpassword)
        val login: Button = findViewById(R.id.btnLogin)
        val btnSettings: ImageView = findViewById(R.id.btnSettings)
        btnSettings.setOnClickListener {
            setPopWindow(it)
        }
        txtAppVersion.setText(txtAppVersion.text.toString() + " " + General.getGeneral(this).AppVersion)

        var btnLogin: Button = findViewById(R.id.btnLogin)


        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@LoginActivity, androidx.lifecycle.Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid

            btnLogin.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        getLocations(2)

        //version stuff
       // getHighestAppVersion()
        username.afterTextChanged {
 //           loginViewModel.loginDataChanged(
 //               username.text.toString(),
 //               password.text.toString()
 //           )
            //password.requestFocus()
        }

        password.apply {
            afterTextChanged {
              // loginViewModel.loginDataChanged(
              //     username.text.toString(),
              //     password.text.toString()
              // )
              //  username.requestFocus()

                login.isEnabled=true
            }


        }

//        password.apply {
//            afterTextChanged {
//                loginViewModel.loginDataChanged(
//                    username.text.toString(),
//                    password.text.toString()
//                )
//                username.requestFocus()
//            }
//            setOnEditorActionListener { _, actionId, _ ->
//                when (actionId) {
//                    EditorInfo.IME_ACTION_DONE ->
//                        login(
//                            username.text.toString(),
//                            password.text.toString()
//                        )
//                }
//                false
//            }
//
//            login.setOnClickListener {
//                //   loading.visibility = View.VISIBLE
//                login(username.text.toString(), password.text.toString())
//            } //       }

        //Abdullah Changes
        login.isEnabled=true
        login.setOnClickListener {
            //   loading.visibility = View.VISIBLE
            //Abdullah Changes
           login(username.text.toString(), password.text.toString())
          //  login("5555","5555")
        }
        username.requestFocus()

        //Initialize classes
        Logger.Initialize(applicationContext)
        ZebraPrinter.establishFirstConnection(mStorage.getDataString("PrinterMacAddress", "00"))
        OCRBackgroundThread.Initialize(this);

    }


    private fun updateUiWithUser(model: UserLoginResultModel) {

        General.getGeneral(applicationContext).UserID=model.Id
        General.getGeneral(applicationContext).UserName=model.FullName
        General.getGeneral(applicationContext).saveGeneral(applicationContext)

        GetSystemControl(model.Id!!,0)
        val welcome = getString(R.string.welcome)
        val displayName = model.FullName

        // TODO : initiate successful logged in experience
        Toast.makeText(
            applicationContext,
            "$welcome $displayName",
            Toast.LENGTH_LONG
        ).show()

        //Initialize the user permissions with the corresponding user id
        UserPermissions.Initialize(applicationContext, General.getGeneral(applicationContext).UserID)

        startActivity(Intent(this, MainMenu::class.java))
        finish()
    }

    private fun showLoginFailed(errorString: String) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}