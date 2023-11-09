package com.hc.mixthebluetooth.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.annotation.RequiresApi
import com.hc.mixthebluetooth.R
import com.hc.mixthebluetooth.Remote.UserPermissions.UserPermissions
import com.hc.mixthebluetooth.activity.rfidLotBond.*
import com.hc.mixthebluetooth.storage.Storage

class MainMenuActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        mStorage = Storage(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val btnLabelPrintFromRFID: Button = findViewById(R.id.btnLabelPrintFromRFID);
        val btnFillBin: Button = findViewById(R.id.btnFillBin);
        val btnCycleCount: Button = findViewById(R.id.btnCycleCount);
        val btnRFIDBond: Button = findViewById(R.id.btnRFIDBond);
        val btnRFIDFillPallete: Button = findViewById(R.id.btnRFIDFillPallete);
        val btnAutoRFIDLotBonding: Button = findViewById(R.id.btnAutoRFIDLotBonding);
        val btnManualDWS: Button = findViewById(R.id.btnManualDWS);
        val btnRfidBulk: Button = findViewById(R.id.btnRfidBulk);
        val btnNonBBBAudit: Button = findViewById(R.id.btnNonBBBAudit);
        val btnAuditAntenna: Button = findViewById(R.id.btnAuditAntenna);
        val btnReplenishmentAuditAntenna: Button = findViewById(R.id.btnReplenishmentAuditAntenna);
        val btnRemoveAuditUPCS: Button = findViewById(R.id.btnRemoveAuditUPCS);
        val btnRepairTransfer: Button = findViewById(R.id.btnRepairTransfer);
        val btnRepairCategorize: Button = findViewById(R.id.btnRepairCategorize);
        val btnRfidBulkTypeUpdate: Button = findViewById(R.id.btnRfidBulkTypeUpdate);
        val btnNonBBBAuditMacys : Button = findViewById(R.id.btnNonBBBAuditMacys);
        val btnAuditAntennaMacys:Button = findViewById(R.id.btnAuditAntennaMacys);
        val btnItemSerialLotBond: Button = findViewById(R.id.btnItemSerialLotBond);
        val btnItemSerialLotBondBulk: Button = findViewById(R.id.btnItemSerialLotBondBulk);
        val btnRFIDPacking : Button = findViewById(R.id.btnRFIDPacking);

        if(UserPermissions.PermissionsReceived()){
            UserPermissions.ValidatePermission("RFIDApp.LabelPrintFromRFID", btnLabelPrintFromRFID)
            UserPermissions.ValidatePermission("RFIDApp.FillBin", btnFillBin);
            UserPermissions.ValidatePermission("RFIDApp.FillBin", btnFillBin);
            UserPermissions.ValidatePermission("RFIDApp.CycleCount", btnCycleCount);
            UserPermissions.ValidatePermission("RFIDApp.RFID-UPCBond", btnRFIDBond);
            UserPermissions.ValidatePermission("RFIDApp.FillPallete", btnRFIDFillPallete);
            UserPermissions.ValidatePermission("RFIDApp.RfidLotBondMainMenu", btnAutoRFIDLotBonding);
            UserPermissions.ValidatePermission("RFIDApp.ItemSerialLotBond", btnItemSerialLotBond);
            UserPermissions.ValidatePermission("RFIDApp.ItemSerialLotBondBulk", btnItemSerialLotBondBulk);
            UserPermissions.ValidatePermission("RFIDApp.ManualDWS", btnManualDWS);
            UserPermissions.ValidatePermission("RFIDApp.RFIDAudit", btnNonBBBAudit);
            UserPermissions.ValidatePermission("RFIDApp.ReplenishmentRFIDAudit", btnReplenishmentAuditAntenna);
            UserPermissions.ValidatePermission("RFIDApp.AuditRemoveUPCs", btnRemoveAuditUPCS);
            UserPermissions.ValidatePermission("RFIDApp.AuditAntenna", btnAuditAntenna);
            UserPermissions.ValidatePermission("RFIDApp.RfidLotBondBulkMainMenu", btnRfidBulk);
            UserPermissions.ValidatePermission("RFIDApp.RepairTransfer", btnRepairTransfer);
            UserPermissions.ValidatePermission("RFIDApp.RepairCategorize", btnRepairCategorize);
            UserPermissions.ValidatePermission("RFIDApp.RfidBulkTypeUpdate", btnRfidBulkTypeUpdate);
            UserPermissions.ValidatePermission("RFIDApp.MacysRFIDAudit", btnNonBBBAuditMacys);
            UserPermissions.ValidatePermission("RFIDApp.MacysAuditAntenna", btnAuditAntennaMacys);
            UserPermissions.ValidatePermission("RFIDApp.RFIDPacking", btnRFIDPacking);
        }else {
            UserPermissions.AddOnReceiveListener {
                UserPermissions.ValidatePermission("RFIDApp.LabelPrintFromRFID", btnLabelPrintFromRFID);
                UserPermissions.ValidatePermission("RFIDApp.FillBin", btnFillBin);
                UserPermissions.ValidatePermission("RFIDApp.CycleCount", btnCycleCount);
                UserPermissions.ValidatePermission("RFIDApp.RFID-UPCBond", btnRFIDBond);
                UserPermissions.ValidatePermission("RFIDApp.FillPallete", btnRFIDFillPallete);
                UserPermissions.ValidatePermission("RFIDApp.RfidLotBondMainMenu", btnAutoRFIDLotBonding);
                UserPermissions.ValidatePermission("RFIDApp.ItemSerialLotBond", btnItemSerialLotBond);
                UserPermissions.ValidatePermission("RFIDApp.ItemSerialLotBondBulk", btnItemSerialLotBondBulk);
                UserPermissions.ValidatePermission("RFIDApp.ManualDWS", btnManualDWS);
                UserPermissions.ValidatePermission("RFIDApp.RFIDAudit", btnNonBBBAudit);
                UserPermissions.ValidatePermission("RFIDApp.ReplenishmentRFIDAudit", btnReplenishmentAuditAntenna);
                UserPermissions.ValidatePermission("RFIDApp.AuditRemoveUPCs", btnRemoveAuditUPCS);
                UserPermissions.ValidatePermission("RFIDApp.AuditAntenna", btnAuditAntenna);
                UserPermissions.ValidatePermission("RFIDApp.RfidLotBondBulkMainMenu", btnRfidBulk);
                UserPermissions.ValidatePermission("RFIDApp.RepairTransfer", btnRepairTransfer);
                UserPermissions.ValidatePermission("RFIDApp.RepairCategorize", btnRepairCategorize);
                UserPermissions.ValidatePermission("RFIDApp.RfidBulkTypeUpdate", btnRfidBulkTypeUpdate);
                UserPermissions.ValidatePermission("RFIDApp.MacysRFIDAudit", btnNonBBBAuditMacys);
                UserPermissions.ValidatePermission("RFIDApp.MacysAuditAntenna", btnAuditAntennaMacys);
                UserPermissions.ValidatePermission("RFIDApp.RFIDPacking", btnRFIDPacking);
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

        btnItemSerialLotBond.setOnClickListener {
            startActivity(Intent(this, ItemSerialLotBondEnhancedActivity::class.java))
        }

        btnItemSerialLotBondBulk.setOnClickListener {
            startActivity(Intent(this, ItemSerialLotBondBulkEnhancedActivity::class.java))
        }


        btnLabelPrintFromRFID.setOnClickListener {
            startActivity(Intent(this, LabelPrintFromRFID::class.java))
        }

        btnRepairTransfer.setOnClickListener {
            startActivity(Intent(this, TransferToRepairActivity::class.java))
        }

        btnFillBin.setOnClickListener {
            startActivity(Intent(this, FillBinActivity::class.java))
        }

        btnCycleCount.setOnClickListener {
            startActivity(Intent(this, CycleCountActivity::class.java))
        }

        btnRFIDBond.setOnClickListener {
            startActivity(Intent(this, RFIDUPCMATCH::class.java))
        }

        btnRFIDFillPallete.setOnClickListener {
            startActivity(Intent(this, FillPalleteActivity::class.java))
        }

        /*btnRFIDLotBond.setOnClickListener {
            startActivity(Intent(this, ScanRFIDActivity::class.java))
        }*/

        btnAutoRFIDLotBonding.setOnClickListener {
            startActivity(Intent(this, RfidLotBondMainMenuActivity::class.java))
        }

        btnManualDWS.setOnClickListener {
            startActivity(Intent(this, ManualDWSActivity::class.java))
        }

        btnRfidBulk.setOnClickListener {
            startActivity(Intent(this, RfidLotBondBulkMainMenuActivity::class.java))
        }

        btnNonBBBAudit.setOnClickListener {
            startActivity(Intent(this, BBBAuditActivity::class.java))
        }

        btnNonBBBAuditMacys.setOnClickListener {
            var intent: Intent = Intent(this, BBBAuditActivity::class.java)
            intent.putExtra("DiscardItemSerial", true)
            startActivity(intent)
        }

        btnAuditAntenna.setOnClickListener {
            mStorage.saveData("AntennaConnectionNextStep", "AuditAntenna")
            BBBAuditAllAntennasActivity.isReplenishment=false;
            DeviceActivity.isReplenishment=false;
            startActivity(Intent(this, DeviceActivity::class.java))
        }
        btnReplenishmentAuditAntenna.setOnClickListener {
            mStorage.saveData("AntennaConnectionNextStep", "AuditAntenna")
            BBBAuditAllAntennasActivity.isReplenishment=true;
            DeviceActivity.isReplenishment=true;
            startActivity(Intent(this, DeviceActivity::class.java))
        }
        btnRemoveAuditUPCS.setOnClickListener {

            AuditRemoveRemoveUPCActivity.BoxBarcode=null;
            startActivity(Intent(this, AuditRemoveRemoveUPCActivity::class.java))
        }

        btnAuditAntennaMacys.setOnClickListener {
            mStorage.saveData("AntennaConnectionNextStep", "AuditAntenna")
            var intent: Intent = Intent(this, DeviceActivity::class.java)
            intent.putExtra("DiscardItemSerial", true)
            startActivity(intent)
        }

        btnRFIDPacking.setOnClickListener {
            mStorage.saveData("AntennaConnectionNextStep", "PackingAntenna")
            var intent: Intent = Intent(this, DeviceActivity::class.java)
            startActivity(intent)
        }

        btnRepairCategorize.setOnClickListener {
            startActivity(Intent(this, RepairCategorizationActivity::class.java))
        }
        btnRfidBulkTypeUpdate.setOnClickListener {
            mStorage.saveData("AntennaConnectionNextStep", "RfidBulkUpdate")
            startActivity(Intent(this, DeviceActivity::class.java))
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

    lateinit var mStorage :Storage

}