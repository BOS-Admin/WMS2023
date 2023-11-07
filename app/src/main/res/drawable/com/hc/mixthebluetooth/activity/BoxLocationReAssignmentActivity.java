package com.hc.mixthebluetooth.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bos.wmsapp.CustomViews.UPCCardDoneView;
import com.bos.wmsapp.CustomViews.UPCCardView;
import com.hc.basiclibrary.ioc.ViewById;
import com.hc.basiclibrary.viewBasic.BasActivity;
import com.hc.mixthebluetooth.Logger;
import com.hc.mixthebluetooth.R;
import com.hc.mixthebluetooth.Remote.Routes.BasicApi;
import com.hc.mixthebluetooth.storage.Storage;
import com.util.UPCAHelper;

import java.util.ArrayList;
import java.util.HashMap;

import Model.AssignBoxLocationResponse;
import Model.AuditRemoveUPCsResponse;
import Model.AuditUPCsToRemove;
import Remote.APIClient;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class BoxLocationReAssignmentActivity extends BasActivity  {

    @ViewById(R.id.btnPopUp)
    private Button btnPopUp;

    @ViewById(R.id.txtBox)
    private EditText txtBox;

    @ViewById(R.id.txtOldLocation)
    private TextView txtOldLocation;

    @ViewById(R.id.txtNewLocation)
    private TextView txtNewLocation;
    @ViewById(R.id.textValidBox)
    private TextView txtValidBox;

    private Storage mStorage;
    private String IPAddress;
    private int UserID;

    private AssignBoxLocationResponse model=null;
    private String popUpMessages="";


    private int boxLocationScoreId=0;
    HashMap<String,ArrayList<String>> removedItems=new HashMap<>();

    public static String ValidBoxBarcode =null;
    public static String StationCode =null;

    private boolean removeUPcs = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reassign_box_location);
        //设置头部
        //setTitle();
        setContext(this);

        mStorage = new Storage(this);//sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
        UserID = mStorage.getDataInt("UserID", -1);
        StationCode = mStorage.getDataString("StationCode", "");

        btnPopUp.setOnClickListener(e -> {
                showMessage("Info",popUpMessages);
        });


        txtBox.setInputType(InputType.TYPE_NULL);

        txtBox.requestFocus();

        txtBox.addTextChangedListener(new BoxTextWatcher());

        txtNewLocation.setOnClickListener(e -> {
            if(removeUPcs){
                Intent myIntent = new Intent(getApplicationContext(),AuditRemoveRemoveUPCActivity.class);
                AuditRemoveRemoveUPCActivity.BoxBarcode=ValidBoxBarcode;
                startActivity(myIntent);
            }
            else
                showMessage("Location",String.join("\n",getLocationMessages()));
        });



    }


    private ArrayList<String> getLocationMessages() {
        ArrayList<String> messages = new ArrayList<>();
        messages.add(locationFullMessage);
        return messages;
    }


    boolean updatingText = false;
    private String error = "";

    private void Beep() {
        new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300);
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

    class BoxTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (updatingText)
                return;
            updatingText = true;

            resetLocation();
            String box = txtBox.getText().toString();
            if (box.length() < 10 || box.length() >13) {
                Beep();
                showMessage("Error","Invalid Box Code ("+box+")");
                txtBox.setText("");
                updatingText = false;
                return;
            }

            txtBox.setEnabled(false);
            ReAssignBox(box);

        }
    }

    private void ReAssignBox (String BoxBarcode) {

        txtBox.setEnabled(false);
        try {

            mStorage = new Storage(this);
            //   String IPAddressWarehouseManager = mStorage.getDataString("IPAddressWarehouseManager", "192.168.10.82");
            BasicApi api = APIClient.getInstanceStatic(IPAddress, true).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.ReAssignBinLocation(UserID, BoxBarcode, StationCode, true)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {

                                    model = s;
                                    txtOldLocation.setText(s.getPrevLocation());

                                    if (s.getLocation().equals("") || s.getLocation().equals("No Location"))
                                        failLocation(truncate(s.getLocation(), 50), s.getResult());

                                    else if (s.getLocation().equals("W1005") && s.getRemoveUPCs()) {
                                        removeUPcs = true;
                                        failLocation("Remove UPCs", "Remove Extra UPCs");
                                    } else
                                        successLocation(s.getLocation(), s.getResult());

                                    Logger.Debug("API", "Assign Location : response " + s);
                                }


                                runOnUiThread(() -> {
                                    ValidBoxBarcode=BoxBarcode;
                                    txtValidBox.setText(BoxBarcode);

                                    txtBox.setEnabled(true);
                                    updatingText = true;
                                    txtBox.setText("");
                                    txtBox.requestFocus();
                                    updatingText = false;

                                });


                            }, (throwable) -> {

                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    String response = ex.response().errorBody().string();
                                    if (response.isEmpty()) {
                                        response = throwable.getMessage();
                                    }
//
                                    showMessage("Error", response);
                                    popUpMessages = response;
                                    Logger.Debug("API", "Error - Returned HTTP Error " + response);
                                } else {
//
                                    showMessage("Error", throwable.getMessage());
                                    popUpMessages = throwable.getMessage();
                                }
                                playError();
                                runOnUiThread(() -> {
                                    ValidBoxBarcode=BoxBarcode;
                                    txtValidBox.setText(BoxBarcode);

                                    txtBox.setEnabled(true);
                                    updatingText = true;
                                    txtBox.setText("");
                                    txtBox.requestFocus();
                                    updatingText = false;

                                });


                            }));
        } catch (Throwable e) {
            runOnUiThread(() -> {
                ValidBoxBarcode=BoxBarcode;
                txtValidBox.setText(BoxBarcode);

                txtBox.setEnabled(true);
                updatingText = true;
                txtBox.setText("");
                txtBox.requestFocus();
                updatingText = false;

            });


            showMessage("Exception", e.getMessage());
            popUpMessages = "Exception\n" + e.getMessage();
            playError();
            Logger.Error("API", "Assign Box  Exception: " + e.getMessage());
            //showMessage(e.getMessage());

        }
    }

    String locationFullMessage = "";

    private void resetLocation () {
        txtValidBox.setText("");
        locationFullMessage = "";
        ValidBoxBarcode="";
        txtValidBox.setText("");

        runOnUiThread(() -> {
            txtNewLocation.setText("");
            txtNewLocation.setBackgroundResource(R.drawable.rounded_corner1);
            txtOldLocation.setText("");
        });
    }

    private void failLocation (String message, String fullMessage){
        locationFullMessage = fullMessage;
        error += "\n" + locationFullMessage;
        runOnUiThread(() -> {
            txtNewLocation.setText(truncate(message, 40));
            txtNewLocation.setBackgroundResource(R.drawable.rounded_corner_red);
        });
    }



    private void successLocation (String message, String fullMessage){
        locationFullMessage = fullMessage;
        runOnUiThread(() -> {
            txtNewLocation.setText(truncate(message, 40));
            txtNewLocation.setBackgroundResource(R.drawable.rounded_corner_green);
        });
    }





    private String truncate(String text, int length) {
        if (text == null)
            return "";
        if (text.length() <= length) {
            return text;
        } else {
            return text.substring(0, length);
        }
    }




    @Override
    public void initAll() {

    }


}