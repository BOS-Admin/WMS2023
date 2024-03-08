package com.bos.wms.mlkit.app.ui.home

import Remote.APIClient
import Remote.BasicApi
import Remote.UserPermissions.UserPermissions
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.app.*
import com.bos.wms.mlkit.app.tpo.TPOMainActivity
import com.bos.wms.mlkit.databinding.FragmentHomeBinding
import com.bos.wms.mlkit.storage.Storage
import com.google.android.material.snackbar.Snackbar
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
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
            UserPermissions.ValidatePermission("WMSApp.PutAway", root.btnPutAway);
            UserPermissions.ValidatePermission("WMSApp.BrandsInToIs", root.btnBrandsInToIs);
            UserPermissions.ValidatePermission("WMSApp.RepriceClassB", root.btnRepriceClassB);
            UserPermissions.ValidatePermission("WMSApp.UPCPricing", root.btnUPCPricing);
            UserPermissions.ValidatePermission("WMSApp.UPCPricingOverride", root.btnUPCPricingOverride);
            UserPermissions.ValidatePermission("WMSApp.UPCRecognitionPricing", root.btnUPCRecognitionPricing);
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
            UserPermissions.ValidatePermission("WMSApp.BrandOCR", root.btnBrandOCR);
            UserPermissions.ValidatePermission("WMSApp.PASBrandOCR", root.btnPASBrandOCR);
            UserPermissions.ValidatePermission("WMSApp.StoreRepriceCount", root.btnStoreRepriceCount);
            UserPermissions.ValidatePermission("WMSApp.Ping", root.btnPing);
            UserPermissions.ValidatePermission("WMSApp.PalletBinAssociation", root.btnPalletBinAssociation);
            UserPermissions.ValidatePermission("WMSApp.PalletRackAssignment", root.btnPalletRackAssignment);
            UserPermissions.ValidatePermission("WMSApp.PalletRackUnAssign", root.btnPalletRackUnAssign);
            /* Transfer Preparation Order */
            UserPermissions.ValidatePermission("WMSApp.TPO.MainMenu", root.btnTPOMainActivity);

        }else {
            UserPermissions.AddOnReceiveListener {

                UserPermissions.ValidatePermission("WMSApp.PutAway", root.btnPutAway);
                UserPermissions.ValidatePermission("WMSApp.BrandsInToIs", root.btnBrandsInToIs);
                UserPermissions.ValidatePermission("WMSApp.RepriceClassB", root.btnRepriceClassB);
                UserPermissions.ValidatePermission("WMSApp.UPCPricing", root.btnUPCPricing);
                UserPermissions.ValidatePermission("WMSApp.UPCPricingOverride", root.btnUPCPricingOverride);
                UserPermissions.ValidatePermission("WMSApp.UPCRecognitionPricing", root.btnUPCRecognitionPricing);
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
                UserPermissions.ValidatePermission("WMSApp.BrandOCR", root.btnBrandOCR);
                UserPermissions.ValidatePermission("WMSApp.PASBrandOCR", root.btnPASBrandOCR);
                UserPermissions.ValidatePermission("WMSApp.StoreRepriceCount", root.btnStoreRepriceCount);
                UserPermissions.ValidatePermission("WMSApp.Ping", root.btnPing);
                UserPermissions.ValidatePermission("WMSApp.PalletBinAssociation", root.btnPalletBinAssociation);
                UserPermissions.ValidatePermission("WMSApp.PalletRackAssignment", root.btnPalletRackAssignment);
                UserPermissions.ValidatePermission("WMSApp.PalletRackUnAssign", root.btnPalletRackUnAssign);

                /* Transfer Preparation Order */
                UserPermissions.ValidatePermission("WMSApp.TPO.MainMenu", root.btnTPOMainActivity);
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

        root.btnBrandsInToIs.setOnClickListener {
            ProceedBrandsInToIsMainActivity()
        }
        root.btnPutAway.setOnClickListener {
            ProceedPutAway()
        }
        root.btnPing.setOnClickListener {
            val intent = Intent (getActivity(), PingActivity::class.java)
            startActivity(intent)
        }

        root.btnPalletBinAssociation.setOnClickListener {
            val intent = Intent (getActivity(), PalletBinAssociationActivity::class.java)
            startActivity(intent)
        }

        root.btnPalletRackAssignment.setOnClickListener {
            val intent = Intent (getActivity(), PalletRackAssignmentActivity::class.java)
            startActivity(intent)
        }

        root.btnPalletRackUnAssign.setOnClickListener {
            val intent = Intent (getActivity(), PalletRackUnAssignActivity::class.java)
            startActivity(intent)
        }



        root.btnTPOMainActivity.setOnClickListener {
            ProceedTPOMainActivity()
        }
        root.btnStoreRepriceCount.setOnClickListener {
            ProceedStoreRepriceCountActivity()
        }
        root.btnRepriceClassB.setOnClickListener {
            ProceedClassBReprice()
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

        root.btnUPCPricingOverride.setOnClickListener {
            ProceedUPCPricingOverride()
        }

        root.btnUPCRecognitionPricing.setOnClickListener {
            ProceedUPCRecognitionPricing()
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

        root.btnBrandOCR.setOnClickListener {
            ProceedBrandOCR()
        }

        root.btnPASBrandOCR.setOnClickListener {
            ProceedPASBrandOCR()
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
                            val intent = Intent (getActivity(), PickingActivity::class.java)
                            startActivity(intent)
                        }
                        },
                        {t:Throwable?->
                            run {

                                Snackbar.make(
                                    binding.root.homeFragmentLayout,
                                    t!!.message.toString(),
                                    Snackbar.LENGTH_LONG
                                )
                                    .setAction("No action", null).show()
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
                            } else if (!ErrorMsg.isEmpty()) {
                                run {
                                    Snackbar.make(
                                        binding.root.homeFragmentLayout,
                                        ErrorMsg,
                                        Snackbar.LENGTH_LONG
                                    )
                                        .setAction("No action", null).show()
                                }
                            }


                        },
                        {t:Throwable?->
                            run {
                                Snackbar.make(
                                    binding.root.homeFragmentLayout,
                                    t!!.message.toString(),
                                    Snackbar.LENGTH_LONG
                                )
                                    .setAction("No action", null).show()
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
    }

    fun ProceedTPOMainActivity() {
        val intent = Intent (getActivity(), TPOMainActivity::class.java)
        startActivity(intent)
    }   fun ProceedBrandsInToIsMainActivity() {
        val intent = Intent (getActivity(), BrandsInToIsActivity::class.java)
        startActivity(intent)
    }
    fun ProceedPutAway() {
        val intent = Intent (getActivity(), PutAwayActivity::class.java)
        startActivity(intent)
    }
    fun ProceedPrintBPrices() {
        val intent = Intent (getActivity(), PrintBPricesActivity::class.java)
        startActivity(intent)
    }
    fun ProceedStoreRepriceCountActivity() {
        val intent = Intent (getActivity(), StoreRepriceCountActivity::class.java)
        startActivity(intent)
    }
  fun ProceedClassBReprice() {
        val intent = Intent (getActivity(), ClassBBulkRepriceActivity::class.java)
        startActivity(intent)
    }

    fun ProceedBolRecognition() {
        val intent = Intent (getActivity(), BolRecognitionActivity::class.java)
        startActivity(intent)
    }

    fun ProceedBrandOCR() {
        val intent = Intent (getActivity(), BrandOCRActivity::class.java)
        startActivity(intent)
    }

    fun ProceedPASBrandOCR() {
        val intent = Intent (getActivity(), PasBrandOCRActivity::class.java)
        startActivity(intent)
    }

    fun ProceedNextReceivingStatus() {
        val intent = Intent (getActivity(), NextReceivingStatusActivity::class.java)
        startActivity(intent)
    }

    fun ProceedUPCPricing() {
        val intent = Intent (getActivity(), UPCPricingActivity::class.java)
        startActivity(intent)
    }

    fun ProceedUPCRecognitionPricing() {
        val intent = Intent (getActivity(), UPCRecognitionPricingActivity::class.java)
        startActivity(intent)
    }

    fun ProceedUPCPricingOverride() {
        val intent = Intent (getActivity(), UPCPricingOverrideActivity::class.java)
        startActivity(intent)
    }

    fun ProceedFoldingScan() {
        val intent = Intent (getActivity(), SerialGeneratorActivity::class.java)
        startActivity(intent)
    }
    fun ProceedShipmentPalleteReceiving() {
        val intent = Intent (getActivity(), ShipmentPalleteReceivingActivity::class.java)
        startActivity(intent)
    }
    fun ProceedShipmentCartonReceiving() {
        val intent = Intent (getActivity(), ShipmentCartonReceivingActivity::class.java)
        startActivity(intent)
    }

    fun ProceedShipmentCartonReceivingV2() {
        val intent = Intent (getActivity(), ShipmentCartonReceivingV2Activity::class.java)
        startActivity(intent)
    }
    fun ProceedShipmentReceivingPalleteCount() {
        val intent = Intent (getActivity(), ShipmentCartonCountReceivingActivity::class.java)
        startActivity(intent)
    }

    fun ProceedItemPricingActivity() {
        val intent = Intent (getActivity(), NewItemPricingActivity::class.java)
        startActivity(intent)
    }
    fun ProceedPGPricingActivity() {
        val intent = Intent (getActivity(), PGPricingActivity::class.java)
        startActivity(intent)
    }

    fun ProceedMissingItem() {
        val intent = Intent (getActivity(), ItemSerialUPCMissingActivity::class.java)
        startActivity(intent)
    }
    fun ProceedPutAwayPallete() {
        val intent = Intent (getActivity(), PutAwayPalleteActivity::class.java)
        startActivity(intent)
    }

    fun ProceedFillPallete() {
        val intent = Intent (getActivity(), FillPalleteActivity::class.java)
        startActivity(intent)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}