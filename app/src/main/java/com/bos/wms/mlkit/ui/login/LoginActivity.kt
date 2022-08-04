package com.bos.wms.mlkit.ui.login

import Model.LocationModel
import Model.UserLoginModel
import Model.UserLoginResultModel
import Remote.APIClient
import Remote.BasicApi
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.R
import com.bos.wms.mlkit.app.MainMenu
import com.bos.wms.mlkit.customView.PopWindowMain
import com.bos.wms.mlkit.databinding.ActivityLoginBinding
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_login.*
import java.io.IOException


class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding
    lateinit var api: BasicApi
    var compositeDisposable= CompositeDisposable()

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
            throw(IOException("Error logging in", e))
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
            throw(IOException("Error logging in", e))
        }
        finally {
        }
    }
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
    }
    fun login(username: String, password: String) {

        try {
            // TODO: handle loggedInUser authentication
            val ddlLogin: Spinner = findViewById(R.id.ddlFloor)
            if(locationModel==null)
                return
            val location:String=locationModel!!.locations[ddlLogin.selectedItemPosition].location
            val locationID:Int = locationModel!!.locations[ddlLogin.selectedItemPosition].id

            General.getGeneral(applicationContext).FloorID=locationID
            General.getGeneral(applicationContext).saveGeneral(applicationContext)

            Log.println(Log.DEBUG,"Location",location+ " - " + locationID.toString())
            var usr: UserLoginModel =  UserLoginModel(username,password)
            api= APIClient.getInstance(IPAddress,true).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.Login(usr)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            updateUiWithUser(s) },
                        {t:Throwable?->
                            run {
                                showLoginFailed(t.toString())
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
        PopWindowMain(
            v, this@LoginActivity
        ) {
            IPAddress= mStorage.getDataString("IPAddress", "192.168.10.82")
            getLocations(2)
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
        val loading: ProgressBar = findViewById(R.id.loading)

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

            login.setOnClickListener {
                //   loading.visibility = View.VISIBLE
                login(username.text.toString(), password.text.toString())
            }
        }
    }

    /*
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val username = binding.username
        val password = binding.password
        val login = binding.login
        val loading = binding.loading

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })



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

            login.setOnClickListener {
                loading.visibility = View.VISIBLE
                login(username.text.toString(), password.text.toString())
            }
        }
    }
*/
    private fun updateUiWithUser(model: UserLoginResultModel) {

        General.getGeneral(applicationContext).UserID=model.Id
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