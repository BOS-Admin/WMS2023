package com.bos.wms.mlkit.app;

import android.app.AlertDialog;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.storage.Storage;

import Model.ClassBRepriceCountResponse;
import Model.ClassBRepriceResponseModel;
import Remote.APIClient;
import Remote.BasicApi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class StoreRepriceCountActivity extends AppCompatActivity {


    public String IPAddress = "";

    int UserID = -1;


    private Button btnPopUp;


    private TextView txtStore;






    private String popUpMessages="";
    private String branch="";

    private TextView textDone;

    private TextView textRemaining;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reprice_count);

        Storage mStorage = new Storage(getApplicationContext());
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");

        UserID = General.getGeneral(this).UserID;
        branch= General.getGeneral(this).mainLocation;

        branch="S1006";

        btnPopUp=findViewById(R.id.btnPopUp);
        txtStore =findViewById(R.id.txtStore);



        textDone =findViewById(R.id.textDone);
        textRemaining =findViewById(R.id.textRemaining);


        txtStore.setText(branch);

        btnPopUp.setOnClickListener(e -> {
            showMessage("Info",popUpMessages);
        });




        textDone.setText("...");
        textRemaining.setText("...");
        GetCount(branch);



    }


    private void showMessage(String title,String msg) {

        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(msg)
                    .setPositiveButton("OK", (dialog, which) -> {
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    // .setCancelable(false)
                    .show();

        });
    }


    boolean updatingText = false;
    private String error = "";






    private void GetCount(String item
    ) {

        txtStore.setEnabled(false);
        try {
            //   String IPAddressWarehouseManager = mStorage.getDataString("IPAddressWarehouseManager", "192.168.10.82");
            BasicApi api = APIClient.getInstanceStatic(IPAddress, true).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.GetRepriceCount(branch)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {
                                    success("Success",s);

                                    Logger.Debug("API", "Undo Assign Location : response " + s);
                                }




                            }, (throwable) -> {
                                String err="";
                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    String response = ex.response().errorBody().string();
                                    if (response.isEmpty()) {
                                        response = throwable.getMessage();
                                    }
//
                                    showMessage("Error", response);
                                    err= response;
                                    Logger.Debug("API", "Error - Returned HTTP Error " + response);
                                } else {
//
                                    showMessage("Error", throwable.getMessage());
                                    err=  throwable.getMessage();
                                }
                                fail("Failed",err);




                            }));
        } catch (Throwable e) {



            showMessage("Exception", e.getMessage());
            fail("Failed",e.getMessage());
            Logger.Error("API", "Assign Box  Exception: " + e.getMessage());
            //showMessage(e.getMessage());

        }
    }

    
    String fullMessage = "";


    private void Beep() {
        new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300);
    }
    private void success(String message, ClassBRepriceCountResponse res){
        this.fullMessage = fullMessage;
        popUpMessages=message+"\n"+fullMessage;
        runOnUiThread(() -> {

            textDone.setText(""+res.getDone());
            textRemaining.setText(""+res.getRemaining());


        });
    }


    private void fail (String message, String fullMessage){

        this.fullMessage = fullMessage;
        error += "\n" + fullMessage;
        popUpMessages=message+"\n"+fullMessage;
        runOnUiThread(() -> {

            Beep();
        });


    }




}