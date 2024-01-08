package com.hc.mixthebluetooth.activity;

import android.app.AlertDialog;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

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

import Model.AuditRemoveUPCsResponse;
import Model.AuditUPCsToRemove;
import Remote.APIClient;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class AuditRemoveRemoveUPCActivity extends BasActivity  {

    @ViewById(R.id.btnPopUp)
    private Button btnPopUp;

    @ViewById(R.id.txtBox)
    private EditText txtBox;

    @ViewById(R.id.txtItem)
    private EditText txtItem;
    private Storage mStorage;
    private String IPAddress;
    private int UserID;

    private AuditRemoveUPCsResponse model=null;
    private String popUpMessages="";
    private String validBoxBarcode="";

    private int boxLocationScoreId=0;
    HashMap<String,ArrayList<String>> removedItems=new HashMap<>();

    public static String BoxBarcode =null;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audit_remove_upcs);
        //设置头部
        //setTitle();
        setContext(this);

        mStorage = new Storage(this);//sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82");
        UserID = mStorage.getDataInt("UserID", -1);

        btnPopUp.setOnClickListener(e -> {
                showMessage("Info",popUpMessages);
        });

        txtItem.setInputType(InputType.TYPE_NULL);
        txtBox.setInputType(InputType.TYPE_NULL);

        txtBox.requestFocus();

        txtBox.addTextChangedListener(new BoxTextWatcher());
        txtItem.addTextChangedListener(new ItemTextWatcher());

        txtItem.setEnabled(false);



    }




    boolean updatingText = false;

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

            String box = txtBox.getText().toString();
            if (box.length() < 10 || box.length() >13) {
                Beep();
                showMessage("Error","Invalid Box Code ("+box+")");
                txtBox.setText("");
                updatingText = false;
                return;
            }

            if(BoxBarcode!=null && !BoxBarcode.equals(box)){
                Beep();
                showMessage("Error","Please scan this box: "+BoxBarcode);
                txtBox.setText("");
                updatingText = false;
                return;
            }
            txtBox.setEnabled(false);
            GetUPcs(box);

        }
    }

    class ItemTextWatcher implements TextWatcher {

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

            String item = txtItem.getText().toString();
            if (item.length() < 10 || item.length() >14) {
                Beep();
                showMessage("Error","Invalid ItemCode Code ("+item+")");
                txtItem.setText("");
                updatingText = false;
                return;
            }

            UPCAHelper helper=new UPCAHelper();
            if(helper.isValidUPCA(item)){
                item = helper.convertToIS(item);
            }

            if(isNumeric(item)){
                item="IN"+item;
            }

            txtItem.setEnabled(false);




           RemoveItem(item,validBoxBarcode);

        }
    }


    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch(Exception e){
            return false;
        }
    }


    @ViewById(R.id.layoutUPCs)
    LinearLayout upcsLayout;

    public void renderItems(){
        if(model == null || model.getItems()==null)
            return;
        upcsLayout.removeAllViews();

        ArrayList<String> items=new ArrayList<>();
        items.add("1");
        items.add("2");
        items.add("3");
        items.add("4");

        for (AuditUPCsToRemove detail:model.getItems()) {

            ArrayList<String> itemsList=new ArrayList<>();

            try{
                if(removedItems.containsKey(detail.getUpc())){
                    itemsList= removedItems.get(detail.getUpc());
                }
            }catch (Exception e){

            }

            if(detail.getQtyRemoved()==detail.getQtyToRemove()){

                addDoneItemToLayout(detail.getUpc()," ",detail.getQtyRemoved()+"/"+detail.getQtyToRemove(),itemsList);
            }
            else{
                addItemToLayout(detail.getUpc()," ",detail.getQtyRemoved()+"/"+detail.getQtyToRemove(),itemsList);
            }
        }
    }


    private void addItemToLayout(String item,String desc,String qty,ArrayList<String> items) {
        runOnUiThread(()->{
            upcsLayout.addView(new UPCCardView(this, item, qty,items));
        });
    }

    private void addDoneItemToLayout(String item,String desc,String qty,ArrayList<String> items) {
        runOnUiThread(()->{
            upcsLayout.addView(new UPCCardDoneView(this, item,qty,items));
        });
    }


    private void GetUPcs (String BoxBarcode) {
        txtBox.setEnabled(false);
        try {

            mStorage = new Storage(this);
            //   String IPAddressWarehouseManager = mStorage.getDataString("IPAddressWarehouseManager", "192.168.10.82");
            BasicApi api = APIClient.getInstanceStatic(IPAddress, true).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.GetAuditUPCsToRemove(UserID,BoxBarcode)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {

                                    model=s;

                                    if(s.getItems().size()==0){
                                        showMessage("Info","No UPCS Found");
                                        popUpMessages="No UPCs Found";
                                        runOnUiThread(()->{

                                            updatingText = true;
                                            txtBox.setText("");
                                            updatingText = false;
                                            txtBox.setEnabled(true);;
                                        });
                                    }

                                    else{
                                        validBoxBarcode=BoxBarcode;
                                        boxLocationScoreId= s.getItems().get(0).getBoxLocationScoreId();
                                        runOnUiThread(()->{
                                            txtBox.setEnabled(false);
                                            txtItem.setEnabled(true);
                                            txtItem.requestFocus();
                                            updatingText = false;
                                            renderItems();
                                        });
                                    }







                                }
                            }, (throwable) -> {

                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    String response = ex.response().errorBody().string();
                                    if (response.isEmpty()) {
                                        response = throwable.getMessage();
                                    }
//
                                    showMessage("Error",response);
                                    popUpMessages=response;
                                    Logger.Debug("API", "Error - Returned HTTP Error " + response);
                                } else {
//
                                    showMessage("Error",throwable.getMessage());
                                    popUpMessages=throwable.getMessage();
                                }
                                playError();
                                runOnUiThread(()->{
                                    updatingText = true;
                                    txtBox.setText("");
                                    updatingText = false;
                                    txtBox.setEnabled(true);
                                });

                            }));
        } catch (Throwable e) {
            runOnUiThread(()->{
                updatingText = true;
                txtBox.setText("");
                updatingText = false;
                txtBox.setEnabled(true);
            });

            showMessage("Exception",e.getMessage());
            popUpMessages="Exception\n"+e.getMessage();
            playError();
            Logger.Error("API", "Audit Exception: " + e.getMessage());
            //showMessage(e.getMessage());

        }


    }



    private void RemoveItem (String Item,String BoxBarcode) {
        txtItem.setEnabled(false);
        try {

            mStorage = new Storage(this);
            //   String IPAddressWarehouseManager = mStorage.getDataString("IPAddressWarehouseManager", "192.168.10.82");
            BasicApi api = APIClient.getInstanceStatic(IPAddress, true).create(BasicApi.class);
            CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.addAll(
                    api.RemoveAuditUPC(UserID,BoxBarcode,Item,boxLocationScoreId)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((s) -> {
                                if (s != null) {

                                    model=s;

                                    if(s.getItems().size()==0){
                                        showMessage("Info","No UPCS Found");
                                        popUpMessages="No UPCs Found";
                                        runOnUiThread(()->{

                                            updatingText = true;
                                            txtItem.setText("");
                                            updatingText = false;
                                            txtItem.setEnabled(true);
                                            txtItem.requestFocus();
                                        });
                                    }

                                    else{

                                        try{

                                            String upcRemoved=s.getLastUPCRemoved();
                                           // showMessage("upcRemoved",upcRemoved);
                                            if(upcRemoved!=null){
                                                if(!removedItems.containsKey(upcRemoved))
                                                    removedItems.put(upcRemoved,new ArrayList<>());
                                                removedItems.get(upcRemoved).add(Item);
                                            }

                                        }catch (Exception e){

                                        }
                                        runOnUiThread(()->{
                                            txtItem.setEnabled(true);
                                            txtItem.requestFocus();
                                            updatingText = true;
                                            txtItem.setText("");
                                            updatingText = false;
                                            txtItem.requestFocus();
                                            renderItems();
                                        });
                                    }

                                }
                            }, (throwable) -> {

                                if (throwable instanceof HttpException) {
                                    HttpException ex = (HttpException) throwable;
                                    String response = ex.response().errorBody().string();
                                    if (response.isEmpty()) {
                                        response = throwable.getMessage();
                                    }
//
                                    showMessage("Error",response);
                                    popUpMessages=response;
                                    Logger.Debug("API", "Error - Returned HTTP Error " + response);
                                } else {
//
                                    showMessage("Error",throwable.getMessage());
                                    popUpMessages=throwable.getMessage();
                                }
                                playError();
                                runOnUiThread(()->{
                                    updatingText = true;
                                    txtItem.setText("");
                                    updatingText = false;
                                    txtItem.setEnabled(true);
                                    txtItem.requestFocus();

                                });

                            }));
        } catch (Throwable e) {
            runOnUiThread(()->{
                updatingText = true;
                txtItem.setText("");
                updatingText = false;
                txtItem.setEnabled(true);
                txtItem.requestFocus();
            });

            showMessage("Exception",e.getMessage());
            popUpMessages="Exception\n"+e.getMessage();
            playError();
            Logger.Error("API", "Audit Exception: " + e.getMessage());
            //showMessage(e.getMessage());

        }


    }


    @Override
    public void initAll() {

    }


}