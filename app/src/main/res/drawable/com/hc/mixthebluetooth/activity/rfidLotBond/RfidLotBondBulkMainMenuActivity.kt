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
import com.hc.mixthebluetooth.storage.Storage

class RfidLotBondBulkMainMenuActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        mStorage = Storage(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.rfid_lot_bond_bulk_activity_main_menu)


        val btnRFIDLotBonding: Button = findViewById(R.id.btnRfidLotBond);
        val btnRFIDLotBondingEnhanced: Button = findViewById(R.id.btnRfidLotBondEnhanced)
        val btnRfidLotBondGenerateItemSerial: Button = findViewById(R.id.btnRfidLotBondGenerateItemSerial)
        val btnRfidLotBondEnhancedMacys: Button = findViewById(R.id.btnRfidLotBondEnhancedMacys)



        if(UserPermissions.PermissionsReceived()){

            UserPermissions.ValidatePermission("RFIDApp.RFIDLotBondBulk", btnRFIDLotBonding)
            UserPermissions.ValidatePermission("RFIDApp.RFIDLotBondBulkEnhanced", btnRFIDLotBondingEnhanced)
            UserPermissions.ValidatePermission("RFIDApp.RFIDLotBondBulkGenerateItemSerial", btnRfidLotBondGenerateItemSerial)
            UserPermissions.ValidatePermission("RFIDApp.MacysRFIDLotBondBulkEnhanced", btnRfidLotBondEnhancedMacys)
        }else {
            UserPermissions.AddOnReceiveListener {
                UserPermissions.ValidatePermission("RFIDApp.RFIDLotBondBulk", btnRFIDLotBonding)
                UserPermissions.ValidatePermission("RFIDApp.RFIDLotBondBulkEnhanced", btnRFIDLotBondingEnhanced)
                UserPermissions.ValidatePermission("RFIDApp.RFIDLotBondBulkGenerateItemSerial", btnRfidLotBondGenerateItemSerial)
                UserPermissions.ValidatePermission("RFIDApp.MacysRFIDLotBondBulkEnhanced", btnRfidLotBondEnhancedMacys)
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
            startActivity(Intent(this, RfidLotBondBulkActivity::class.java))
        }
        btnRFIDLotBondingEnhanced.setOnClickListener {
            startActivity(Intent(this, RfidLotBondBulkEnhancedActivity::class.java))
        }
        btnRfidLotBondGenerateItemSerial.setOnClickListener {
            startActivity(Intent(this, RfidLotBondBulkGenerateItemSerialActivity::class.java))
        }
        btnRfidLotBondEnhancedMacys.setOnClickListener {
            startActivity(Intent(this, MacysRfidLotBondBulkEnhancedActivity::class.java))
        }



    }

    lateinit var mStorage :Storage

}