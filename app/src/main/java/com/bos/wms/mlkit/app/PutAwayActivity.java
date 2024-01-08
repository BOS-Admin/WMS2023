package com.bos.wms.mlkit.app;

import android.app.AlertDialog;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bos.wms.mlkit.General;
import com.bos.wms.mlkit.R;
import com.bos.wms.mlkit.app.bosApp.PackingDCActivity;
import com.bos.wms.mlkit.customView.UPCCardDoneView;
import com.bos.wms.mlkit.storage.Storage;

import java.util.ArrayList;
import java.util.List;

import Model.BoxTypeModel;
import Model.BrandInToIsResponseModel;
import Model.Pricing.PricingStandModel;
import Remote.APIClient;
import Remote.BasicApi;
import Remote.UserPermissions.UserPermissions;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class PutAwayActivity extends AppCompatActivity {


    boolean updatingText = false;
    TextView txtRack;
    TextView lblType;
    TextView lblAssignedRack;
    TextView txtBox;
    TextView lblResult;
    String IPAddress;

    String assignedRack;
    String boxCode;
    ArrayList<String> putAwayList = new ArrayList<>();
    ArrayList<String> boxes = new ArrayList<>();
    ArrayAdapter adapter;

    List<BoxTypeModel> boxTypes=null;

    BoxTypeModel currentType=null;

    public static boolean allowRackOverride=false;

    //    TextView txtCount;
//
    Button btnShowPopUp;
General general;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        Storage mStorage = new Storage(this);
        IPAddress = mStorage.getDataString("IPAddress2", "192.168.50.20");
        general = General.getGeneral(getApplicationContext());

        setContentView(R.layout.activity_put_away);
     //   SetActivityMainView(findViewById(R.id.putAway));
        adapter = new ArrayAdapter<String>(this,
                R.layout.activity_listview, putAwayList);

        ListView listView = findViewById(R.id.items_list);
        listView.setAdapter(adapter);
        txtRack = findViewById(R.id.txtRack);
        lblType = findViewById(R.id.lblType);
        lblResult = findViewById(R.id.lblResult);
        txtBox = findViewById(R.id.txtBox);
        lblAssignedRack = findViewById(R.id.lblAssignedRack);
        btnShowPopUp =findViewById(R.id.btnPopUp);
        btnShowPopUp.setOnClickListener(e->{
            ShowDetails();
        });
        txtRack.addTextChangedListener(new RackTextWatcher());
        txtBox.addTextChangedListener(new PaletteTextWatcher());
        txtRack.setEnabled(false);


        allowRackOverride= UserPermissions.HasPermission("Replenishment.OverrideAssignedRackPutAway");
        SetPopup("Info","Allow Rack Assignment Override: "+allowRackOverride);

        txtBox.setEnabled(false);
        lblResult.requestFocus();
        getBoxTypes();
    }

    String popupTitle="";
    String popupMessage="";
    public void ShowDetails(){
        runOnUiThread(() -> {

            new AlertDialog.Builder(this)
                    .setTitle(popupTitle==null?"Title":popupTitle)
                    .setMessage(popupMessage==null?"":popupMessage)
                    .setPositiveButton("OK", (dialog, which) -> {
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    // .setCancelable(false)
                    .show();

        });
    }




    public void getBoxTypes() {
        try {

            txtBox.setEnabled(false);
            BasicApi api = APIClient.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.GetBoxTypes()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s.size()>0){
                                    boxTypes=s;
                                    txtBox.setEnabled(true);
                                    txtBox.requestFocus();
                                }

                                else {

                                    showMessage("Error","Failed to get box types",false);
                                }


                            }, (throwable) -> {
                                String error = "";
                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    error = ex.response().errorBody().string();
                                    if (error.isEmpty()) error = throwable.getMessage();
                                    error =error  +"\n (API Error)" ;

                                } else {
                                    error =throwable.getMessage() +"\n (API Error)" ;
                                }
//                                Logger.Log("Get Box Types ", "Get Box Types ", error);
                                UpdateResult("Failed","Failed",error, false);

                            }));
        } catch (Throwable e) {
//            Logger.Log("Replenishment", "PutAway", "Get Box Types  - Exception: " + e.getMessage());
            UpdateResult("Failed","Failed", e.getMessage()+"\n (Exception)", false);

        }
    }


    private void showMessage(String title,String msg,boolean success) {

        UpdateResult(title,msg,success);
        runOnUiThread(() -> {
            if(!success)
                Beep();
            lblResult.setText(msg);
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



    public BoxTypeModel determineType(String box){
        lblType.setText("Box Type: ");
        if(boxTypes==null || boxTypes.size()==0) {
            UpdateResult("Failed to get box types",false);
            return null;
        }
        for(BoxTypeModel m : boxTypes){
            String[] types=m.getPrefix().split(",");
            for(String t : types){
                if(box.startsWith(t)){
                    lblType.setText("Box Type: "+m.getType());
                    return m;
                }

            }
        }
        return  null;
    }



    public void validatePalette(String box) {
        try {

            txtBox.setEnabled(false);
            if(boxes.contains(box)){
                txtBox.setText("");
                updatingText=false;
                txtBox.setEnabled(true);
                txtBox.requestFocus();
                showMessage("Error","Box ("+box+") already scanned !!!",false);
                return;
            }

            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.ValidatePutAwayPalette(box,  general.UserID)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if ( s.length() <100){

                                    boxCode =box;
                                    assignedRack=s;
                                    txtRack.setEnabled(true);
                                    txtRack.requestFocus();
                                    lblAssignedRack.setText(s);

                                }

                                else {
                                    showMessage("Error","XX Invalid Palette XX\n ("+box+")", false);
                                    txtBox.setText("");
                                    updatingText = false;
                                    txtBox.setEnabled(true);
                                    txtBox.requestFocus();
                                }

                                updatingText=false;
                            }, (throwable) -> {
                                String error = "";
                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    error = ex.response().errorBody().string();
                                    if (error.isEmpty()) error = throwable.getMessage();
                                    error+="\n (API Error)" ;
                                } else {
                                    error = throwable.getMessage()+"\n (API Error)" ;
                                }
//                                Logger.Log("PutAway", "Validate Palette", error);
                                UpdateResult("Failed","Failed",error, false);
                                txtBox.setText("");
                                updatingText = false;
                                txtBox.setEnabled(true);
                                txtBox.requestFocus();
                            }));
        } catch (Throwable e) {
//            Logger.Log("Replenishment", "PutAway", "Validate Palette - Exception: " + e.getMessage());
            UpdateResult("Failed","Failed","Validate Palette - Exception: " + e.getMessage(), false);
            txtRack.setText("");
            updatingText = false;
            txtBox.setEnabled(true);
            txtBox.requestFocus();
        }
    }


    public void postPalettePutAway() {
        try {

            txtRack.setEnabled(false);


            BasicApi api = APIClient.getInstanceStatic(IPAddress, false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.PostPutAway(boxCode, txtRack.getText().toString(), general.UserID  ,allowRackOverride)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s){
                                    UpdateResult("Success","Post PutAway Done ",true);
                                    boxes.add(boxCode);
                                    putAwayList.add(boxCode +" => "+  txtRack.getText().toString());
                                    adapter.notifyDataSetChanged();
                                    //txtCount.setText("Count = "+putAwayList.stream().count());
                                    assignedRack=null;
                                    boxCode =null;
                                    txtBox.setText("");
                                    txtRack.setText("");
                                    updatingText = false;
                                    txtRack.setEnabled(false);
                                    txtBox.setEnabled(true);
                                    txtBox.requestFocus();
                                    lblAssignedRack.setText("");
                                }

                                else {
                                    showMessage("Error","XX Invalid PutAway XX\n ("+ boxCode +","+ assignedRack +")", false);
                                    txtRack.setText("");
                                    txtRack.setEnabled(true);
                                    txtRack.requestFocus();
                                    updatingText = false;
                                    lblAssignedRack.setText("");
                                }



                            }, (throwable) -> {
                                String error = "";
                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    error =ex.response().errorBody().string();
                                    if (error.isEmpty()) error = throwable.getMessage();
                                    error =error +"\n (API Error)";
                                } else {
                                    error = throwable.getMessage()+"\n (API Error)";
                                }
//                                Logger.Log("PutAway", "Validate PutAway", error);
                                UpdateResult("Failed","Failed",error, false);

                                txtRack.setText("");
                                txtRack.setEnabled(true);
                                txtRack.requestFocus();
                                updatingText = false;

                                lblAssignedRack.setText("");
                            }));
        } catch (Throwable e) {
//            Logger.Log("Replenishment", "PutAway", "Validate PutAway - Exception: " + e.getMessage());
            UpdateResult("Failed","Failed","Validate PutAway - Exception: " + e.getMessage(), false);
            txtRack.setText("");
            txtRack.setEnabled(true);
            txtRack.requestFocus();
            updatingText = false;
            txtRack.setEnabled(true);
            lblAssignedRack.setText("");

        }
    }


    public void validateBin(String bin) {
        try {

            ResetResult();
            txtBox.setEnabled(false);
            if(boxes.contains(bin)){
                txtBox.setText("");
                updatingText=false;
                txtBox.setEnabled(true);
                txtBox.requestFocus();
                showMessage("Error","Bin ("+bin+") already scanned !!!",false);
                return;
            }

            BasicApi api = APIClient.INSTANCE.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.ValidateClassBPutAway(bin, general.UserID)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s){
                                    boxCode =bin;
                                    txtRack.setEnabled(true);
                                    txtRack.requestFocus();

                                }

                                else {
                                    showMessage("Error","XX Invalid Bin XX\n ("+bin+")",false);
                                    txtBox.setText("");
                                    updatingText = false;
                                    txtBox.setEnabled(true);
                                    txtBox.requestFocus();
                                }

                                updatingText=false;
                            }, (throwable) -> {
                                String error = "";
                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    error = ex.response().errorBody().string() ;
                                    if (error.isEmpty()) error = throwable.getMessage();
                                    error+=" \n (API Error)";
                                } else {
                                    error = throwable.getMessage()+"\n (API Error)";
                                }
