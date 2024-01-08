package com.hc.mixthebluetooth;

import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;
import com.hc.mixthebluetooth.activity.single.HoldBluetooth;
import com.util.General;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.TextView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

public class WeightResultActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_result);

        Bundle b = getIntent().getExtras();
        String weightStr = ""; // or other values
        if(b != null)
            weightStr = b.getString("weightStr");
        TextView txtWeight=(findViewById(R.id.txtWeight));
        txtWeight.setText(weightStr);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_weight_result);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                General.getGeneral(view.getContext()).InRFID=false;
                HoldBluetooth mHoldBluetooth = HoldBluetooth.getInstance();
                mHoldBluetooth.connect(General.getGeneral(view.getContext()).Device);
                finish();
//                Intent intent = new Intent(WeightResultActivity.this, FillBinActivity.class);
//                finish();
//                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_weight_result);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}