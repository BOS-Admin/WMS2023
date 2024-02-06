package com.bos.wms.mlkit.app.Utils;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bos.wms.mlkit.R;
import com.google.android.material.snackbar.Snackbar;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class AppHelperActivity extends AppCompatActivity {


    private View activityMainView = null;

    private Button activityResultButton = null;

    private String popupMessage="";
    private String popupTitle="Info";

    protected TextView lblResult;

    public int HelperColorOrange = Color.parseColor("#FFB200");
    public int HelperColorGreen = Color.parseColor("#52ac24");
    public int HelperColorRed = Color.parseColor("#ef2112");
    public int HelperColorBlack = Color.parseColor("#000000");

    public int HelperColorWhite = Color.parseColor("#FFFFFF");



    /**
     * Sets the activity main view which is used in showing snackbars and such
     * @param view
     */
    public void SetActivityMainView(View view){
        this.activityMainView = view;
        try{
            this.lblResult= findViewById(R.id.lblResult);
        }
        catch (Exception e){ }


    }

    /**
     * Sets the activity result button showcasing the current activity state
     * @param button
     */
    public void SetActivityResultButton(Button button){
        this.activityResultButton = button;
        this.activityResultButton.setVisibility(View.INVISIBLE);
    }

    /**
     * Gets the activity result button
     * @return
     */
    public Button GetActivityResultButton(){
        return this.activityResultButton;
    }

    /**
     * Gets the activity main view
     * @return
     */
    public View GetActivityMainView(){
        return this.activityMainView;
    }

    /**
     * Shows An Error Dialog That Closes The Activity On Done
     * @param title
     * @param message
     */
    public void ShowErrorReturnDialog(String title, String message){
        if(activityMainView != null){
            activityMainView.post(() -> ShowErrorReturnDialogOnThread(title, message));
        }else {
            ShowErrorReturnDialogOnThread(title, message);
        }
    }

    /**
     * Shows the dialog on the thread the function was called from
     * @param title
     * @param message
     */
    private void ShowErrorReturnDialogOnThread(String title, String message){
        AlertDialog builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Ok", (dialogInterface, i) -> finish()).show();
    }

    /**
     * This Function Is A Shortcut For Creating Dialogs
     */
    public AlertDialog.Builder CreateDialog(String title, String message, int icon){
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setIcon(icon)
                .setTitle(title)
                .setMessage(message);
        return builder;
    }

    /**
     * This Function Is A Shortcut For Displaying Alert Dialogs
     */
    public AlertDialog ShowAlertDialog(String title, String message){
        return CreateDialog(title, message, android.R.drawable.ic_dialog_alert).setPositiveButton("Ok", (dialog, which) -> {

        }).show();
    }

    /**
     * This Function Is A Shortcut For Displaying Alert Dialogs
     */
     public AlertDialog ShowAlertDialog(String title, String message,boolean success){
        UpdateResult(message,success);
        return ShowAlertDialog(title,message);
    }

    public AlertDialog ShowAlertDialog(String title, String message,String fullMessage,boolean success){
        UpdateResult(title,message,fullMessage,success);
        return ShowAlertDialog(title,message);
    }




    public AlertDialog ShowAlertDialogChoices(String title, String message,String msgPositive,String msgNegative,DialogInterface.OnClickListener positiveListener,DialogInterface.OnClickListener negativeListener ){
        return CreateDialog(title, message, android.R.drawable.ic_dialog_alert).setPositiveButton(msgPositive, positiveListener).setNegativeButton(msgNegative, negativeListener).show();
    }

    /**
     * This Function Is A Shortcut For Showing A Toast Message
     */
    public void ShowToastMessage(String message, int length){
        Toast.makeText(this, message, length).show();
    }

    /**
     * This Function Is A Shortcut For Showing A Toast Message,Updating Result Text and Making a Sound
     */
    public void ShowToastMessage(String message, int length,boolean success){
       UpdateResult(message,success);
       ShowToastMessage(message,length);
    }

    public void ShowToastMessage(String message,String fullmessage, int length,boolean success){
        UpdateResult(message,fullmessage, success);
        ShowToastMessage(message,length);
    }

    /**
     * This Function Is A Shortcut For Showing A Snackbar Message
     */
    public void ShowSnackBar(View view, String message, int length){
        Snackbar.make(view, message, length)
                .setAction("No action", null).show();
    }


    /**
     * This Function Is A Shortcut For Showing A Snackbar Message, Updating Result Text and Making a Sound
     */

    public void ShowSnackBar(View view, String message, int length,boolean success){
        UpdateResult(message,success);
        ShowSnackBar(view,message,length);
    }

    public void ShowSnackBar(View view, String message,String fullMessage, int length,boolean success){
        UpdateResult(message,fullMessage,success);
        ShowSnackBar(view,message,length);
    }


    /**
     * This Function Is A Shortcut For Showing A Snackbar Message
     */
    public void ShowSnackBar(String message, int length){
        Snackbar.make(this.activityMainView, message, length)
                .setAction("No action", null).show();
    }


    /**
     * This Function Is A Shortcut For Showing A Snackbar Message
     */
    public void ShowSnackBar(String message, int length,boolean success){
        UpdateResult(message,success);
        ShowSnackBar(message,length);
    }

    public void ShowSnackBar(String message,String fullMessage, int length,boolean success){
        UpdateResult(message,fullMessage,success);
        ShowSnackBar(message,length);
    }





    /**
     * This Function Is A Shortcut For Showing A SnackBar Message with custom background
     */
    public void showSnackBarWithBackgroundColor(String message, int length,int color)
    {
        Snackbar.make(this.activityMainView, message, length)
                .setAction("No action", null).setBackgroundTintMode(PorterDuff.Mode.SRC_OVER).setBackgroundTint(color).show();
    }


    /**
     * This Function Is A Shortcut For Showing A SnackBar Message with custom background, Updating Result Text and Making a Sound
     */
    public void showSnackBarWithBackgroundColor(String message, int length, int color,boolean success)
    {
        UpdateResult(message,success);
        showSnackBarWithBackgroundColor(message,length,color);

    }

    /**
     * Creates a simple beat animation for a view
     * @param view
     * @param duration
     * @param listener
     */
    public void CreateViewBeatAnimation(View view, int duration, Animator.AnimatorListener listener){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ObjectAnimator animate = ObjectAnimator.ofPropertyValuesHolder(view,
                        PropertyValuesHolder.ofFloat("scaleX", 1.1f),
                        PropertyValuesHolder.ofFloat("scaleY", 1.1f));
                animate.setDuration(duration);
                animate.setRepeatCount(ObjectAnimator.RESTART);
                animate.setRepeatMode(ObjectAnimator.REVERSE);
                if(listener != null)
                    animate.addListener(listener);
                animate.start();
            }
        });
    }

    /**
     * Creates a simple flashing animation for a view
     * @param view
     * @param duration
     */
    public void CreateFlashingAnimation(View view, int duration){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AnimationDrawable animation = new AnimationDrawable();

                Handler handler = new Handler();

                animation.addFrame(new ColorDrawable(Color.GREEN), 100);
                animation.addFrame(new ColorDrawable(Color.WHITE), 100);
                animation.setOneShot(false);



                view.setBackgroundDrawable(animation);

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        animation.start();
                    }
                }, 100);

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        view.setBackgroundColor(Color.WHITE);
                    }
                }, duration);

            }
        });
    }

    /**
     * Creates a barcode scanner listener for the current activity, it will not auto focus
     * @param view
     * @param listener
     */
    public void CreateNonFocusBarcodeScanner(EditText view, BarcodeScannedListener listener){

        view.setInputType(InputType.TYPE_NULL);

        /**
         * This is used to always keep focus on the edit text so we can detect its text change
         * The text changes only when the user uses the scan method on the device and the text is pasted not typed
         */
        /*view.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                view.requestFocus();
            }
        });*/

        /**
         * This functions blocks the keyboard from poping up incase the uses presses on the edit text
         */
        view.setOnClickListener((click) -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        });


        /**
         * Check for when the text of the edit is changed and add the pasted text to the scanned barcodes
         */
        view.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                if(s.length() != 0 && count > 2){
                    view.removeTextChangedListener(this);
                    view.setText(" ");
                    view.addTextChangedListener(this);

                    if(listener != null)
                        listener.onBarcodeScanned(s.toString().replaceAll(" ", ""));

                }else if(s.length() != 0 && !s.toString().isEmpty()){;
                    view.removeTextChangedListener(this);
                    view.setText(" ");
                    view.addTextChangedListener(this);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        });

        view.requestFocus();
    }

    /**
     * Creates a barcode scanner listener for the current activity
     * @param view
     * @param listener
     */
    public void CreateBarcodeScanner(EditText view, BarcodeScannedListener listener){

        view.setInputType(InputType.TYPE_NULL);

        /**
         * This is used to always keep focus on the edit text so we can detect its text change
         * The text changes only when the user uses the scan method on the device and the text is pasted not typed
         */
        view.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                view.requestFocus();
            }
        });

        /**
         * This functions blocks the keyboard from poping up incase the uses presses on the edit text
         */
        view.setOnClickListener((click) -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        });


        /**
         * Check for when the text of the edit is changed and add the pasted text to the scanned barcodes
         */
        view.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                if(s.length() != 0 && count > 2){
                    view.removeTextChangedListener(this);
                    view.setText(" ");
                    view.addTextChangedListener(this);

                    if(listener != null)
                        listener.onBarcodeScanned(s.toString().replaceAll(" ", ""));

                }else if(s.length() != 0 && !s.toString().isEmpty()){;
                    view.removeTextChangedListener(this);
                    view.setText(" ");
                    view.addTextChangedListener(this);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        });

        view.requestFocus();
    }

    /**
     * Creates a barcode scanner listener for the current activity, Taking One Character At A Time Rather Than Paste
     * @param view
     * @param listener
     */
    public void CreateNonPasteBarcodeScanner(EditText view, BarcodeScannedListener listener){

        final CountDownTimer[] releaseBarcodeTimer = {null};

        view.setInputType(InputType.TYPE_NULL);

        /**
         * This is used to always keep focus on the edit text so we can detect its text change
         * The text changes only when the user uses the scan method on the device and the text is pasted not typed
         */
        view.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                view.requestFocus();
            }
        });

        /**
         * This functions blocks the keyboard from poping up incase the uses presses on the edit text
         */
        view.setOnClickListener((click) -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        });


        /**
         * Check for when the text of the edit is changed and add the pasted text to the scanned barcodes
         */
        view.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {

                if(releaseBarcodeTimer[0] != null){
                    releaseBarcodeTimer[0].cancel();
                    releaseBarcodeTimer[0] = null;
                }

                TextWatcher currentTextWatcher = this;
                String currentText = view.getText().toString();

                releaseBarcodeTimer[0] = new CountDownTimer(500, 500) {
                    @Override
                    public void onTick(long l) {

                    }

                    @Override
                    public void onFinish() {
                        if(currentText.length() != 0 && currentText.length() > 2){
                            view.removeTextChangedListener(currentTextWatcher);
                            view.setText(" ");
                            view.addTextChangedListener(currentTextWatcher);

                            if(listener != null)
                                listener.onBarcodeScanned(currentText.replaceAll(" ", ""));

                        }else if(s.length() != 0 && !s.toString().isEmpty()){;
                            view.removeTextChangedListener(currentTextWatcher);
                            view.setText(" ");
                            view.addTextChangedListener(currentTextWatcher);
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }
                    }
                }.start();
            }
        });

        view.requestFocus();
    }

    /**
     * Creates a barcode scanner listener for the current activity, Taking One Character At A Time Rather Than Paste
     * @param view
     * @param listener
     */
    public void CreateBothPasteBarcodeScanner(EditText view, BarcodeScannedListener listener){

        final CountDownTimer[] releaseBarcodeTimer = {null};

        view.setInputType(InputType.TYPE_NULL);

        /**
         * This is used to always keep focus on the edit text so we can detect its text change
         * The text changes only when the user uses the scan method on the device and the text is pasted not typed
         */
        view.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                view.requestFocus();
            }
        });

        /**
         * This functions blocks the keyboard from poping up incase the uses presses on the edit text
         */
        view.setOnClickListener((click) -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        });


        /**
         * Check for when the text of the edit is changed and add the pasted text to the scanned barcodes
         */
        view.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {

                if(releaseBarcodeTimer[0] != null){
                    releaseBarcodeTimer[0].cancel();
                    releaseBarcodeTimer[0] = null;
                }

                if(count == 1){
                    TextWatcher currentTextWatcher = this;
                    String currentText = view.getText().toString();

                    releaseBarcodeTimer[0] = new CountDownTimer(50, 50) {
                        @Override
                        public void onTick(long l) {

                        }

                        @Override
                        public void onFinish() {
                            if(currentText.length() != 0 && currentText.length() > 2){
                                view.removeTextChangedListener(currentTextWatcher);
                                view.setText(" ");
                                view.addTextChangedListener(currentTextWatcher);

                                if(listener != null)
                                    listener.onBarcodeScanned(currentText.replaceAll(" ", ""));

                            }else if(s.length() != 0 && !s.toString().isEmpty()){;
                                view.removeTextChangedListener(currentTextWatcher);
                                view.setText(" ");
                                view.addTextChangedListener(currentTextWatcher);
                                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                            }
                        }
                    }.start();
                }else if(s.length() != 0 && count > 2){
                    view.removeTextChangedListener(this);
                    view.setText(" ");
                    view.addTextChangedListener(this);

                    if(listener != null)
                        listener.onBarcodeScanned(s.toString().replaceAll(" ", ""));

                }else if(s.length() != 0 && !s.toString().isEmpty()){;
                    view.removeTextChangedListener(this);
                    view.setText(" ");
                    view.addTextChangedListener(this);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        });

        view.requestFocus();
    }

    /* Sounds Region */

    ToneGenerator toneGenerator = null;

    /**
     * Plays A Simple Success Sound On The Device
     */
    public void PlaySuccessSound(){
        PlayTone(ToneGenerator.TONE_SUP_PIP,5000);
    }

    /**
     * Plays A Simple Error Sound On The Device
     */
    public void PlayErrorSound(){
        PlayTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 2500);
    }

    public  void PlayTone(final int toneType, final int timout) {
        try {
            new Thread() {
                @Override
                public void run() {
                    try {
                        sleep(timout);
                        if (toneGenerator != null) {
                            toneGenerator.stopTone();
                        }
                    } catch (InterruptedException e) {
                    }
                }
            }.start();
            if (toneGenerator == null) {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 99);
            }
            toneGenerator.startTone(toneType);
        } catch (Exception e) {
        }
    }

    public void ShowAppMessage(boolean isError, String message)
    {

    }

    public void SetPopupMessage(String msg){
        this.popupMessage=msg==null?"":msg;
    }
    public String GetPopupMessage(){
        return popupMessage;
    }

    public void SetPopupTitle(String title){
        this.popupTitle=title==null?"":title;
    }
    public String GetPopupTitle(){
        return popupTitle;
    }

    public void SetPopup(String title,String msg){
        SetPopupTitle(title);
        SetPopupMessage(msg);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        ShowDetails();
     return true;
    }

    public void ShowDetails(){
        ShowAlertDialog(popupTitle==null?"Title":popupTitle,popupMessage==null?"":popupMessage);
    }


    public void SetResultState(Button btnResult,String msg,ResponseColor Color,String PopUpTitle, String PopUpMsg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch(Color){
                    case BLACK:
                        btnResult.setBackgroundColor(HelperColorBlack);
                        btnResult.setTextColor(HelperColorWhite);
                        break;
                    case GREEN:
                        btnResult.setBackgroundColor(HelperColorGreen);
                        break;
                    case ORANGE:
                        btnResult.setBackgroundColor(HelperColorOrange);
                        break;
                    case RED:
                        btnResult.setBackgroundColor(HelperColorRed);
                        break;
                }

                btnResult.setText(msg);
                SetPopup(PopUpTitle,PopUpMsg);
            }
        });

    }

    private void Beep() {
        new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_SUP_ERROR, 300);
    }

    protected void ResetResult(){
        SetPopup("Info","");
        if(lblResult !=null){
            lblResult.setText("");
        }
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

    /**
     * Shows the activity result button with changing colors depending on the state
     * @param title
     * @param state
     */
    public void ShowActivityResultButton(String title, ActivityActionsResultState state) {

        if(activityResultButton == null)
            return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (state){
                    case None:
                        activityResultButton.setVisibility(View.INVISIBLE);
                        break;
                    case Success:
                        activityResultButton.setText(title);
                        activityResultButton.setBackgroundColor(HelperColorGreen);
                        activityResultButton.setVisibility(View.VISIBLE);
                        CreateViewBeatAnimation(activityResultButton, 300, null);
                        break;
                    case Pending:
                        activityResultButton.setText(title);
                        activityResultButton.setBackgroundColor(HelperColorOrange);
                        activityResultButton.setVisibility(View.VISIBLE);
                        CreateViewBeatAnimation(activityResultButton, 300, null);
                        break;
                    case PendingStageTwo:
                        activityResultButton.setText(title);
                        activityResultButton.setBackgroundColor(HelperColorBlack);
                        activityResultButton.setVisibility(View.VISIBLE);
                        CreateViewBeatAnimation(activityResultButton, 300, null);
                        break;
                    case Error:
                        activityResultButton.setText(title);
                        activityResultButton.setBackgroundColor(HelperColorRed);
                        activityResultButton.setVisibility(View.VISIBLE);
                        CreateViewBeatAnimation(activityResultButton, 300, null);
                        break;
                    default:
                        activityResultButton.setVisibility(View.INVISIBLE);
                        break;
                }
            }
        });
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch(Exception e){
            return false;
        }
    }


    public static String round(double n){
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        Double d = n;
        return df.format(d);

    }

}