//                                Logger.Log("PutAway", "Validate Bin", error);
                                UpdateResult("Error","Failed XX",error, false);
                                txtBox.setText("");
                                updatingText = false;
                                txtBox.setEnabled(true);
                                txtBox.requestFocus();
                            }));
        } catch (Throwable e) {
//            Logger.Log("Replenishment", "PutAway", "Validate Bin - Exception: " + e.getMessage());
            UpdateResult("Error","Failed XX",e.getMessage() +" \n(Validate Bin - Exception)", false);
            txtRack.setText("");
            updatingText = false;
            txtBox.setEnabled(true);
            txtBox.requestFocus();
        }
    }


    public void postPutAway(String rack) {
        try {
            ResetResult();
            txtRack.setEnabled(false);
            BasicApi api = APIClient.INSTANCE.getInstanceStatic(IPAddress,false).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.PostClassBPutAway(boxCode, rack, general.UserID,currentType.getId()==2?'B':'N')
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s){
                                    UpdateResult("Success","Post PutAway Done ",true);
                                    boxes.add(boxCode);
                                    putAwayList.add(0, boxCode +" => "+ rack);
                                    if(putAwayList.size()>200)
                                        putAwayList.remove(200);
                                    adapter.notifyDataSetChanged();
                                    //txtCount.setText("Count = "+putAwayList.stream().count());
                                    boxCode =null;
                                    txtBox.setText("");
                                    txtRack.setText("");
                                    updatingText = false;
                                    txtRack.setEnabled(false);
                                    txtBox.setEnabled(true);
                                    txtBox.requestFocus();

                                }

                                else {
                                    UpdateResult("Failed","XX Invalid PutAway XX\n ("+ boxCode +","+ rack +")", false);
                                    txtRack.setText("");
                                    txtRack.setEnabled(true);
                                    txtRack.requestFocus();
                                    updatingText = false;
                                }



                            }, (throwable) -> {
                                String error = "";
                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    error = ex.response().errorBody().string();
                                    if (error.isEmpty()) error = throwable.getMessage();
                                    error+="\n(API Error)";
                                } else {
                                    error = throwable.getMessage()+"\n(API Error)";
                                }
                                UpdateResult("Failed",error, false);

                                txtRack.setText("");
                                txtRack.setEnabled(true);
                                txtRack.requestFocus();
                                updatingText = false;

                            }));
        } catch (Throwable e) {
//            Logger.Log("Replenishment", "PutAway", "Validate PutAway - Exception: " + e.getMessage());
            UpdateResult("Failed",e.getMessage() +"\n"+"(Validate PutAway - Exception)" , false);
            txtRack.setText("");
            txtRack.setEnabled(true);
            txtRack.requestFocus();
            updatingText = false;

        }
    }



    class RackTextWatcher implements TextWatcher {

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

            String rack = txtRack.getText().toString();
            if (rack.length() < 5) {
                Beep();
                showMessage("Error","Invalid Rack Code ("+rack+")", false);
                txtRack.setText("");
                updatingText = false;
                return;
            }


            if(boxCode ==null || (currentType.getId()==1  && assignedRack==null) ){
                Beep();
                showMessage("Error"," Scan Box Code First" , false);
                txtRack.setText("");
                updatingText = false;
                return;
            }

            if(currentType.getId()==1 && !allowRackOverride && !assignedRack.equals("No_Assigned_Rack") && !rack.equals(assignedRack)){
                Beep();
                showMessage("Error","Invalid Rack Code ("+rack+"), must be ("+assignedRack+")" , false);
                txtRack.setText("");
                updatingText = false;
                return;
            }

            if(currentType==null)
            {
                UpdateResult("Failed to determine box type",false);
                return;
            }
            if(currentType.getId()==1)
                postPalettePutAway();
            else
                postPutAway(rack);


        }
    }

    class PaletteTextWatcher implements TextWatcher {

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

            lblResult.setText("");
            popupTitle="";
            popupMessage="";

            updatingText = true;
            lblAssignedRack.setText("");

            String box = txtBox.getText().toString();
            if (box.length() < 5) {
                Beep();
                showMessage("Error","Invalid Palette Code ("+box+")", false);
                txtBox.setText("");
                updatingText = false;

                return;
            }
            if (boxTypes==null)
            {
                Beep();
                showMessage("Error","Box Types Not Loaded Yet", false);
                txtBox.setText("");
                updatingText = false;

                return;
            }
            lblAssignedRack.requestFocus();

            currentType=determineType(box);


            if(currentType==null)
            {
                UpdateResult("Failed to determine box type",false);
                txtBox.setText("");
                updatingText=false;
                return;
            }

            if(currentType.getId()==1)
                validatePalette(box);
            else
                validateBin(box);

        }
    }



    private void Beep() {
        new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300);
    }




    private void enableActivity(boolean value){
        //btnClear.setEnabled(value);
        txtBox.setEnabled(value);
        txtRack.setEnabled(false);
        txtBox.requestFocus();
    }



    protected void ResetResult(){
        SetPopup("Info","");
        if(lblResult !=null){
            lblResult.setText("");
        }
    }



    private void SetPopup(String title,String fullMessage){
        popupTitle=title;
        popupMessage=fullMessage;
    }


    protected void UpdateResult(String message,boolean success){
        if (!success)
            Beep();
        SetPopup(success?"Info":"Error",message);
        if(lblResult !=null){
            lblResult.setText(message);
            lblResult.setTextColor(success?Color.GREEN:Color.RED);
        }
    }

    protected void UpdateResult(String message,String fullMessage,boolean success){
        if (!success)
            Beep();
        SetPopup(success?"Info":"Error",fullMessage);
        if(lblResult !=null){
            lblResult.setText(message);
            lblResult.setTextColor(success?Color.GREEN:Color.RED);
        }
    }

    protected void UpdateResult(String title,String message,String fullMessage,boolean success){
        if (!success)
            Beep();
        SetPopup(title,fullMessage);
        if(lblResult !=null){
            lblResult.setText(message);
            lblResult.setTextColor(success?Color.GREEN:Color.RED);
        }
    }



}