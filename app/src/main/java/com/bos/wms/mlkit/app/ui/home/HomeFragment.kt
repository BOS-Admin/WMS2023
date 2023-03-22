package com.bos.wms.mlkit.app.ui.home

import Model.UserLoginModel
import Remote.APIClient
import Remote.BasicApi
import Remote.UserPermissions.UserPermissions
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.AlarmClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.R
import com.bos.wms.mlkit.app.*
import com.bos.wms.mlkit.databinding.FragmentHomeBinding
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.content_locationcheck.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import java.io.IOException

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        if(UserPermissions.PermissionsReceived()){
            UserPermissions.ValidatePermission("WMSApp.UPCPricing", root.btnUPCPricing);
            UserPermissions.ValidatePermission("WMSApp.PGPricing", root.btnMenuPGPricing);
            UserPermissions.ValidatePermission("WMSApp.PutAwayPallete", root.btnMenuPutAwayPallete);
            UserPermissions.ValidatePermission("WMSApp.MissingItem", root.btnSerialMissing);
            UserPermissions.ValidatePermission("WMSApp.BolRecognition", root.btnBolRecognition);
            UserPermissions.ValidatePermission("WMSApp.LocationCheck", root.btnLocationCheck);
            UserPermissions.ValidatePermission("WMSApp.FillPallete", root.btnMenuFillPallete);
            UserPermissions.ValidatePermission("WMSApp.SerialGenerator", root.btnSerialGenerator);
            UserPermissions.ValidatePermission("WMSApp.ShipmentPalleteReceiving", root.btnMenuShipmentPalleteReceiving);
            UserPermissions.ValidatePermission("WMSApp.ShipmentCartonReceiving", root.btnMenuShipmentCartonReceiving);
            UserPermissions.ValidatePermission("WMSApp.ShipmentPalleteCount", root.btnMenuShipmentReceivingPalleteCount);
            UserPermissions.ValidatePermission("WMSApp.CartonReceivingV2", root.btnMenuShipmentCartonReceivingV2);
            UserPermissions.ValidatePermission("WMSApp.ItemPricing", root.btnMenuItemPricing);
            UserPermissions.ValidatePermission("WMSApp.PickingOrder", root.btnMenuPicking);
            UserPermissions.ValidatePermission("WMSApp.EmptyBox", root.btnEmptyBox);

        }else {
            UserPermissions.AddOnReceiveListener {
                UserPermissions.ValidatePermission("WMSApp.UPCPricing", root.btnUPCPricing);
                UserPermissions.ValidatePermission("WMSApp.PGPricing", root.btnMenuPGPricing);
                UserPermissions.ValidatePermission("WMSApp.PutAwayPallete", root.btnMenuPutAwayPallete);
                UserPermissions.ValidatePermission("WMSApp.MissingItem", root.btnSerialMissing);
                UserPermissions.ValidatePermission("WMSApp.BolRecognition", root.btnBolRecognition);
                UserPermissions.ValidatePermission("WMSApp.LocationCheck", root.btnLocationCheck);
                UserPermissions.ValidatePermission("WMSApp.FillPallete", root.btnMenuFillPallete);
                UserPermissions.ValidatePermission("WMSApp.SerialGenerator", root.btnSerialGenerator);
                UserPermissions.ValidatePermission("WMSApp.ShipmentPalleteReceiving", root.btnMenuShipmentPalleteReceiving);
                UserPermissions.ValidatePermission("WMSApp.ShipmentCartonReceiving", root.btnMenuShipmentCartonReceiving);
                UserPermissions.ValidatePermission("WMSApp.ShipmentPalleteCount", root.btnMenuShipmentReceivingPalleteCount);
                UserPermissions.ValidatePermission("WMSApp.CartonReceivingV2", root.btnMenuShipmentCartonReceivingV2);
                UserPermissions.ValidatePermission("WMSApp.ItemPricing", root.btnMenuItemPricing);
                UserPermissions.ValidatePermission("WMSApp.PickingOrder", root.btnMenuPicking);
                UserPermissions.ValidatePermission("WMSApp.EmptyBox", root.btnEmptyBox);
            }
        }

        if(UserPermissions.GotPermissionError){
            AlertDialog.Builder(root.context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("An Error Occurred")
                .setMessage(UserPermissions.GetLatestError())
                .setNegativeButton("Close", null)
                .show()
        }else {
            UserPermissions.AddOnErrorListener {
                AlertDialog.Builder(root.context)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("An Error Occurred")
                    .setMessage(it)
                    .setNegativeButton("Close", null)
                    .show()
            }
        }

        root.btnMenuPicking.setOnClickListener {
            ProceedPickingClick()
        }

        root.btnBolRecognition.setOnClickListener {
            ProceedBolRecognition()
        }
        root.btnLocationCheck.setOnClickListener {
            ProceedLocationCheckClick()
        }
        root.btnMenuFillPallete.setOnClickListener {
            ProceedFillPallete()
        }
        root.btnMenuPutAwayPallete.setOnClickListener {
            ProceedPutAwayPallete()
        }
        root.btnSerialGenerator.setOnClickListener {
            ProceedFoldingScan()
        }
        root.btnUPCPricing.setOnClickListener {
            ProceedUPCPricing()
        }
        root.btnMenuShipmentPalleteReceiving.setOnClickListener {
            ProceedShipmentPalleteReceiving()
        }
        root.btnMenuShipmentCartonReceiving.setOnClickListener {
            ProceedShipmentCartonReceiving()
        }
        root.btnMenuShipmentReceivingPalleteCount.setOnClickListener {
            ProceedShipmentReceivingPalleteCount()
        }
        root.btnMenuItemPricing.setOnClickListener {
            ProceedItemPricingActivity()
        }
        root.btnMenuPGPricing.setOnClickListener {
            ProceedPGPricingActivity()
        }
        root.btnSerialMissing.setOnClickListener {
            ProceedMissingItem()
        }

        root.btnMenuShipmentCartonReceivingV2.setOnClickListener {
            ProceedShipmentCartonReceivingV2()
        }

        root.btnEmptyBox.setOnClickListener {
            ProceedEmptyBox()
        }



        mStorage= Storage(context?.applicationContext) //sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")

        return root
    }
    lateinit var api: BasicApi
    var compositeDisposable= CompositeDisposable()
    lateinit var  mStorage:Storage ;  //sp存储
    var IPAddress =""
      fun ProceedPickingClick() {

        try {
            // TODO: handle loggedInUser authentication


            api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.AssignPickingList(General.getGeneral(context).UserID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                        if(s){
                            txtStatus.setText("")
                            val intent = Intent (getActivity(), PickingActivity::class.java)
                            startActivity(intent)
                        }
                        },
                        {t:Throwable?->
                            run {
                                txtStatus.setText(t!!.message)
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            throw(IOException("ProceedPickingClick", e))
        }
        finally {
        }
    }

    fun ProceedLocationCheckClick() {

        try {
            // TODO: handle loggedInUser authentication

            api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            var FloorID: Int=General.getGeneral(context?.applicationContext).FloorID
            compositeDisposable.addAll(
                api.AssignLocationCheck(General.getGeneral(context).UserID,FloorID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            var ErrorMsg = ""
                            try {
                                ErrorMsg = s.string()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                            if (ErrorMsg.isEmpty()) {
                                val intent = Intent (getActivity(), LocationCheckActivity::class.java)
                                startActivity(intent)
                                txtStatus.setText("")
                            } else if (!ErrorMsg.isEmpty()) {
                                txtStatus.setTextColor(Color.RED)
                                txtStatus.setText(ErrorMsg)
                                //(ErrorMsg)
                            }


                        },
                        {t:Throwable?->
                            run {
                                txtStatus.setTextColor(Color.RED)
                                txtStatus.setText(t!!.message.toString())
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            throw(IOException("ProceedLocationCheckClick", e))
        }
        finally {
        }
    }

    fun ProceedEmptyBox() {
        val intent = Intent (getActivity(), EmptyBoxActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }

    fun ProceedBolRecognition() {
        val intent = Intent (getActivity(), BolRecognitionActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }

    fun ProceedNextReceivingStatus() {
        val intent = Intent (getActivity(), NextReceivingStatusActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }

    fun ProceedUPCPricing() {
        val intent = Intent (getActivity(), UPCPricingActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }
    fun ProceedFoldingScan() {
        val intent = Intent (getActivity(), SerialGeneratorActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }
    fun ProceedShipmentPalleteReceiving() {
        val intent = Intent (getActivity(), ShipmentPalleteReceivingActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }
    fun ProceedShipmentCartonReceiving() {
        val intent = Intent (getActivity(), ShipmentCartonReceivingActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }

    fun ProceedShipmentCartonReceivingV2() {
        val intent = Intent (getActivity(), ShipmentCartonReceivingV2Activity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }
    fun ProceedShipmentReceivingPalleteCount() {
        val intent = Intent (getActivity(), ShipmentCartonCountReceivingActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }

    fun ProceedItemPricingActivity() {
        val intent = Intent (getActivity(), ItemPricingActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }
    fun ProceedPGPricingActivity() {
        val intent = Intent (getActivity(), PGPricingActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }

    fun ProceedMissingItem() {
        val intent = Intent (getActivity(), ItemSerialUPCMissingActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }
    fun ProceedPutAwayPallete() {
        val intent = Intent (getActivity(), PutAwayPalleteActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }

    fun ProceedFillPallete() {
        val intent = Intent (getActivity(), FillPalleteActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}