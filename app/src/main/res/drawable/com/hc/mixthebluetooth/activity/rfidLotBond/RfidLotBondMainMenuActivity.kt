package com.hc.mixthebluetooth.activity.rfidLotBond

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.annotation.RequiresApi
import com.hc.mixthebluetooth.R
import com.hc.mixthebluetooth.Remote.UserPermissions.UserPermissions
import com.hc.mixthebluetooth.activity.ScanAutoFRIDActivity
import com.hc.mixthebluetooth.storage.Storage

class RfidLotBondMainMenuActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        mStorage = Storage(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.rfid_lot_bond_activity_main_menu)


        val btnRFIDLotBonding: Button = findViewById(R.id.btnRfidLotBond);
        val btnRFIDLotBondingEnhanced: Button = findViewById(R.id.btnRfidLotBondEnhanced);
        val btnRfidLotBondAutoItemSerial: Button = findViewById(R.id.btnRfidLotBondAutoItemSerial);
        val btnRfidLotBondEnhancedMacys: Button = findViewById(R.id.btnRfidLotBondEnhancedMacys);


        if(UserPermissions.PermissionsReceived()){

            UserPermissions.ValidatePermission("RFIDApp.RFIDLotBond", btnRFIDLotBonding);
            UserPermissions.ValidatePermission("RFIDApp.RFIDLotBondEnhanced", btnRFIDLotBondingEnhanced);
            UserPermissions.ValidatePermission("RFIDApp.RfIDLotBondGenerateItemSerial", btnRfidLotBondAutoItemSerial);
            UserPermissions.ValidatePermission("RFIDApp.MacysRFIDLotBondEnhanced", btnRfidLotBondEnhancedMacys);

        }else {
            UserPermissions.AddOnReceiveListener {
                UserPermissions.ValidatePermission("RFIDApp.RFIDLotBond", btnRFIDLotBonding);
                UserPermissions.ValidatePermission("RFIDApp.RFIDLotBondEnhanced", btnRFIDLotBondingEnhanced);
                UserPermissions.ValidatePermission("RFIDApp.RfIDLotBondGenerateItemSerial", btnRfidLotBondAutoItemSerial);
                UserPermissions.ValidatePermission("RFIDApp.MacysRFIDLotBondEnhanced", btnRfidLotBondEnhancedMacys);
            }
        }

        UserPermissions.AddOnErrorListener {
            AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("An Error Occurred")
                .setMessage(it)
                .setNegativeButton("Close", null)
                .show()
        }




        btnRFIDLotBonding.setOnClickListener {
            startActivity(Intent(this, ScanAutoFRIDActivity::class.java))
        }
        btnRFIDLotBondingEnhanced.setOnClickListener {
            startActivity(Intent(this, RfidLotBondEnhancedActivity::class.java))
        }
        btnRfidLotBondAutoItemSerial.setOnClickListener {
            startActivity(Intent(this, RfidLotBondItemSerialGeneratedActivity::class.java))
        }
        btnRfidLotBondEnhancedMacys.setOnClickListener {
            startActivity(Intent(this, MacysRfidLotBondEnhancedActivity::class.java))
        }




    }

    lateinit var mStorage :Storage

}