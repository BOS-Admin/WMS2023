package com.hc.mixthebluetooth
import Model.LocationModel
import Model.UserLoginModel
import Model.UserLoginResultModel
import Remote.APIClient
import Remote.APIClient.getInstanceStatic
import com.hc.mixthebluetooth.Remote.Routes.BasicApi
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bos.wms.mlkit.ui.login.LoginViewModel
import com.bos.wms.mlkit.ui.login.LoginViewModelFactory
import com.hc.mixthebluetooth.Remote.UserPermissions.UserPermissions
import com.hc.mixthebluetooth.activity.*
import com.hc.mixthebluetooth.customView.PopWindowMain
import com.hc.mixthebluetooth.storage.Storage
import com.util.General
import com.util.General.getGeneral
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
class LoginActivity : AppCompatActivity() {
    private lateinit var loginViewModel: LoginViewModel
    lateinit var api: BasicApi
    var compositeDisposable= CompositeDisposable()
    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }
    fun PromptInput(RFID: String?) {
        val alertdialog = AlertDialog.Builder(this)
        alertdialog.setTitle("Fill up Item info")
        val Weight = EditText(this)
        val Length = EditText(this)
        val Width = EditText(this)
        val Height = EditText(this)
        val BoxCode = EditText(this)

        Weight.imeOptions = EditorInfo.IME_ACTION_NEXT
        Length.imeOptions = EditorInfo.IME_ACTION_NEXT
        Width.imeOptions = EditorInfo.IME_ACTION_NEXT
        Height.imeOptions = EditorInfo.IME_ACTION_NEXT
        BoxCode.imeOptions = EditorInfo.IME_ACTION_NEXT

        Weight.hint = "Weight" //editbox1 hint
        Weight.gravity = Gravity.CENTER //editbox in center
        Weight.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        Length.hint = "L" //editbox2 hint
        Length.gravity = Gravity.CENTER
        Length.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        Width.hint = "W" //editbox3 hint
        Width.gravity = Gravity.CENTER
        Width.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        Height.hint = "H" //editbox4 hint
        Height.gravity = Gravity.CENTER
        Height.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL


        BoxCode.hint = "BoxCode" //editbox4 hint
        BoxCode.gravity = Gravity.CENTER

        //set up in a linear layout
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(20, 20, 20, 20) //set margin
        val layoutParams2 = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams2.setMargins(20, 20, 20, 20) //set margin
        //layoutParams2.width = 110
        layoutParams2.weight = 0.3f
        val lp = LinearLayout(applicationContext)
        lp.orientation = LinearLayout.VERTICAL
        val lp2 = LinearLayout(applicationContext)
        lp2.orientation = LinearLayout.HORIZONTAL
        lp2.gravity = Gravity.CENTER
        lp.addView(Weight, layoutParams)
        lp2.addView(Length, layoutParams2)
        lp2.addView(Width, layoutParams2)
        lp2.addView(Height, layoutParams2)
        lp.addView(BoxCode, layoutParams)
        lp.addView(lp2, layoutParams)
        alertdialog.setView(lp)
        alertdialog.setPositiveButton(
            "OK"
        ) { dialogInterface: DialogInterface?, i: Int -> }
        alertdialog.setNegativeButton(
            "Cancel"
        ) { dialogInterface, i ->

            dialogInterface.dismiss()
        }
        runOnUiThread {
            val alert = alertdialog.create()
            alert.setCanceledOnTouchOutside(false)
            alert.show()
        }

    }

    /**
     * This functions get the current user location from the current isp ip
     * This also gets the default repair warehouse location code
     */
    fun loadRepairLocationData(){
        try {
            val mStorage = Storage(this) //sp存储
            val IPAddress =  mStorage.getDataString("IPAddress", "192.168.50.20:5000")
            val LocalIP=mStorage.localIpAddress

            val api = getInstanceStatic(IPAddress, false).create<BasicApi>(BasicApi::class.java)
            val compositeDisposable = CompositeDisposable()
            compositeDisposable.addAll(
                api.GetLocation(LocalIP, 2, -1)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s: LocationModel? ->
                            if (s != null) {
                                TransferToRepairActivity.CurrentLocation = s.mainLocation;//"W1005";//s.mainLocation;
                                Logger.Debug("GotLocation", LocalIP + " " + s.mainLocation)
                            }
                        }
                    ) { throwable: Throwable ->
                        if (throwable is HttpException) {
                            val ex = throwable as HttpException
                            var response = ex.response().errorBody()!!.string()
                            if (response.isEmpty()) {
                                response = "API Error Occurred"
                            }
                            Logger.Debug(
                                "API",
                                "CheckLocation - Error In HTTP Response: $response"
                            )
                        } else {
                            Logger.Error(
                                "API",
                                "CheckLocation - Error In Response: " + throwable.message
                            )
                        }
                    },
                api.GetSystemControlValue("RepairLocationCode")
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s: String? ->
                            if (s != null) {
                                TransferToRepairActivity.CurrentRepairLocationCode = s;
                                Logger.Debug("GotRepairLocation", s)
                            }
                        }
                    ) { throwable: Throwable ->
                        if (throwable is HttpException) {
                            val ex = throwable as HttpException
                            var response = ex.response().errorBody()!!.string()
                            if (response.isEmpty()) {
                                response = "API Error Occurred"
                            }
                            Logger.Debug(
                                "API",
                                "CheckLocation - SystemControl - Error In HTTP Response: $response"
                            )
                        } else {
                            Logger.Error(
                                "API",
                                "CheckLocation - SystemControl - Error In Response: " + throwable.message
                            )
                        }
                    })
        } catch (e: Throwable) {
            Logger.Error("API", "CheckLocation - Error Connecting: " + e.message)
        }
    }

    fun getLocations( LocationTypeID: Int) {

        try {
            // TODO: handle loggedInUser authentication
            val mStorage = Storage(this) //sp存储
            val IPAddress =  mStorage.getDataString("IPAddress", "192.168.50.20:5000")
            val LocalIP=mStorage.localIpAddress

            api= APIClient.getInstance(IPAddress,true).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.GetLocation(LocalIP,LocationTypeID,-1)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            General.getGeneral(this).mainLocation = s.mainLocation
                            General.getGeneral(this).mainLocationId = s.mainLocationID
                            updateUiWithLocations(s)
                           // PromptInput("hlnk")
                             },
                        {t:Throwable?->
                            run {
                                showLoginFailed("Locations:" +IPAddress +t.toString())
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            showLoginFailed("Error logging in:"+ e)
        }
        finally {
        }
    }
    lateinit var locationModel: LocationModel
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
        val ddlLogin:Spinner = findViewById(R.id.ddlFloor)
        ddlLogin.setAdapter(adapter)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun GetSystemControl(UserID: Int, FloorID: Int, model: UserLoginResultModel) {

        try {
            // TODO: handle loggedInUser authentication
            val mStorage = Storage(this) //sp存储
            val IPAddress = mStorage.getDataString("IPAddress", "192.168.50.20:5000")
            api= APIClient.getInstance(IPAddress ,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.GetSystemControls(UserID,FloorID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            General.getGeneral(applicationContext).saveGeneral(applicationContext,s)
                            val DefaultShelfNbOfBins:String= General.getGeneral(application).getSetting(applicationContext,"DefaultShelfNbOfBins")
                            Log.println(Log.DEBUG,"DefaultShelfNbOfBins",DefaultShelfNbOfBins)

                            val welcome = "Welcome "
                            val displayName = model.FullName
                            // TODO : initiate successful logged in experience
                            //Abdullah Changes

                            Toast.makeText(
                                applicationContext,
                                "$welcome $displayName",
                                Toast.LENGTH_LONG
                            ).show()

                            for( x in s.settings){
                                if(x.code=="LotBondTimeBetweenItems"){
                                    val mStorage = Storage(this) //sp存储
                                    mStorage.saveData("LotBondTimeBetweenItems",x.value)
                                }

                            }

                            //Attempt To Get The User Permissions
                            UserPermissions.Initialize(applicationContext, UserID)

                            //Abdullah Changes

//                            DeviceActivity.isReplenishment=true;
//                            mStorage.saveData("AntennaConnectionNextStep", "AuditAntenna")
//                            startActivity(Intent(this, DeviceActivity::class.java))

                            startActivity(Intent(this, UndoBoxLocationAssignmentActivity::class.java))
//                            startActivity(Intent(this, MainMenuActivity::class.java))
//                            finish()
                        },
                        {t:Throwable?->
                            run {
                                showLoginFailed(t.toString())
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            showLoginFailed("Login"+e.toString())
        }
        finally {
        }
    }
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Closing App")
            .setMessage("Are you sure you want to close the application?")
            .setPositiveButton("Yes",
                DialogInterface.OnClickListener { dialog, which -> finishAffinity();
                    System.exit(0); })
            .setNegativeButton("No", null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun login(username: String, password: String) {
        try {
            // TODO: handle loggedInUser authentication
            val ddlLogin:Spinner = findViewById(R.id.ddlFloor)
            val location:String=locationModel.locations[ddlLogin.selectedItemPosition].location
            val locationID:Int = locationModel.locations[ddlLogin.selectedItemPosition].id
            Log.println(Log.DEBUG,"Location",location+ " - " + locationID.toString())
            var mStorage = Storage(this) //sp存储
            mStorage.saveData("LocationID",locationID)

            var IPAddress = ""
            IPAddress = mStorage.getDataString("IPAddress", "192.168.50.20:5000")
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
                                if(t?.message!=null){
                                    run {
                                        showLoginFailed(t.message.toString()+ " (API Error )")
                                    }
                                }
                            }

                        }
                    )
            )
        } catch (e: Throwable) {
            showLoginFailed("Error logging in"+e.toString())
            //throw(IOException("Error logging in", e))
        }
        finally {
        }
    }

    lateinit var btnSettings:ImageView;

    private fun setPopWindow(v: View) {
        Extensions.setImageDrawableWithAnimation(btnSettings, getDrawable(R.drawable.baseline_close_icon), 300);
        PopWindowMain(
            v, this@LoginActivity
        ) {
            getLocations(2)
            //loadRepairLocationData()

            Extensions.setImageDrawableWithAnimation(btnSettings, getDrawable(R.drawable.baseline_settings_icon), 300);
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)
        val username:EditText = findViewById(R.id.txtusername)
        val password:EditText = findViewById(R.id.txtpassword)
        val login:Button = findViewById(R.id.btnLogin)
        btnSettings = findViewById(R.id.btnSettings)

        val txtAppVersion:TextView = findViewById(R.id.txtAppVersion)

        btnSettings.setOnClickListener {
            setPopWindow(it)
        }

        txtAppVersion.setText(getGeneral(this).AppVersion)
       // val loading:ProgressBar = findViewById(R.id.loading)

         var btnLogin:Button = findViewById(R.id.btnLogin)


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
        //loadRepairLocationData()

        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        login(
                            username.text.toString(),
                            password.text.toString()
                        )
                }
                false
            }

            //Abdullah Changes
            login.isEnabled=true;
            login.setOnClickListener {
                //throw RuntimeException("Test Crash");
                //   loading.visibility = View.VISIBLE
                //Abdullah Changes

               login("5555", "5555")
               // login(username.text.toString(), password.text.toString())
            }
        }

        Logger.Initialize(applicationContext)

    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateUiWithUser(model: UserLoginResultModel) {
        var mStorage = Storage(this) //sp存储
        mStorage.saveData("UserID",model.Id)
        mStorage.saveData("UserCode",model.UserCode)
        mStorage.saveData("UserFullName",model.FullName)

        GetSystemControl(model.Id!!,0,model)
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